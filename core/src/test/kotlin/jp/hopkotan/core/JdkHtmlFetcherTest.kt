/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.core

import com.sun.net.httpserver.HttpServer
import java.io.IOException
import java.net.InetSocketAddress
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** [JdkHtmlFetcher] のテスト. 本物の HTTP サーバを, 手元で動かす. */
class JdkHtmlFetcherTest {
    private val server: HttpServer = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    private val statuses = ArrayDeque<Int>()
    private val agents = mutableListOf<String?>()
    private var now = 0L
    private val slept = mutableListOf<Long>()

    init {
        server.createContext("/") { exchange ->
            agents += exchange.requestHeaders.getFirst("User-Agent")
            val status = statuses.removeFirstOrNull() ?: 200
            val body = "ページ".toByteArray()
            exchange.sendResponseHeaders(status, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.start()
    }

    @AfterTest
    fun stop() = server.stop(0)

    private val url get() = "http://127.0.0.1:${server.address.port}/page"

    private fun fetcher(attempts: Int = 3) =
        JdkHtmlFetcher(
            intervalMs = 1000,
            attempts = attempts,
            sleeper = {
                slept += it
                now += it
            },
            clock = { now },
        )

    /** 取得して本文を返す. 名乗り (User-Agent) を付ける. 最初は待たない. */
    @Test
    fun fetch() {
        assertEquals("ページ", fetcher().fetch(url))
        assertEquals(true, agents.single()?.startsWith("hop-kotan/"))
        assertEquals(emptyList(), slept)
    }

    /** 続けて取得するときは, 前の取得から間隔を空ける. 間隔がたっていれば, 待たない. */
    @Test
    fun interval() {
        val fetcher = fetcher()
        fetcher.fetch(url)
        now += 300
        fetcher.fetch(url)
        now += 5000
        fetcher.fetch(url)

        assertEquals(listOf(700L), slept)
    }

    /** 失敗したら, 間隔を空けて, やり直す. */
    @Test
    fun retry() {
        statuses += listOf(500, 503)

        assertEquals("ページ", fetcher().fetch(url))
        assertEquals(3, agents.size)
        assertEquals(listOf(1000L, 1000L), slept)
    }

    /** 試行の回数を超えたら, 失敗にする. */
    @Test
    fun giveUp() {
        statuses += listOf(404, 404, 404)

        assertFailsWith<IOException> { fetcher().fetch(url) }
        assertEquals(3, agents.size)
    }
}
