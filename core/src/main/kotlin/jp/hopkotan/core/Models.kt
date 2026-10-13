/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.core

import kotlinx.serialization.Serializable

/** ビールのシリーズ. */
@Serializable
enum class Series {
    /** 定番のビール (ORIGINALS). */
    ORIGINALS,

    /** 一期一会のビール (FREEDOMS). */
    FREEDOMS,
}

/**
 * 1 つのビール.
 *
 * @property id 一意の ID (`freedoms/<スラッグ>` か `originals/<名前>`)
 * @property series シリーズ
 * @property name 名前
 * @property style ビアスタイル
 * @property url 元のページの URL
 * @property imageUrl 画像の URL (画像そのものは, 保存しない)
 * @property released 発売月 (`2026-09`. 分からなければ null. ORIGINALS は null)
 * @property abv アルコール度数 (%. 分からなければ null)
 * @property ibu 苦味の単位 (分からなければ null)
 */
@Serializable
data class Beer(
    val id: String,
    val series: Series,
    val name: String,
    val style: String,
    val url: String,
    val imageUrl: String? = null,
    val released: String? = null,
    val abv: Double? = null,
    val ibu: Int? = null,
)

/**
 * タップリストの 1 つのビール.
 *
 * @property name 名前
 * @property style ビアスタイル
 * @property beerId [Beer.id] (定番と FREEDOMS のビールでなければ null)
 * @property url 元のページの URL (なければ null)
 * @property imageUrl 画像の URL (なければ null)
 */
@Serializable
data class Tap(
    val name: String,
    val style: String,
    val beerId: String? = null,
    val url: String? = null,
    val imageUrl: String? = null,
)

/**
 * 量と価格.
 *
 * @property label 呼び名 (M, L)
 * @property ml 量 (ml)
 * @property yen 価格 (円. 「750 ~」のように, ビールによって上がるときの, 最低の価格)
 */
@Serializable
data class PriceSize(
    val label: String,
    val ml: Int,
    val yen: Int,
)

/**
 * 今日のタップリスト.
 *
 * @property date 更新日 (`2026-10-13`)
 * @property sizes 量と価格
 * @property taps タップのビール (サイトの並びのまま)
 */
@Serializable
data class TapList(
    val date: String,
    val sizes: List<PriceSize>,
    val taps: List<Tap>,
)

/**
 * 取得や解析の失敗.
 *
 * @param message メッセージ
 * @param cause 原因
 */
class ScrapeException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
