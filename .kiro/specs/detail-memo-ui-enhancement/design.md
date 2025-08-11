# Design Document

## Overview

画像詳細画面でのメモ機能UI/UX改善は、既存のimage-memo機能を基盤として実装されます。主な改善点は、メモテキストの視認性向上のための背景色追加と、詳細情報エリアのスワイプによる表示・非表示切り替え機能です。

この設計では、既存のDetailActivityとMemoInputViewを拡張し、新しいSwipeableDetailPanelコンポーネントを追加することで、最小限の変更で最大の効果を得ることを目指します。

## Architecture

### UI層の拡張

既存のDetailActivityのレイアウト構造を以下のように変更します：

```
DetailActivity
├── ImageView (フルスクリーン表示可能)
└── SwipeableDetailPanel (新規コンポーネント)
    ├── SwipeHandle (視覚的インジケーター)
    ├── MemoInputView (背景色付きに拡張)
    ├── TagDisplayView (既存)
    └── OtherDetailViews (既存)
```

### 状態管理の拡張

DetailViewModelに新しい状態管理機能を追加：
- パネル表示状態（表示/非表示）
- アニメーション状態
- スワイプジェスチャー状態

## Components and Interfaces

### 1. SwipeableDetailPanel (新規カスタムビュー)

```kotlin
class SwipeableDetailPanel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    
    private val swipeHandle: SwipeHandleView
    private val contentContainer: LinearLayout
    private val gestureDetector: GestureDetector
    private val animator: ValueAnimator
    
    private var panelState: PanelState = PanelState.SHOWN
    private var onPanelStateChangedListener: ((PanelState) -> Unit)? = null
    
    enum class PanelState {
        SHOWN,      // パネル表示状態
        HIDDEN,     // パネル非表示状態
        ANIMATING   // アニメーション中
    }
    
    fun setPanelState(state: PanelState, animate: Boolean = true) {
        if (panelState == PanelState.ANIMATING && animate) return
        
        val targetTranslationY = when (state) {
            PanelState.SHOWN -> 0f
            PanelState.HIDDEN -> height.toFloat()
            PanelState.ANIMATING -> return
        }
        
        if (animate && !isReducedMotionEnabled()) {
            animateToPosition(targetTranslationY, state)
        } else {
            translationY = targetTranslationY
            panelState = state
            onPanelStateChangedListener?.invoke(state)
            announceStateChange(state)
        }
    }
    
    private fun animateToPosition(targetY: Float, targetState: PanelState) {
        panelState = PanelState.ANIMATING
        
        animator.apply {
            setFloatValues(translationY, targetY)
            duration = ANIMATION_DURATION
            interpolator = DecelerateInterpolator()
            
            addUpdateListener { animation ->
                translationY = animation.animatedValue as Float
            }
            
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    panelState = targetState
                    onPanelStateChangedListener?.invoke(targetState)
                    announceStateChange(targetState)
                }
            })
            
            start()
        }
    }
    
    private fun handleSwipeGesture(velocityY: Float, deltaY: Float): Boolean {
        val threshold = height * SWIPE_THRESHOLD_RATIO
        
        return when (panelState) {
            PanelState.SHOWN -> {
                if (deltaY > threshold || velocityY > MIN_FLING_VELOCITY) {
                    setPanelState(PanelState.HIDDEN)
                    true
                } else false
            }
            PanelState.HIDDEN -> {
                if (deltaY < -threshold || velocityY < -MIN_FLING_VELOCITY) {
                    setPanelState(PanelState.SHOWN)
                    true
                } else false
            }
            PanelState.ANIMATING -> false
        }
    }
    
    private fun announceStateChange(state: PanelState) {
        val announcement = when (state) {
            PanelState.SHOWN -> context.getString(R.string.detail_panel_shown)
            PanelState.HIDDEN -> context.getString(R.string.detail_panel_hidden)
            PanelState.ANIMATING -> return
        }
        announceForAccessibility(announcement)
    }
    
    companion object {
        private const val ANIMATION_DURATION = 300L
        private const val SWIPE_THRESHOLD_RATIO = 0.3f
        private const val MIN_FLING_VELOCITY = 1000f
    }
}
```

