/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.app

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.view.Gravity
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.view.isVisible
import com.google.android.material.button.MaterialButton
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import jp.hopkotan.core.Beer
import jp.hopkotan.core.MenuUrl
import jp.hopkotan.core.Tap

/** ログのタグ. */
private const val TAG = "Menu"

/**
 * 店の QR コードを読み込んで, メニューのページをアプリの中 (WebView) に表示するタブ.
 *
 * QR コードは, Google のコードスキャナ (Google Play 開発者サービス) で読む (カメラの権限は, 要らない).
 * 表示するのは, `https` のページだけ. 注文のページは, 認証や支払いで別のサイトを通るので, `https` の
 * ページは, どのホストでもアプリの中で続ける (`https` 以外の, ほかのアプリを開くリンクだけ外で開く).
 * テーブルや注文の情報は, 端末に保存しない (アプリを閉じたら, Cookie と保存したデータを消す). 画面が作り直される
 * だけのとき (回転など) は, 別の画面 (タブ) へ移っても, QR コードを読み直さずに続けられるよう, Cookie を残して,
 * 開いていたページを開き直す.
 *
 * 注文のページ (Air レジ オーダーなど) は, Cookie でセッションを持つので, WebView でも, 他のサイトの
 * Cookie を受け付ける (WebView は, 最初は受け付けず, Chrome では動くのに, 認証エラーになることがある).
 * セッションは WebView の中にだけできて, ブラウザ (Chrome) とは共有しないので, 注文はアプリの中で完結させる.
 * 原因を調べられるように, ページの移動, HTTP のエラー, ページの JavaScript のエラーをログに出す.
 *
 * 「注文数を選ぶ」を押したら, そのビールを飲んだ記録に入れる画面を出す ([MenuRecorder]). 上の「記録」で,
 * 今日のタップから手で記録できる.
 *
 * @property activity 画面
 * @property actions アプリに頼む操作
 * @property taps 今日のタップ
 * @property beers ORIGINALS と FREEDOMS のビール
 * @param onOpened メニューで見つけたビールのうち, 今日のタップリストにないものを足す (あとから開栓したビール)
 */
