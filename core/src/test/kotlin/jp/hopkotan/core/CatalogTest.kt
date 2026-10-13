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
            DrinkLog.record(
                "1",
                "2026-10-13",
                Tap("a", "s", "freedoms/a"),
                PriceSize("M", 310, 750),
                5.0,
            )

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

    /** ビールを書く. 書いてあるビールは, 次の実行で取り直さない. */
    @Test
    fun updateBeers() {
        val dir = Files.createTempDirectory("hop")
        val fetched = mutableListOf<String>()
        val scraper =
            Scraper { url ->
                fetched += url
                when {
                    url == "$base/originals/" -> Fixtures.originals
                    url == "$base/freedoms/" -> Fixtures.freedoms(listOf(Triple("A", "S", "a")))
                    else -> Fixtures.detail("5.0", "20")
                }
            }
        val job = CatalogJob(scraper, dir)

        assertEquals(4, job.updateBeers())
        val first = fetched.count { it == "$base/freedoms/a/" }
        assertEquals(4, job.updateBeers())

        assertEquals(1, first)
        assertEquals(1, fetched.count { it == "$base/freedoms/a/" })
        assertEquals(
            4,
            CatalogJson.decodeBeers(Files.readString(dir.resolve(CatalogJob.BEERS_FILE))).size,
        )
    }
}