### 2. SwipeHandleView (新規カスタムビュー)

```kotlin
class SwipeHandleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val handleRect = RectF()
    
    private var handleColor: Int = ContextCompat.getColor(context, R.color.swipe_handle_color)
    private var isVisible: Boolean = true
    
    init {
        contentDescription = context.getString(R.string.swipe_handle_description)
        isClickable = true
        isFocusable = true
        
        // タップでもパネル切り替え可能
        setOnClickListener {
            (parent as? SwipeableDetailPanel)?.togglePanelState()
        }
    }
    
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (!isVisible || canvas == null) return
        
        val handleWidth = width * 0.3f
        val handleHeight = height * 0.4f
        val centerX = width / 2f
        val centerY = height / 2f
        
        handleRect.set(
            centerX - handleWidth / 2,
            centerY - handleHeight / 2,
            centerX + handleWidth / 2,
            centerY + handleHeight / 2
        )
        
        paint.color = handleColor
        canvas.drawRoundRect(handleRect, handleHeight / 2, handleHeight / 2, paint)
    }
    
    fun setHandleVisibility(visible: Boolean) {
        isVisible = visible
        invalidate()
    }
    
    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo?) {
        super.onInitializeAccessibilityNodeInfo(info)
        info?.apply {
            addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK)
            roleDescription = context.getString(R.string.swipe_handle_role)
        }
    }
}
```

### 3. MemoInputView拡張 (背景色対応)

```kotlin
// 既存のMemoInputViewを拡張
class MemoInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    
    private val backgroundDrawable: GradientDrawable
    private var memoBackgroundColor: Int = ContextCompat.getColor(context, R.color.memo_background)
    
    init {
        // 背景色の設定
        backgroundDrawable = GradientDrawable().apply {
            cornerRadius = resources.getDimension(R.dimen.memo_background_corner_radius)
            setColor(memoBackgroundColor)
        }
        
        // パディングを追加してテキストと背景の間にスペースを確保
        val padding = resources.getDimensionPixelSize(R.dimen.memo_background_padding)
        setPadding(padding, padding, padding, padding)
    }
    
    fun setMemo(memo: String) {
        editText.setText(memo)
        updateCharacterCount(memo.length)
        updateBackgroundVisibility(memo.isNotBlank())
    }
    
    private fun updateBackgroundVisibility(hasMemo: Boolean) {
        background = if (hasMemo) backgroundDrawable else null
        
        // アクセシビリティ用の状態更新
        contentDescription = if (hasMemo) {
            context.getString(R.string.memo_with_background_description)
        } else {
            context.getString(R.string.memo_input_description)
        }
    }
    
    fun setMemoBackgroundColor(@ColorInt color: Int) {
        memoBackgroundColor = color
        backgroundDrawable.setColor(color)
        
        // コントラスト比を確認
        val textColor = editText.currentTextColor
        if (!hasMinimumContrast(color, textColor)) {
            Log.w(TAG, "Memo background color may not have sufficient contrast")
        }
    }
    
    private fun hasMinimumContrast(backgroundColor: Int, textColor: Int): Boolean {
        return ColorUtils.calculateContrast(textColor, backgroundColor) >= MIN_CONTRAST_RATIO
    }
    
    companion object {
        private const val MIN_CONTRAST_RATIO = 4.5
        private const val TAG = "MemoInputView"
    }
}
```

### 4. DetailViewModel拡張

