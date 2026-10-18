/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * サイトのタップリストを取得したあとに, 開栓したビール (店のメニューで注文して, 見つかったもの).
 *
 * タップリストの日付ごとに持つ. 日付が同じタップリストにだけ, 足して見せる. 新しい日付のタップリストに
 * 足すときは, 前の日付の分を捨てる (サイトのタップリストに, 載っているはずなので). 変更しない.
 *
 * @property date タップリストの日付 (YYYY-MM-DD. まだなければ空)
 * @property taps 開栓したビール (足した順)
 */
@Serializable
data class OpenedTaps(
    val date: String = "",
    val taps: List<Tap> = emptyList(),
) {
    /**
     * タップリストに, 開栓したビールを足す (日付が同じときだけ. タップリストにあるビールは, 足さない).
     *
     * @param list サイトのタップリスト
     * @return 足したタップリスト
     */
    fun applyTo(list: TapList): TapList =
        if (list.date == date) {
            list.copy(taps = list.taps + taps.filter { tap -> list.taps.none { same(it, tap) } })
        } else {
            list
        }

    /**
     * 開栓したビールを足す. タップリストか, 開栓したビールに, もうあれば, 足さない.
     *
     * @param list サイトのタップリスト (日付が違えば, 前の日付の分を捨てて, この日付で始める)
     * @param tap 開栓したビール
     * @return 新しい一覧
     */
    fun add(
        list: TapList,
        tap: Tap,
    ): OpenedTaps {
        val base = if (list.date == date) this else OpenedTaps(list.date)
        val known = list.taps + base.taps

        return if (known.any { same(it, tap) }) base else base.copy(taps = base.taps + tap)
    }

    /**
     * JSON にする.
     *
     * @return JSON (末尾に改行)
     */
    fun toJson(): String = json.encodeToString(this) + "\n"

    private fun same(
        a: Tap,
        b: Tap,
    ): Boolean = Ratings.key(a.beerId, a.name) == Ratings.key(b.beerId, b.name)

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
         * @return 開栓したビール
         */
        fun fromJson(text: String): OpenedTaps = json.decodeFromString(text)
    }
}
