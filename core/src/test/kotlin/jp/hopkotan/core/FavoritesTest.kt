/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** [Favorites] のテスト. */
class FavoritesTest {
    private val upopo = Beer("originals/upopo", Series.ORIGINALS, "upopo", "PILSNER", "u")
    private val ikor = Beer("freedoms/ikor", Series.FREEDOMS, "ikor", "HAZY IPA", "i")

    /** 切り替えると, 足した順に足し, もう一度で外す. */
    @Test
    fun toggle() {
        val favorites = Favorites().toggle(upopo.id).toggle(ikor.id)

        assertEquals(Favorites(listOf(upopo.id, ikor.id)), favorites)
        assertTrue(upopo.id in favorites)
        assertEquals(Favorites(listOf(ikor.id)), favorites.toggle(upopo.id))
        assertFalse(upopo.id in favorites.toggle(upopo.id))
    }

    /** お気に入りのビールを, 新しく足した順に返し, 一覧にないビールは除く. */
    @Test
    fun beersIn() {
        val favorites = Favorites(listOf(upopo.id, "gone", ikor.id))

        assertEquals(listOf(ikor, upopo), favorites.beersIn(listOf(upopo, ikor)))
        assertEquals(emptyList(), Favorites().beersIn(listOf(upopo)))
    }

    /** JSON にして, 読み戻せる. */
    @Test
    fun json() {
        val favorites = Favorites(listOf(upopo.id, "name:Guest Ale"))

        assertEquals(favorites, Favorites.fromJson(favorites.toJson()))
        assertTrue(favorites.toJson().endsWith("\n"))
    }
}
