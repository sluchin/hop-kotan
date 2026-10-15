/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import jp.hopkotan.core.Tap

/** 通知のチャンネルの ID. */
private const val CHANNEL_ID = "new_beer"

/** 通知の ID (新しい通知は, 前の通知を置き換える). */
private const val NOTIFICATION_ID = 1

/** 通知した日を覚える, 設定のファイルの名前. */
private const val PREFS = "new_beer"

/** 最後に通知した, タップリストの日付の, 設定のキー. */
private const val KEY_LAST_DATE = "last_date"

/** 新しく出たビールを通知する. */
object NewBeerNotifier {
    /**
     * このタップリストの日付で, まだ通知していなければ, 新しいビールを通知する.
     *
     * 同じ日に 2 回通知しない (定期の確認は 1 日に何度も動く). 通知の権限がないときは, 通知せず, 通知した
     * ことにもしない (あとで権限を許可すれば, その日のうちは通知する).
     *
     * @param context コンテキスト
     * @param date タップリストの日付
     * @param taps 新しく出たビール
     */
    fun notifyOnce(
        context: Context,
        date: String,
        taps: List<Tap>,
    ) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val granted =
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        if (taps.isEmpty() || !granted || prefs.getString(KEY_LAST_DATE, null) == date) {
            return
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notify_channel),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
        val open =
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        val text = taps.joinToString(", ") { it.name }
        val notification =
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.notify_title, taps.size))
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        prefs.edit { putString(KEY_LAST_DATE, date) }
    }
}