class MenuPanel(
    private val activity: Activity,
    actions: PanelActions,
    taps: () -> List<Tap>,
    beers: () -> List<Beer>,
    onOpened: (List<Tap>) -> Unit,
) {
    private val hint =
        TextView(activity).apply {
            setText(R.string.menu_hint)
            setPadding(0, activity.dp(HINT_PADDING_DP), 0, activity.dp(HINT_PADDING_DP))
        }
    private val host = TextView(activity)
    private val reload =
        MaterialButton(activity, null, android.R.attr.borderlessButtonStyle).apply {
            setText(R.string.menu_reload)
            isVisible = false
            setOnClickListener { web.reload() }
        }
    private val web = createWebView()
    private val recorder = MenuRecorder(activity, actions, taps, beers, web, onOpened)
    private val manual =
        MaterialButton(activity, null, android.R.attr.borderlessButtonStyle).apply {
            setText(R.string.menu_record)
            isVisible = false
            setOnClickListener { recorder.pickOther() }
        }

    /** タブの内容. */
    val view: LinearLayout =
        LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            addView(
                LinearLayout(activity).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    addView(
                        MaterialButton(activity).apply {
                            setText(R.string.menu_scan)
                            setOnClickListener { scan() }
                        },
                    )
                    addView(host, LinearLayout.LayoutParams(0, WRAP, 1f))
                    addView(manual)
                    addView(reload)
                },
            )
            addView(hint)
            addView(web, LinearLayout.LayoutParams(MATCH, 0, 1f))
        }

    /**
     * WebView の履歴を戻れるか.
     *
     * @return 戻れるなら true
     */
    fun canGoBack(): Boolean = web.canGoBack()

    /** WebView の履歴を 1 つ戻る. */
    fun goBack() = web.goBack()

    /**
     * 今開いているメニューのページの URL (メニューをまだ開いていなければ null).
     *
     * 画面が作り直されても (回転, ダークモードの切り替え, メモリ不足での終了), 同じページを開き直すために,
     * 画面の状態として一時的に預ける (端末には保存しない).
     */
    val currentUrl: String?
        get() = web.url?.takeIf { MenuUrl.opensInApp(it) }

    /**
     * 画面が作り直されたときに, 預けておいたページを開き直す (QR コードを読み直さなくてよい).
     *
     * Cookie は, 画面が作り直されても消さないので (`destroy` の `clear`), セッションが続く.
     *
     * @param url [currentUrl] で預けた URL
     */
    fun restore(url: String?) {
        if (MenuUrl.parse(url) != null) {
            AppLog.i(TAG, "Restoring the menu page")
            open(url, clear = false)
        }
    }

    /**
     * WebView を片付ける.
     *
     * @param clear Cookie と, 保存したデータを消すなら true. 画面が作り直されるだけのときは false にして,
     *   作り直したあとも, セッションを続ける
     */
    fun destroy(clear: Boolean) {
        if (clear) {
            clearData()
        }
        web.destroy()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(): WebView =
        WebView(activity).apply {
            visibility = View.GONE
            // 注文のページは, JavaScript で動くので, 有効にする (ホストを限っているので, 許す)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.javaScriptCanOpenWindowsAutomatically = false
            // セッションの Cookie を, 他のサイトのものも受け付ける (Chrome と同じにする)
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            // ページの JavaScript のエラーと警告を, ログに出す (情報のメッセージは, デバッグ版だけ)
            webChromeClient =
                object : WebChromeClient() {
                    override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                        // 「注文数を選ぶ」が押されたときの, 注文の画面の文字と, メニューのページの文字は, ログに書かない
                        val body = message.message()
                        val text = "Console ${message.messageLevel()}: $body"
                        when {
                            body.startsWith(
                                ORDER_MESSAGE,
                            ) -> recorder.onOrder(body.removePrefix(ORDER_MESSAGE))
                            body.startsWith(
                                MENU_MESSAGE,
                            ) -> recorder.onMenu(body.removePrefix(MENU_MESSAGE))
                            message.messageLevel() == ConsoleMessage.MessageLevel.ERROR ||
                                message.messageLevel() == ConsoleMessage.MessageLevel.WARNING ->
                                AppLog.w(TAG, text)
                            else -> AppLog.d(TAG) { text }
                        }
                        return true
                    }
                }
            webViewClient =
                object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest,
                    ): Boolean {
                        val url = request.url.toString()
                        val inside = MenuUrl.opensInApp(url)
                        // 問い合わせ (テーブルやセッションの値) は, ログに書かない
                        AppLog.d(
                            TAG,
                        ) {
                            "Navigate to ${request.url.scheme}://${request.url.host}${request.url.path}"
                        }
                        if (inside) {
                            request.url.host?.let {
                                host.text =
                                    activity.getString(R.string.menu_host, it)
                            }
                        } else {
                            AppLog.i(TAG, "Opening a ${request.url.scheme} link outside the app")
                            openOutside(url)
                        }

                        return !inside
                    }

                    override fun onReceivedHttpError(
                        view: WebView,
                        request: WebResourceRequest,
                        errorResponse: WebResourceResponse,
                    ) {
                        AppLog.w(
                            TAG,
                            "HTTP ${errorResponse.statusCode} from ${request.url.host}${request.url.path}",
                        )
                    }

                    override fun onReceivedError(
                        view: WebView,
                        request: WebResourceRequest,
                        error: WebResourceError,
                    ) {
                        AppLog.w(TAG, "Could not load ${request.url.host}: ${error.description}")
                    }

                    override fun onPageFinished(
                        view: WebView,
                        url: String,
                    ) = recorder.watchOrder()
                }
        }

    /** QR コードを読む (読めたらメニューを開く). */
    private fun scan() {
        val options =
            GmsBarcodeScannerOptions
                .Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
        GmsBarcodeScanning
            .getClient(activity, options)
            .startScan()
            .addOnSuccessListener { open(it.rawValue) }
            .addOnFailureListener {
                toast(
                    activity.getString(R.string.menu_failed, it.message ?: it.javaClass.simpleName),
                )
            }
    }

    /**
     * QR コードの内容が, 表示してよい URL なら, メニューを開く.
     *
     * @param raw QR コードの内容
     * @param clear 前の Cookie と保存したデータを消すなら true (QR コードを読み直したとき). 作り直された画面で,
     *   同じページを開き直すときは false (セッションを続ける)
     */
    private fun open(
        raw: String?,
        clear: Boolean = true,
    ) {
        val url = MenuUrl.parse(raw)
        if (url == null) {
            toast(activity.getString(R.string.menu_invalid))

            return
        }
        if (clear) {
            clearData()
        }
        host.text = activity.getString(R.string.menu_host, MenuUrl.host(url))
        hint.isVisible = false
        reload.isVisible = true
        manual.isVisible = true
        web.isVisible = true
        web.loadUrl(url)
    }

    /**
     * アプリの外で開く (ほかのアプリを開くリンク). `intent:` のリンクは, 開けるアプリがなければ,
     * 代わりの `https` のページ (`browser_fallback_url`) を, アプリの中で開く.
     *
     * @param url リンク
     */
    private fun openOutside(url: String) {
        val intent =
            runCatching { Intent.parseUri(url, Intent.URI_INTENT_SCHEME) }
                .getOrElse { Intent(Intent.ACTION_VIEW, url.toUri()) }
                .apply {
                    // ほかのアプリの, 公開していない画面は開かない (ブラウザから開けるものだけ)
                    addCategory(Intent.CATEGORY_BROWSABLE)
                    component = null
                    selector = null
                }
        try {
            activity.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            val fallback = intent.getStringExtra(FALLBACK_URL)?.takeIf { MenuUrl.opensInApp(it) }
            AppLog.w(TAG, "No app for the link (fallback: ${fallback != null})", e)
            fallback?.let { web.loadUrl(it) }
        }
    }

    /** Cookie と, 保存したデータ (キャッシュ, ローカルストレージ) を消す. */
    private fun clearData() {
        CookieManager.getInstance().removeAllCookies(null)
        WebStorage.getInstance().deleteAllData()
        web.clearCache(true)
        web.clearHistory()
    }

    private fun toast(message: String) = Toast.makeText(activity, message, Toast.LENGTH_LONG).show()

    private companion object {
        const val MATCH = LinearLayout.LayoutParams.MATCH_PARENT
        const val WRAP = LinearLayout.LayoutParams.WRAP_CONTENT
        const val HINT_PADDING_DP = 12

        /** `intent:` のリンクの, 開けるアプリがないときの, 代わりの URL の名前. */
        const val FALLBACK_URL = "browser_fallback_url"
    }
}
