/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/** 各ページの解析のテスト. */
class ParsersTest {
    private val base = "https://hopkotan.com"

    /** 詳細のページから, ABV と IBU (小数でも) を読む. */
    @Test
    fun detailSpec() {
        assertEquals(BeerSpec(5.0, 50), DetailParser.parse(Fixtures.detail("5.0", "50")))
        assertEquals(BeerSpec(4.5, 25), DetailParser.parse(Fixtures.detail("4.5", "25.0")))
    }

    /** 仕様がない, 数でない値は, null. */
    @Test
    fun detailSpecMissing() {
        assertEquals(BeerSpec(null, null), DetailParser.parse(Fixtures.detail(null, null)))
        assertEquals(BeerSpec(null, null), DetailParser.parse(Fixtures.detail("-", "n/a")))
        assertEquals(BeerSpec(5.0, null), DetailParser.parse(Fixtures.detail("5.0", null)))
        assertEquals(
            BeerSpec(null, null),
            DetailParser.parse("""<dl class="beer-spec"><dt>ABV</dt></dl>"""),
        )
        assertEquals(BeerSpec(null, 40), DetailParser.parse(Fixtures.detail(null, "40")))
    }

    /** FREEDOMS の一覧から, 項目と次のページを読む. 項目は, 名前かリンクがなければ, 捨てる. */
    @Test
    fun freedomsPage() {
        val html =
            Fixtures.freedoms(
                listOf(Triple("A", "Pale Ale", "a"), Triple("", "x", "b")),
                next = "$base/freedoms/page/2/",
            ) +
                """<div class="itemlist"><div class="item"><div class="item-name">NoLink</div></div></div>"""
        val page = FreedomsParser.parse(html, "$base/freedoms/")

        assertEquals(
            listOf(
                FreedomsItem("A", "Pale Ale", "$base/freedoms/a/", "$base/img/a.jpg", "2026-09"),
            ),
            page.items,
        )
        assertEquals("$base/freedoms/page/2/", page.nextUrl)
    }

    /** 最後のページは, 次がない. 画像がなければ, null. */
    @Test
    fun freedomsLastPage() {
        val html = """<div class="itemlist"><div class="item"><div class="item-thum"><a href="/freedoms/z/"><img></a></div>
            <div class="item-name">Z</div><div class="item-style">S</div></div></div>"""
        val page = FreedomsParser.parse(html, "$base/freedoms/")

        assertEquals(listOf(FreedomsItem("Z", "S", "$base/freedoms/z/", null, null)), page.items)
        assertNull(page.nextUrl)
    }

    /** 発売月の表記を, 年-月 にする. 読めなければ null. */
    @Test
    fun parseMonth() {
        assertEquals("2026-09", FreedomsParser.parseMonth("Sep. 2026"))
        assertEquals("2022-11", FreedomsParser.parseMonth("NOV. 2022"))
        assertEquals("2022-02", FreedomsParser.parseMonth("Feb.2022"))
        assertNull(FreedomsParser.parseMonth("Foo. 2022"))
        assertNull(FreedomsParser.parseMonth("2022"))
    }

    /** ORIGINALS: id が外側にあっても, 同じ要素にあっても読める. id がないものは捨てる. */
    @Test
    fun originals() {
        val beers =
            OriginalsParser.parse(
                Fixtures.originals + Fixtures.ORIGINALS_NO_ID,
                "$base/originals/",
            )

        assertEquals(
            listOf(
                Beer(
                    "originals/alpha",
                    Series.ORIGINALS,
                    "alpha",
                    "PILSNER",
                    "$base/originals/alpha/",
                    "$base/img/alpha.png",
                    null,
                    5.0,
                    25,
                ),
                Beer(
                    "originals/beta",
                    Series.ORIGINALS,
                    "beta",
                    "PALE ALE",
                    "$base/originals/#beta",
                    null,
                    null,
                    null,
                    null,
                ),
                Beer(
                    "originals/gamma",
                    Series.ORIGINALS,
                    "gamma",
                    "LAGER",
                    "$base/originals/#gamma",
                    null,
                    null,
                    null,
                    null,
                ),
            ),
            beers,
        )
    }

    /** ORIGINALS: id がある要素が, ページに 1 つもないとき, ビールは 0 件. */
    @Test
    fun originalsWithoutAnyId() {
        assertEquals(
            emptyList(),
            OriginalsParser.parse(Fixtures.ORIGINALS_NO_ID, "$base/originals/"),
        )
    }

    /** タップリスト: 更新日, 量と価格 (数のないものは捨てる), タップ (名前がないものは捨てる). */
    @Test
    fun taplist() {
        val list = TaplistParser.parse(Fixtures.taproom("2026.1.5"), "$base/taproom/")

        assertEquals("2026-01-05", list.date)
        assertEquals(listOf(PriceSize("M", 310, 750), PriceSize("L", 420, 900)), list.sizes)
        assertEquals(
            listOf(
                Tap(
                    "alpha",
                    "PILSNER",
                    "originals/alpha",
                    "$base/originals/#alpha",
                    "$base/img/tap-alpha.jpg",
                ),
                Tap(
                    "Test Beer",
                    "Hazy IPA",
                    "freedoms/test-beer",
                    "$base/freedoms/test-beer/",
                    "$base/img/tap-test.jpg",
                ),
                Tap("No Link", "Saison", null, null, null),
            ),
            list.taps,
        )
    }

    /** タップリスト: 更新日がなければ, 失敗にする. */
    @Test
    fun taplistWithoutDate() {
        assertFailsWith<ScrapeException> { TaplistParser.parse("<p>x</p>", "$base/taproom/") }
    }
}
