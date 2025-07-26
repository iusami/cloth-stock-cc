package com.example.clothstock.ui.camera

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
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
 * 
 * Note: Android固有のクラス（ContextCompat等）はインストルメンテーションテストで検証
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

    // ===== 初期状態テスト =====

    @Test
    fun `初期状態_全ての状態がfalse`() {
        // When & Then
        assertFalse("初期状態では権限状態がfalse", permissionManager.isPermissionGranted())
        assertFalse("初期状態では永続拒否状態がfalse", permissionManager.isPermanentlyDenied())
    }

    // ===== コールバック処理テスト =====

    @Test
    fun `PermissionManager_正常に作成される`() {
        // When & Then
        assertNotNull("PermissionManagerが作成されるべき", permissionManager)
    }
}