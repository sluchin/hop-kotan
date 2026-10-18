/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.app

import android.app.Activity
import android.app.AlertDialog
import android.widget.LinearLayout
import android.widget.ScrollView
import com.google.android.material.button.MaterialButton
import jp.hopkotan.core.Ratings
import jp.hopkotan.core.TapEntry

/**
 * 今日のタップのビールの画面を出す (星の評価, お気に入りのボタン, 量ごとの記録のボタン).
 *
 * 量のボタン (「M 310 ml (750 円) を記録」) を押すと, 飲んだ記録に入れて, 閉じる.
 *
 * @receiver 画面
 * @param entry タップのビール
 * @param actions アプリに頼む操作
 * @param favorites お気に入りの操作
 */
fun Activity.showTapDialog(
    entry: TapEntry,
    actions: PanelActions,
    favorites: FavoriteActions,
) {
    val tap = entry.tap
    val key = Ratings.key(tap.beerId, tap.name)
    showOnce {
        lateinit var dialog: AlertDialog
        val content =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(DIALOG_PADDING_DP), 0, dp(DIALOG_PADDING_DP), 0)
                addView(
                    ratingBar(actions.ratings.of(key), withClear = true) {
                        actions.ratings.set(key, it)
                    },
                )
                addView(favoriteButton(key, favorites))
                actions.sizes().forEach { size ->
                    addView(
                        MaterialButton(this@showTapDialog).apply {
                            text = getString(R.string.size_item, size.label, size.ml, size.yen)
                            setOnClickListener {
                                dialog.dismiss()
                                actions.onRecord(tap, size, entry.beer?.abv)
                            }
                        },
                    )
                }
            }
        dialog =
            AlertDialog
                .Builder(this)
                .setTitle(tap.name)
                .setMessage(tap.style)
                .setView(ScrollView(this).apply { addView(content) })
                .setNegativeButton(R.string.close, null)
                .show()
        dialog
    }
}
