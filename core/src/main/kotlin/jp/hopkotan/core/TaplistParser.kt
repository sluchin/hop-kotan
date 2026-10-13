/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.core

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/** タップルームのページの解析. */
object TaplistParser {
    private val datePattern = Regex("""(\d{4})\.(\d{1,2})\.(\d{1,2})""")
    private val numberPattern = Regex("""\d+""")

    /**
     * タップルームのページから, 今日のタップリストを取り出す.
     *
     * @param html ページの HTML
     * @param baseUrl ページの URL (相対の URL を直すため)
     * @return タップリスト
     * @throws ScrapeException 更新日が見つからないとき
     */
    fun parse(
        html: String,
        baseUrl: String,
    ): TapList {
        val doc = Jsoup.parse(html, baseUrl)
        val dateText = doc.select("dt:contains(更新日) + dd").text()
        val match =
            datePattern.find(dateText)
                ?: throw ScrapeException("The update date of the tap list was not found")
        val (year, month, day) = match.destructured
        val sizes = doc.select(".taplist-price__unit").mapNotNull { toSize(it) }
        val taps =
            doc
                .select(".taproom-taplist__item")
                .map { toTap(it) }
                .filter { it.name.isNotEmpty() }

        return TapList("%s-%02d-%02d".format(year, month.toInt(), day.toInt()), sizes, taps)
    }

    private fun toSize(unit: Element): PriceSize? {
        val ml = numberPattern.find(unit.select(".taplist-price__size").text())?.value?.toInt()
        val yen = numberPattern.find(unit.select("dd").text())?.value?.toInt()

        return if (ml == null || yen == null) {
            null
        } else {
            PriceSize(unit.select(".taplist-price__label").text().trim(), ml, yen)
        }
    }

    private fun toTap(element: Element): Tap {
        val url =
            element
                .selectFirst("a[href]")
                ?.absUrl("href")
                ?.trim()

        return Tap(
            name = element.select(".taproom-taplist__name").text().trim(),
            style = element.select(".taproom-taplist__style").text().trim(),
            beerId = url?.let { BeerIds.fromUrl(it) },
            url = url,
            imageUrl = element.selectFirst("img[src]")?.absUrl("src"),
        )
    }
}
