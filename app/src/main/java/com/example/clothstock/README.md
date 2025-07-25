# ClothStock アプリケーション構造

## パッケージ構成

### UI層 (ui/)
- **camera/** - カメラ機能関連
  - CameraActivity, CameraViewModel
  - 写真撮影とプレビュー機能
  
- **gallery/** - ギャラリー表示関連  
  - GalleryFragment, GalleryViewModel
  - 衣服アイテム一覧表示
  
- **detail/** - 詳細表示・編集関連
  - DetailActivity, TaggingActivity
  - アイテム詳細表示とタグ編集

### データ層 (data/)
- **model/** - データモデル
  - ClothItem, TagData
  - エンティティクラスとUIモデル
  
- **database/** - Room データベース
  - ClothDatabase, ClothDao
  - ローカルデータベース操作
  
- **repository/** - リポジトリ
  - ClothRepository
  - データアクセスの抽象化

### ユーティリティ (util/)
- 共通ユーティリティクラス
- 拡張関数
- 定数定義

## アーキテクチャ
- **MVVM** (Model-View-ViewModel) パターン
- **Repository** パターン
- **LiveData** と **DataBinding** を使用
- **TDD** (テスト駆動開発) アプローチ