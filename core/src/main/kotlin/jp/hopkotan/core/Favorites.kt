/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * お気に入りのビール.
 *
 * ビールは, 評価と同じキー ([Ratings.key]. ID があれば ID, なければ名前) で持つ. 変更しない. 足したり
 * 外したりすると, 新しい一覧を返す.
 *
 * @property keys お気に入りのビールのキー (足した順)
 */
@Serializable
data class Favorites(
    val keys: List<String> = emptyList(),
) {
    /**
     * お気に入りか.
     *
     * @param key [Ratings.key] で作ったキー
     * @return お気に入りなら true
     */
    operator fun contains(key: String): Boolean = key in keys

    /**
     * お気に入りを切り替える (お気に入りなら外し, そうでなければ, 最後に足す).
     *
     * @param key [Ratings.key] で作ったキー
     * @return 新しい一覧
     */
    fun toggle(key: String): Favorites = Favorites(if (key in keys) keys - key else keys + key)

    /**
     * ビールのうち, お気に入りのものを, 足した順 (新しいものが先) に返す.
     *
     * @param beers 全てのビール
     * @return お気に入りのビール (一覧にないビールは, 除く)
     */
    fun beersIn(beers: List<Beer>): List<Beer> {
        val byKey = beers.associateBy { Ratings.key(it.id, it.name) }

        return keys.asReversed().mapNotNull { byKey[it] }
    }

    /**
     * JSON にする.
     *
     * @return JSON (末尾に改行)
     */
    fun toJson(): String = json.encodeToString(this) + "\n"

    /** JSON の読み書き. */
    companion object {
        private val json =
            Json {
                prettyPrint = true
                encodeDefaults = true
            }

        /**
         * JSON から読む.
         *
         * @param text JSON
         * @return お気に入り
         */
        fun fromJson(text: String): Favorites = json.decodeFromString(text)
    }
}
