#!/bin/bash
# data/ のファイルが, リポジトリの内容と違うときだけ, GitHub の Contents API でコミットする.
# API で作ったコミットは, GitHub が署名するので, Verified になる (git push のコミットは, ならない).
#
# 使い方: GH_TOKEN=<トークン> scripts/publish-data.sh <ファイル>... (リポジトリの直下から. Actions で使う)
# 環境変数: GITHUB_REPOSITORY (owner/repo), BRANCH (既定は main), MESSAGE (コミットの件名)

set -euo pipefail

BRANCH="${BRANCH:-main}"
MESSAGE="${MESSAGE:-chore(data): Update the data}"

if [ $# -lt 1 ]; then
    echo "Usage: $0 <file>..." >&2
    exit 1
fi

for file in "$@"; do
    [ -f "$file" ] || { echo "Error: $file not found" >&2; exit 1; }
    api="repos/${GITHUB_REPOSITORY}/contents/${file}"

    # 今のファイルの sha と内容 (なければ, 空)
    remote="$(gh api "${api}?ref=${BRANCH}" 2>/dev/null || true)"
    sha="$(printf '%s' "$remote" | jq -r '.sha // empty')"
    if [ -n "$sha" ]; then
        current="$(gh api -H 'Accept: application/vnd.github.raw+json' "${api}?ref=${BRANCH}")"
        if [ "$current" = "$(cat "$file")" ]; then
            echo "${file}: no change"
            continue
        fi
    fi

    args=(-X PUT "$api" -f "message=${MESSAGE}" -f "branch=${BRANCH}" -f "content=$(base64 -w0 "$file")")
    if [ -n "$sha" ]; then
        args+=(-f "sha=${sha}")
    fi
    gh api "${args[@]}" --jq '.commit.html_url'
    echo "${file}: committed"
done
