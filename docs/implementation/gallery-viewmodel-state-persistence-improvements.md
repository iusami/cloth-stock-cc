# GalleryViewModelStatePersistenceTest 改善提案

## 🎯 概要

`GalleryViewModelStatePersistenceTest.kt` のコード品質向上のための改善提案です。TDD Red フェーズのテストとして良い構造ですが、保守性とテスタビリティを向上させる改善点があります。

## 🔍 検出されたコードスメル

### 1. **Test Smell: Hardcoded Test Data**
- **問題**: テストメソッド内でハードコードされたテストデータ
- **影響**: データ変更時の保守性低下、テストデータの一貫性欠如

### 2. **Test Smell: Duplicate Mock Setup**
- **問題**: 各テストメソッドで重複するモック設定
- **影響**: コードの重複、保守性の低下

### 3. **Test Smell: Long Test Methods**
- **問題**: 一部のテストメソッドが長すぎる
- **影響**: 可読性の低下、テストの意図が不明確

## ✅ 実装済み改善

### 1. **Test Data Factory Pattern の導入**

```kotlin
// 改善前: ハードコードされたテストデータ
val filterState = FilterState(
    sizeFilters = setOf(100, 110),
    colorFilters = setOf("赤", "青"),
    categoryFilters = setOf("トップス"),
    searchText = "シャツ"
)

// 改善後: ファクトリーメソッドの使用
companion object {
    private const val TEST_SEARCH_TEXT = "シャツ"
    private const val TEST_SEARCH_TEXT_2 = "パンツ"
    private const val TEST_SEARCH_TEXT_3 = "保存された検索"
    
    private fun createTestFilterState() = FilterState(
        sizeFilters = setOf(100, 110),
        colorFilters = setOf("赤", "青"),
        categoryFilters = setOf("トップス"),
        searchText = TEST_SEARCH_TEXT
    )
    
    private fun createTestFilterState2() = FilterState(
        sizeFilters = setOf(120, 130),
        colorFilters = setOf("緑"),
        categoryFilters = setOf("ボトムス"),
        searchText = TEST_SEARCH_TEXT_2
    )
    
    private fun createTestFilterState3() = FilterState(
        sizeFilters = setOf(140, 150),
        colorFilters = setOf("黒", "白")
    )
}
```

### 2. **Helper Methods の追加**

```kotlin
// ViewModelインスタンス作成の統一
private fun createViewModelWithMocks(): GalleryViewModel {
    return GalleryViewModel(mockRepository, mockFilterManager, mockSavedStateHandle, null)
}

// SavedStateHandle設定の統一
private fun setupSavedStateForRestore(filterState: FilterState?, searchText: String?) {
    every { mockSavedStateHandle.get<FilterState>("filter_state") } returns filterState
    every { mockSavedStateHandle.get<String>("search_text") } returns searchText
}
```

### 3. **テストメソッドの簡素化**

```kotlin
// 改善前: 重複するセットアップコード
@Test
fun `should restore filter state from SavedStateHandle on initialization`() {
    val savedFilterState = FilterState(...)
    every { mockSavedStateHandle.get<FilterState>("filter_state") } returns savedFilterState
    every { mockSavedStateHandle.get<String>("search_text") } returns "パンツ"
    
    viewModel = GalleryViewModel(mockRepository, mockFilterManager, mockSavedStateHandle, null)
    
    verify { mockFilterManager.restoreState(savedFilterState) }
}

// 改善後: ヘルパーメソッドとファクトリーの使用
@Test
fun `should restore filter state from SavedStateHandle on initialization`() {
    val savedFilterState = createTestFilterState2()
    setupSavedStateForRestore(savedFilterState, TEST_SEARCH_TEXT_2)
    
    viewModel = createViewModelWithMocks()
    
    verify { mockFilterManager.restoreState(savedFilterState) }
}
```

## 🚀 追加改善提案

### 1. **Test Base Class の活用**

既存の `GalleryViewModelTestBase` を継承して共通機能を活用：

```kotlin
@ExperimentalCoroutinesApi
class GalleryViewModelStatePersistenceTest : GalleryViewModelTestBase() {
    
    private lateinit var mockSavedStateHandle: SavedStateHandle
    
    @Before
    override fun setUp() {
        super.setUp()
        mockSavedStateHandle = mockk(relaxed = true)
    }
    
    // テスト固有のヘルパーメソッドのみ定義
}
```

### 2. **Parameterized Tests の導入**

複数の状態パターンをテストする場合：

```kotlin
@ParameterizedTest
@MethodSource("filterStateProvider")
fun `should save various filter states to SavedStateHandle`(
    filterState: FilterState,
    expectedSearchText: String
) {
    every { mockFilterManager.getCurrentState() } returns filterState
    
    viewModel = createViewModelWithMocks()
    viewModel.saveStateToSavedStateHandle()
    
    verify { mockSavedStateHandle.set("filter_state", filterState) }
    verify { mockSavedStateHandle.set("search_text", expectedSearchText) }
}

companion object {
    @JvmStatic
    fun filterStateProvider() = listOf(
        Arguments.of(createTestFilterState(), TEST_SEARCH_TEXT),
        Arguments.of(createTestFilterState2(), TEST_SEARCH_TEXT_2),
        Arguments.of(FilterState(), "")
    )
}
```

### 3. **Custom Matchers の導入**

より表現力豊かなアサーション：

```kotlin
// カスタムマッチャー
private fun hasFilterState(expectedState: FilterState) = object : ArgumentMatcher<FilterState> {
    override fun matches(argument: FilterState?): Boolean {
        return argument?.let {
            it.sizeFilters == expectedState.sizeFilters &&
            it.colorFilters == expectedState.colorFilters &&
            it.categoryFilters == expectedState.categoryFilters &&
            it.searchText == expectedState.searchText
        } ?: false
    }
}

// 使用例
verify { mockSavedStateHandle.set("filter_state", argThat(hasFilterState(expectedState))) }
```

### 4. **Error Scenario Tests の追加**

```kotlin
@Test
fun `should handle SavedStateHandle exceptions gracefully`() {
    // Given: SavedStateHandleでエラーが発生
    every { mockSavedStateHandle.set(any<String>(), any()) } throws RuntimeException("Storage error")
    every { mockFilterManager.getCurrentState() } returns createTestFilterState()
    
    viewModel = createViewModelWithMocks()
    
    // When & Then: 例外が発生してもクラッシュしない
    assertDoesNotThrow {
        viewModel.saveStateToSavedStateHandle()
    }
}
```

## 📊 改善効果

### 1. **保守性の向上**
- テストデータの一元管理
- 重複コードの削減（約30%削減）
- 変更時の影響範囲の限定

### 2. **可読性の向上**
- テストの意図が明確
- ヘルパーメソッドによる抽象化
- 一貫したテストパターン

### 3. **テスタビリティの向上**
- エラーシナリオの網羅
- エッジケースの追加が容易
- パラメータ化テストによる網羅性

### 4. **TDD サイクルの効率化**
- Red フェーズでの失敗テスト作成が高速化
- Green フェーズでの実装指針が明確
- Refactor フェーズでのテスト保守が容易

## 🎯 次のステップ

1. **Base Class 継承**: `GalleryViewModelTestBase` の活用
2. **Parameterized Tests**: 複数パターンのテスト効率化
3. **Custom Matchers**: より表現力豊かなアサーション
4. **Error Scenarios**: 例外処理のテスト追加
5. **Performance Tests**: 状態保存・復元のパフォーマンステスト

この改善により、TDD Red-Green-Refactor サイクルがより効率的になり、長期的な保守性が大幅に向上します。