/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.app

import android.view.Gravity
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView

/** ドロワーの見出しの余白 (dp). */
private const val HEADER_PADDING_DP = 24

/**
 * 左から引き出すメニュー (ナビゲーションドロワー).
 *
 * 上のバーの左のボタン (三本線) か, 画面の左端から引き出す. 戻るの操作で, 閉じる. 項目は, 下のタブと同じ.
 *
 * @param activity 画面
 * @param content 中身 (下のタブの画面)
 * @param onSelect 項目を選んだ (下のタブのメニューの ID を受け取る)
 */
class AppDrawer(
    activity: AppCompatActivity,
    content: View,
    onSelect: (Int) -> Unit,
) {
    /** 画面をまとめた View. */
    val view = DrawerLayout(activity)

    private val navigation =
        NavigationView(activity).apply {
            addHeaderView(
                TextView(activity).apply {
                    setTextAppearance(
                        com.google.android.material.R.style.TextAppearance_Material3_TitleLarge,
                    )
                    setText(R.string.app_name)
                    val padding = activity.dp(HEADER_PADDING_DP)
                    setPadding(padding, padding, padding, padding)
                },
            )
            inflateMenu(R.menu.bottom_nav)
            setNavigationItemSelectedListener {
                view.closeDrawer(this)
                onSelect(it.itemId)
                true
            }
        }
    private val toggle =
        ActionBarDrawerToggle(activity, view, R.string.drawer_open, R.string.drawer_close)

    init {
        view.addView(content)
        view.addView(
            navigation,
            DrawerLayout.LayoutParams(
                DrawerLayout.LayoutParams.WRAP_CONTENT,
                DrawerLayout.LayoutParams.MATCH_PARENT,
                Gravity.START,
            ),
        )
        view.addDrawerListener(toggle)
        val back =
            object : OnBackPressedCallback(false) {
                override fun handleOnBackPressed() = view.closeDrawer(navigation)
            }
        view.addDrawerListener(
            object : DrawerLayout.SimpleDrawerListener() {
                override fun onDrawerOpened(drawerView: View) {
                    back.isEnabled = true
                }

                override fun onDrawerClosed(drawerView: View) {
                    back.isEnabled = false
                }
            },
        )
        activity.onBackPressedDispatcher.addCallback(activity, back)
        // 上のバーの三本線のボタンを押したら, 開け閉めする
        activity.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(
                    menu: Menu,
                    menuInflater: MenuInflater,
                ) = Unit

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
                    toggle.onOptionsItemSelected(menuItem)
            },
            activity,
        )
        activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toggle.syncState()
        // ドロワーを, 上のバーの下から始める (画面は, 上のバーの下まで広がるので, 見出しが隠れないように)
        navigation.setOnApplyWindowInsetsListener { target, insets ->
            val top = insets.getInsets(WindowInsets.Type.systemBars()).top
            val params = target.layoutParams as ViewGroup.MarginLayoutParams
            if (params.topMargin != top) {
                params.topMargin = top
                target.layoutParams = params
            }
            insets
        }
    }

    /**
     * 今の画面の項目を, 選んだ見た目にする.
     *
     * @param itemId 下のタブのメニューの ID
     */
    fun select(itemId: Int) = navigation.setCheckedItem(itemId)
}
