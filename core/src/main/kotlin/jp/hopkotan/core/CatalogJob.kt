/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.core

import java.nio.file.Files
import java.nio.file.Path

/**
 * 取得したデータを, ディレクトリのファイルに書く.
 *
 * @property scraper 取得
 * @property dir 書き出すディレクトリ (なければ作る)
 */
class CatalogJob(
    private val scraper: Scraper,
    private val dir: Path,
) {
    /**
     * タップリストを取得して, `taplist.json` に書く.
     *
     * @return タップの数
     */
    fun updateTaplist(): Int {
        val list = scraper.scrapeTaplist()
        write(TAPLIST_FILE, CatalogJson.encodeTaplist(list))

        return list.taps.size
    }

    /**
     * ORIGINALS と FREEDOMS を取得して, `beers.json` に書く.
     *
     * すでに書いてあるビールは, 詳細のページを取り直さない. ORIGINALS は, 1 ページなので, 毎回取る.
     *
     * @return ビールの数
     */
    fun updateBeers(): Int {
        val file = dir.resolve(BEERS_FILE)
        val known =
            if (Files.exists(file)) {
                CatalogJson.decodeBeers(Files.readString(file)).associateBy { it.id }
            } else {
                emptyMap()
            }
        val beers = scraper.scrapeOriginals() + scraper.scrapeFreedoms(known)
        write(BEERS_FILE, CatalogJson.encodeBeers(beers))

        return beers.size
    }

    private fun write(
        name: String,
        text: String,
    ) {
        Files.createDirectories(dir)
        Files.writeString(dir.resolve(name), text)
    }

    /** 書き出すファイルの名前. */
    companion object {
        /** タップリストのファイル. */
        const val TAPLIST_FILE = "taplist.json"

        /** ビールのファイル. */
        const val BEERS_FILE = "beers.json"
    }
}
