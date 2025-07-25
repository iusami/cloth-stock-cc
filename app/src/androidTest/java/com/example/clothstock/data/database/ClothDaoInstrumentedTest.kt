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
}