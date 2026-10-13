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
| `app` | Android アプリ (これから作る) |

## 使い方 (開発)

Docker があれば、動きます (Android Studio も、JDK も、要りません)。

```bash
export UID GID=$(id -g)
docker compose run --rm test           # 単体テスト
docker compose run --rm scrape-taplist # data/taplist.json を更新
docker compose run --rm scrape-beers   # data/beers.json を更新 (初回は、時間がかかる)
```

そのほかのコマンドは、[SETUP.md](SETUP.md) と [.claude/CLAUDE.md](.claude/CLAUDE.md) にあります。

## ライセンス

[GPL-3.0-or-later](LICENSE)
