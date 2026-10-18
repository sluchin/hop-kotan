/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.app

import android.Manifest
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import jp.hopkotan.core.Beer
import jp.hopkotan.core.DrinkLog
import jp.hopkotan.core.Favorites
import jp.hopkotan.core.NewTaps
import jp.hopkotan.core.OpenedTaps
import jp.hopkotan.core.PriceSize
import jp.hopkotan.core.Ratings
import jp.hopkotan.core.Tap
import jp.hopkotan.core.TapEntries
import jp.hopkotan.core.TapList
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.concurrent.Executors

/** ログのタグ. */
private const val TAG = "Data"

/** 量と価格が取れていないときの, 既定 (サイトの M と L). */
private val DEFAULT_SIZES = listOf(PriceSize("M", 310, 750), PriceSize("L", 420, 900))

/**
 * 忽布古丹醸造のビールを見て, 飲んだビールを記録する画面 (5 つのタブ: 今日, 最近, ビール, 記録, メニュー).
 * 左から引き出すメニュー ([AppDrawer]) から, お気に入りのビールの画面を開く.
 *
 * データは, GitHub の JSON (GitHub Actions が, 毎日更新する) を, 起動のときに取得する. 取得できなければ,
 * 端末に保存したデータを表示する. 飲んだ記録は, 端末の中だけに保存する.
 */
class MainActivity : AppCompatActivity() {
    private lateinit var store: DataStore
    private lateinit var status: TextView
    private lateinit var pull: SwipeRefreshLayout
    private lateinit var today: TodayPanel
    private lateinit var historyPanel: HistoryPanel
    private lateinit var beersPanel: BeersPanel
    private lateinit var drinksPanel: DrinksPanel
    private lateinit var menuPanel: MenuPanel
    private lateinit var favoritesPanel: FavoritesPanel
    private lateinit var nav: BottomNavigationView
    private var current = 0
    private lateinit var panels: List<View>

    private var beers = emptyList<Beer>()
    private var taplist: TapList? = null
    private var history = emptyList<TapList>()
    private var drinks = DrinkLog()
    private var ratings = Ratings()

