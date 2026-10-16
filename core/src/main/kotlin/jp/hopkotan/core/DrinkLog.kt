/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.core

import kotlinx.serialization.Serializable

/**
 * 飲んだビールの 1 件の記録.
 *
 * タップリストは毎日変わるので, 名前とスタイルと価格は, 記録のときの値をそのまま持つ.
 *
 * @property id 記録の ID (アプリが付ける. 削除に使う)
 * @property date 飲んだ日 (`2026-10-13`)
 * @property beerName ビールの名前
 * @property style ビアスタイル
 * @property beerId [Beer.id] (定番と FREEDOMS のビールでなければ null)
 * @property sizeLabel 量の呼び名 (M, L)
 * @property ml 量 (ml)
 * @property yen 価格 (円)
 * @property abv アルコール度数 (%. 分からなければ null)
 * @property note メモ
 * @property time 飲んだ時刻 (`20:15`. [DrinkLog.record] は null で作るので, 呼び出し側が付ける. 昔の記録は null)
 */
@Serializable
data class DrinkRecord(
    val id: String,
    val date: String,
    val beerName: String,
    val style: String,
    val beerId: String? = null,
    val sizeLabel: String,
    val ml: Int,
    val yen: Int,
    val abv: Double? = null,
    val note: String = "",
    val time: String? = null,
)

/**
 * 1 日の集計.
 *
 * @property date 日 (`2026-10-13`)
 * @property count 杯数
 * @property ml 量の合計 (ml)
 * @property yen 価格の合計 (円)
 * @property alcoholG 純アルコールの量 (g. ABV が分からない記録は, 含めない)
 * @property kcal おおよその摂取カロリー (kcal. スタイルごとの目安. [Calories])
 */
data class DaySummary(
    val date: String,
    val count: Int,
    val ml: Int,
    val yen: Int,
    val alcoholG: Double,
    val kcal: Double,
)

/**
 * 飲んだビールの記録の一覧 (変更しない. 追加や削除は, 新しい一覧を返す).
 *
 * @property records 記録 (追加した順)
 */
data class DrinkLog(
    val records: List<DrinkRecord> = emptyList(),
) {
    /**
     * 記録を足した一覧を返す.
     *
     * @param record 足す記録
     * @return 新しい一覧
     */
    fun add(record: DrinkRecord): DrinkLog = DrinkLog(records + record)

    /**
     * 記録を消した一覧を返す.
     *
     * @param id 消す記録の ID (なければ, 変わらない)
     * @return 新しい一覧
     */
    fun remove(id: String): DrinkLog = DrinkLog(records.filter { it.id != id })

    /**
     * 記録を差し替えた一覧を返す (同じ ID の記録を, 同じ位置で入れ替える).
     *
     * @param record 新しい記録 (ID が同じ記録を置き換える)
     * @return 新しい一覧 (同じ ID の記録がなければ, 変わらない)
     */
    fun update(record: DrinkRecord): DrinkLog =
        DrinkLog(records.map { if (it.id == record.id) record else it })

    /**
     * 1 日の記録を返す.
     *
     * @param date 日 (`2026-10-13`)
     * @return その日の記録 (追加した順)
     */
    fun forDate(date: String): List<DrinkRecord> = records.filter { it.date == date }

    /**
     * 記録のある日を返す.
     *
     * @return 日 (新しい順)
     */
    fun dates(): List<String> = records.map { it.date }.distinct().sortedDescending()

    /**
     * 1 日の集計を返す.
     *
     * @param date 日 (`2026-10-13`)
     * @return 集計 (記録がなければ, 全て 0)
     */
    fun summary(date: String): DaySummary {
        val day = forDate(date)
        val alcohol = day.sumOf { alcoholGrams(it) }
        val kcal = day.sumOf { Calories.kcal(it.style, it.abv, it.ml) }

        return DaySummary(date, day.size, day.sumOf { it.ml }, day.sumOf { it.yen }, alcohol, kcal)
    }

    /** 記録の作り方と, 計算の定数. */
    companion object {
        /** エタノールの密度 (g/ml). */
        private const val ETHANOL_DENSITY = 0.8

        /** 百分率の分母. */
        private const val PERCENT = 100.0

        /**
         * タップリストのビールから, 記録を作る.
         *
         * @param id 記録の ID
         * @param date 飲んだ日 (`2026-10-13`)
         * @param tap 飲んだビール
         * @param size 量と価格
         * @param abv アルコール度数 (%. 分からなければ null)
         * @return 記録
         */
        fun record(
            id: String,
            date: String,
            tap: Tap,
            size: PriceSize,
            abv: Double?,
        ): DrinkRecord =
            DrinkRecord(
                id = id,
                date = date,
                beerName = tap.name,
                style = tap.style,
                beerId = tap.beerId,
                sizeLabel = size.label,
                ml = size.ml,
                yen = size.yen,
                abv = abv,
            )

        private fun alcoholGrams(record: DrinkRecord): Double =
            (record.abv ?: 0.0) / PERCENT * record.ml * ETHANOL_DENSITY
    }
}
