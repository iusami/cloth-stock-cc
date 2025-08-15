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

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val buttonRect = RectF()
    
    private var buttonBackgroundColor: Int = ContextCompat.getColor(context, R.color.md_theme_light_surface)
    private var buttonStrokeColor: Int = ContextCompat.getColor(context, R.color.md_theme_light_outline)
    private var isVisible: Boolean = true
    
    // dp to px 変換用
    private val density = context.resources.displayMetrics.density

    companion object {
        private const val BUTTON_WIDTH_DP = 120f  // ボタン幅（dp）
        private const val BUTTON_HEIGHT_DP = 32f  // ボタン高さ（dp）
        private const val CORNER_RADIUS_DP = 16f  // 角丸半径（dp）
        private const val STROKE_WIDTH_DP = 1f    // ボーダー幅（dp）
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
        if (isHighContrastEnabled()) {
            buttonBackgroundColor = ContextCompat.getColor(context, R.color.md_theme_dark_surface)
            buttonStrokeColor = ContextCompat.getColor(context, R.color.md_theme_dark_outline)
        } else {
            buttonBackgroundColor = ContextCompat.getColor(context, R.color.md_theme_light_surface)
            buttonStrokeColor = ContextCompat.getColor(context, R.color.md_theme_light_outline)
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
        
        // ボタン風UIの描画
        val buttonWidth = BUTTON_WIDTH_DP * density
        val buttonHeight = BUTTON_HEIGHT_DP * density
        val cornerRadius = CORNER_RADIUS_DP * density
        val strokeWidth = STROKE_WIDTH_DP * density
        
        val centerX = width / 2f
        val centerY = height / 2f
        
        buttonRect.set(
            centerX - buttonWidth / 2,
            centerY - buttonHeight / 2,
            centerX + buttonWidth / 2,
            centerY + buttonHeight / 2
        )
        
        // 背景描画
        backgroundPaint.apply {
            color = buttonBackgroundColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(buttonRect, cornerRadius, cornerRadius, backgroundPaint)
        
        // ボーダー描画
        strokePaint.apply {
            color = buttonStrokeColor
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            isAntiAlias = true
        }
        canvas.drawRoundRect(buttonRect, cornerRadius, cornerRadius, strokePaint)
        
        // テキストは描画しない（ボタン外観のみ）
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
     * ボタン色の更新（動的変更対応）
     * 
     * @param backgroundColor 新しい背景色
     * @param strokeColor 新しいボーダー色
     */
    fun setButtonColors(backgroundColor: Int, strokeColor: Int) {
        var changed = false
        if (buttonBackgroundColor != backgroundColor) {
            buttonBackgroundColor = backgroundColor
            changed = true
        }
        if (buttonStrokeColor != strokeColor) {
            buttonStrokeColor = strokeColor
            changed = true
        }
        if (changed) {
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
