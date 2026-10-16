# セットアップ

## Docker で開発する

1. Docker をインストールする。
2. ホストのユーザー ID を環境変数にエクスポートする (生成物が root 所有にならないように):

   ```bash
   export UID GID=$(id -g)
   ```
3. サービスを実行する (必ず `--rm` を付ける):

   | コマンド | 内容 |
   |:---|:---|
   | `docker compose run --rm build` | 全てのモジュールのビルド (APK: `app/build/outputs/apk/plain/debug/app-plain-debug.apk`)。`private/kotan-mark.svg` (サブモジュール) があれば、マークの APK (`marked`。配布しない) も作る |
   | `docker compose run --rm build-marked` | 忽布古丹醸造のマークのアイコンの APK だけ (`private/` が要る) |
   | `docker compose run --rm test` | 単体テスト |
   | `docker compose run --rm coverage` | `core` の行と分岐が 100% か |
   | `docker compose run --rm format` / `format-fix` | 整形の確認 / 整形して書き換える |
   | `docker compose run --rm analyze` | 静的解析 (detekt と Android Lint) |
   | `docker compose run --rm docs` | ドキュメント (`core/build/dokka/html/index.html`) |
   | `docker compose run --rm scrape-taplist` | `data/taplist.json` と、履歴 `data/taplist/<日付>.json` と `data/taplist/index.json` を更新 |
   | `docker compose run --rm scrape-beers` | `data/beers.json` を更新 |
   | `docker compose run --rm clean` | 生成物を消す |
   | `docker compose run --rm dev` | 開発シェル |

## 実機へのインストール (ホストの adb)

```bash
adb install -r app/build/outputs/apk/plain/debug/app-plain-debug.apk
```

ホストに `adb` を入れて、スマートフォンの「USB デバッグ」を有効にして、USB でつなぐ。

## アイコンの素材 (private のサブモジュール)

公式のマークから作るアイコンを使うときは private のリポジトリ `hop-kotan-assets` (自分だけが、読める) をサブモジュール `private/` として取り込む。取り込まなくても、オリジナルのアイコンで、ビルドできる。

```bash
git submodule update --init private      # 取り込む (SSH で private のリポジトリを読める鍵が要る)
git submodule update --remote private    # 最新にする
```

CI (GitHub Actions) はサブモジュールを取り込まない。マークを入れたビルドは手元だけで使い、配布しない。

## GitHub Actions

| ワークフロー | 内容 |
|:---|:---|
| `build.yml` (CI) | push と PR で `build` `test` `coverage` `format` `analyze` `docs` を実行する |
| `update-data.yml` | 毎日 12:30 と 17:30 (JST) にタップリストを取得する (土日祝日は 12 時、平日は 17 時から開くため)。同じ回にビール (ORIGINALS と FREEDOMS と、タップに出たもの) の新しいものを探して、あれば保存する。タップリストとビールは別のファイルに保存する。手動 (Run workflow) でも実行できる。変わったときだけ、GitHub の Contents API で `data/*.json` をコミットする (API で作ったコミットは GitHub が署名する) |

`update-data.yml` は `permissions: contents: write` を宣言しているので、`data/*.json` をコミットできる。コミットできないときはリポジトリ (または組織) の Settings > Actions > General > Workflow permissions が書き込みを禁止していないか確認する。

## ツールの版 (実際に使うもの)

| 項目 | 版 |
|:---|:---|
| JDK | 17 (Docker のイメージ) |
| Android Gradle Plugin / compileSdk / minSdk | 9.4.1 / 37 / 33 |
| Material Components / AppCompat | 1.14.0 / 1.7.1 |
| Gradle | 9.8.1 (ラッパー) |
| Kotlin | 2.4.21 |
| jsoup | 1.23.2 |
| kotlinx-serialization-json | 1.11.0 |
| ktlint (プラグイン) / detekt / Dokka / JaCoCo | 14.2.0 / 1.23.8 / 2.2.0 / 0.8.15 |