```kotlin
class DetailViewModel(
    private val repository: ClothRepository,
    private val preferencesManager: DetailPreferencesManager // 新規追加
) : ViewModel() {
    
    // 既存のプロパティ...
    
    // 新規追加: パネル状態管理
    private val _panelState = MutableLiveData<SwipeableDetailPanel.PanelState>()
    val panelState: LiveData<SwipeableDetailPanel.PanelState> = _panelState
    
    private val _isFullScreenMode = MutableLiveData<Boolean>()
    val isFullScreenMode: LiveData<Boolean> = _isFullScreenMode
    
    init {
        // セッション中のパネル状態を復元
        _panelState.value = preferencesManager.getLastPanelState()
        _isFullScreenMode.value = _panelState.value == SwipeableDetailPanel.PanelState.HIDDEN
    }
    
    fun setPanelState(state: SwipeableDetailPanel.PanelState) {
        _panelState.value = state
        _isFullScreenMode.value = state == SwipeableDetailPanel.PanelState.HIDDEN
        
        // セッション中の状態を保存
        preferencesManager.saveLastPanelState(state)
    }
    
    fun togglePanelState() {
        val currentState = _panelState.value ?: SwipeableDetailPanel.PanelState.SHOWN
        val newState = when (currentState) {
            SwipeableDetailPanel.PanelState.SHOWN -> SwipeableDetailPanel.PanelState.HIDDEN
            SwipeableDetailPanel.PanelState.HIDDEN -> SwipeableDetailPanel.PanelState.SHOWN
            SwipeableDetailPanel.PanelState.ANIMATING -> return // アニメーション中は無視
        }
        setPanelState(newState)
    }
    
    override fun onCleared() {
        super.onCleared()
        // ViewModelが破棄される際に状態を保存
        _panelState.value?.let { state ->
            preferencesManager.saveLastPanelState(state)
        }
    }
}
```

### 5. DetailPreferencesManager (新規クラス)

```kotlin
class DetailPreferencesManager(context: Context) {
    
    private val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    fun saveLastPanelState(state: SwipeableDetailPanel.PanelState) {
        sharedPreferences.edit()
            .putString(KEY_LAST_PANEL_STATE, state.name)
            .apply()
    }
    
    fun getLastPanelState(): SwipeableDetailPanel.PanelState {
        val stateName = sharedPreferences.getString(KEY_LAST_PANEL_STATE, null)
        return try {
            stateName?.let { SwipeableDetailPanel.PanelState.valueOf(it) }
                ?: SwipeableDetailPanel.PanelState.SHOWN
        } catch (e: IllegalArgumentException) {
            SwipeableDetailPanel.PanelState.SHOWN
        }
    }
    
    companion object {
        private const val PREFS_NAME = "detail_preferences"
        private const val KEY_LAST_PANEL_STATE = "last_panel_state"
    }
}
```

## Data Models

### パネル状態の定義

```kotlin
enum class PanelState {
    SHOWN,      // パネル表示状態
    HIDDEN,     // パネル非表示状態  
    ANIMATING   // アニメーション中
}

data class PanelConfiguration(
    val animationDuration: Long = 300L,
    val swipeThreshold: Float = 0.3f,
    val minFlingVelocity: Float = 1000f,
    val enableReducedMotion: Boolean = false
) {
    companion object {
        fun fromDeviceCapabilities(context: Context): PanelConfiguration {
            val isLowEndDevice = isLowEndDevice(context)
            val hasReducedMotion = hasReducedMotionEnabled(context)
            
            return PanelConfiguration(
                animationDuration = if (isLowEndDevice) 200L else 300L,
                enableReducedMotion = hasReducedMotion
            )
        }
        
        private fun isLowEndDevice(context: Context): Boolean {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            return activityManager.isLowRamDevice
        }
        
        private fun hasReducedMotionEnabled(context: Context): Boolean {
            val resolver = context.contentResolver
            return Settings.Global.getFloat(
                resolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            ) == 0.0f
        }
    }
}
```

## Error Handling

### スワイプ機能のエラーハンドリング

