# Design Document

## Overview

画像メモ機能は、既存のcloth-stockアプリケーションに統合される形で実装されます。この機能により、ユーザーは衣服の写真に対して自由形式のテキストメモを追加し、そのメモ内容を検索で見つけることができるようになります。

設計では、既存のMVVMアーキテクチャとRoomデータベースを活用し、最小限の変更で最大の効果を得ることを目指します。

## Architecture

### データ層の拡張

既存の`ClothItem`エンティティにメモフィールドを追加し、データベーススキーマを拡張します。

```kotlin
@Entity(tableName = ClothItem.TABLE_NAME)
data class ClothItem(
    // 既存フィールド
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val imagePath: String,
    @Embedded
    val tagData: TagData,
    val createdAt: Date,
    
    // 新規追加
    val memo: String = ""
)
```

### 検索機能の拡張

既存の`GallerySearchManager`と`ClothDao`の検索クエリを拡張し、メモ内容も検索対象に含めます。

### UI層の拡張

- **DetailActivity**: メモ入力フィールドを追加
- **GalleryFragment**: メモ有無の視覚的インジケーターを追加
- **ClothItemAdapter**: メモプレビュー表示機能を追加

## Components and Interfaces

### 1. データモデル拡張

#### ClothItem拡張
```kotlin
data class ClothItem(
    // 既存フィールド...
    val memo: String = ""
) : Validatable {
    
    fun withUpdatedMemo(newMemo: String): ClothItem {
        return copy(memo = newMemo.take(MAX_MEMO_LENGTH))
    }
    
    fun hasMemo(): Boolean = memo.isNotBlank()
    
    fun getMemoPreview(maxLength: Int = 50): String {
        return if (memo.length <= maxLength) memo
        else "${memo.take(maxLength)}..."
    }
    
    companion object {
        const val MAX_MEMO_LENGTH = 1000
    }
}
```

### 2. データアクセス層拡張

#### ClothDao拡張
```kotlin
@Dao
interface ClothDao {
    // 既存メソッド...
    
    // メモを含む検索クエリ
    @Query("""
        SELECT * FROM cloth_items 
        WHERE (:searchText IS NULL OR :searchText = '' OR 
               color LIKE '%' || :searchText || '%' OR 
               category LIKE '%' || :searchText || '%' OR
               memo LIKE '%' || :searchText || '%')
        ORDER BY createdAt DESC
    """)
    fun searchItemsByTextWithMemo(searchText: String?): Flow<List<ClothItem>>
    
    // フィルター検索にメモ検索を統合
    @Query("""
        SELECT * FROM cloth_items 
        WHERE (:sizeFilters IS NULL OR size IN (:sizeFilters))
        AND (:colorFilters IS NULL OR color IN (:colorFilters))
        AND (:categoryFilters IS NULL OR category IN (:categoryFilters))
        AND (:searchText IS NULL OR :searchText = '' OR 
             color LIKE '%' || :searchText || '%' OR 
             category LIKE '%' || :searchText || '%' OR
             memo LIKE '%' || :searchText || '%')
        ORDER BY createdAt DESC
    """)
    fun searchItemsWithFiltersAndMemo(
        sizeFilters: List<Int>?,
        colorFilters: List<String>?,
        categoryFilters: List<String>?,
        searchText: String?
    ): Flow<List<ClothItem>>
}
```

### 3. UI コンポーネント拡張

#### MemoInputView (新規カスタムビュー)
```kotlin
class MemoInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    
    private val editText: EditText
    private val characterCountText: TextView
    private var onMemoChangedListener: ((String) -> Unit)? = null
    
    fun setMemo(memo: String) {
        editText.setText(memo)
        updateCharacterCount(memo.length)
    }
    
    fun getMemo(): String = editText.text.toString()
    
    fun setOnMemoChangedListener(listener: (String) -> Unit) {
        onMemoChangedListener = listener
    }
    
    private fun updateCharacterCount(count: Int) {
        characterCountText.text = "$count/${ClothItem.MAX_MEMO_LENGTH}"
        characterCountText.setTextColor(
            if (count > ClothItem.MAX_MEMO_LENGTH * 0.9) 
                ContextCompat.getColor(context, R.color.warning_color)
            else 
                ContextCompat.getColor(context, R.color.normal_text_color)
        )
    }
}
```

#### MemoIndicatorView (新規カスタムビュー)
```kotlin
class MemoIndicatorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var hasMemo = false
    
    fun setHasMemo(hasMemo: Boolean) {
        this.hasMemo = hasMemo
        visibility = if (hasMemo) VISIBLE else GONE
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (hasMemo && canvas != null) {
            // メモアイコンを描画
            paint.color = ContextCompat.getColor(context, R.color.memo_indicator_color)
            canvas.drawCircle(width / 2f, height / 2f, width / 4f, paint)
        }
    }
}
```

## Data Models

### データベースマイグレーション

```kotlin
object DatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // メモカラムを追加
            database.execSQL("ALTER TABLE cloth_items ADD COLUMN memo TEXT NOT NULL DEFAULT ''")
            
            // メモ検索用インデックスを作成
            database.execSQL("CREATE INDEX IF NOT EXISTS index_cloth_items_memo ON cloth_items(memo)")
        }
    }
}
```

### バリデーション拡張

```kotlin
// ClothItem.validate()メソッドの拡張
override fun validate(): ValidationResult {
    return when {
        imagePath.isBlank() -> ValidationResult.error("画像パスが設定されていません", "imagePath")
        memo.length > MAX_MEMO_LENGTH -> ValidationResult.error("メモが長すぎます", "memo")
        else -> {
            val tagValidation = tagData.validate()
            if (tagValidation.isError()) {
                tagValidation
            } else {
                ValidationResult.success()
            }
        }
    }
}
```

