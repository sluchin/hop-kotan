# プロジェクト設定

忽布古丹醸造 (HOP KOTAN BREWING) のサイトから、ビールのデータを取得して、ソートやフィルタをして見る Android アプリ。毎日の「今日のタップリスト」から、その日に飲んだビールを記録できる。

```
GitHub Actions (毎日) ──取得──> hopkotan.com   (/taproom/ 毎日 2 回, /originals/ と /freedoms/ は新しい分だけ)
       │
       └─ 解析して data/taplist.json (と履歴 data/taplist/<日付>.json) と data/beers.json を更新 (GitHub の Contents API でコミット)
                     │
Android アプリ ◀── JSON を HTTPS でダウンロード (raw.githubusercontent.com)。端末で、ソートとフィルタ
       └─ 飲んだビールの記録は、端末に保存 (DrinkLog)
```

- `core`: データの型 (`Beer` `TapList` `DrinkRecord`)、HTML の解析 (jsoup)、取得 (`Scraper` `JdkHtmlFetcher`)、JSON (`CatalogJson`)、飲んだ記録の集計 (`DrinkLog`)。Kotlin の JVM。カバレッジ 100%。
- `scraper`: コマンドの入口 (`taplist <dir>` と `beers <dir>`)。薄く保つ。
- `app`: Android アプリ (Kotlin。Material Components の View。Compose は使わない)。画面の下のナビゲーションで切り替える 5 つのタブ: **今日** (タップリスト。タップして、量 (M か L) を選ぶと、飲んだビールを記録)、**最近** (最近 14 日分のタップリストを日ごとに表示。履歴は `data/taplist/<日付>.json` と目次 `index.json`。過去の日は変わらないので、端末にあれば取り直さない)、**ビール** (全ビール。画面には件数と「絞り込み」のボタンだけ。ポップアップに文字の検索、並べ方 (評価を含む。昇順と降順)、スタイル、シリーズ、発売年、ABV と IBU の下限、評価の下限、今日のタップのみ。タップすると詳細。「飲んだ」で記録、「サイトを開く」)、**記録** (日ごとの一覧と集計。長く押すと, 変える (日, 時刻, 量, 価格, メモ. その日以外にも変えられる. `RecordEditor`) か, 消す)、**メニュー** (店の QR コードを Google のコードスキャナ (`play-services-code-scanner`。カメラの権限は不要) で読んで、WebView に表示する。`MenuPanel`。表示するのは https だけ。ホストの判定は `core` の `MenuUrl`。ホストの外はブラウザで開く。注文は代行しない。下の帯 (`MenuBar`) にページの文字 (`evaluateJavascript`) から、`core` の `MenuMatcher` で見つけたビールを並べ、タップで飲んだ記録に入れる (文字は保存しない。手で選ぶ「ほかを記録」もある)。テーブルや注文の情報は保存せず、Cookie とキャッシュを消す)。起動のときに、`data/*.json` をダウンロードして、端末に保存する (オフラインでは、保存したデータを表示する)。飲んだ記録は端末の `drinks.json` にだけ保存する (どこにも送らない)。星 (5 つ) の評価はビールごとに、端末の `ratings.json` にだけ保存する (キーはビールの ID か、`name:<名前>`。`core` の `Ratings`)。一覧の行には写真 (`ImageLoader`)、スタイル、ABV の印を出す。新しいビールの通知: WorkManager (`NewBeerWorker`。3 時間ごと) が、最新のタップリストと履歴を取得して、前の日のタップリストと比べ (`core` の `NewTaps`)、前の日になかったビールがあれば通知する (`NewBeerNotifier`。同じ日に 1 回。通知の権限がなければ、しない)。画面の部品は薄く保つ (ソートと絞り込みは `core` の `BeerQuery`)。

**Android Studio は使わない。** ビルド、テスト、整形、静的解析、ドキュメントの生成は全てコマンドラインで、Docker の中で実行する。

## サイトの扱い (守ること)

- `robots.txt` は `/wp-admin/` だけを禁止している (2026-10-13 に確認)。それ以外のページを取得してよい。利用規約は確認していない。**個人で使う範囲にする。**
- **サイトに負荷をかけない**: 取得の間隔は 2 秒以上 (`JdkHtmlFetcher`)。取得済みのビールは取り直さない (`known`)。名前を名乗る User-Agent (`hop-kotan/0.1 (+https://github.com/sluchin/hop-kotan)`) を付ける。
- **サイトの画像と文章をリポジトリに置かない**: 保存するのは名前、スタイル、ABV、IBU、発売月、価格、元ページの URL、画像の URL だけ。画像は元の URL を参照する。説明文は保存しない。
- **写真は実行のときに、サイトの URL から取得する** (`ImageLoader`)。リポジトリにも、アプリにも、含めない。一覧にはサイトの小さい画像 (240px) を使う。同時に取得するのは 2 つまで。取得した画像は端末のキャッシュに残して、2 回目からは通信しない。**写真つきの APK を公開しない** (権利は忽布古丹醸造にある。公開するなら、許可を得る)。
- サイトの作りが変わって、1 件も読めなくなったら、`ScrapeException` で失敗にする (Actions が失敗して、気づける)。

