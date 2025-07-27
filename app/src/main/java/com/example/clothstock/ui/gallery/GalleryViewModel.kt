package com.example.clothstock.ui.gallery

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clothstock.data.model.ClothItem
import com.example.clothstock.data.repository.ClothRepository
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
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                clothRepository.getAllItems()
                    .catch { exception ->
                        throw exception
                    }
                    .collect { items ->
                        _clothItems.value = items
                        _isEmpty.value = items.isEmpty()
                        _isLoading.value = false
                    }

            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "不明なエラーが発生しました"
                _isLoading.value = false
            }
        }
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
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                val isDeleted = clothRepository.deleteItemById(itemId)
                
                if (isDeleted) {
                    // 削除成功時はデータを再読み込み
                    loadClothItems()
                } else {
                    _errorMessage.value = "アイテムの削除に失敗しました"
                    _isLoading.value = false
                }

            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "削除エラーが発生しました"
                _isLoading.value = false
            }
        }
    }
}