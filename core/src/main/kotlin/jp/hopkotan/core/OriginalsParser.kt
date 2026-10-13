/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.core

import org.jsoup.Jsoup

/** ORIGINALS のページの解析. */
object OriginalsParser {
    /**
     * ORIGINALS のページから, ビールを取り出す.
     *
     * 1 つのビールは, `.product-info` の中にある. 名前は, 近くの `id` (`upopo` など) を使う.
     *
     * @param html ページの HTML
     * @param baseUrl ページの URL (相対の URL を直すため)
     * @return ビール (ページの順)
     */
    fun parse(
        html: String,
        baseUrl: String,
    ): List<Beer> {
        val doc = Jsoup.parse(html, baseUrl)

        return doc.select(".product-info").mapNotNull { info ->
            val name = info.closest("[id]")?.id().orEmpty()
            val spec = parseSpec(info)
            val section = info.closest("[id]")
            val image = section?.selectFirst(".product-img img")?.absUrl("src")

            if (name.isEmpty()) {
                null
            } else {
                Beer(
                    id = "originals/$name",
                    series = Series.ORIGINALS,
                    name = name,
                    style = info.select(".product-style").text().trim(),
                    url =
                        info
                            .selectFirst(".product-info__link a")
                            ?.absUrl("href")
                            ?.ifEmpty { null } ?: "$baseUrl#$name",
                    imageUrl = image?.ifEmpty { null },
                    abv = spec.abv,
                    ibu = spec.ibu,
                )
            }
        }
    }
}
