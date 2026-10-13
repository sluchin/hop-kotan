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
import jp.hopkotan.core.DaySummary
import jp.hopkotan.core.DrinkLog
import jp.hopkotan.core.DrinkRecord

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
 * 飲んだビールの記録を, 日ごとに表示するタブ (長く押すと, 消せる).
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
     */
    fun update(log: DrinkLog) {
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
        list.adapter =
            rowAdapter(activity, rows, actions.images, { describe(it) }, { it is DrinkRow.Item })
        list.setOnItemLongClickListener { _, _, position, _ ->
            (rows[position] as? DrinkRow.Item)?.let { confirmDelete(it.record) } != null
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
                        ),
                    showImage = false,
                )
            is DrinkRow.Item ->
                RowModel(
                    title = row.record.beerName,
                    subtitle =
                        activity.getString(
                            R.string.record_size,
                            row.record.style,
                            row.record.sizeLabel,
                            row.record.ml,
                        ),
                    badge = activity.getString(R.string.yen_badge, formatYen(row.record.yen)),
                    imageUrl = actions.imageOf(row.record.beerId),
                )
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
            ).setPositiveButton(R.string.delete) { _, _ -> actions.onDelete(record.id) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private companion object {
        const val MATCH = LinearLayout.LayoutParams.MATCH_PARENT
    }
}
