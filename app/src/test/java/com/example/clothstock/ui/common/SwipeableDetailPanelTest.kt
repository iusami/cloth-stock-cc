package com.example.clothstock.ui.common

import android.content.Context
import android.view.MotionEvent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * SwipeableDetailPanelのユニットテスト
 * 
 * Task 5.1: SwipeableDetailPanel 基本構造のユニットテスト作成
 * Task 5.2: SwipeableDetailPanel 基本構造の最小実装のテスト
 * Task 5.3: SwipeableDetailPanel 基本構造のリファクタリングのテスト
 * Task 6.1: SwipeableDetailPanel スワイプジェスチャー検出のユニットテスト作成
 * 
 * TDD Red-Green-Refactorサイクルに基づくテスト実装
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28]) // RobolectricでViewのインフレートを正しく動作させるため
class SwipeableDetailPanelTest {

    private lateinit var context: Context
    private lateinit var panel: SwipeableDetailPanel

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        panel = SwipeableDetailPanel(context)
    }

    // ===== Task 5.1 & 5.2 & 5.3: 基本構造のユニットテスト =====

    @Test
    fun `panel should be initialized with SHOWN state`() {
        // When
        val state = panel.getPanelState()
        
        // Then
        assertEquals(SwipeableDetailPanel.PanelState.SHOWN, state)
    }

    @Test
    fun `togglePanelState should switch between SHOWN and HIDDEN`() {
        // Given - 初期状態はSHOWN
        
        // When - 1回目の切り替え
        panel.togglePanelState()
        
        // Then
        assertEquals(SwipeableDetailPanel.PanelState.HIDDEN, panel.getPanelState())
        
        // When - 2回目の切り替え
        panel.togglePanelState()
        
        // Then
        assertEquals(SwipeableDetailPanel.PanelState.SHOWN, panel.getPanelState())
    }

    @Test
    fun `setPanelState should update panel state`() {
        // When
        panel.setPanelState(SwipeableDetailPanel.PanelState.HIDDEN)
        
        // Then
        assertEquals(SwipeableDetailPanel.PanelState.HIDDEN, panel.getPanelState())
        
        // When
        panel.setPanelState(SwipeableDetailPanel.PanelState.ANIMATING)
        
        // Then
        assertEquals(SwipeableDetailPanel.PanelState.ANIMATING, panel.getPanelState())
    }

    @Test
    fun `panel should have correct layout structure`() {
        // When
        panel.layout(0, 0, 100, 200)
        
        // Then
        assertEquals(100, panel.width)
        assertEquals(200, panel.height)
        assertTrue(panel is android.view.ViewGroup)
    }
    
    @Test
    fun `setPanelState should notify listener when state changes`() {
        // Given
        var notifiedState: SwipeableDetailPanel.PanelState? = null
        panel.onPanelStateChangedListener = { state ->
            notifiedState = state
        }
        
        // When
        panel.setPanelState(SwipeableDetailPanel.PanelState.HIDDEN)
        
        // Then
        assertEquals(SwipeableDetailPanel.PanelState.HIDDEN, notifiedState)
    }
    
    @Test
    fun `setPanelState should not notify listener when state does not change`() {
        // Given
        var notifyCount = 0
        panel.onPanelStateChangedListener = { 
            notifyCount++
        }
        
        // 初期状態はSHOWN
        
        // When
        panel.setPanelState(SwipeableDetailPanel.PanelState.SHOWN)
        
        // Then
        assertEquals(0, notifyCount) // 通知されないはず
    }
    
    // ===== Task 5.3: リファクタリング機能のテスト =====
    
    @Test
    fun `setPanelState with notifyListener false should not notify listener`() {
        // Given
        var notifiedState: SwipeableDetailPanel.PanelState? = null
        panel.onPanelStateChangedListener = { state ->
            notifiedState = state
        }
        
        // When
        panel.setPanelState(SwipeableDetailPanel.PanelState.HIDDEN, notifyListener = false)
        
        // Then
        assertEquals(SwipeableDetailPanel.PanelState.HIDDEN, panel.getPanelState())
        assertNull(notifiedState) // 通知されないはず
    }
    
    @Test
    fun `resetPanelState should reset to SHOWN and notify listener`() {
        // Given
        panel.setPanelState(SwipeableDetailPanel.PanelState.HIDDEN)
        var notifiedState: SwipeableDetailPanel.PanelState? = null
        panel.onPanelStateChangedListener = { state ->
            notifiedState = state
        }
        
        // When
        panel.resetPanelState()
        
        // Then
        assertEquals(SwipeableDetailPanel.PanelState.SHOWN, panel.getPanelState())
        assertEquals(SwipeableDetailPanel.PanelState.SHOWN, notifiedState)
    }
    
    @Test
    fun `togglePanelState should not change state when ANIMATING`() {
        // Given
        panel.setPanelState(SwipeableDetailPanel.PanelState.ANIMATING)
        var notifiedState: SwipeableDetailPanel.PanelState? = null
        panel.onPanelStateChangedListener = { state ->
            notifiedState = state
        }
        
        // When
        panel.togglePanelState()
        
        // Then
        assertEquals(SwipeableDetailPanel.PanelState.ANIMATING, panel.getPanelState())
        assertNull(notifiedState) // 通知されないはず
    }
    
    // ===== Task 6.1: スワイプジェスチャー検出のユニットテスト =====
    
    @Test
    fun `panel should handle swipe up gesture to hide panel`() {
        // Given
        panel.setPanelState(SwipeableDetailPanel.PanelState.SHOWN)
        panel.layout(0, 0, 100, 200)
        
        // When - 上スワイプジェスチャー（フリング）
        val downEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 50f, 150f, 0)
        val moveEvent = MotionEvent.obtain(0, 50, MotionEvent.ACTION_MOVE, 50f, 50f, 0)
        val upEvent = MotionEvent.obtain(0, 100, MotionEvent.ACTION_UP, 50f, 50f, 0)
        
        // Then - onTouchEventでイベントを処理（戻り値は実装に依存）
        panel.onTouchEvent(downEvent)
        panel.onTouchEvent(moveEvent)
        panel.onTouchEvent(upEvent)
        
        // 上スワイプによりパネルがHIDDENになるはず
        assertEquals(SwipeableDetailPanel.PanelState.HIDDEN, panel.getPanelState())
        
        downEvent.recycle()
        moveEvent.recycle()
        upEvent.recycle()
    }
    
    @Test
    fun `panel should handle swipe down gesture to show panel`() {
        // Given
        panel.setPanelState(SwipeableDetailPanel.PanelState.HIDDEN)
        panel.layout(0, 0, 100, 200)
        
        // When - 下スワイプジェスチャー（フリング）
        val downEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 50f, 50f, 0)
        val moveEvent = MotionEvent.obtain(0, 50, MotionEvent.ACTION_MOVE, 50f, 150f, 0)
        val upEvent = MotionEvent.obtain(0, 100, MotionEvent.ACTION_UP, 50f, 150f, 0)
        
        // Then - onTouchEventでイベントを処理（戻り値は実装に依存）
        panel.onTouchEvent(downEvent)
        panel.onTouchEvent(moveEvent)
        panel.onTouchEvent(upEvent)
        
        // 下スワイプによりパネルがSHOWNになるはず
        assertEquals(SwipeableDetailPanel.PanelState.SHOWN, panel.getPanelState())
        
        downEvent.recycle()
        moveEvent.recycle()
        upEvent.recycle()
    }
    
    @Test 
    fun `panel should detect minimum swipe threshold`() {
        // Given
        panel.setPanelState(SwipeableDetailPanel.PanelState.SHOWN)
        panel.layout(0, 0, 100, 200)
        
        // When - 閾値以下の短いスワイプ（5ピクセル移動、50ピクセル未満なので無視される）
        val downEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 50f, 100f, 0)
        val moveEvent = MotionEvent.obtain(0, 20, MotionEvent.ACTION_MOVE, 50f, 95f, 0)
        val upEvent = MotionEvent.obtain(0, 50, MotionEvent.ACTION_UP, 50f, 95f, 0)
        
        // Then - onTouchEventでイベントを処理、スワイプとしては認識されない
        panel.onTouchEvent(downEvent)
        panel.onTouchEvent(moveEvent)
        panel.onTouchEvent(upEvent)
        
        // 閾値以下なので状態は変わらないはず
        assertEquals(SwipeableDetailPanel.PanelState.SHOWN, panel.getPanelState())
        
        downEvent.recycle()
        moveEvent.recycle()
        upEvent.recycle()
    }
    
    @Test
    fun `panel should detect fling velocity for quick swipes`() {
        // Given
        panel.setPanelState(SwipeableDetailPanel.PanelState.SHOWN)
        panel.layout(0, 0, 100, 200)
        
        // When - 高速フリングスワイプ（短時間での移動 = 高速度）
        val downEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 50f, 150f, 0)
        val moveEvent = MotionEvent.obtain(0, 10, MotionEvent.ACTION_MOVE, 50f, 100f, 0) // 短時間で大移動
        val upEvent = MotionEvent.obtain(0, 20, MotionEvent.ACTION_UP, 50f, 100f, 0)
        
        // Then - onTouchEventでイベントを処理（戻り値は実装に依存）
        panel.onTouchEvent(downEvent)
        panel.onTouchEvent(moveEvent)
        panel.onTouchEvent(upEvent)
        
        // 高速スワイプによりパネルがHIDDENになるはず
        assertEquals(SwipeableDetailPanel.PanelState.HIDDEN, panel.getPanelState())
        
        downEvent.recycle()
        moveEvent.recycle()
        upEvent.recycle()
    }
    
    @Test
    fun `panel should resolve gesture conflicts with parent views`() {
        // Given
        panel.setPanelState(SwipeableDetailPanel.PanelState.SHOWN)
        panel.layout(0, 0, 100, 200)
        
        // When - 水平スワイプ（垂直ではないので無視される）
        val downEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 50f, 100f, 0)
        val moveEvent = MotionEvent.obtain(0, 50, MotionEvent.ACTION_MOVE, 150f, 100f, 0) // 水平移動
        val upEvent = MotionEvent.obtain(0, 100, MotionEvent.ACTION_UP, 150f, 100f, 0)
        
        // Then - onTouchEventでイベントを処理、水平スワイプは無視される
        panel.onTouchEvent(downEvent)
        panel.onTouchEvent(moveEvent)
        panel.onTouchEvent(upEvent)
        
        // 水平スワイプなので状態は変わらないはず（競合回避）
        assertEquals(SwipeableDetailPanel.PanelState.SHOWN, panel.getPanelState())
        
        downEvent.recycle()
        moveEvent.recycle()
        upEvent.recycle()
    }
}
