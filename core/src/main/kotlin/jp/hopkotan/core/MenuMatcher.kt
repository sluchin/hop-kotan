/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.core

import java.text.Normalizer

/** メニューのページの文字から, 知っているビールの名前を探す. */
object MenuMatcher {
    /** 英字と数字の文字コードの上限 (これ以上は, 日本語など). */
    private const val ASCII_LIMIT = 128

    /** 探す名前の最短の長さ (これより短い名前は, 別の言葉に含まれやすいので, 探さない). */
    private const val MIN_LENGTH = 3

    /**
     * ページの文字に含まれるビールを探す.
     *
     * 全角と半角, 大文字と小文字, 空白と記号の違いは, 同じとする. 英字と数字だけの名前は, 日本語を除いた
     * 文字でも探す (`kampoca−カンポチャ−2026` や `カンポチャ-kampoca 2026` で, `kampoca 2026` が見つかる).
     * 同じビールは 1 つにする (今日のタップを先に出す).
     *
     * @param text ページの文字
     * @param taps 今日のタップ
     * @param beers ORIGINALS と FREEDOMS のビール
     * @return 見つかったビール
     */
    fun find(
        text: String,
        taps: List<Tap>,
        beers: List<Beer>,
    ): List<Tap> {
        val page = normalize(text)
        // メニューは, 英字の名前に日本語の読みを挟んだり, 並べたりする. 英字と数字だけの名前は, 日本語を除いた文字でも探す
        val latin = page.filter { it.code < ASCII_LIMIT }
        val candidates = taps + beers.map { Tap(it.name, it.style, it.id, it.url, it.imageUrl) }

        return candidates
            .filter {
                val name = normalize(it.name)
                name.length >= MIN_LENGTH &&
                    (
                        page.contains(name) ||
                            (name.all { c -> c.code < ASCII_LIMIT } && latin.contains(name))
                    )
            }.distinctBy { Ratings.key(it.beerId, it.name) }
    }

    /**
     * 注文しようとしているビールを探す. 文字の候補を, 狭い範囲から順に見て, ビールが見つかった最初の候補の
     * ビールを返す (注文の画面の見出しから, 外側へ. 外側ほど, 後ろのメニューのビールも含むため).
     *
     * @param candidates 文字の候補 (狭い範囲から順)
     * @param taps 今日のタップ
     * @param beers ORIGINALS と FREEDOMS のビール
     * @return ビール (どの候補にもなければ, 空)
     */
    fun findOrdered(
        candidates: List<String>,
        taps: List<Tap>,
        beers: List<Beer>,
    ): List<Tap> =
        candidates
            .asSequence()
            .map { find(it, taps, beers) }
            .firstOrNull { it.isNotEmpty() }
            .orEmpty()

    /** 文字の違い (幅, 大文字と小文字, 空白, 記号) をなくす. */
    private fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFKC).lowercase().filter { it.isLetterOrDigit() }
}
