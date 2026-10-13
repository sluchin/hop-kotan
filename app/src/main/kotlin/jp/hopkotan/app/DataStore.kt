/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.app

import android.content.Context
import jp.hopkotan.core.Beer
import jp.hopkotan.core.CatalogJob
import jp.hopkotan.core.CatalogJson
import jp.hopkotan.core.DrinkLog
import jp.hopkotan.core.TapList
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** 接続と読み込みの待ち時間 (ミリ秒). */
private const val TIMEOUT_MS = 15_000

/** 飲んだ記録のファイルの名前. */
private const val DRINKS_FILE = "drinks.json"

/**
 * ビールのデータ (GitHub の JSON) と, 飲んだ記録を, 端末に保存する.
 *
 * ダウンロードしたデータは, 端末に残す (オフラインでも表示するため). 飲んだ記録は, 端末の中だけに
 * 保存し, どこにも送らない.
 *
 * @property context コンテキスト
 */
class DataStore(
    private val context: Context,
) {
    /**
     * 端末に残してあるビールを読む.
     *
     * @return ビール (なければ, 空)
     */
    fun loadBeers(): List<Beer> =
        read(CatalogJob.BEERS_FILE)
            ?.let {
                runCatching { CatalogJson.decodeBeers(it) }.getOrNull()
            }.orEmpty()

    /**
     * 端末に残してあるタップリストを読む.
     *
     * @return タップリスト (なければ null)
     */
    fun loadTaplist(): TapList? =
        read(CatalogJob.TAPLIST_FILE)?.let {
            runCatching { CatalogJson.decodeTaplist(it) }.getOrNull()
        }

    /**
     * 飲んだ記録を読む.
     *
     * @return 記録 (なければ, 空)
     */
    fun loadDrinks(): DrinkLog =
        read(DRINKS_FILE)?.let { runCatching { CatalogJson.decodeDrinks(it) }.getOrNull() }
            ?: DrinkLog()

    /**
     * 飲んだ記録を保存する (書き込みの途中で終わっても, 前の記録が残るように, 別のファイルに書いてから置き換える).
     *
     * @param log 記録
     */
    fun saveDrinks(log: DrinkLog) = write(DRINKS_FILE, CatalogJson.encodeDrinks(log))

    /**
     * 最新のデータをダウンロードして, 端末に保存する.
     *
     * @return 取得したビールとタップリスト
     * @throws IOException 取得できなかったとき
     * @throws IllegalArgumentException JSON が読めなかったとき
     */
    fun download(): Pair<List<Beer>, TapList> {
        val beersText = fetch(CatalogJob.BEERS_FILE)
        val taplistText = fetch(CatalogJob.TAPLIST_FILE)
        val beers = CatalogJson.decodeBeers(beersText)
        val taplist = CatalogJson.decodeTaplist(taplistText)
        write(CatalogJob.BEERS_FILE, beersText)
        write(CatalogJob.TAPLIST_FILE, taplistText)

        return beers to taplist
    }

    private fun fetch(name: String): String {
        val connection = URL(BuildConfig.DATA_URL + name).openConnection() as HttpURLConnection
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS

        return try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("HTTP ${connection.responseCode} for $name")
            }
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun read(name: String): String? =
        File(context.filesDir, name)
            .takeIf {
                it.exists()
            }?.readText(Charsets.UTF_8)

    private fun write(
        name: String,
        text: String,
    ) {
        val file = File(context.filesDir, name)
        val temp = File(context.filesDir, "$name.tmp")
        temp.writeText(text, Charsets.UTF_8)
        if (!temp.renameTo(file)) {
            throw IOException("Failed to replace $name")
        }
    }
}
