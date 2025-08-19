package com.example.clothstock.ui.gallery

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clothstock.data.model.ClothItem
import com.example.clothstock.data.model.FilterOptions
import com.example.clothstock.data.model.PaginationSearchParameters
import com.example.clothstock.data.repository.ClothRepository
import com.example.clothstock.data.repository.FilterManager
import com.example.clothstock.data.model.FilterState
import com.example.clothstock.data.model.FilterType
import com.example.clothstock.data.preferences.FilterPreferencesManager
import com.example.clothstock.data.model.SelectionState
import com.example.clothstock.data.model.DeletionResult
import com.example.clothstock.data.model.DeletionFailure
import com.example.clothstock.ui.common.LoadingStateManager
import com.example.clothstock.ui.common.RetryMechanism
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import android.util.Log
import android.annotation.SuppressLint
// Task 12: プログレッシブローディング用import
import com.example.clothstock.data.repository.SearchCache

/**
 * ギャラリー画面のViewModel
 * 
 * TDD Greenフェーズ実装
 * 衣服アイテムの表示、フィルタリング、ソート、削除機能を提供
 */
class GalleryViewModel(
    private val clothRepository: ClothRepository,
    private val filterManager: FilterManager,
    private val savedStateHandle: SavedStateHandle? = null,
    private val filterPreferencesManager: FilterPreferencesManager? = null
) : ViewModel() {

    companion object {
        private const val TAG = "GalleryViewModel"
        private const val SEARCH_DEBOUNCE_DELAY_MS = 300L
        private const val PROGRESSIVE_BATCH_SIZE = 20
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

    // ===== Task5: フィルター・検索機能のLiveData =====

    private val _currentFilters = MutableLiveData<FilterState>()
    val currentFilters: LiveData<FilterState> = _currentFilters

    private val _availableFilterOptions = MutableLiveData<FilterOptions>()
    val availableFilterOptions: LiveData<FilterOptions> = _availableFilterOptions

    private val _isFiltersActive = MutableLiveData<Boolean>()
    val isFiltersActive: LiveData<Boolean> = _isFiltersActive

    private val _currentSearchText = MutableLiveData<String>()
    val currentSearchText: LiveData<String> = _currentSearchText

    // 検索デバウンシング用
    private var searchJob: Job? = null
    private val searchDelayMs = SEARCH_DEBOUNCE_DELAY_MS

    // ===== 選択状態管理のLiveData =====

    private val _selectionState = MutableLiveData<SelectionState>()
    val selectionState: LiveData<SelectionState> = _selectionState

    private val _isDeletionInProgress = MutableLiveData<Boolean>()
    val isDeletionInProgress: LiveData<Boolean> = _isDeletionInProgress

    private val _deletionResult = MutableLiveData<DeletionResult?>()
    val deletionResult: LiveData<DeletionResult?> = _deletionResult
    
    // ===== Task 12: プログレッシブローディング用フィールド =====
    
    // 検索結果キャッシュ
    private val searchCache = SearchCache(maxSize = 10)
    
    // プログレッシブローディング状態
    private val _progressiveLoadingState = 
        MutableLiveData<com.example.clothstock.ui.gallery.ProgressiveLoadingState>()
    val progressiveLoadingState: 
        LiveData<com.example.clothstock.ui.gallery.ProgressiveLoadingState> = _progressiveLoadingState
    
    // メモリプレッシャー状態
    private val _memoryPressureLevel = MutableLiveData<com.example.clothstock.ui.gallery.MemoryPressureLevel>()
    val memoryPressureLevel: LiveData<com.example.clothstock.ui.gallery.MemoryPressureLevel> = _memoryPressureLevel
    
    // 画像品質
    private val _currentImageQuality = MutableLiveData<com.example.clothstock.ui.gallery.ImageQuality>()
    val currentImageQuality: LiveData<com.example.clothstock.ui.gallery.ImageQuality> = _currentImageQuality
    
    // 追加データ有無
    private val _hasMoreData = MutableLiveData<Boolean>()
    val hasMoreData: LiveData<Boolean> = _hasMoreData
    
    // プログレッシブローディング一時停止状態
    private val _isProgressiveLoadingPaused = MutableLiveData<Boolean>()
    val isProgressiveLoadingPaused: LiveData<Boolean> = _isProgressiveLoadingPaused
    
    // プログレッシブローディング用ジョブ
    private var progressiveLoadingJob: Job? = null

    // ===== 初期化 =====

    init {
        Log.d(TAG, "GalleryViewModel initialized")
        
        // デフォルト値で初期化
        _clothItems.value = emptyList()
        _isLoading.value = false
        _errorMessage.value = null
        _isEmpty.value = true // 初期状態は空

        // Task 12: プログレッシブローディング関連の初期化
        _progressiveLoadingState.value = com.example.clothstock.ui.gallery.ProgressiveLoadingState()
        _memoryPressureLevel.value = com.example.clothstock.ui.gallery.MemoryPressureLevel.LOW
        _currentImageQuality.value = com.example.clothstock.ui.gallery.ImageQuality.HIGH
        _hasMoreData.value = true
        _isProgressiveLoadingPaused.value = false

        // Task 10: 保存された状態を復元
        restoreFilterStateFromSavedState()

        // フィルター・検索機能の初期化
        _currentFilters.value = filterManager.getCurrentState()
        _isFiltersActive.value = filterManager.getCurrentState().hasActiveFilters()
        _currentSearchText.value = filterManager.getCurrentState().searchText

        // 選択状態管理の初期化
        _selectionState.value = SelectionState()
        _isDeletionInProgress.value = false
        _deletionResult.value = null

        // 初期データ読み込み
        Log.d(TAG, "Starting initial data load")
        loadClothItems()
        loadAvailableFilterOptions()
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

    // ===== Task5: フィルター・検索機能の実装 =====

    /**
     * 利用可能なフィルターオプションを読み込み
     */
    private fun loadAvailableFilterOptions() {
        viewModelScope.launch {
            try {
                val options = clothRepository.getAvailableFilterOptions()
                _availableFilterOptions.value = options
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load filter options", e)
                // エラー時は空のFilterOptionsを設定
                _availableFilterOptions.value = FilterOptions(
                    availableSizes = emptyList(),
                    availableColors = emptyList(),
                    availableCategories = emptyList()
                )
            }
        }
    }

    /**
     * フィルターを適用
     */
    fun applyFilter(filterType: FilterType, value: String) {
        filterManager.updateFilter(filterType, setOf(value))
        _currentFilters.value = filterManager.getCurrentState()
        _isFiltersActive.value = filterManager.getCurrentState().hasActiveFilters()
        applyCurrentFiltersAndSearch()
    }

    /**
     * 指定フィルターを削除
     */
    fun removeFilter(filterType: FilterType, value: String) {
        filterManager.removeFilter(filterType, value)
        _currentFilters.value = filterManager.getCurrentState()
        _isFiltersActive.value = filterManager.getCurrentState().hasActiveFilters()
        applyCurrentFiltersAndSearch()
    }

    /**
     * 全フィルターをクリア
     */
    fun clearAllFilters() {
        filterManager.clearAllFilters()
        _currentFilters.value = filterManager.getCurrentState()
        _isFiltersActive.value = false
        applyCurrentFiltersAndSearch()
    }

    /**
     * 検索を実行（デバウンシング付き）
     */
    fun performSearch(searchText: String) {
        _currentSearchText.value = searchText
        
        // 既存の検索ジョブをキャンセル
        searchJob?.cancel()
        
        // 新しい検索ジョブを開始
        searchJob = viewModelScope.launch {
            delay(searchDelayMs) // デバウンシング
            filterManager.updateSearchText(searchText)
            applyCurrentFiltersAndSearch()
        }
    }

    /**
     * 検索をクリア
     */
    fun clearSearch() {
        searchJob?.cancel()
        _currentSearchText.value = ""
        filterManager.updateSearchText("")
        applyCurrentFiltersAndSearch()
    }

    /**
     * 現在のフィルターと検索条件を適用
     */
    private fun applyCurrentFiltersAndSearch() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                val currentState = filterManager.getCurrentState()
                val searchText = if (currentState.searchText.isNotBlank()) currentState.searchText else null

                val items = if (currentState.hasActiveFilters() || searchText != null) {
                    // フィルターまたは検索条件がある場合
                    clothRepository.searchItemsWithFilters(
                        sizeFilters = if (currentState.sizeFilters.isNotEmpty()) {
                            currentState.sizeFilters.toList()
                        } else null,
                        colorFilters = if (currentState.colorFilters.isNotEmpty()) {
                            currentState.colorFilters.toList()
                        } else null,
                        categoryFilters = if (currentState.categoryFilters.isNotEmpty()) {
                            currentState.categoryFilters.toList()
                        } else null,
                        searchText = searchText
                    ).first()
                } else {
                    // フィルター・検索条件がない場合は全件取得
                    clothRepository.getAllItems().first()
                }

                _clothItems.value = items
                _isEmpty.value = items.isEmpty()
                
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "フィルタリング・検索エラーが発生しました"
                Log.e(TAG, "Filter/search error", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // ===== Task 10: 状態保存・復元機能 =====
    
    /**
     * ViewModelがクリアされる際に状態を保存
     */
    override fun onCleared() {
        super.onCleared()
        saveStateToSavedStateHandle()
    }
    
    /**
     * テスト用：状態をSavedStateHandleに保存
     */
    fun saveStateToSavedStateHandle() {
        savedStateHandle?.let { handle ->
            val currentState = filterManager.getCurrentState()
            handle.set("filter_state", currentState)
            handle.set("search_text", currentState.searchText)
        }
    }
    
    /**
     * SavedStateHandleから状態を復元
     */
    private fun restoreFilterStateFromSavedState() {
        savedStateHandle?.let { handle ->
            try {
                val savedFilterState = handle.get<FilterState>("filter_state")
                val savedSearchText = handle.get<String>("search_text")
                
                if (savedFilterState != null) {
                    filterManager.restoreState(savedFilterState)
                } else if (savedSearchText != null) {
                    filterManager.updateSearchText(savedSearchText)
                }
            } catch (e: ClassCastException) {
                // 型キャストエラーの場合は無視してデフォルト状態を使用
                Log.w(TAG, "Failed to restore filter state from SavedStateHandle", e)
            }
        }
    }
    
    /**
     * フィルター設定をSharedPreferencesに保存
     */
    fun saveFilterPreferences() {
        filterPreferencesManager?.let { manager ->
            val currentState = filterManager.getCurrentState()
            manager.saveFilterState(currentState)
        }
    }
    
    /**
     * SharedPreferencesからフィルター設定を読み込み
     */
    fun loadFilterPreferences() {
        filterPreferencesManager?.let { manager ->
            if (manager.hasFilterPreferences()) {
                val savedState = manager.loadFilterState()
                filterManager.restoreState(savedState)
                _currentFilters.value = savedState
                _isFiltersActive.value = savedState.hasActiveFilters()
                _currentSearchText.value = savedState.searchText
                applyCurrentFiltersAndSearch()
            }
        }
    }
    
    /**
     * SharedPreferencesからフィルター設定をクリア
     */
    fun clearFilterPreferences() {
        filterPreferencesManager?.let { manager ->
            manager.clearFilterState()
        }
    }
    
    // ===== Task 12: プログレッシブローディング機能 =====
    
    /**
     * プログレッシブローディングを開始
     * 
     * @param searchQuery 検索クエリ（省略時は現在のフィルター状態を使用）
     */
    @SuppressLint("NullSafeMutableLiveData")
    fun startProgressiveLoading(searchQuery: String? = null) {
        progressiveLoadingJob?.cancel()
        
        progressiveLoadingJob = viewModelScope.launch {
            try {
                val currentState = if (searchQuery != null) {
                    filterManager.updateSearchText(searchQuery)
                    filterManager.getCurrentState()
                } else {
                    filterManager.getCurrentState()
                }
                
                // キャッシュから結果を取得試行
                val cachedItems = searchCache.get(currentState)
                if (cachedItems != null) {
                    Log.d(TAG, "プログレッシブローディング: キャッシュヒット（${cachedItems.size}件）")
                    _clothItems.value = cachedItems // Non-null確認済みのためlint警告は無視可能
                    _isEmpty.value = cachedItems.isEmpty()
                    _hasMoreData.value = false // キャッシュからの場合は全件取得済み
                    return@launch
                }
                
                // 初期状態設定
                _progressiveLoadingState.value = com.example.clothstock.ui.gallery.ProgressiveLoadingState(
                    isLoading = true,
                    currentOffset = 0,
                    batchSize = PROGRESSIVE_BATCH_SIZE,
                    hasMoreData = true,
                    isPaused = false
                )
                _clothItems.value = emptyList()
                _hasMoreData.value = true
                _isLoading.value = true
                
                // 最初のバッチを読み込み
                loadNextBatch()
                
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "プログレッシブローディング開始エラー"
                Log.e(TAG, "プログレッシブローディング開始エラー", e)
            }
        }
    }
    
    /**
     * 次のバッチを読み込み
     */
    fun loadNextBatch() {
        if (!canLoadNextBatch()) return
        
        viewModelScope.launch {
            performBatchLoading()
        }
    }
    
    /**
     * バッチ読み込みの条件チェック
     */
    private fun canLoadNextBatch(): Boolean {
        val currentLoadingState = _progressiveLoadingState.value
        val isPaused = _isProgressiveLoadingPaused.value == true
        val canLoad = currentLoadingState != null && 
                     currentLoadingState.hasMoreData && 
                     !currentLoadingState.isLoading && 
                     !isPaused
                     
        if (!canLoad) {
            Log.d(TAG, "プログレッシブローディング条件未満足のため、バッチ読み込みをスキップ")
        }
        return canLoad
    }
    
    /**
     * バッチ読み込みの実行
     */
    private suspend fun performBatchLoading() {
        val currentLoadingState = _progressiveLoadingState.value ?: return
        
        try {
            _progressiveLoadingState.value = currentLoadingState.copy(isLoading = true)
            
            val parameters = createPaginationParameters(currentLoadingState)
            val items = clothRepository.searchItemsWithPagination(parameters).first()
            
            updateItemsAndState(items, currentLoadingState)
            
        } catch (e: Exception) {
            handleBatchLoadingError(e, currentLoadingState)
        } finally {
            finalizeLoadingState()
        }
    }
    
    /**
     * ページネーションパラメーターを作成
     */
    private fun createPaginationParameters(loadingState: ProgressiveLoadingState): PaginationSearchParameters {
        val currentState = filterManager.getCurrentState()
        return PaginationSearchParameters(
            sizeFilters = currentState.sizeFilters.takeIf { it.isNotEmpty() }?.toList(),
            colorFilters = currentState.colorFilters.takeIf { it.isNotEmpty() }?.toList(),
            categoryFilters = currentState.categoryFilters.takeIf { it.isNotEmpty() }?.toList(),
            searchText = currentState.searchText.takeIf { it.isNotBlank() },
            offset = loadingState.currentOffset,
            limit = loadingState.batchSize
        )
    }
    
    /**
     * アイテムと状態を更新
     */
    private fun updateItemsAndState(items: List<ClothItem>, currentLoadingState: ProgressiveLoadingState) {
        val currentItems = _clothItems.value ?: emptyList()
        val newItems = currentItems + items
        val hasMore = items.size == currentLoadingState.batchSize
        
        _clothItems.value = newItems
        _isEmpty.value = newItems.isEmpty()
        _hasMoreData.value = hasMore
        
        _progressiveLoadingState.value = currentLoadingState.copy(
            isLoading = false,
            currentOffset = currentLoadingState.currentOffset + items.size,
            hasMoreData = hasMore
        )
        
        // 全件取得完了時にキャッシュに保存
        if (!hasMore) {
            val currentState = filterManager.getCurrentState()
            searchCache.put(currentState, newItems)
            Log.d(TAG, "プログレッシブローディング完了：${newItems.size}件をキャッシュに保存")
        }
    }
    
    /**
     * バッチ読み込みエラーを処理
     */
    private fun handleBatchLoadingError(e: Exception, currentLoadingState: ProgressiveLoadingState) {
        _errorMessage.value = e.message ?: "バッチ読み込みエラー"
        _progressiveLoadingState.value = currentLoadingState.copy(isLoading = false)
        Log.e(TAG, "バッチ読み込みエラー", e)
    }
    
    /**
     * 読み込み状態の最終化
     */
    private fun finalizeLoadingState() {
        if (_progressiveLoadingState.value?.hasMoreData == false) {
            _isLoading.value = false
        }
    }
    
    /**
     * メモリプレッシャー発生時の処理
     * 
     * @param level メモリプレッシャーレベル（省略時はHIGH）
     */
    fun onMemoryPressure(
        level: com.example.clothstock.ui.gallery.MemoryPressureLevel = 
            com.example.clothstock.ui.gallery.MemoryPressureLevel.HIGH
    ) {
        _memoryPressureLevel.value = level
        
        // キャッシュサイズを削減
        searchCache.onMemoryPressure(level)
        
        // プログレッシブローディングを一時停止
        if (level == com.example.clothstock.ui.gallery.MemoryPressureLevel.HIGH) {
            _isProgressiveLoadingPaused.value = true
            // 画像品質を下げる
            _currentImageQuality.value = com.example.clothstock.ui.gallery.ImageQuality.LOW
        } else if (level == com.example.clothstock.ui.gallery.MemoryPressureLevel.MEDIUM) {
            _currentImageQuality.value = com.example.clothstock.ui.gallery.ImageQuality.MEDIUM
        }
        
        Log.d(TAG, "メモリプレッシャー処理: レベル=$level, キャッシュサイズ=${searchCache.size()}")
    }
    
    /**
     * メモリプレッシャー解除時の処理
     */
    fun onMemoryPressureRelieved() {
        _memoryPressureLevel.value = com.example.clothstock.ui.gallery.MemoryPressureLevel.LOW
        _currentImageQuality.value = com.example.clothstock.ui.gallery.ImageQuality.HIGH
        _isProgressiveLoadingPaused.value = false
        
        Log.d(TAG, "メモリプレッシャー解除")
    }
    
    /**
     * 検索結果をキャッシュに保存
     * テスト用メソッド
     */
    fun cacheSearchResults(filterState: FilterState, items: List<ClothItem>) {
        searchCache.put(filterState, items)
    }
    
    /**
     * 検索キャッシュサイズを取得
     * テスト用メソッド
     */
    fun getSearchCacheSize(): Int = searchCache.size()
    
    /**
     * メモリプレッシャーレベルを設定
     * テスト用メソッド
     */
    fun setMemoryPressureLevel(level: com.example.clothstock.ui.gallery.MemoryPressureLevel) {
        _memoryPressureLevel.value = level
    }
    
    /**
     * 現在の画像品質を取得
     * テスト用メソッド
     */
    fun getCurrentImageQuality(): com.example.clothstock.ui.gallery.ImageQuality = 
        _currentImageQuality.value ?: com.example.clothstock.ui.gallery.ImageQuality.HIGH

    // ===== 選択操作メソッド（Task3: TDD REFACTOR段階） =====

    /**
     * 選択モードに入り、指定されたアイテムを選択
     * 
     * @param itemId 初期選択するアイテムのID
     */
    fun enterSelectionMode(itemId: Long) {
        try {
            Log.d(TAG, "Entering selection mode with item ID: $itemId")
            
            val currentState = _selectionState.value ?: SelectionState()
            val newState = currentState.copy(
                isSelectionMode = true,
                selectedItemIds = setOf(itemId),
                totalSelectedCount = 1
            )
            _selectionState.value = newState
            
            Log.d(TAG, "Selection mode entered successfully. Selected items: ${newState.selectedItemIds}")
        } catch (e: Exception) {
            Log.e(TAG, "Error entering selection mode with item ID: $itemId", e)
            // エラー時でも選択モードには入る（最小限の機能を保証）
            _selectionState.value = SelectionState(isSelectionMode = true)
        }
    }

    /**
     * アイテムの選択状態をトグル
     * 
     * @param itemId 選択/非選択を切り替えるアイテムのID
     */
    fun toggleItemSelection(itemId: Long) {
        try {
            Log.d(TAG, "Toggling selection for item ID: $itemId")
            
            val currentState = _selectionState.value ?: SelectionState()
            val newState = if (currentState.selectedItemIds.contains(itemId)) {
                // 既に選択されている場合は削除
                val newSelectedIds = currentState.selectedItemIds - itemId
                currentState.copy(
                    selectedItemIds = newSelectedIds,
                    totalSelectedCount = newSelectedIds.size
                )
            } else {
                // 選択されていない場合は追加
                val newSelectedIds = currentState.selectedItemIds + itemId
                currentState.copy(
                    selectedItemIds = newSelectedIds,
                    totalSelectedCount = newSelectedIds.size
                )
            }
            _selectionState.value = newState
            
            Log.d(TAG, "Item selection toggled. Selected count: ${newState.totalSelectedCount}")
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling selection for item ID: $itemId", e)
            // エラー時は状態を変更しない
        }
    }

    /**
     * 選択状態をクリアし、選択モードを終了
     */
    fun clearSelection() {
        try {
            Log.d(TAG, "Clearing selection state")
            _selectionState.value = SelectionState()
            Log.d(TAG, "Selection state cleared successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing selection state", e)
            // エラー時でもクリアを強制実行
            _selectionState.value = SelectionState()
        }
    }

    /**
     * 現在選択されているアイテムのリストを取得
     * 
     * @return 選択されているClothItemのリスト。エラー時は空のリストを返す
     */
    fun getSelectedItems(): List<ClothItem> {
        return try {
            val currentSelection = _selectionState.value ?: SelectionState()
            val allItems = _clothItems.value ?: emptyList()
            
            val selectedItems = allItems.filter { item ->
                currentSelection.selectedItemIds.contains(item.id)
            }
            
            Log.d(TAG, "Retrieved ${selectedItems.size} selected items")
            selectedItems
        } catch (e: Exception) {
            Log.e(TAG, "Error getting selected items", e)
            emptyList()
        }
    }

    /**
     * 選択モードかどうかを確認
     * 
     * @return 選択モードの場合true、そうでなければfalse
     */
    fun isInSelectionMode(): Boolean {
        return try {
            _selectionState.value?.isSelectionMode ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking selection mode", e)
            false
        }
    }

    /**
     * 指定されたアイテムが選択されているかどうかを確認
     * 
     * @param itemId 確認するアイテムのID
     * @return 選択されている場合true、そうでなければfalse
     */
    fun isItemSelected(itemId: Long): Boolean {
        return try {
            val currentSelection = _selectionState.value ?: SelectionState()
            currentSelection.selectedItemIds.contains(itemId)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if item $itemId is selected", e)
            false
        }
    }

    /**
     * 現在の選択数を取得
     * 
     * @return 選択されているアイテムの数
     */
    fun getSelectedCount(): Int {
        return try {
            _selectionState.value?.totalSelectedCount ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "Error getting selected count", e)
            0
        }
    }

    // ===== 一括削除機能（Task4: TDD GREEN段階） =====

    /**
     * 選択されたアイテムを一括削除
     * 
     * Repository層と連携し、削除進捗をLiveDataで通知する
     * REFACTOR段階: エラーハンドリング、進捗報告、リトライ機能を強化
     */
    fun deleteSelectedItems() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting batch deletion of selected items")
                
                val currentSelection = _selectionState.value ?: SelectionState()
                if (!currentSelection.hasSelection()) {
                    handleEmptySelection()
                    return@launch
                }
                
                // 削除進行中の状態に設定
                _isDeletionInProgress.value = true
                _deletionResult.value = null
                
                val selectedItemIds = currentSelection.selectedItemIds.toList()
                Log.d(TAG, "Deleting ${selectedItemIds.size} items: $selectedItemIds")
                
                // 実際の削除処理を実行
                val deletionResult = executeBatchDeletion(selectedItemIds)
                _deletionResult.value = deletionResult
                
                // 削除後の処理
                handleDeletionCompletion(deletionResult)
                
            } catch (e: Exception) {
                handleDeletionError(e)
            } finally {
                _isDeletionInProgress.value = false
                Log.d(TAG, "Batch deletion process completed")
            }
        }
    }
    
    /**
     * 空の選択状態の処理
     */
    private fun handleEmptySelection() {
        Log.d(TAG, "No items selected for deletion")
        _deletionResult.value = DeletionResult(
            totalRequested = 0,
            successfulDeletions = 0,
            failedDeletions = 0,
            failedItems = emptyList()
        )
    }
    
    /**
     * 一括削除の実行処理
     */
    private suspend fun executeBatchDeletion(selectedItemIds: List<Long>): DeletionResult {
        var successCount = 0
        var failureCount = 0
        val failures = mutableListOf<DeletionFailure>()
        
        // 進捗報告機能: 各アイテムを順次削除
        for ((index, itemId) in selectedItemIds.withIndex()) {
            try {
                Log.d(TAG, "Processing deletion ${index + 1}/${selectedItemIds.size}: item $itemId")
                
                val success = deleteItemWithRetry(itemId)
                if (success) {
                    successCount++
                } else {
                    failureCount++
                    failures.add(DeletionFailure(itemId, "削除エラー（リトライ失敗）", null))
                }
                
            } catch (e: IllegalStateException) {
                failureCount++
                failures.add(DeletionFailure(itemId, e.message ?: "削除エラー", e))
                Log.e(TAG, "Unexpected error deleting item $itemId", e)
            }
            
            // 長時間処理の場合はキャンセル可能性をチェック
            coroutineContext.ensureActive()
        }
        
        return DeletionResult(
            totalRequested = selectedItemIds.size,
            successfulDeletions = successCount,
            failedDeletions = failureCount,
            failedItems = failures
        )
    }
    
    /**
     * リトライ機能付きで単一アイテムを削除
     */
    private suspend fun deleteItemWithRetry(itemId: Long): Boolean {
        val retryResult = RetryMechanism.executeForDatabase {
            val deleted = clothRepository.deleteItemById(itemId)
            if (!deleted) {
                error("アイテム $itemId の削除に失敗しました")
            }
            deleted
        }
        
        return when (retryResult) {
            is RetryMechanism.RetryResult.Success -> {
                Log.d(TAG, "Successfully deleted item $itemId with ${retryResult.attemptCount} attempts")
                true
            }
            is RetryMechanism.RetryResult.Failure -> {
                Log.e(TAG, "Failed to delete item $itemId after ${retryResult.attemptCount} attempts", 
                     retryResult.lastException)
                false
            }
        }
    }
    
    /**
     * 削除完了後の処理
     */
    private fun handleDeletionCompletion(deletionResult: DeletionResult) {
        if (deletionResult.successfulDeletions > 0) {
            clearSelection()
            loadClothItems() // 成功したアイテムを画面から削除
            
            val resultMessage = if (deletionResult.failedDeletions == 0) {
                "全 ${deletionResult.successfulDeletions} アイテムの削除が完了しました"
            } else {
                "${deletionResult.successfulDeletions} アイテムの削除が完了しました" +
                "（${deletionResult.failedDeletions} アイテムの削除に失敗）"
            }
            Log.i(TAG, "Batch deletion completed: $resultMessage")
        } else {
            Log.w(TAG, "No items were successfully deleted. Total failures: ${deletionResult.failedDeletions}")
        }
    }
    
    /**
     * 削除エラーハンドリング
     */
    private fun handleDeletionError(e: Exception) {
        Log.e(TAG, "Unexpected error during batch deletion", e)
        
        // 予期しないエラーの場合も適切な結果を設定
        val currentSelection = _selectionState.value ?: SelectionState()
        val requestedCount = currentSelection.selectedItemIds.size
        
        _deletionResult.value = DeletionResult(
            totalRequested = requestedCount,
            successfulDeletions = 0,
            failedDeletions = requestedCount,
            failedItems = listOf(DeletionFailure(
                itemId = 0, // 特定アイテムではなく全体のエラー
                reason = e.message ?: "予期しないエラーが発生しました",
                exception = e
            ))
        )
    }
}
