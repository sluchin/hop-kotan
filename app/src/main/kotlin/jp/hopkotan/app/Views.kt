/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.app

import android.app.AlertDialog
import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import jp.hopkotan.core.PriceSize
import java.text.NumberFormat
import java.util.Locale

/** 画面の端と, 内容の間の余白 (dp). */
const val CONTENT_PADDING_DP = 12

/**
 * パネルが, アプリに頼む操作.
 *
 * @property onRecord 飲んだビールを記録する (名前などを持つビール, 量, ABV を受け取る)
 * @property onDelete 記録を消す (記録の ID を受け取る)
 * @property sizes 選べる量と価格
 * @property images 写真の読み込み
 * @property imageOf ビールの ID から, 写真の URL を探す (なければ null)
 * @property ratings 星の評価
 */
class PanelActions(
    val onRecord: (jp.hopkotan.core.Tap, PriceSize, Double?) -> Unit,
    val onDelete: (String) -> Unit,
    val sizes: () -> List<PriceSize>,
    val images: ImageLoader,
    val imageOf: (String?) -> String?,
    val ratings: RatingActions,
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
 * 一覧の 1 行の内容.
 *
 * @property title 1 行目 (名前)
 * @property subtitle 2 行目 (スタイルなど)
 * @property extra 3 行目 (小さな字. なければ null)
 * @property badge 右端の印 (ABV や価格. なければ null)
 * @property badgeLead 右端の印の左の印 (IBU. 同じ形. なければ null)
 * @property imageUrl 写真の URL (なければ, 代わりの絵)
 * @property showImage 写真の場所を出すか (日の見出しは, 出さない)
 */
data class RowModel(
    val title: String,
    val subtitle: String,
    val extra: String? = null,
    val badge: String? = null,
    val badgeLead: String? = null,
    val imageUrl: String? = null,
    val showImage: Boolean = true,
    val isNew: Boolean = false,
)

/**
 * 写真つきの行 (名前, スタイル, 小さな字, 右端の印) を並べる, 一覧のアダプタを作る.
 *
 * @param context コンテキスト
 * @param items 項目
 * @param images 写真の読み込み
 * @param model 項目から, 行の内容を作る
 * @param enabled 項目を選べるか (見出しは選べない)
 * @return アダプタ
 */
fun <T> rowAdapter(
    context: Context,
    items: List<T>,
    images: ImageLoader,
    model: (T) -> RowModel,
    enabled: (T) -> Boolean = { true },
): ArrayAdapter<T> =
    object : ArrayAdapter<T>(context, R.layout.item_row, items) {
        override fun getView(
            position: Int,
            convertView: View?,
            parent: ViewGroup,
        ): View {
            val row =
                convertView
                    ?: LayoutInflater.from(context).inflate(R.layout.item_row, parent, false)
            val content = model(items[position])
            row.findViewById<TextView>(R.id.title).text = content.title
            row.findViewById<TextView>(R.id.new_mark).visibility =
                if (content.isNew) View.VISIBLE else View.GONE
            row.findViewById<TextView>(R.id.subtitle).text = content.subtitle
            row.findViewById<TextView>(R.id.extra).bind(content.extra)
            row.findViewById<TextView>(R.id.badge_lead).bind(content.badgeLead)
            row.findViewById<TextView>(R.id.badge).bind(content.badge)
            val thumb = row.findViewById<ImageView>(R.id.thumb)
            thumb.visibility = if (content.showImage) View.VISIBLE else View.GONE
            images.load(thumb, content.imageUrl)

            return row
        }

        override fun isEnabled(position: Int): Boolean = enabled(items[position])
    }

/** 文字があれば表示して, なければ隠す. */
private fun TextView.bind(text: String?) {
    this.text = text
    visibility = if (text == null) View.GONE else View.VISIBLE
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
    // 行を素早く 2 回タップすると, 量の画面が重なって開き, 2 杯記録されるので, 開いている間は開かない
    if (sizeDialog?.isShowing == true) return
    val labels = sizes.map { getString(R.string.size_item, it.label, it.ml, it.yen) }
    sizeDialog =
        AlertDialog
            .Builder(this)
            .setTitle(title)
            .setItems(labels.toTypedArray()) { _, index -> onChoose(sizes[index]) }
            .setNegativeButton(R.string.cancel, null)
            .show()
}

/** いま開いている, 量を選ぶ画面 (同時に 2 つ開かないため). */
private var sizeDialog: AlertDialog? = null

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
 * システムバーとアクションバーの下に, 内容が隠れないよう, 余白を入れる (下は, 入れない).
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

/**
 * 本文の文字の見た目 (Material 3) の, 文字の部品を作る.
 *
 * @receiver コンテキスト
 * @param small 小さい字にするか
 * @return 文字の部品
 */
fun Context.bodyText(small: Boolean = false): TextView =
    TextView(this).apply {
        setTextAppearance(
            if (small) {
                com.google.android.material.R.style.TextAppearance_Material3_BodySmall
            } else {
                com.google.android.material.R.style.TextAppearance_Material3_BodyMedium
            },
        )
    }
