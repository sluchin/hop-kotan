/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.app

import android.content.Context
import jp.hopkotan.core.Favorites

/** お気に入りのファイルの名前. */
private const val FAVORITES_FILE = "favorites.json"

/** ログのタグ. */
private const val TAG = "Data"

/** お気に入りのビール ([Favorites]) を, 端末に保存する. */
object FavoritesStore {
    /**
     * お気に入りを読む.
     *
     * @param context コンテキスト
     * @return お気に入り (なければ, 空)
     */
    fun load(context: Context): Favorites =
        context.readLocal(FAVORITES_FILE)?.let {
            runCatching { Favorites.fromJson(it) }
                .onFailure { AppLog.w(TAG, "Could not read the favorites", it) }
                .getOrNull()
        } ?: Favorites()

    /**
     * お気に入りを保存する (書き込みの途中で終わっても, 前のものが残るように, 別のファイルに書いてから置き換える).
     *
     * @param context コンテキスト
     * @param favorites お気に入り
     */
    fun save(
        context: Context,
        favorites: Favorites,
    ) = context.writeLocal(FAVORITES_FILE, favorites.toJson())
}
