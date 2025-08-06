package com.example.clothstock.ui.gallery

import android.util.Log
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/**
 * 検索機能管理クラス
 * 
 * Strategy PatternとDelegation Patternを適用
 * 検索関連の責任を一元管理
 */
class GallerySearchManager(
    fragment: GalleryFragment,
    private val viewModel: GalleryViewModel
) {
    private val fragmentRef = WeakReference(fragment)
    private var searchJob: Job? = null
    
    companion object {
        private const val TAG = "GallerySearchManager"
        private const val SEARCH_DEBOUNCE_DELAY_MS = 300L
        private const val MIN_SEARCH_LENGTH = 2
        private const val MAX_SEARCH_LENGTH = 50
        private const val DISABLED_ALPHA = 0.5f
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
     * デバウンス付き検索実行
     */
    private fun performDebouncedSearch(searchText: String) {
        searchJob?.cancel()
        
        fragmentRef.get()?.let { fragment ->
            searchJob = fragment.viewLifecycleOwner.lifecycleScope.launch {
                try {
                    delay(SEARCH_DEBOUNCE_DELAY_MS)
                    
                    val validationResult = validateSearchText(searchText.trim())
                    
                    when (validationResult.status) {
                        SearchValidationStatus.VALID -> {
                            viewModel.performSearch(validationResult.processedText)
                        }
                        SearchValidationStatus.EMPTY -> {
                            viewModel.clearSearch()
                        }
                        SearchValidationStatus.TOO_SHORT -> {
                            // 短すぎる場合は何もしない
                        }
                        SearchValidationStatus.TOO_LONG -> {
                            viewModel.performSearch(validationResult.processedText)
                        }
                        SearchValidationStatus.INVALID -> {
                            Log.w(TAG, "Invalid search text")
                        }
                    }
                    
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e // CancellationExceptionは再スロー
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "IllegalStateException during debounced search", e)
                } catch (e: UninitializedPropertyAccessException) {
                    Log.e(TAG, "UninitializedPropertyAccessException during debounced search", e)
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
     * リソースクリーンアップ
     */
    fun cleanup() {
        searchJob?.cancel()
        searchJob = null
        fragmentRef.clear()
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
