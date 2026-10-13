/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.core

import org.jsoup.nodes.Element

/**
 * ビールの仕様.
 *
 * @property abv アルコール度数 (%. なければ null)
 * @property ibu 苦味の単位 (なければ null)
 */
data class BeerSpec(
    val abv: Double?,
    val ibu: Int?,
)

/** ビールの詳細のページ (FREEDOMS) の解析. */
object DetailParser {
    /**
     * 詳細のページから, ABV と IBU を取り出す.
     *
     * @param html ページの HTML
     * @return 仕様 (なければ, null の項目)
     */
    fun parse(html: String): BeerSpec = parseSpec(org.jsoup.Jsoup.parse(html))
}

/**
 * `dl.beer-spec` の `ABV` と `IBU` を取り出す.
 *
 * @param root 探す範囲
 * @return 仕様 (なければ, null の項目)
 */
internal fun parseSpec(root: Element): BeerSpec {
    val values =
        root
            .select("dl.beer-spec dt")
            .associate { it.text().trim().uppercase() to it.nextElementSibling()?.text().orEmpty() }

    return BeerSpec(
        abv = values["ABV"]?.trim()?.toDoubleOrNull(),
        ibu = values["IBU"]?.trim()?.toDoubleOrNull()?.toInt(),
    )
}
