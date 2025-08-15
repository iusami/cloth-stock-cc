package com.example.clothstock.ui.common

import android.content.Context
import android.content.res.Resources
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.example.clothstock.BuildConfig
import com.example.clothstock.R
import com.example.clothstock.data.model.ClothItem
import com.google.android.material.color.MaterialColors
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * メモ入力用カスタムビュー
 * 
 * TDD Green フェーズ実装
 * 
 * 機能:
 * - リアルタイム文字数カウント表示
 * - 1000文字制限の文字数制限機能
 * - 制限に近づいた時の視覚的フィードバック（警告色表示）
 * - メモ変更時のコールバック機能
 * - アクセシビリティ対応（contentDescription設定）
 * 
 * 使用法:
 * ```
 * val memoInputView = MemoInputView(context)
 * memoInputView.setMemo("既存のメモ")
 * memoInputView.setOnMemoChangedListener { memo -> 
 *     // メモ変更処理
 * }
 * val currentMemo = memoInputView.getMemo()
 * ```
 */
@Suppress("TooManyFunctions") // カスタムビューのAPIは多数の関数が必要
class MemoInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    // UI要素への参照
    private val textInputLayout: TextInputLayout
    private val editTextMemo: TextInputEditText
    private val textCharacterCount: TextView
    private val iconWarning: ImageView

    // リスナー
    private var onMemoChangedListener: ((String) -> Unit)? = null

    // 現在の文字数とカウント
    private var currentCharacterCount: Int = 0
    
    // TextWatcher無限ループ防止フラグ
    private var isUpdatingText: Boolean = false
    
    // 警告表示の閾値（90%）
    private val warningThreshold = (ClothItem.MAX_MEMO_LENGTH * WARNING_THRESHOLD_RATIO).toInt()
    
    // TextWatcher
    private lateinit var textWatcher: TextWatcher
    
    // 背景色機能（Task 2 追加）
    private val backgroundDrawable: GradientDrawable
    private var memoBackgroundColor: Int = ContextCompat.getColor(context, R.color.memo_background)
    
    // 背景状態キャッシュ（点滅防止用）
    private var cachedBackgroundState: Boolean? = null
    
    // 部分invalidate用のRect（再利用してGC負荷軽減）
    private val invalidateRect = Rect()
    
    // 描画処理軽量化用キャッシュ
    private var cachedWarningColor: Int? = null
    private var cachedNormalColor: Int? = null
    private var cachedWarningState: Boolean? = null
    
    companion object {
        // 文字数警告の閾値比率（90%）
        private const val WARNING_THRESHOLD_RATIO = 0.9
        // WCAG 最小コントラスト比
        private const val MIN_CONTRAST_RATIO = 4.5
        private const val TAG = "MemoInputView"
    }

    init {
        // 背景描画可能オブジェクトの初期化（Task 2 追加）
        backgroundDrawable = GradientDrawable().apply {
            cornerRadius = resources.getDimension(R.dimen.memo_background_corner_radius)
            setColor(memoBackgroundColor)
        }
        
        // レイアウトをinflate
        LayoutInflater.from(context).inflate(R.layout.view_memo_input, this, true)
        
        // ビューの参照を取得
        textInputLayout = findViewById(R.id.textInputLayoutMemo)
        editTextMemo = findViewById(R.id.editTextMemo)
        textCharacterCount = findViewById(R.id.textCharacterCount)
        iconWarning = findViewById(R.id.iconWarning)
        
        // TextWatcherを初期化
        textWatcher = object : TextWatcher {
            @Suppress("EmptyFunctionBlock")
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // 無限ループ防止：プログラムによるテキスト更新中は処理をスキップ
                if (isUpdatingText) return
                
                val text = s?.toString() ?: ""
                handleTextChange(text)
            }
            @Suppress("EmptyFunctionBlock")
            override fun afterTextChanged(s: Editable?) {}
        }
        
        // 初期設定
        setupEditText()
        updateCharacterCount(0)
        setupAccessibility()
        
        // 背景初期設定（Task 2 追加）
        setupBackgroundPadding()
    }

    /**
     * EditTextの設定とTextWatcherの追加
     */
    private fun setupEditText() {
        editTextMemo.addTextChangedListener(textWatcher)
    }

    /**
     * テキスト変更時の処理（軽量化版）
     * 
     * @param text 現在のテキスト内容
     */
    private fun handleTextChange(text: String) {
        // 文字数チェック（文字列生成を避けるため、lengthを先にチェック）
        val textLength = text.length
        val needsTrimming = textLength > ClothItem.MAX_MEMO_LENGTH
        
        val finalText: String
        val finalLength: Int
        
        if (needsTrimming) {
            // 文字数制限でトリミング（substring使用でGC負荷軽減）
            finalText = text.substring(0, ClothItem.MAX_MEMO_LENGTH)
            finalLength = ClothItem.MAX_MEMO_LENGTH
            
            // トリミングされた場合はテキストを更新
            isUpdatingText = true // 無限ループ防止フラグを設定
            try {
                editTextMemo.setText(finalText)
                editTextMemo.setSelection(finalLength)
            } finally {
                isUpdatingText = false // フラグを必ずリセット
            }
            return // TextWatcherが再度呼ばれるため、ここで終了
        } else {
            finalText = text
            finalLength = textLength
        }
        
        // 文字数カウント更新（変更された場合のみ）
        if (currentCharacterCount != finalLength) {
            currentCharacterCount = finalLength
            updateCharacterCount(currentCharacterCount)
        }
        
        // リスナーを呼び出し
        onMemoChangedListener?.invoke(finalText)
    }

    /**
     * 文字数カウント表示を更新（部分invalidate版）
     * 
     * @param count 現在の文字数
     */
    private fun updateCharacterCount(count: Int) {
        // カウンター文字列を更新
        textCharacterCount.text = "$count/${ClothItem.MAX_MEMO_LENGTH}"
        
        // 警告状態の判定と表示更新
        val isWarning = count >= warningThreshold
        updateWarningState(isWarning)
        
        // アクセシビリティ用のcontentDescriptionを更新
        textCharacterCount.contentDescription = "${count}文字中、最大${ClothItem.MAX_MEMO_LENGTH}文字"
        
        // 文字数カウンター領域のみ部分invalidate
        invalidateCharacterCountArea()
    }

    /**
     * 警告状態の表示を更新（軽量化・キャッシュ版）
     * 
     * @param isWarning 警告状態かどうか
     */
    private fun updateWarningState(isWarning: Boolean) {
        // 状態が変更されていない場合は処理をスキップ（描画処理軽量化）
        if (cachedWarningState == isWarning) {
            return
        }
        
        // 状態キャッシュを更新
        cachedWarningState = isWarning
        
        if (isWarning) {
            // 警告状態: キャッシュされた警告色を使用、警告アイコン表示
            textCharacterCount.setTextColor(getWarningColor())
            iconWarning.visibility = View.VISIBLE
            
            // アクセシビリティ: 警告状態を音声読み上げに含める
            textCharacterCount.contentDescription = 
                "警告: ${currentCharacterCount}文字中、最大${ClothItem.MAX_MEMO_LENGTH}文字。文字数制限に近づいています。"
        } else {
            // 通常状態: キャッシュされた通常色を使用、警告アイコン非表示
            textCharacterCount.setTextColor(getNormalColor())
            iconWarning.visibility = View.GONE
            
            // アクセシビリティ: 通常状態の読み上げ
            textCharacterCount.contentDescription = 
                "${currentCharacterCount}文字中、最大${ClothItem.MAX_MEMO_LENGTH}文字"
        }
        
        // 部分invalidateで描画負荷を軽減
        invalidateCharacterCountArea()
        invalidateWarningIconArea()
    }

    /**
     * アクセシビリティの設定
     */
    private fun setupAccessibility() {
        // EditTextのアクセシビリティ
        editTextMemo.contentDescription = context.getString(R.string.memo_content_description)
        
        // 文字数カウンターのアクセシビリティ
        textCharacterCount.contentDescription = context.getString(R.string.character_count_description)
        
        // 警告アイコンのアクセシビリティ
        iconWarning.contentDescription = context.getString(R.string.warning_icon_description)
        
        // ビュー全体にフォーカス可能性を設定
        isFocusable = true
        isFocusableInTouchMode = true
    }

    // ===== 部分invalidate機能（点滅防止用） =====

    /**
     * 文字数カウンター領域のみを部分invalidate
     */
    private fun invalidateCharacterCountArea() {
        if (textCharacterCount.visibility == View.VISIBLE) {
            getViewBounds(textCharacterCount, invalidateRect)
            invalidate(invalidateRect)
        }
    }

    /**
     * 警告アイコン領域のみを部分invalidate
     */
    private fun invalidateWarningIconArea() {
        if (iconWarning.visibility == View.VISIBLE) {
            getViewBounds(iconWarning, invalidateRect)
            invalidate(invalidateRect)
        }
    }

    /**
     * 指定されたビューの領域を取得
     * 
     * @param view 領域を取得するビュー
     * @param outRect 結果を格納するRect
     */
    private fun getViewBounds(view: View, outRect: Rect) {
        outRect.set(view.left, view.top, view.right, view.bottom)
    }

    /**
     * 警告色をキャッシュ付きで取得（描画処理軽量化）
     */
    private fun getWarningColor(): Int {
        if (cachedWarningColor == null) {
            cachedWarningColor = MaterialColors.getColor(
                context, 
                com.google.android.material.R.attr.colorError, 
                "colorError"
            )
        }
        return cachedWarningColor!!
    }

    /**
     * 通常色をキャッシュ付きで取得（描画処理軽量化）
     */
    private fun getNormalColor(): Int {
        if (cachedNormalColor == null) {
            cachedNormalColor = MaterialColors.getColor(
                context, 
                com.google.android.material.R.attr.colorOnSurfaceVariant, 
                "colorOnSurfaceVariant"
            )
        }
        return cachedNormalColor!!
    }

    // ===== パブリックAPI =====

    /**
     * メモテキストを設定
     * 
     * @param memo 設定するメモテキスト（nullの場合は空文字列として処理）
     */
    fun setMemo(memo: String?) {
        val safeText = memo ?: ""
        val trimmedText = safeText.take(ClothItem.MAX_MEMO_LENGTH)
        
        // 無限ループ防止フラグを使用してテキストを設定
        isUpdatingText = true
        try {
            editTextMemo.setText(trimmedText)
            editTextMemo.setSelection(trimmedText.length)
        } finally {
            isUpdatingText = false
        }
        
        // 手動で文字数カウント更新
        currentCharacterCount = trimmedText.length
        updateCharacterCount(currentCharacterCount)
        
        // 背景表示を更新（Task 2 追加）
        updateBackgroundVisibility(trimmedText.isNotBlank())
    }

    /**
     * 現在のメモテキストを取得
     * 
     * @return 現在のメモテキスト（空文字列の場合もある）
     */
    fun getMemo(): String {
        return editTextMemo.text?.toString() ?: ""
    }

    /**
     * メモ変更リスナーを設定
     * 
     * @param listener メモが変更された時に呼ばれるリスナー
     */
    fun setOnMemoChangedListener(listener: ((String) -> Unit)?) {
        onMemoChangedListener = listener
    }

    /**
     * 現在の文字数カウントテキストを取得（テスト用）
     * 
     * @return "現在の文字数/最大文字数" 形式の文字列
     */
    fun getCharacterCountText(): String {
        return textCharacterCount.text.toString()
    }

    /**
     * 現在が警告状態かどうかを判定（テスト用）
     * 
     * @return 警告状態の場合true、通常状態の場合false
     */
    fun isCharacterCountInWarning(): Boolean {
        return currentCharacterCount >= warningThreshold
    }

    /**
     * 入力フィールドにフォーカスを設定
     */
    fun requestMemoFocus() {
        editTextMemo.requestFocus()
    }

    /**
     * 入力フィールドからフォーカスをクリア
     */
    fun clearMemoFocus() {
        editTextMemo.clearFocus()
    }

    /**
     * 入力可能状態を設定
     * 
     * @param enabled 入力可能な場合true、無効な場合false
     */
    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        textInputLayout.isEnabled = enabled
        editTextMemo.isEnabled = enabled
    }

    // ===== 背景色機能（Task 2 追加） =====

    /**
     * 背景のパディングを設定
     */
    private fun setupBackgroundPadding() {
        val padding = resources.getDimensionPixelSize(R.dimen.memo_background_padding)
        setPadding(padding, padding, padding, padding)
    }

    /**
     * メモ背景色を設定
     * 
     * @param color 設定する背景色
     */
    fun setMemoBackgroundColor(@ColorInt color: Int) {
        try {
            memoBackgroundColor = color
            backgroundDrawable.setColor(color)
            
            // コントラスト比を確認してアクセシビリティ警告
            validateContrastRatio(color)
        } catch (e: Resources.NotFoundException) {
            if (BuildConfig.DEBUG) {
                android.util.Log.e(TAG, "Error setting memo background color - resource not found", e)
            }
        } catch (e: IllegalArgumentException) {
            if (BuildConfig.DEBUG) {
                android.util.Log.e(TAG, "Error setting memo background color - invalid color", e)
            }
        }
    }
    
    /**
     * 背景色のコントラスト比を検証
     * 
     * @param backgroundColor 検証する背景色
     */
    private fun validateContrastRatio(@ColorInt backgroundColor: Int) {
        try {
            val textColor = editTextMemo.currentTextColor
            if (!hasMinimumContrast(backgroundColor, textColor)) {
                if (BuildConfig.DEBUG) {
                    val actualRatio = getContrastRatio(backgroundColor, textColor)
                    android.util.Log.w(
                        TAG, 
                        "Memo background color may not have sufficient contrast. " +
                        "Required: $MIN_CONTRAST_RATIO, Actual: $actualRatio"
                    )
                }
            }
        } catch (e: IllegalArgumentException) {
            if (BuildConfig.DEBUG) {
                android.util.Log.w(TAG, "Error validating contrast ratio", e)
            }
        }
    }

    /**
     * 背景の表示・非表示を更新（点滅防止キャッシュ機能付き）
     * 
     * @param hasMemo メモにコンテンツがある場合true
     */
    private fun updateBackgroundVisibility(hasMemo: Boolean) {
        // 背景状態が実際に変更された場合のみ更新（点滅防止）
        if (cachedBackgroundState == hasMemo) {
            return // 状態変更なし、冗長な更新を回避
        }
        
        // キャッシュ状態を更新
        cachedBackgroundState = hasMemo
        
        // 背景の実際の更新（状態変更時のみ）
        background = if (hasMemo) backgroundDrawable else null
        
        // アクセシビリティ用の状態更新
        contentDescription = if (hasMemo) {
            context.getString(R.string.memo_with_background_description)
        } else {
            context.getString(R.string.memo_input_description)
        }
    }

    /**
     * 最小コントラスト比をチェック
     * 
     * @param backgroundColor 背景色
     * @param textColor テキスト色
     * @return WCAG基準を満たす場合true
     */
    private fun hasMinimumContrast(@ColorInt backgroundColor: Int, @ColorInt textColor: Int): Boolean {
        return ColorUtils.calculateContrast(textColor, backgroundColor) >= MIN_CONTRAST_RATIO
    }

    /**
     * コントラスト比を計算
     * 
     * @param color1 色1
     * @param color2 色2
     * @return コントラスト比
     */
    fun getContrastRatio(@ColorInt color1: Int, @ColorInt color2: Int): Double {
        return ColorUtils.calculateContrast(color1, color2)
    }

    /**
     * ハイコントラストモード対応の色調整
     * 
     * @param enabled ハイコントラストモードが有効な場合true
     */
    fun adjustColorsForHighContrast(enabled: Boolean) {
        try {
            val targetColor = if (enabled) {
                // ハイコントラストモード用の色に調整
                ContextCompat.getColor(context, R.color.swipe_handle_color_high_contrast)
            } else {
                // 通常の色に戻す（デフォルトの背景色）
                ContextCompat.getColor(context, R.color.memo_background)
            }
            setMemoBackgroundColor(targetColor)
        } catch (e: Resources.NotFoundException) {
            if (BuildConfig.DEBUG) {
                android.util.Log.e(TAG, "Error adjusting colors - resource not found", e)
            }
        }
    }

    /**
     * ハイコントラストモードが有効かチェック
     * 
     * AccessibilityManagerを使用してハイコントラスト状態を判定
     * 利用可能なアクセシビリティ機能の組み合わせで総合判断
     * 
     * @return ハイコントラストモードが有効な場合true
     */
    fun isHighContrastModeEnabled(): Boolean {
        return try {
            val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) 
                as android.view.accessibility.AccessibilityManager? ?: return false
            
            // アクセシビリティ機能の組み合わせでハイコントラスト判定
            val hasSpokenFeedback = accessibilityManager.getEnabledAccessibilityServiceList(
                android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_SPOKEN
            ).isNotEmpty()
            
            accessibilityManager.isEnabled && (accessibilityManager.isTouchExplorationEnabled || hasSpokenFeedback)
        } catch (e: SecurityException) {
            if (BuildConfig.DEBUG) {
                android.util.Log.w(TAG, "Error detecting high contrast mode - security", e)
            }
            false
        } catch (e: IllegalStateException) {
            if (BuildConfig.DEBUG) {
                android.util.Log.w(TAG, "Error detecting high contrast mode - state", e)
            }
            false
        }
    }

    /**
     * 背景描画可能オブジェクトを取得（テスト用）
     * 
     * @return 背景描画可能オブジェクト
     */
    fun getMemoBackgroundDrawable(): GradientDrawable = backgroundDrawable
}
