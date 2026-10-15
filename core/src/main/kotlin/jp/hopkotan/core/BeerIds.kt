/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.core

import java.net.URI

/** ビールの URL から, [Beer.id] を作る. */
object BeerIds {
    private const val FREEDOMS = "freedoms"
    private const val ORIGINALS = "originals"
    private const val ORIGINALS_PLUS = "originalsplus"
    private const val HARVEST_BREW = "harvest_brew"

    /**
     * URL から ID を作る.
     *
     * `https://hopkotan.com/freedoms/<スラッグ>/` は `freedoms/<スラッグ>`,
     * `https://hopkotan.com/originals/<名前>/` と `https://hopkotan.com/originals/#<名前>` は
     * `originals/<名前>`. スラッグの百分率符号化 (%xx) は, 元に戻す.
     *
     * @param url ビールの URL (前後の空白は無視する)
     * @return ID. ビールの URL でなければ null
     */
    fun fromUrl(url: String): String? {
        val uri = runCatching { URI(url.trim()) }.getOrNull() ?: return null
        val segments =
            uri.path
                .orEmpty()
                .split('/')
                .filter { it.isNotEmpty() }
        val series = segments.firstOrNull()
        val slug =
            when (series) {
                FREEDOMS, ORIGINALS_PLUS, HARVEST_BREW -> segments.getOrNull(1)
                ORIGINALS -> segments.getOrNull(1) ?: uri.fragment
                else -> null
            }

        return slug?.takeIf { it.isNotEmpty() }?.let { "$series/$it" }
    }

    /**
     * ID から, シリーズを返す.
     *
     * @param id [Beer.id]
     * @return シリーズ (知らないシリーズなら null)
     */
    fun seriesOf(id: String): Series? =
        when (id.substringBefore('/')) {
            ORIGINALS -> Series.ORIGINALS
            FREEDOMS -> Series.FREEDOMS
            ORIGINALS_PLUS -> Series.ORIGINALS_PLUS
            HARVEST_BREW -> Series.HARVEST_BREW
            else -> null
        }
}
