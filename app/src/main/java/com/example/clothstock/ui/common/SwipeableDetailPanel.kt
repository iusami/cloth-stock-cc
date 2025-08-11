package com.example.clothstock.ui.common

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout

/**
 * スワイプ可能詳細パネル
 * 
 * TDD Green フェーズ - 最小実装
 * SwipeHandleView のテストを通すための基本クラス
 */
class SwipeableDetailPanel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    /**
     * パネルの状態定義
     */
    enum class PanelState {
        SHOWN,      // パネル表示状態
        HIDDEN,     // パネル非表示状態  
        ANIMATING   // アニメーション中
    }

    private var panelState: PanelState = PanelState.SHOWN

    /**
     * パネル状態を切り替える（最小実装）
     * SwipeHandleView のテスト用
     */
    fun togglePanelState() {
        // 最小実装: 基本的な状態切り替えのみ
        panelState = when (panelState) {
            PanelState.SHOWN -> PanelState.HIDDEN
            PanelState.HIDDEN -> PanelState.SHOWN  
            PanelState.ANIMATING -> panelState // アニメーション中は変更しない
        }
    }

    /**
     * 現在のパネル状態を取得
     */
    fun getPanelState(): PanelState = panelState
}
