# セットアップ

## Docker で開発する

1. Docker をインストールする。
2. ホストのユーザー ID を、環境変数にエクスポートする (生成物が、root 所有にならないように):

   ```bash
   export UID GID=$(id -g)
   ```
3. サービスを実行する (必ず `--rm` を付ける):

   | コマンド | 内容 |
   |:---|:---|
   | `docker compose run --rm build` | 全てのモジュールのビルド |
   | `docker compose run --rm test` | 単体テスト |
   | `docker compose run --rm coverage` | `core` の行と分岐が 100% か |
   | `docker compose run --rm format` / `format-fix` | 整形の確認 / 整形して書き換える |
   | `docker compose run --rm analyze` | 静的解析 (detekt) |
   | `docker compose run --rm docs` | ドキュメント (`core/build/dokka/html/index.html`) |
   | `docker compose run --rm scrape-taplist` | `data/taplist.json` を更新 |
   | `docker compose run --rm scrape-beers` | `data/beers.json` を更新 |
   | `docker compose run --rm clean` | 生成物を消す |
   | `docker compose run --rm dev` | 開発シェル |

## アイコンの素材 (private のサブモジュール)

公式のマークから作るアイコンを使うときは、private のリポジトリ `hop-kotan-assets` (自分だけが、読める) を、サブモジュール `private/` として取り込む。取り込まなくても、オリジナルのアイコンで、ビルドできる。

```bash
git submodule update --init private      # 取り込む (SSH で private のリポジトリを読める鍵が要る)
git submodule update --remote private    # 最新にする
```

CI (GitHub Actions) は、サブモジュールを取り込まない。マークを入れたビルドは、手元だけで使い、配布しない。

## GitHub Actions

| ワークフロー | 内容 |
|:---|:---|
| `build.yml` (CI) | push と PR で、`build` `test` `coverage` `format` `analyze` `docs` を実行する |
| `update-data.yml` | 毎日、タップリストを取得する。毎週月曜に、ビールも取得する。手動 (Run workflow) でも実行できる。変わったときだけ、GitHub の Contents API で `data/*.json` をコミットする (API で作ったコミットは、GitHub が署名する) |

`update-data.yml` は、`permissions: contents: write` を宣言しているので、`data/*.json` をコミットできる。コミットできないときは、リポジトリ (または組織) の Settings > Actions > General > Workflow permissions が、書き込みを禁止していないか確認する。

## ツールの版 (実際に使うもの)

| 項目 | 版 |
|:---|:---|
| JDK | 17 (Docker のイメージ) |
| Gradle | 9.8.1 (ラッパー) |
| Kotlin | 2.4.21 |
| jsoup | 1.23.2 |
| kotlinx-serialization-json | 1.11.0 |
| ktlint (プラグイン) / detekt / Dokka / JaCoCo | 14.2.0 / 1.23.8 / 2.2.0 / 0.8.15 |