## アイコンの素材 (忽布古丹醸造のマーク)

アイコンは 2 つある。`private/` (サブモジュール。private のリポジトリ `hop-kotan-assets`) があれば、公式のマーク (`private/kotan-mark.svg` など) から作る。なければ、このリポジトリのオリジナルのアイコン (ビールのグラスとホップ) を使う (別のプライベートリポジトリをサブモジュールとして置く方式は esp32-thermo-system の `app/thermo-node/private` と同じ)。

- **公式のマークをこのリポジトリ (公開) にコミットしない。** 履歴にも残さない。マークの権利は忽布古丹醸造にあり、許可は得ていない。`private/` の中身は `hop-kotan-assets` に置く。
- **マークを入れてビルドした APK を公開しない** (CI の成果物、リリース、配布)。CI はサブモジュールを取り込まない (`actions/checkout` の `submodules` を指定しない)。
- マークがなくても、ビルドできること。`private/kotan-mark.svg` があれば、`app/build.gradle.kts` がプロダクトフレーバー `marked` を足す (マークの SVG から、アイコンの絵を作る `GenerateMarkIcon`)。なければ、`plain` (オリジナルのアイコン。`app/src/plain/res/`) だけ。
- スクリーンショットや、READMEの画像に公式のマークを写さない。

## コマンド

> **クラウドのセッション (claude.ai/code。環境変数 `CLAUDE_CODE_REMOTE` が `true`) では、`docker compose run --rm <サービス>` を使わず、`scripts/cloud-docker.sh run <サービス>` で実行する。** 詳しくは thermo-term の `.claude/CLAUDE.md` と同じ。

- `docker compose run` には必ず `--rm` を付ける (終わったコンテナを残さないため)。`UID` と `GID` をシェルでエクスポートしておく。
- ビルド: `docker compose run --rm build` (デバッグ APK: `app/build/outputs/apk/plain/debug/app-plain-debug.apk`。`private/kotan-mark.svg` があれば、マークの APK (`app/build/outputs/apk/marked/debug/app-marked-debug.apk`。配布しない) も作る。なければ plain だけ。CI は plain だけ)
- マークのアイコンの APK だけ (`private/` が要る。配布しない): `docker compose run --rm build-marked` (`app/build/outputs/apk/marked/debug/app-marked-debug.apk`)
- 単体テスト: `docker compose run --rm test`
- カバレッジ (`core` の行と分岐が 100%): `docker compose run --rm coverage`
- 整形の確認: `docker compose run --rm format` / 整形して書き換える: `docker compose run --rm format-fix`
- 静的解析 (detekt と Android Lint): `docker compose run --rm analyze`
- ドキュメント (Dokka): `docker compose run --rm docs` (`core/build/dokka/html/index.html`)
- データの取得: `docker compose run --rm scrape-taplist` (`data/taplist.json`) / `docker compose run --rm scrape-beers` (`data/beers.json`。初回は詳細のページを 1 件ずつ取るので、時間がかかる)
- クリーン: `docker compose run --rm clean` / 開発シェル: `docker compose run --rm dev`

## ログ

- ログは `AppLog` (`AppLog.kt`) だけを使う (`android.util.Log` を直接呼ばない)。タグは、ファイルごとの `private const val TAG` (機能の名前)。
- `AppLog.d` はデバッグ版だけ出す (判定は、関数の中の `BuildConfig.DEBUG`。引数は、文字列を返す関数にして、リリース版では文字列も作らない)。`AppLog.i` (最低限の情報)、`AppLog.w` (失敗したが続けられる)、`AppLog.e` (利用者の操作が失敗した) は、リリース版でも出す。
- パスワード、トークン、鍵などの秘密は、どのレベルでも書かない。ログの文字は英語。
- 落ちたときの例外は、Android が logcat に出し、Google Play では Play Console の Android Vitals が集める (コードは要らない)。見方は `SETUP.md` の「ログを見る」。

## コーディング規約

ktlint (Kotlin 公式のスタイル) と、detekt (既定の規則) に従う。迷ったら、ツールが正しい。Kotlin 公式のコーディング規約の日本語訳と、このプロジェクトでの違いは `CODING_STYLE.md`。

- **インデントは空白 4 つ。1 行は 100 桁まで** (`.editorconfig`)。整形は `format-fix` が行う (手で、整形しない)。
- **ファイルの先頭に著作権とライセンスを書く**:

  ```kotlin
  /*
   * Copyright (c) 2026 Tetsuya Higashi
   *
   * SPDX-License-Identifier: GPL-3.0-or-later
   */
  ```
