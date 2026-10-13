/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.app

import android.app.AlertDialog
import android.content.Context
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import jp.hopkotan.core.PriceSize
import java.text.NumberFormat
import java.util.Locale

/** 画面の端と, 内容の間の余白 (dp). */
const val CONTENT_PADDING_DP = 12

/** 項目の 2 行目の, 文字の大きさ (sp). */
private const val SECOND_LINE_SP = 13f

/**
 * パネルが, アプリに頼む操作.
 *
 * @property onRecord 飲んだビールを記録する (名前などを持つビール, 量, ABV を受け取る)
 * @property onDelete 記録を消す (記録の ID を受け取る)
 * @property sizes 選べる量と価格
 */
class PanelActions(
    val onRecord: (jp.hopkotan.core.Tap, PriceSize, Double?) -> Unit,
    val onDelete: (String) -> Unit,
    val sizes: () -> List<PriceSize>,
)

/**
 * 金額を, 3 桁ごとの区切りつきの文字列にする.
 *
 * @param yen 円
 * @return 文字列 (`1,650`)
 */
fun formatYen(yen: Int): String = NumberFormat.getIntegerInstance(Locale.JAPAN).format(yen)

/**
 * 小数 1 桁の文字列にする.
 *
 * @param value 値
 * @return 文字列 (`5.0`)
 */
fun formatDecimal(value: Double): String = String.format(Locale.JAPAN, "%.1f", value)

/**
 * 2 行の項目 (1 行目は太字, 2 行目は小さい字) を並べる, 一覧のアダプタを作る.
 *
 * @param context コンテキスト
 * @param items 項目
 * @param first 1 行目の文字
 * @param second 2 行目の文字
 * @param enabled 項目を選べるか (見出しは選べない)
 * @return アダプタ
 */
fun <T> twoLineAdapter(
    context: Context,
    items: List<T>,
    first: (T) -> CharSequence,
    second: (T) -> CharSequence,
    enabled: (T) -> Boolean = { true },
): ArrayAdapter<T> =
    object : ArrayAdapter<T>(
        context,
        android.R.layout.simple_list_item_2,
        android.R.id.text1,
        items,
    ) {
        override fun getView(
            position: Int,
            convertView: View?,
            parent: ViewGroup,
        ): View {
            val row = super.getView(position, convertView, parent)
            val item = items[position]
            row.findViewById<TextView>(android.R.id.text1).text = first(item)
            row.findViewById<TextView>(android.R.id.text2).apply {
                text = second(item)
                textSize = SECOND_LINE_SP
            }

            return row
        }

        override fun isEnabled(position: Int): Boolean = enabled(items[position])
    }

/**
 * 選択肢のスピナーを作る.
 *
 * @receiver コンテキスト
 * @param items 選択肢の文字
 * @param onSelected 選んだときの処理
 * @return スピナー
 */
fun Context.spinner(
    items: List<String>,
    onSelected: () -> Unit,
): Spinner =
    Spinner(this).apply {
        adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, items)
        onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) = onSelected()

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
    }

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
    val labels = sizes.map { getString(R.string.size_item, it.label, it.ml, it.yen) }
    AlertDialog
        .Builder(this)
        .setTitle(title)
        .setItems(labels.toTypedArray()) { _, index -> onChoose(sizes[index]) }
        .setNegativeButton(R.string.cancel, null)
        .show()
}

/**
 * ボタンを作る.
 *
 * @receiver コンテキスト
 * @param label 文字の資源
 * @param onClick 押したときの処理
 * @return ボタン
 */
fun Context.button(
    label: Int,
    onClick: () -> Unit,
): Button =
    Button(this).apply {
        setText(label)
        setOnClickListener { onClick() }
    }

/**
 * 部品に, 小さな見出しを付ける.
 *
 * @receiver コンテキスト
 * @param label 見出しの資源
 * @param view 部品
 * @return 見出しと部品を縦に並べたもの
 */
fun Context.labeled(
    label: Int,
    view: View,
): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        addView(TextView(context).apply { setText(label) })
        addView(view)
    }

/**
 * 部品を, 同じ幅で横に並べる.
 *
 * @receiver コンテキスト
 * @param views 部品
 * @return 横に並べた部品
 */
fun Context.row(vararg views: View): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        views.forEach {
            addView(it, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
    }

/**
 * システムバーとアクションバーの下に, 内容が隠れないよう, 余白を入れる.
 *
 * @receiver コンテキスト
 * @param root 画面の一番外側の部品
 */
fun Context.applyInsets(root: View) {
    val padding = dp(CONTENT_PADDING_DP)
    val actionBarSize =
        TypedValue().let {
            theme.resolveAttribute(android.R.attr.actionBarSize, it, true)
            TypedValue.complexToDimensionPixelSize(it.data, resources.displayMetrics)
        }
    root.setOnApplyWindowInsetsListener { target, insets ->
        val bars = insets.getInsets(WindowInsets.Type.systemBars())
        target.setPadding(
            bars.left + padding,
            bars.top + actionBarSize + padding,
            bars.right + padding,
            bars.bottom + padding,
        )
        insets
    }
}
