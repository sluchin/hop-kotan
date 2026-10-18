/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.core

import kotlin.test.Test
import kotlin.test.assertEquals

/** [OpenedTaps] のテスト. */
class OpenedTapsTest {
    private val upopo = Tap("upopo", "PILSNER", "originals/upopo")
    private val ikor = Tap("ikor", "HAZY IPA", "freedoms/ikor")
    private val guest = Tap("Guest Ale", "ALE")
    private val list = TapList("2026-10-18", emptyList(), listOf(upopo))

    /** タップリストにないビールを足し, タップリストか, もう足したビールは, 足さない. */
    @Test
    fun add() {
        val opened = OpenedTaps().add(list, ikor).add(list, guest)

        assertEquals(OpenedTaps("2026-10-18", listOf(ikor, guest)), opened)
        assertEquals(opened, opened.add(list, upopo))
        assertEquals(opened, opened.add(list, ikor.copy(name = "IKOR")))
        assertEquals(opened, opened.add(list, Tap("Guest Ale", "IPA")))
    }

    /** 日付の違うタップリストに足すと, 前の日付の分を捨てる. */
    @Test
    fun addToNewDate() {
        val old = OpenedTaps("2026-10-17", listOf(guest))

        assertEquals(OpenedTaps("2026-10-18", listOf(ikor)), old.add(list, ikor))
        assertEquals(OpenedTaps("2026-10-18"), old.add(list, upopo))
    }

    /** 日付が同じタップリストにだけ, 重ならないビールを足して見せる. */
    @Test
    fun applyTo() {
        val opened = OpenedTaps("2026-10-18", listOf(ikor, upopo))

        assertEquals(listOf(upopo, ikor), opened.applyTo(list).taps)
        assertEquals(list, OpenedTaps("2026-10-17", listOf(ikor)).applyTo(list))
    }

    /** JSON に書いて読むと, 同じになる. 空の JSON は, 空の一覧. */
    @Test
    fun json() {
        val opened = OpenedTaps("2026-10-18", listOf(ikor, guest))

        assertEquals(opened, OpenedTaps.fromJson(opened.toJson()))
        assertEquals(OpenedTaps(), OpenedTaps.fromJson("{}"))
    }
}