- **コメントと KDoc は日本語。** 句読点は「, 」(カンマと空白) と「.」(ピリオド) にする (「、」と「。」は使わない)。読点は読みやすさのために、必要なところだけに打つ (「は」「が」「を」「も」の直後には基本的に打たない)。ログのメッセージ、例外のメッセージ、識別子は英語。
- **公開 (public) の宣言には KDoc を付ける** (`docs` が `Undocumented` の警告で失敗する)。`@param`、`@return`、`@throws` を書く。`private` にも、意味と単位が分かる説明を付ける。
- **KDoc の中で、`[s]` や `[%]` のように、角括弧で単位を書かない** (リンクとして解釈される)。`(s)` や `(%)` と書く。
- **マジックナンバーを使わない** (detekt の `MagicNumber`)。`private const val` で名前を付ける。
- **警告をゼロにする。** Kotlin の警告はエラーになる (`allWarningsAsErrors`)。警告を抑える (`@Suppress`) ときは理由をコメントに書く。
- 変更しない値は `val` にする。`!!` はテスト以外で、使わない。汎用の例外を握りつぶさない。

## Android アプリの落とし穴

- `minSdk` 33、`compileSdk` と `targetSdk` は 37、`buildToolsVersion` は `37.0.0` (Docker のイメージの SDK は読み取り専用)。足りない SDK のコンポーネントは `Dockerfile` に足して、イメージを作り直す。
- 配色は `res/values/colors.xml` と `res/values-night/colors.xml` (琥珀色。ライトとダーク)。Material の部品は `Theme.HopKotan` (`Theme.Material3.DayNight`) で使う。
- 画面 (`app`) にはシステムバーの余白 (edge-to-edge) を入れる (`applyInsets`)。画面は実機で確認する (エミュレータは置いていない)。
- 文字は全て `res/values/strings.xml` に置く (Lint が `UnusedResources` と `SetTextI18n` をエラーにする)。使わない文字列は残さない。
- 数の表や、選択肢の表 (`val`) は detekt の `MagicNumber` から外してある (`config/detekt/detekt.yml`)。
- `app` にはテストを置かない (薄い層)。`core` の行と分岐は 100%。
- 実機で未確認: 画面の見た目、GitHub からのダウンロード、記録の保存。

## 開発フロー

1. Push 時に GitHub Actions (`.github/workflows/build.yml`) が開発用の Docker イメージで、`build` / `test` / `coverage` / `format` / `analyze` / `docs` を実行する。ログはアーティファクト (`<サービス名>_log`) に保存する。3 日より前の実行は削除する (`cleanup`)。
2. データの更新 (`.github/workflows/update-data.yml`): 毎日 12:30 と 17:30 (JST) にタップリストを取得する。同じ回に、ビール (ORIGINALS と FREEDOMS。タップに出た ORIGINALS plus と HARVEST BREW も) の新しいものを探す (タップに出た新しいビールを、その日のうちに、IBU と ABV つきで見られるように)。タップリストとビールは別のファイルに保存する。変わったときだけ、GitHub の Contents API で `data/*.json` をコミットする (API で作ったコミットは GitHub が署名するので、Verified になる)。

### コード修正時のルール

コードを修正したら、次の順に全て成功することを確認する。**どれも、警告を出さない。**

1. `format-fix` (整形) → 2. `format` (確認) → 3. `analyze` → 4. `test` → 5. `coverage` (100%) → 6. `build` → 7. `docs` (KDoc を変えたときは必ず)

- ビルドやテストを実行できなかったときはそのことを報告する (成功したとは書かない)。
- **例外**: 変更が文書だけの場合はビルドしない。
- ユーザーに依頼されない限り、`git commit` / `git push` は行わない。

## テスト

JVM 上で実行する (JUnit 5 と `kotlin-test`)。サイトにはアクセスしない。ページは作り物の HTML (`Fixtures`) と、偽の取得 (`HtmlFetcher`) で返す。サイトの文章はテストにも置かない。`JdkHtmlFetcher` は手元の HTTP サーバ (`com.sun.net.httpserver`) で確認する。

- 配置: `core/src/test/kotlin/` (同じパッケージ)。クラス名は `<対象>Test`。
- カバレッジ: `core` の行と分岐は 100%。ソースを足したら、テストを足す (正常系と、失敗)。
- **実サイトでの確認は別に必要**: サイトの HTML が変わると、解析が合わなくなる。`scrape-taplist` と `scrape-beers` を実行して、件数を確認する。

## コミットガイドライン

- コミットメッセージは英語で記述
- **件名は Conventional Commits の形式で書く**: `type(scope): description` (type: `feat` `fix` `docs` `style` `refactor` `test` `build` `ci` `chore`。scope は `core` / `scraper` / `app` / `docker` / `gradle` / `data`)。description は英語で、命令形。
- 件名のあとに空行を 1 行入れて、本文を書く。本文はダッシュ/ハイフンで始まる箇条書き
- コミットメッセージに `Co-Authored-By` または Claude 帰属行を含めない
- **rebase したあとの push は `git push --force-with-lease --force-if-includes origin <ブランチ名>` で行う** (`--force` は使わない)。ユーザーに依頼されたときだけ、push する。
