/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.core

import kotlin.test.Test
import kotlin.test.assertEquals

/** [Ratings] のテスト. */
class RatingsTest {
    /** 評価を付けると読める. 範囲の外は収め, 0 で消す. 評価のないビールは 0. */
    @Test
    fun setAndGet() {
        val ratings =
            Ratings()
                .set("a", 4)
                .set("b", 9)
                .set("c", 2)
                .set("c", -1)

        assertEquals(4, ratings.of("a"))
        assertEquals(Ratings.MAX, ratings.of("b"))
        assertEquals(0, ratings.of("c"))
        assertEquals(0, ratings.of("none"))
        assertEquals(setOf("a", "b"), ratings.stars.keys)
    }

    /** キーは ID か名前. 星の文字は, 0 なら空. */
    @Test
    fun keyAndText() {
        assertEquals("freedoms/a", Ratings.key("freedoms/a", "A"))
        assertEquals("name:A", Ratings.key(null, "A"))
        assertEquals("", Ratings.text(0))
        assertEquals("★★★☆☆", Ratings.text(3))
        assertEquals("★★★★★", Ratings.text(5))
    }

    /** JSON にして読み戻せる. */
    @Test
    fun jsonRoundTrip() {
        val ratings = Ratings().set("a", 3).set("name:ビール", 5)

        assertEquals(ratings, CatalogJson.decodeRatings(CatalogJson.encodeRatings(ratings)))
    }
}
