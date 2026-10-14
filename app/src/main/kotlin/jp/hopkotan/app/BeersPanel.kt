/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.core.net.toUri
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import jp.hopkotan.core.Beer
import jp.hopkotan.core.BeerFilter
import jp.hopkotan.core.BeerQuery
import jp.hopkotan.core.Ratings
import jp.hopkotan.core.Series
import jp.hopkotan.core.SortKey
import jp.hopkotan.core.Tap

/** ABV の下限の選択肢 (%). */
private val ABV_CHOICES = listOf(4.0, 5.0, 6.0, 7.0, 8.0, 9.0)

/** IBU の下限の選択肢. */
private val IBU_CHOICES = listOf(20, 40, 60, 80)

/**
 * 全てのビールを, ソートと絞り込みをして表示するタブ (タップすると, 詳細と, 記録のボタンが出る).
 *
 * @property activity 画面
 * @property actions アプリに頼む操作
 */
class BeersPanel(
    private val activity: Activity,
    private val actions: PanelActions,
) {
    private var beers = emptyList<Beer>()
    private var onTapIds = emptySet<String>()
    private var styleChoices = emptyList<String>()
    private var yearChoices = emptyList<String>()

    private val allLabel = activity.getString(R.string.filter_all)
    private val searchLayout =
        TextInputLayout(
            activity,
            null,
            com.google.android.material.R.attr.textInputOutlinedStyle,
        ).apply {
            hint = activity.getString(R.string.search_hint)
        }
    private val search = TextInputEditText(searchLayout.context).also { searchLayout.addView(it) }
    private val toggle =
        MaterialButton(
            activity,
            null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle,
        ).apply {
            setText(R.string.filter_toggle)
        }
    private val sort = activity.spinner(sortLabels()) { refresh() }
    private val order =
        activity.spinner(
            listOf(R.string.order_desc, R.string.order_asc).map {
                activity.getString(it)
            },
        ) { refresh() }
    private val style = activity.spinner(listOf(allLabel)) { refresh() }
    private val series =
        activity.spinner(listOf(allLabel, SERIES_ORIGINALS, SERIES_FREEDOMS)) {
            refresh()
        }
    private val year = activity.spinner(listOf(allLabel)) { refresh() }
    private val abv =
        activity.spinner(
            choices(ABV_CHOICES.map { it.toInt().toString() }),
        ) { refresh() }
    private val ibu = activity.spinner(choices(IBU_CHOICES.map { it.toString() })) { refresh() }
    private val stars =
        activity.spinner(
            choices((1..Ratings.MAX).map { it.toString() }),
        ) { refresh() }
    private val tapOnly = CheckBox(activity).apply { setText(R.string.filter_on_tap) }
    private val count = TextView(activity)
    private val list = ListView(activity)

    /** 絞り込みの部品 (「絞り込み」のボタンで, 開閉する). */
    private val filterBox =
        LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            addView(
                activity.row(
                    activity.labeled(R.string.filter_style, style),
                    activity.labeled(R.string.filter_series, series),
                ),
            )
            addView(
                activity.row(
                    activity.labeled(R.string.filter_year, year),
                    activity.labeled(R.string.filter_abv, abv),
                    activity.labeled(R.string.filter_ibu, ibu),
                ),
            )
            addView(activity.row(activity.labeled(R.string.filter_stars, stars)))
            addView(tapOnly)
        }

    /** タブの内容. */
    val view: LinearLayout =
        LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            addView(
                LinearLayout(activity).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    addView(searchLayout, LinearLayout.LayoutParams(0, WRAP, 1f))
                    addView(toggle)
                },
            )
            addView(
                activity.row(
                    activity.labeled(R.string.label_sort, sort),
                    activity.labeled(R.string.label_order, order),
                ),
            )
            addView(count)
            addView(list, LinearLayout.LayoutParams(MATCH, MATCH))
        }

    init {
        search.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int,
                ) = Unit

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int,
                ) = refresh()

                override fun afterTextChanged(s: Editable?) = Unit
            },
        )
        tapOnly.setOnCheckedChangeListener { _, _ -> refresh() }
        toggle.setOnClickListener { showFilters() }
    }

    /**
     * ビールを更新する (スタイルと発売年の選択肢も作り直す).
     *
     * @param newBeers 全てのビール
     * @param newOnTapIds 今日のタップにあるビールの ID
     */
    fun update(
        newBeers: List<Beer>,
        newOnTapIds: Set<String>,
    ) {
        beers = newBeers
        onTapIds = newOnTapIds
        styleChoices = BeerQuery.styles(newBeers)
        yearChoices = BeerQuery.years(newBeers)
        reset(style, listOf(allLabel) + styleChoices)
        reset(year, listOf(allLabel) + yearChoices)
        refresh()
    }

    private fun sortLabels(): List<String> =
        listOf(
            R.string.sort_released,
            R.string.sort_name,
            R.string.sort_abv,
            R.string.sort_ibu,
            R.string.sort_style,
            R.string.sort_rating,
        ).map { activity.getString(it) }

    private fun choices(values: List<String>): List<String> =
        listOf(activity.getString(R.string.filter_any)) +
            values.map { activity.getString(R.string.filter_min, it) }

    /** 選択肢を入れ替える (前に選んでいたものが, まだあれば, それを選んだままにする). */
    private fun reset(
        spinner: Spinner,
        items: List<String>,
    ) {
        val selected = spinner.selectedItem as? String
        spinner.adapter =
            android.widget.ArrayAdapter(
                activity,
                android.R.layout.simple_spinner_dropdown_item,
                items,
            )
        spinner.setSelection(items.indexOf(selected).coerceAtLeast(0))
    }

    private fun filter(): BeerFilter =
        BeerFilter(
            text = search.text.toString(),
            style = styleChoices.getOrNull(style.selectedItemPosition - 1),
            series = listOf(null, Series.ORIGINALS, Series.FREEDOMS)[series.selectedItemPosition],
            minAbv = ABV_CHOICES.getOrNull(abv.selectedItemPosition - 1),
            minIbu = IBU_CHOICES.getOrNull(ibu.selectedItemPosition - 1),
            year = yearChoices.getOrNull(year.selectedItemPosition - 1),
            minStars = (1..Ratings.MAX).toList().getOrNull(stars.selectedItemPosition - 1),
            onTapIds = if (tapOnly.isChecked) onTapIds else null,
        )

    /** 絞り込みの部品を, ポップアップに出す (選ぶとすぐ, 一覧に反映する). */
    private fun showFilters() {
        AlertDialog
            .Builder(activity)
            .setTitle(R.string.filter_toggle)
            .setView(ScrollView(activity).apply { addView(filterBox) })
            .setPositiveButton(R.string.close, null)
            .setOnDismissListener { (filterBox.parent as? ViewGroup)?.removeView(filterBox) }
            .show()
    }

    private fun refresh() {
        val sorted =
            BeerQuery.apply(
                beers,
                filter(),
                SortKey.entries[sort.selectedItemPosition],
                descending = order.selectedItemPosition == 0,
                ratings = actions.ratings.all(),
            )
        count.text = activity.getString(R.string.count, sorted.size)
        list.replaceAdapter(rowAdapter(activity, sorted, actions.images, { describe(it) }))
        list.setOnItemClickListener { _, _, position, _ -> showDetail(sorted[position]) }
    }

    private fun describe(beer: Beer): RowModel {
        val extra =
            joinExtra(
                Ratings.text(actions.ratings.of(Ratings.key(beer.id, beer.name))),
                if (beer.id in onTapIds) activity.getString(R.string.on_tap) else null,
            )

        return RowModel(
            title = beer.name,
            subtitle =
                activity.getString(
                    R.string.row_style_released,
                    beer.style,
                    beer.released ?: beer.series.name,
                ),
            extra = extra,
            badgeLead = beer.ibu?.let { activity.getString(R.string.ibu_label, it.toString()) },
            badge = beer.abv?.let { activity.getString(R.string.abv_badge, formatDecimal(it)) },
            imageUrl = beer.imageUrl,
        )
    }

    private fun detailText(beer: Beer): String {
        val unknown = activity.getString(R.string.unknown)

        return activity.getString(
            R.string.beer_detail,
            beer.style,
            beer.released ?: beer.series.name,
            beer.abv?.let { formatDecimal(it) } ?: unknown,
            beer.ibu?.toString() ?: unknown,
        )
    }

    private fun showDetail(beer: Beer) {
        AlertDialog
            .Builder(activity)
            .setTitle(beer.name)
            .setMessage(detailText(beer))
            .setView(
                activity.ratingBar(actions.ratings.of(Ratings.key(beer.id, beer.name))) {
                    actions.ratings.set(Ratings.key(beer.id, beer.name), it)
                },
            ).setPositiveButton(R.string.drink) { _, _ ->
                val tap = Tap(beer.name, beer.style, beer.id, beer.url, beer.imageUrl)
                activity.showSizeDialog(
                    beer.name,
                    actions.sizes(),
                ) { actions.onRecord(tap, it, beer.abv) }
            }.setNeutralButton(R.string.open_site) { _, _ ->
                activity.startActivity(Intent(Intent.ACTION_VIEW, beer.url.toUri()))
            }.setNegativeButton(R.string.close, null)
            .show()
    }

    private companion object {
        const val MATCH = LinearLayout.LayoutParams.MATCH_PARENT
        const val WRAP = LinearLayout.LayoutParams.WRAP_CONTENT
        const val SERIES_ORIGINALS = "ORIGINALS"
        const val SERIES_FREEDOMS = "FREEDOMS"
    }
}
