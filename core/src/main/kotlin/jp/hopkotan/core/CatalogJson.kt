/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.core

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** データの JSON の読み書き (アプリは, 同じ形式を読む). */
object CatalogJson {
    private val json =
        Json {
            prettyPrint = true
            encodeDefaults = true
        }

    /**
     * ビールを JSON にする.
     *
     * @param beers ビール
     * @return JSON (末尾に改行)
     */
    fun encodeBeers(beers: List<Beer>): String = json.encodeToString(beers) + "\n"

    /**
     * JSON からビールを読む.
     *
     * @param text JSON
     * @return ビール
     */
    fun decodeBeers(text: String): List<Beer> = json.decodeFromString(text)

    /**
     * タップリストを JSON にする.
     *
     * @param list タップリスト
     * @return JSON (末尾に改行)
     */
    fun encodeTaplist(list: TapList): String = json.encodeToString(list) + "\n"

    /**
     * JSON からタップリストを読む.
     *
     * @param text JSON
     * @return タップリスト
     */
    fun decodeTaplist(text: String): TapList = json.decodeFromString(text)

    /**
     * 飲んだビールの記録を JSON にする.
     *
     * @param log 記録
     * @return JSON (末尾に改行)
     */
    fun encodeDrinks(log: DrinkLog): String = json.encodeToString(log.records) + "\n"

    /**
     * JSON から飲んだビールの記録を読む.
     *
     * @param text JSON
     * @return 記録
     */
    fun decodeDrinks(text: String): DrinkLog = DrinkLog(json.decodeFromString(text))
}
