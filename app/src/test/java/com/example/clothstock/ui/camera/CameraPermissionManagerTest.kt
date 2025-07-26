package com.example.clothstock.ui.camera

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import org.junit.Assert.*

/**
 * CameraPermissionManager のユニットテスト
 * 
 * TDD アプローチに従い、まず失敗するテストを作成してから実装を行う
 * カメラ権限の状態管理とダイアログハンドリングのテスト
 */
@RunWith(MockitoJUnitRunner::class)
class CameraPermissionManagerTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockPermissionLauncher: ActivityResultLauncher<String>

    @Mock
    private lateinit var mockRationaleCallback: () -> Unit

    private lateinit var permissionManager: CameraPermissionManager

    @Before
    fun setUp() {
        permissionManager = CameraPermissionManager(
            context = mockContext,
            permissionLauncher = mockPermissionLauncher,
            onPermissionRationaleNeeded = mockRationaleCallback
        )
    }

    // ===== 権限チェックテスト =====

    @Test
    fun `checkCameraPermission_権限が許可済み_trueを返す`() {
        // Given
        `when`(ContextCompat.checkSelfPermission(mockContext, android.Manifest.permission.CAMERA))
            .thenReturn(PackageManager.PERMISSION_GRANTED)

        // When
        val result = permissionManager.checkCameraPermission()

        // Then
        assertTrue("カメラ権限が許可済みの場合はtrueを返すべき", result)
    }

    @Test
    fun `checkCameraPermission_権限が拒否済み_falseを返す`() {
        // Given
        `when`(ContextCompat.checkSelfPermission(mockContext, android.Manifest.permission.CAMERA))
            .thenReturn(PackageManager.PERMISSION_DENIED)

        // When
        val result = permissionManager.checkCameraPermission()

        // Then
        assertFalse("カメラ権限が拒否済みの場合はfalseを返すべき", result)
    }

    // ===== 権限リクエストテスト =====

    @Test
    fun `requestCameraPermission_権限が未許可_ランチャーを呼び出す`() {
        // Given
        `when`(ContextCompat.checkSelfPermission(mockContext, android.Manifest.permission.CAMERA))
            .thenReturn(PackageManager.PERMISSION_DENIED)

        // When
        permissionManager.requestCameraPermission()

        // Then
        verify(mockPermissionLauncher).launch(android.Manifest.permission.CAMERA)
    }

    @Test
    fun `requestCameraPermission_権限が許可済み_ランチャーを呼び出さない`() {
        // Given
        `when`(ContextCompat.checkSelfPermission(mockContext, android.Manifest.permission.CAMERA))
            .thenReturn(PackageManager.PERMISSION_GRANTED)

        // When
        permissionManager.requestCameraPermission()

        // Then
        verify(mockPermissionLauncher, never()).launch(any())
    }

    // ===== 権限結果処理テスト =====

    @Test
    fun `handlePermissionResult_許可された_適切な状態更新`() {
        // When
        permissionManager.handlePermissionResult(isGranted = true)

        // Then
        assertTrue("権限許可後は権限状態がtrueになるべき", permissionManager.isPermissionGranted())
        assertFalse("権限許可後はdeny状態がfalseになるべき", permissionManager.isPermanentlyDenied())
    }

    @Test
    fun `handlePermissionResult_拒否された_適切な状態更新`() {
        // When
        permissionManager.handlePermissionResult(isGranted = false)

        // Then
        assertFalse("権限拒否後は権限状態がfalseになるべき", permissionManager.isPermissionGranted())
    }

    // ===== 権限説明ダイアログテスト =====

    @Test
    fun `shouldShowRationale_初回拒否_説明が必要`() {
        // Given
        permissionManager.handlePermissionResult(isGranted = false)

        // When
        val shouldShow = permissionManager.shouldShowRationale()

        // Then
        assertTrue("初回権限拒否では説明ダイアログが必要", shouldShow)
    }

    @Test
    fun `shouldShowRationale_永続拒否状態_説明不要`() {
        // Given
        permissionManager.handlePermissionResult(isGranted = false)
        permissionManager.markAsPermanentlyDenied()

        // When
        val shouldShow = permissionManager.shouldShowRationale()

        // Then
        assertFalse("永続拒否状態では説明ダイアログは不要", shouldShow)
    }

    // ===== 永続拒否状態テスト =====

    @Test
    fun `markAsPermanentlyDenied_状態が正しく設定される`() {
        // When
        permissionManager.markAsPermanentlyDenied()

        // Then
        assertTrue("永続拒否フラグが正しく設定されるべき", permissionManager.isPermanentlyDenied())
    }

    @Test
    fun `isPermanentlyDenied_初期状態_false`() {
        // When & Then
        assertFalse("初期状態では永続拒否はfalse", permissionManager.isPermanentlyDenied())
    }

    // ===== エラーハンドリングテスト =====

    @Test
    fun `requestCameraPermission_例外発生_適切にハンドリング`() {
        // Given
        `when`(ContextCompat.checkSelfPermission(mockContext, android.Manifest.permission.CAMERA))
            .thenThrow(RuntimeException("権限チェックエラー"))

        // When & Then - 例外が発生してもクラッシュしない
        try {
            permissionManager.requestCameraPermission()
            // 例外がキャッチされていることを確認
        } catch (e: Exception) {
            fail("例外が適切にハンドリングされていない: ${e.message}")
        }
    }

    // ===== 状態リセットテスト =====

    @Test
    fun `resetState_全ての状態がリセットされる`() {
        // Given
        permissionManager.handlePermissionResult(isGranted = false)
        permissionManager.markAsPermanentlyDenied()

        // When
        permissionManager.resetState()

        // Then
        assertFalse("リセット後は権限状態がfalse", permissionManager.isPermissionGranted())
        assertFalse("リセット後は永続拒否状態がfalse", permissionManager.isPermanentlyDenied())
    }
}