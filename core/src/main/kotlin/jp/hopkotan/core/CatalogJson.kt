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
     * 日付の一覧 (履歴の目次) を JSON にする.
     *
     * @param dates 日付 (`2026-10-13`)
     * @return JSON (末尾に改行)
     */
    fun encodeDates(dates: List<String>): String = json.encodeToString(dates) + "\n"

    /**
     * JSON から日付の一覧を読む.
     *
     * @param text JSON
     * @return 日付
     */
    fun decodeDates(text: String): List<String> = json.decodeFromString(text)

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

    /**
     * 星の評価を JSON にする.
     *
     * @param ratings 評価
     * @return JSON (末尾に改行)
     */
    fun encodeRatings(ratings: Ratings): String = json.encodeToString(ratings.stars) + "\n"

    /**
     * JSON から星の評価を読む.
     *
     * @param text JSON
     * @return 評価
     */
    fun decodeRatings(text: String): Ratings = Ratings(json.decodeFromString(text))
}
