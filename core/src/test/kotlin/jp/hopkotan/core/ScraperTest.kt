/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** [Scraper] のテスト. ページは, 偽物の取得で返す. */
class ScraperTest {
    private val base = "https://hopkotan.com"

    /** URL ごとの HTML を返し, 取得した URL を覚える偽物. */
    private class FakeFetcher(
        private val pages: Map<String, String>,
    ) : HtmlFetcher {
        val fetched = mutableListOf<String>()

        override fun fetch(url: String): String {
            fetched += url

            return pages.getValue(url)
        }
    }

    private fun freedomsPages(): Map<String, String> =
        mapOf(
            "$base/freedoms/" to
                Fixtures.freedoms(
                    listOf(Triple("Old", "Saison", "old"), Triple("Not Beer", "x", "")),
                    month = "Jan. 2025",
                    next = "$base/freedoms/page/2/",
                ),
            "$base/freedoms/page/2/" to
                Fixtures.freedoms(
                    listOf(
                        Triple("New B", "IPA", "new-b"),
                        Triple("New A", "IPA", "new-a"),
                        Triple("Old", "Saison", "old"),
                    ),
                ),
            "$base/freedoms/old/" to Fixtures.detail("6.0", "30"),
            "$base/freedoms/new-a/" to Fixtures.detail("5.5", "40"),
            "$base/freedoms/new-b/" to Fixtures.detail(null, null),
        )

    /** タップリストを取得する. タップが 0 件なら失敗にする. */
    @Test
    fun taplist() {
        val ok = Scraper(FakeFetcher(mapOf("$base/taproom/" to Fixtures.taproom())))
        assertEquals(3, ok.scrapeTaplist().taps.size)

        val empty =
            Scraper(
                FakeFetcher(
                    mapOf(
                        "$base/taproom/" to
                            Fixtures.taproom().substringBefore("<div class=\"taproom-taplist\">"),
                    ),
                ),
            )
        assertFailsWith<ScrapeException> { empty.scrapeTaplist() }
    }

    /** ORIGINALS を取得する. 0 件なら失敗にする. */
    @Test
    fun originals() {
        val ok = Scraper(FakeFetcher(mapOf("$base/originals/" to Fixtures.originals)))
        assertEquals(
            listOf("originals/alpha", "originals/beta", "originals/gamma"),
            ok.scrapeOriginals().map { it.id },
        )

        val empty = Scraper(FakeFetcher(mapOf("$base/originals/" to "<p>x</p>")))
        assertFailsWith<ScrapeException> { empty.scrapeOriginals() }
    }

    /** FREEDOMS: 全ページをたどり, ビールでない項目と, 重複を除き, 新しい順に並べる. */
    @Test
    fun freedoms() {
        val fetcher = FakeFetcher(freedomsPages())
        val beers = Scraper(fetcher).scrapeFreedoms()

        assertEquals(listOf("New A", "New B", "Old"), beers.map { it.name })
        assertEquals(
            Beer(
                "freedoms/new-a",
                Series.FREEDOMS,
                "New A",
                "IPA",
                "$base/freedoms/new-a/",
                "$base/img/new-a.jpg",
                "2026-09",
                5.5,
                40,
            ),
            beers[0],
        )
        assertEquals(null, beers[1].abv)
        assertEquals(
            3,
            fetcher.fetched.count {
                it.contains("/freedoms/") &&
                    !it.contains("page") &&
                    it != "$base/freedoms/"
            },
        )
    }

    /** FREEDOMS: 取得済みのビールは, 詳細のページを取り直さない. */
    @Test
    fun freedomsKnown() {
        val known =
            Beer(
                "freedoms/old",
                Series.FREEDOMS,
                "Old",
                "Saison",
                "$base/freedoms/old/",
                null,
                "2025-01",
                6.0,
                30,
            )
        val fetcher = FakeFetcher(freedomsPages())
        val beers = Scraper(fetcher).scrapeFreedoms(mapOf(known.id to known))

        assertEquals(known, beers.last())
        assertEquals(false, fetcher.fetched.contains("$base/freedoms/old/"))
    }

    /** タップのビール: ORIGINALS plus と HARVEST BREW だけ, 詳細のページから読む (取得済みは, 取り直さない). */
    @Test
    fun tapBeers() {
        val taps =
            listOf(
                Tap(
                    "ikor",
                    "Melon Hazy IPA",
                    "originalsplus/ikor",
                    "$base/originalsplus/ikor/",
                    "$base/i.jpg",
                ),
                Tap(
                    "Pilsner 2026",
                    "Harvest Pilsner",
                    "harvest_brew/p-2026",
                    "$base/harvest_brew/p-2026/",
                ),
                Tap(
                    "Pilsner 2026",
                    "Harvest Pilsner",
                    "harvest_brew/p-2026",
                    "$base/harvest_brew/p-2026/",
                ),
                Tap("alpha", "PILSNER", "originals/alpha", "$base/originals/#alpha"),
                Tap("No Link", "Saison"),
                Tap("No Page", "Saison", "originalsplus/no-page"),
            )
        val fetcher =
            FakeFetcher(
                mapOf(
                    "$base/originalsplus/ikor/" to Fixtures.detail("7.5", "28"),
                    "$base/harvest_brew/p-2026/" to Fixtures.detail("5.0", "30"),
                ),
            )
        val beers = Scraper(fetcher).scrapeTapBeers(taps)

        assertEquals(listOf("originalsplus/ikor", "harvest_brew/p-2026"), beers.map { it.id })
        assertEquals(
            Beer(
                "originalsplus/ikor",
                Series.ORIGINALS_PLUS,
                "ikor",
                "Melon Hazy IPA",
                "$base/originalsplus/ikor/",
                "$base/i.jpg",
                null,
                7.5,
                28,
            ),
            beers[0],
        )
        assertEquals(Series.HARVEST_BREW, beers[1].series)
        assertEquals(2, fetcher.fetched.size)

        val again = Scraper(fetcher).scrapeTapBeers(taps, beers.associateBy { it.id })
        assertEquals(beers, again)
        assertEquals(2, fetcher.fetched.size)
    }

    /** FREEDOMS: 1 件も読めなければ, 失敗にする. */
    @Test
    fun freedomsEmpty() {
        val scraper = Scraper(FakeFetcher(mapOf("$base/freedoms/" to "<p>x</p>")))
        assertFailsWith<ScrapeException> { scraper.scrapeFreedoms() }
    }

    /** FREEDOMS: ページ送りが終わらないときは, 上限で失敗にする. */
    @Test
    fun freedomsEndless() {
        val loop = Fixtures.freedoms(listOf(Triple("A", "S", "a")), next = "$base/freedoms/")
        val scraper =
            Scraper(
                FakeFetcher(
                    mapOf(
                        "$base/freedoms/" to loop,
                        "$base/freedoms/a/" to Fixtures.detail(null, null),
                    ),
                ),
            )
        assertFailsWith<ScrapeException> { scraper.scrapeFreedoms() }
    }
}
