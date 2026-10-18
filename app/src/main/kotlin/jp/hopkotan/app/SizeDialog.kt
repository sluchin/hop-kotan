/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.ContextWrapper
import jp.hopkotan.core.PriceSize
import java.lang.ref.WeakReference

/**
 * 量を選ぶダイアログを出す.
 *
 * @receiver コンテキスト
 * @param title 題名 (ビールの名前)
 * @param sizes 量と価格
 * @param onChoose 選んだときの処理
 */
fun Context.showSizeDialog(
    title: String,
    sizes: List<PriceSize>,
    onChoose: (PriceSize) -> Unit,
) {
    // 行を素早く 2 回タップすると, 量の画面が重なって開き, 2 杯記録されるので, 同じ画面で開いている間は開かない.
    // 画面 (Activity) が作り直される (ダークモードの切り替えなど) と, 前の画面の量の画面は, 閉じられずに
    // 開いたまま (isShowing が true) に残るので, 前の画面のものは見ない (見ると, 二度と開けなくなる)
    val open = sizeDialog?.get()?.takeIf { it.context.unwrapped() === unwrapped() }
    if (open?.isShowing == true) return
    val labels = sizes.map { getString(R.string.size_item, it.label, it.ml, it.yen) }
    val dialog =
        AlertDialog
            .Builder(this)
            .setTitle(title)
            .setItems(labels.toTypedArray()) { _, index -> onChoose(sizes[index]) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    sizeDialog = WeakReference(dialog)
}

/**
 * テーマなどで包まれたコンテキストから, 中の画面 (Activity) を取り出す (なければ, いちばん内側).
 *
 * @receiver コンテキスト
 * @return 画面
 */
private fun Context.unwrapped(): Context {
    var owner = this
    while (owner is ContextWrapper && owner !is Activity) owner = owner.baseContext
    return owner
}

/** いま開いている, 量を選ぶ画面 (同時に 2 つ開かないため. 前の画面を, 残さないように, 弱い参照で持つ). */
private var sizeDialog: WeakReference<AlertDialog>? = null
