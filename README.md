# hop-kotan

忽布古丹醸造 (HOP KOTAN BREWING) のビールのデータを、サイトから取得して、ソートやフィルタをして見る Android アプリ。毎日のタップリスト (忽布古丹のタップルーム BEER KOTAN) から、**その日に飲んだビールを記録**できます。

> 非公式の個人用のアプリです。ビールのデータの出典は、忽布古丹醸造のサイト (https://hopkotan.com/) です。

## 仕組み

```
GitHub Actions (毎日) ──取得──> hopkotan.com
       │
       └─ data/taplist.json (今日のタップリスト) と data/beers.json (ORIGINALS と FREEDOMS) を更新
                     │
Android アプリ ◀── JSON をダウンロード (raw.githubusercontent.com)。端末で、ソートとフィルタ
       └─ 飲んだビールの記録は、端末に保存
```

アプリがサイトを直接取りにいくことはありません。サイトへのアクセスは、1 日に 1 回 (タップリスト) と、1 週間に 1 回 (ビール) だけです。

## データ

| ファイル | 内容 | 更新 |
|:---|:---|:---|
| [data/taplist.json](data/taplist.json) | 今日のタップリスト (更新日、量と価格、ビール名、スタイル) | 毎日 2 回 |
| [data/taplist/](data/taplist) | 日ごとのタップリストの履歴 (`YYYY-MM-DD.json`) と、その日付の目次 (`index.json`) | 毎日 2 回 |
| [data/beers.json](data/beers.json) | ORIGINALS と FREEDOMS のビール (名前、スタイル、発売月、ABV、IBU、元のページの URL) | 毎日 (新しいものがあれば) |

画像と説明文は、保存しません (画像は、サイトの URL を参照します)。

## 構成

| モジュール | 内容 |
|:---|:---|
| `core` | データの型、HTML の解析 (jsoup)、取得、JSON、飲んだ記録の集計 (Kotlin の JVM。カバレッジ 100%) |
| `scraper` | データを取得する、コマンドの入口 |
| `app` | Android アプリ (5 つのタブ: 今日、最近、ビール、記録、メニュー) |

## アプリ

| タブ | 内容 |
|:---|:---|
| 今日 | 今日のタップリスト (写真、ABV、IBU つき)。ビールをタップして、量 (M か L) を選ぶと、飲んだビールを記録します。今日の杯数、量、金額、純アルコール量も出ます |
| (全てのタブ) | **星 (5 つ) の評価**。今日と最近は長く押す、記録は押す、ビールは詳細の画面で、星を付けます。ビールごとの評価で、一覧に ★ を出します。端末の中だけに保存します (`ratings.json`) |
| 最近 | 最近 14 日分のタップリストを、日ごとに見ます (見るだけ) |
| ビール | 全てのビール (写真つき。右に IBU と ABV の印)。並べ方 (発売月、名前、ABV、IBU、スタイル、評価。昇順と降順)。名前とスタイルの検索は画面に出し、「絞り込み」のボタンで開くポップアップで、スタイル、シリーズ、発売年、ABV と IBU の下限、評価 (星) の下限、今日のタップのみを選びます。タップすると、詳細と「飲んだ」「サイトを開く」 |
| メニュー | お店の QR コードを読み込んで、メニューのページを、アプリの中 (WebView) に表示します。QR コードは、Google のコードスキャナで読みます (カメラの権限は要りません。Google Play 開発者サービスが必要です)。表示するのは https のページだけで、QR コードのホストの外へ移るときは、ブラウザで開きます。注文は、メニューのページの中で行います (アプリは、注文を代行しません)。テーブルや注文の情報は保存せず、Cookie も消します |
| 記録 | 飲んだ記録を、日ごとに表示します (杯数、量、金額、純アルコール量)。長く押すと、消せます |

起動のときに、データ (JSON) を GitHub から取得して、端末に保存します。写真は、忽布古丹醸造のサイトの画像を、そのとき取得して、端末に残します (アプリには、含みません)。Material 3 の配色 (琥珀色。ライトとダーク) です。飲んだ記録は、端末の中だけに保存します (どこにも送りません)。Android 13 以上が要ります。

## 使い方 (開発)

Docker があれば、動きます (Android Studio も、JDK も、要りません)。

```bash
export UID GID=$(id -g)
docker compose run --rm build          # APK (app/build/outputs/apk/plain/debug/app-plain-debug.apk)
docker compose run --rm test           # 単体テスト
docker compose run --rm scrape-taplist # data/taplist.json を更新
docker compose run --rm scrape-beers   # data/beers.json を更新 (初回は、時間がかかる)
```

そのほかのコマンドは、[SETUP.md](SETUP.md) と [.claude/CLAUDE.md](.claude/CLAUDE.md) にあります。

## アイコン

オリジナルのアイコン (ビールのグラスとホップ) を使います。作者の端末のビルドだけは、private のサブモジュール `private/` の、忽布古丹醸造のマークを使います (権利は、忽布古丹醸造にあります。このリポジトリには、含みません。配布もしません)。

## ライセンス

[GPL-3.0-or-later](LICENSE)
