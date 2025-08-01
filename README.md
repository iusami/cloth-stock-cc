# cloth-stock

**cloth-stock**は、個人の衣服を管理するためのAndroidアプリケーションです。このアプリを使えば、手持ちの衣服を写真に撮り、タグを付けて整理することで、簡単にカタログ化し、必要な服をすぐに見つけ出すことができます。

## 主な機能

*   **写真撮影**: アプリ内からカメラを起動し、衣服の写真を撮影します。
*   **タグ管理**: 撮影した写真に、サイズ（60〜160）、色、カテゴリなどのタグを付けて管理できます。
*   **ギャラリー表示**: タグ付けされたすべての衣服の写真を一覧で表示します。
*   **タグ編集**: 既存のアイテムのタグを後から変更できます。
*   **権限処理**: カメラ機能を利用するための適切な権限管理を行います。

## 技術スタック

このプロジェクトは、以下の技術を使用して開発されています。

*   **言語**: [Kotlin](https://kotlinlang.org/)
*   **UI**: [Material Design Components](https://material.io/develop/android)
*   **アーキテクチャ**: MVVM (Model-View-ViewModel)
*   **非同期処理**: [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
*   **データベース**: [Room](https://developer.android.com/training/data-storage/room)
*   **カメラ**: [CameraX](https://developer.android.com/training/camerax)
*   **画像読み込み**: [Glide](https://github.com/bumptech/glide)
*   **テスト**:
    *   ユニットテスト: [JUnit](https://junit.org/junit5/), [Mockito](https://site.mockito.org/), [Robolectric](http://robolectric.org/)
    *   UIテスト: [Espresso](https://developer.android.com/training/testing/espresso)
*   **静的解析**: [detekt](https://detekt.dev/)

## アーキテクチャ

このアプリケーションは、**MVVM (Model-View-ViewModel)** アーキテクチャを採用しています。また、データ層の抽象化のために**Repositoryパターン**を使用しています。

パッケージ構成は、機能ベースで以下のように分割されています。

*   `ui/camera/`: 写真撮影機能
*   `ui/gallery/`: 衣服アイテムのギャラリー表示
*   `ui/detail/`: アイテム詳細・タグ編集
*   `data/database/`: Roomデータベースの定義
*   `data/model/`: データモデル (ClothItem, Tagなど)
*   `data/repository/`: データアクセス層

## セットアップとビルド

### 必要なもの

*   Android Studio
*   Android SDK
*   JDK 11以上

### ビルドコマンド

プロジェクトをビルドするには、以下のコマンドを実行します。

```bash
./gradlew build
```

デバッグ用のAPKをビルドする場合は、以下のコマンドを実行します。

```bash
./gradlew assembleDebug
```

## テスト

### ユニットテスト

ユニットテストを実行するには、以下のコマンドを実行します。

```bash
./gradlew testDebugUnitTest --stacktrace
```

### UIテスト

UIテスト（インストルメンテーションテスト）を実行するには、接続されているデバイスまたはエミュレータで以下のコマンドを実行します。

```bash
./gradlew connectedAndroidTest
```

### 静的解析

[detekt](https.detekt.dev/) を使用して静的コード解析を実行します。

```bash
./gradlew detekt
```

## リリース手順

このプロジェクトでは、GitHub Actionsによる自動リリースが設定されています。

リリースを行うには、以下の手順に従ってください。

1.  **バージョンの決定**: [セマンティックバージョニング](https://semver.org/lang/ja/) に従い、新しいバージョン番号を決定します。（例: `v1.0.0`）
2.  **Gitタグの作成**: ローカルのmainブランチが最新の状態であることを確認し、以下のコマンドでGitタグを作成します。

    ```bash
    git tag -a vX.Y.Z -m "Release vX.Y.Z"
    ```

    `vX.Y.Z` は **1.** で決定したバージョン番号に置き換えてください。

3.  **Gitタグのプッシュ**: 作成したタグをリモートリポジトリにプッシュします。

    ```bash
    git push origin vX.Y.Z
    ```

4.  **自動リリース**: タグがプッシュされると、GitHub Actionsのワークフローが自動的にトリガーされます。このワークフローは以下の処理を実行します。
    *   テストの実行
    *   リリース用のAPKファイルのビルド
    *   GitHub Releaseの作成
    *   ビルドしたAPKファイルをリリースに添付

5.  **リリースの確認**: GitHubの[Releases](https://github.com/gemini-claude-be/cloth-stock-cc/releases)ページにアクセスし、新しいリリースが作成されていることを確認します。

以上でリリース作業は完了です。
