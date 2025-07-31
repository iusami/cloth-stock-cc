package com.example.clothstock.ui.gallery

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clothstock.data.model.ClothItem
import com.example.clothstock.data.repository.ClothRepository
import com.example.clothstock.ui.common.LoadingStateManager
import com.example.clothstock.ui.common.RetryMechanism
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import android.util.Log

/**
 * ギャラリー画面のViewModel
 * 
 * TDD Greenフェーズ実装
 * 衣服アイテムの表示、フィルタリング、ソート、削除機能を提供
 */
class GalleryViewModel(
    private val clothRepository: ClothRepository
) : ViewModel() {

    companion object {
        private const val TAG = "GalleryViewModel"
    }

    // ===== LiveData定義 =====

    private val _clothItems = MutableLiveData<List<ClothItem>>()
    val clothItems: LiveData<List<ClothItem>> = _clothItems

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _isEmpty = MutableLiveData<Boolean>()
    val isEmpty: LiveData<Boolean> = _isEmpty

    private val _loadingState = MutableLiveData<LoadingStateManager.LoadingState>(LoadingStateManager.LoadingState.Idle)
    val loadingState: LiveData<LoadingStateManager.LoadingState> = _loadingState

    private val _lastOperation = MutableLiveData<String>()
    val lastOperation: LiveData<String> = _lastOperation

    // ===== 初期化 =====

    init {
        Log.d(TAG, "GalleryViewModel initialized")
        
        // デフォルト値で初期化
        _clothItems.value = emptyList()
        _isLoading.value = false
        _errorMessage.value = null
        _isEmpty.value = true // 初期状態は空

        // 初期データ読み込み
        Log.d(TAG, "Starting initial data load")
        loadClothItems()
    }

    // ===== パブリックメソッド =====

    /**
     * 衣服アイテムを読み込み
     */
    fun loadClothItems() {
        _lastOperation.value = "loadClothItems"
        Log.d(TAG, "Starting loadClothItems")
        
        viewModelScope.launch {
            Log.d(TAG, "Setting loading state to true")
            _isLoading.value = true
            _loadingState.value = LoadingStateManager.LoadingState.Loading("アイテムを読み込み中...")
            _errorMessage.value = null

            // リトライ機能付きでデータベースアクセス
            Log.d(TAG, "Executing database access with retry mechanism")
            val retryResult = RetryMechanism.executeForDatabase {
                loadClothItemsInternal()
            }

            Log.d(TAG, "RetryMechanism result type: ${retryResult.javaClass.simpleName}")
            when (retryResult) {
                is RetryMechanism.RetryResult.Success -> {
                    val items = retryResult.result
                    Log.d(TAG, "SUCCESS: Successfully loaded ${items.size} items")
                    logItemDetails(items)
                    
                    _clothItems.value = items
                    _isEmpty.value = items.isEmpty()
                    _loadingState.value = LoadingStateManager.LoadingState.Success
                    
                    Log.d(TAG, "isEmpty set to: ${items.isEmpty()}")
                }
                is RetryMechanism.RetryResult.Failure -> {
                    val errorMsg = retryResult.lastException.message ?: "アイテムの読み込みに失敗しました"
                    Log.e(TAG, "FAILURE: Failed to load items: $errorMsg", retryResult.lastException)
                    Log.e(TAG, "FAILURE: Retry attempts: ${retryResult.attemptCount}")
                    Log.e(TAG, "FAILURE: Last exception type: ${retryResult.lastException.javaClass.simpleName}")
                    
                    _errorMessage.value = errorMsg
                    _loadingState.value = LoadingStateManager.LoadingState.Error(
                        "アイテムの読み込みに失敗しました",
                        retryResult.lastException
                    )
                }
            }
            
            Log.d(TAG, "Setting loading state to false")
            _isLoading.value = false
        }
    }

    /**
     * アイテム読み込みの内部実装（リトライ対応）
     */
    private suspend fun loadClothItemsInternal(): List<ClothItem> {
        Log.d(TAG, "loadClothItemsInternal: Starting database query")
        
        // Flowの最初の値を取得（一度だけ）
        val items = clothRepository.getAllItems()
            .catch { exception ->
                Log.e(TAG, "loadClothItemsInternal: Database error", exception)
                throw exception
            }
            .first() // 最初の値のみを取得
        
        Log.d(TAG, "loadClothItemsInternal: Retrieved ${items.size} items from repository")
        Log.d(TAG, "loadClothItemsInternal: Returning ${items.size} items")
        return items
    }

    /**
     * データをリフレッシュ
     */
    fun refreshData() {
        clearErrorMessage()
        loadClothItems()
    }

    /**
     * エラーメッセージをクリア
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    /**
     * カテゴリでフィルタリング
     */
    fun filterByCategory(category: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                clothRepository.getItemsByCategory(category)
                    .catch { exception ->
                        throw exception
                    }
                    .collect { items ->
                        _clothItems.value = items
                        _isEmpty.value = items.isEmpty()
                        _isLoading.value = false
                    }

            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "フィルタリングエラーが発生しました"
                _isLoading.value = false
            }
        }
    }

    /**
     * 色でフィルタリング
     */
    fun filterByColor(color: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                clothRepository.getItemsByColor(color)
                    .catch { exception ->
                        throw exception
                    }
                    .collect { items ->
                        _clothItems.value = items
                        _isEmpty.value = items.isEmpty()
                        _isLoading.value = false
                    }

            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "フィルタリングエラーが発生しました"
                _isLoading.value = false
            }
        }
    }

    /**
     * フィルタをクリア（全アイテム表示）
     */
    fun clearFilters() {
        loadClothItems()
    }

    /**
     * 日付の降順でソート（最新順）
     */
    fun sortByDateDescending() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                // 最新のアイテムを無制限で取得（制限なしを表現するため大きな数値を使用）
                clothRepository.getRecentItems(Int.MAX_VALUE)
                    .catch { exception ->
                        throw exception
                    }
                    .collect { items ->
                        _clothItems.value = items
                        _isEmpty.value = items.isEmpty()
                        _isLoading.value = false
                    }

            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "ソートエラーが発生しました"
                _isLoading.value = false
            }
        }
    }

    /**
     * アイテムを削除
     * 
     * 注意: 削除操作はリトライ対象外のため、lastOperationは設定しない
     */
    fun deleteItem(itemId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _loadingState.value = LoadingStateManager.LoadingState.Loading("アイテムを削除中...")
            _errorMessage.value = null

            // リトライ機能付きで削除実行
            val retryResult = RetryMechanism.executeForDatabase {
                val isDeleted = clothRepository.deleteItemById(itemId)
                if (!isDeleted) {
                    throw Exception("アイテムの削除に失敗しました")
                }
                isDeleted
            }

            when (retryResult) {
                is RetryMechanism.RetryResult.Success -> {
                    _loadingState.value = LoadingStateManager.LoadingState.Success
                    // 削除成功時はデータを再読み込み
                    loadClothItems()
                }
                is RetryMechanism.RetryResult.Failure -> {
                    _errorMessage.value = retryResult.lastException.message ?: "削除エラーが発生しました"
                    _loadingState.value = LoadingStateManager.LoadingState.Error(
                        "削除に失敗しました",
                        retryResult.lastException
                    )
                    _isLoading.value = false
                }
            }
        }
    }

    /**
     * エラー状態をクリア
     */
    fun clearError() {
        _errorMessage.value = null
        _loadingState.value = LoadingStateManager.LoadingState.Idle
    }

    /**
     * 最後の操作を再試行
     * 
     * 注意: 削除操作は安全性の観点からリトライ対象外
     * 削除に失敗した場合は手動で再度削除を実行する必要がある
     */
    fun retryLastOperation() {
        Log.d(TAG, "Retrying last operation: ${_lastOperation.value}")
        when (_lastOperation.value) {
            "loadClothItems" -> loadClothItems()
            // deleteItemは安全性のためリトライ対象外（ケースを削除）
            else -> loadClothItems() // デフォルトはデータ再読み込み
        }
    }
    
    /**
     * アイテム詳細のログ出力（詳細デバッグ版）
     */
    private fun logItemDetails(items: List<ClothItem>) {
        Log.d(TAG, "=== DATABASE ITEMS DEBUG ===")
        Log.d(TAG, "Total items count: ${items.size}")
        
        if (items.isEmpty()) {
            Log.d(TAG, "No items found in database")
            return
        }
        
        items.forEachIndexed { index, item ->
            Log.d(TAG, "--- Item $index ---")
            Log.d(TAG, "  ID: ${item.id}")
            Log.d(TAG, "  ImagePath: '${item.imagePath}'")
            Log.d(TAG, "  ImagePath length: ${item.imagePath?.length ?: 0}")
            Log.d(TAG, "  Path starts with content://: ${item.imagePath?.startsWith("content://") ?: false}")
            Log.d(TAG, "  Path starts with file://: ${item.imagePath?.startsWith("file://") ?: false}")
            Log.d(TAG, "  CreatedAt: ${item.createdAt}")
            Log.d(TAG, "  TagData: ${item.tagData}")
            
            if (item.imagePath.isNullOrBlank()) {
                Log.w(TAG, "  WARNING: Item ${item.id} has null/blank image path")
            } else if (!item.imagePath.startsWith("content://") && !item.imagePath.startsWith("file://")) {
                Log.w(TAG, "  WARNING: Item ${item.id} has suspicious image path format")
            }
        }
        Log.d(TAG, "=== END DATABASE ITEMS DEBUG ===")
    }
}