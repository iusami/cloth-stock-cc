package com.example.clothstock.ui.tagging

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.example.clothstock.data.model.ClothItem
import com.example.clothstock.data.model.TagData
import com.example.clothstock.data.model.ValidationResult
import com.example.clothstock.data.repository.ClothRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue

/**
 * TaggingViewModelのユニットテスト
 * 
 * TDDアプローチに従い、最初に失敗するテストを作成してから実装を行う
 * タグ管理、バリデーション、保存機能、状態管理をテスト
 */
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class TaggingViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var clothRepository: ClothRepository

    @Mock
    private lateinit var tagDataObserver: Observer<TagData>

    @Mock
    private lateinit var validationErrorObserver: Observer<String?>

    @Mock
    private lateinit var isLoadingObserver: Observer<Boolean>

    @Mock
    private lateinit var saveResultObserver: Observer<TaggingViewModel.SaveResult>

    private lateinit var viewModel: TaggingViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        
        // テスト開始時にViewModelを初期化
        viewModel = TaggingViewModel(clothRepository)
        
        // Observerを登録
        viewModel.tagData.observeForever(tagDataObserver)
        viewModel.validationError.observeForever(validationErrorObserver)
        viewModel.isLoading.observeForever(isLoadingObserver)
        viewModel.saveResult.observeForever(saveResultObserver)
    }

    // ===== 初期状態テスト =====

    @Test
    fun 初期状態_デフォルトTagDataが設定されている() {
        // Given: ViewModelが初期化済み
        
        // When: 初期状態を確認
        val tagData = viewModel.tagData.value
        
        // Then: デフォルト値が設定されている
        assertNotNull(tagData)
        assertEquals(TagData.DEFAULT_SIZE, tagData!!.size)
        assertEquals(TagData.DEFAULT_COLOR, tagData.color)
        assertEquals(TagData.DEFAULT_CATEGORY, tagData.category)
    }

    @Test
    fun 初期状態_ローディング中ではない() {
        // Given: ViewModelが初期化済み
        
        // When: 初期状態を確認
        val isLoading = viewModel.isLoading.value
        
        // Then: ローディング中ではない
        assertFalse(isLoading ?: true)
    }

    @Test
    fun 初期状態_バリデーションエラーなし() {
        // Given: ViewModelが初期化済み
        
        // When: 初期状態を確認
        val validationError = viewModel.validationError.value
        
        // Then: バリデーションエラーなし
        assertNull(validationError)
    }

    // ===== タグデータ更新テスト =====

    @Test
    fun サイズ更新_有効値で正常に更新される() {
        // Given: 有効なサイズ値
        val newSize = 120
        
        // When: サイズを更新
        viewModel.updateSize(newSize)
        
        // Then: サイズが更新される
        val tagData = viewModel.tagData.value
        assertNotNull(tagData)
        assertEquals(newSize, tagData!!.size)
    }

    @Test
    fun 色更新_有効値で正常に更新される() {
        // Given: 有効な色
        val newColor = "赤"
        
        // When: 色を更新
        viewModel.updateColor(newColor)
        
        // Then: 色が更新される
        val tagData = viewModel.tagData.value
        assertNotNull(tagData)
        assertEquals(newColor, tagData!!.color)
    }

    @Test
    fun カテゴリ更新_有効値で正常に更新される() {
        // Given: 有効なカテゴリ
        val newCategory = "トップス"
        
        // When: カテゴリを更新
        viewModel.updateCategory(newCategory)
        
        // Then: カテゴリが更新される
        val tagData = viewModel.tagData.value
        assertNotNull(tagData)
        assertEquals(newCategory, tagData!!.category)
    }

    // ===== バリデーションテスト =====

    @Test
    fun バリデーション_サイズが範囲外の場合エラー表示() {
        // Given: 範囲外のサイズ
        val invalidSize = 200
        
        // When: 無効なサイズを設定
        viewModel.updateSize(invalidSize)
        
        // Then: バリデーションエラーが表示される
        val validationError = viewModel.validationError.value
        assertNotNull(validationError)
        assertTrue(validationError!!.contains("サイズは60～160"))
    }

    @Test
    fun バリデーション_サイズが小さすぎる場合エラー表示() {
        // Given: 最小値以下のサイズ
        val invalidSize = 50
        
        // When: 無効なサイズを設定
        viewModel.updateSize(invalidSize)
        
        // Then: バリデーションエラーが表示される
        val validationError = viewModel.validationError.value
        assertNotNull(validationError)
        assertTrue(validationError!!.contains("サイズは60～160"))
    }

    /**
     * バリデーションテストのヘルパーメソッド
     * 
     * エラーが発生することを確認する共通処理
     */
    private fun assertValidationError(expectedErrorText: String, action: () -> Unit) {
        // When: バリデーション対象の操作を実行
        action()
        
        // Then: 期待されるエラーメッセージが表示される
        val validationError = viewModel.validationError.value
        assertNotNull(validationError)
        assertTrue(validationError!!.contains(expectedErrorText))
    }

    @Test
    fun バリデーション_色が空の場合エラー表示() {
        assertValidationError("色を入力してください") {
            viewModel.updateColor("")
        }
    }

    @Test
    fun バリデーション_カテゴリが空の場合エラー表示() {
        assertValidationError("カテゴリを選択してください") {
            viewModel.updateCategory("   ") // 空白のみ
        }
    }

    @Test
    fun バリデーション_すべて有効な場合エラーなし() {
        // Given: すべて有効な値
        val validSize = 120
        val validColor = "青"
        val validCategory = "ボトムス"
        
        // When: 有効な値を設定
        viewModel.updateSize(validSize)
        viewModel.updateColor(validColor)
        viewModel.updateCategory(validCategory)
        
        // Then: バリデーションエラーなし
        val validationError = viewModel.validationError.value
        assertNull(validationError)
    }

    // ===== 保存機能テスト =====

    @Test
    fun 保存_有効なデータで成功する() = runTest {
        // Given: 有効なタグデータと画像パス
        val validTagData = TagData(120, "赤", "トップス")
        val imagePath = "/path/to/image.jpg"
        viewModel.updateSize(validTagData.size)
        viewModel.updateColor(validTagData.color)
        viewModel.updateCategory(validTagData.category)
        
        // リポジトリが成功を返すようにモック
        `when`(clothRepository.insertItem(any(ClothItem::class.java))).thenReturn(1L)
        
        // When: 保存を実行
        viewModel.saveTaggedItem(imagePath)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then: 保存が成功する
        verify(clothRepository).insertItem(any(ClothItem::class.java))
        val saveResult = viewModel.saveResult.value
        assertTrue(saveResult is TaggingViewModel.SaveResult.Success)
    }

    @Test
    fun 保存_無効なデータで失敗する() = runTest {
        // Given: 無効なタグデータ
        val invalidTagData = TagData(200, "", "")
        val imagePath = "/path/to/image.jpg"
        viewModel.updateSize(invalidTagData.size)
        viewModel.updateColor(invalidTagData.color)
        viewModel.updateCategory(invalidTagData.category)
        
        // When: 保存を実行
        viewModel.saveTaggedItem(imagePath)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then: バリデーションエラーで失敗する
        val saveResult = viewModel.saveResult.value
        assertTrue(saveResult is TaggingViewModel.SaveResult.Error)
    }

    @Test
    fun 保存中_ローディング状態が正しく管理される() = runTest {
        // Given: 有効なデータ
        val validTagData = TagData(120, "赤", "トップス")
        val imagePath = "/path/to/image.jpg"
        viewModel.updateSize(validTagData.size)
        viewModel.updateColor(validTagData.color)
        viewModel.updateCategory(validTagData.category)
        
        `when`(clothRepository.insertItem(any(ClothItem::class.java))).thenReturn(1L)
        
        // When: 保存を開始
        viewModel.saveTaggedItem(imagePath)
        
        // Then: ローディング状態が正しく変化する
        testDispatcher.scheduler.advanceUntilIdle()
        verify(isLoadingObserver).onChanged(true)  // 開始時
        verify(isLoadingObserver).onChanged(false) // 完了時
    }

    @Test
    fun 保存_リポジトリ例外で失敗する() = runTest {
        // Given: 有効なデータだがリポジトリで例外発生
        val validTagData = TagData(120, "赤", "トップス")
        val imagePath = "/path/to/image.jpg"
        viewModel.updateSize(validTagData.size)
        viewModel.updateColor(validTagData.color)
        viewModel.updateCategory(validTagData.category)
        
        `when`(clothRepository.insertItem(any(ClothItem::class.java))).thenThrow(RuntimeException("Database error"))
        
        // When: 保存を実行
        viewModel.saveTaggedItem(imagePath)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then: エラー結果が返される
        val saveResult = viewModel.saveResult.value
        assertTrue(saveResult is TaggingViewModel.SaveResult.Error)
        assertTrue((saveResult as TaggingViewModel.SaveResult.Error).message.contains("Database error"))
    }

    // ===== 状態管理テスト =====

    @Test
    fun 状態管理_複数回の更新で最新状態が保持される() {
        // Given: 複数回の更新
        
        // When: 連続して更新
        viewModel.updateSize(100)
        viewModel.updateColor("緑")
        viewModel.updateSize(130)
        viewModel.updateCategory("アウター")
        
        // Then: 最新の状態が保持される
        val tagData = viewModel.tagData.value
        assertNotNull(tagData)
        assertEquals(130, tagData!!.size)
        assertEquals("緑", tagData.color)
        assertEquals("アウター", tagData.category)
    }

    @Test
    fun 状態管理_初期化後に元の状態に戻る() {
        // Given: 変更されたデータ
        viewModel.updateSize(150)
        viewModel.updateColor("黄色")
        viewModel.updateCategory("アクセサリー")
        
        // When: 初期化
        viewModel.resetToDefault()
        
        // Then: デフォルト状態に戻る
        val tagData = viewModel.tagData.value
        assertNotNull(tagData)
        assertEquals(TagData.DEFAULT_SIZE, tagData!!.size)
        assertEquals(TagData.DEFAULT_COLOR, tagData.color)
        assertEquals(TagData.DEFAULT_CATEGORY, tagData.category)
    }

    // ===== エラーハンドリングテスト =====

    @Test
    fun エラーハンドリング_null画像パスで保存失敗() = runTest {
        // Given: 有効なタグデータだがnull画像パス
        val validTagData = TagData(120, "赤", "トップス")
        viewModel.updateSize(validTagData.size)
        viewModel.updateColor(validTagData.color)
        viewModel.updateCategory(validTagData.category)
        
        // When: null画像パスで保存
        viewModel.saveTaggedItem(null)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then: エラー結果が返される
        val saveResult = viewModel.saveResult.value
        assertTrue(saveResult is TaggingViewModel.SaveResult.Error)
        assertTrue((saveResult as TaggingViewModel.SaveResult.Error).message.contains("画像パス"))
    }

    @Test
    fun エラーハンドリング_空画像パスで保存失敗() = runTest {
        // Given: 有効なタグデータだが空画像パス
        val validTagData = TagData(120, "赤", "トップス")
        viewModel.updateSize(validTagData.size)
        viewModel.updateColor(validTagData.color)
        viewModel.updateCategory(validTagData.category)
        
        // When: 空画像パスで保存
        viewModel.saveTaggedItem("")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then: エラー結果が返される
        val saveResult = viewModel.saveResult.value
        assertTrue(saveResult is TaggingViewModel.SaveResult.Error)
        assertTrue((saveResult as TaggingViewModel.SaveResult.Error).message.contains("画像パス"))
    }
}