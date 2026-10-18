/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.app

import android.annotation.SuppressLint
import android.app.Activity
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
 * 表示するのは, `https` のページだけ. QR コードのホスト (とそのサブドメイン) の外へ移るときは,
 * ブラウザで開く. テーブルや注文の情報は, 保存しない (画面を離れたら, Cookie と保存したデータを消す).
 *
 * 注文のページ (Air レジ オーダーなど) は, Cookie でセッションを持つので, WebView でも, 他のサイトの
 * Cookie を受け付ける (WebView は, 最初は受け付けず, Chrome では動くのに, 認証エラーになることがある).
 * セッションは WebView の中にだけできて, ブラウザ (Chrome) とは共有しないので, 注文はアプリの中で完結させる.
 * 原因を調べられるように, ページの移動, HTTP のエラー, ページの JavaScript のエラーを, ログに出す.
 *
 * 下に, 飲んだビールを記録する帯 ([MenuBar]) を出す.
 *
 * @property activity 画面
 * @property actions アプリに頼む操作
 * @property taps 今日のタップ
 * @property beers ORIGINALS と FREEDOMS のビール
 */
class MenuPanel(
    private val activity: Activity,
    actions: PanelActions,
    taps: () -> List<Tap>,
    beers: () -> List<Beer>,
) {
    private var base: String? = null
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
    private val bar = MenuBar(activity, actions, taps, beers, web).apply { view.isVisible = false }

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
                    addView(reload)
                },
            )
            addView(hint)
            addView(web, LinearLayout.LayoutParams(MATCH, 0, 1f))
            addView(bar.view)
        }

    /**
     * WebView の履歴を戻れるか.
     *
     * @return 戻れるなら true
     */
    fun canGoBack(): Boolean = web.canGoBack()

    /** WebView の履歴を 1 つ戻る. */
    fun goBack() = web.goBack()

    /** WebView を片付ける (Cookie と, 保存したデータを消す). */
    fun destroy() {
        clearData()
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
                        val text = "Console ${message.messageLevel()}: ${message.message()}"
                        when (message.messageLevel()) {
                            ConsoleMessage.MessageLevel.ERROR,
                            ConsoleMessage.MessageLevel.WARNING,
                            -> AppLog.w(TAG, text)
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
                        val inside = base?.let { MenuUrl.isInside(url, it) } == true
                        // 問い合わせ (テーブルやセッションの値) は, ログに書かない
                        AppLog.d(
                            TAG,
                        ) { "Navigate to ${request.url.host}${request.url.path} (inside: $inside)" }
                        if (!inside) {
                            AppLog.i(TAG, "Opening ${request.url.host} in the browser")
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
                    ) = bar.find()
                }
        }

    /** QR コードを読む (読めたら, メニューを開く). */
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

    /** QR コードの内容が, 表示してよい URL なら, メニューを開く. */
    private fun open(raw: String?) {
        val url = MenuUrl.parse(raw)
        if (url == null) {
            toast(activity.getString(R.string.menu_invalid))

            return
        }
        clearData()
        base = url
        host.text = activity.getString(R.string.menu_host, MenuUrl.host(url))
        hint.isVisible = false
        reload.isVisible = true
        bar.view.isVisible = true
        web.isVisible = true
        web.loadUrl(url)
    }

    /** アプリの外 (ブラウザ) で開く (`https` だけ). */
    private fun openOutside(url: String) {
        if (MenuUrl.parse(url) != null) {
            activity.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
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
    }
}
