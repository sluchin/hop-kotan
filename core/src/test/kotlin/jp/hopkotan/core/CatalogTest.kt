/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.core

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** [CatalogJson] と [CatalogJob] のテスト. */
class CatalogTest {
    private val base = "https://hopkotan.com"
    private val beer =
        Beer(
            "freedoms/a",
            Series.FREEDOMS,
            "篝火",
            "Alt",
            "$base/freedoms/a/",
            null,
            "2026-09",
            5.0,
            50,
        )
    private val list =
        TapList("2026-10-13", listOf(PriceSize("M", 310, 750)), listOf(Tap("a", "s")))

    /** ビール, タップリスト, 記録は, JSON にして読み戻せる. 日本語は, そのまま書く. */
    @Test
    fun jsonRoundTrip() {
        val record =
            DrinkLog
                .record(
                    "1",
                    "2026-10-13",
                    Tap("a", "s", "freedoms/a"),
                    PriceSize("M", 310, 750),
                    5.0,
                ).copy(time = "20:15")

        assertEquals(listOf(beer), CatalogJson.decodeBeers(CatalogJson.encodeBeers(listOf(beer))))
        assertTrue(CatalogJson.encodeBeers(listOf(beer)).contains("篝火"))
        assertEquals(list, CatalogJson.decodeTaplist(CatalogJson.encodeTaplist(list)))
        assertEquals(
            DrinkLog(listOf(record)),
            CatalogJson.decodeDrinks(CatalogJson.encodeDrinks(DrinkLog(listOf(record)))),
        )
    }

    /** タップリストを書く. ディレクトリがなければ作る. */
    @Test
    fun updateTaplist() {
        val dir = Files.createTempDirectory("hop").resolve("data")
        val job = CatalogJob(Scraper { Fixtures.taproom() }, dir)

        assertEquals(3, job.updateTaplist())
        assertEquals(
            "2026-10-13",
            CatalogJson.decodeTaplist(Files.readString(dir.resolve(CatalogJob.TAPLIST_FILE))).date,
        )
    }

    /** タップリストは, 日ごとの履歴にも書く. 目次は, 新しい日が先で, 同じ日は 1 つにする. */
    @Test
    fun updateTaplistKeepsHistory() {
        val dir = Files.createTempDirectory("hop")
        val old = "${CatalogJob.HISTORY_DIR}/${CatalogJob.INDEX_FILE}"
        Files.createDirectories(dir.resolve(CatalogJob.HISTORY_DIR))
        Files.writeString(dir.resolve(old), CatalogJson.encodeDates(listOf("2026-10-01")))
        val job = CatalogJob(Scraper { Fixtures.taproom() }, dir)

        job.updateTaplist()
        job.updateTaplist()

        assertEquals(
            listOf("2026-10-13", "2026-10-01"),
            CatalogJson.decodeDates(Files.readString(dir.resolve(old))),
        )
        assertEquals(
            Files.readString(dir.resolve(CatalogJob.TAPLIST_FILE)),
            Files.readString(dir.resolve("${CatalogJob.HISTORY_DIR}/2026-10-13.json")),
        )
    }

    /** ビールを書く. 書いてあるビールは, 次の実行で取り直さない. タップに出た ORIGINALS plus は, 外れても残す. */
    @Test
    fun updateBeers() {
        val dir = Files.createTempDirectory("hop")
        val fetched = mutableListOf<String>()
        var withPlus = true
        val plusTap =
            """<div class="taproom-taplist__item"><div class="taproom-taplist__thum">
            <a href="/originalsplus/ikor/"><img src="/img/ikor.jpg"></a></div>
            <div class="taproom-taplist__name">ikor</div><div class="taproom-taplist__style">Hazy</div></div>"""
        val marker = "<div class=\"taproom-taplist\">"
        val scraper =
            Scraper { url ->
                fetched += url
                when {
                    url == "$base/taproom/" ->
                        Fixtures.taproom().let {
                            if (withPlus) it.replace(marker, marker + plusTap) else it
                        }
                    url == "$base/originals/" -> Fixtures.originals
                    url == "$base/freedoms/" -> Fixtures.freedoms(listOf(Triple("A", "S", "a")))
                    else -> Fixtures.detail("5.0", "20")
                }
            }
        val job = CatalogJob(scraper, dir)

        assertEquals(5, job.updateBeers())
        val first = fetched.count { it == "$base/freedoms/a/" }
        withPlus = false
        assertEquals(5, job.updateBeers())

        assertEquals(1, first)
        assertEquals(1, fetched.count { it == "$base/freedoms/a/" })
        assertEquals(1, fetched.count { it == "$base/originalsplus/ikor/" })
        val saved = CatalogJson.decodeBeers(Files.readString(dir.resolve(CatalogJob.BEERS_FILE)))
        assertEquals(5, saved.size)
        assertEquals(Series.ORIGINALS_PLUS, saved.last().series)
    }
}
