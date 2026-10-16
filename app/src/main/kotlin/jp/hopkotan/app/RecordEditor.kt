/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.app

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.text.InputType
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import jp.hopkotan.core.DrinkRecord
import jp.hopkotan.core.PriceSize
import java.time.LocalDate
import java.time.LocalTime

/** 変える画面の余白 (dp). */
private const val FORM_PADDING_DP = 20

/**
 * 飲んだ記録を変える画面を出す (日, 時刻, 量, 価格, メモ. 日は今日以外にも変えられる).
 *
 * @receiver コンテキスト
 * @param record 変える記録
 * @param sizes 選べる量と価格 (記録の量がなければ, 記録の量も選べる)
 * @param onSave 保存の処理 (変えた記録を受け取る)
 */
fun Context.showRecordEditor(
    record: DrinkRecord,
    sizes: List<PriceSize>,
    onSave: (DrinkRecord) -> Unit,
) {
    val form = RecordForm(this, record, sizes)
    AlertDialog
        .Builder(this)
        .setTitle(record.beerName)
        .setView(form.view)
        .setPositiveButton(R.string.save) { _, _ -> onSave(form.result()) }
        .setNegativeButton(R.string.cancel, null)
        .show()
}

/**
 * 記録を変える入力欄.
 *
 * @property context コンテキスト
 * @property record 変える記録
 */
private class RecordForm(
    private val context: Context,
    private val record: DrinkRecord,
    sizes: List<PriceSize>,
) {
    private val current = PriceSize(record.sizeLabel, record.ml, record.yen)
    private val choices = if (current in sizes) sizes else sizes + current
    private var date = LocalDate.parse(record.date)
    private var time: LocalTime? = record.time?.let { LocalTime.parse(it) }
    private var size = current
    private val dateButton = Button(context)
    private val timeButton = Button(context)
    private val yen = field(InputType.TYPE_CLASS_NUMBER, yenText(record.yen), R.string.edit_yen)
    private val abv =
        field(
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL,
            record.abv?.let { context.getString(R.string.edit_abv_value, it) }.orEmpty(),
            R.string.edit_abv,
        )
    private val note = field(InputType.TYPE_CLASS_TEXT, record.note, R.string.edit_note)

    /** 入力欄を並べた画面. */
    val view: LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = context.dp(FORM_PADDING_DP)
            setPadding(pad, pad, pad, 0)
            listOf(dateButton, timeButton, sizeSpinner(), yen, abv, note).forEach { addView(it) }
        }

    init {
        dateButton.setOnClickListener { pickDate() }
        timeButton.setOnClickListener { pickTime() }
        refresh()
    }

    /**
     * 入力した内容で, 記録を作る.
     *
     * @return 変えた記録 (価格が数でなければ, 量の価格. ABV が数でなければ, 分からない)
     */
    fun result(): DrinkRecord =
        record.copy(
            date = date.toString(),
            time = time?.toString(),
            sizeLabel = size.label,
            ml = size.ml,
            yen = yen.text.toString().toIntOrNull() ?: size.yen,
            abv = abv.text.toString().toDoubleOrNull(),
            note = note.text.toString().trim(),
        )

    private fun yenText(value: Int): String = context.getString(R.string.edit_yen_value, value)

    private fun field(
        type: Int,
        value: String,
        hint: Int,
    ): EditText =
        EditText(context).apply {
            inputType = type
            setText(value)
            setHint(hint)
        }

    private fun refresh() {
        dateButton.text = date.toString()
        timeButton.text = time?.toString() ?: context.getString(R.string.edit_time_none)
    }

    private fun pickDate() {
        DatePickerDialog(
            context,
            { _, y, m, d ->
                date = LocalDate.of(y, m + 1, d)
                refresh()
            },
            date.year,
            date.monthValue - 1,
            date.dayOfMonth,
        ).show()
    }

    private fun pickTime() {
        val at = time ?: LocalTime.now()
        TimePickerDialog(
            context,
            { _, h, m ->
                time = LocalTime.of(h, m)
                refresh()
            },
            at.hour,
            at.minute,
            true,
        ).show()
    }

    private fun sizeSpinner(): Spinner =
        Spinner(context).apply {
            adapter =
                ArrayAdapter(
                    context,
                    android.R.layout.simple_spinner_dropdown_item,
                    choices.map { context.getString(R.string.size_item, it.label, it.ml, it.yen) },
                )
            setSelection(choices.indexOf(current))
            onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long,
                    ) {
                        if (choices[position] != size) {
                            size = choices[position]
                            yen.setText(yenText(size.yen))
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) = Unit
                }
        }
}
