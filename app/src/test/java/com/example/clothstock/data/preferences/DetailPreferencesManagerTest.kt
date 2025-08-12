package com.example.clothstock.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.clothstock.ui.common.SwipeableDetailPanel
import io.mockk.*
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * DetailPreferencesManager のユニットテスト
 * 
 * TDD Red フェーズ - 失敗テストを作成
 * Requirements: 5.1, 5.2, 5.4, 5.5 に対応
 */
class DetailPreferencesManagerTest {

    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var detailPreferencesManager: DetailPreferencesManager

    @Before
    fun setup() {
        context = mockk()
        sharedPreferences = mockk()
        editor = mockk(relaxed = true)
        
        every { context.getSharedPreferences(any(), any()) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.apply() } just Runs
        every { editor.commit() } returns true  // リファクタリング後のcommit()に対応
        
        detailPreferencesManager = DetailPreferencesManager(context)
    }

    /**
     * パネル状態の保存機能の失敗テスト
     * Requirement 5.1: パネル状態をセッション中に保持する
     */
    @Test
    fun `saveLastPanelState should save SHOWN state to SharedPreferences`() {
        // GIVEN: SHOWN状態を保存する
        val panelState = SwipeableDetailPanel.PanelState.SHOWN
        
        // WHEN: パネル状態を保存する
        detailPreferencesManager.saveLastPanelState(panelState)
        
        // THEN: SharedPreferences に正しく保存される
        verify { editor.putString("last_panel_state", "SHOWN") }
        verify { editor.commit() }
    }

    @Test
    fun `saveLastPanelState should save HIDDEN state to SharedPreferences`() {
        // GIVEN: HIDDEN状態を保存する
        val panelState = SwipeableDetailPanel.PanelState.HIDDEN
        
        // WHEN: パネル状態を保存する
        detailPreferencesManager.saveLastPanelState(panelState)
        
        // THEN: SharedPreferences に正しく保存される
        verify { editor.putString("last_panel_state", "HIDDEN") }
        verify { editor.commit() }
    }

    @Test
    fun `saveLastPanelState should save ANIMATING state to SharedPreferences`() {
        // GIVEN: ANIMATING状態を保存する
        val panelState = SwipeableDetailPanel.PanelState.ANIMATING
        
        // WHEN: パネル状態を保存する
        detailPreferencesManager.saveLastPanelState(panelState)
        
        // THEN: SharedPreferences に正しく保存される
        verify { editor.putString("last_panel_state", "ANIMATING") }
        verify { editor.commit() }
    }

    /**
     * パネル状態の復元機能の失敗テスト  
     * Requirement 5.2: ナビゲーション時に同じパネル状態を適用する
     */
    @Test
    fun `getLastPanelState should return SHOWN when stored value is SHOWN`() {
        // GIVEN: SharedPreferences に SHOWN が保存されている
        every { sharedPreferences.getString("last_panel_state", null) } returns "SHOWN"
        
        // WHEN: パネル状態を取得する
        val result = detailPreferencesManager.getLastPanelState()
        
        // THEN: SHOWN状態が返される
        assertEquals(SwipeableDetailPanel.PanelState.SHOWN, result)
    }

    @Test
    fun `getLastPanelState should return HIDDEN when stored value is HIDDEN`() {
        // GIVEN: SharedPreferences に HIDDEN が保存されている
        every { sharedPreferences.getString("last_panel_state", null) } returns "HIDDEN"
        
        // WHEN: パネル状態を取得する
        val result = detailPreferencesManager.getLastPanelState()
        
        // THEN: HIDDEN状態が返される
        assertEquals(SwipeableDetailPanel.PanelState.HIDDEN, result)
    }

    @Test
    fun `getLastPanelState should return ANIMATING when stored value is ANIMATING`() {
        // GIVEN: SharedPreferences に ANIMATING が保存されている  
        every { sharedPreferences.getString("last_panel_state", null) } returns "ANIMATING"
        
        // WHEN: パネル状態を取得する
        val result = detailPreferencesManager.getLastPanelState()
        
        // THEN: ANIMATING状態が返される
        assertEquals(SwipeableDetailPanel.PanelState.ANIMATING, result)
    }

    /**
     * 不正値のフォールバック処理の失敗テスト
     * Requirement 5.4: アプリが再開された際の状態復元
     */
    @Test
    fun `getLastPanelState should return default SHOWN when no stored value exists`() {
        // GIVEN: SharedPreferences に何も保存されていない
        every { sharedPreferences.getString("last_panel_state", null) } returns null
        
        // WHEN: パネル状態を取得する  
        val result = detailPreferencesManager.getLastPanelState()
        
        // THEN: デフォルトのSHOWN状態が返される
        assertEquals(SwipeableDetailPanel.PanelState.SHOWN, result)
    }

