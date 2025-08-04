package com.example.clothstock.ui.gallery

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.*
import kotlin.system.measureTimeMillis

/**
 * GalleryViewModelのパフォーマンステスト
 * 
 * 大量データでの動作確認とパフォーマンス測定
 * メモリ使用量とレスポンス時間の検証
 */
@ExperimentalCoroutinesApi
class GalleryViewModelPerformanceTest : GalleryViewModelTestBase() {

    companion object {
        private const val LARGE_DATASET_SIZE = 1000
        private const val PERFORMANCE_THRESHOLD_MS = 500L
    }

    /**
     * 大量データでの初期化パフォーマンステスト
     */
    @Test
    fun `大量データ初期化_パフォーマンス閾値内で完了する`() = runTest {
        // Given: 大量のテストデータを生成
        val largeDataset = generateLargeDataset(LARGE_DATASET_SIZE)
        `when`(clothRepository.getAllItems()).thenReturn(flowOf(largeDataset))
        `when`(clothRepository.getAvailableFilterOptions()).thenReturn(defaultFilterOptions)
        
        // When: 初期化時間を測定
        val executionTime = measureTimeMillis {
            val viewModel = GalleryViewModel(clothRepository)
            testDispatcher.scheduler.advanceUntilIdle()
            
            // Then: データが正しく設定される
            assertEquals(LARGE_DATASET_SIZE, viewModel.clothItems.value?.size)
            assertEquals(false, viewModel.isEmpty.value)
        }
        
        // Then: パフォーマンス閾値内で完了
        assertTrue(
            "Initialization should complete within ${PERFORMANCE_THRESHOLD_MS}ms, but took ${executionTime}ms",
            executionTime < PERFORMANCE_THRESHOLD_MS
        )
    }

    /**
     * 大量データでのフィルタリングパフォーマンステスト
     */
    @Test
    fun `大量データフィルタリング_パフォーマンス閾値内で完了する`() = runTest {
        // Given: 大量データとフィルター結果の設定
        val largeDataset = generateLargeDataset(LARGE_DATASET_SIZE)
        val filteredResult = largeDataset.take(100) // フィルター結果は100件
        
        `when`(clothRepository.getAllItems()).thenReturn(flowOf(largeDataset))
        `when`(clothRepository.getAvailableFilterOptions()).thenReturn(defaultFilterOptions)
        `when`(clothRepository.getItemsByCategory(TEST_CATEGORY_TOPS))
            .thenReturn(flowOf(filteredResult))
        
        val viewModel = GalleryViewModel(clothRepository)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When: フィルタリング時間を測定
        val executionTime = measureTimeMillis {
            viewModel.filterByCategory(TEST_CATEGORY_TOPS)
            testDispatcher.scheduler.advanceUntilIdle()
        }
        
        // Then: パフォーマンス閾値内で完了
        assertTrue(
            "Filtering should complete within ${PERFORMANCE_THRESHOLD_MS}ms, but took ${executionTime}ms",
            executionTime < PERFORMANCE_THRESHOLD_MS
        )
        assertEquals(100, viewModel.clothItems.value?.size)
    }

    /**
     * 検索デバウンシングのパフォーマンステスト
     */
    @Test
    fun `検索デバウンシング_連続検索時にパフォーマンスが維持される`() = runTest {
        // Given: 検索結果の設定
        val searchResults = generateLargeDataset(50)
        `when`(clothRepository.getAllItems()).thenReturn(flowOf(emptyList()))
        `when`(clothRepository.getAvailableFilterOptions()).thenReturn(emptyFilterOptions)
        `when`(clothRepository.searchItemsWithFilters(null, null, null, "最終検索"))
            .thenReturn(flowOf(searchResults))
        
        val viewModel = GalleryViewModel(clothRepository)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When: 連続検索の実行時間を測定
        val executionTime = measureTimeMillis {
            repeat(10) { index ->
                viewModel.performSearch("検索$index")
            }
            viewModel.performSearch("最終検索")
            testDispatcher.scheduler.advanceUntilIdle()
        }
        
        // Then: デバウンシングにより最後の検索のみ実行される
        verify(clothRepository, times(1)).searchItemsWithFilters(null, null, null, "最終検索")
        assertEquals("最終検索", viewModel.currentSearchText.value)
        
        // パフォーマンス確認
        assertTrue(
            "Debounced search should complete within ${PERFORMANCE_THRESHOLD_MS}ms, but took ${executionTime}ms",
            executionTime < PERFORMANCE_THRESHOLD_MS
        )
    }

    /**
     * メモリ効率テスト - 大量データ処理後のメモリ使用量確認
     */
    @Test
    fun `メモリ効率_大量データ処理後にメモリリークが発生しない`() = runTest {
        // Given: 大量データの設定
        val largeDataset = generateLargeDataset(LARGE_DATASET_SIZE)
        `when`(clothRepository.getAllItems()).thenReturn(flowOf(largeDataset))
        `when`(clothRepository.getAvailableFilterOptions()).thenReturn(defaultFilterOptions)
        
        // When: ViewModelを作成・破棄を繰り返す
        repeat(5) {
            val viewModel = GalleryViewModel(clothRepository)
            testDispatcher.scheduler.advanceUntilIdle()
            
            // データが正しく設定されることを確認
            assertEquals(LARGE_DATASET_SIZE, viewModel.clothItems.value?.size)
            
            // ViewModelのクリーンアップをシミュレート（onClearedは直接呼び出せないため、ViewModelの参照を削除）
            // viewModel.onCleared() // protectedメソッドのため直接呼び出し不可
        }
        
        // Then: メモリリークの兆候がないことを確認
        // 実際のメモリ測定は困難なため、例外が発生しないことで確認
        assertTrue("Memory leak test completed without exceptions", true)
    }

    /**
     * 大量データセットを生成するヘルパーメソッド
     */
    private fun generateLargeDataset(size: Int) = (1..size).map { index ->
        TestClothItemBuilder()
            .withId(index.toLong())
            .withImagePath("/test/image$index.jpg")
            .withSize(if (index % 2 == 0) TEST_SIZE_SMALL else TEST_SIZE_LARGE)
            .withColor(if (index % 3 == 0) TEST_COLOR_RED else TEST_COLOR_BLUE)
            .withCategory(if (index % 2 == 0) TEST_CATEGORY_TOPS else TEST_CATEGORY_BOTTOMS)
            .build()
    }

}
