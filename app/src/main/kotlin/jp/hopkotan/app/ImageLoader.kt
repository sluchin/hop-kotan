/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import android.widget.ImageView
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.Executors

/** ログのタグ. */
private const val TAG = "Images"

/** 接続と読み込みの待ち時間 (ミリ秒). */
private const val TIMEOUT_MS = 15_000

/** 同時に取得する画像の数 (サイトに負荷をかけない). */
private const val THREADS = 2

/** メモリに残す画像の量の, 使えるメモリに対する割合の分母. */
private const val MEMORY_FRACTION = 8

/**
 * 画像 (サイトの URL) を読み込んで, 表示する.
 *
 * 画像は, リポジトリにも, アプリにも, 含めない. 実行のときに, サイトの URL から取得して, 端末の
 * キャッシュ (メモリと, ファイル) に残す. 2 回目からは, 通信しない. 取得できなければ, 代わりの絵のまま.
 *
 * @param context コンテキスト
 */
class ImageLoader(
    context: Context,
) {
    private val dir = File(context.cacheDir, "images").apply { mkdirs() }
    private val executor = Executors.newFixedThreadPool(THREADS)
    private val memory =
        object : LruCache<String, Bitmap>(
            (Runtime.getRuntime().maxMemory() / MEMORY_FRACTION).toInt(),
        ) {
            override fun sizeOf(
                key: String,
                value: Bitmap,
            ): Int = value.byteCount
        }

    /**
     * 画像を表示する (取得できるまでは, 代わりの絵).
     *
     * @param view 表示する部品 (一覧の部品は, 使い回されるので, 取得が終わったときに, 同じ URL のときだけ表示する)
     * @param url 画像の URL (なければ, 代わりの絵のまま)
     */
    fun load(
        view: ImageView,
        url: String?,
    ) {
        view.tag = url
        view.scaleType = ImageView.ScaleType.CENTER_CROP
        view.setImageResource(R.drawable.thumb_placeholder)
        val cached = url?.let { memory.get(it) }
        if (cached != null) {
            view.setImageBitmap(cached)
        } else if (url != null) {
            executor.execute {
                val bitmap = fromFile(url) ?: download(url)
                if (bitmap != null) {
                    memory.put(url, bitmap)
                    view.post { if (view.tag == url) show(view, url, bitmap) }
                }
            }
        }
    }

    /** 取得を止める (画面を閉じるとき). */
    fun shutdown() = executor.shutdown()

    private fun show(
        view: ImageView,
        url: String,
        bitmap: Bitmap,
    ) {
        // 透明な PNG (ORIGINALS のボトル) は, 切り取らずに, 全体を見せる
        view.scaleType =
            if (url.endsWith(
                    ".png",
                )
            ) {
                ImageView.ScaleType.FIT_CENTER
            } else {
                ImageView.ScaleType.CENTER_CROP
            }
        view.setImageBitmap(bitmap)
    }

    private fun fromFile(url: String): Bitmap? =
        file(url)
            .takeIf {
                it.exists()
            }?.let { BitmapFactory.decodeFile(it.path) }

    // 取得できなければ, 代わりの絵のままにする (エラーは, 画面に出さない)
    @Suppress("SwallowedException")
    private fun download(url: String): Bitmap? {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        connection.setRequestProperty("User-Agent", USER_AGENT)

        return try {
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val bytes = connection.inputStream.use { it.readBytes() }
                file(url).writeBytes(bytes)
                AppLog.d(TAG) { "Downloaded an image (${bytes.size} bytes): $url" }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } else {
                AppLog.d(
                    TAG,
                ) { "Could not download an image (HTTP ${connection.responseCode}): $url" }
                null
            }
        } catch (e: IOException) {
            // 通信できないと, 画像の数だけ出るので, デバッグ版だけ
            AppLog.d(TAG) { "Could not download an image: $url ($e)" }
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun file(url: String): File {
        val digest = MessageDigest.getInstance("SHA-256").digest(url.toByteArray())

        return File(dir, digest.joinToString("") { "%02x".format(it) })
    }

    private companion object {
        const val USER_AGENT = "hop-kotan/0.1 (+https://github.com/sluchin/hop-kotan)"
    }
}
