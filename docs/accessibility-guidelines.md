# SwipeableDetailPanel アクセシビリティガイドライン

## 概要

SwipeableDetailPanel機能は、すべてのユーザーが等しく利用できるよう、包括的なアクセシビリティ機能を実装しています。本ガイドラインでは、視覚、聴覚、運動機能に制約があるユーザーへの対応方法と、開発者向けのアクセシビリティ実装指針を説明します。

## 対応するアクセシビリティ基準

### WCAG 2.1 準拠レベル
- **レベルAA準拠**: コントラスト比、フォーカス管理、キーボード操作
- **レベルAAA対応**: 高コントラストモード、詳細な音声説明

### Android アクセシビリティサービス対応
- **TalkBack**: 完全対応
- **Switch Access**: キーボード・スイッチ操作対応
- **Voice Access**: 音声操作対応
- **高コントラスト**: システム設定連動

## 機能別アクセシビリティ対応

### 1. SwipeHandleView アクセシビリティ

#### 基本設定
```xml
<!-- res/layout/swipe_handle_view.xml -->
<com.example.clothstock.ui.common.SwipeHandleView
    android:id="@+id/swipeHandle"
    android:layout_width="match_parent"
    android:layout_height="@dimen/swipe_handle_height"
    android:contentDescription="@string/swipe_handle_description"
    android:focusable="true"
    android:clickable="true"
    android:nextFocusDown="@id/editTextMemo" />
```

#### 文字列リソース
```xml
<!-- res/values/strings.xml -->
<string name="swipe_handle_description">詳細パネルハンドル。タップで表示・非表示を切り替えできます</string>
<string name="swipe_handle_role">コントロール</string>
<string name="swipe_handle_action_show">詳細パネルを表示</string>
<string name="swipe_handle_action_hide">詳細パネルを非表示</string>
```

#### プログラム実装
```kotlin
class SwipeHandleView : View {
    
    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo?) {
        super.onInitializeAccessibilityNodeInfo(info)
        info?.apply {
            // ロール設定
            roleDescription = context.getString(R.string.swipe_handle_role)
            
            // カスタムアクション追加
            addAction(AccessibilityNodeInfo.AccessibilityAction(
                AccessibilityNodeInfo.ACTION_CLICK,
                context.getString(
                    if (isPanelShown) R.string.swipe_handle_action_hide 
                    else R.string.swipe_handle_action_show
                )
            ))
            
            // 状態情報
            isEnabled = true
            isClickable = true
            isFocusable = true
        }
    }
    
    override fun performAccessibilityAction(action: Int, arguments: Bundle?): Boolean {
        return when (action) {
            AccessibilityNodeInfo.ACTION_CLICK -> {
                performClick()
                true
            }
            else -> super.performAccessibilityAction(action, arguments)
        }
    }
}
```

### 2. パネル状態変更の音声通知

#### LiveRegion設定
```xml
<!-- SwipeableDetailPanel内 -->
<LinearLayout
    android:id="@+id/contentContainer"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:accessibilityLiveRegion="polite"
    android:contentDescription="@string/detail_panel_description" />
```

#### 状態変更アナウンス実装
```kotlin
class SwipeableDetailPanel : ConstraintLayout {
    
    private fun announceStateChange(state: PanelState) {
        val announcement = when (state) {
            PanelState.SHOWN -> getString(R.string.detail_panel_shown_announcement)
            PanelState.HIDDEN -> getString(R.string.detail_panel_hidden_announcement)
            PanelState.ANIMATING -> return // アニメーション中は通知しない
        }
        
        // LiveRegionを使った自動アナウンス
        announceForAccessibility(announcement)
        
        // アクセシビリティサービスへの直接通知
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) 
            as AccessibilityManager
        
        if (accessibilityManager.isEnabled) {
            val accessibilityEvent = AccessibilityEvent.obtain(
                AccessibilityEvent.TYPE_ANNOUNCEMENT
            ).apply {
                text.add(announcement)
                packageName = context.packageName
                className = this@SwipeableDetailPanel.javaClass.name
            }
            
            accessibilityManager.sendAccessibilityEvent(accessibilityEvent)
        }
    }
}
```