```kotlin
class SwipeGestureErrorHandler {
    
    fun handleAnimationError(error: Exception, panel: SwipeableDetailPanel): SwipeErrorResult {
        return when (error) {
            is IllegalStateException -> {
                Log.e(TAG, "Animation state error", error)
                // アニメーション状態をリセット
                panel.resetAnimationState()
                SwipeErrorResult.Recovered("アニメーション状態をリセットしました")
            }
            is OutOfMemoryError -> {
                Log.e(TAG, "Memory error during animation", error)
                // アニメーションを無効化
                panel.disableAnimations()
                SwipeErrorResult.Degraded("メモリ不足のためアニメーションを無効化しました")
            }
            else -> {
                Log.e(TAG, "Unexpected animation error", error)
                SwipeErrorResult.Error("予期しないエラーが発生しました")
            }
        }
    }
    
    fun handleGestureConflict(
        currentGesture: GestureType,
        conflictingGesture: GestureType
    ): GestureResolution {
        return when {
            currentGesture == GestureType.SWIPE_PANEL && 
            conflictingGesture == GestureType.SCROLL_MEMO -> {
                // メモスクロールを優先
                GestureResolution.PreferSecondary
            }
            currentGesture == GestureType.SWIPE_PANEL && 
            conflictingGesture == GestureType.IMAGE_ZOOM -> {
                // 画像ズームを優先
                GestureResolution.PreferSecondary
            }
            else -> GestureResolution.PreferPrimary
        }
    }
    
    companion object {
        private const val TAG = "SwipeGestureErrorHandler"
    }
}

sealed class SwipeErrorResult {
    data class Recovered(val message: String) : SwipeErrorResult()
    data class Degraded(val message: String) : SwipeErrorResult()
    data class Error(val message: String) : SwipeErrorResult()
}

enum class GestureType {
    SWIPE_PANEL,
    SCROLL_MEMO,
    IMAGE_ZOOM,
    TAP
}

enum class GestureResolution {
    PreferPrimary,
    PreferSecondary,
    Cancel
}
```

## Testing Strategy

### 1. ユニットテスト

#### SwipeableDetailPanelテスト
```kotlin
class SwipeableDetailPanelTest {
    
    @Test
    fun `setPanelState should update state correctly`() {
        val panel = SwipeableDetailPanel(context)
        var stateChanged = false
        
        panel.setOnPanelStateChangedListener { state ->
            stateChanged = true
            assertEquals(SwipeableDetailPanel.PanelState.HIDDEN, state)
        }
        
        panel.setPanelState(SwipeableDetailPanel.PanelState.HIDDEN, animate = false)
        
        assertTrue(stateChanged)
        assertEquals(SwipeableDetailPanel.PanelState.HIDDEN, panel.panelState)
    }
    
    @Test
    fun `handleSwipeGesture should respect threshold`() {
        val panel = SwipeableDetailPanel(context)
        panel.layout(0, 0, 100, 200) // height = 200
        
        // 閾値以下のスワイプは無視
        val smallDelta = 200 * 0.2f // 20% < 30% threshold
        val result = panel.handleSwipeGesture(0f, smallDelta)
        
        assertFalse(result)
        assertEquals(SwipeableDetailPanel.PanelState.SHOWN, panel.panelState)
    }
}
```

#### MemoInputView背景色テスト
```kotlin
class MemoInputViewBackgroundTest {
    
    @Test
    fun `setMemo should show background when memo is not empty`() {
        val memoView = MemoInputView(context)
        
        memoView.setMemo("テストメモ")
        
        assertNotNull(memoView.background)
    }
    
    @Test
    fun `setMemo should hide background when memo is empty`() {
        val memoView = MemoInputView(context)
        
        memoView.setMemo("")
        
        assertNull(memoView.background)
    }
    
    @Test
    fun `setMemoBackgroundColor should maintain minimum contrast`() {
        val memoView = MemoInputView(context)
        val lowContrastColor = Color.parseColor("#CCCCCC") // グレー
        
        memoView.setMemoBackgroundColor(lowContrastColor)
        
        // ログに警告が出力されることを確認
        // 実際の実装では、コントラスト比チェックのロジックをテスト
    }
}
```

