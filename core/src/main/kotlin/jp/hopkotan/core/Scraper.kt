/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.core

/**
 * 忽布古丹醸造のサイトから, ビールのデータを取得する.
 *
 * @property fetcher ページの取得
 */
class Scraper(
    private val fetcher: HtmlFetcher,
) {
    /**
     * タップルームの, 今日のタップリストを取得する.
     *
     * @return タップリスト
     * @throws ScrapeException タップが 1 つも読めなかったとき (サイトの作りが変わったと考える)
     */
    fun scrapeTaplist(): TapList {
        val list = TaplistParser.parse(fetcher.fetch(TAPROOM_URL), TAPROOM_URL)
        if (list.taps.isEmpty()) {
            throw ScrapeException("No taps were found in $TAPROOM_URL")
        }

        return list
    }

    /**
     * ORIGINALS のビールを取得する.
     *
     * @return ビール (ページの順)
     * @throws ScrapeException ビールが 1 つも読めなかったとき
     */
    fun scrapeOriginals(): List<Beer> {
        val beers = OriginalsParser.parse(fetcher.fetch(ORIGINALS_URL), ORIGINALS_URL)
        if (beers.isEmpty()) {
            throw ScrapeException("No beers were found in $ORIGINALS_URL")
        }

        return beers
    }

    /**
     * FREEDOMS のビールを取得する.
     *
     * 一覧を最後のページまでたどる. 詳細のページ (ABV と IBU) は, [known] にないビールだけ取得する.
     *
     * @param known すでに取得したビール (ID が同じものは, 取り直さない)
     * @return ビール (発売月の新しい順, 同じ月は名前順)
     * @throws ScrapeException ビールが 1 つも読めなかったとき, ページが多すぎるとき
     */
    fun scrapeFreedoms(known: Map<String, Beer> = emptyMap()): List<Beer> {
        val beers =
            collectItems()
                .mapNotNull { item -> BeerIds.fromUrl(item.url)?.let { it to item } }
                .distinctBy { it.first }
                .map { (id, item) -> known[id] ?: toBeer(id, item) }
        if (beers.isEmpty()) {
            throw ScrapeException("No beers were found in $FREEDOMS_URL")
        }

        return beers.sortedWith(
            compareByDescending<Beer> { it.released.orEmpty() }.thenBy { it.name },
        )
    }

    /**
     * タップに出ている, ORIGINALS plus と HARVEST BREW のビールを取得する.
     *
     * 一覧のページはたどらず, タップのビールの詳細のページから ABV と IBU を読む (取得済みのビールは, 取り直さない).
     *
     * @param taps 今日のタップ
     * @param known 取得済みのビール (ID が鍵)
     * @return ビール (タップの並び)
     */
    fun scrapeTapBeers(
        taps: List<Tap>,
        known: Map<String, Beer> = emptyMap(),
    ): List<Beer> =
        taps
            .mapNotNull(::tapBeerSource)
            .distinctBy { it.id }
            .map { known[it.id] ?: toTapBeer(it) }

    private class TapBeerSource(
        val tap: Tap,
        val id: String,
        val url: String,
        val series: Series,
    )

    private fun tapBeerSource(tap: Tap): TapBeerSource? {
        val id = tap.beerId
        val url = tap.url
        val series = id?.let(BeerIds::seriesOf)?.takeIf { it.fromTap }

        return if (id != null && url != null && series != null) {
            TapBeerSource(tap, id, url, series)
        } else {
            null
        }
    }

    private fun toTapBeer(source: TapBeerSource): Beer {
        val spec = DetailParser.parse(fetcher.fetch(source.url))

        return Beer(
            id = source.id,
            series = source.series,
            name = source.tap.name,
            style = source.tap.style,
            url = source.url,
            imageUrl = source.tap.imageUrl,
            abv = spec.abv,
            ibu = spec.ibu,
        )
    }

    /** 一覧の全てのページをたどって, 項目を集める. */
    private fun collectItems(): List<FreedomsItem> {
        val items = mutableListOf<FreedomsItem>()
        var url: String? = FREEDOMS_URL
        var pages = 0

        while (url != null) {
            if (pages >= MAX_PAGES) {
                throw ScrapeException("The list of $FREEDOMS_URL has more than $MAX_PAGES pages")
            }
            val page = FreedomsParser.parse(fetcher.fetch(url), url)
            items += page.items
            url = page.nextUrl
            pages++
        }

        return items
    }

    /** 詳細のページを取得して, ビールにする. */
    private fun toBeer(
        id: String,
        item: FreedomsItem,
    ): Beer {
        val spec = DetailParser.parse(fetcher.fetch(item.url))

        return Beer(
            id = id,
            series = Series.FREEDOMS,
            name = item.name,
            style = item.style,
            url = item.url,
            imageUrl = item.imageUrl,
            released = item.released,
            abv = spec.abv,
            ibu = spec.ibu,
        )
    }

    /** サイトの URL と, 安全のための上限. */
    companion object {
        /** タップルームのページ (毎日). */
        const val TAPROOM_URL = "https://hopkotan.com/taproom/"

        /** ORIGINALS のページ. */
        const val ORIGINALS_URL = "https://hopkotan.com/originals/"

        /** FREEDOMS の一覧の 1 ページ目. */
        const val FREEDOMS_URL = "https://hopkotan.com/freedoms/"

        /** たどる一覧のページ数の上限 (ページ送りが終わらないときに, 止める). */
        const val MAX_PAGES = 200
    }
}
