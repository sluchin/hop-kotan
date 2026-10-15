/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.app

import android.app.AlertDialog
import android.content.Context
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ListAdapter
import android.widget.ListView
import android.widget.RatingBar
import jp.hopkotan.core.Ratings

/**
 * パネルが, アプリに頼む星の評価の操作.
 *
 * @property of ビールの評価 (キーを受け取って, 星の数を返す. 評価がなければ 0)
 * @property all 全ての評価
 * @property set 評価を変える (キーと星の数を受け取る. 0 なら評価を消す)
 */
class RatingActions(
    val of: (String) -> Int,
    val all: () -> Ratings,
    val set: (String, Int) -> Unit,
)

/**
 * 星 (5 つ) を選ぶ部品を作る.
 *
 * 星の部品 (RatingBar) を幅いっぱいに広げると, 星が 5 つより多く並んで, 半分の星 (1.5 など) しか選べなく
 * なる. 幅を中身に合わせて, 中央に置く.
 *
 * @receiver コンテキスト
 * @param current 今の星の数
 * @param onChange 星を選んだときの処理 (星の数を受け取る)
 * @return 星の部品を入れた枠
 */
fun Context.ratingBar(
    current: Int,
    onChange: (Int) -> Unit,
): LinearLayout {
    val bar =
        RatingBar(this).apply {
            numStars = Ratings.MAX
            stepSize = 1f
            rating = current.toFloat()
            setOnRatingBarChangeListener { _, value, fromUser ->
                if (fromUser) onChange(value.toInt())
            }
        }

    return LinearLayout(this).apply {
        gravity = Gravity.CENTER_HORIZONTAL
        addView(
            bar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
    }
}

/**
 * 星の評価を選ぶ画面を出す (星をタップすると, すぐに保存する. 同じ星をもう一度タップしても消えないので,
 * 「消す」のボタンで消す).
 *
 * @receiver コンテキスト
 * @param name ビールの名前
 * @param current 今の星の数
 * @param onChange 評価を変える処理 (星の数を受け取る. 0 なら評価を消す)
 */
fun Context.showRatingDialog(
    name: String,
    current: Int,
    onChange: (Int) -> Unit,
) {
    AlertDialog
        .Builder(this)
        .setTitle(name)
        .setView(ratingBar(current, onChange))
        .setNeutralButton(R.string.rating_clear) { _, _ -> onChange(0) }
        .setPositiveButton(R.string.close, null)
        .show()
}

/**
 * 一覧の項目を入れ替える (一覧の位置は, そのままにする).
 *
 * @receiver 一覧
 * @param newAdapter 新しいアダプタ
 */
fun ListView.replaceAdapter(newAdapter: ListAdapter) {
    val position = firstVisiblePosition
    val top = getChildAt(0)?.top ?: 0
    adapter = newAdapter
    setSelectionFromTop(position, top)
}

/**
 * 3 行目の小さな字を作る (なにもなければ null).
 *
 * @param parts 並べる文字 (null は飛ばす)
 * @return 文字を区切りでつないだもの
 */
fun joinExtra(vararg parts: String?): String? =
    parts
        .filterNotNull()
        .filter { it.isNotEmpty() }
        .joinToString(" / ")
        .ifEmpty { null }
