/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** [BeerIds] のテスト. */
class BeerIdsTest {
    /** FREEDOMS の URL から, スラッグを使った ID を作る. */
    @Test
    fun freedomsSlug() {
        assertEquals(
            "freedoms/test-beer",
            BeerIds.fromUrl("https://hopkotan.com/freedoms/test-beer/"),
        )
    }

    /** 百分率符号化のスラッグは, 元に戻す. */
    @Test
    fun freedomsPercentEncoded() {
        assertEquals(
            "freedoms/篝火",
            BeerIds.fromUrl("https://hopkotan.com/freedoms/%e7%af%9d%e7%81%ab/"),
        )
    }

    /** ORIGINALS の詳細の URL と, アンカーの URL (前後に空白がある) から, 同じ ID を作る. */
    @Test
    fun originalsPathAndFragment() {
        assertEquals("originals/alpha", BeerIds.fromUrl("https://hopkotan.com/originals/alpha/"))
        assertEquals("originals/alpha", BeerIds.fromUrl("https://hopkotan.com/originals/#alpha "))
    }

    /** ビールの URL でないとき, スラッグがないとき, URL として読めないときは, null. */
    @Test
    fun notBeerUrls() {
        assertNull(BeerIds.fromUrl("https://hopkotan.com/news/x/"))
        assertNull(BeerIds.fromUrl("https://hopkotan.com/freedoms/"))
        assertNull(BeerIds.fromUrl("https://hopkotan.com/originals/"))
        assertNull(BeerIds.fromUrl("https://hopkotan.com/originals/#"))
        assertNull(BeerIds.fromUrl("mailto:someone@example.com"))
        assertNull(BeerIds.fromUrl("http://exa mple.com/freedoms/x/"))
    }
}