    @Test
    fun `getLastPanelState should return default SHOWN when stored value is invalid`() {
        // GIVEN: SharedPreferences に不正な値が保存されている
        every { sharedPreferences.getString("last_panel_state", null) } returns "INVALID_STATE"
        
        // WHEN: パネル状態を取得する
        val result = detailPreferencesManager.getLastPanelState()
        
        // THEN: デフォルトのSHOWN状態が返される
        assertEquals(SwipeableDetailPanel.PanelState.SHOWN, result)
    }

    @Test
    fun `getLastPanelState should return default SHOWN when stored value is empty string`() {
        // GIVEN: SharedPreferences に空文字列が保存されている
        every { sharedPreferences.getString("last_panel_state", null) } returns ""
        
        // WHEN: パネル状態を取得する
        val result = detailPreferencesManager.getLastPanelState()
        
        // THEN: デフォルトのSHOWN状態が返される
        assertEquals(SwipeableDetailPanel.PanelState.SHOWN, result)
    }

    /**
     * SharedPreferences 操作の失敗テスト
     * Requirement 5.5: 新しいアプリセッション開始時のデフォルト状態
     */
    @Test
    fun `should use correct SharedPreferences name`() {
        // GIVEN & WHEN: DetailPreferencesManager を初期化する
        DetailPreferencesManager(context)
        
        // THEN: 正しい preferences 名でアクセスする
        verify { context.getSharedPreferences("detail_preferences", Context.MODE_PRIVATE) }
    }

    @Test
    fun `should use correct SharedPreferences key for panel state`() {
        // GIVEN: パネル状態を保存する
        val panelState = SwipeableDetailPanel.PanelState.SHOWN
        
        // WHEN: 状態保存を実行する
        detailPreferencesManager.saveLastPanelState(panelState)
        
        // THEN: 正しいキーが使用される
        verify { editor.putString("last_panel_state", "SHOWN") }
    }

    @Test
    fun `getLastPanelState should use correct SharedPreferences key`() {
        // GIVEN: SharedPreferences から値を取得する
        every { sharedPreferences.getString("last_panel_state", null) } returns "SHOWN"
        
        // WHEN: 状態取得を実行する
        detailPreferencesManager.getLastPanelState()
        
        // THEN: 正しいキーが使用される
        verify { sharedPreferences.getString("last_panel_state", null) }
    }

    /**
     * リファクタリング後のエラーハンドリングテスト
     */
    @Test
    fun `saveLastPanelState should handle commit failure gracefully`() {
        // GIVEN: commit() が失敗する
        every { editor.commit() } returns false
        val panelState = SwipeableDetailPanel.PanelState.SHOWN
        
        // WHEN: パネル状態を保存する
        detailPreferencesManager.saveLastPanelState(panelState)
        
        // THEN: エラーログが記録されるが例外は投げられない
        verify { editor.putString("last_panel_state", "SHOWN") }
        verify { editor.commit() }
    }

    @Test
    fun `saveLastPanelState should handle exception gracefully`() {
        // GIVEN: putString() でSecurityExceptionが発生する
        every { editor.putString(any(), any()) } throws SecurityException("Test exception")
        val panelState = SwipeableDetailPanel.PanelState.SHOWN
        
        // WHEN: パネル状態を保存する
        detailPreferencesManager.saveLastPanelState(panelState)
        
        // THEN: 例外が捕捉される
        verify { editor.putString("last_panel_state", "SHOWN") }
    }

    @Test
    fun `getLastPanelState should handle exception gracefully`() {
        // GIVEN: getString() でSecurityExceptionが発生する
        every { sharedPreferences.getString(any(), any()) } throws SecurityException("Test exception")
        
        // WHEN: 状態取得を実行する
        val result = detailPreferencesManager.getLastPanelState()
        
        // THEN: デフォルト状態が返される
        assertEquals(SwipeableDetailPanel.PanelState.SHOWN, result)
    }

    @Test
    fun `clearCache should reset cache state`() {
        // GIVEN: キャッシュが設定されている
        detailPreferencesManager.saveLastPanelState(SwipeableDetailPanel.PanelState.HIDDEN)
        
        // WHEN: キャッシュをクリアする
        detailPreferencesManager.clearCache()
        
        // THEN: 次回の取得でSharedPreferencesから再読み込みされる
        every { sharedPreferences.getString("last_panel_state", null) } returns "SHOWN"
        val result = detailPreferencesManager.getLastPanelState()
        assertEquals(SwipeableDetailPanel.PanelState.SHOWN, result)
    }
}
