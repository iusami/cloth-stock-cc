package com.example.clothstock.ui.camera

import android.content.Context
import android.content.Intent
import android.provider.Settings
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import org.junit.Assert.*

/**
 * PermissionRationaleDialog のユニットテスト
 * 
 * TDD アプローチで権限説明ダイアログの動作をテスト
 * ユーザーアクションに対するコールバック処理を検証
 */
@RunWith(MockitoJUnitRunner::class)
class PermissionRationaleDialogTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockOnPermissionGranted: () -> Unit

    @Mock
    private lateinit var mockOnPermissionDenied: () -> Unit

    @Mock
    private lateinit var mockOnSettingsRequested: () -> Unit

    private lateinit var dialogController: PermissionRationaleDialog.Controller

    @Before
    fun setUp() {
        dialogController = PermissionRationaleDialog.Controller(
            context = mockContext,
            onPermissionGranted = mockOnPermissionGranted,
            onPermissionDenied = mockOnPermissionDenied,
            onSettingsRequested = mockOnSettingsRequested
        )
    }

    // ===== ダイアログ表示テスト =====

    @Test
    fun `showRationaleDialog_初回表示_適切なメッセージとボタン`() {
        // When
        val dialogInfo = dialogController.buildRationaleDialogInfo(isFirstTime = true)

        // Then
        assertNotNull("ダイアログ情報が作成されるべき", dialogInfo)
        assertTrue("初回表示では「許可」ボタンが表示されるべき", dialogInfo.showAllowButton)
        assertFalse("初回表示では「設定」ボタンは非表示", dialogInfo.showSettingsButton)
        assertTrue("タイトルが設定されるべき", dialogInfo.title.isNotEmpty())
        assertTrue("メッセージが設定されるべき", dialogInfo.message.isNotEmpty())
    }

    @Test
    fun `showRationaleDialog_永続拒否後_設定ボタン表示`() {
        // When
        val dialogInfo = dialogController.buildRationaleDialogInfo(isFirstTime = false)

        // Then
        assertNotNull("ダイアログ情報が作成されるべき", dialogInfo)
        assertFalse("永続拒否後は「許可」ボタンは非表示", dialogInfo.showAllowButton)
        assertTrue("永続拒否後は「設定」ボタンが表示されるべき", dialogInfo.showSettingsButton)
        assertTrue("設定用メッセージが含まれるべき", 
            dialogInfo.message.contains("設定") || dialogInfo.message.contains("アプリ情報"))
    }

    // ===== ユーザーアクション処理テスト =====

    @Test
    fun `handleAllowButtonClick_許可コールバック呼び出し`() {
        // When
        dialogController.handleAllowButtonClick()

        // Then
        verify(mockOnPermissionGranted).invoke()
        verifyNoInteractions(mockOnPermissionDenied, mockOnSettingsRequested)
    }

    @Test
    fun `handleDenyButtonClick_拒否コールバック呼び出し`() {
        // When
        dialogController.handleDenyButtonClick()

        // Then
        verify(mockOnPermissionDenied).invoke()
        verifyNoInteractions(mockOnPermissionGranted, mockOnSettingsRequested)
    }

    @Test
    fun `handleSettingsButtonClick_設定コールバック呼び出し`() {
        // When
        dialogController.handleSettingsButtonClick()

        // Then
        verify(mockOnSettingsRequested).invoke()
        verifyNoInteractions(mockOnPermissionGranted, mockOnPermissionDenied)
    }

    // ===== 設定画面遷移テスト =====

    @Test
    fun `createSettingsIntent_適切なインテント作成`() {
        // Given
        val packageName = "com.example.clothstock"
        `when`(mockContext.packageName).thenReturn(packageName)

        // When
        val intent = dialogController.createSettingsIntent()

        // Then
        assertNotNull("設定画面用インテントが作成されるべき", intent)
        assertEquals("設定画面アクションが正しい", Settings.ACTION_APPLICATION_DETAILS_SETTINGS, intent.action)
        assertTrue("パッケージURIが含まれるべき", intent.data?.toString()?.contains(packageName) == true)
    }

    @Test
    fun `isValidSettingsIntent_有効なインテント_trueを返す`() {
        // Given
        val validIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        validIntent.data = android.net.Uri.parse("package:com.example.clothstock")

        // When
        val isValid = dialogController.isValidSettingsIntent(validIntent)

        // Then
        assertTrue("有効な設定インテントはtrueを返すべき", isValid)
    }

    @Test
    fun `isValidSettingsIntent_無効なインテント_falseを返す`() {
        // Given
        val invalidIntent = Intent()

        // When
        val isValid = dialogController.isValidSettingsIntent(invalidIntent)

        // Then
        assertFalse("無効なインテントはfalseを返すべき", isValid)
    }

    // ===== メッセージローカライゼーションテスト =====

    @Test
    fun `getDialogMessages_初回用メッセージ_適切な内容`() {
        // When
        val messages = dialogController.getDialogMessages(isFirstTime = true)

        // Then
        assertNotNull("初回用メッセージが取得できるべき", messages)
        assertTrue("カメラに関する説明が含まれるべき", 
            messages.message.contains("カメラ") || messages.message.contains("撮影"))
        assertTrue("アプリ機能の説明が含まれるべき", 
            messages.message.contains("写真") || messages.message.contains("衣服"))
    }

    @Test
    fun `getDialogMessages_設定用メッセージ_適切な内容`() {
        // When
        val messages = dialogController.getDialogMessages(isFirstTime = false)

        // Then
        assertNotNull("設定用メッセージが取得できるべき", messages)
        assertTrue("設定手順の説明が含まれるべき", 
            messages.message.contains("設定") || messages.message.contains("権限"))
        assertTrue("手動操作の案内が含まれるべき", 
            messages.message.contains("手動") || messages.message.contains("有効"))
    }

    // ===== エラーハンドリングテスト =====

    @Test
    fun `handleException_例外発生時_適切にハンドリング`() {
        // Given
        val exception = RuntimeException("テストエラー")

        // When & Then - 例外が発生してもクラッシュしない
        try {
            dialogController.handleException(exception)
            // 例外が適切にハンドリングされていることを確認
        } catch (e: Exception) {
            fail("例外が適切にハンドリングされていない: ${e.message}")
        }
    }
}