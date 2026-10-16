/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.core

/**
 * ビールのおおよその摂取カロリー.
 *
 * カロリーは, アルコール (7 kcal/g) と, 発酵で残った糖分 (炭水化物. 4 kcal/g) の和で見積もる.
 * 糖分の量は, ビアスタイルごとの目安 (100 ml あたりの g) で決める. 実際のビールの値ではない.
 */
object Calories {
    /** アルコール 1 g のカロリー (kcal). */
    private const val KCAL_PER_ALCOHOL_G = 7.0

    /** 炭水化物 1 g のカロリー (kcal). */
    private const val KCAL_PER_CARB_G = 4.0

    /** エタノールの密度 (g/ml). */
    private const val ETHANOL_DENSITY = 0.8

    /** 百分率の分母. */
    private const val PERCENT = 100.0

    /** 100 ml の単位 (ml). */
    private const val UNIT_ML = 100.0

    /** スタイルが分からないときの, 100 ml あたりの糖分 (g). */
    private const val CARB_DEFAULT = 3.0

    /**
     * ビアスタイルの名前に含まれる語と, 100 ml あたりの糖分 (g) の目安.
     *
     * 上から順に探して, 最初に合った値を使う (大文字と小文字は区別しない).
     */
    private val CARB_BY_KEYWORD: List<Pair<String, Double>> =
        listOf(
            "brut" to 0.5,
            "zero ibu" to 3.5,
            "akira" to 1.5,
            "light" to 1.5,
            "imperial" to 5.0,
            "double" to 4.5,
            "triple" to 5.0,
            "hazy" to 4.0,
            "new england" to 4.0,
            "juicy" to 4.0,
            "neipa" to 4.0,
            "west coast" to 2.5,
            "cold ipa" to 2.0,
            "schwarz" to 3.5,
            "dark" to 4.0,
            "brown" to 4.0,
            "stout" to 4.5,
            "porter" to 4.5,
            "bock" to 4.5,
            "wheat" to 3.5,
            "weizen" to 3.5,
            "wit" to 3.5,
            "roggen" to 3.5,
            "fruit" to 4.5,
            "sour" to 3.0,
            "saison" to 2.0,
            "session" to 2.0,
            "pilsner" to 2.5,
            "dortmunder" to 3.0,
            "helles" to 3.0,
            "lager" to 3.0,
            "ipl" to 2.5,
            "ipa" to 3.0,
            "pale" to 3.0,
            "golden" to 3.0,
        )

    /**
     * 100 ml あたりの糖分 (g) の目安を返す.
     *
     * @param style ビアスタイル (空でもよい)
     * @return 糖分 (g/100 ml)
     */
    fun carbPer100ml(style: String): Double {
        val lower = style.lowercase()

        return CARB_BY_KEYWORD.firstOrNull { lower.contains(it.first) }?.second ?: CARB_DEFAULT
    }

    /**
     * 1 杯のおおよそのカロリーを返す.
     *
     * @param style ビアスタイル
     * @param abv アルコール度数 (%. 分からなければ null. アルコール分は 0 として数える)
     * @param ml 量 (ml)
     * @return カロリー (kcal)
     */
    fun kcal(
        style: String,
        abv: Double?,
        ml: Int,
    ): Double {
        val alcoholG = (abv ?: 0.0) / PERCENT * ml * ETHANOL_DENSITY
        val carbG = carbPer100ml(style) * ml / UNIT_ML

        return alcoholG * KCAL_PER_ALCOHOL_G + carbG * KCAL_PER_CARB_G
    }
}
