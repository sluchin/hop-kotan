/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.app

import android.app.Activity
import android.app.AlertDialog
import android.webkit.WebView
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import jp.hopkotan.core.Beer
import jp.hopkotan.core.MenuMatcher
import jp.hopkotan.core.Tap
import org.json.JSONTokener

/**
 * メニューの下に出す, 飲んだビールを記録する帯.
 *
 * メニューのページの文字から, 知っているビールを探して (端末の中だけで. 文字は保存しない), 見つけたものを
 * 並べる. タップすると, 量を選んで, 飲んだ記録に入れる. 見つからないときは, 今日のタップから手で選ぶ.
 *
 * @property activity 画面
 * @property actions アプリに頼む操作
 * @property taps 今日のタップ
 * @property beers ORIGINALS と FREEDOMS のビール
 * @property web メニューを表示している WebView
 */
class MenuBar(
    private val activity: Activity,
    private val actions: PanelActions,
    private val taps: () -> List<Tap>,
    private val beers: () -> List<Beer>,
    private val web: WebView,
) {
    private val found = LinearLayout(activity)
    private val label = TextView(activity)

    /** 帯の内容. */
    val view: LinearLayout =
        LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            addView(label)
            addView(HorizontalScrollView(activity).apply { addView(found) })
            addView(
                LinearLayout(activity).apply {
                    addView(button(R.string.menu_find) { find() })
                    addView(button(R.string.menu_other) { pickOther() })
                },
            )
        }

    /** ページの文字から, ビールを探す (ページが変わったときと, 「探す」を押したときに呼ぶ). */
    fun find() {
        web.evaluateJavascript("document.body ? document.body.innerText : ''") { json ->
            val text = (JSONTokener(json).nextValue() as? String).orEmpty()
            show(MenuMatcher.find(text, taps(), beers()))
        }
    }

    private fun show(list: List<Tap>) {
        label.setText(if (list.isEmpty()) R.string.menu_none_found else R.string.menu_found)
        found.removeAllViews()
        list.forEach { tap ->
            found.addView(
                Chip(activity).apply {
                    text = tap.name
                    setOnClickListener { record(tap) }
                },
            )
        }
    }

    private fun pickOther() {
        val list = taps()
        AlertDialog
            .Builder(activity)
            .setTitle(R.string.menu_pick)
            .setItems(list.map { it.name }.toTypedArray()) { _, index -> record(list[index]) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun record(tap: Tap) {
        val abv = beers().firstOrNull { it.id == tap.beerId }?.abv
        activity.showSizeDialog(tap.name, actions.sizes()) { actions.onRecord(tap, it, abv) }
    }

    private fun button(
        text: Int,
        onClick: () -> Unit,
    ): MaterialButton =
        MaterialButton(activity, null, android.R.attr.borderlessButtonStyle).apply {
            setText(text)
            setOnClickListener { onClick() }
        }
}
