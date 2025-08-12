package com.example.clothstock.ui.common

import android.content.Context
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
}
