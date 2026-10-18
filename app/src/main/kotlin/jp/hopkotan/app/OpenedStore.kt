/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.app

import android.content.Context
import jp.hopkotan.core.OpenedTaps

/** 開栓したビールのファイルの名前. */
private const val OPENED_FILE = "opened.json"

/** ログのタグ. */
private const val TAG = "Data"

/** サイトのタップリストを取得したあとに, 開栓したビール ([OpenedTaps]) を, 端末に保存する. */
object OpenedStore {
    /**
     * 開栓したビールを読む.
     *
     * @param context コンテキスト
     * @return 開栓したビール (なければ, 空)
     */
    fun load(context: Context): OpenedTaps =
        context.readLocal(OPENED_FILE)?.let {
            runCatching { OpenedTaps.fromJson(it) }
                .onFailure { AppLog.w(TAG, "Could not read the opened beers", it) }
                .getOrNull()
        } ?: OpenedTaps()

    /**
     * 開栓したビールを保存する (書き込みの途中で終わっても, 前のものが残るように, 別のファイルに書いてから置き換える).
     *
     * @param context コンテキスト
     * @param opened 開栓したビール
     */
    fun save(
        context: Context,
        opened: OpenedTaps,
    ) = context.writeLocal(OPENED_FILE, opened.toJson())
}