#### アナウンス文字列リソース
```xml
<!-- res/values/strings.xml -->
<string name="detail_panel_shown_announcement">詳細パネルが表示されました。メモの編集やタグ情報の確認ができます</string>
<string name="detail_panel_hidden_announcement">詳細パネルが非表示になりました。画像が全画面表示されています</string>
<string name="detail_panel_description">衣服の詳細情報パネル</string>
```

### 3. MemoInputView アクセシビリティ

#### 入力フィールド設定
```xml
<!-- MemoInputView内のEditText -->
<EditText
    android:id="@+id/editTextMemo"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:hint="@string/memo_input_hint"
    android:contentDescription="@string/memo_input_description"
    android:imeOptions="actionDone"
    android:inputType="textMultiLine|textCapSentences"
    android:nextFocusDown="@id/textCharacterCount" />
    
<TextView
    android:id="@+id/textCharacterCount"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:contentDescription="@string/character_count_description"
    android:accessibilityLiveRegion="polite" />
```

#### 背景状態の説明更新
```kotlin
class MemoInputView : LinearLayout {
    
    private fun updateBackgroundVisibility(hasMemo: Boolean) {
        background = if (hasMemo) backgroundDrawable else null
        
        // アクセシビリティ説明の更新
        contentDescription = when {
            hasMemo -> getString(R.string.memo_with_background_description)
            else -> getString(R.string.memo_input_description)
        }
        
        // 背景状態の変更をアナウンス
        if (hasMemo) {
            announceForAccessibility(getString(R.string.memo_background_added))
        }
    }
    
    private fun updateCharacterCount(count: Int) {
        val maxLength = ClothItem.MAX_MEMO_LENGTH
        textCharacterCount.text = "$count/$maxLength"
        
        // 文字数情報のアクセシビリティ説明
        val description = when {
            count > maxLength * 0.9 -> {
                getString(R.string.character_count_warning_description, count, maxLength)
            }
            count > maxLength -> {
                getString(R.string.character_count_error_description, count, maxLength)
            }
            else -> {
                getString(R.string.character_count_normal_description, count, maxLength)
            }
        }
        
        textCharacterCount.contentDescription = description
        
        // 警告レベル到達時のアナウンス
        if (count > maxLength * 0.9 && count <= maxLength) {
            announceForAccessibility(getString(R.string.memo_length_warning_announcement))
        }
    }
}
```

#### メモ関連文字列リソース
```xml
<!-- res/values/strings.xml -->
<string name="memo_input_hint">メモを入力してください</string>
<string name="memo_input_description">衣服のメモ入力欄</string>
<string name="memo_with_background_description">衣服のメモ入力欄（背景付き）</string>
<string name="memo_background_added">メモ背景が表示されました</string>

<string name="character_count_description">文字数カウンター</string>
<string name="character_count_normal_description">%1$d文字入力済み、%2$d文字まで入力可能</string>
<string name="character_count_warning_description">警告：%1$d文字入力済み、制限%2$d文字に近づいています</string>
<string name="character_count_error_description">エラー：%1$d文字入力済み、制限%2$d文字を超過しています</string>

<string name="memo_length_warning_announcement">メモが制限文字数に近づいています</string>
```

### 4. コントラスト比とカラー設定

#### カラーリソース（標準モード）
```xml
<!-- res/values/colors.xml -->
<color name="memo_background">#80FFFFFF</color> <!-- 白50%透明 -->
<color name="memo_text">#000000</color> <!-- 黒 -->
<color name="swipe_handle_color">#666666</color> <!-- グレー -->
<color name="swipe_handle_background">#CCFFFFFF</color> <!-- 白80%透明 -->

<!-- 警告色 -->
<color name="memo_warning_background">#FFFFCC99</color> <!-- オレンジ80%透明 -->
<color name="memo_error_background">#FFFF9999</color> <!-- 赤80%透明 -->
```

