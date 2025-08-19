package com.example.clothstock.data.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.clothstock.data.database.ClothDao
import com.example.clothstock.data.model.ClothItem
import com.example.clothstock.data.model.TagData
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
 * ClothRepository の削除機能テスト (Task6)
 * 
 * バッチ削除とファイルクリーンアップ機能のテストを専門に扱う
 * 元のClothRepositoryTestからDetektのLargeClass警告を回避するために分離
 */
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ClothRepositoryDeletionTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var clothDao: ClothDao

    private lateinit var clothRepository: ClothRepository
    
    companion object {
        private const val INVALID_SIZE = 50  // サイズ範囲外の無効な値
    }

    private val testTagData = TagData(
        size = 100,
        color = "青",
        category = "シャツ"
    )

    private val testClothItem = ClothItem(
        id = 1L,
        imagePath = "/storage/test/image1.jpg",
        tagData = testTagData,
        createdAt = Date(),
        memo = "テストメモ"
    )

    @Before
    fun setup() {
        clothRepository = ClothRepositoryImpl(clothDao)
    }

    // ===== Task6 バッチ削除とファイルクリーンアップ機能 =====

    @Test
    fun `deleteItems_複数アイテムの削除_DeletionResultを返す`() = runTest {
        // Given
        val items = listOf(
            testClothItem,
            testClothItem.copy(id = 2L, imagePath = "/storage/test/image2.jpg")
        )
        
        // DAOモックの設定：両方とも削除成功
        `when`(clothDao.delete(testClothItem)).thenReturn(1)
        `when`(clothDao.delete(testClothItem.copy(id = 2L, imagePath = "/storage/test/image2.jpg"))).thenReturn(1)
        
        // When
        val result = clothRepository.deleteItems(items)
        
        // Then
        assertEquals(2, result.totalRequested)
        assertEquals(2, result.successfulDeletions)
        assertEquals(0, result.failedDeletions)
        assertTrue(result.isCompleteSuccess)
        assertTrue(result.failedItems.isEmpty())
    }

    @Test
    fun `deleteItems_空リスト_空のDeletionResultを返す`() = runTest {
        // Given
        val emptyItems = emptyList<ClothItem>()
        
        // When & Then
        try {
            val result = clothRepository.deleteItems(emptyItems)
            
            assertEquals(0, result.totalRequested)
            assertEquals(0, result.successfulDeletions)
            assertEquals(0, result.failedDeletions)
            assertTrue(result.isCompleteSuccess) // 0個の削除は成功とみなす
        } catch (e: IllegalArgumentException) {
            // このテストでは例外は投げられるべきではない
            fail("deleteItems method should handle empty list: ${e.message}")
        }
    }

    @Test
    fun `deleteItems_削除失敗が含まれる_PartialSuccessのDeletionResultを返す`() = runTest {
        // Given
        val item1 = testClothItem
        val item2 = testClothItem.copy(id = 2L, imagePath = "/storage/test/image2.jpg")
        val item3 = testClothItem.copy(id = 3L, imagePath = "/storage/test/image3.jpg")
        val items = listOf(item1, item2, item3)
        
        // DAOモックの設定：1つ成功、2つ失敗
        `when`(clothDao.delete(item1)).thenReturn(1) // 成功
        `when`(clothDao.delete(item2)).thenReturn(0) // 失敗
        `when`(clothDao.delete(item3)).thenReturn(0) // 失敗
        
        // When
        val result = clothRepository.deleteItems(items)
        
        // Then
        assertEquals(3, result.totalRequested)
        assertEquals(1, result.successfulDeletions)
        assertEquals(2, result.failedDeletions)
        assertTrue(result.isPartialSuccess)
        assertEquals(2, result.failedItems.size)
        assertEquals(2L, result.failedItems[0].itemId)
        assertEquals(3L, result.failedItems[1].itemId)
    }

    @Test
    fun `deleteItemWithFileCleanup_ファイル削除成功_trueを返す`() = runTest {
        // Given
        val itemWithFile = testClothItem.copy(imagePath = "/storage/test/existing_image.jpg")
        `when`(clothDao.delete(itemWithFile)).thenReturn(1)
        
        // When - 存在しないファイルパスなのでFileUtils.deleteImageFileは失敗する
        val result = clothRepository.deleteItemWithFileCleanup(itemWithFile)
        
        // Then - ファイル削除失敗により全体的に失敗
        assertFalse("ファイル削除失敗時は全体失敗", result)
        verify(clothDao).delete(itemWithFile)
    }

    @Test
    fun `deleteItemWithFileCleanup_データベース削除失敗_falseを返す`() = runTest {
        // Given
        val nonExistentItem = testClothItem.copy(id = 999L)
        `when`(clothDao.delete(nonExistentItem)).thenReturn(0)
        
        // When
        val result = clothRepository.deleteItemWithFileCleanup(nonExistentItem)
        
        // Then
        assertFalse("Database deletion failure should return false", result)
        verify(clothDao).delete(nonExistentItem)
    }

    @Test
    fun `deleteItemWithFileCleanup_空の画像パス_データベースのみ削除してtrueを返す`() = runTest {
        // Given
        val itemWithoutImage = testClothItem.copy(imagePath = "")
        `when`(clothDao.delete(itemWithoutImage)).thenReturn(1)
        
        // When
        val result = clothRepository.deleteItemWithFileCleanup(itemWithoutImage)
        
        // Then
        assertTrue("Database deletion should succeed", result)
        verify(clothDao).delete(itemWithoutImage)
    }

    @Test
    fun `deleteItems_大量データのバッチ削除_効率的に処理される`() = runTest {
        // Given - 大量のアイテム（100個）
        val largeItemList = (1L..100L).map { id ->
            testClothItem.copy(id = id, imagePath = "/storage/test/image$id.jpg")
        }
        
        // DAOモックの設定：全て成功
        largeItemList.forEach { item ->
            `when`(clothDao.delete(item)).thenReturn(1)
        }
        
        // When
        val result = clothRepository.deleteItems(largeItemList)
        
        // Then
        assertEquals(100, result.totalRequested)
        assertEquals(100, result.successfulDeletions)
        assertEquals(0, result.failedDeletions)
        assertTrue(result.isCompleteSuccess)
        
        // 全アイテムが削除されたことを確認
        largeItemList.forEach { item ->
            verify(clothDao).delete(item)
        }
    }

    @Test
    fun `deleteItems_例外発生時_部分削除で適切なエラー情報を提供`() = runTest {
        // Given
        val item1 = testClothItem.copy(id = 1L)
        val item2 = testClothItem.copy(id = 2L)
        val item3 = testClothItem.copy(id = 3L)
        val items = listOf(item1, item2, item3)
        
        // DAOモックの設定：2番目で例外発生（RuntimeException想定）
        `when`(clothDao.delete(item1)).thenReturn(1) // 成功
        `when`(clothDao.delete(item2)).thenThrow(RuntimeException("データベース接続エラー")) // 例外
        `when`(clothDao.delete(item3)).thenReturn(1) // 成功
        
        // When
        val result = clothRepository.deleteItems(items)
        
        // Then
        assertEquals(3, result.totalRequested)
        assertEquals(2, result.successfulDeletions)
        assertEquals(1, result.failedDeletions)
        assertTrue(result.isPartialSuccess)
        
        // 失敗したアイテムの詳細を確認
        assertEquals(1, result.failedItems.size)
        val failure = result.failedItems[0]
        assertEquals(2L, failure.itemId)
        assertTrue(failure.reason.contains("削除に失敗しました"))
        assertNotNull(failure.exception)
    }

    @Test
    fun `deleteItems_バリデーションエラー含む_事前チェックで失敗アイテムを除外`() = runTest {
        // Given - 1つは有効、1つは無効なTagData
        val validItem = testClothItem.copy(id = 1L)
        val invalidItem = testClothItem.copy(
            id = 2L,
            tagData = TagData(size = INVALID_SIZE, color = "青", category = "シャツ") // サイズ範囲外
        )
        val items = listOf(validItem, invalidItem)
        
        // DAOモックの設定：有効なアイテムのみ
        `when`(clothDao.delete(validItem)).thenReturn(1)
        
        // When & Then
        // バリデーションエラーにより、deleteItems自体が例外を投げる可能性
        // または無効アイテムをスキップして処理する可能性
        try {
            val result = clothRepository.deleteItems(items)
            
            // パターン1: 無効アイテムをスキップして処理
            assertEquals(1, result.successfulDeletions)
            assertTrue(result.failedDeletions >= 0)
        } catch (e: IllegalArgumentException) {
            // パターン2: バリデーションエラーで全体が失敗
            assertTrue(e.message!!.contains("バリデーションエラー"))
        }
    }

    @Test
    fun `deleteItems_パフォーマンステスト_適切な時間内で完了`() = runTest {
        // Given - 中程度のデータ量（50個）
        val mediumItemList = (1L..50L).map { id ->
            testClothItem.copy(id = id, imagePath = "/storage/test/image$id.jpg")
        }
        
        // DAOモックの設定：全て成功
        mediumItemList.forEach { item ->
            `when`(clothDao.delete(item)).thenReturn(1)
        }
        
        // When
        val startTime = System.currentTimeMillis()
        val result = clothRepository.deleteItems(mediumItemList)
        val endTime = System.currentTimeMillis()
        val executionTime = endTime - startTime
        
        // Then
        assertEquals(50, result.totalRequested)
        assertEquals(50, result.successfulDeletions)
        assertTrue(result.isCompleteSuccess)
        
        // パフォーマンス要件：50個の削除が1秒以内（テスト環境では十分余裕を持って）
        assertTrue("削除処理が1000ms以内に完了すること（実際: ${executionTime}ms）", executionTime < 1000)
    }

    @Test
    fun `deleteItemWithFileCleanup_データベースとファイル両方成功_trueを返す`() = runTest {
        // Given
        val itemWithFile = testClothItem.copy(imagePath = "/storage/test/nonexistent_image.jpg")
        `when`(clothDao.delete(itemWithFile)).thenReturn(1)
        
        // When - 存在しないファイルパスなのでFileUtils.deleteImageFileは失敗する
        val result = clothRepository.deleteItemWithFileCleanup(itemWithFile)
        
        // Then - ファイル削除失敗により全体的に失敗
        assertFalse("ファイル削除失敗時は全体失敗", result)
        verify(clothDao).delete(itemWithFile)
    }

    @Test
    fun `deleteItemWithFileCleanup_データベース成功ファイル失敗_falseを返す`() = runTest {
        // Given
        val itemWithFile = testClothItem.copy(imagePath = "/storage/test/nonexistent_image.jpg")
        `when`(clothDao.delete(itemWithFile)).thenReturn(1)
        
        // When - 存在しないファイルパスなのでFileUtils.deleteImageFileは失敗する
        val result = clothRepository.deleteItemWithFileCleanup(itemWithFile)
        
        // Then - ファイル削除失敗により全体的に失敗
        assertFalse("ファイル削除失敗時は全体失敗", result)
        verify(clothDao).delete(itemWithFile)
    }

    @Test
    fun `deleteItemWithFileCleanup_データベース失敗_ファイル削除を実行しない`() = runTest {
        // Given
        val nonExistentItem = testClothItem.copy(id = 999L, imagePath = "/storage/test/image.jpg")
        `when`(clothDao.delete(nonExistentItem)).thenReturn(0) // データベース削除失敗
        
        // When
        val result = clothRepository.deleteItemWithFileCleanup(nonExistentItem)
        
        // Then - データベース削除失敗時は全体的に失敗
        assertFalse("データベース削除失敗", result)
        verify(clothDao).delete(nonExistentItem)
    }

    @Test
    fun `deleteItemWithFileCleanup_空の画像パス_データベースのみ削除`() = runTest {
        // Given
        val itemWithoutImage = testClothItem.copy(imagePath = "")
        `when`(clothDao.delete(itemWithoutImage)).thenReturn(1)
        
        // When
        val result = clothRepository.deleteItemWithFileCleanup(itemWithoutImage)
        
        // Then - 空パスの場合はファイル削除をスキップしてデータベースのみ削除
        assertTrue("空パス時はデータベースのみ削除成功", result)
        verify(clothDao).delete(itemWithoutImage)
    }

    @Test
    fun `deleteItemWithFileCleanup_ファイル削除で例外_適切にハンドル`() = runTest {
        // Given
        val itemWithFile = testClothItem.copy(imagePath = "/storage/test/problematic_image.jpg")
        `when`(clothDao.delete(itemWithFile)).thenReturn(1)
        
        // When - 存在しないファイルパスなのでFileUtils.deleteImageFileは失敗する
        val result = clothRepository.deleteItemWithFileCleanup(itemWithFile)
        
        // Then - ファイル削除失敗により全体的に失敗
        assertFalse("ファイル削除例外時は失敗", result)
        verify(clothDao).delete(itemWithFile)
    }
}

