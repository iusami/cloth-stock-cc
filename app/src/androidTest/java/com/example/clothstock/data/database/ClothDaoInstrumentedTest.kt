package com.example.clothstock.data.database

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.clothstock.data.model.ClothItem
import com.example.clothstock.data.model.TagData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import java.util.Date

/**
 * ClothDao のインストルメンテーションテスト
 * 
 * 実際のAndroid環境でデータベース操作をテストする
 */
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class ClothDaoInstrumentedTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: ClothDatabase
    private lateinit var clothDao: ClothDao

    @Before
    fun setup() {
        // 実際のAndroid環境でインメモリデータベースを作成
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ClothDatabase::class.java
        ).build()

        clothDao = database.clothDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    // ===== 実環境でのデータベース統合テスト =====

    @Test
    fun `実環境でのCRUD操作_正常に動作する`() = runTest {
        // Given
        val tagData = TagData(
            size = 120,
            color = "ネイビー",
            category = "ジャケット"
        )
        val clothItem = ClothItem.create(
            imagePath = "/storage/emulated/0/Pictures/jacket.jpg",
            tagData = tagData
        )

        // When - Insert
        val insertedId = clothDao.insert(clothItem)

        // Then - Insert確認
        assertTrue("IDが生成される", insertedId > 0)

        // When - Select
        val retrievedItem = clothDao.getItemById(insertedId)

        // Then - Select確認
        assertNotNull("アイテムが取得できる", retrievedItem)
        assertEquals(tagData.size, retrievedItem!!.tagData.size)
        assertEquals(tagData.color, retrievedItem.tagData.color)
        assertEquals(tagData.category, retrievedItem.tagData.category)

        // When - Update
        val updatedTagData = tagData.copy(color = "ブラック")
        val updatedItem = retrievedItem.copy(tagData = updatedTagData)
        val updateCount = clothDao.update(updatedItem)

        // Then - Update確認
        assertEquals(1, updateCount)
        val updatedRetrievedItem = clothDao.getItemById(insertedId)
        assertEquals("ブラック", updatedRetrievedItem!!.tagData.color)

        // When - Delete
        val deleteCount = clothDao.delete(updatedRetrievedItem)

        // Then - Delete確認
        assertEquals(1, deleteCount)
        val deletedItem = clothDao.getItemById(insertedId)
        assertNull("削除されたアイテムは取得できない", deletedItem)
    }

    @Test
    fun `実環境でのFlow操作_リアクティブに動作する`() = runTest {
        // Given
        val item1 = ClothItem.create(
            "/path1.jpg",
            TagData(80, "赤", "Tシャツ")
        )
        val item2 = ClothItem.create(
            "/path2.jpg",
            TagData(90, "青", "シャツ")
        )

        // When - 最初は空
        var allItems = clothDao.getAllItems().first()
        assertTrue("最初は空", allItems.isEmpty())

        // When - 1つ追加
        clothDao.insert(item1)
        allItems = clothDao.getAllItems().first()

        // Then
        assertEquals(1, allItems.size)
        assertEquals("Tシャツ", allItems[0].tagData.category)

        // When - もう1つ追加
        clothDao.insert(item2)
        allItems = clothDao.getAllItems().first()

        // Then
        assertEquals(2, allItems.size)
        // 新しく追加されたものが最初に来る（createdAt DESCソート）
        assertEquals("シャツ", allItems[0].tagData.category)
        assertEquals("Tシャツ", allItems[1].tagData.category)
    }

    @Test
    fun `実環境でのTypeConverter_正しく動作する`() = runTest {
        // Given - 特定の日時でアイテムを作成
        val specificDate = Date(1642678800000L) // 2022-01-20 12:00:00 UTC
        val clothItem = ClothItem(
            id = 0,
            imagePath = "/test.jpg",
            tagData = TagData(100, "白", "シャツ"),
            createdAt = specificDate
        )

        // When
        val insertedId = clothDao.insert(clothItem)
        val retrievedItem = clothDao.getItemById(insertedId)

        // Then - Dateが正確に保存・復元される
        assertNotNull(retrievedItem)
        assertEquals(specificDate.time, retrievedItem!!.createdAt.time)
    }

    @Test
    fun `実環境での検索クエリ_正しく動作する`() = runTest {
        // Given - 複数カテゴリのアイテムを追加
        val items = listOf(
            ClothItem.create("/shirt1.jpg", TagData(80, "白", "シャツ")),
            ClothItem.create("/shirt2.jpg", TagData(90, "青", "シャツ")),
            ClothItem.create("/pants1.jpg", TagData(100, "黒", "パンツ")),
            ClothItem.create("/jacket1.jpg", TagData(110, "グレー", "ジャケット"))
        )

        items.forEach { clothDao.insert(it) }

        // When - カテゴリ別検索
        val shirts = clothDao.getItemsByCategory("シャツ").first()
        val pants = clothDao.getItemsByCategory("パンツ").first()

        // Then
        assertEquals(2, shirts.size)
        assertEquals(1, pants.size)
        assertTrue("すべてシャツ", shirts.all { it.tagData.category == "シャツ" })
        assertTrue("すべてパンツ", pants.all { it.tagData.category == "パンツ" })

        // When - サイズ範囲検索
        val mediumSizes = clothDao.getItemsBySizeRange(85, 105).first()

        // Then
        assertEquals(3, mediumSizes.size) // 90, 100のアイテム
        assertTrue("範囲内のサイズ", mediumSizes.all { 
            it.tagData.size in 85..105 
        })
    }

    @Test
    fun `実環境での大量データ_パフォーマンステスト`() = runTest {
        // Given - 100個のアイテムを作成
        val startTime = System.currentTimeMillis()
        
        repeat(100) { index ->
            val item = ClothItem.create(
                "/test_$index.jpg",
                TagData(
                    size = 60 + (index % 100), // 60-159の範囲
                    color = "色$index",
                    category = "カテゴリ${index % 5}" // 5種類のカテゴリ
                )
            )
            clothDao.insert(item)
        }

        val insertTime = System.currentTimeMillis() - startTime

        // When - 全件取得
        val retrieveStartTime = System.currentTimeMillis()
        val allItems = clothDao.getAllItems().first()
        val retrieveTime = System.currentTimeMillis() - retrieveStartTime

        // Then
        assertEquals(100, allItems.size)
        assertTrue("挿入パフォーマンス", insertTime < 5000) // 5秒以内
        assertTrue("取得パフォーマンス", retrieveTime < 1000) // 1秒以内

        // 検索パフォーマンステスト
        val searchStartTime = System.currentTimeMillis()
        val categoryItems = clothDao.getItemsByCategory("カテゴリ0").first()
        val searchTime = System.currentTimeMillis() - searchStartTime

        assertEquals(20, categoryItems.size) // 100÷5=20個
        assertTrue("検索パフォーマンス", searchTime < 500) // 0.5秒以内
    }

    // ===== Task3: 新しいフィルター・検索機能のテスト =====

    @Test
    fun `searchItemsByText_様々な検索条件で正しく動作する`() = runTest {
        // Given - 検索テスト用のサンプルデータを準備
        val testItems = listOf(
            ClothItem.create("/shirt_red.jpg", TagData(100, "赤", "シャツ")),
            ClothItem.create("/pants_blue.jpg", TagData(110, "青", "パンツ")),
            ClothItem.create("/jacket_red.jpg", TagData(120, "赤", "ジャケット")),
            ClothItem.create("/shirt_white.jpg", TagData(90, "白", "シャツ")),
            ClothItem.create("/dress_black.jpg", TagData(80, "黒", "ワンピース"))
        )
        
        testItems.forEach { clothDao.insert(it) }

        // When - 色で検索（部分一致）
        val redItems = clothDao.searchItemsByText("赤").first()

        // Then
        assertEquals("赤を含むアイテムが2件", 2, redItems.size)
        assertTrue("すべて赤を含む", redItems.all { it.tagData.color.contains("赤") })

        // When - カテゴリで検索
        val shirtItems = clothDao.searchItemsByText("シャツ").first()

        // Then
        assertEquals("シャツを含むアイテムが2件", 2, shirtItems.size)
        assertTrue("すべてシャツを含む", shirtItems.all { it.tagData.category.contains("シャツ") })

        // When - 存在しない文字列で検索
        val notFoundItems = clothDao.searchItemsByText("存在しない").first()

        // Then
        assertTrue("該当なし", notFoundItems.isEmpty())

        // When - 空文字列で検索
        val allItemsFromEmptySearch = clothDao.searchItemsByText("").first()

        // Then
        assertEquals("空文字列では全件取得", 5, allItemsFromEmptySearch.size)
    }

    @Test
    fun `searchItemsWithFilters_複合条件で正しく動作する`() = runTest {
        // Given - 複合検索テスト用のサンプルデータ
        val testItems = listOf(
            ClothItem.create("/item1.jpg", TagData(100, "赤", "シャツ")),
            ClothItem.create("/item2.jpg", TagData(110, "青", "シャツ")),
            ClothItem.create("/item3.jpg", TagData(120, "赤", "パンツ")),
            ClothItem.create("/item4.jpg", TagData(100, "緑", "ジャケット")),
            ClothItem.create("/item5.jpg", TagData(90, "赤", "シャツ")),
            ClothItem.create("/item6.jpg", TagData(130, "白", "ワンピース"))
        )
        
        testItems.forEach { clothDao.insert(it) }

        // When - サイズフィルターのみ
        val size100Items = clothDao.searchItemsWithFilters(
            sizeFilters = listOf(100),
            colorFilters = null,
            categoryFilters = null,
            searchText = null
        ).first()

        // Then
        assertEquals("サイズ100のアイテムが2件", 2, size100Items.size)
        assertTrue("すべてサイズ100", size100Items.all { it.tagData.size == 100 })

        // When - 色フィルターのみ
        val redItems = clothDao.searchItemsWithFilters(
            sizeFilters = null,
            colorFilters = listOf("赤"),
            categoryFilters = null,
            searchText = null
        ).first()

        // Then
        assertEquals("赤いアイテムが3件", 3, redItems.size)
        assertTrue("すべて赤", redItems.all { it.tagData.color == "赤" })

        // When - 複数の色フィルター
        val redAndBlueItems = clothDao.searchItemsWithFilters(
            sizeFilters = null,
            colorFilters = listOf("赤", "青"),
            categoryFilters = null,
            searchText = null
        ).first()

        // Then
        assertEquals("赤または青のアイテムが4件", 4, redAndBlueItems.size)
        assertTrue("すべて赤または青", redAndBlueItems.all { 
            it.tagData.color == "赤" || it.tagData.color == "青" 
        })

        // When - サイズと色の複合条件
        val size100RedItems = clothDao.searchItemsWithFilters(
            sizeFilters = listOf(100),
            colorFilters = listOf("赤"),
            categoryFilters = null,
            searchText = null
        ).first()

        // Then
        assertEquals("サイズ100かつ赤のアイテムが1件", 1, size100RedItems.size)
        val item = size100RedItems[0]
        assertEquals(100, item.tagData.size)
        assertEquals("赤", item.tagData.color)

        // When - すべてのフィルター条件
        val complexFilterItems = clothDao.searchItemsWithFilters(
            sizeFilters = listOf(90, 100),
            colorFilters = listOf("赤"),
            categoryFilters = listOf("シャツ"),
            searchText = "シャツ"
        ).first()

        // Then
        assertEquals("複合条件で2件", 2, complexFilterItems.size)
        assertTrue("すべて条件を満たす", complexFilterItems.all { 
            (it.tagData.size == 90 || it.tagData.size == 100) &&
            it.tagData.color == "赤" &&
            it.tagData.category == "シャツ"
        })
    }

    @Test
    fun `getDistinctSizes_重複なしのサイズリストを返す`() = runTest {
        // Given - 様々なサイズのアイテム
        val testItems = listOf(
            ClothItem.create("/item1.jpg", TagData(100, "赤", "シャツ")),
            ClothItem.create("/item2.jpg", TagData(110, "青", "パンツ")),
            ClothItem.create("/item3.jpg", TagData(100, "緑", "ジャケット")), // 重複サイズ
            ClothItem.create("/item4.jpg", TagData(90, "白", "シャツ")),
            ClothItem.create("/item5.jpg", TagData(110, "黒", "ワンピース")) // 重複サイズ
        )
        
        testItems.forEach { clothDao.insert(it) }

        // When
        val distinctSizes = clothDao.getDistinctSizes()

        // Then
        assertEquals("重複なしで3種類", 3, distinctSizes.size)
        assertEquals("ソートされている", listOf(90, 100, 110), distinctSizes.sorted())
        assertTrue("重複なし", distinctSizes.toSet().size == distinctSizes.size)
    }

    @Test
    fun `getDistinctColors_重複なしの色リストを返す`() = runTest {
        // Given - 様々な色のアイテム
        val testItems = listOf(
            ClothItem.create("/item1.jpg", TagData(100, "赤", "シャツ")),
            ClothItem.create("/item2.jpg", TagData(110, "青", "パンツ")),
            ClothItem.create("/item3.jpg", TagData(120, "赤", "ジャケット")), // 重複色
            ClothItem.create("/item4.jpg", TagData(90, "緑", "シャツ")),
            ClothItem.create("/item5.jpg", TagData(130, "青", "ワンピース")) // 重複色
        )
        
        testItems.forEach { clothDao.insert(it) }

        // When
        val distinctColors = clothDao.getDistinctColors()

        // Then
        assertEquals("重複なしで3種類", 3, distinctColors.size)
        assertTrue("赤が含まれる", distinctColors.contains("赤"))
        assertTrue("青が含まれる", distinctColors.contains("青"))
        assertTrue("緑が含まれる", distinctColors.contains("緑"))
        assertTrue("重複なし", distinctColors.toSet().size == distinctColors.size)
    }

    @Test
    fun `getDistinctCategories_重複なしのカテゴリリストを返す`() = runTest {
        // Given - 様々なカテゴリのアイテム
        val testItems = listOf(
            ClothItem.create("/item1.jpg", TagData(100, "赤", "シャツ")),
            ClothItem.create("/item2.jpg", TagData(110, "青", "パンツ")),
            ClothItem.create("/item3.jpg", TagData(120, "緑", "シャツ")), // 重複カテゴリ
            ClothItem.create("/item4.jpg", TagData(90, "白", "ジャケット")),
            ClothItem.create("/item5.jpg", TagData(130, "黒", "パンツ")) // 重複カテゴリ
        )
        
        testItems.forEach { clothDao.insert(it) }

        // When
        val distinctCategories = clothDao.getDistinctCategories()

        // Then
        assertEquals("重複なしで3種類", 3, distinctCategories.size)
        assertTrue("シャツが含まれる", distinctCategories.contains("シャツ"))
        assertTrue("パンツが含まれる", distinctCategories.contains("パンツ"))
        assertTrue("ジャケットが含まれる", distinctCategories.contains("ジャケット"))
        assertTrue("重複なし", distinctCategories.toSet().size == distinctCategories.size)
    }

    @Test
    fun `searchItemsWithFilters_空のデータベースで正しく動作する`() = runTest {
        // Given - 空のデータベース（何も挿入しない）

        // When
        val results = clothDao.searchItemsWithFilters(
            sizeFilters = listOf(100),
            colorFilters = listOf("赤"),
            categoryFilters = listOf("シャツ"),
            searchText = "test"
        ).first()

        // Then
        assertTrue("空のリストが返される", results.isEmpty())
    }

    @Test
    fun `getDistinct系メソッド_空のデータベースで正しく動作する`() = runTest {
        // Given - 空のデータベース

        // When & Then
        assertTrue("サイズリストが空", clothDao.getDistinctSizes().isEmpty())
        assertTrue("色リストが空", clothDao.getDistinctColors().isEmpty())
        assertTrue("カテゴリリストが空", clothDao.getDistinctCategories().isEmpty())
    }

    @Test
    fun `searchItemsByText_nullや空文字列を適切に処理する`() = runTest {
        // Given
        val testItem = ClothItem.create("/test.jpg", TagData(100, "赤", "シャツ"))
        clothDao.insert(testItem)

        // When - null（実際には空文字列として扱われる）
        val nullResults = clothDao.searchItemsByText("").first()

        // Then
        assertEquals("空文字列では全件取得", 1, nullResults.size)
    }

    // ===== 🔴 TDD Red: Task2 メモ検索機能テスト（失敗するテスト） =====

    @Test
    fun `searchItemsByText_メモ内容で検索_正しく動作する`() = runTest {
        // Given - メモ付きのテストデータを準備
        val itemWithMemo1 = ClothItem.create("/memo1.jpg", TagData(100, "青", "シャツ")).copy(memo = "お気に入りの一枚")
        val itemWithMemo2 = ClothItem.create("/memo2.jpg", TagData(110, "赤", "パンツ")).copy(memo = "仕事用のパンツ")
        val itemWithMemo3 = ClothItem.create("/memo3.jpg", TagData(120, "黒", "アウター")).copy(memo = "")
        val itemWithMemo4 = ClothItem.create("/memo4.jpg", TagData(130, "白", "シャツ")).copy(memo = "カジュアル用")

        clothDao.insert(itemWithMemo1)
        clothDao.insert(itemWithMemo2)
        clothDao.insert(itemWithMemo3)
        clothDao.insert(itemWithMemo4)

        // When - メモ内容で検索（部分一致）
        val favoriteItems = clothDao.searchItemsByText("お気に入り").first()

        // Then
        assertEquals("お気に入りを含むアイテムが1件", 1, favoriteItems.size)
        assertTrue("メモにお気に入りを含む", favoriteItems[0].memo.contains("お気に入り"))

        // When - メモ内容で検索（別のキーワード）
        val workItems = clothDao.searchItemsByText("仕事").first()

        // Then
        assertEquals("仕事を含むアイテムが1件", 1, workItems.size)
        assertTrue("メモに仕事を含む", workItems[0].memo.contains("仕事"))

        // When - メモと既存フィールドの複合検索
        val shirtItems = clothDao.searchItemsByText("シャツ").first()

        // Then - カテゴリ「シャツ」とメモに「シャツ」を含むアイテム両方を取得
        assertEquals("シャツ関連アイテムが2件", 2, shirtItems.size)

        // When - 存在しないメモキーワードで検索
        val notFoundItems = clothDao.searchItemsByText("存在しないメモ").first()

        // Then
        assertTrue("該当なし", notFoundItems.isEmpty())
    }

    @Test
    fun `searchItemsWithFilters_メモ検索とフィルター組み合わせ_正しく動作する`() = runTest {
        // Given - メモとフィルター条件の複合テスト用データ
        val testItems = listOf(
            ClothItem.create("/filter1.jpg", TagData(100, "赤", "シャツ")).copy(memo = "赤いシャツ、お気に入り"),
            ClothItem.create("/filter2.jpg", TagData(100, "青", "シャツ")).copy(memo = "青いシャツ、普段用"),
            ClothItem.create("/filter3.jpg", TagData(110, "赤", "パンツ")).copy(memo = "赤いパンツ、フォーマル"),
            ClothItem.create("/filter4.jpg", TagData(120, "黒", "アウター")).copy(memo = "")
        )

        testItems.forEach { clothDao.insert(it) }

        // When - サイズフィルター + メモ検索
        val size100FavoriteItems = clothDao.searchItemsWithFilters(
            sizeFilters = listOf(100),
            colorFilters = null,
            categoryFilters = null,
            searchText = "お気に入り"
        ).first()

        // Then
        assertEquals("サイズ100でお気に入りのアイテムが1件", 1, size100FavoriteItems.size)
        assertEquals("サイズが100", 100, size100FavoriteItems[0].tagData.size)
        assertTrue("メモにお気に入りを含む", size100FavoriteItems[0].memo.contains("お気に入り"))

        // When - 色フィルター + メモ検索
        val redItems = clothDao.searchItemsWithFilters(
            sizeFilters = null,
            colorFilters = listOf("赤"),
            categoryFilters = null,
            searchText = "赤い"
        ).first()

        // Then - 色が赤 OR メモに「赤い」を含むアイテム
        assertEquals("赤い関連アイテムが2件", 2, redItems.size)
        assertTrue("すべて赤色またはメモに赤いを含む", redItems.all { 
            it.tagData.color == "赤" || it.memo.contains("赤い") 
        })

        // When - 複合フィルター + メモ検索
        val complexSearch = clothDao.searchItemsWithFilters(
            sizeFilters = listOf(100, 110),
            colorFilters = listOf("赤"),
            categoryFilters = listOf("シャツ", "パンツ"),
            searchText = "フォーマル"
        ).first()

        // Then
        assertEquals("複合条件に合致するアイテム", 1, complexSearch.size)
    }

    @Test
    fun `searchItemsWithPagination_メモ検索対応_正しく動作する`() = runTest {
        // Given - ページネーションテスト用のメモ付きデータ
        val testItems = (1..10).map { i ->
            ClothItem.create("/page$i.jpg", TagData(100 + i, "色$i", "カテゴリ$i"))
                .copy(memo = "ページテスト用アイテム$i")
        }

        testItems.forEach { clothDao.insert(it) }

        // When - メモ検索でページネーション（最初の5件）
        val parameters = com.example.clothstock.data.model.PaginationSearchParameters(
            sizeFilters = null,
            colorFilters = null,
            categoryFilters = null,
            searchText = "ページテスト",
            offset = 0,
            limit = 5
        )
        val firstPageItems = clothDao.searchItemsWithPagination(parameters).first()

        // Then
        assertEquals("最初の5件取得", 5, firstPageItems.size)
        assertTrue("すべてメモにページテストを含む", firstPageItems.all { 
            it.memo.contains("ページテスト") 
        })

        // When - 次の5件取得
        val secondPageParameters = parameters.copy(offset = 5)
        val secondPageItems = clothDao.searchItemsWithPagination(secondPageParameters).first()

        // Then
        assertEquals("次の5件取得", 5, secondPageItems.size)
        assertTrue("すべてメモにページテストを含む", secondPageItems.all { 
            it.memo.contains("ページテスト") 
        })
    }

    @Test
    fun `getFilteredItemCount_メモ検索条件_正しくカウントする`() = runTest {
        // Given - カウントテスト用データ
        val testItems = listOf(
            ClothItem.create("/count1.jpg", TagData(100, "赤", "シャツ")).copy(memo = "カウントテスト用"),
            ClothItem.create("/count2.jpg", TagData(110, "青", "パンツ")).copy(memo = "カウントテスト用"),
            ClothItem.create("/count3.jpg", TagData(120, "緑", "アウター")).copy(memo = "別のメモ"),
            ClothItem.create("/count4.jpg", TagData(130, "黒", "シャツ")).copy(memo = "")
        )

        testItems.forEach { clothDao.insert(it) }

        // When - メモ検索での総数取得
        val count = clothDao.getFilteredItemCount(
            sizeFilters = null,
            colorFilters = null,
            categoryFilters = null,
            searchText = "カウントテスト"
        )

        // Then
        assertEquals("カウントテストを含むアイテム数", 2, count)

        // When - フィルター + メモ検索での総数取得
        val filteredCount = clothDao.getFilteredItemCount(
            sizeFilters = listOf(100, 110),
            colorFilters = null,
            categoryFilters = null,
            searchText = "カウントテスト"
        )

        // Then
        assertEquals("サイズ条件 + カウントテスト条件", 2, filteredCount)
    }

    @Test
    fun `searchItemsByText_メモが空文字列の場合_適切に処理する`() = runTest {
        // Given - 空メモのアイテム
        val itemWithEmptyMemo = ClothItem.create("/empty.jpg", TagData(100, "赤", "シャツ")).copy(memo = "")
        val itemWithMemo = ClothItem.create("/with_memo.jpg", TagData(110, "青", "パンツ")).copy(memo = "メモあり")

        clothDao.insert(itemWithEmptyMemo)
        clothDao.insert(itemWithMemo)

        // When - 空文字列メモでは引っかからない検索
        val searchResults = clothDao.searchItemsByText("存在しないキーワード").first()

        // Then
        assertTrue("空メモアイテムは引っかからない", searchResults.isEmpty())

        // When - 全体検索（空文字列検索）
        val allResults = clothDao.searchItemsByText("").first()

        // Then
        assertEquals("空文字列検索では全件取得", 2, allResults.size)
    }
}