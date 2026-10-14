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
import jp.hopkotan.core.Ratings
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
    private val header =
        TextView(activity).apply {
            setTextAppearance(
                com.google.android.material.R.style.TextAppearance_Material3_BodyMedium,
            )
        }
    private val summary =
        TextView(activity).apply {
            setTextAppearance(
                com.google.android.material.R.style.TextAppearance_Material3_LabelLarge,
            )
            setBackgroundResource(R.drawable.badge_background)
            setPadding(
                activity.dp(CARD_PADDING_DP),
                activity.dp(CARD_PADDING_DP),
                activity.dp(CARD_PADDING_DP),
                activity.dp(CARD_PADDING_DP),
            )
        }
    private val list = ListView(activity).apply { divider = null }

    /** タブの内容. */
    val view: LinearLayout =
        LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            addView(header)
            addView(
                summary,
                LinearLayout.LayoutParams(MATCH, WRAP).apply {
                    setMargins(0, activity.dp(CARD_PADDING_DP), 0, activity.dp(CARD_PADDING_DP))
                },
            )
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
        list.replaceAdapter(rowAdapter(activity, entries, actions.images, { describe(it) }))
        list.setOnItemLongClickListener { _, _, position, _ ->
            val tap = entries[position].tap
            val key = Ratings.key(tap.beerId, tap.name)
            activity.showRatingDialog(tap.name, actions.ratings.of(key)) {
                actions.ratings.set(key, it)
            }
            true
        }
        list.setOnItemClickListener { _, _, position, _ ->
            val entry = entries[position]
            activity.showSizeDialog(entry.tap.name, actions.sizes()) {
                actions.onRecord(entry.tap, it, entry.beer?.abv)
            }
        }
    }

    private fun describe(entry: TapEntry): RowModel {
        val beer = entry.beer

        return RowModel(
            title = entry.tap.name,
            subtitle = entry.tap.style,
            extra =
                joinExtra(
                    Ratings.text(actions.ratings.of(Ratings.key(entry.tap.beerId, entry.tap.name))),
                ),
            badgeLead = beer?.ibu?.let { activity.getString(R.string.ibu_label, it.toString()) },
            badge = beer?.abv?.let { activity.getString(R.string.abv_badge, formatDecimal(it)) },
            imageUrl = entry.tap.imageUrl ?: beer?.imageUrl,
        )
    }

    private companion object {
        const val MATCH = LinearLayout.LayoutParams.MATCH_PARENT
        const val WRAP = LinearLayout.LayoutParams.WRAP_CONTENT
        const val CARD_PADDING_DP = 10
    }
}
