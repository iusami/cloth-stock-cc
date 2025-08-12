package com.example.clothstock.data.preferences

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.clothstock.ui.common.SwipeableDetailPanel

/**
 * 詳細画面の設定管理クラス
 * 
 * TDD Refactor フェーズ - 完全実装
 * エラーハンドリング、不正値フォールバック、パフォーマンス最適化を追加
 * Requirements: 5.1, 5.2, 5.4, 5.5 に対応
 */
class DetailPreferencesManager(context: Context) {
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )
    
    // パフォーマンス最適化のためキャッシュ
    @Volatile
    private var cachedPanelState: SwipeableDetailPanel.PanelState? = null
    private var hasCacheBeenLoaded = false
    
    /**
     * パネル状態を保存する
     * Requirement 5.1: セッション中のパネル状態保持
     */
    fun saveLastPanelState(state: SwipeableDetailPanel.PanelState) {
        try {
            val success = sharedPreferences.edit()
                .putString(KEY_LAST_PANEL_STATE, state.name)
                .commit() // apply()よりもcommit()でエラーハンドリングを可能にする
            
            if (success) {
                // 保存成功時にキャッシュを更新
                cachedPanelState = state
                hasCacheBeenLoaded = true
                Log.d(TAG, "Panel state saved successfully: $state")
            } else {
                Log.e(TAG, "Failed to save panel state: $state")
            }
        } catch (e: java.lang.Exception) {
            when (e) {
                is SecurityException, is IllegalStateException -> {
                    Log.e(TAG, "${e.javaClass.simpleName} while saving panel state: $state", e)
                    // エラー時はキャッシュだけ更新（セッション中は維持される）
                    cachedPanelState = state
                }
                else -> throw e
            }
        }
    }
    
    /**
     * 最後のパネル状態を取得する  
     * Requirement 5.2: ナビゲーション時の状態適用
     * Requirement 5.4: アプリ再開時の状態復元
     * Requirement 5.5: 新規セッション時のデフォルト状態
     */
    fun getLastPanelState(): SwipeableDetailPanel.PanelState {
        // キャッシュからの取得でパフォーマンス最適化
        val cached = cachedPanelState
        if (hasCacheBeenLoaded && cached != null) {
            return cached
        }
        
        return loadPanelStateFromPreferences()
    }
    
    /**
     * SharedPreferences からパネル状態を読み込む
     */
    private fun loadPanelStateFromPreferences(): SwipeableDetailPanel.PanelState {
        try {
            val stateName = sharedPreferences.getString(KEY_LAST_PANEL_STATE, null)
            val resolvedState = resolveStateFromString(stateName)
            
            // キャッシュを更新
            cachedPanelState = resolvedState
            hasCacheBeenLoaded = true
            
            Log.d(TAG, "Panel state loaded: $resolvedState (from: $stateName)")
            return resolvedState
            
        } catch (e: java.lang.Exception) {
            when (e) {
                is SecurityException, is IllegalStateException -> {
                    Log.e(TAG, "${e.javaClass.simpleName} while loading panel state", e)
                    return getDefaultStateAndUpdateCache()
                }
                else -> throw e
            }
        }
    }
    
    /**
     * 文字列からパネル状態を解決する（不正値フォールバック処理）
     */
    private fun resolveStateFromString(stateName: String?): SwipeableDetailPanel.PanelState {
        return when {
            stateName.isNullOrBlank() -> {
                Log.d(TAG, "No saved panel state, using default SHOWN")
                SwipeableDetailPanel.PanelState.SHOWN
            }
            else -> try {
                SwipeableDetailPanel.PanelState.valueOf(stateName)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Invalid panel state value: '$stateName', using default SHOWN", e)
                SwipeableDetailPanel.PanelState.SHOWN
            }
        }
    }
    
    /**
     * デフォルト状態を取得してキャッシュを更新する（エラー時のフォールバック処理）
     */
    private fun getDefaultStateAndUpdateCache(): SwipeableDetailPanel.PanelState {
        val defaultState = SwipeableDetailPanel.PanelState.SHOWN
        cachedPanelState = defaultState
        hasCacheBeenLoaded = true
        return defaultState
    }

    /**
     * キャッシュをクリアする（テスト用、メモリ最適化用）
     */
    fun clearCache() {
        cachedPanelState = null
        hasCacheBeenLoaded = false
        Log.d(TAG, "Panel state cache cleared")
    }
    
    companion object {
        private const val PREFS_NAME = "detail_preferences"
        private const val KEY_LAST_PANEL_STATE = "last_panel_state"
        private const val TAG = "DetailPreferencesManager"
    }
}
