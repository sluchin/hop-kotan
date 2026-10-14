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
     * 履歴として, 同じ内容を `taplist/<日付>.json` にも書き, 目次 `taplist/index.json` (新しい日が先)
     * に日付を足す. 同じ日に何度も取得したら, その日のファイルを書き直す (最後の内容が残る).
     *
     * @return タップの数
     */
    fun updateTaplist(): Int {
        val list = scraper.scrapeTaplist()
        val text = CatalogJson.encodeTaplist(list)
        write(TAPLIST_FILE, text)
        write("$HISTORY_DIR/${list.date}.json", text)
        write("$HISTORY_DIR/$INDEX_FILE", CatalogJson.encodeDates(addDate(list.date)))

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

    /** 目次に日付を足して, 新しい日が先の並びにする. */
    private fun addDate(date: String): List<String> {
        val file = dir.resolve("$HISTORY_DIR/$INDEX_FILE")
        val known =
            if (Files.exists(file)) CatalogJson.decodeDates(Files.readString(file)) else emptyList()

        return (known + date).distinct().sortedDescending()
    }

    private fun write(
        name: String,
        text: String,
    ) {
        val file = dir.resolve(name)
        Files.createDirectories(file.parent)
        Files.writeString(file, text)
    }

    /** 書き出すファイルの名前. */
    companion object {
        /** タップリストのファイル. */
        const val TAPLIST_FILE = "taplist.json"

        /** 日ごとのタップリスト (履歴) のディレクトリ. */
        const val HISTORY_DIR = "taplist"

        /** 履歴の目次のファイル (日付の一覧. HISTORY_DIR の中). */
        const val INDEX_FILE = "index.json"

        /** ビールのファイル. */
        const val BEERS_FILE = "beers.json"
    }
}
