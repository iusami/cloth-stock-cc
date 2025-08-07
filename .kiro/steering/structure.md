# プロジェクト構造

## 現在の構造

```
cloth-stock/
├── .git/           # Gitバージョン管理
├── .kiro/          # Kiro AIアシスタント設定
│   └── steering/   # AIガイダンス文書
├── docker/         # Docker設定ファイル
│   ├── Dockerfile  # Android開発環境イメージ
│   └── scripts/    # Docker関連スクリプト
├── docs/           # アプリケーション開発ドキュメント
├── docker-compose.yml  # Docker Compose設定
├── .dockerignore   # Docker ビルド除外設定
├── README.md       # プロジェクト文書
└── gradlew         # Gradle Wrapper
```

## 推奨 Android 構造（Docker 環境対応）

```
cloth-stock/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/clothstock/
│   │   │   │   ├── ui/          # UI関連（Activity、Fragment、Adapter）
│   │   │   │   │   ├── camera/  # カメラ撮影画面
│   │   │   │   │   ├── gallery/ # 衣服ギャラリー画面
│   │   │   │   │   └── detail/  # 衣服詳細画面
│   │   │   │   ├── data/        # データ層
│   │   │   │   │   ├── database/# Room データベース
│   │   │   │   │   ├── model/   # データモデル
│   │   │   │   │   └── repository/ # リポジトリパターン
│   │   │   │   ├── utils/       # ユーティリティクラス
│   │   │   │   └── MainActivity.kt
│   │   │   ├── res/
│   │   │   │   ├── layout/      # レイアウトXMLファイル
│   │   │   │   ├── drawable/    # 画像リソース
│   │   │   │   ├── values/      # 文字列、色、スタイル
│   │   │   │   └── menu/        # メニューリソース
│   │   │   └── AndroidManifest.xml
│   │   ├── test/                # ユニットテスト
│   │   └── androidTest/         # インストルメンテーションテスト
│   ├── build.gradle.kts         # アプリレベルビルド設定
│   └── proguard-rules.pro       # ProGuard設定
├── docker/                      # Docker設定
│   ├── Dockerfile               # Android開発環境イメージ
│   ├── Dockerfile.ci            # CI/CD用イメージ
│   └── scripts/                 # Docker関連スクリプト
│       ├── setup-android.sh     # Android SDK セットアップ
│       ├── start-emulator.sh    # エミュレータ起動
│       └── run-tests.sh         # テスト実行スクリプト
├── docs/                        # アプリケーション開発ドキュメント
│   ├── api/                     # API仕様書
│   ├── design/                  # 設計ドキュメント
│   ├── implementation/          # 実装ドキュメント（Kiro作成）
│   ├── docker/                  # Docker環境ドキュメント
│   └── user-guide/              # ユーザーガイド
├── docker-compose.yml           # Docker Compose設定
├── docker-compose.override.yml  # ローカル開発用オーバーライド
├── .dockerignore                # Docker ビルド除外設定
├── build.gradle.kts             # プロジェクトレベルビルド設定
├── gradle.properties            # Gradle設定
└── settings.gradle.kts          # プロジェクト設定
```

## 命名規則（Kotlin/Android）

- パッケージ名: com.example.clothstock（小文字、ドット区切り）
- クラス名: PascalCase（例: ClothItem, CameraActivity）
- 関数・変数名: camelCase（例: capturePhoto, clothList）
- 定数: UPPER_SNAKE_CASE（例: MAX_IMAGE_SIZE）
- リソースファイル: snake_case（例: activity_main.xml, ic_camera.xml）

## 主要ディレクトリ（将来）

### アプリケーション

- `ui/camera/` - カメラ撮影機能
- `ui/gallery/` - 衣服一覧表示
- `ui/detail/` - 衣服詳細・タグ編集
- `data/database/` - Room データベース定義
- `data/model/` - ClothItem、Tag 等のデータモデル
- `data/repository/` - データアクセス抽象化

### Docker 環境

- `docker/` - Docker 設定ファイル
- `docker/scripts/` - 環境構築・運用スクリプト
- `docker-compose.yml` - 開発環境オーケストレーション
- `.dockerignore` - Docker ビルド除外設定

### ドキュメント

- `docs/api/` - API 仕様書・データベーススキーマ
- `docs/design/` - UI/UX 設計、アーキテクチャ設計
- `docs/implementation/` - Kiro が作成する実装ドキュメント
- `docs/docker/` - Docker 環境構築・運用ガイド
- `docs/user-guide/` - ユーザー向け操作ガイド

## ファイル構成

### アプリケーション構成

- MVVM アーキテクチャパターンを採用
- 機能別にパッケージを分割
- リソースファイルは用途別に整理
- テストファイルは対応するソースと同じ構造で配置

### Docker 環境構成

- `Dockerfile`: マルチステージビルドでサイズ最適化
- `docker-compose.yml`: 開発環境の統合管理
- `docker-compose.override.yml`: ローカル開発用設定
- ボリュームマウント: ソースコード、Gradle キャッシュ
- 環境変数: Android SDK パス、ライセンス設定

### ドキュメント構成

- ドキュメントは用途別に docs フォルダ内で整理
- Kiro が実装時に作成するドキュメントは docs/implementation/に保存
- Docker 関連ドキュメントは docs/docker/に保存
- 環境構築手順、トラブルシューティングを含む

### 開発フロー

1. `docker compose up -d` で環境起動
2. コンテナ内でコード編集・ビルド・テスト
3. ホストで Git 操作・ドキュメント編集
4. `docker compose down` で環境停止
