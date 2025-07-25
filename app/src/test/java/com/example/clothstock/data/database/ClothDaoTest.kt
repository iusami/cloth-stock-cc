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
 * ClothDao のユニットテスト
 * 
 * Room のテストユーティリティを使用してCRUD操作をテストする
 */
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class ClothDaoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: ClothDatabase
    private lateinit var clothDao: ClothDao

    private val testTagData = TagData(
        size = 100,
        color = "青",
        category = "シャツ"
    )

    private val testClothItem = ClothItem(
        id = 0, // auto generate
        imagePath = "/storage/test/image1.jpg",
        tagData = testTagData,
        createdAt = Date()
    )

    @Before
    fun setup() {
        // インメモリデータベースを作成（テスト用）
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ClothDatabase::class.java
        )
            .allowMainThreadQueries() // テスト用にメインスレッド許可
            .build()

        clothDao = database.clothDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    // ===== INSERT テスト =====

    @Test
    fun `insert_新しいClothItem_成功する`() = runTest {
        // When
        val insertedId = clothDao.insert(testClothItem)

        // Then
        assertTrue("IDが生成される", insertedId > 0)
        
        // 実際にデータベースに保存されているかチェック
        val allItems = clothDao.getAllItems().first()
        assertEquals(1, allItems.size)
        assertEquals(testClothItem.imagePath, allItems[0].imagePath)
        assertEquals(testClothItem.tagData, allItems[0].tagData)
    }

    @Test
    fun `insert_複数のClothItem_正しく保存される`() = runTest {
        // Given
        val item1 = testClothItem // 既にimage1.jpgパスを持っている
        val item2 = testClothItem.copy(imagePath = "/storage/test/image2.jpg")

        // When
        val id1 = clothDao.insert(item1)
        val id2 = clothDao.insert(item2)

        // Then
        assertNotEquals("異なるIDが生成される", id1, id2)
        
        val allItems = clothDao.getAllItems().first()
        assertEquals(2, allItems.size)
    }

    // ===== SELECT テスト =====

    @Test
    fun `getAllItems_空のデータベース_空のリストを返す`() = runTest {
        // When
        val items = clothDao.getAllItems().first()

        // Then
        assertTrue("空のリスト", items.isEmpty())
    }

    @Test
    fun `getAllItems_データあり_正しい順序で返す`() = runTest {
        // Given - 複数のアイテムを異なる時間で挿入
        val now = Date()
        val earlier = Date(now.time - 1000) // 1秒前
        
        val item1 = testClothItem.copy(imagePath = "/test1.jpg", createdAt = earlier)
        val item2 = testClothItem.copy(imagePath = "/test2.jpg", createdAt = now)

        clothDao.insert(item1)
        clothDao.insert(item2)

        // When
        val items = clothDao.getAllItems().first()

        // Then
        assertEquals(2, items.size)
        // 新しいものが最初に来る（ORDER BY createdAt DESC）
        assertEquals("/test2.jpg", items[0].imagePath)
        assertEquals("/test1.jpg", items[1].imagePath)
    }

    @Test
    fun `getItemById_存在するID_正しいアイテムを返す`() = runTest {
        // Given
        val insertedId = clothDao.insert(testClothItem)

        // When
        val foundItem = clothDao.getItemById(insertedId)

        // Then
        assertNotNull("アイテムが見つかる", foundItem)
        assertEquals(insertedId, foundItem!!.id)
        assertEquals(testClothItem.imagePath, foundItem.imagePath)
    }

    @Test
    fun `getItemById_存在しないID_nullを返す`() = runTest {
        // When
        val foundItem = clothDao.getItemById(999L)

        // Then
        assertNull("存在しないIDではnull", foundItem)
    }

    // ===== UPDATE テスト =====

    @Test
    fun `update_存在するアイテム_正しく更新される`() = runTest {
        // Given
        val insertedId = clothDao.insert(testClothItem)
        val insertedItem = clothDao.getItemById(insertedId)!!

        val updatedTagData = TagData(
            size = 120,
            color = "赤",
            category = "パンツ"
        )
        val updatedItem = insertedItem.copy(tagData = updatedTagData)

        // When
        val updatedRows = clothDao.update(updatedItem)

        // Then
        assertEquals("1行が更新される", 1, updatedRows)
        
        val foundItem = clothDao.getItemById(insertedId)!!
        assertEquals(updatedTagData, foundItem.tagData)
        assertEquals(insertedItem.imagePath, foundItem.imagePath) // 他のフィールドは変更されない
    }

    @Test
    fun `update_存在しないアイテム_0行が更新される`() = runTest {
        // Given
        val nonExistentItem = testClothItem.copy(id = 999L)

        // When
        val updatedRows = clothDao.update(nonExistentItem)

        // Then
        assertEquals("0行が更新される", 0, updatedRows)
    }

    // ===== DELETE テスト =====

    @Test
    fun `delete_存在するアイテム_正しく削除される`() = runTest {
        // Given
        val insertedId = clothDao.insert(testClothItem)
        val insertedItem = clothDao.getItemById(insertedId)!!

        // When
        val deletedRows = clothDao.delete(insertedItem)

        // Then
        assertEquals("1行が削除される", 1, deletedRows)
        
        val foundItem = clothDao.getItemById(insertedId)
        assertNull("削除されたアイテムは見つからない", foundItem)
        
        val allItems = clothDao.getAllItems().first()
        assertTrue("全体が空になる", allItems.isEmpty())
    }

    @Test
    fun `delete_存在しないアイテム_0行が削除される`() = runTest {
        // Given
        val nonExistentItem = testClothItem.copy(id = 999L)

        // When
        val deletedRows = clothDao.delete(nonExistentItem)

        // Then
        assertEquals("0行が削除される", 0, deletedRows)
    }

    @Test
    fun `deleteById_存在するID_正しく削除される`() = runTest {
        // Given
        val insertedId = clothDao.insert(testClothItem)

        // When
        val deletedRows = clothDao.deleteById(insertedId)

        // Then
        assertEquals("1行が削除される", 1, deletedRows)
        assertNull("削除されたアイテムは見つからない", clothDao.getItemById(insertedId))
    }

    // ===== 検索・フィルタリングテスト =====

    @Test
    fun `getItemsByCategory_指定カテゴリ_正しくフィルタリングされる`() = runTest {
        // Given
        val shirtTag = TagData(100, "青", "シャツ")
        val pantsTag = TagData(110, "黒", "パンツ")
        
        clothDao.insert(testClothItem.copy(imagePath = "/shirt.jpg", tagData = shirtTag))
        clothDao.insert(testClothItem.copy(imagePath = "/pants.jpg", tagData = pantsTag))

        // When
        val shirtItems = clothDao.getItemsByCategory("シャツ").first()

        // Then
        assertEquals(1, shirtItems.size)
        assertEquals("シャツ", shirtItems[0].tagData.category)
        assertEquals("/shirt.jpg", shirtItems[0].imagePath)
    }

    @Test
    fun `getItemsBySizeRange_指定範囲_正しくフィルタリングされる`() = runTest {
        // Given
        val smallTag = TagData(80, "赤", "シャツ")
        val mediumTag = TagData(100, "青", "シャツ")
        val largeTag = TagData(120, "緑", "シャツ")
        
        clothDao.insert(testClothItem.copy(imagePath = "/small.jpg", tagData = smallTag))
        clothDao.insert(testClothItem.copy(imagePath = "/medium.jpg", tagData = mediumTag))
        clothDao.insert(testClothItem.copy(imagePath = "/large.jpg", tagData = largeTag))

        // When - サイズ90～110の範囲で検索
        val mediumItems = clothDao.getItemsBySizeRange(90, 110).first()

        // Then
        assertEquals(1, mediumItems.size)
        assertEquals(100, mediumItems[0].tagData.size)
    }

    // ===== アイテム数カウントテスト =====

    @Test
    fun `getItemCount_データあり_正しい数を返す`() = runTest {
        // Given
        clothDao.insert(testClothItem.copy(imagePath = "/test1.jpg"))
        clothDao.insert(testClothItem.copy(imagePath = "/test2.jpg"))

        // When
        val count = clothDao.getItemCount()

        // Then
        assertEquals(2, count)
    }

    @Test
    fun `getItemCount_空のデータベース_0を返す`() = runTest {
        // When
        val count = clothDao.getItemCount()

        // Then
        assertEquals(0, count)
    }
}