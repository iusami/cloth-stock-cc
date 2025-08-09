package com.example.clothstock.ui.common

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.clothstock.R
import com.example.clothstock.data.model.ClothItem
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
    
    // 警告表示の閾値（90%）
    private val warningThreshold = (ClothItem.MAX_MEMO_LENGTH * WARNING_THRESHOLD_RATIO).toInt()
    
    companion object {
        // 文字数警告の閾値比率（90%）
        private const val WARNING_THRESHOLD_RATIO = 0.9
    }

    init {
        // レイアウトをinflate
        LayoutInflater.from(context).inflate(R.layout.view_memo_input, this, true)
        
        // ビューの参照を取得
        textInputLayout = findViewById(R.id.textInputLayoutMemo)
        editTextMemo = findViewById(R.id.editTextMemo)
        textCharacterCount = findViewById(R.id.textCharacterCount)
        iconWarning = findViewById(R.id.iconWarning)
        
        // 初期設定
        setupEditText()
        updateCharacterCount(0)
        setupAccessibility()
    }

    /**
     * EditTextの設定とTextWatcherの追加
     */
    private fun setupEditText() {
        editTextMemo.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // 不要
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val text = s?.toString() ?: ""
                handleTextChange(text)
            }

            override fun afterTextChanged(s: Editable?) {
                // 不要
            }
        })
    }

    /**
     * テキスト変更時の処理
     * 
     * @param text 現在のテキスト内容
     */
    private fun handleTextChange(text: String) {
        // 文字数制限の適用
        val trimmedText = if (text.length > ClothItem.MAX_MEMO_LENGTH) {
            text.take(ClothItem.MAX_MEMO_LENGTH)
        } else {
            text
        }
        
        // 文字数制限でトリミングされた場合はテキストを更新
        if (trimmedText != text) {
            editTextMemo.setText(trimmedText)
            editTextMemo.setSelection(trimmedText.length)
            return // TextWatcherが再度呼ばれるため、ここで終了
        }
        
        // 文字数カウント更新
        currentCharacterCount = trimmedText.length
        updateCharacterCount(currentCharacterCount)
        
        // リスナーを呼び出し
        onMemoChangedListener?.invoke(trimmedText)
    }

    /**
     * 文字数カウント表示を更新
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
    }

    /**
     * 警告状態の表示を更新
     * 
     * @param isWarning 警告状態かどうか
     */
    private fun updateWarningState(isWarning: Boolean) {
        if (isWarning) {
            // 警告状態: テキスト色を警告色に変更、警告アイコン表示
            val warningColor = ContextCompat.getColor(context, android.R.color.holo_red_light)
            textCharacterCount.setTextColor(warningColor)
            iconWarning.visibility = View.VISIBLE
            
            // アクセシビリティ: 警告状態を音声読み上げに含める
            textCharacterCount.contentDescription = 
                "警告: ${currentCharacterCount}文字中、最大${ClothItem.MAX_MEMO_LENGTH}文字。文字数制限に近づいています。"
        } else {
            // 通常状態: デフォルト色、警告アイコン非表示
            val normalColor = ContextCompat.getColor(context, android.R.color.secondary_text_light)
            textCharacterCount.setTextColor(normalColor)
            iconWarning.visibility = View.GONE
            
            // アクセシビリティ: 通常状態の読み上げ
            textCharacterCount.contentDescription = 
                "${currentCharacterCount}文字中、最大${ClothItem.MAX_MEMO_LENGTH}文字"
        }
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

    // ===== パブリックAPI =====

    /**
     * メモテキストを設定
     * 
     * @param memo 設定するメモテキスト（nullの場合は空文字列として処理）
     */
    fun setMemo(memo: String?) {
        val safeText = memo ?: ""
        val trimmedText = safeText.take(ClothItem.MAX_MEMO_LENGTH)
        
        // TextWatcherが動作しないよう一時的に無効化してからテキスト設定
        editTextMemo.setText(trimmedText)
        editTextMemo.setSelection(trimmedText.length)
        
        // 手動で文字数カウント更新
        currentCharacterCount = trimmedText.length
        updateCharacterCount(currentCharacterCount)
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
}
