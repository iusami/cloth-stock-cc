package com.example.clothstock.accessibility

import android.content.Context
import android.view.View
import android.view.accessibility.AccessibilityManager
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.example.clothstock.R

/**
 * Task 14: アクセシビリティヘルパークラス
 * 
 * TalkBack音声通知、フォーカス管理、動的contentDescription更新を提供
 */
@Suppress("TooManyFunctions")
object AccessibilityHelper {
    
    private const val FOCUS_SCALE_FACTOR = 1.05f
    private const val NORMAL_SCALE_FACTOR = 1.0f

    /**
     * TalkBackでメッセージを音声通知する
     * 
     * @param view 通知の起点となるView
     * @param message 読み上げるメッセージ
     */
    fun announceForAccessibility(view: View, message: String) {
        if (isAccessibilityEnabled(view.context)) {
            view.announceForAccessibility(message)
        }
    }

    /**
     * フィルター適用時の結果をTalkBackで通知
     */
    fun announceFilterApplied(view: View, resultCount: Int) {
        val context = view.context
        val message = context.getString(R.string.filter_applied_announcement, resultCount)
        announceForAccessibility(view, message)
    }

    /**
     * フィルタークリア時の通知
     */
    fun announceFilterCleared(view: View, totalCount: Int) {
        val context = view.context
        val message = context.getString(R.string.filter_cleared_announcement, totalCount)
        announceForAccessibility(view, message)
    }

    /**
     * 検索結果の通知
     */
    fun announceSearchResults(view: View, resultCount: Int) {
        val context = view.context
        val message = if (resultCount > 0) {
            context.getString(R.string.search_results_announcement, resultCount)
        } else {
            context.getString(R.string.search_no_results_announcement)
        }
        announceForAccessibility(view, message)
    }

    /**
     * Chipの動的contentDescription更新
     * 
     * @param chip 更新するChip
     * @param baseDescription ベースとなる説明文（例：「色: 赤。」）
     * @param isSelected 選択状態
     */
    fun updateChipContentDescription(chip: View, baseDescription: String, isSelected: Boolean) {
        val context = chip.context
        val selectionStatus = if (isSelected) {
            context.getString(R.string.chip_selected)
        } else {
            context.getString(R.string.chip_not_selected)
        }
        
        val fullDescription = String.format(baseDescription, selectionStatus)
        chip.contentDescription = fullDescription
    }

    /**
     * サイズChipの専用contentDescription更新
     */
    fun updateSizeChipContentDescription(chip: View, size: Int, isSelected: Boolean) {
        val context = chip.context
        val selectionStatus = if (isSelected) {
            context.getString(R.string.chip_selected)
        } else {
            context.getString(R.string.chip_not_selected)
        }
        
        val description = context.getString(R.string.chip_size_description, size, selectionStatus)
        chip.contentDescription = description
    }

    /**
     * フィルターボタンの動的contentDescription更新
     * 
     * @param button フィルターボタン
     * @param activeFiltersDescription 現在適用中のフィルター説明
     */
    fun updateFilterButtonDescription(button: View, activeFiltersDescription: String?) {
        val context = button.context
        val description = if (!activeFiltersDescription.isNullOrEmpty()) {
            context.getString(R.string.filter_button_description, activeFiltersDescription)
        } else {
            context.getString(R.string.filter_button_no_filters)
        }
        button.contentDescription = description
    }

    /**
     * 検索バーの動的contentDescription更新
     * 
     * @param searchView 検索バー
     * @param currentQuery 現在の検索キーワード
     */
    fun updateSearchViewDescription(searchView: View, currentQuery: String?) {
        val context = searchView.context
        val description = if (!currentQuery.isNullOrEmpty()) {
            context.getString(R.string.search_view_description, currentQuery)
        } else {
            context.getString(R.string.search_view_empty)
        }
        searchView.contentDescription = description
    }

    /**
     * AccessibilityManagerが有効かどうか確認
     */
    fun isAccessibilityEnabled(context: Context): Boolean {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        return accessibilityManager?.isEnabled == true && accessibilityManager.isTouchExplorationEnabled
    }

    /**
     * Viewにキーボードナビゲーション用のAccessibilityDelegateを設定
     * 
     * @param view 設定するView
     * @param instructions 操作方法の説明
     */
    fun setupKeyboardNavigationDelegate(view: View, instructions: String) {
        ViewCompat.setAccessibilityDelegate(view, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfoCompat
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                
                // キーボード操作の説明を追加
                info.addAction(
                    AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_CLICK,
                        instructions
                    )
                )
            }
        })
    }

    /**
     * Chipグループ全体にアクセシビリティ機能を設定
     */
    fun setupChipGroupAccessibility(chipGroup: View, groupDescription: String) {
        chipGroup.contentDescription = groupDescription
        
        // グループ全体の説明を設定
        ViewCompat.setAccessibilityDelegate(chipGroup, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfoCompat
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                
                val context = host.context
                val instructions = context.getString(R.string.chip_selection_hint)
                info.addAction(
                    AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_FOCUS,
                        instructions
                    )
                )
            }
        })
    }

    /**
     * LiveRegion設定（動的コンテンツ変更の通知用）
     */
    fun setLiveRegion(view: View, mode: Int = ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE) {
        ViewCompat.setAccessibilityLiveRegion(view, mode)
    }

    /**
     * 高コントラスト用のフォーカス表示改善
     */
    fun enhanceFocusForHighContrast(view: View) {
        // 高コントラストモードでのフォーカス表示を改善
        view.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                // フォーカス取得時の強調表示
                v.scaleX = FOCUS_SCALE_FACTOR
                v.scaleY = FOCUS_SCALE_FACTOR
            } else {
                // フォーカス失去時の元に戻す
                v.scaleX = NORMAL_SCALE_FACTOR
                v.scaleY = NORMAL_SCALE_FACTOR
            }
        }
    }
}

