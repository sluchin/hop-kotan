/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.scraper

import jp.hopkotan.core.CatalogJob
import jp.hopkotan.core.JdkHtmlFetcher
import jp.hopkotan.core.Scraper
import java.nio.file.Paths
import kotlin.system.exitProcess

/**
 * サイトからデータを取得して, ディレクトリの JSON に書く.
 *
 * 使い方: `taplist <ディレクトリ>` (タップリスト) か `beers <ディレクトリ>` (ORIGINALS と FREEDOMS).
 * 失敗したら, メッセージを出して, 終了コード 1 で終わる (Actions を失敗させる).
 *
 * @param args コマンドとディレクトリ
 */
fun main(args: Array<String>) {
    val result =
        runCatching {
            val job =
                CatalogJob(Scraper(JdkHtmlFetcher()), Paths.get(args.getOrElse(1) { usage() }))
            when (args.getOrNull(0)) {
                "taplist" -> "taps: ${job.updateTaplist()}"
                "beers" -> "beers: ${job.updateBeers()}"
                else -> usage()
            }
        }
    result
        .onSuccess { println(it) }
        .onFailure {
            System.err.println("Error: ${it.message}")
            exitProcess(1)
        }
}

/** 使い方を伝えて, 失敗にする. */
private fun usage(): Nothing = error("Usage: taplist|beers <directory>")
