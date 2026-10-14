/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.core

import kotlin.test.Test
import kotlin.test.assertEquals

/** [MenuMatcher] のテスト. */
class MenuMatcherTest {
    private val upopo = Tap("upopo", "PILSNER", "originals/upopo")
    private val harvest = Tap("Harvest Brew", "ALE")
    private val short = Tap("ab", "X")
    private val beers =
        listOf(
            Beer("originals/upopo", Series.ORIGINALS, "upopo", "PILSNER", "https://h/u"),
            Beer("freedoms/kagaribi", Series.FREEDOMS, "篝火 Kagaribi", "Alt", "https://h/k"),
        )

    /** 名前が含まれるビールを返す. 全角, 大文字, 空白, 記号の違いは同じ. 同じビールは 1 つ. */
    @Test
    fun find() {
        val text = "ＵＰＯＰＯ ¥750\nHARVEST-brew 700円\n篝火  kagaribi\nab"

        assertEquals(
            listOf("upopo", "Harvest Brew", "篝火 Kagaribi"),
            MenuMatcher.find(text, listOf(upopo, harvest, short), beers).map { it.name },
        )
        assertEquals("originals/upopo", MenuMatcher.find(text, listOf(upopo), beers)[0].beerId)
    }

    /** 見つからなければ空. 短すぎる名前は探さない. */
    @Test
    fun findNothing() {
        assertEquals(emptyList(), MenuMatcher.find("nothing here", listOf(upopo), beers))
        assertEquals(emptyList(), MenuMatcher.find("ab ab ab", listOf(short), emptyList()))
    }
}
