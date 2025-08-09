package com.example.clothstock.ui.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.example.clothstock.R
import kotlin.math.min

/**
 * メモ有無を表示するカスタムインジケータービュー
 * 
 * Material Design 3準拠のスタイリングで、
 * ギャラリービューでメモが記録されたアイテムに表示され、
 * ユーザーがメモの存在を視覚的に認識できる機能を提供する
 * 
 * Requirements: 4.1, 4.2, 6.2
 */
class MemoIndicatorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val DEFAULT_SIZE_DP = 24
        private const val ELEVATION_DP = 2
        private const val CORNER_RADIUS_DP = 8
        private const val ALPHA_SELECTED = 0.9f
        private const val ALPHA_SHADOW = 0.1f
        private const val ALPHA_MULTIPLIER = 255
        private const val ICON_SIZE_RATIO = 0.6f
    }

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val iconDrawable: Drawable?
    private val backgroundRect = RectF()
    private val shadowRect = RectF()
    
    private val cornerRadius: Float
    private val shadowOffset: Float
    /**
     * メモの存在状態
     */
    var hasMemo: Boolean = false
        private set

    init {
        // アクセシビリティ設定
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        
        // メモアイコンを取得
        iconDrawable = ContextCompat.getDrawable(context, R.drawable.ic_memo_24)
        
        // Material Design 3準拠の寸法計算
        val displayMetrics = context.resources.displayMetrics
        cornerRadius = CORNER_RADIUS_DP * displayMetrics.density
        shadowOffset = ELEVATION_DP * displayMetrics.density
        
        // 色設定の初期化
        initializePaints()
        
        // 初期状態では非表示
        visibility = GONE
        updateContentDescription()
    }
    
    /**
     * Material Design 3準拠のペイント設定を初期化
     */
    private fun initializePaints() {
        val backgroundColor = ContextCompat.getColor(context, R.color.memo_indicator_background)
        val shadowColor = ColorUtils.setAlphaComponent(backgroundColor, (ALPHA_MULTIPLIER * ALPHA_SHADOW).toInt())
        
        backgroundPaint.color = ColorUtils.setAlphaComponent(
            backgroundColor, 
            (ALPHA_MULTIPLIER * ALPHA_SELECTED).toInt()
        )
        shadowPaint.color = shadowColor
        
        iconDrawable?.setTint(ContextCompat.getColor(context, R.color.memo_indicator_icon))
    }

    /**
     * メモの有無を設定し、表示状態を更新する
     * 
     * @param hasMemo メモが存在するかどうか
     */
    fun setHasMemo(hasMemo: Boolean) {
        if (this.hasMemo != hasMemo) {
            this.hasMemo = hasMemo
            visibility = if (hasMemo) VISIBLE else GONE
            updateContentDescription()
            invalidate() // 再描画を要求
        }
    }

    /**
     * アクセシビリティ用のcontentDescriptionを更新
     */
    private fun updateContentDescription() {
        contentDescription = if (hasMemo) {
            context.getString(R.string.memo_indicator_description)
        } else {
            context.getString(R.string.memo_indicator_none_description)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // 固定サイズ（24dp）を設定
        val size = (DEFAULT_SIZE_DP * context.resources.displayMetrics.density).toInt()
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (!hasMemo) {
            return
        }
        
        // Material Design 3準拠のシャドウ付き角丸描画
        updateDrawingRects()
        
        // シャドウを描画（Material Design 3の elevation効果）
        canvas.drawRoundRect(shadowRect, cornerRadius, cornerRadius, shadowPaint)
        
        // メイン背景を描画
        canvas.drawRoundRect(backgroundRect, cornerRadius, cornerRadius, backgroundPaint)
        
        // メモアイコンを描画
        drawIcon(canvas)
    }
    
    /**
     * 描画用のRectFを更新
     */
    private fun updateDrawingRects() {
        val margin = shadowOffset / 2
        
        // シャドウの位置（少し下にオフセット）
        shadowRect.set(
            margin + shadowOffset,
            margin + shadowOffset,
            width - margin + shadowOffset,
            height - margin + shadowOffset
        )
        
        // メイン背景の位置
        backgroundRect.set(
            margin,
            margin,
            width - margin,
            height - margin
        )
    }
    
    /**
     * アイコンを最適化された方法で描画
     */
    private fun drawIcon(canvas: Canvas) {
        iconDrawable?.let { drawable ->
            val availableSize = min(backgroundRect.width(), backgroundRect.height())
            val iconSize = (availableSize * ICON_SIZE_RATIO).toInt() // 背景の60%サイズ
            
            val centerX = backgroundRect.centerX().toInt()
            val centerY = backgroundRect.centerY().toInt()
            val halfIconSize = iconSize / 2
            
            drawable.setBounds(
                centerX - halfIconSize,
                centerY - halfIconSize,
                centerX + halfIconSize,
                centerY + halfIconSize
            )
            drawable.draw(canvas)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // リソースクリーンアップは自動的に処理される
    }
}
