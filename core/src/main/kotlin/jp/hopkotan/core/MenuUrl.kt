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
     * 移動の先を, アプリの中 (WebView) で開くかを決める.
     *
     * 注文のページは, 認証 (リクルート ID など) や支払いで, 別のサイトを通ってから戻ってくる. セッションは
     * WebView の中にだけあるので, `https` のページは, どのホストでも, アプリの中で続ける. `https` 以外
     * (`intent:`, `tel:`, `mailto:` など, ほかのアプリを開くリンク) は, アプリの外で開く.
     *
     * @param url 開こうとしている URL
     * @return アプリの中で開くなら true
     */
    fun opensInApp(url: String): Boolean = parse(url) != null

    /**
     * 表示するホスト名を返す.
     *
     * @param base メニューの URL ([parse] で認められたもの)
     * @return ホスト名
     */
    fun host(base: String): String = URI(base).host
}
