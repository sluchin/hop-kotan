# 開発用のイメージ (dev)
FROM ubuntu:24.04 AS dev

ENV DEBIAN_FRONTEND=noninteractive

# ログの日本語 (Kotlin のメッセージ, KDoc の警告など) を, 文字化けさせない
ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8

# JDK 17 (ホストの JDK は使わない)
RUN apt-get update && apt-get install -y --no-install-recommends \
    openjdk-17-jdk-headless \
    unzip \
    curl \
    ca-certificates \
    git \
    && rm -rf /var/lib/apt/lists/*

# Gradle のキャッシュ. 名前付きボリュームを mount する (空のボリュームは, この権限を引き継ぐ)
ENV GRADLE_USER_HOME=/gradle-home
RUN mkdir -p /gradle-home && chmod 777 /gradle-home

# コンテナはホストユーザーで実行されるが, イメージ内の /workspace は, そのユーザーの所有ではないため
RUN git config --system --add safe.directory '*'

WORKDIR /workspace
