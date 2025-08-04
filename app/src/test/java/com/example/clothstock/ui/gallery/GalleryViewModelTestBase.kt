package com.example.clothstock.ui.gallery

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.clothstock.data.model.ClothItem
import com.example.clothstock.data.model.TagData
import com.example.clothstock.data.repository.ClothRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Assert.assertEquals
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.Mockito.`when`
import kotlinx.coroutines.runBlocking
import java.util.Date

/**
 * GalleryViewModelテストの基底クラス
 * 
 * 共通のセットアップとヘルパーメソッドを提供
 * テストクラスの分割時に継承して使用
 */
@ExperimentalCoroutinesApi
abstract class GalleryViewModelTestBase {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    protected val testDispatcher = StandardTestDispatcher()

    @Mock
    protected lateinit var clothRepository: ClothRepository

    // テスト定数
    companion object {
        const val TEST_SIZE_SMALL = 100
        const val TEST_SIZE_LARGE = 120
        const val TEST_COLOR_RED = "赤"
        const val TEST_COLOR_BLUE = "青"
        const val TEST_CATEGORY_TOPS = "トップス"
        const val TEST_CATEGORY_BOTTOMS = "ボトムス"
        const val TEST_IMAGE_PATH_1 = "/test/image1.jpg"
        const val TEST_IMAGE_PATH_2 = "/test/image2.jpg"
        const val ERROR_MESSAGE_DELETE_FAILED = "アイテムの削除に失敗しました"
        const val SEARCH_DEBOUNCE_DELAY_MS = 300L
    }

    // テストデータファクトリー
    protected class TestClothItemBuilder {
        private var id: Long = 1L
        private var imagePath: String = TEST_IMAGE_PATH_1
        private var size: Int = TEST_SIZE_SMALL
        private var color: String = TEST_COLOR_RED
        private var category: String = TEST_CATEGORY_TOPS
        private var createdAt: Date = Date()

        fun withId(id: Long) = apply { this.id = id }
        fun withImagePath(path: String) = apply { this.imagePath = path }
        fun withSize(size: Int) = apply { this.size = size }
        fun withColor(color: String) = apply { this.color = color }
        fun withCategory(category: String) = apply { this.category = category }
        fun withCreatedAt(date: Date) = apply { this.createdAt = date }

        fun build() = ClothItem(
            id = id,
            imagePath = imagePath,
            tagData = TagData(size = size, color = color, category = category),
            createdAt = createdAt
        )
    }

    protected val testClothItems = listOf(
        TestClothItemBuilder()
            .withId(1L)
            .withImagePath(TEST_IMAGE_PATH_1)
            .withSize(TEST_SIZE_SMALL)
            .withColor(TEST_COLOR_RED)
            .withCategory(TEST_CATEGORY_TOPS)
            .build(),
        TestClothItemBuilder()
            .withId(2L)
            .withImagePath(TEST_IMAGE_PATH_2)
            .withSize(TEST_SIZE_LARGE)
            .withColor(TEST_COLOR_BLUE)
            .withCategory(TEST_CATEGORY_BOTTOMS)
            .build()
    )

    protected val defaultFilterOptions = com.example.clothstock.data.model.FilterOptions(
        availableSizes = listOf(TEST_SIZE_SMALL, TEST_SIZE_LARGE),
        availableColors = listOf(TEST_COLOR_RED, TEST_COLOR_BLUE),
        availableCategories = listOf(TEST_CATEGORY_TOPS, TEST_CATEGORY_BOTTOMS)
    )

    protected val emptyFilterOptions = com.example.clothstock.data.model.FilterOptions(
        availableSizes = emptyList(),
        availableColors = emptyList(),
        availableCategories = emptyList()
    )

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * リポジトリの基本的なモック設定を行うヘルパーメソッド
     */
    protected fun setupBasicRepositoryMocks(
        items: List<ClothItem> = testClothItems,
        filterOptions: com.example.clothstock.data.model.FilterOptions = defaultFilterOptions
    ) {
        `when`(clothRepository.getAllItems()).thenReturn(flowOf(items))
        runBlocking {
            `when`(clothRepository.getAvailableFilterOptions()).thenReturn(filterOptions)
        }
    }

    /**
     * ViewModelを初期化し、テストディスパッチャーを進めるヘルパーメソッド
     */
    protected fun createViewModelAndAdvance(): GalleryViewModel {
        val viewModel = GalleryViewModel(clothRepository)
        testDispatcher.scheduler.advanceUntilIdle()
        return viewModel
    }

    /**
     * 特定のフィルター条件でのモック設定を行うヘルパーメソッド
     */
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
        color?.let { 
            `when`(clothRepository.getItemsByColor(it)).thenReturn(flowOf(expectedResult))
        }
        size?.let { 
            `when`(clothRepository.getItemsBySizeRange(it, it)).thenReturn(flowOf(expectedResult))
        }
        searchText?.let { 
            `when`(clothRepository.searchItemsWithFilters(null, null, null, it)).thenReturn(flowOf(expectedResult))
        }
    }

    /**
     * ViewModelの基本状態をアサートするヘルパーメソッド
     */
    protected fun assertViewModelInitialState(viewModel: GalleryViewModel, expectedItems: List<ClothItem>) {
        assertEquals(expectedItems, viewModel.clothItems.value)
        assertEquals(expectedItems.isEmpty(), viewModel.isEmpty.value)
        assertEquals(false, viewModel.isLoading.value)
    }
}