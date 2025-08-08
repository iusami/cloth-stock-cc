package com.example.clothstock.data.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.clothstock.data.database.ClothDao
import com.example.clothstock.data.model.ClothItem
import com.example.clothstock.data.model.TagData
import com.example.clothstock.data.model.CategoryCount
import com.example.clothstock.data.model.ColorCount
import com.example.clothstock.data.model.SizeCount
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import org.junit.Assert.*
import java.util.Date

/**
 * ClothRepository のユニットテスト
 * 
 * リポジトリパターンによるデータアクセス層のテスト
 * Mockitoを使用してDAOをモック化し、ビジネスロジックをテスト
 */
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ClothRepositoryTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var clothDao: ClothDao

    private lateinit var clothRepository: ClothRepository

    private val testTagData = TagData(
        size = 100,
        color = "青",
        category = "シャツ"
    )

    private val testClothItem = ClothItem(
        id = 1L,
        imagePath = "/storage/test/image1.jpg",
        tagData = testTagData,
        createdAt = Date()
    )

    @Before
    fun setup() {
        clothRepository = ClothRepositoryImpl(clothDao)
    }

    // ===== INSERT テスト =====

    @Test
    fun `insertItem_正常なClothItem_成功する`() = runTest {
        // Given
        `when`(clothDao.insert(testClothItem)).thenReturn(1L)

        // When
        val result = clothRepository.insertItem(testClothItem)

        // Then
        assertEquals(1L, result)
        verify(clothDao).insert(testClothItem)
    }

    @Test
    fun `insertItems_複数のClothItem_成功する`() = runTest {
        // Given
        val items = listOf(testClothItem, testClothItem.copy(id = 2L))
        `when`(clothDao.insertAll(items)).thenReturn(listOf(1L, 2L))

        // When
        val result = clothRepository.insertItems(items)

        // Then
        assertEquals(listOf(1L, 2L), result)
        verify(clothDao).insertAll(items)
    }

    // ===== UPDATE テスト =====

    @Test
    fun `updateItem_存在するClothItem_成功する`() = runTest {
        // Given
        `when`(clothDao.update(testClothItem)).thenReturn(1)

        // When
        val result = clothRepository.updateItem(testClothItem)

        // Then
        assertTrue(result)
        verify(clothDao).update(testClothItem)
    }

    @Test
    fun `updateItem_存在しないClothItem_失敗する`() = runTest {
        // Given
        `when`(clothDao.update(testClothItem)).thenReturn(0)

        // When
        val result = clothRepository.updateItem(testClothItem)

        // Then
        assertFalse(result)
        verify(clothDao).update(testClothItem)
    }

    // ===== DELETE テスト =====

    @Test
    fun `deleteItem_存在するClothItem_成功する`() = runTest {
        // Given
        `when`(clothDao.delete(testClothItem)).thenReturn(1)

        // When
        val result = clothRepository.deleteItem(testClothItem)

        // Then
        assertTrue(result)
        verify(clothDao).delete(testClothItem)
    }

    @Test
    fun `deleteItemById_存在するID_成功する`() = runTest {
        // Given
        `when`(clothDao.deleteById(1L)).thenReturn(1)

        // When
        val result = clothRepository.deleteItemById(1L)

        // Then
        assertTrue(result)
        verify(clothDao).deleteById(1L)
    }

    // ===== SELECT テスト =====

    @Test
    fun `getAllItems_データあり_正しいFlowを返す`() = runTest {
        // Given
        val items = listOf(testClothItem)
        `when`(clothDao.getAllItems()).thenReturn(flowOf(items))

        // When
        val result = clothRepository.getAllItems().first()

        // Then
        assertEquals(items, result)
        verify(clothDao).getAllItems()
    }

    @Test
    fun `getItemById_存在するID_正しいアイテムを返す`() = runTest {
        // Given
        `when`(clothDao.getItemById(1L)).thenReturn(testClothItem)

        // When
        val result = clothRepository.getItemById(1L)

        // Then
        assertEquals(testClothItem, result)
        verify(clothDao).getItemById(1L)
    }

    @Test
    fun `getItemById_存在しないID_nullを返す`() = runTest {
        // Given
        `when`(clothDao.getItemById(999L)).thenReturn(null)

        // When
        val result = clothRepository.getItemById(999L)

        // Then
        assertNull(result)
        verify(clothDao).getItemById(999L)
    }

    // ===== 検索・フィルタリングテスト =====

    @Test
    fun `getItemsByCategory_指定カテゴリ_正しいFlowを返す`() = runTest {
        // Given
        val items = listOf(testClothItem)
        `when`(clothDao.getItemsByCategory("シャツ")).thenReturn(flowOf(items))

        // When
        val result = clothRepository.getItemsByCategory("シャツ").first()

        // Then
        assertEquals(items, result)
        verify(clothDao).getItemsByCategory("シャツ")
    }

    @Test
    fun `searchItems_複合条件_正しいFlowを返す`() = runTest {
        // Given
        val items = listOf(testClothItem)
        val category = "シャツ"
        val color = "青"
        val minSize = 90
        val maxSize = 110
        `when`(clothDao.searchItems(category, color, minSize, maxSize)).thenReturn(flowOf(items))

        // When
        val result = clothRepository.searchItems(category, color, minSize, maxSize).first()

        // Then
        assertEquals(items, result)
        verify(clothDao).searchItems(category, color, minSize, maxSize)
    }

    // ===== 統計・集計テスト =====

    @Test
    fun `getItemCount_データあり_正しい数を返す`() = runTest {
        // Given
        `when`(clothDao.getItemCount()).thenReturn(5)

        // When
        val result = clothRepository.getItemCount()

        // Then
        assertEquals(5, result)
        verify(clothDao).getItemCount()
    }

    @Test
    fun `getItemCountByCategory_カテゴリ別統計_正しいMapを返す`() = runTest {
        // Given
        val categoryCounts = listOf(
            CategoryCount("シャツ", 3),
            CategoryCount("パンツ", 2)
        )
        `when`(clothDao.getItemCountByCategory()).thenReturn(categoryCounts)

        // When
        val result = clothRepository.getItemCountByCategory()

        // Then
        val expected = mapOf("シャツ" to 3, "パンツ" to 2)
        assertEquals(expected, result)
        verify(clothDao).getItemCountByCategory()
    }

    @Test
    fun `getItemCountByColor_色別統計_正しいMapを返す`() = runTest {
        // Given
        val colorCounts = listOf(
            ColorCount("青", 2),
            ColorCount("赤", 1)
        )
        `when`(clothDao.getItemCountByColor()).thenReturn(colorCounts)

        // When
        val result = clothRepository.getItemCountByColor()

        // Then
        val expected = mapOf("青" to 2, "赤" to 1)
        assertEquals(expected, result)
        verify(clothDao).getItemCountByColor()
    }

    @Test
    fun `getItemCountBySize_サイズ別統計_正しいMapを返す`() = runTest {
        // Given
        val sizeCounts = listOf(
            SizeCount(100, 2),
            SizeCount(110, 1)
        )
        `when`(clothDao.getItemCountBySize()).thenReturn(sizeCounts)

        // When
        val result = clothRepository.getItemCountBySize()

        // Then
        val expected = mapOf(100 to 2, 110 to 1)
        assertEquals(expected, result)
        verify(clothDao).getItemCountBySize()
    }

    // ===== バリデーションテスト =====

    @Test
    fun `insertItem_無効なTagData_例外をスロー`() = runTest {
        // Given - TagDataのvalidate()で範囲外判定される有効なサイズでオブジェクト作成
        val invalidTagData = TagData(size = 50, color = "青", category = "シャツ") // サイズ50は範囲外（60-160）だがinit通過用に正の数
        val invalidItem = testClothItem.copy(tagData = invalidTagData)

        // When & Then
        try {
            clothRepository.insertItem(invalidItem)
            fail("例外がスローされるべき")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("バリデーションエラー"))
        }
        
        // DAOは呼び出されない
        verify(clothDao, never()).insert(invalidItem)
    }

    @Test
    fun `updateItem_無効なTagData_例外をスロー`() = runTest {
        // Given - TagDataのvalidate()で範囲外判定される有効なサイズでオブジェクト作成
        val invalidTagData = TagData(size = 200, color = "青", category = "シャツ") // サイズ200は範囲外（60-160）だがinit通過用に正の数
        val invalidItem = testClothItem.copy(tagData = invalidTagData)

        // When & Then
        try {
            clothRepository.updateItem(invalidItem)
            fail("例外がスローされるべき")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("バリデーションエラー"))
        }
        
        // DAOは呼び出されない
        verify(clothDao, never()).update(invalidItem)
    }

    // ===== エラーハンドリングテスト =====

    @Test
    fun `insertItem_DAOで例外発生_例外を再スロー`() = runTest {
        // Given
        `when`(clothDao.insert(testClothItem)).thenThrow(RuntimeException("データベースエラー"))

        // When & Then
        try {
            clothRepository.insertItem(testClothItem)
            fail("例外がスローされるべき")
        } catch (e: RuntimeException) {
            assertEquals("データベースエラー", e.message)
        }
    }

    @Test
    fun `getAllItems_DAOで例外発生_例外を再スロー`() = runTest {
        // Given
        `when`(clothDao.getAllItems()).thenThrow(RuntimeException("データベースエラー"))

        // When & Then
        try {
            clothRepository.getAllItems().first()
            fail("例外がスローされるべき")
        } catch (e: RuntimeException) {
            assertEquals("データベースエラー", e.message)
        }
    }

    // ===== Task4: 新しいフィルター・検索機能のテスト =====

    @Test
    fun `searchItemsByText_正常な検索テキスト_正しいFlowを返す`() = runTest {
        // Given
        val searchText = "シャツ"
        val expectedItems = listOf(testClothItem)
        `when`(clothDao.searchItemsByText(searchText)).thenReturn(flowOf(expectedItems))

        // When
        val result = clothRepository.searchItemsByText(searchText).first()

        // Then
        assertEquals(expectedItems, result)
        verify(clothDao).searchItemsByText(searchText)
    }

    @Test
    fun `searchItemsByText_null検索テキスト_全件取得を返す`() = runTest {
        // Given
        val expectedItems = listOf(testClothItem)
        `when`(clothDao.searchItemsByText(null)).thenReturn(flowOf(expectedItems))

        // When
        val result = clothRepository.searchItemsByText(null).first()

        // Then
        assertEquals(expectedItems, result)
        verify(clothDao).searchItemsByText(null)
    }

    @Test
    fun `searchItemsByText_空文字列検索_全件取得を返す`() = runTest {
        // Given
        val expectedItems = listOf(testClothItem)
        `when`(clothDao.searchItemsByText("")).thenReturn(flowOf(expectedItems))

        // When
        val result = clothRepository.searchItemsByText("").first()

        // Then
        assertEquals(expectedItems, result)
        verify(clothDao).searchItemsByText("")
    }

    @Test
    fun `searchItemsByText_該当なし_空リストを返す`() = runTest {
        // Given
        val searchText = "存在しない"
        `when`(clothDao.searchItemsByText(searchText)).thenReturn(flowOf(emptyList()))

        // When
        val result = clothRepository.searchItemsByText(searchText).first()

        // Then
        assertTrue(result.isEmpty())
        verify(clothDao).searchItemsByText(searchText)
    }

    @Test
    fun `searchItemsWithFilters_サイズフィルターのみ_正しいFlowを返す`() = runTest {
        // Given
        val sizeFilters = listOf(100, 110)
        val expectedItems = listOf(testClothItem)
        `when`(clothDao.searchItemsWithFilters(sizeFilters, null, null, null))
            .thenReturn(flowOf(expectedItems))

        // When
        val result = clothRepository.searchItemsWithFilters(sizeFilters, null, null, null).first()

        // Then
        assertEquals(expectedItems, result)
        verify(clothDao).searchItemsWithFilters(sizeFilters, null, null, null)
    }

    @Test
    fun `searchItemsWithFilters_色フィルターのみ_正しいFlowを返す`() = runTest {
        // Given
        val colorFilters = listOf("青", "赤")
        val expectedItems = listOf(testClothItem)
        `when`(clothDao.searchItemsWithFilters(null, colorFilters, null, null))
            .thenReturn(flowOf(expectedItems))

        // When
        val result = clothRepository.searchItemsWithFilters(null, colorFilters, null, null).first()

        // Then
        assertEquals(expectedItems, result)
        verify(clothDao).searchItemsWithFilters(null, colorFilters, null, null)
    }

    @Test
    fun `searchItemsWithFilters_カテゴリフィルターのみ_正しいFlowを返す`() = runTest {
        // Given
        val categoryFilters = listOf("シャツ", "パンツ")
        val expectedItems = listOf(testClothItem)
        `when`(clothDao.searchItemsWithFilters(null, null, categoryFilters, null))
            .thenReturn(flowOf(expectedItems))

        // When
        val result = clothRepository.searchItemsWithFilters(null, null, categoryFilters, null).first()

        // Then
        assertEquals(expectedItems, result)
        verify(clothDao).searchItemsWithFilters(null, null, categoryFilters, null)
    }

    @Test
    fun `searchItemsWithFilters_複合条件_正しいFlowを返す`() = runTest {
        // Given
        val sizeFilters = listOf(100)
        val colorFilters = listOf("青")
        val categoryFilters = listOf("シャツ")
        val searchText = "カジュアル"
        val expectedItems = listOf(testClothItem)
        `when`(clothDao.searchItemsWithFilters(sizeFilters, colorFilters, categoryFilters, searchText))
            .thenReturn(flowOf(expectedItems))

        // When
        val result = clothRepository.searchItemsWithFilters(
            sizeFilters, colorFilters, categoryFilters, searchText
        ).first()

        // Then
        assertEquals(expectedItems, result)
        verify(clothDao).searchItemsWithFilters(sizeFilters, colorFilters, categoryFilters, searchText)
    }

    @Test
    fun `searchItemsWithFilters_空のフィルター_全件取得を返す`() = runTest {
        // Given
        val expectedItems = listOf(testClothItem)
        `when`(clothDao.searchItemsWithFilters(null, null, null, null))
            .thenReturn(flowOf(expectedItems))

        // When
        val result = clothRepository.searchItemsWithFilters(null, null, null, null).first()

        // Then
        assertEquals(expectedItems, result)
        verify(clothDao).searchItemsWithFilters(null, null, null, null)
    }

    @Test
    fun `searchItemsWithFilters_該当なし_空リストを返す`() = runTest {
        // Given
        val sizeFilters = listOf(999) // 存在しないサイズ
        `when`(clothDao.searchItemsWithFilters(sizeFilters, null, null, null))
            .thenReturn(flowOf(emptyList()))

        // When
        val result = clothRepository.searchItemsWithFilters(sizeFilters, null, null, null).first()

        // Then
        assertTrue(result.isEmpty())
        verify(clothDao).searchItemsWithFilters(sizeFilters, null, null, null)
    }

    @Test
    fun `getAvailableFilterOptions_正常なデータ_FilterOptionsを返す`() = runTest {
        // Given
        val expectedSizes = listOf(80, 90, 100, 110)
        val expectedColors = listOf("青", "赤", "緑")
        val expectedCategories = listOf("シャツ", "パンツ", "ジャケット")
        
        `when`(clothDao.getDistinctSizes()).thenReturn(expectedSizes)
        `when`(clothDao.getDistinctColors()).thenReturn(expectedColors)
        `when`(clothDao.getDistinctCategories()).thenReturn(expectedCategories)

        // When
        val result = clothRepository.getAvailableFilterOptions()

        // Then
        assertEquals(expectedSizes, result.availableSizes)
        assertEquals(expectedColors, result.availableColors)
        assertEquals(expectedCategories, result.availableCategories)
        
        verify(clothDao).getDistinctSizes()
        verify(clothDao).getDistinctColors()
        verify(clothDao).getDistinctCategories()
    }

    @Test
    fun `getAvailableFilterOptions_空のデータベース_空のFilterOptionsを返す`() = runTest {
        // Given
        `when`(clothDao.getDistinctSizes()).thenReturn(emptyList())
        `when`(clothDao.getDistinctColors()).thenReturn(emptyList())
        `when`(clothDao.getDistinctCategories()).thenReturn(emptyList())

        // When
        val result = clothRepository.getAvailableFilterOptions()

        // Then
        assertTrue(result.availableSizes.isEmpty())
        assertTrue(result.availableColors.isEmpty())
        assertTrue(result.availableCategories.isEmpty())
        assertTrue(result.isEmpty())
        
        verify(clothDao).getDistinctSizes()
        verify(clothDao).getDistinctColors()
        verify(clothDao).getDistinctCategories()
    }

    @Test
    fun `getAvailableFilterOptions_DAOで例外発生_RuntimeExceptionを再スロー`() = runTest {
        // Given
        `when`(clothDao.getDistinctSizes()).thenThrow(RuntimeException("データベースアクセスエラー"))

        // When & Then
        try {
            clothRepository.getAvailableFilterOptions()
            fail("RuntimeExceptionがスローされるべき")
        } catch (e: RuntimeException) {
            assertEquals("データベースアクセスエラー", e.message)
        }
        
        verify(clothDao).getDistinctSizes()
    }

    @Test
    fun `searchItemsByText_DAOで例外発生_例外を再スロー`() = runTest {
        // Given
        val searchText = "テスト"
        `when`(clothDao.searchItemsByText(searchText)).thenThrow(RuntimeException("検索エラー"))

        // When & Then
        try {
            clothRepository.searchItemsByText(searchText).first()
            fail("RuntimeExceptionがスローされるべき")
        } catch (e: RuntimeException) {
            assertEquals("検索エラー", e.message)
        }
        
        verify(clothDao).searchItemsByText(searchText)
    }

    @Test
    fun `searchItemsWithFilters_DAOで例外発生_例外を再スロー`() = runTest {
        // Given
        val sizeFilters = listOf(100)
        `when`(clothDao.searchItemsWithFilters(sizeFilters, null, null, null))
            .thenThrow(RuntimeException("フィルター検索エラー"))

        // When & Then
        try {
            clothRepository.searchItemsWithFilters(sizeFilters, null, null, null).first()
            fail("RuntimeExceptionがスローされるべき")
        } catch (e: RuntimeException) {
            assertEquals("フィルター検索エラー", e.message)
        }
        
        verify(clothDao).searchItemsWithFilters(sizeFilters, null, null, null)
    }

    // ===== 🔴 TDD Red: Task3 メモ検索機能特化テスト =====

    @Test
    fun `searchItemsByText_メモ内容で検索_正しく動作する`() = runTest {
        // Given - メモ付きテストデータ
        val searchText = "お気に入り"
        val itemWithMemo = testClothItem.copy(memo = "お気に入りの一枚")
        val expectedItems = listOf(itemWithMemo)

        `when`(clothDao.searchItemsByText(searchText)).thenReturn(flowOf(expectedItems))

        // When
        val result = clothRepository.searchItemsByText(searchText).first()

        // Then
        assertEquals(expectedItems, result)
        assertTrue("メモにお気に入りを含む", result[0].memo.contains("お気に入り"))
        verify(clothDao).searchItemsByText(searchText)
    }

    @Test
    fun `searchItemsByText_メモと既存フィールドの複合検索_正しく動作する`() = runTest {
        // Given - 「シャツ」でカテゴリとメモ両方にヒット
        val searchText = "シャツ"
        val categoryShirt = testClothItem.copy(tagData = testTagData.copy(category = "シャツ"), memo = "")
        val memoShirt = testClothItem.copy(
            id = 2L, 
            tagData = testTagData.copy(category = "パンツ"), 
            memo = "シャツに合う"
        )
        val expectedItems = listOf(categoryShirt, memoShirt)

        `when`(clothDao.searchItemsByText(searchText)).thenReturn(flowOf(expectedItems))

        // When
        val result = clothRepository.searchItemsByText(searchText).first()

        // Then
        assertEquals(2, result.size)
        assertTrue("カテゴリまたはメモにシャツを含む", 
            result.any { it.tagData.category.contains("シャツ") || it.memo.contains("シャツ") }
        )
        verify(clothDao).searchItemsByText(searchText)
    }

    @Test
    fun `searchItemsByText_空メモの場合_適切に処理する`() = runTest {
        // Given - 存在しないキーワードで検索（空メモアイテムでは引っかからない）
        val searchText = "存在しないキーワード"
        val expectedItems = emptyList<ClothItem>()

        `when`(clothDao.searchItemsByText(searchText)).thenReturn(flowOf(expectedItems))

        // When
        val result = clothRepository.searchItemsByText(searchText).first()

        // Then
        assertTrue("空メモでは該当なし", result.isEmpty())
        verify(clothDao).searchItemsByText(searchText)
    }

    @Test
    fun `searchItemsWithFilters_メモ検索とフィルター組み合わせ_正しく動作する`() = runTest {
        // Given - サイズフィルター + メモ検索
        val sizeFilters = listOf(100)
        val searchText = "お気に入り"
        val expectedItem = testClothItem.copy(
            tagData = testTagData.copy(size = 100),
            memo = "お気に入りの服"
        )
        val expectedItems = listOf(expectedItem)

        `when`(clothDao.searchItemsWithFilters(sizeFilters, null, null, searchText))
            .thenReturn(flowOf(expectedItems))

        // When
        val result = clothRepository.searchItemsWithFilters(sizeFilters, null, null, searchText).first()

        // Then
        assertEquals(1, result.size)
        assertEquals(100, result[0].tagData.size)
        assertTrue("メモにお気に入りを含む", result[0].memo.contains("お気に入り"))
        verify(clothDao).searchItemsWithFilters(sizeFilters, null, null, searchText)
    }

    @Test
    fun `searchItemsWithFilters_複合条件とメモ検索_正しく動作する`() = runTest {
        // Given - サイズ、色、カテゴリ + メモ検索
        val sizeFilters = listOf(100, 110)
        val colorFilters = listOf("赤")
        val categoryFilters = listOf("シャツ")
        val searchText = "フォーマル"
        
        val expectedItem = testClothItem.copy(
            tagData = testTagData.copy(size = 100, color = "赤", category = "シャツ"),
            memo = "フォーマル用途"
        )
        val expectedItems = listOf(expectedItem)

        `when`(clothDao.searchItemsWithFilters(sizeFilters, colorFilters, categoryFilters, searchText))
            .thenReturn(flowOf(expectedItems))

        // When
        val result = clothRepository.searchItemsWithFilters(
            sizeFilters, colorFilters, categoryFilters, searchText
        ).first()

        // Then
        assertEquals(1, result.size)
        val item = result[0]
        assertTrue("サイズ条件に合致", sizeFilters.contains(item.tagData.size))
        assertTrue("色条件に合致", colorFilters.contains(item.tagData.color))
        assertTrue("カテゴリ条件に合致", categoryFilters.contains(item.tagData.category))
        assertTrue("メモ検索条件に合致", item.memo.contains("フォーマル"))
        
        verify(clothDao).searchItemsWithFilters(sizeFilters, colorFilters, categoryFilters, searchText)
    }

    @Test
    fun `searchItemsWithFilters_メモ特殊文字検索_正しく動作する`() = runTest {
        // Given - 特殊文字を含むメモ検索
        val searchText = "100%"
        val expectedItem = testClothItem.copy(memo = "100%コットン素材")
        val expectedItems = listOf(expectedItem)

        `when`(clothDao.searchItemsWithFilters(null, null, null, searchText))
            .thenReturn(flowOf(expectedItems))

        // When
        val result = clothRepository.searchItemsWithFilters(null, null, null, searchText).first()

        // Then
        assertEquals(1, result.size)
        assertTrue("特殊文字を含むメモ検索", result[0].memo.contains("100%"))
        verify(clothDao).searchItemsWithFilters(null, null, null, searchText)
    }

    @Test
    fun `getFilteredItemCount_メモ検索条件_正しくカウントする`() = runTest {
        // Given - メモ検索での総数取得
        val searchText = "カウントテスト"
        val expectedCount = 3

        `when`(clothDao.getFilteredItemCount(null, null, null, searchText))
            .thenReturn(expectedCount)

        // When
        val result = clothRepository.getFilteredItemCount(null, null, null, searchText)

        // Then
        assertEquals(3, result)
        verify(clothDao).getFilteredItemCount(null, null, null, searchText)
    }

    @Test
    fun `getFilteredItemCount_フィルターとメモ検索組み合わせ_正しくカウントする`() = runTest {
        // Given - サイズフィルター + メモ検索での総数取得
        val sizeFilters = listOf(100, 110)
        val searchText = "カウントテスト"
        val expectedCount = 2

        `when`(clothDao.getFilteredItemCount(sizeFilters, null, null, searchText))
            .thenReturn(expectedCount)

        // When
        val result = clothRepository.getFilteredItemCount(sizeFilters, null, null, searchText)

        // Then
        assertEquals(2, result)
        verify(clothDao).getFilteredItemCount(sizeFilters, null, null, searchText)
    }
}