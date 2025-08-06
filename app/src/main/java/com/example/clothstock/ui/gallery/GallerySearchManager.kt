package com.example.clothstock.ui.gallery

import android.util.Log
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import java.lang.ref.WeakReference

/**
 * 検索機能管理クラス
 * 
 * Strategy PatternとDelegation Patternを適用
 * 検索関連の責任を一元管理
 * パフォーマンス最適化と包括的ログ記録を実装
 */
class GallerySearchManager(
    fragment: GalleryFragment,
    private val viewModel: GalleryViewModel
) {
    private val fragmentRef = WeakReference(fragment)
    private var searchJob: Job? = null
    
    // パフォーマンス最適化
    private var lastSearchTime = 0L
    private var searchCount = 0
    private val searchMetrics = mutableMapOf<String, Long>()
    
    // グレースフルデグラデーション状態
    private var isGracefulDegradationEnabled = false
    
    companion object {
        private const val TAG = "GallerySearchManager"
        private const val SEARCH_DEBOUNCE_DELAY_MS = 300L
        private const val MIN_SEARCH_LENGTH = 2
        private const val MAX_SEARCH_LENGTH = 50
        private const val DISABLED_ALPHA = 0.5f
        private const val RETRY_DELAY_BASE_MS = 1000L
        private const val METRICS_LOG_INTERVAL = 10
    }
    
    /**
     * 検索バーの初期化
     */
    fun setupSearchBar(searchView: SearchView) {
        Log.d(TAG, "Setting up search bar")
        
        try {
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    Log.d(TAG, "Search submitted: $query")
                    searchView.clearFocus()
                    searchJob?.cancel()
                    performImmediateSearch(query ?: "")
                    return true
                }
                
                override fun onQueryTextChange(newText: String?): Boolean {
                    Log.d(TAG, "Search text changed: $newText")
                    performDebouncedSearch(newText ?: "")
                    return true
                }
            })
            
            searchView.setOnCloseListener {
                Log.d(TAG, "Search cleared")
                searchJob?.cancel()
                viewModel.clearSearch()
                false
            }
            
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException setting up search bar", e)
            disableSearchUI(searchView)
        } catch (e: UninitializedPropertyAccessException) {
            Log.e(TAG, "UninitializedPropertyAccessException setting up search bar", e)
            disableSearchUI(searchView)
        }
    }
    
    /**
     * 即座に検索実行
     */
    private fun performImmediateSearch(searchText: String) {
        val trimmedText = searchText.trim()
        
        when {
            trimmedText.isEmpty() -> viewModel.clearSearch()
            else -> viewModel.performSearch(trimmedText)
        }
    }
    
    /**
     * デバウンス付き検索実行（パフォーマンス最適化付き）
     */
    private fun performDebouncedSearch(searchText: String) {
        searchJob?.cancel()
        
        val startTime = System.currentTimeMillis()
        
        fragmentRef.get()?.let { fragment ->
            searchJob = fragment.viewLifecycleOwner.lifecycleScope.launch {
                try {
                    // グレースフルデグラデーション時は遅延を短縮
                    val debounceDelay = if (isGracefulDegradationEnabled) {
                        SEARCH_DEBOUNCE_DELAY_MS / 2
                    } else {
                        SEARCH_DEBOUNCE_DELAY_MS
                    }
                    
                    delay(debounceDelay)
                    
                    val validationResult = validateSearchText(searchText.trim())
                    
                    when (validationResult.status) {
                        SearchValidationStatus.VALID -> {
                            recordSearchMetric("VALID_SEARCH", startTime)
                            viewModel.performSearch(validationResult.processedText)
                        }
                        SearchValidationStatus.EMPTY -> {
                            recordSearchMetric("EMPTY_SEARCH", startTime)
                            viewModel.clearSearch()
                        }
                        SearchValidationStatus.TOO_SHORT -> {
                            recordSearchMetric("TOO_SHORT_SEARCH", startTime)
                            // 短すぎる場合は何もしない
                        }
                        SearchValidationStatus.TOO_LONG -> {
                            recordSearchMetric("TOO_LONG_SEARCH", startTime)
                            viewModel.performSearch(validationResult.processedText)
                        }
                        SearchValidationStatus.INVALID -> {
                            recordSearchMetric("INVALID_SEARCH", startTime)
                            Log.w(TAG, "Invalid search text")
                        }
                    }
                    
                } catch (e: kotlinx.coroutines.CancellationException) {
                    recordSearchMetric("CANCELLED_SEARCH", startTime)
                    throw e // CancellationExceptionは再スロー
                } catch (e: IllegalStateException) {
                    recordSearchMetric("ERROR_SEARCH", startTime)
                    Log.e(TAG, "IllegalStateException during debounced search", e)
                    handleSearchError(searchText, e)
                } catch (e: UninitializedPropertyAccessException) {
                    recordSearchMetric("ERROR_SEARCH", startTime)
                    Log.e(TAG, "UninitializedPropertyAccessException during debounced search", e)
                    handleSearchError(searchText, e)
                }
            }
        }
    }
    
    /**
     * 検索テキストバリデーション
     */
    private fun validateSearchText(text: String): SearchValidationResult {
        return when {
            text.isEmpty() -> SearchValidationResult(SearchValidationStatus.EMPTY, text)
            text.length < MIN_SEARCH_LENGTH -> SearchValidationResult(SearchValidationStatus.TOO_SHORT, text)
            text.length > MAX_SEARCH_LENGTH -> SearchValidationResult(
                SearchValidationStatus.TOO_LONG,
                text.substring(0, MAX_SEARCH_LENGTH)
            )
            text.contains(Regex("[<>&\"']")) -> SearchValidationResult(SearchValidationStatus.INVALID, text)
            else -> SearchValidationResult(SearchValidationStatus.VALID, text)
        }
    }
    
    /**
     * 検索UI無効化
     */
    private fun disableSearchUI(searchView: SearchView) {
        searchView.isEnabled = false
        searchView.alpha = DISABLED_ALPHA
    }
    
    /**
     * タイムアウト付き検索実行
     */
    fun performSearchWithTimeout(searchText: String, timeoutMs: Long) {
        searchJob?.cancel()
        
        fragmentRef.get()?.let { fragment ->
            searchJob = fragment.viewLifecycleOwner.lifecycleScope.launch {
                try {
                    kotlinx.coroutines.withTimeout(timeoutMs) {
                        val validationResult = validateSearchText(searchText.trim())
                        
                        when (validationResult.status) {
                            SearchValidationStatus.VALID -> {
                                viewModel.performSearch(validationResult.processedText)
                            }
                            SearchValidationStatus.EMPTY -> {
                                viewModel.clearSearch()
                            }
                            else -> {
                                Log.w(TAG, "Invalid search text for timeout search")
                            }
                        }
                    }
                } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                    Log.e(TAG, "Search timeout after ${timeoutMs}ms", e)
                    handleSearchError(searchText, RuntimeException("Search timeout", e))
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "IllegalState during timeout search", e)
                    handleSearchError(searchText, e)
                } catch (e: CancellationException) {
                    Log.e(TAG, "CancellationException during timeout search", e)
                    handleSearchError(searchText, RuntimeException("Search cancelled", e))
                } catch (e: SecurityException) {
                    Log.e(TAG, "SecurityException during timeout search", e) 
                    handleSearchError(searchText, e)
                }
            }
        }
    }
    
    /**
     * 検索キャンセル
     */
    fun cancelSearch() {
        Log.d(TAG, "Cancelling search")
        searchJob?.cancel()
        searchJob = null
        viewModel.clearSearch()
    }
    
    /**
     * 失敗した検索のリトライ
     */
    fun retryFailedSearch(searchText: String, retryCount: Int) {
        Log.d(TAG, "Retrying failed search (attempt $retryCount): $searchText")
        
        fragmentRef.get()?.let { fragment ->
            searchJob = fragment.viewLifecycleOwner.lifecycleScope.launch {
                try {
                    delay(retryCount * RETRY_DELAY_BASE_MS) // 指数バックオフ
                    
                    val validationResult = validateSearchText(searchText.trim())
                    
                    when (validationResult.status) {
                        SearchValidationStatus.VALID -> {
                            viewModel.performSearch(validationResult.processedText)
                        }
                        SearchValidationStatus.EMPTY -> {
                            viewModel.clearSearch()
                        }
                        else -> {
                            Log.w(TAG, "Invalid search text for retry")
                        }
                    }
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "IllegalState during search retry", e)
                    handleSearchError(searchText, e)
                } catch (e: CancellationException) {
                    Log.e(TAG, "CancellationException during search retry", e)
                    handleSearchError(searchText, RuntimeException("Retry cancelled", e))
                } catch (e: SecurityException) {
                    Log.e(TAG, "SecurityException during search retry", e)
                    handleSearchError(searchText, e)
                }
            }
        }
    }
    
    /**
     * 検索エラー処理
     */
    fun handleSearchError(searchText: String, error: Exception) {
        Log.e(TAG, "Handling search error for: $searchText", error)
        
        // エラーハンドラーがあれば使用、なければログのみ
        try {
            // 基本的なエラーハンドリング
            viewModel.clearSearch()
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalState during search error handling", e)
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException during search error handling", e)
        }
    }
    
    /**
     * グレースフルデグラデーション有効化
     */
    fun enableGracefulDegradation(degradationReason: String) {
        Log.w(TAG, "Enabling graceful degradation: $degradationReason")
        
        isGracefulDegradationEnabled = true
        
        // 基本的な検索機能のみ有効化
        // 高度な機能（デバウンス、バリデーション等）を簡素化
        recordSearchMetric("GRACEFUL_DEGRADATION_ENABLED", System.currentTimeMillis())
    }
    
    /**
     * 検索メトリクス記録
     */
    private fun recordSearchMetric(metricType: String, startTime: Long) {
        val duration = System.currentTimeMillis() - startTime
        searchMetrics[metricType] = searchMetrics.getOrDefault(metricType, 0L) + duration
        searchCount++
        
        // 定期的にメトリクスをログ出力
        if (searchCount % METRICS_LOG_INTERVAL == 0) {
            Log.i(TAG, "Search metrics - Total searches: $searchCount")
            searchMetrics.forEach { (type, totalDuration) ->
                Log.i(TAG, "  $type: ${totalDuration}ms total")
            }
        }
    }
    
    /**
     * 検索メトリクス取得（テスト用）
     */
    fun getSearchMetrics(): Map<String, Long> {
        return searchMetrics.toMap()
    }
    
    /**
     * リソースクリーンアップ
     */
    fun cleanup() {
        searchJob?.cancel()
        searchJob = null
        fragmentRef.clear()
        searchMetrics.clear()
        searchCount = 0
        lastSearchTime = 0L
        isGracefulDegradationEnabled = false
    }
}

/**
 * 検索バリデーション状態
 */
enum class SearchValidationStatus {
    VALID, EMPTY, TOO_SHORT, TOO_LONG, INVALID
}

/**
 * 検索バリデーション結果
 */
data class SearchValidationResult(
    val status: SearchValidationStatus,
    val processedText: String
)
