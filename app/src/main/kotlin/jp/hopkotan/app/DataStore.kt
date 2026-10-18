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
import jp.hopkotan.core.Ratings
import jp.hopkotan.core.TapList
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** ログのタグ. */
private const val TAG = "Data"

/** 接続と読み込みの待ち時間 (ミリ秒). */
private const val TIMEOUT_MS = 15_000

/** 履歴を取得する日数 (最近の何日分か). */
private const val RECENT_DAYS = 14

/** 星の評価のファイルの名前. */
private const val RATINGS_FILE = "ratings.json"

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
        context
            .readLocal(CatalogJob.BEERS_FILE)
            ?.let {
                runCatching { CatalogJson.decodeBeers(it) }
                    .onFailure { AppLog.w(TAG, "Could not read the saved beers", it) }
                    .getOrNull()
            }.orEmpty()

    /**
     * 端末に残してあるタップリストを読む.
     *
     * @return タップリスト (なければ null)
     */
    fun loadTaplist(): TapList? =
        context.readLocal(CatalogJob.TAPLIST_FILE)?.let {
            runCatching { CatalogJson.decodeTaplist(it) }
                .onFailure { AppLog.w(TAG, "Could not read the saved taplist", it) }
                .getOrNull()
        }

    /**
     * 端末に残してある, 日ごとのタップリスト (履歴) を読む.
     *
     * @return タップリスト (新しい日が先)
     */
    fun loadHistory(): List<TapList> =
        File(context.filesDir, CatalogJob.HISTORY_DIR)
            .listFiles { file -> file.name != CatalogJob.INDEX_FILE && file.extension == "json" }
            .orEmpty()
            .mapNotNull { file ->
                runCatching { CatalogJson.decodeTaplist(file.readText(Charsets.UTF_8)) }
                    .onFailure {
                        AppLog.w(
                            TAG,
                            "Could not read the saved history ${file.name}",
                            it,
                        )
                    }.getOrNull()
            }.sortedByDescending { it.date }

    /**
     * 飲んだ記録を読む.
     *
     * @return 記録 (なければ, 空)
     */
    fun loadDrinks(): DrinkLog =
        context
            .readLocal(
                DRINKS_FILE,
            )?.let {
                runCatching { CatalogJson.decodeDrinks(it) }
                    .onFailure { AppLog.w(TAG, "Could not read the saved drinks", it) }
                    .getOrNull()
            }
            ?: DrinkLog()

    /**
     * 飲んだ記録を保存する (書き込みの途中で終わっても, 前の記録が残るように, 別のファイルに書いてから置き換える).
     *
     * @param log 記録
     */
    fun saveDrinks(log: DrinkLog) = context.writeLocal(DRINKS_FILE, CatalogJson.encodeDrinks(log))

    /**
     * 星の評価を読む.
     *
     * @return 評価 (なければ, 空)
     */
    fun loadRatings(): Ratings =
        context
            .readLocal(
                RATINGS_FILE,
            )?.let {
                runCatching { CatalogJson.decodeRatings(it) }
                    .onFailure { AppLog.w(TAG, "Could not read the saved ratings", it) }
                    .getOrNull()
            }
            ?: Ratings()

    /**
     * 星の評価を保存する.
     *
     * @param ratings 評価
     */
    fun saveRatings(ratings: Ratings) =
        context.writeLocal(RATINGS_FILE, CatalogJson.encodeRatings(ratings))

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
        context.writeLocal(CatalogJob.BEERS_FILE, beersText)
        context.writeLocal(CatalogJob.TAPLIST_FILE, taplistText)

        return beers to taplist
    }

    /**
     * 最近の日のタップリスト (履歴) をダウンロードして, 端末に保存する.
     *
     * 過去の日は変わらないので, 端末にある日は取り直さない (最新の日だけ, 毎回取り直す).
     *
     * @return 端末にある履歴 (新しい日が先)
     * @throws IOException 目次が取得できなかったとき
     */
    fun downloadHistory(): List<TapList> {
        val dir = CatalogJob.HISTORY_DIR
        val dates =
            CatalogJson.decodeDates(fetch("$dir/${CatalogJob.INDEX_FILE}")).take(RECENT_DAYS)
        dates.forEachIndexed { index, date ->
            val name = "$dir/$date.json"
            if (index == 0 || !File(context.filesDir, name).exists()) {
                val text = fetch(name)
                CatalogJson.decodeTaplist(text)
                context.writeLocal(name, text)
            }
        }

        return loadHistory()
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
}