### 2. インストルメンテーションテスト

#### スワイプジェスチャーテスト
```kotlin
class SwipeGestureEspressoTest {
    
    @Test
    fun testSwipeUpHidesPanel() {
        onView(withId(R.id.swipeable_detail_panel))
            .perform(swipeUp())
        
        onView(withId(R.id.detail_content))
            .check(matches(not(isDisplayed())))
        
        onView(withId(R.id.image_view))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun testSwipeDownShowsPanel() {
        // まずパネルを隠す
        onView(withId(R.id.swipeable_detail_panel))
            .perform(swipeUp())
        
        // 下スワイプで表示
        onView(withId(R.id.swipeable_detail_panel))
            .perform(swipeDown())
        
        onView(withId(R.id.detail_content))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun testSwipeHandleTapTogglesPanelState() {
        onView(withId(R.id.swipe_handle))
            .perform(click())
        
        onView(withId(R.id.detail_content))
            .check(matches(not(isDisplayed())))
        
        onView(withId(R.id.swipe_handle))
            .perform(click())
        
        onView(withId(R.id.detail_content))
            .check(matches(isDisplayed()))
    }
}
```

#### メモ背景色テスト
```kotlin
class MemoBackgroundEspressoTest {
    
    @Test
    fun testMemoBackgroundVisibilityWithText() {
        onView(withId(R.id.memo_input))
            .perform(typeText("テストメモ"))
        
        onView(withId(R.id.memo_input_view))
            .check(matches(hasBackground()))
    }
    
    @Test
    fun testMemoBackgroundHiddenWhenEmpty() {
        onView(withId(R.id.memo_input))
            .perform(clearText())
        
        onView(withId(R.id.memo_input_view))
            .check(matches(not(hasBackground())))
    }
}
```

### 3. アクセシビリティテスト

```kotlin
class DetailMemoAccessibilityTest {
    
    @Test
    fun testSwipeHandleAccessibility() {
        onView(withId(R.id.swipe_handle))
            .check(matches(hasContentDescription()))
            .check(matches(isClickable()))
            .check(matches(isFocusable()))
    }
    
    @Test
    fun testPanelStateAnnouncement() {
        // TalkBackが有効な状態をシミュレート
        enableAccessibilityService()
        
        onView(withId(R.id.swipe_handle))
            .perform(click())
        
        // アクセシビリティアナウンスメントを確認
        verify(accessibilityManager).announce(
            eq("詳細パネルが非表示になりました")
        )
    }
    
    @Test
    fun testMemoBackgroundContrast() {
        onView(withId(R.id.memo_input))
            .perform(typeText("テストメモ"))
        
        onView(withId(R.id.memo_input_view))
            .check(matches(hasMinimumContrast(4.5f)))
    }
}
```

## Implementation Considerations

### 1. パフォーマンス最適化

- **アニメーション最適化**: ハードウェアアクセラレーションの活用
- **メモリ効率**: 大きな画像でのメモリ使用量最適化
- **ローエンドデバイス対応**: 簡略化されたアニメーション

### 2. ユーザビリティ

- **直感的なジェスチャー**: 自然なスワイプ動作
- **視覚的フィードバック**: スワイプハンドルとアニメーション
- **状態の永続化**: セッション中の設定保持

### 3. アクセシビリティ

- **スクリーンリーダー対応**: 適切な状態アナウンス
- **キーボードナビゲーション**: 代替操作方法の提供
- **コントラスト比**: WCAG準拠の色彩設計

### 4. デバイス対応

- **画面サイズ対応**: タブレットと電話での最適化
- **向き変更対応**: 縦横回転時の状態保持
- **システム設定対応**: アニメーション無効化設定への対応