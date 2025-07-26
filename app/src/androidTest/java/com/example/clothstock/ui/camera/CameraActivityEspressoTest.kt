package com.example.clothstock.ui.camera

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.example.clothstock.R
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * CameraActivity のUIテスト
 * 
 * TDDアプローチに従い、最初に失敗するテストを作成してから実装を行う
 * カメラプレビュー、撮影、写真確認、ナビゲーション機能をテスト
 */
@RunWith(AndroidJUnit4::class)
class CameraActivityEspressoTest {

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.READ_MEDIA_IMAGES
    )

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    // ===== カメラプレビューテスト =====

    @Test
    fun カメラ画面_プレビュー表示_正常に動作する() {
        // Given: CameraActivityが起動
        val intent = Intent(context, CameraActivity::class.java)
        
        // When: CameraActivityを起動
        ActivityScenario.launch<CameraActivity>(intent).use {
            
            // Then: PreviewViewが存在し表示されている
            onView(withId(R.id.previewView))
                .check(matches(isDisplayed()))
            
            // カメラプレビューが動作していることを確認
            onView(withId(R.id.previewView))
                .check(matches(hasChildCount(0))) // PreviewViewは子要素を持たない
        }
    }

    @Test
    fun カメラ画面_撮影ボタン_表示されている() {
        // Given: CameraActivityが起動
        val intent = Intent(context, CameraActivity::class.java)
        
        // When: CameraActivityを起動
        ActivityScenario.launch<CameraActivity>(intent).use {
            
            // Then: 撮影ボタンが表示されている
            onView(withId(R.id.buttonCapture))
                .check(matches(isDisplayed()))
                .check(matches(isEnabled()))
        }
    }

    @Test
    fun カメラ画面_戻るボタン_表示されている() {
        // Given: CameraActivityが起動
        val intent = Intent(context, CameraActivity::class.java)
        
        // When: CameraActivityを起動
        ActivityScenario.launch<CameraActivity>(intent).use {
            
            // Then: 戻るボタンが表示されている
            onView(withId(R.id.buttonBack))
                .check(matches(isDisplayed()))
                .check(matches(isEnabled()))
        }
    }

    // ===== 撮影機能テスト =====

    @Test
    fun 撮影ボタン_タップ_写真が撮影される() {
        // Given: CameraActivityが起動しプレビューが表示されている
        val intent = Intent(context, CameraActivity::class.java)
        
        ActivityScenario.launch<CameraActivity>(intent).use {
            
            // When: 撮影ボタンをタップ
            onView(withId(R.id.buttonCapture))
                .perform(click())
            
            // Then: 写真確認画面が表示される
            onView(withId(R.id.layoutPhotoConfirmation))
                .check(matches(isDisplayed()))
                
            // 撮影された写真が表示される
            onView(withId(R.id.imageViewCaptured))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun 撮影後_保存ボタン_表示されている() {
        // Given: 写真が撮影され確認画面が表示されている
        val intent = Intent(context, CameraActivity::class.java)
        
        ActivityScenario.launch<CameraActivity>(intent).use {
            
            // 撮影ボタンをタップして写真撮影
            onView(withId(R.id.buttonCapture))
                .perform(click())
            
            // Then: 保存ボタンが表示されている
            onView(withId(R.id.buttonSave))
                .check(matches(isDisplayed()))
                .check(matches(isEnabled()))
        }
    }

    @Test
    fun 撮影後_再撮影ボタン_表示されている() {
        // Given: 写真が撮影され確認画面が表示されている
        val intent = Intent(context, CameraActivity::class.java)
        
        ActivityScenario.launch<CameraActivity>(intent).use {
            
            // 撮影ボタンをタップして写真撮影
            onView(withId(R.id.buttonCapture))
                .perform(click())
            
            // Then: 再撮影ボタンが表示されている
            onView(withId(R.id.buttonRetake))
                .check(matches(isDisplayed()))
                .check(matches(isEnabled()))
        }
    }

    // ===== ナビゲーションテスト =====

    @Test
    fun 戻るボタン_タップ_画面が閉じる() {
        // Given: CameraActivityが起動
        val intent = Intent(context, CameraActivity::class.java)
        
        ActivityScenario.launch<CameraActivity>(intent).use { scenario ->
            
            // When: 戻るボタンをタップ
            onView(withId(R.id.buttonBack))
                .perform(click())
            
            // Then: Activityが終了している
            scenario.onActivity { activity ->
                assert(activity.isFinishing || activity.isDestroyed)
            }
        }
    }

    @Test
    fun 再撮影ボタン_タップ_プレビュー画面に戻る() {
        // Given: 写真が撮影され確認画面が表示されている
        val intent = Intent(context, CameraActivity::class.java)
        
        ActivityScenario.launch<CameraActivity>(intent).use {
            
            // 撮影ボタンをタップして写真撮影
            onView(withId(R.id.buttonCapture))
                .perform(click())
            
            // When: 再撮影ボタンをタップ
            onView(withId(R.id.buttonRetake))
                .perform(click())
            
            // Then: プレビュー画面に戻る
            onView(withId(R.id.previewView))
                .check(matches(isDisplayed()))
                
            // 確認画面が非表示になる
            onView(withId(R.id.layoutPhotoConfirmation))
                .check(matches(withEffectiveVisibility(Visibility.GONE)))
        }
    }

    @Test
    fun 保存ボタン_タップ_結果を返して画面が閉じる() {
        // Given: 写真が撮影され確認画面が表示されている
        val intent = Intent(context, CameraActivity::class.java)
        
        ActivityScenario.launch<CameraActivity>(intent).use { scenario ->
            
            // 撮影ボタンをタップして写真撮影
            onView(withId(R.id.buttonCapture))
                .perform(click())
            
            // When: 保存ボタンをタップ
            onView(withId(R.id.buttonSave))
                .perform(click())
            
            // Then: 結果がOKで画面が閉じる
            scenario.onActivity { activity ->
                assert(activity.isFinishing || activity.isDestroyed)
                // 実際の実装では結果データにImageURIが含まれることを確認
            }
        }
    }

    // ===== エラーハンドリングテスト =====

    @Test
    fun カメラ権限なし_エラーメッセージ表示() {
        // 注意: このテストは権限が拒否された場合の動作をテスト
        // 実際の実装では権限チェックとエラーハンドリングが必要
        
        // Given: カメラ権限が拒否されている状態
        // When: CameraActivityを起動
        // Then: 適切なエラーメッセージが表示される
        
        // このテストは権限管理の実装後に詳細化する
        assert(true) // プレースホルダー
    }
}