#### 高コントラストモード用カラー
```xml
<!-- res/values-night/colors.xml -->
<color name="memo_background">#E0000000</color> <!-- 黒90%透明 -->
<color name="memo_text">#FFFFFF</color> <!-- 白 -->
<color name="swipe_handle_color">#FFFFFF</color> <!-- 白 -->
<color name="swipe_handle_background">#CC000000</color> <!-- 黒80%透明 -->

<!-- 高コントラスト警告色 -->
<color name="memo_warning_background">#E0332200</color> <!-- 濃いオレンジ90%透明 -->
<color name="memo_error_background">#E0330000</color> <!-- 濃い赤90%透明 -->
```

#### プログラムによるコントラスト確認
```kotlin
object AccessibilityColorUtils {
    
    private const val MIN_CONTRAST_RATIO_AA = 4.5
    private const val MIN_CONTRAST_RATIO_AAA = 7.0
    
    fun validateContrast(backgroundColor: Int, textColor: Int): ContrastValidation {
        val ratio = ColorUtils.calculateContrast(textColor, backgroundColor)
        
        return ContrastValidation(
            ratio = ratio,
            isAACompliant = ratio >= MIN_CONTRAST_RATIO_AA,
            isAAACompliant = ratio >= MIN_CONTRAST_RATIO_AAA,
            recommendation = getRecommendation(ratio)
        )
    }
    
    private fun getRecommendation(ratio: Double): String {
        return when {
            ratio >= MIN_CONTRAST_RATIO_AAA -> "優秀：AAA基準を満たしています"
            ratio >= MIN_CONTRAST_RATIO_AA -> "良好：AA基準を満たしています"
            ratio >= 3.0 -> "改善推奨：コントラストが不十分です"
            else -> "要修正：コントラストが不適切です"
        }
    }
    
    fun adjustForHighContrast(context: Context, originalColor: Int): Int {
        val isHighContrastEnabled = Settings.Secure.getInt(
            context.contentResolver,
            "high_text_contrast_enabled",
            0
        ) == 1
        
        return if (isHighContrastEnabled) {
            // 高コントラストモード用の色調整
            adjustColorForHighContrast(originalColor)
        } else {
            originalColor
        }
    }
    
    private fun adjustColorForHighContrast(color: Int): Int {
        // 明度を調整してコントラストを向上
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        
        // 明度を極端に調整（0.2以下または0.8以上）
        hsv[2] = if (hsv[2] < 0.5f) 0.1f else 0.9f
        
        return Color.HSVToColor(Color.alpha(color), hsv)
    }
    
    data class ContrastValidation(
        val ratio: Double,
        val isAACompliant: Boolean,
        val isAAACompliant: Boolean,
        val recommendation: String
    )
}
```

### 5. キーボードナビゲーション

#### フォーカス順序設定
```xml
<!-- DetailActivity レイアウト -->
<com.example.clothstock.ui.common.SwipeHandleView
    android:id="@+id/swipeHandle"
    android:nextFocusDown="@id/editTextMemo"
    android:nextFocusUp="@id/imageViewClothDetail" />

<EditText
    android:id="@+id/editTextMemo"
    android:nextFocusDown="@id/textSize"
    android:nextFocusUp="@id/swipeHandle" />

<TextView
    android:id="@+id/textSize"
    android:focusable="true"
    android:nextFocusDown="@id/textColor"
    android:nextFocusUp="@id/editTextMemo" />

<TextView
    android:id="@+id/textColor"
    android:focusable="true"
    android:nextFocusDown="@id/textCategory"
    android:nextFocusUp="@id/textSize" />

<TextView
    android:id="@+id/textCategory"
    android:focusable="true"
    android:nextFocusUp="@id/textColor" />
```

