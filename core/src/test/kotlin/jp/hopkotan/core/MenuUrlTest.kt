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

    /** https のページは, どのホストでも, アプリの中で開く. それ以外は, アプリの外. */
    @Test
    fun opensInApp() {
        assertTrue(MenuUrl.opensInApp("https://order.example.com/cart"))
        assertTrue(MenuUrl.opensInApp("https://login.example.org/auth"))
        assertFalse(MenuUrl.opensInApp("http://order.example.com/"))
        assertFalse(MenuUrl.opensInApp("intent://x#Intent;scheme=app;end"))
        assertFalse(MenuUrl.opensInApp("tel:0123"))
    }

    /** 表示するホスト名. */
    @Test
    fun host() {
        assertEquals("order.example.com", MenuUrl.host(base))
    }
}
