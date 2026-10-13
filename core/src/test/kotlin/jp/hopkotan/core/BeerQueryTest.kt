/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.core

import kotlin.test.Test
import kotlin.test.assertEquals

/** [BeerQuery] と [TapEntries] のテスト. */
class BeerQueryTest {
    private fun beer(
        name: String,
        style: String,
        released: String?,
        abv: Double?,
        ibu: Int?,
    ) = Beer("freedoms/$name", Series.FREEDOMS, name, style, "u", null, released, abv, ibu)

    private val a = beer("Alpha", "Pale Ale", "2026-09", 5.5, 40)
    private val b = beer("beta", "PALE ALE", "2025-01", 7.0, null)
    private val c = beer("Gamma", "Hazy IPA", null, null, 60)
    private val d =
        beer(
            "delta",
            "Saison",
            "2025-12",
            4.0,
            10,
        ).copy(id = "originals/delta", series = Series.ORIGINALS)
    private val all = listOf(a, b, c, d)

    private fun names(list: List<Beer>) = list.map { it.name }

    /** 並べ方: 既定は新しい順 (発売月が分からないものは最後). 昇順にしても, 最後. */
    @Test
    fun sortByReleased() {
        assertEquals(listOf("Alpha", "delta", "beta", "Gamma"), names(BeerQuery.apply(all)))
        assertEquals(
            listOf("beta", "delta", "Alpha", "Gamma"),
            names(BeerQuery.apply(all, descending = false)),
        )
    }

    /** 並べ方: 名前 (大文字と小文字を区別しない), ABV と IBU (分からないものは最後), スタイル. */
    @Test
    fun sortByOthers() {
        assertEquals(
            listOf("Alpha", "beta", "delta", "Gamma"),
            names(BeerQuery.apply(all, sort = SortKey.NAME, descending = false)),
        )
        assertEquals(
            listOf("beta", "Alpha", "delta", "Gamma"),
            names(BeerQuery.apply(all, sort = SortKey.ABV)),
        )
        assertEquals(
            listOf("delta", "Alpha", "beta", "Gamma"),
            names(BeerQuery.apply(all, sort = SortKey.ABV, descending = false)),
        )
        assertEquals(
            listOf("Gamma", "Alpha", "delta", "beta"),
            names(BeerQuery.apply(all, sort = SortKey.IBU)),
        )
        assertEquals(
            listOf("Gamma", "Alpha", "beta", "delta"),
            names(BeerQuery.apply(all, sort = SortKey.STYLE, descending = false)),
        )
    }

    /** 絞り込み: 文字 (名前とスタイル), スタイル (大文字と小文字は区別しない), シリーズ. */
    @Test
    fun filterByTextStyleSeries() {
        assertEquals(listOf("Alpha"), names(BeerQuery.apply(all, BeerFilter(text = " alp "))))
        assertEquals(listOf("Gamma"), names(BeerQuery.apply(all, BeerFilter(text = "hazy"))))
        assertEquals(
            listOf("Alpha", "beta"),
            names(BeerQuery.apply(all, BeerFilter(style = "pale ale"))),
        )
        assertEquals(
            listOf("delta"),
            names(BeerQuery.apply(all, BeerFilter(series = Series.ORIGINALS))),
        )
    }

    /** 絞り込み: ABV と IBU の下限 (分からないものは除く), 発売年, 今日のタップ. */
    @Test
    fun filterByNumbersYearAndTap() {
        assertEquals(
            listOf("beta", "Alpha"),
            names(BeerQuery.apply(all, BeerFilter(minAbv = 5.0), SortKey.ABV)),
        )
        assertEquals(
            listOf("Gamma", "Alpha"),
            names(BeerQuery.apply(all, BeerFilter(minIbu = 40), SortKey.IBU)),
        )
        assertEquals(
            listOf("delta", "beta"),
            names(BeerQuery.apply(all, BeerFilter(year = "2025"))),
        )
        assertEquals(
            listOf("beta"),
            names(BeerQuery.apply(all, BeerFilter(onTapIds = setOf(b.id)))),
        )
    }

    /** 条件は, 全て満たすものだけ残す. 何もなければ, 空. */
    @Test
    fun filterCombined() {
        assertEquals(
            listOf("Alpha"),
            names(
                BeerQuery.apply(all, BeerFilter(style = "Pale Ale", minAbv = 5.0, year = "2026")),
            ),
        )
        assertEquals(emptyList(), names(BeerQuery.apply(all, BeerFilter(text = "none"))))
    }

    /** スタイルの一覧 (大文字と小文字の違いは同じ, 空は除く) と, 発売年の一覧 (新しい順). */
    @Test
    fun stylesAndYears() {
        assertEquals(
            listOf("Hazy IPA", "Pale Ale", "Saison"),
            BeerQuery.styles(
                all + beer("e", "", null, null, null),
            ),
        )
        assertEquals(listOf("2026", "2025"), BeerQuery.years(all))
    }

    /** タップに, ビールのデータを結び付ける. ID がない, データがないタップは, null. */
    @Test
    fun tapEntries() {
        val list =
            TapList(
                "2026-10-13",
                emptyList(),
                listOf(
                    Tap("Alpha", "s", a.id),
                    Tap("Other", "s"),
                    Tap("Gone", "s", "freedoms/gone"),
                ),
            )
        val entries = TapEntries.of(list, all)

        assertEquals(listOf(a, null, null), entries.map { it.beer })
        assertEquals(listOf("Alpha", "Other", "Gone"), entries.map { it.tap.name })
    }
}