#### カスタムキー操作処理
```kotlin
class SwipeableDetailPanel : ConstraintLayout {
    
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_SPACE,
            KeyEvent.KEYCODE_ENTER -> {
                if (swipeHandle.hasFocus()) {
                    togglePanelState()
                    true
                } else {
                    super.onKeyDown(keyCode, event)
                }
            }
            KeyEvent.KEYCODE_ESCAPE -> {
                // ESCキーでパネル表示に戻る
                if (panelState == PanelState.HIDDEN) {
                    setPanelState(PanelState.SHOWN)
                    true
                } else {
                    super.onKeyDown(keyCode, event)
                }
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
    
    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        // アクセシビリティサービス使用時の特別処理
        if (isAccessibilityServiceEnabled() && event?.action == KeyEvent.ACTION_DOWN) {
            return handleAccessibilityKeyEvent(event) || super.dispatchKeyEvent(event)
        }
        return super.dispatchKeyEvent(event)
    }
    
    private fun handleAccessibilityKeyEvent(event: KeyEvent): Boolean {
        return when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                // 上矢印キーでパネル非表示
                if (panelState == PanelState.SHOWN) {
                    setPanelState(PanelState.HIDDEN)
                    announceForAccessibility("詳細パネルを非表示にしました")
                    true
                } else false
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                // 下矢印キーでパネル表示
                if (panelState == PanelState.HIDDEN) {
                    setPanelState(PanelState.SHOWN)
                    announceForAccessibility("詳細パネルを表示しました")
                    true
                } else false
            }
            else -> false
        }
    }
}
```

### 6. アクセシビリティサービス検出

#### サービス状態確認
```kotlin
object AccessibilityUtils {
    
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) 
            as AccessibilityManager
        return accessibilityManager.isEnabled
    }
    
    fun isTalkBackEnabled(context: Context): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        
        return enabledServices.contains("com.google.android.marvin.talkback") ||
               enabledServices.contains("com.samsung.android.app.talkback") ||
               enabledServices.contains("talkback")
    }
    
    fun isSwitchAccessEnabled(context: Context): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        
        return enabledServices.contains("switchaccess") ||
               enabledServices.contains("com.google.android.accessibility.switchaccess")
    }
    
    fun getAccessibilityTimeout(context: Context): Int {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) 
            as AccessibilityManager
        
        return if (isAccessibilityServiceEnabled(context)) {
            // アクセシビリティサービス使用時は長めのタイムアウト
            accessibilityManager.getRecommendedTimeoutMillis(3000, 
                AccessibilityManager.FLAG_CONTENT_CONTROLS)
        } else {
            3000 // 標準タイムアウト
        }
    }
}
```

## テスト指針

### 自動テスト

#### Espresso アクセシビリティチェック
```kotlin
class AccessibilityTest {
    
    @BeforeClass
    fun enableAccessibilityChecks() {
        AccessibilityChecks.enable()
            .setRunChecksFromRootView(true)
            .setSuppressingResultMatcher(
                allOf(
                    matchesCheck(DuplicateClickableBoundsCheck::class.java),
                    hasDescendant(withText("ギャラリー"))
                )
            )
    }
    
    @Test
    fun testSwipeHandleAccessibility() {
        onView(withId(R.id.swipeHandle))
            .check(matches(hasContentDescription()))
            .check(matches(isFocusable()))
            .check(matches(isClickable()))
            .check(matches(hasMinimumTouchTargetSize()))
    }
    
    @Test
    fun testContrastRatio() {
        onView(withId(R.id.editTextMemo))
            .perform(typeText("テストメモ"))
            
        onView(withId(R.id.memoInputView))
            .check(matches(hasMinimumContrast(4.5f)))
    }
}
```

