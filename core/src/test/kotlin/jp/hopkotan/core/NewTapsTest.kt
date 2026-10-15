/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** [NewTaps] のテスト. */
class NewTapsTest {
    private fun tap(
        name: String,
        beerId: String? = null,
    ) = Tap(name = name, style = "IPA", beerId = beerId)

    private fun list(
        date: String,
        vararg taps: Tap,
    ) = TapList(date = date, sizes = emptyList(), taps = taps.toList())

    /** 前の日になかったビールだけを, 今日の並びで返す. */
    @Test
    fun findReturnsOnlyNewTaps() {
        val previous = list("2026-10-13", tap("a", "originals/a"), tap("b", "originals/b"))
        val current =
            list(
                "2026-10-14",
                tap("c", "freedoms/c"),
                tap("a", "originals/a"),
                tap("d", "freedoms/d"),
            )

        assertEquals(listOf("c", "d"), NewTaps.find(previous, current).map { it.name })
    }

    /** ID がないビールは名前で比べる (ID があるビールと名前が同じでも, 別のものとして扱う). */
    @Test
    fun findUsesNameWhenThereIsNoId() {
        val previous = list("2026-10-13", tap("Harvest Brew"))
        val current =
            list("2026-10-14", tap("Harvest Brew"), tap("Other"), tap("Harvest Brew", "x/y"))

        assertEquals(
            listOf("Other", "Harvest Brew"),
            NewTaps.find(previous, current).map { it.name },
        )
    }

    /** 変わりがなければ, 空. */
    @Test
    fun findReturnsEmptyWhenNothingIsNew() {
        val same = list("2026-10-14", tap("a", "originals/a"))

        assertEquals(emptyList(), NewTaps.find(list("2026-10-13", tap("a", "originals/a")), same))
    }

    /** 今日より前で一番新しい日を選ぶ (今日と未来の日は選ばない). */
    @Test
    fun previousOfPicksTheLatestEarlierDate() {
        val current = list("2026-10-14")
        val history =
            listOf(list("2026-10-12"), list("2026-10-14"), list("2026-10-13"), list("2026-10-15"))

        assertEquals("2026-10-13", NewTaps.previousOf(history, current)?.date)
    }

    /** 前の日がなければ, null. */
    @Test
    fun previousOfReturnsNullWithoutEarlierDate() {
        assertNull(NewTaps.previousOf(listOf(list("2026-10-14")), list("2026-10-14")))
        assertNull(NewTaps.previousOf(emptyList(), list("2026-10-14")))
    }
}
