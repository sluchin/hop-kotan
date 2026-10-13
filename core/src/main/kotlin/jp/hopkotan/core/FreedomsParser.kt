/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.core

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/**
 * FREEDOMS の一覧の 1 つの項目.
 *
 * @property name 名前
 * @property style ビアスタイル
 * @property url 詳細のページの URL
 * @property imageUrl 画像の URL (なければ null)
 * @property released 発売月 (`2026-09`. 読めなければ null)
 */
data class FreedomsItem(
    val name: String,
    val style: String,
    val url: String,
    val imageUrl: String?,
    val released: String?,
)

/**
 * FREEDOMS の一覧の 1 ページ.
 *
 * @property items 項目
 * @property nextUrl 次のページの URL (最後のページは null)
 */
data class FreedomsPage(
    val items: List<FreedomsItem>,
    val nextUrl: String?,
)

/** FREEDOMS の一覧のページの解析. */
object FreedomsParser {
    private val monthPattern = Regex("""^([A-Za-z]{3})\.?\s*(\d{4})$""")
    private val months =
        listOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")

    /**
     * 一覧の 1 ページを解析する.
     *
     * @param html ページの HTML
     * @param baseUrl ページの URL (相対の URL を直すため)
     * @return 項目と, 次のページの URL
     */
    fun parse(
        html: String,
        baseUrl: String,
    ): FreedomsPage {
        val doc = Jsoup.parse(html, baseUrl)
        val items =
            doc
                .select("div.itemlist > div.item")
                .map { toItem(it) }
                .filter { it.name.isNotEmpty() && it.url.isNotEmpty() }
        val next = doc.selectFirst("a.nextpostslink[href]")?.absUrl("href")

        return FreedomsPage(items, next)
    }

    /**
     * 発売月の表記 (`Sep. 2026`, `NOV. 2022`, `Feb.2022`) を `2026-09` に直す.
     *
     * @param text 表記
     * @return `年-月`. 読めなければ null
     */
    fun parseMonth(text: String): String? {
        val match = monthPattern.find(text.trim()) ?: return null
        val month = months.indexOf(match.groupValues[1].lowercase()) + 1

        return if (month == 0) null else "%s-%02d".format(match.groupValues[2], month)
    }

    private fun toItem(element: Element): FreedomsItem =
        FreedomsItem(
            name = element.select(".item-name").text().trim(),
            style = element.select(".item-style").text().trim(),
            url = element.selectFirst(".item-thum a")?.absUrl("href").orEmpty(),
            imageUrl = element.selectFirst(".item-thum img")?.absUrl("src")?.ifEmpty { null },
            released = parseMonth(element.select(".item-cat").text()),
        )
}
