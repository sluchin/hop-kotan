/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.core

/**
 * タップのビールと, そのビールのデータ.
 *
 * @property tap タップのビール
 * @property beer ビールのデータ (ORIGINALS と FREEDOMS にないビールは null)
 */
data class TapEntry(
    val tap: Tap,
    val beer: Beer?,
)

/** タップリストと, ビールのデータを結び付ける. */
object TapEntries {
    /**
     * タップごとに, ビールのデータ (ABV と IBU など) を結び付ける.
     *
     * @param list タップリスト
     * @param beers ビール
     * @return タップのビール (サイトの並びのまま)
     */
    fun of(
        list: TapList,
        beers: List<Beer>,
    ): List<TapEntry> {
        val byId = beers.associateBy { it.id }

        return list.taps.map { TapEntry(it, it.beerId?.let(byId::get)) }
    }
}