    /** サイトのタップリストを取得したあとに, 開栓したビール (店のメニューで注文して, 見つかったもの). */
    private var opened = OpenedTaps()
    private var favorites = Favorites()
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var images: ImageLoader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = DataStore(this)
        images = ImageLoader(this)
        beers = store.loadBeers()
        taplist = store.loadTaplist()
        history = store.loadHistory()
        drinks = store.loadDrinks()
        ratings = store.loadRatings()
        opened = OpenedStore.load(this)
        favorites = FavoritesStore.load(this)
        setContentView(buildScreen())
        // 新しいビールの通知: 権限を求めて (許可されなくても動く), 定期の確認を予約する
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
            .launch(Manifest.permission.POST_NOTIFICATIONS)
        NewBeerWorker.schedule(this)
        render(null)
        refresh()
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (current == MENU_INDEX && menuPanel.canGoBack()) {
                        menuPanel.goBack()
                    } else if (current == FAVORITES_INDEX) {
                        // お気に入りの画面からは, 前のタブに戻る
                        show(NAV_IDS.indexOf(nav.selectedItemId))
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            },
        )
    }

    override fun onDestroy() {
        menuPanel.destroy()
        executor.shutdown()
        images.shutdown()
        super.onDestroy()
    }

    /** パネルが, アプリに頼む操作. */
    private val actions by lazy {
        PanelActions(
            ::record,
            RecordActions(
                { id ->
                    drinks = drinks.remove(id)
                    save()
                },
                { record ->
                    drinks = drinks.update(record)
                    save()
                },
            ),
            { taplist?.sizes?.ifEmpty { null } ?: DEFAULT_SIZES },
            images,
            { id -> beers.firstOrNull { it.id == id }?.imageUrl },
            RatingActions({ ratings.of(it) }, { ratings }, ::rate),
        )
    }

    /** お気に入りの操作 (切り替えたら, 保存して, 画面を更新する. 書き込みは, 別のスレッドで行う). */
    private val favoriteActions =
        FavoriteActions({ it in favorites }) { key ->
            favorites = favorites.toggle(key)
            val snapshot = favorites
            executor.execute { FavoritesStore.save(this, snapshot) }
            render(null)
        }

    private fun buildScreen(): View {
        today = TodayPanel(this, actions)
        historyPanel = HistoryPanel(this, actions)
        beersPanel = BeersPanel(this, actions, favoriteActions)
        drinksPanel = DrinksPanel(this, actions)
        menuPanel = MenuPanel(this, actions, { todayTaps?.taps.orEmpty() }, { beers }, ::addOpened)
        favoritesPanel = FavoritesPanel(this, actions, favoriteActions)
        panels =
            listOf(
                today.view,
                historyPanel.view,
                beersPanel.view,
                drinksPanel.view,
                menuPanel.view,
                favoritesPanel.view,
            )
        status =
            TextView(this).apply {
                setTextAppearance(
                    com.google.android.material.R.style.TextAppearance_Material3_BodySmall,
                )
            }

        val content = FrameLayout(this).apply { panels.forEach { addView(it) } }
        nav =
            BottomNavigationView(this).apply {
                inflateMenu(R.menu.bottom_nav)
                // 項目が 4 つ以上だと, 既定では選んだものにしか名前が出ないので, 全てに出す
                labelVisibilityMode = NavigationBarView.LABEL_VISIBILITY_LABELED
                setOnItemSelectedListener {
                    show(NAV_IDS.indexOf(it.itemId))
                    true
                }
            }
        show(0)
        // 下に引いて更新する. 一覧が先頭でないとき (と, メニューの Web ページ) は, 引いても更新しない
        pull =
            SwipeRefreshLayout(this).apply {
                addView(content)
                setOnChildScrollUpCallback { _, _ -> current == MENU_INDEX || canScrollUp(content) }
                setOnRefreshListener { refresh() }
            }

        val root =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                addView(status, LinearLayout.LayoutParams(MATCH, WRAP))
                addView(pull, LinearLayout.LayoutParams(MATCH, 0, 1f))
                addView(nav)
                applyInsets(this)
            }

        return AppDrawer(this, root) { show(FAVORITES_INDEX) }.view
    }

    /**
     * パネルを切り替える (お気に入りの画面のときは, 下のタブを, どれも選んでいない見た目にする).
     *
     * @param index パネルの番号 (下のタブの順. お気に入りは [FAVORITES_INDEX])
     */
    private fun show(index: Int) {
        current = index
        nav.menu.setGroupCheckable(0, index != FAVORITES_INDEX, true)
        panels.forEachIndexed { i, panel ->
            panel.visibility =
                if (i == index) View.VISIBLE else View.GONE
        }
    }

    /** データを取得して, 画面を更新する (通信は, 別のスレッドで行う). */
    private fun refresh() {
        status.setText(R.string.status_loading)
        pull.isRefreshing = true
        executor.execute {
            val result = runCatching { store.download() }
            // 履歴は, 取得できなくても, 端末にあるものを表示する
            val newHistory =
                runCatching {
                    store.downloadHistory()
                }.getOrElse {
                    AppLog.w(TAG, "Could not download the history, using the saved one", it)
                    store.loadHistory()
                }
            runOnUiThread {
                pull.isRefreshing = false
                result
                    .onSuccess { (newBeers, newTaplist) ->
                        AppLog.i(
                            TAG,
                            "Downloaded ${newBeers.size} beers and ${newHistory.size} taplists",
                        )
                        beers = newBeers
                        taplist = newTaplist
                        history = newHistory
                        render(null)
                    }.onFailure {
                        AppLog.w(TAG, "Could not download the data", it)
                        render(it.message ?: it.javaClass.simpleName)
                    }
            }
        }
    }

    /**
     * メニューで見つけたビールのうち, 今日のタップリストにないものを足して, 保存する (あとから開栓したビール).
     *
     * @param found メニューで見つけたビール
     */
    private fun addOpened(found: List<Tap>) {
        val list = taplist ?: return
        val added = found.fold(opened) { acc, tap -> acc.add(list, tap) }
        if (added == opened) return
        val count = added.taps.size - (if (added.date == opened.date) opened.taps.size else 0)
        opened = added
        executor.execute { OpenedStore.save(this, added) }
        if (count > 0) {
            Toast.makeText(this, getString(R.string.opened_added, count), Toast.LENGTH_SHORT).show()
        }
        render(null)
    }

    /** 今日のタップリスト (サイトのタップリストに, あとから開栓したビールを足したもの. まだなければ null). */
    private val todayTaps: TapList?
        get() = taplist?.let { opened.applyTo(it) }

    /** 全てのタブと, 状態の表示を更新する. */
    private fun render(error: String?) {
        val list = todayTaps
        val entries = list?.let { TapEntries.of(it, beers) }.orEmpty()
        val previous = list?.let { NewTaps.previousOf(history, it) }
        val newTaps =
            if (list != null &&
                previous != null
            ) {
                NewTaps.find(previous, list)
            } else {
                emptyList()
            }
        today.update(entries, list?.date, drinks.summary(LocalDate.now().toString()), newTaps)
        historyPanel.update(history, beers)
        val onTapIds = entries.mapNotNull { it.beer?.id }.toSet()
        beersPanel.update(beers, onTapIds)
        favoritesPanel.update(beers, favorites, onTapIds)
        drinksPanel.update(drinks, beers)
        status.text =
            when {
                error != null -> getString(R.string.status_failed, error)
                list == null -> getString(R.string.status_empty)
                else -> getString(R.string.status_ready, list.date, list.taps.size, beers.size)
            }
    }

    private fun record(
        tap: Tap,
        size: PriceSize,
        abv: Double?,
    ) {
        drinks =
            drinks.add(
                DrinkLog
                    .record(
                        UUID.randomUUID().toString(),
                        LocalDate.now().toString(),
                        tap,
                        size,
                        abv,
                    ).copy(time = LocalTime.now().truncatedTo(ChronoUnit.MINUTES).toString()),
            )
        save()
        Toast.makeText(this, getString(R.string.recorded, tap.name), Toast.LENGTH_SHORT).show()
    }

    /** 星の評価を変えて, 保存する (書き込みは, 別のスレッドで行う). */
    private fun rate(
        key: String,
        value: Int,
    ) {
        ratings = ratings.set(key, value)
        val snapshot = ratings
        executor.execute { store.saveRatings(snapshot) }
        render(null)
    }

    /** 記録を保存して, 画面を更新する (書き込みは, 別のスレッドで行う). */
    private fun save() {
        val snapshot = drinks
        executor.execute { store.saveDrinks(snapshot) }
        render(null)
    }

    private companion object {
        const val MENU_INDEX = 4
        const val FAVORITES_INDEX = 5
        val NAV_IDS =
            listOf(R.id.nav_today, R.id.nav_recent, R.id.nav_beers, R.id.nav_drinks, R.id.nav_menu)
        const val MATCH = LinearLayout.LayoutParams.MATCH_PARENT
        const val WRAP = LinearLayout.LayoutParams.WRAP_CONTENT
    }
}

/** 表示されている部分に, 上へスクロールできる一覧や画面があるか (更新の引き下げと, 区別する). */
private fun canScrollUp(view: View): Boolean =
    view.isVisible &&
        (
            view.canScrollVertically(-1) ||
                (view is ViewGroup && view.children.any { canScrollUp(it) })
        )
