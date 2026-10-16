/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.app

import android.app.Activity
import android.app.AlertDialog
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import jp.hopkotan.core.Beer
import jp.hopkotan.core.DaySummary
import jp.hopkotan.core.DrinkLog
import jp.hopkotan.core.DrinkRecord
import jp.hopkotan.core.Ratings

/** 記録の一覧の 1 行 (日の見出しか, 1 件の記録). */
private sealed interface DrinkRow {
    /**
     * 日の見出し.
     *
     * @property summary その日の集計
     */
    data class Day(
        val summary: DaySummary,
    ) : DrinkRow

    /**
     * 1 件の記録.
     *
     * @property record 記録
     */
    data class Item(
        val record: DrinkRecord,
    ) : DrinkRow
}

/**
 * 飲んだビールの記録を, 日ごとに表示するタブ (長く押すと, 変えるか消せる).
 *
 * @property activity 画面
 * @property actions アプリに頼む操作
 */
class DrinksPanel(
    private val activity: Activity,
    private val actions: PanelActions,
) {
    private val total =
        TextView(activity).apply {
            setTextAppearance(
                com.google.android.material.R.style.TextAppearance_Material3_BodyMedium,
            )
        }
    private var beers: List<Beer> = emptyList()
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
     * @param log 飲んだ記録
     * @param beers ビールのデータ (記録を変えるとき, 足りない ABV を補う)
     */
    fun update(
        log: DrinkLog,
        beers: List<Beer>,
    ) {
        this.beers = beers
        val rows =
            log.dates().flatMap { date ->
                listOf<DrinkRow>(DrinkRow.Day(log.summary(date))) +
                    log.forDate(date).map { DrinkRow.Item(it) }
            }
        total.text =
            if (rows.isEmpty()) {
                activity.getString(R.string.drinks_none)
            } else {
                activity.getString(
                    R.string.drinks_total,
                    log.dates().size,
                    log.records.size,
                    formatYen(log.records.sumOf { it.yen }),
                )
            }
        list.replaceAdapter(
            rowAdapter(activity, rows, actions.images, { describe(it) }, { it is DrinkRow.Item }),
        )
        list.setOnItemClickListener { _, _, position, _ ->
            (rows[position] as? DrinkRow.Item)?.let { item ->
                val key = Ratings.key(item.record.beerId, item.record.beerName)
                activity.showRatingDialog(item.record.beerName, actions.ratings.of(key)) {
                    actions.ratings.set(key, it)
                }
            }
        }
        list.setOnItemLongClickListener { _, _, position, _ ->
            (rows[position] as? DrinkRow.Item)?.let { showActions(it.record) } != null
        }
    }

    private fun describe(row: DrinkRow): RowModel =
        when (row) {
            is DrinkRow.Day ->
                RowModel(
                    title = row.summary.date,
                    subtitle =
                        activity.getString(
                            R.string.day_summary,
                            row.summary.count,
                            row.summary.ml,
                            formatYen(row.summary.yen),
                            formatDecimal(row.summary.alcoholG),
                            Math.round(row.summary.kcal),
                        ),
                    showImage = false,
                )
            is DrinkRow.Item ->
                RowModel(
                    title = row.record.beerName,
                    subtitle = recordSubtitle(row.record),
                    extra =
                        joinExtra(
                            Ratings.text(
                                actions.ratings.of(
                                    Ratings.key(row.record.beerId, row.record.beerName),
                                ),
                            ),
                        ),
                    badge = activity.getString(R.string.yen_badge, formatYen(row.record.yen)),
                    imageUrl = actions.imageOf(row.record.beerId),
                )
        }

    /** 記録の 2 行目 (時刻があれば, 先頭に付ける). */
    private fun recordSubtitle(record: DrinkRecord): String =
        if (record.time == null) {
            activity.getString(R.string.record_size, record.style, record.sizeLabel, record.ml)
        } else {
            activity.getString(
                R.string.record_size_time,
                record.style,
                record.sizeLabel,
                record.ml,
                record.time,
            )
        }

    private fun showActions(record: DrinkRecord) {
        AlertDialog
            .Builder(activity)
            .setTitle(record.beerName)
            .setItems(
                arrayOf(activity.getString(R.string.edit), activity.getString(R.string.delete)),
            ) { _, which ->
                if (which == 0) {
                    activity.showRecordEditor(
                        DrinkLog.complete(record, beers),
                        actions.sizes(),
                        actions.records.edit,
                    )
                } else {
                    confirmDelete(record)
                }
            }.show()
    }

    private fun confirmDelete(record: DrinkRecord) {
        AlertDialog
            .Builder(activity)
            .setTitle(R.string.delete_title)
            .setMessage(
                activity.getString(
                    R.string.record_line,
                    record.beerName,
                    record.sizeLabel,
                    record.ml,
                ),
            ).setPositiveButton(R.string.delete) { _, _ -> actions.records.delete(record.id) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private companion object {
        const val MATCH = LinearLayout.LayoutParams.MATCH_PARENT
    }
}
