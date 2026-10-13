/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.app

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import jp.hopkotan.core.Beer
import jp.hopkotan.core.DrinkLog
import jp.hopkotan.core.PriceSize
import jp.hopkotan.core.Tap
import jp.hopkotan.core.TapEntries
import jp.hopkotan.core.TapList
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.Executors

/** 量と価格が取れていないときの, 既定 (サイトの M と L). */
private val DEFAULT_SIZES = listOf(PriceSize("M", 310, 750), PriceSize("L", 420, 900))

/**
 * 忽布古丹醸造のビールを見て, 飲んだビールを記録する画面 (3 つのタブ: 今日, ビール, 記録).
 *
 * データは, GitHub の JSON (GitHub Actions が, 毎日更新する) を, 起動のときに取得する. 取得できなければ,
 * 端末に保存したデータを表示する. 飲んだ記録は, 端末の中だけに保存する.
 */
class MainActivity : Activity() {
    private lateinit var store: DataStore
    private lateinit var status: TextView
    private lateinit var today: TodayPanel
    private lateinit var beersPanel: BeersPanel
    private lateinit var drinksPanel: DrinksPanel
    private lateinit var panels: List<View>

    private var beers = emptyList<Beer>()
    private var taplist: TapList? = null
    private var drinks = DrinkLog()
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = DataStore(this)
        beers = store.loadBeers()
        taplist = store.loadTaplist()
        drinks = store.loadDrinks()
        setContentView(buildScreen())
        render(null)
        refresh()
    }

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }

    private fun buildScreen(): View {
        val actions =
            PanelActions(::record, ::delete) {
                taplist?.sizes?.ifEmpty { null }
                    ?: DEFAULT_SIZES
            }
        today = TodayPanel(this, actions)
        beersPanel = BeersPanel(this, actions)
        drinksPanel = DrinksPanel(this, actions)
        panels = listOf(today.view, beersPanel.view, drinksPanel.view)
        status = TextView(this)

        val tabs =
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                listOf(R.string.tab_today, R.string.tab_beers, R.string.tab_drinks).forEachIndexed {
                    index,
                    label,
                    ->
                    addView(button(label) { show(index) }, LinearLayout.LayoutParams(0, WRAP, 1f))
                }
                addView(
                    button(R.string.refresh) { refresh() },
                    LinearLayout.LayoutParams(0, WRAP, 1f),
                )
            }
        val content = FrameLayout(this).apply { panels.forEach { addView(it) } }
        show(0)

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(tabs)
            addView(status)
            addView(content, LinearLayout.LayoutParams(MATCH, MATCH))
            applyInsets(this)
        }
    }

    private fun show(index: Int) =
        panels.forEachIndexed { i, panel ->
            panel.visibility =
                if (i == index) View.VISIBLE else View.GONE
        }

    /** データを取得して, 画面を更新する (通信は, 別のスレッドで行う). */
    private fun refresh() {
        status.setText(R.string.status_loading)
        executor.execute {
            val result = runCatching { store.download() }
            runOnUiThread {
                result
                    .onSuccess { (newBeers, newTaplist) ->
                        beers = newBeers
                        taplist = newTaplist
                        render(null)
                    }.onFailure { render(it.message ?: it.javaClass.simpleName) }
            }
        }
    }

    /** 全てのタブと, 状態の表示を更新する. */
    private fun render(error: String?) {
        val list = taplist
        val entries = list?.let { TapEntries.of(it, beers) }.orEmpty()
        today.update(entries, list?.date, drinks.summary(LocalDate.now().toString()))
        beersPanel.update(beers, entries.mapNotNull { it.beer?.id }.toSet())
        drinksPanel.update(drinks)
        status.text =
            when {
                error != null -> getString(R.string.status_failed, error)
                list == null -> getString(R.string.status_empty)
                else -> getString(R.string.status_ready, list.date, list.taps.size, beers.size)
            }
    }

    private fun record(
        tap: Tap,
        size: PriceSize,
        abv: Double?,
    ) {
        drinks =
            drinks.add(
                DrinkLog.record(
                    UUID.randomUUID().toString(),
                    LocalDate.now().toString(),
                    tap,
                    size,
                    abv,
                ),
            )
        save()
        Toast.makeText(this, getString(R.string.recorded, tap.name), Toast.LENGTH_SHORT).show()
    }

    private fun delete(id: String) {
        drinks = drinks.remove(id)
        save()
    }

    /** 記録を保存して, 画面を更新する (書き込みは, 別のスレッドで行う). */
    private fun save() {
        val snapshot = drinks
        executor.execute { store.saveDrinks(snapshot) }
        render(null)
    }

    private companion object {
        const val MATCH = LinearLayout.LayoutParams.MATCH_PARENT
        const val WRAP = LinearLayout.LayoutParams.WRAP_CONTENT
    }
}
