/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.app

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import jp.hopkotan.core.NewTaps
import java.io.IOException
import java.util.concurrent.TimeUnit

/** 定期の確認の名前. */
private const val WORK_NAME = "new_beer"

/** 確認の間隔 (時間). データは 1 日 2 回 (12:30 と 17:30) 更新される. */
private const val INTERVAL_HOURS = 3L

/**
 * 最新のタップリストを取得して, 前の日になかったビールがあれば通知する, 定期の仕事.
 *
 * @param context コンテキスト
 * @param params 仕事の引数
 */
class NewBeerWorker(
    context: Context,
    params: WorkerParameters,
) : Worker(context, params) {
    override fun doWork(): Result =
        try {
            val store = DataStore(applicationContext)
            val current = store.download().second
            val previous = NewTaps.previousOf(store.downloadHistory(), current)
            if (previous != null) {
                NewBeerNotifier.notifyOnce(
                    applicationContext,
                    current.date,
                    NewTaps.find(previous, current),
                )
            }
            Result.success()
        } catch (_: IOException) {
            Result.retry()
        } catch (_: IllegalArgumentException) {
            Result.failure()
        }

    companion object {
        /**
         * 定期の確認を予約する (すでに予約してあれば, そのままにする).
         *
         * @param context コンテキスト
         */
        fun schedule(context: Context) {
            val request =
                PeriodicWorkRequestBuilder<NewBeerWorker>(INTERVAL_HOURS, TimeUnit.HOURS)
                    .setConstraints(
                        Constraints
                            .Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build(),
                    ).build()
            WorkManager
                .getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
