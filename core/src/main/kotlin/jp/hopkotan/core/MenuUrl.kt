/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.core

import java.net.URI

/** 店のメニューの URL (QR コードの内容) を確かめる. */
object MenuUrl {
    /**
     * QR コードの内容が, 表示してよい URL かを確かめる.
     *
     * `https` で, ホスト名があり, ユーザー名やパスワードを含まないものだけを認める.
     *
     * @param raw QR コードの内容
     * @return URL (認められなければ null)
     */
    fun parse(raw: String?): String? {
        val text = raw?.trim().orEmpty()
        val uri = runCatching { URI(text) }.getOrNull()
        val valid =
            uri?.let {
                it.scheme.equals("https", ignoreCase = true) &&
                    it.host != null &&
                    it.userInfo == null
            } == true

        return if (valid) text else null
    }

    /**
     * URL が, メニューのホストの中 (同じホストか, そのサブドメイン) かを確かめる.
     *
     * 中でなければ, アプリの中では開かない (支払いなどの別のサイトは, ブラウザで開く).
     *
     * @param url 開こうとしている URL
     * @param base メニューの URL ([parse] で認められたもの)
     * @return 中なら true
     */
    fun isInside(
        url: String,
        base: String,
    ): Boolean {
        val target = parse(url)?.let { URI(it).host.lowercase() }
        val host = URI(base).host.lowercase()

        return target != null && (target == host || target.endsWith(".$host"))
    }

    /**
     * 表示するホスト名を返す.
     *
     * @param base メニューの URL ([parse] で認められたもの)
     * @return ホスト名
     */
    fun host(base: String): String = URI(base).host
}