## Error Handling

### メモ関連エラー処理

1. **文字数制限エラー**
   - リアルタイムでの文字数カウント表示
   - 制限超過時の警告表示
   - 自動トリミング機能

2. **検索エラー処理**
   - メモ検索失敗時のフォールバック
   - 既存の検索機能への自動切り替え
   - エラー状態の適切な表示

3. **データベースエラー処理**
   - マイグレーション失敗時の対応
   - メモデータ破損時の復旧処理
   - バックアップ・復元機能

### エラーハンドリング戦略

```kotlin
class MemoErrorHandler {
    fun handleMemoSaveError(error: Exception, memo: String): MemoSaveResult {
        return when (error) {
            is SQLiteConstraintException -> {
                Log.e(TAG, "Database constraint violation", error)
                MemoSaveResult.Error("データベースエラーが発生しました")
            }
            is IllegalArgumentException -> {
                Log.w(TAG, "Invalid memo data", error)
                MemoSaveResult.Error("メモの内容が無効です")
            }
            else -> {
                Log.e(TAG, "Unexpected error saving memo", error)
                MemoSaveResult.Error("予期しないエラーが発生しました")
            }
        }
    }
}

sealed class MemoSaveResult {
    object Success : MemoSaveResult()
    data class Error(val message: String) : MemoSaveResult()
}
```

## Testing Strategy

### 1. ユニットテスト

#### データモデルテスト
```kotlin
class ClothItemMemoTest {
    @Test
    fun `withUpdatedMemo should update memo correctly`() {
        val item = ClothItem.create("path", TagData(), Date())
        val updatedItem = item.withUpdatedMemo("Test memo")
        
        assertEquals("Test memo", updatedItem.memo)
    }
    
    @Test
    fun `withUpdatedMemo should truncate long memo`() {
        val longMemo = "a".repeat(ClothItem.MAX_MEMO_LENGTH + 100)
        val item = ClothItem.create("path", TagData(), Date())
        val updatedItem = item.withUpdatedMemo(longMemo)
        
        assertEquals(ClothItem.MAX_MEMO_LENGTH, updatedItem.memo.length)
    }
}
```

#### DAOテスト
```kotlin
class ClothDaoMemoTest {
    @Test
    fun `searchItemsByTextWithMemo should find items by memo content`() = runTest {
        val item = ClothItem.create("path", TagData(), Date()).copy(memo = "購入場所：渋谷")
        dao.insert(item)
        
        val results = dao.searchItemsByTextWithMemo("渋谷").first()
        
        assertEquals(1, results.size)
        assertEquals("購入場所：渋谷", results[0].memo)
    }
}
```

### 2. インストルメンテーションテスト

#### UI テスト
```kotlin
class MemoInputEspressoTest {
    @Test
    fun testMemoInputAndSave() {
        onView(withId(R.id.memo_input))
            .perform(typeText("テストメモ"))
        
        onView(withId(R.id.save_button))
            .perform(click())
        
        onView(withText("保存しました"))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun testMemoCharacterLimit() {
        val longText = "a".repeat(ClothItem.MAX_MEMO_LENGTH + 10)
        
        onView(withId(R.id.memo_input))
            .perform(typeText(longText))
        
        onView(withId(R.id.character_count))
            .check(matches(hasTextColor(R.color.warning_color)))
    }
}
```

### 3. 統合テスト

#### 検索機能統合テスト
```kotlin
class MemoSearchIntegrationTest {
    @Test
    fun testMemoSearchIntegration() = runTest {
        // データ準備
        val items = listOf(
            ClothItem.create("path1", TagData(category = "シャツ"), Date()).copy(memo = "お気に入り"),
            ClothItem.create("path2", TagData(category = "パンツ"), Date()).copy(memo = "仕事用")
        )
        repository.insertItems(items)
        
        // 検索実行
        val results = repository.searchItemsByText("お気に入り").first()
        
        // 検証
        assertEquals(1, results.size)
        assertEquals("お気に入り", results[0].memo)
    }
}
```

### 4. パフォーマンステスト

```kotlin
class MemoPerformanceTest {
    @Test
    fun testMemoSearchPerformance() = runTest {
        // 大量データ準備
        val items = (1..1000).map { i ->
            ClothItem.create("path$i", TagData(), Date()).copy(memo = "メモ$i")
        }
        repository.insertItems(items)
        
        // 検索パフォーマンス測定
        val startTime = System.currentTimeMillis()
        val results = repository.searchItemsByText("メモ500").first()
        val endTime = System.currentTimeMillis()
        
        assertTrue("Search should complete within 100ms", endTime - startTime < 100)
        assertEquals(1, results.size)
    }
}
```

## Implementation Considerations

### 1. パフォーマンス最適化

- **データベースインデックス**: メモフィールドに検索用インデックスを作成
- **検索クエリ最適化**: LIKE演算子の効率的な使用
- **メモリ使用量**: 長いメモテキストのメモリ効率的な処理

### 2. ユーザビリティ

- **リアルタイム文字数カウント**: ユーザーが制限を意識できるUI
- **メモプレビュー**: ギャラリービューでのメモ内容の視覚的表示
- **検索結果ハイライト**: 検索語句のハイライト表示

### 3. アクセシビリティ

- **スクリーンリーダー対応**: 適切なcontentDescriptionの設定
- **フォーカス管理**: キーボードナビゲーションの適切な実装
- **色覚対応**: 色だけに依存しない情報伝達

### 4. 国際化対応

- **多言語メモ**: Unicode文字の適切な処理
- **文字数制限**: 言語による文字数の違いを考慮
- **検索機能**: 各言語の検索特性を考慮した実装