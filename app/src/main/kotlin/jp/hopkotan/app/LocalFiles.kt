/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.app

import android.content.Context
import java.io.File
import java.io.IOException

/**
 * 端末のファイルを読む.
 *
 * @receiver コンテキスト
 * @param name ファイルの名前 (アプリのディレクトリの中)
 * @return 内容 (ファイルがなければ null)
 */
fun Context.readLocal(name: String): String? =
    File(filesDir, name).takeIf { it.exists() }?.readText(Charsets.UTF_8)

/**
 * 端末のファイルに書く (途中で終わっても, 前の内容が残るように, 別のファイルに書いてから置き換える).
 *
 * @receiver コンテキスト
 * @param name ファイルの名前 (アプリのディレクトリの中. 途中のディレクトリは作る)
 * @param text 内容
 * @throws IOException 置き換えられなかったとき
 */
fun Context.writeLocal(
    name: String,
    text: String,
) {
    val file = File(filesDir, name)
    file.parentFile?.mkdirs()
    val temp = File(filesDir, "$name.tmp")
    temp.writeText(text, Charsets.UTF_8)
    if (!temp.renameTo(file)) {
        throw IOException("Failed to replace $name")
    }
}
