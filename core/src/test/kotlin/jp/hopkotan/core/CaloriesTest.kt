/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** [Calories] のテスト. */
class CaloriesTest {
    /** 糖分の目安: 語が合ったスタイル, 大文字と小文字, 合わないスタイル, 空. */
    @Test
    fun carbPer100ml() {
        assertEquals(0.5, Calories.carbPer100ml("Brut Beer"), 1e-9)
        assertEquals(4.0, Calories.carbPer100ml("HAZY IPA"), 1e-9)
        assertEquals(5.0, Calories.carbPer100ml("Imperial Pilsner"), 1e-9)
        assertEquals(3.0, Calories.carbPer100ml("American IPA"), 1e-9)
        assertEquals(3.0, Calories.carbPer100ml("Unknown"), 1e-9)
        assertEquals(3.0, Calories.carbPer100ml(""), 1e-9)
    }

    /** 1 杯のカロリー: アルコール 7 kcal/g と糖分 4 kcal/g の和. ABV なしは, アルコール 0. */
    @Test
    fun kcal() {
        // 5% の 500 ml: アルコール 20 g (140 kcal), 糖分 3 g/100ml * 5 = 15 g (60 kcal)
        assertEquals(200.0, Calories.kcal("American IPA", 5.0, 500), 1e-9)
        assertEquals(60.0, Calories.kcal("American IPA", null, 500), 1e-9)
        assertEquals(0.0, Calories.kcal("American IPA", 5.0, 0), 1e-9)
    }

    /** 同じ ABV と量でも, 濃いスタイルの方が高い. 強いビールほど高い. */
    @Test
    fun kcalByStyle() {
        assertTrue(Calories.kcal("Hazy IPA", 6.0, 300) > Calories.kcal("Brut Beer", 6.0, 300))
        assertTrue(Calories.kcal("Pilsner", 8.0, 300) > Calories.kcal("Pilsner", 4.0, 300))
    }
}
