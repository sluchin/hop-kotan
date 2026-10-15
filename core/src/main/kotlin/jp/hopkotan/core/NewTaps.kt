/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.core

/** 前の日のタップリストと比べて, 新しく出たビールを見つける. */
object NewTaps {
    /**
     * 前の日になくて, 今日のタップリストにあるビールを返す.
     *
     * ビールの同じさは [Tap.beerId] で決める (なければ, 名前).
     *
     * @param previous 前の日のタップリスト
     * @param current 今日のタップリスト
     * @return 新しく出たビール (今日のタップリストの並びのまま)
     */
    fun find(
        previous: TapList,
        current: TapList,
    ): List<Tap> {
        val known = previous.taps.map(::keyOf).toSet()

        return current.taps.filter { keyOf(it) !in known }
    }

    /**
     * 履歴から, 今日より前で一番新しい日のタップリストを選ぶ.
     *
     * @param history 履歴 (順序は問わない)
     * @param current 今日のタップリスト
     * @return 前の日のタップリスト. なければ null
     */
    fun previousOf(
        history: List<TapList>,
        current: TapList,
    ): TapList? = history.filter { it.date < current.date }.maxByOrNull { it.date }

    private fun keyOf(tap: Tap): String = tap.beerId ?: "name:${tap.name}"
}
