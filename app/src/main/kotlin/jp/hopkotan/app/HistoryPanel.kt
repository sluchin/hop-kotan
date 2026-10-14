/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.app

import android.app.Activity
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import jp.hopkotan.core.Beer
import jp.hopkotan.core.Ratings
import jp.hopkotan.core.TapEntries
import jp.hopkotan.core.TapEntry
import jp.hopkotan.core.TapList

/** 履歴の一覧の 1 行 (日の見出しか, その日のタップのビール). */
private sealed interface HistoryRow {
    /**
     * 日の見出し.
     *
     * @property list その日のタップリスト
     */
    data class Day(
        val list: TapList,
    ) : HistoryRow

    /**
     * その日のタップのビール.
     *
     * @property entry タップのビール
     */
    data class Item(
        val entry: TapEntry,
    ) : HistoryRow
}

/**
 * 最近の日のタップリストを, 日ごとに表示するタブ (見るだけ).
 *
 * @property activity 画面
 * @property actions アプリに頼む操作
 */
class HistoryPanel(
    private val activity: Activity,
    private val actions: PanelActions,
) {
    private val total =
        TextView(activity).apply {
            setTextAppearance(
                com.google.android.material.R.style.TextAppearance_Material3_BodyMedium,
            )
        }
    private val list = ListView(activity).apply { divider = null }

    /** タブの内容. */
    val view: LinearLayout =
        LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            addView(total)
            addView(list, LinearLayout.LayoutParams(MATCH, MATCH))
        }

    /**
     * 表示を更新する.
     *
     * @param history 日ごとのタップリスト (新しい日が先)
     * @param beers ビール
     */
    fun update(
        history: List<TapList>,
        beers: List<Beer>,
    ) {
        val rows =
            history.flatMap { day ->
                listOf<HistoryRow>(HistoryRow.Day(day)) +
                    TapEntries.of(day, beers).map { HistoryRow.Item(it) }
            }
        total.text =
            if (history.isEmpty()) {
                activity.getString(R.string.recent_none)
            } else {
                activity.getString(R.string.recent_total, history.size)
            }
        list.replaceAdapter(
            rowAdapter(activity, rows, actions.images, { describe(it) }, { it is HistoryRow.Item }),
        )
        list.setOnItemClickListener { _, _, position, _ ->
            (rows[position] as? HistoryRow.Item)?.let { item ->
                val tap = item.entry.tap
                val key = Ratings.key(tap.beerId, tap.name)
                activity.showRatingDialog(tap.name, actions.ratings.of(key)) {
                    actions.ratings.set(key, it)
                }
            }
        }
    }

    private fun describe(row: HistoryRow): RowModel =
        when (row) {
            is HistoryRow.Day ->
                RowModel(
                    title = row.list.date,
                    subtitle = activity.getString(R.string.count, row.list.taps.size),
                    showImage = false,
                )
            is HistoryRow.Item -> {
                val beer = row.entry.beer

                RowModel(
                    title = row.entry.tap.name,
                    subtitle = row.entry.tap.style,
                    extra =
                        joinExtra(
                            Ratings.text(
                                actions.ratings.of(
                                    Ratings.key(row.entry.tap.beerId, row.entry.tap.name),
                                ),
                            ),
                        ),
                    badgeLead =
                        beer?.ibu?.let {
                            activity.getString(
                                R.string.ibu_label,
                                it.toString(),
                            )
                        },
                    badge =
                        beer?.abv?.let {
                            activity.getString(
                                R.string.abv_badge,
                                formatDecimal(it),
                            )
                        },
                    imageUrl = row.entry.tap.imageUrl ?: beer?.imageUrl,
                )
            }
        }

    private companion object {
        const val MATCH = LinearLayout.LayoutParams.MATCH_PARENT
    }
}
