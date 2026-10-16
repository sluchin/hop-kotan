/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.core

import kotlin.test.Test
import kotlin.test.assertEquals

/** [DrinkLog] のテスト. */
class DrinkLogTest {
    private val sizeM = PriceSize("M", 310, 750)
    private val sizeL = PriceSize("L", 420, 900)
    private val tap = Tap("Test", "IPA", "freedoms/test")

    /** タップとサイズから, 記録を作る. */
    @Test
    fun record() {
        val record = DrinkLog.record("1", "2026-10-13", tap, sizeM, 5.0)

        assertEquals(
            DrinkRecord("1", "2026-10-13", "Test", "IPA", "freedoms/test", "M", 310, 750, 5.0),
            record,
        )
    }

    /** 足す, 消す, 日ごとに取り出す. 元の一覧は変わらない. */
    @Test
    fun addAndRemove() {
        val empty = DrinkLog()
        val a = DrinkLog.record("1", "2026-10-13", tap, sizeM, 5.0)
        val b = DrinkLog.record("2", "2026-10-12", tap, sizeL, null)
        val log = empty.add(a).add(b)

        assertEquals(0, empty.records.size)
        assertEquals(listOf(a), log.forDate("2026-10-13"))
        assertEquals(listOf("2026-10-13", "2026-10-12"), log.dates())
        assertEquals(listOf(b), log.remove("1").records)
        assertEquals(log, log.remove("none"))
    }

    /** 1 日の集計: 杯数, 量, 金額, 純アルコール (ABV が分からない記録は含めない). */
    @Test
    fun summary() {
        val log =
            DrinkLog()
                .add(DrinkLog.record("1", "2026-10-13", tap, sizeM, 5.0))
                .add(DrinkLog.record("2", "2026-10-13", tap, sizeL, null))
        val summary = log.summary("2026-10-13")

        assertEquals(2, summary.count)
        assertEquals(730, summary.ml)
        assertEquals(1650, summary.yen)
        assertEquals(310 * 0.05 * 0.8, summary.alcoholG, 1e-9)
        assertEquals(
            Calories.kcal("IPA", 5.0, 310) + Calories.kcal("IPA", null, 420),
            summary.kcal,
            1e-9,
        )
        assertEquals(DaySummary("2026-10-14", 0, 0, 0, 0.0, 0.0), log.summary("2026-10-14"))
    }
}
