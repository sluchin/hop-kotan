/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.core

/** ビールの並べ方. */
enum class SortKey {
    /** 発売月. */
    RELEASED,

    /** 名前. */
    NAME,

    /** アルコール度数 (ABV). */
    ABV,

    /** 苦味 (IBU). */
    IBU,

    /** ビアスタイル. */
    STYLE,

    /** 星の評価. */
    RATING,
}

/**
 * ビールの絞り込み (指定した条件を, 全て満たすものだけ残す).
 *
 * @property text 名前かスタイルに含まれる文字 (大文字と小文字は区別しない. 空なら, 絞らない)
 * @property style ビアスタイル (大文字と小文字は区別しない. null なら, 絞らない)
 * @property series シリーズ (null なら, 絞らない)
 * @property minAbv ABV の下限 (%. 分からないビールは, 除く. null なら, 絞らない)
 * @property minIbu IBU の下限 (分からないビールは, 除く. null なら, 絞らない)
 * @property year 発売年 (`2026`. 発売月が分からないビールは, 除く. null なら, 絞らない)
 * @property minStars 星の下限 (評価していないビールは, 除く. null なら, 絞らない)
 * @property onTapIds 残すビールの ID (今日のタップにあるものだけ見るとき. null なら, 絞らない)
 */
data class BeerFilter(
    val text: String = "",
    val style: String? = null,
    val series: Series? = null,
    val minAbv: Double? = null,
    val minIbu: Int? = null,
    val year: String? = null,
    val minStars: Int? = null,
    val onTapIds: Set<String>? = null,
)

/** ビールのソートと絞り込み. */
object BeerQuery {
    /**
     * 絞り込んで, 並べる.
     *
     * 値が分からない (null) ビールは, 昇順でも降順でも, 最後に並べる. 同じ値は, 名前順.
     *
     * @param beers ビール
     * @param filter 絞り込み
     * @param sort 並べ方
     * @param descending 降順にするか
     * @param ratings 星の評価 (絞り込みと, 並べ方 [SortKey.RATING] に使う)
     * @return 絞り込んで並べたビール
     */
    fun apply(
        beers: List<Beer>,
        filter: BeerFilter = BeerFilter(),
        sort: SortKey = SortKey.RELEASED,
        descending: Boolean = true,
        ratings: Ratings = Ratings(),
    ): List<Beer> =
        beers
            .filter { matches(it, filter, ratings) }
            .sortedWith(comparator(sort, descending, ratings))

    /**
     * ビアスタイルの一覧を返す (大文字と小文字の違いは, 同じものとする).
     *
     * @param beers ビール
     * @return スタイル (名前順. 同じものは, 最初に出てきた表記)
     */
    fun styles(beers: List<Beer>): List<String> =
        beers
            .map { it.style }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() }

    /**
     * 発売年の一覧を返す.
     *
     * @param beers ビール
     * @return 発売年 (`2026` など. 新しい順)
     */
    fun years(beers: List<Beer>): List<String> =
        beers.mapNotNull { it.released?.take(YEAR_LENGTH) }.distinct().sortedDescending()

    private fun matches(
        beer: Beer,
        filter: BeerFilter,
        ratings: Ratings,
    ): Boolean =
        matchesLabel(beer, filter) &&
            matchesNumbers(beer, filter, ratings) &&
            matchesTap(beer, filter)

    /** 文字, スタイル, シリーズ. */
    private fun matchesLabel(
        beer: Beer,
        filter: BeerFilter,
    ): Boolean {
        val text = filter.text.trim()

        return (text.isEmpty() || containsText(beer, text)) &&
            (filter.style == null || beer.style.equals(filter.style, ignoreCase = true)) &&
            (filter.series == null || beer.series == filter.series)
    }

    /** ABV, IBU, 発売年, 星. */
    private fun matchesNumbers(
        beer: Beer,
        filter: BeerFilter,
        ratings: Ratings,
    ): Boolean {
        val abv = filter.minAbv
        val ibu = filter.minIbu
        val stars = filter.minStars

        return (abv == null || (beer.abv ?: -1.0) >= abv) &&
            (ibu == null || (beer.ibu ?: -1) >= ibu) &&
            (stars == null || ratings.of(Ratings.key(beer.id, beer.name)) >= stars) &&
            (filter.year == null || beer.released?.startsWith(filter.year) == true)
    }

    /** 今日のタップ. */
    private fun matchesTap(
        beer: Beer,
        filter: BeerFilter,
    ): Boolean = filter.onTapIds == null || beer.id in filter.onTapIds

    private fun containsText(
        beer: Beer,
        text: String,
    ): Boolean =
        beer.name.contains(text, ignoreCase = true) || beer.style.contains(text, ignoreCase = true)

    private fun comparator(
        sort: SortKey,
        descending: Boolean,
        ratings: Ratings,
    ): Comparator<Beer> {
        val main =
            when (sort) {
                SortKey.RELEASED -> by(descending) { it.released }
                SortKey.NAME -> by(descending) { it.name.lowercase() }
                SortKey.ABV -> by(descending) { it.abv }
                SortKey.IBU -> by(descending) { it.ibu }
                SortKey.STYLE -> by(descending) { it.style.lowercase() }
                SortKey.RATING ->
                    by(descending) { ratings.of(Ratings.key(it.id, it.name)).takeIf { s -> s > 0 } }
            }

        return main.thenBy { it.name.lowercase() }
    }

    /** 値 (null は最後) で比べる. */
    private fun <T : Comparable<T>> by(
        descending: Boolean,
        selector: (Beer) -> T?,
    ): Comparator<Beer> {
        val order: Comparator<T> = if (descending) reverseOrder() else naturalOrder()

        return compareBy(nullsLast(order), selector)
    }

    /** 発売年の文字数 (`2026`). */
    private const val YEAR_LENGTH = 4
}
