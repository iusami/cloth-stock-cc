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
        `when`(clothDao.searchItems("シャツ", "青", 90, 110)).thenReturn(flowOf(items))

        // When
        val result = clothRepository.searchItems("シャツ", "青", 90, 110).first()

        // Then
        assertEquals(items, result)
        verify(clothDao).searchItems("シャツ", "青", 90, 110)
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
        // Given
        val invalidTagData = TagData(size = 50, color = "青", category = "シャツ") // サイズが範囲外
        val invalidItem = testClothItem.copy(tagData = invalidTagData)

        // When & Then
        try {
            clothRepository.insertItem(invalidItem)
            fail("例外がスローされるべき")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("バリデーションエラー"))
        }
        
        // DAOは呼び出されない
        verify(clothDao, never()).insert(any())
    }

    @Test
    fun `updateItem_無効なTagData_例外をスロー`() = runTest {
        // Given
        val invalidTagData = TagData(size = 200, color = "青", category = "シャツ") // サイズが範囲外
        val invalidItem = testClothItem.copy(tagData = invalidTagData)

        // When & Then
        try {
            clothRepository.updateItem(invalidItem)
            fail("例外がスローされるべき")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("バリデーションエラー"))
        }
        
        // DAOは呼び出されない
        verify(clothDao, never()).update(any())
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
}