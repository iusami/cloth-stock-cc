# Android 開発環境用 Dockerfile
# ベースイメージとしてOpenJDK 17を使用
FROM openjdk:17-jdk-slim

# メンテナ情報
LABEL maintainer="cloth-stock development team"
LABEL description="Android development environment for cloth-stock app"

# 環境変数設定
ENV ANDROID_HOME=/opt/android-sdk
ENV ANDROID_SDK_ROOT=$ANDROID_HOME
ENV PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/build-tools

# 必要なパッケージのインストール
RUN apt-get update && apt-get install -y \
    wget \
    unzip \
    git \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Gradleのインストール
ENV GRADLE_VERSION=8.2.1
RUN wget https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip -O /tmp/gradle.zip && \
    unzip /tmp/gradle.zip -d /opt && \
    rm /tmp/gradle.zip && \
    ln -s /opt/gradle-${GRADLE_VERSION}/bin/gradle /usr/local/bin/gradle

# Android SDK Command Line Toolsのダウンロードとインストール（rootユーザーで実行）
RUN mkdir -p $ANDROID_HOME/cmdline-tools && \
    wget https://dl.google.com/android/repository/commandlinetools-linux-10406996_latest.zip -O /tmp/cmdline-tools.zip && \
    unzip /tmp/cmdline-tools.zip -d $ANDROID_HOME/cmdline-tools && \
    mv $ANDROID_HOME/cmdline-tools/cmdline-tools $ANDROID_HOME/cmdline-tools/latest && \
    rm /tmp/cmdline-tools.zip

# 非rootユーザーの作成（セキュリティ強化）
RUN groupadd -r android && useradd -r -g android -m android && \
    chown -R android:android $ANDROID_HOME

# ユーザーを切り替えてから後続のコマンドを実行
USER android
WORKDIR /home/android

# Android SDKライセンスの自動受諾
RUN yes | sdkmanager --licenses

# 必要なAndroid SDK コンポーネントのインストール
RUN sdkmanager \
    "platform-tools" \
    "build-tools;34.0.0" \
    "platforms;android-34" \
    "platforms;android-33" \
    "extras;android;m2repository" \
    "extras;google;m2repository"

# 作業ディレクトリの設定
WORKDIR /workspace

# コンテナ起動時のデフォルトコマンド
CMD ["bash"]