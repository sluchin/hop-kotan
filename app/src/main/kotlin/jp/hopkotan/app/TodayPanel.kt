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
import jp.hopkotan.core.DaySummary
import jp.hopkotan.core.TapEntry

/**
 * 今日のタップリストを表示して, 飲んだビールを記録するタブ.
 *
 * @property activity 画面
 * @property actions アプリに頼む操作
 */
class TodayPanel(
    private val activity: Activity,
    private val actions: PanelActions,
) {
    private val header = TextView(activity)
    private val summary = TextView(activity)
    private val list = ListView(activity)

    /** タブの内容. */
    val view: LinearLayout =
        LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            addView(header)
            addView(summary)
            addView(list, LinearLayout.LayoutParams(MATCH, MATCH))
        }

    /**
     * 表示を更新する.
     *
     * @param entries タップのビール
     * @param date タップリストの更新日 (なければ null)
     * @param today 今日の飲んだ記録の集計
     */
    fun update(
        entries: List<TapEntry>,
        date: String?,
        today: DaySummary,
    ) {
        header.text =
            if (date ==
                null
            ) {
                activity.getString(R.string.today_none)
            } else {
                activity.getString(R.string.today_header, date)
            }
        summary.text =
            activity.getString(
                R.string.today_summary,
                today.count,
                today.ml,
                formatYen(today.yen),
                formatDecimal(today.alcoholG),
            )
        list.adapter =
            twoLineAdapter(activity, entries, { it.tap.name }, { describe(it) })
        list.setOnItemClickListener { _, _, position, _ ->
            val entry = entries[position]
            activity.showSizeDialog(entry.tap.name, actions.sizes()) {
                actions.onRecord(entry.tap, it, entry.beer?.abv)
            }
        }
    }

    private fun describe(entry: TapEntry): String {
        val unknown = activity.getString(R.string.unknown)
        val abv = entry.beer?.abv?.let { formatDecimal(it) } ?: unknown
        val ibu = entry.beer?.ibu?.toString() ?: unknown

        return activity.getString(R.string.tap_detail, entry.tap.style, abv, ibu)
    }

    private companion object {
        const val MATCH = LinearLayout.LayoutParams.MATCH_PARENT
    }
}
