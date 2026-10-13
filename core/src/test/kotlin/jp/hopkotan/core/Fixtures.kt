/*
 * Copyright (c) 2026 Tetsuya Higashi
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package jp.hopkotan.core

/** テスト用の HTML (サイトの作りを真似た, 作り物. サイトの文章は含まない). */
internal object Fixtures {
    /** タップルームのページの, 2 つのタップ (リンクあり, なし) と空の名前の項目. */
    fun taproom(date: String = "2026.10.13"): String =
        """
        <dl><dt><span>更新日</span></dt><dd>$date</dd></dl>
        <dl class="taplist-price">
          <div class="taplist-price__unit medium">
            <dt><span class="taplist-price__label">M</span><span class="taplist-price__size">310ml</span></dt>
            <dd class="">750 ~</dd>
          </div>
          <div class="taplist-price__unit large">
            <dt><span class="taplist-price__label">L</span><span class="taplist-price__size">420ml</span></dt>
            <dd>900 ~</dd>
          </div>
          <div class="taplist-price__unit"><dt><span class="taplist-price__label">X</span></dt><dd>ask</dd></div>
          <div class="taplist-price__unit"><dt><span class="taplist-price__label">Y</span>
            <span class="taplist-price__size">100ml</span></dt><dd>ask</dd></div>
        </dl>
        <div class="taproom-taplist">
          <div class="taproom-taplist__item"><div class="taproom-taplist__thum">
            <a href="https://hopkotan.com/originals/#alpha "><img src="/img/tap-alpha.jpg"></a></div>
            <div class="taproom-taplist__name">alpha</div><div class="taproom-taplist__style">PILSNER</div></div>
          <div class="taproom-taplist__item"><div class="taproom-taplist__thum">
            <a href="/freedoms/test-beer/"><img src="/img/tap-test.jpg"></a></div>
            <div class="taproom-taplist__name">Test Beer</div><div class="taproom-taplist__style">Hazy IPA</div></div>
          <div class="taproom-taplist__item"><div class="taproom-taplist__thum"></div>
            <div class="taproom-taplist__name">No Link</div><div class="taproom-taplist__style">Saison</div></div>
          <div class="taproom-taplist__item"><div class="taproom-taplist__name"></div></div>
        </div>
        """.trimIndent()

    /** FREEDOMS の一覧の 1 ページ. [next] があれば, 次のページへのリンクを付ける. */
    fun freedoms(
        items: List<Triple<String, String, String>>,
        month: String = "Sep. 2026",
        next: String? = null,
    ): String {
        val body =
            items.joinToString("\n") { (name, style, slug) ->
                """<div class="item animated"><div class="item-cat"><a href="/brand/x/">$month</a></div>
                <div class="item-thum"><a href="/freedoms/$slug/"><img src="/img/$slug.jpg"></a></div>
                <div class="item-name">$name</div><div class="item-style">$style</div></div>"""
            }
        val link =
            next
                ?.let { """<a class="nextpostslink" rel="next" href="$it">next</a>""" }
                .orEmpty()

        return """<div class="itemlist list">$body</div>$link"""
    }

    /** 詳細のページ. */
    fun detail(
        abv: String?,
        ibu: String?,
    ): String {
        val rows =
            listOfNotNull(
                abv?.let { "<dt>ABV</dt><dd>$it</dd>" },
                ibu?.let { "<dt>IBU</dt><dd>$it</dd>" },
            )
        val spec =
            if (rows.isEmpty()) {
                ""
            } else {
                """<dl class="beer-spec">${rows.joinToString(
                    "",
                )}</dl>"""
            }

        return "<div class=\"beer-detail\"><p>text</p>$spec</div>"
    }

    /** ORIGINALS のページ: id が外側の div にある 1 つ目と, 同じ要素にある 2 つ目と, id がない 3 つ目. */
    val originals: String =
        """
        <div id="wrap"><div id="content">
        <div id="alpha" class="animatedParent">
          <div class="originals-col alpha"><div class="originals-cont"><div class="product-img">
            <img src="/img/alpha.png"></div><div class="product-info">
            <div class="product-style">PILSNER</div>
            <dl class="beer-spec"><dt>ABV</dt><dd>5.0</dd><dt>IBU</dt><dd>25</dd></dl>
            <div class="product-info__link"><a href="https://hopkotan.com/originals/alpha/">more</a></div>
          </div></div></div>
        </div>
        <div id="beta" class="originals-col beta"><div class="product-info">
          <div class="product-style">PALE ALE</div>
        </div></div>
        <div id="gamma" class="originals-col gamma"><div class="product-img"><img></div>
          <div class="product-info"><div class="product-style">LAGER</div>
          <div class="product-info__link"><a>more</a></div></div></div>
        </div></div>
        """.trimIndent()

    /** id がない ORIGINALS のビール. */
    const val ORIGINALS_NO_ID: String =
        """<div class="product-info"><div class="product-style">X</div></div>"""
}
