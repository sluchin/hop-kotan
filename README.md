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
| [data/taplist.json](data/taplist.json) | 今日のタップリスト (更新日、量と価格、ビール名、スタイル) | 毎日 |
| [data/beers.json](data/beers.json) | ORIGINALS と FREEDOMS のビール (名前、スタイル、発売月、ABV、IBU、元のページの URL) | 毎週 |

画像と説明文は、保存しません (画像は、サイトの URL を参照します)。

## 構成

| モジュール | 内容 |
|:---|:---|
| `core` | データの型、HTML の解析 (jsoup)、取得、JSON、飲んだ記録の集計 (Kotlin の JVM。カバレッジ 100%) |
| `scraper` | データを取得する、コマンドの入口 |
| `app` | Android アプリ (3 つのタブ: 今日、ビール、記録) |

## アプリ

| タブ | 内容 |
|:---|:---|
| 今日 | 今日のタップリスト (写真、ABV、IBU つき)。ビールをタップして、量 (M か L) を選ぶと、飲んだビールを記録します。今日の杯数、量、金額、純アルコール量も出ます |
| ビール | 全てのビール (写真つき)。並べ方 (発売月、名前、ABV、IBU、スタイル。昇順と降順)、絞り込み (スタイル、シリーズ、発売年、ABV と IBU の下限、今日のタップのみ、名前とスタイルの検索)。タップすると、詳細と「飲んだ」「サイトを開く」 |
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
