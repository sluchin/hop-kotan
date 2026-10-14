/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.core

/**
 * ビールの星の評価 (1 から 5. 評価のないビールは 0).
 *
 * 変更しない. 評価を変えると, 新しい一覧を返す.
 *
 * @property stars 評価 ([key] から星の数)
 */
data class Ratings(
    val stars: Map<String, Int> = emptyMap(),
) {
    /**
     * ビールの評価を返す.
     *
     * @param key [key] で作ったキー
     * @return 星の数 (評価がなければ 0)
     */
    fun of(key: String): Int = stars[key] ?: 0

    /**
     * 評価を変えた一覧を返す.
     *
     * @param key [key] で作ったキー
     * @param value 星の数 (0 から [MAX] の外は, 範囲に収める. 0 なら評価を消す)
     * @return 新しい一覧
     */
    fun set(
        key: String,
        value: Int,
    ): Ratings {
        val clamped = value.coerceIn(0, MAX)

        return Ratings(if (clamped == 0) stars - key else stars + (key to clamped))
    }

    /** 評価のキーと, 星の表示. */
    companion object {
        /** 星の最大の数. */
        const val MAX = 5

        /**
         * ビールの評価のキーを作る (ID があれば ID. なければ名前).
         *
         * @param beerId [Beer.id] (なければ null)
         * @param name ビールの名前
         * @return キー
         */
        fun key(
            beerId: String?,
            name: String,
        ): String = beerId ?: "name:$name"

        /**
         * 星の数を, 星の文字にする.
         *
         * @param value 星の数
         * @return 文字 (`★★★☆☆`. 0 なら空)
         */
        fun text(value: Int): String =
            if (value <= 0) "" else "★".repeat(value) + "☆".repeat(MAX - value)
    }
}
