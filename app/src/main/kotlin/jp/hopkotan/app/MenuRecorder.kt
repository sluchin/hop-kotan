/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.app

import android.app.Activity
import android.app.AlertDialog
import android.webkit.WebView
import android.widget.Toast
import jp.hopkotan.core.Beer
import jp.hopkotan.core.MenuMatcher
import jp.hopkotan.core.Tap
import org.json.JSONArray
import org.json.JSONException

/** ログのタグ. */
private const val TAG = "Menu"

/** 注文の画面の文字を, アプリに渡すときの, コンソールのメッセージの先頭. */
const val ORDER_MESSAGE = "hopkotan:order:"

/** メニューのページの文字を, アプリに渡すときの, コンソールのメッセージの先頭. */
const val MENU_MESSAGE = "hopkotan:menu:"

/**
 * ページに入れるスクリプト. 「注文数を選ぶ」が押されたら, 注文の画面の文字の候補 (画面の上の見出しと,
 * ボタンの外側の部品の文字. 狭い順) を, コンソールのメッセージでアプリに渡す (ページから, アプリの機能を
 * 呼べる窓口は作らない). また, ページの中身が変わって, 1 秒落ち着いたら, ページの文字を渡す (メニューは,
 * ページを開いたあとに組み立てられるため. 文字が前と同じなら, 渡さない). 2 回入れても, 1 回だけ動く.
 */
private val ORDER_WATCH_SCRIPT =
    """
    (function () {
      if (window.__hopKotanOrder) return;
      window.__hopKotanOrder = true;
      document.addEventListener('click', function (event) {
        var button = event.target.closest('button, a, [role="button"]');
        if (!button || (button.innerText || '').indexOf('注文数を選ぶ') < 0) return;
        var texts = [];
        [0.25, 0.5].forEach(function (x) {
          [24, 48, 72, 96].forEach(function (y) {
            var element = document.elementFromPoint(window.innerWidth * x, y);
            if (element && element.innerText) texts.push(element.innerText);
          });
        });
        for (var node = button.parentElement; node && node !== document.body; node = node.parentElement) {
          var text = node.innerText || '';
          if (text.length <= 2000) texts.push(text);
        }
        console.log('$ORDER_MESSAGE' + JSON.stringify(texts));
      }, true);
      var timer = null;
      var last = '';
      var send = function () {
        var text = document.body ? document.body.innerText : '';
        if (text === last) return;
        last = text;
        console.log('$MENU_MESSAGE' + text);
      };
      new MutationObserver(function () {
        clearTimeout(timer);
        timer = setTimeout(send, 1000);
      }).observe(document.documentElement, { childList: true, subtree: true, characterData: true });
      timer = setTimeout(send, 1000);
    })();
    """.trimIndent()

/**
 * メニューのページで注文したビールを, 飲んだ記録に入れる.
 *
 * 「注文数を選ぶ」が押されたら, 注文の画面の文字から, 知っているビールを探して (端末の中だけで. 文字は
 * 保存しない), 記録するかを聞いて, 記録するなら, 量を選ぶ画面を出す. メニューのページにあるビールのうち, 今日のタップリストにない
 * ものは, 今日のタップリストに足す (サイトのタップリストを取得したあとに, 開栓したビール). ビールが複数見つかったら, 先にどのビールかを選ぶ. 見つからない
 * ときと, 手で記録したいときは, 今日のタップから選ぶ ([pickOther]).
 *
 * @property activity 画面
 * @property actions アプリに頼む操作
 * @property taps 今日のタップ
 * @property beers ORIGINALS と FREEDOMS のビール
 * @property web メニューを表示している WebView
 * @property onOpened 見つけたビールのうち, 今日のタップリストにないものを足す (あとから開栓したビール)
 */
class MenuRecorder(
    private val activity: Activity,
    private val actions: PanelActions,
    private val taps: () -> List<Tap>,
    private val beers: () -> List<Beer>,
    private val web: WebView,
    private val onOpened: (List<Tap>) -> Unit,
) {
    /** ページを読み込んだら, 「注文数を選ぶ」を見張るスクリプトを入れる. */
    fun watchOrder() = web.evaluateJavascript(ORDER_WATCH_SCRIPT, null)

    /**
     * 「注文数を選ぶ」が押されたときに, 注文の画面の文字から, ビールを探して, 記録するかを聞く.
     *
     * @param json 文字の候補 (JSON の配列. 狭い順)
     */
    fun onOrder(json: String) {
        val candidates =
            try {
                JSONArray(json).let { array -> List(array.length()) { array.optString(it) } }
            } catch (e: JSONException) {
                AppLog.w(TAG, "Could not read the order message", e)
                return
            }
        val list = MenuMatcher.findOrdered(candidates, taps(), beers())
        AppLog.d(TAG) { "Found ${list.size} beers for the order" }
        // 今日のタップリストにないビール (あとから開栓したビール) は, 今日のタップリストに足す
        onOpened(list)
        when (list.size) {
            0 -> Toast.makeText(activity, R.string.menu_none_found, Toast.LENGTH_LONG).show()
            1 ->
                // 記録するか, しないかを, 先に聞く (同じビールを続けて頼むときや, 注文をやめたとき)
                AlertDialog
                    .Builder(activity)
                    .setMessage(activity.getString(R.string.menu_record_ask, list[0].name))
                    .setPositiveButton(R.string.menu_record_yes) { _, _ -> record(list[0]) }
                    .setNegativeButton(R.string.menu_record_no, null)
                    .show()
            else -> choose(R.string.menu_pick_found, list, R.string.menu_record_no)
        }
    }

    /**
     * メニューのページの文字から, メニューにあるビールを探して, 今日のタップリストにないものを足す
     * (サイトのタップリストを取得したあとに, 開栓したビール).
     *
     * @param text ページの文字
     */
    fun onMenu(text: String) {
        val list = MenuMatcher.find(text, taps(), beers())
        AppLog.d(TAG) { "Found ${list.size} beers on the menu" }
        onOpened(list)
    }

    /** 今日のタップから, 飲んだビールを選んで, 記録する (見つからないときと, 手で記録したいとき). */
    fun pickOther() = choose(R.string.menu_pick, taps())

    /**
     * ビールを選ぶ画面を出して, 選んだビールを記録する.
     *
     * @param title 題名の文字列のリソース
     * @param list ビール
     * @param negative 選ばずに閉じるボタンの文字列のリソース
     */
    private fun choose(
        title: Int,
        list: List<Tap>,
        negative: Int = R.string.cancel,
    ) {
        AlertDialog
            .Builder(activity)
            .setTitle(title)
            .setItems(list.map { it.name }.toTypedArray()) { _, index -> record(list[index]) }
            .setNegativeButton(negative, null)
            .show()
    }

    /**
     * 量を選ぶ画面を出して, 飲んだ記録に入れる.
     *
     * @param tap ビール
     */
    private fun record(tap: Tap) {
        val abv = beers().firstOrNull { it.id == tap.beerId }?.abv
        activity.showSizeDialog(tap.name, actions.sizes()) { actions.onRecord(tap, it, abv) }
    }
}
