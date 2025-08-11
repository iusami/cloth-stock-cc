package com.example.clothstock.ui.common

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.example.clothstock.R

/**
 * スワイプハンドルビュー
 * 
 * TDD Refactor フェーズ - 完全実装
 * 品質向上とアクセシビリティ完全対応
 * 
 * 機能:
 * - Material Design準拠の視覚的表示
 * - タップによるパネル切り替え機能  
 * - アクセシビリティ完全対応
 * - ハイコントラストモード対応
 * - 視覚的フィードバック
 * 
 * Requirements: 3.1, 3.4, 3.5, 6.2, 6.4
 */
class SwipeHandleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val handleRect = RectF()
    
    private var handleColor: Int = ContextCompat.getColor(context, R.color.swipe_handle_color)
    private var isVisible: Boolean = true

    companion object {
        private const val HANDLE_WIDTH_RATIO = 0.3f
        private const val HANDLE_HEIGHT_RATIO = 0.4f
    }

    init {
        // アクセシビリティ設定（リソースファイル使用）
        contentDescription = context.getString(R.string.swipe_handle_description)
        isClickable = true
        isFocusable = true
        
        // ハイコントラストモード対応の色設定
        setupColors()
        
        // タップでパネル切り替え機能
        setOnClickListener {
            (parent as? SwipeableDetailPanel)?.togglePanelState()
        }
    }

    /**
     * ハイコントラストモードに応じた色設定
     */
    private fun setupColors() {
        handleColor = if (isHighContrastEnabled()) {
            ContextCompat.getColor(context, R.color.swipe_handle_color_high_contrast)
        } else {
            ContextCompat.getColor(context, R.color.swipe_handle_color)
        }
    }

    /**
     * ハイコントラストモードが有効かチェック
     * UiModeManagerを使用してナイトモードを検出
     */
    private fun isHighContrastEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as android.app.UiModeManager
            uiModeManager.nightMode == android.app.UiModeManager.MODE_NIGHT_YES
        } else {
            false
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isVisible) return
        
        // Material Design準拠のハンドル描画（改良版）
        val handleWidthRatio = HANDLE_WIDTH_RATIO
        val handleHeightRatio = HANDLE_HEIGHT_RATIO
        
        val handleWidth = width * handleWidthRatio
        val handleHeight = height * handleHeightRatio
        val centerX = width / 2f
        val centerY = height / 2f
        
        handleRect.set(
            centerX - handleWidth / 2,
            centerY - handleHeight / 2,
            centerX + handleWidth / 2,
            centerY + handleHeight / 2
        )
        
        // Paint設定の改善
        paint.apply {
            color = handleColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        val cornerRadius = handleHeight / 2
        canvas.drawRoundRect(handleRect, cornerRadius, cornerRadius, paint)
    }

    /**
     * ハンドルの表示・非表示を設定
     * 
     * @param visible true:表示、false:非表示
     */
    fun setHandleVisibility(visible: Boolean) {
        if (isVisible != visible) {
            isVisible = visible
            invalidate()
            
            // アクセシビリティ向けアナウンス
            announceVisibilityChange(visible)
        }
    }

    /**
     * ハンドル色の更新（動的変更対応）
     * 
     * @param color 新しいハンドル色
     */
    fun setHandleColor(color: Int) {
        if (handleColor != color) {
            handleColor = color
            invalidate()
        }
    }

    /**
     * 表示状態変更のアクセシビリティアナウンス
     */
    private fun announceVisibilityChange(visible: Boolean) {
        val message = if (visible) {
            context.getString(R.string.detail_panel_shown)
        } else {
            context.getString(R.string.detail_panel_hidden)
        }
        announceForAccessibility(message)
    }

    /**
     * アクセシビリティ情報の安全な実装
     * ViewCompatを使用してAPIレベル差異を吸収
     */
    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo?) {
        super.onInitializeAccessibilityNodeInfo(info)
        info?.apply {
            addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK)
            className = "android.widget.Button"
            
            // 詳細な状態情報
            isClickable = this@SwipeHandleView.isClickable
            isFocusable = this@SwipeHandleView.isFocusable
        }
        
        // ViewCompatを使用してroleDescriptionを安全に設定
        ViewCompat.setAccessibilityDelegate(this, object : androidx.core.view.AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfoCompat
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.roleDescription = context.getString(R.string.swipe_handle_role)
            }
        })
    }

    /**
     * 設定変更（ハイコントラストモード等）への対応
     */
    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        setupColors()
        invalidate()
    }
}