#### カスタムマッチャー
```kotlin
fun hasMinimumContrast(minimumRatio: Float): Matcher<View> {
    return object : TypeSafeMatcher<View>() {
        override fun describeTo(description: Description?) {
            description?.appendText("has minimum contrast ratio of $minimumRatio")
        }
        
        override fun matchesSafely(item: View?): Boolean {
            if (item !is TextView) return true
            
            val textColor = item.currentTextColor
            val backgroundColor = extractBackgroundColor(item)
            val ratio = ColorUtils.calculateContrast(textColor, backgroundColor)
            
            return ratio >= minimumRatio
        }
    }
}

fun hasMinimumTouchTargetSize(): Matcher<View> {
    return object : TypeSafeMatcher<View>() {
        override fun describeTo(description: Description?) {
            description?.appendText("has minimum touch target size of 48dp")
        }
        
        override fun matchesSafely(item: View?): Boolean {
            val minSize = (48 * item!!.resources.displayMetrics.density).toInt()
            return item.measuredWidth >= minSize && item.measuredHeight >= minSize
        }
    }
}
```

### 手動テスト チェックリスト

#### TalkBack テスト
- [ ] TalkBackを有効にして全ての要素が読み上げられる
- [ ] SwipeHandleのタップで適切なアナウンスが流れる
- [ ] パネル状態変更時にアナウンスされる
- [ ] メモ入力時に文字数が読み上げられる
- [ ] 警告状態が適切にアナウンスされる

#### Switch Access テスト
- [ ] スイッチ操作で全ての要素にフォーカス移動できる
- [ ] SwipeHandleでパネル切り替えができる
- [ ] メモ入力ができる
- [ ] スキャン順序が論理的である

#### キーボード操作テスト
- [ ] Tabキーでフォーカス移動できる
- [ ] Enter/スペースキーでSwipeHandleを操作できる
- [ ] 矢印キーでパネル操作できる（アクセシビリティサービス有効時）
- [ ] ESCキーでパネル表示に戻れる

#### 高コントラストモードテスト
- [ ] システムの高コントラスト設定に連動する
- [ ] メモ背景色が適切に調整される
- [ ] SwipeHandleの色が適切に調整される
- [ ] 7:1以上のコントラスト比が確保される

## 開発者向けガイドライン

### 新機能追加時のアクセシビリティチェック

1. **contentDescription設定**
   - 全ての操作可能要素に適切な説明を設定
   - 状態変化時に説明を更新

2. **フォーカス管理**
   - nextFocus属性で論理的なフォーカス順序を設定
   - 動的にフォーカスを移動させる場合は注意

3. **コントラスト確認**
   - 新しい色を追加する際は必ずコントラスト比を確認
   - AccessibilityColorUtilsを使用して検証

4. **キーボード操作対応**
   - マウス/タッチ操作の代替手段を提供
   - カスタムキー処理を適切に実装

5. **状態変更の通知**
   - 重要な状態変更はannounceForAccessibilityで通知
   - LiveRegionを適切に設定

### パフォーマンス考慮事項

#### アクセシビリティサービス使用時の最適化
```kotlin
class AccessibilityPerformanceManager {
    
    fun adjustAnimationForAccessibility(context: Context, defaultDuration: Long): Long {
        return when {
            !AccessibilityUtils.isAccessibilityServiceEnabled(context) -> defaultDuration
            AccessibilityUtils.isTalkBackEnabled(context) -> defaultDuration * 1.5 // 少し長め
            AccessibilityUtils.isSwitchAccessEnabled(context) -> defaultDuration * 2 // より長め
            else -> defaultDuration
        }
    }
    
    fun shouldUseReducedMotion(context: Context): Boolean {
        val scale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1.0f
        )
        
        return scale == 0.0f || AccessibilityUtils.isAccessibilityServiceEnabled(context)
    }
}
```

## まとめ

SwipeableDetailPanel機能は、包括的なアクセシビリティ対応により、すべてのユーザーが等しく利用できる機能として実装されています。継続的な改善とユーザーフィードバックの収集を通じて、より使いやすい機能に発展させていきます。

アクセシビリティに関する問題や改善提案がある場合は、プロジェクトのIssueトラッカーまでお知らせください。