/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** [MenuUrl] のテスト. */
class MenuUrlTest {
    private val base = "https://order.example.com/menu?t=1"

    /** https で, ホスト名があるものだけを認める (前後の空白は除く). */
    @Test
    fun parse() {
        assertEquals(base, MenuUrl.parse("  $base \n"))
        assertEquals("HTTPS://order.example.com/", MenuUrl.parse("HTTPS://order.example.com/"))
        assertNull(MenuUrl.parse(null))
        assertNull(MenuUrl.parse(""))
        assertNull(MenuUrl.parse("http://order.example.com/"))
        assertNull(MenuUrl.parse("javascript:alert(1)"))
        assertNull(MenuUrl.parse("https:///path"))
        assertNull(MenuUrl.parse("https://user:pass@order.example.com/"))
        assertNull(MenuUrl.parse("https://bad host/"))
        assertNull(MenuUrl.parse("hello world"))
    }

    /** 同じホストと, そのサブドメインは中. 別のホストや, 似た名前や, 認められない URL は外. */
    @Test
    fun isInside() {
        assertTrue(MenuUrl.isInside("https://order.example.com/cart", base))
        assertTrue(MenuUrl.isInside("https://ORDER.example.com/cart", base))
        assertTrue(MenuUrl.isInside("https://img.order.example.com/a.png", base))
        assertFalse(MenuUrl.isInside("https://pay.example.com/", base))
        assertFalse(MenuUrl.isInside("https://evilorder.example.com/", base))
        assertFalse(MenuUrl.isInside("http://order.example.com/", base))
        assertFalse(MenuUrl.isInside("intent://x", base))
    }

    /** 表示するホスト名. */
    @Test
    fun host() {
        assertEquals("order.example.com", MenuUrl.host(base))
    }
}
