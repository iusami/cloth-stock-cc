# テスト構造改善ドキュメント

## 概要

GalleryViewModelTestの巨大化問題を解決するため、テストクラスを機能別に分割し、保守性とテスト実行効率を向上させました。

## 改善前の問題点

### 1. Code Smells
- **Large Class**: 600行を超える巨大なテストクラス
- **Duplicate Code**: リポジトリモック設定の重複
- **Complex Conditionals**: setupFilterMocksの複雑なwhen文

### 2. 保守性の問題
- テストメソッドが多すぎて目的の機能を見つけにくい
- 機能追加時にファイルが更に肥大化
- テスト実行時間の増加

## 改善後の構造

### テストクラス分割

```
app/src/test/java/com/example/clothstock/ui/gallery/
├── GalleryViewModelTestBase.kt          # 共通基底クラス
├── GalleryViewModelCoreTest.kt          # 基本機能テスト
├── GalleryViewModelFilterTest.kt        # フィルター・検索機能テスト
├── GalleryViewModelPerformanceTest.kt   # パフォーマンステスト
└── GalleryViewModelTest.kt              # 既存ファイル（段階的移行用）
```

### 1. GalleryViewModelTestBase.kt
**目的**: 共通のセットアップとヘルパーメソッドを提供

**主要機能**:
- テスト定数の定義
- TestClothItemBuilder（Builder パターン）
- 共通のモック設定メソッド
- ViewModelの初期化ヘルパー

**改善点**:
```kotlin
// Builder パターンでテストデータ作成を簡素化
private class TestClothItemBuilder {
    fun withId(id: Long) = apply { this.id = id }
    fun withSize(size: Int) = apply { this.size = size }
    // ...
    fun build() = ClothItem(...)
}

// Strategy パターンでモック設定を分離
protected fun setupFilterMocks(
    category: String? = null,
    color: String? = null,
    size: Int? = null,
    searchText: String? = null,
    expectedResult: List<ClothItem> = emptyList()
) {
    category?.let { 
        `when`(clothRepository.getItemsByCategory(it)).thenReturn(flowOf(expectedResult))
    }
    // ...
}
```

### 2. GalleryViewModelCoreTest.kt
**目的**: 基本機能（CRUD操作、基本フィルタリング）のテスト

**テスト対象**:
- 初期化処理
- データ読み込み
- 基本的なフィルタリング（カテゴリ、色）
- アイテム削除
- エラーハンドリング

### 3. GalleryViewModelFilterTest.kt
**目的**: 高度なフィルター・検索機能のテスト

**テスト対象**:
- 複合フィルター操作
- 検索デバウンシング
- フィルター状態管理
- 検索とフィルターの組み合わせ

### 4. GalleryViewModelPerformanceTest.kt
**目的**: パフォーマンス関連のテスト

**テスト対象**:
- 大量データでの初期化
- フィルタリングパフォーマンス
- メモリ効率テスト
- 検索デバウンシングのパフォーマンス

## 設計パターンの適用

### 1. Builder Pattern
テストデータの作成を柔軟かつ読みやすくしました：

```kotlin
val testItem = TestClothItemBuilder()
    .withId(1L)
    .withSize(TEST_SIZE_SMALL)
    .withColor(TEST_COLOR_RED)
    .withCategory(TEST_CATEGORY_TOPS)
    .build()
```

### 2. Strategy Pattern
モック設定の複雑な条件分岐を排除：

```kotlin
// 改善前（複雑なwhen文）
when {
    category != null -> `when`(clothRepository.getItemsByCategory(category))...
    color != null -> `when`(clothRepository.getItemsByColor(color))...
    // ...
}

// 改善後（Strategy パターン）
category?.let { 
    `when`(clothRepository.getItemsByCategory(it)).thenReturn(flowOf(expectedResult))
}
color?.let { 
    `when`(clothRepository.getItemsByColor(it)).thenReturn(flowOf(expectedResult))
}
```

### 3. Template Method Pattern
基底クラスで共通処理を定義し、各テストクラスで特化した処理を実装：

```kotlin
abstract class GalleryViewModelTestBase {
    // 共通のセットアップ
    protected fun setupBasicRepositoryMocks() { ... }
    protected fun createViewModelAndAdvance() { ... }
}

class GalleryViewModelCoreTest : GalleryViewModelTestBase() {
    // 基本機能に特化したテスト
}
```

## パフォーマンス改善

### 1. テスト実行時間の短縮
- 機能別分割により、必要な部分のみテスト実行可能
- 並列実行の効率化

### 2. メモリ使用量の最適化
- 大量データテストを専用クラスに分離
- テストデータの再利用

### 3. デバウンシングテストの改善
```kotlin
@Test
fun `検索デバウンシング_連続入力時に最後の検索のみ実行される`() = runTest {
    // パフォーマンス測定付きのテスト
    val executionTime = measureTimeMillis {
        repeat(10) { index ->
            viewModel.performSearch("検索$index")
        }
        viewModel.performSearch("最終検索")
        testDispatcher.scheduler.advanceUntilIdle()
    }
    
    // パフォーマンス閾値の確認
    assertTrue(executionTime < PERFORMANCE_THRESHOLD_MS)
}
```

## ベストプラクティスの適用

### 1. Null Safety
```kotlin
// 改善前
when {
    category != null -> // 複雑な条件分岐
}

// 改善後
category?.let { // Kotlinのnull safety活用
    `when`(clothRepository.getItemsByCategory(it))...
}
```

### 2. Coroutines Usage
```kotlin
// 適切なrunTestの使用
@Test
fun `テスト名`() = runTest {
    // コルーチンテストの適切な実装
    testDispatcher.scheduler.advanceUntilIdle()
}
```

### 3. Resource Management
```kotlin
@After
fun tearDown() {
    Dispatchers.resetMain() // リソースの適切なクリーンアップ
}
```

## 今後の拡張性

### 1. 新機能追加時
- 対応する専用テストクラスに追加
- 基底クラスの共通機能を活用

### 2. テストカバレッジ向上
- 各クラスで特化した観点からのテスト
- エッジケースの網羅的なカバー

### 3. CI/CD統合
- 機能別テスト実行
- 段階的テスト実行戦略

## まとめ

この改善により以下の効果を実現：

1. **保守性向上**: 機能別分割による可読性向上
2. **テスト効率化**: 必要な部分のみの実行が可能
3. **設計品質向上**: 適切なデザインパターンの適用
4. **拡張性確保**: 新機能追加時の影響範囲最小化
5. **パフォーマンス最適化**: 実行時間とメモリ使用量の改善

TDD Red-Green-RefactorサイクルにおけるRefactorフェーズとして、テストコード自体の品質向上を実現しました。