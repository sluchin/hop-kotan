/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.core

import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * JDK の HTTP クライアントで, ページを取得する.
 *
 * サイトに負荷をかけないよう, 前の取得から [intervalMs] 以上, 間を空ける. 失敗したら, 間を空けて,
 * [attempts] 回まで, やり直す.
 *
 * @property intervalMs 取得の最小の間隔 (ミリ秒)
 * @property attempts 1 つのページの試行の回数
 * @property sleeper 待つ関数 (ミリ秒. テストで置き換える)
 * @property clock 今の時刻 (ミリ秒. テストで置き換える)
 * @property client HTTP クライアント
 */
class JdkHtmlFetcher(
    private val intervalMs: Long = DEFAULT_INTERVAL_MS,
    private val attempts: Int = DEFAULT_ATTEMPTS,
    private val sleeper: (Long) -> Unit = Thread::sleep,
    private val clock: () -> Long = System::currentTimeMillis,
    private val client: HttpClient =
        HttpClient
            .newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_S))
            .build(),
) : HtmlFetcher {
    /** 前の取得を始めた時刻 (ミリ秒. まだなければ null). */
    private var lastMs: Long? = null

    override fun fetch(url: String): String {
        var failure: IOException? = null

        repeat(attempts) {
            waitInterval()
            try {
                return request(url)
            } catch (e: IOException) {
                failure = e
            }
            lastMs = clock()
        }

        throw IOException("Failed to fetch $url after $attempts attempts", failure)
    }

    /** 前の取得から [intervalMs] たつまで待つ. */
    private fun waitInterval() {
        val last = lastMs
        if (last != null) {
            val rest = last + intervalMs - clock()
            if (rest > 0) {
                sleeper(rest)
            }
        }
        lastMs = clock()
    }

    private fun request(url: String): String {
        val request =
            HttpRequest
                .newBuilder(URI.create(url))
                .header("User-Agent", USER_AGENT)
                .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_S))
                .GET()
                .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString(Charsets.UTF_8))
        if (response.statusCode() != HTTP_OK) {
            throw IOException("HTTP ${response.statusCode()} for $url")
        }

        return response.body()
    }

    private companion object {
        const val DEFAULT_INTERVAL_MS = 2000L
        const val DEFAULT_ATTEMPTS = 3
        const val CONNECT_TIMEOUT_S = 10L
        const val REQUEST_TIMEOUT_S = 30L
        const val HTTP_OK = 200
        const val USER_AGENT = "hop-kotan/0.1 (+https://github.com/sluchin/hop-kotan)"
    }
}
