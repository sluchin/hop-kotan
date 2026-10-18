/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.widget.LinearLayout
import androidx.core.net.toUri
import com.google.android.material.button.MaterialButton
import jp.hopkotan.core.Beer
import jp.hopkotan.core.Ratings
import jp.hopkotan.core.Tap

/** ポップアップの中身の, 左右の余白 (dp. ポップアップの字に合わせる). */
const val DIALOG_PADDING_DP = 24

/**
 * パネルが, アプリに頼むお気に入りの操作.
 *
 * @property has お気に入りか (評価と同じキー ([Ratings.key]) を受け取る)
 * @property toggle お気に入りを切り替える (キーを受け取る)
 */
class FavoriteActions(
    val has: (String) -> Boolean,
    val toggle: (String) -> Unit,
)

/**
 * ビールの一覧の 1 行 (名前, スタイルと発売月, 評価とお気に入りとタップ中の印, IBU と ABV, 写真) を作る.
 *
 * @receiver 画面
 * @param beer ビール
 * @param actions アプリに頼む操作
 * @param favorites お気に入りの操作
 * @param onTap 今日のタップにあるか
 * @return 行の内容
 */
fun Activity.beerRow(
    beer: Beer,
    actions: PanelActions,
    favorites: FavoriteActions,
    onTap: Boolean,
): RowModel {
    val key = Ratings.key(beer.id, beer.name)

    return RowModel(
        title = beer.name,
        subtitle =
            getString(
                R.string.row_style_released,
                beer.style,
                beer.released ?: beer.series.name,
            ),
        extra =
            joinExtra(
                if (favorites.has(key)) getString(R.string.favorite_mark) else null,
                Ratings.text(actions.ratings.of(key)),
                if (onTap) getString(R.string.on_tap) else null,
            ),
        badgeLead = beer.ibu?.let { getString(R.string.ibu_label, it.toString()) },
        badge = beer.abv?.let { getString(R.string.abv_badge, formatDecimal(it)) },
        imageUrl = beer.imageUrl,
    )
}

/**
 * ビールの詳細 (スタイル, 発売月, ABV, IBU) と, 星の評価と, お気に入りのボタンを, ポップアップに出す.
 *
 * 「飲んだ」で量を選んで記録し, 「サイトを開く」でビールのページを開く.
 *
 * @receiver 画面
 * @param beer ビール
 * @param actions アプリに頼む操作
 * @param favorites お気に入りの操作
 */
fun Activity.showBeerDetail(
    beer: Beer,
    actions: PanelActions,
    favorites: FavoriteActions,
) {
    val key = Ratings.key(beer.id, beer.name)
    val unknown = getString(R.string.unknown)
    val content =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(DIALOG_PADDING_DP), 0, dp(DIALOG_PADDING_DP), 0)
            addView(
                ratingBar(
                    actions.ratings.of(key),
                    withClear = true,
                ) { actions.ratings.set(key, it) },
            )
            addView(favoriteButton(key, favorites))
        }
    AlertDialog
        .Builder(this)
        .setTitle(beer.name)
        .setMessage(
            getString(
                R.string.beer_detail,
                beer.style,
                beer.released ?: beer.series.name,
                beer.abv?.let { formatDecimal(it) } ?: unknown,
                beer.ibu?.toString() ?: unknown,
            ),
        ).setView(content)
        .setPositiveButton(R.string.drink) { _, _ ->
            val tap = Tap(beer.name, beer.style, beer.id, beer.url, beer.imageUrl)
            showSizeDialog(beer.name, actions.sizes()) { actions.onRecord(tap, it, beer.abv) }
        }.setNeutralButton(R.string.open_site) { _, _ ->
            startActivity(Intent(Intent.ACTION_VIEW, beer.url.toUri()))
        }.setNegativeButton(R.string.close, null)
        .show()
}

/**
 * お気に入りに足すか, 外すボタンを作る (押すと, 切り替えて, ボタンの字と絵も変える).
 *
 * @receiver 画面
 * @param key ビールのキー
 * @param favorites お気に入りの操作
 * @return ボタン
 */
fun Activity.favoriteButton(
    key: String,
    favorites: FavoriteActions,
): MaterialButton =
    MaterialButton(
        this,
        null,
        com.google.android.material.R.attr.materialButtonOutlinedStyle,
    ).apply {
        val bind = {
            val has = favorites.has(key)
            setText(if (has) R.string.favorite_remove else R.string.favorite_add)
            setIconResource(if (has) R.drawable.ic_favorite else R.drawable.ic_favorite_border)
        }
        bind()
        setOnClickListener {
            favorites.toggle(key)
            bind()
        }
    }
