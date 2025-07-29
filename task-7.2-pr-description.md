# Task 7.2: タグ編集機能のTDD実装

## User Story

**要件4**: 衣服の所有者として、既存の写真のタグを編集したい。そうすることで、衣服情報を更新または修正できる。

### 受け入れ基準
- 4.1: ユーザーがタグ付きの写真を表示したとき、システムはタグの編集オプションを提供する
- 4.2: ユーザーがタグ編集を選択したとき、システムは既存データで事前入力されたタグ付けインターフェースを表示する
- 4.3: ユーザーがタグ情報を変更したとき、システムは更新された入力を検証する
- 4.4: ユーザーが編集されたタグを保存したとき、システムは新しいタグ情報でデータベースを更新する
- 4.5: ユーザーが編集をキャンセルした場合、システムは元のタグデータを保持する

## 設計意図

### TDDアプローチの採用
このPRは**Test-Driven Development (TDD)**のRed-Green-Refactorサイクルに従って実装されています：

1. **Red Phase**: 失敗するテストを先行作成
2. **Green Phase**: テストを通すための最低限の実装
3. **Refactor Phase**: コードの品質向上とUX改善

### アーキテクチャ設計
- **MVVM パターン**: TaggingActivity と TaggingViewModel の分離
- **Repository パターン**: データアクセス層の抽象化を活用
- **既存コンポーネントの再利用**: 新規作成モードと編集モードで同じActivity/ViewModelを共用

### データフロー設計
```
DetailActivity → TaggingActivity (編集モード)
    ↓
TaggingViewModel.setEditMode(clothItemId)
    ↓
Repository.getItemById() → 既存データ読み込み
    ↓
UI事前入力 → ユーザー編集 → Repository.updateItem()
```

## 追加機能

### 1. 編集モード対応 (Phase 2 - Green)
- **TaggingActivity**
  - 編集モード検出処理 (`EXTRA_EDIT_MODE`, `EXTRA_CLOTH_ITEM_ID`)
  - タイトル・ボタンテキストの動的変更
  - 既存データでのフィールド事前入力

- **TaggingViewModel**
  - `setEditMode(clothItemId)` メソッド追加
  - `loadClothItem()` による既存データ読み込み
  - `updateExistingItem()` による更新処理実装

### 2. UX改善機能 (Phase 3 - Refactor)
- **アニメーション機能**
  - フィールド更新時のフェードイン/フェードアウトアニメーション
  - 編集モードでの初期フォーカス設定

- **変更追跡システム**
  - ダーティフラグ管理 (`hasUnsavedChanges`)
  - 元データとの変更検出機能
  - 変更なし時のキャンセル確認スキップ

- **改善されたキャンセル処理**
  - モード別メッセージ表示（新規作成 vs 編集）
  - 編集モード時の「元に戻す」オプション追加
  - 未保存変更の適切な警告表示

### 3. テストカバレッジ (Phase 1 - Red)
**TaggingActivityEspressoTest.kt** に9つの編集モード専用テストを追加：

- ✅ 既存データでのフィールド事前入力テスト
- ✅ 編集モード画面タイトル確認テスト  
- ✅ 保存ボタンでの更新処理テスト
- ✅ キャンセル時の元データ保持テスト
- ✅ 無効なClothItemIDのエラー処理テスト
- ✅ 存在しないIDのエラー処理テスト
- ✅ バリデーションエラー処理テスト
- ✅ 変更なし保存の許可テスト
- ✅ DetailActivityからの編集遷移テスト

## 技術詳細

### API変更
- **TaggingActivity**: 新しいIntent Extraに対応
  ```kotlin
  const val EXTRA_CLOTH_ITEM_ID = "extra_cloth_item_id"
  const val EXTRA_EDIT_MODE = "extra_edit_mode"  
  ```

- **TaggingViewModel**: 編集モード専用メソッド追加
  ```kotlin
  fun setEditMode(clothItemId: Long)
  fun hasUnsavedChanges(): Boolean
  fun revertToOriginal()
  ```

### データベース依存
- 既存の `ClothRepository.getItemById()` と `updateItem()` を活用
- 追加のデータベース変更は不要

## テスト結果
- ✅ ユニットテスト: 全て成功
- ✅ ビルドテスト: エラーなし
- ✅ 既存機能: 影響なし（後方互換性維持）

## 影響範囲
- **変更ファイル**:
  - `TaggingActivity.kt` (編集モード対応)
  - `TaggingViewModel.kt` (編集機能追加)  
  - `TaggingActivityEspressoTest.kt` (テスト追加)

- **影響なし**: 既存の新規作成機能は完全に保持

---

この実装により、cloth-stockアプリのタグ編集機能が完全に動作し、ユーザーは撮影済みの衣服写真のタグ情報を簡単に更新・修正できるようになります。