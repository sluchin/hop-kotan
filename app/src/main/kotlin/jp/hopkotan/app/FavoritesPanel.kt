/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.app

import android.app.Activity
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.core.view.isVisible
import jp.hopkotan.core.Beer
import jp.hopkotan.core.Favorites

/**
 * お気に入りのビールの画面 (左から引き出すメニュー ([AppDrawer]) から開く).
 *
 * 新しく足したビールを先に並べる. タップすると, 「ビール」のタブと同じ詳細を出す (お気に入りから外せる).
 *
 * @property activity 画面
 * @property actions アプリに頼む操作
 * @property favorites お気に入りの操作
 */
class FavoritesPanel(
    private val activity: Activity,
    private val actions: PanelActions,
    private val favorites: FavoriteActions,
) {
    private val title =
        TextView(activity).apply {
            setTextAppearance(
                com.google.android.material.R.style.TextAppearance_Material3_TitleLarge,
            )
            setText(R.string.favorites_title)
        }
    private val count = TextView(activity)
    private val empty = activity.bodyText().apply { setText(R.string.favorites_empty) }
    private val list = ListView(activity)

    /** 画面の内容. */
    val view: LinearLayout =
        LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            addView(
                LinearLayout(activity).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    addView(title, LinearLayout.LayoutParams(0, WRAP, 1f))
                    addView(count)
                },
            )
            addView(empty)
            addView(list, LinearLayout.LayoutParams(MATCH, MATCH))
        }

    /**
     * お気に入りのビールを並べ直す.
     *
     * @param beers 全てのビール
     * @param book お気に入り
     * @param onTapIds 今日のタップにあるビールの ID
     */
    fun update(
        beers: List<Beer>,
        book: Favorites,
        onTapIds: Set<String>,
    ) {
        val shown = book.beersIn(beers)
        count.text = activity.getString(R.string.count, shown.size)
        empty.isVisible = shown.isEmpty()
        list.replaceAdapter(
            rowAdapter(activity, shown, actions.images, {
                activity.beerRow(it, actions, favorites, it.id in onTapIds)
            }),
        )
        list.setOnItemClickListener { _, _, position, _ ->
            activity.showBeerDetail(shown[position], actions, favorites)
        }
    }

    private companion object {
        const val MATCH = LinearLayout.LayoutParams.MATCH_PARENT
        const val WRAP = LinearLayout.LayoutParams.WRAP_CONTENT
    }
}
