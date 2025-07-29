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
import kotlinx.coroutines.launch

/**
 * ギャラリー画面のViewModel
 * 
 * TDD Greenフェーズ実装
 * 衣服アイテムの表示、フィルタリング、ソート、削除機能を提供
 */
class GalleryViewModel(
    private val clothRepository: ClothRepository
) : ViewModel() {

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
        // デフォルト値で初期化
        _clothItems.value = emptyList()
        _isLoading.value = false
        _errorMessage.value = null
        _isEmpty.value = true // 初期状態は空

        // 初期データ読み込み
        loadClothItems()
    }

    // ===== パブリックメソッド =====

    /**
     * 衣服アイテムを読み込み
     */
    fun loadClothItems() {
        _lastOperation.value = "loadClothItems"
        
        viewModelScope.launch {
            _isLoading.value = true
            _loadingState.value = LoadingStateManager.LoadingState.Loading("アイテムを読み込み中...")
            _errorMessage.value = null

            // リトライ機能付きでデータベースアクセス
            val retryResult = RetryMechanism.executeForDatabase {
                loadClothItemsInternal()
            }

            when (retryResult) {
                is RetryMechanism.RetryResult.Success -> {
                    _clothItems.value = retryResult.result
                    _isEmpty.value = retryResult.result.isEmpty()
                    _loadingState.value = LoadingStateManager.LoadingState.Success
                }
                is RetryMechanism.RetryResult.Failure -> {
                    _errorMessage.value = retryResult.lastException.message ?: "アイテムの読み込みに失敗しました"
                    _loadingState.value = LoadingStateManager.LoadingState.Error(
                        "アイテムの読み込みに失敗しました",
                        retryResult.lastException
                    )
                }
            }
            
            _isLoading.value = false
        }
    }

    /**
     * アイテム読み込みの内部実装（リトライ対応）
     */
    private suspend fun loadClothItemsInternal(): List<ClothItem> {
        val items = mutableListOf<ClothItem>()
        clothRepository.getAllItems()
            .catch { exception ->
                throw exception
            }
            .collect { itemList ->
                items.clear()
                items.addAll(itemList)
            }
        return items.toList()
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
     */
    fun deleteItem(itemId: Long) {
        _lastOperation.value = "deleteItem"
        
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
     */
    fun retryLastOperation() {
        when (_lastOperation.value) {
            "loadClothItems" -> loadClothItems()
            "deleteItem" -> {
                // 削除の再試行は安全性のため実装しない
                _errorMessage.value = "削除操作の再試行はサポートされていません"
            }
            else -> loadClothItems()
        }
    }
}