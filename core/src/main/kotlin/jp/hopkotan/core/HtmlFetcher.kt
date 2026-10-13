/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.core

import java.io.IOException

/** ページの HTML を取得する (テストでは, 偽物に置き換える). */
fun interface HtmlFetcher {
    /**
     * ページを取得する.
     *
     * @param url ページの URL
     * @return HTML
     * @throws IOException 取得できなかったとき
     */
    fun fetch(url: String): String
}
