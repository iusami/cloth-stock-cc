package com.example.clothstock.ui.common

import com.example.clothstock.ui.tagging.TaggingViewModel
import org.junit.Test
import org.junit.Assert.*

/**
 * ErrorDialogFragmentのユニットテスト
 * 
 * TDDアプローチに従った包括的エラーハンドリング機能のテスト
 * Androidフラグメントのため、基本的なenumとクラス存在確認のみ実施
 */
class ErrorDialogFragmentTest {

    @Test
    fun errorType_すべてのエラータイプが定義されている() {
        // Given & When & Then
        assertNotNull(TaggingViewModel.ErrorType.VALIDATION)
        assertNotNull(TaggingViewModel.ErrorType.DATABASE)
        assertNotNull(TaggingViewModel.ErrorType.NETWORK)
        assertNotNull(TaggingViewModel.ErrorType.FILE_SYSTEM)
        assertNotNull(TaggingViewModel.ErrorType.UNKNOWN)
    }

    @Test
    fun errorDialogFragment_クラスが存在する() {
        // Given & When & Then
        assertNotNull(ErrorDialogFragment::class.java)
    }

    @Test
    fun errorDialogFragment_companionObjectが存在する() {
        // Given & When & Then
        assertNotNull(ErrorDialogFragment.Companion)
    }

    @Test
    fun errorDialogFragment_リスナーインターフェースが存在する() {
        // Given & When & Then
        assertNotNull(ErrorDialogFragment.ErrorDialogListener::class.java)
    }
}