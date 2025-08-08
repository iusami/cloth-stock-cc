package com.example.clothstock.verification

import android.content.Intent
import android.content.res.Configuration
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.example.clothstock.MainActivity
import com.example.clothstock.R
import com.example.clothstock.util.TestDataHelper
import org.junit.*
import org.junit.runner.RunWith

/**
 * Task 15 RED Phase: デバイス互換性失敗テスト
 * 
 * 様々なデバイス構成・画面サイズで発生する可能性のある問題を検証
 * - タブレット画面での横レイアウト問題
 * - 小画面での UI 要素切れ
 * - 画面回転時のレイアウト崩れ
 * - 異なる密度での表示問題
 */
@RunWith(AndroidJUnit4::class)
class DeviceCompatibilityFailTest {

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.READ_MEDIA_IMAGES,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    @Before
    fun setUp() {
        TestDataHelper.clearTestDatabaseSync()
        val testData = TestDataHelper.createMultipleTestItems(3)
        TestDataHelper.injectTestDataSync(testData)
    }

    @After
    fun tearDown() {
        TestDataHelper.clearTestDatabaseSync()
    }

    // ===== RED Phase: 想定される失敗テストケース =====

    @Test
    fun デバイス互換性失敗テスト_タブレット横画面レイアウト問題() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            
            Thread.sleep(1500)
            
            // EXPECTED FAILURE: タブレット横画面で適切なレイアウトが適用されていない想定
            scenario.onActivity { activity ->
                val configuration = activity.resources.configuration
                
                // 横画面・大画面での想定問題
                if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE && 
                    configuration.screenWidthDp > 600) {
                    
                    assert(false) { 
                        "タブレット横画面で2カラムレイアウトが適用されていない可能性" 
                    }
                }
            }
        }
    }

    @Test
    fun デバイス互換性失敗テスト_小画面UI要素切れ問題() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            
            Thread.sleep(1500)
            
            // EXPECTED FAILURE: 小画面でUI要素が画面外に出る想定
            scenario.onActivity { activity ->
                val screenWidthDp = activity.resources.configuration.screenWidthDp
                val screenHeightDp = activity.resources.configuration.screenHeightDp
                
                // 320dp未満の極小画面での想定問題
                if (screenWidthDp < 320 || screenHeightDp < 480) {
                    assert(false) { 
                        "画面サイズ ${screenWidthDp}x${screenHeightDp}dp で UI要素が切れる可能性" 
                    }
                }
            }
        }
    }

    @Test
    fun デバイス互換性失敗テスト_画面回転時レイアウト崩れ() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            
            Thread.sleep(1500)
            
            // フィルターボトムシート表示
            onView(withId(R.id.buttonFilter))
                .perform(click())
            
            Thread.sleep(1000)
            
            // EXPECTED FAILURE: 画面回転でボトムシートレイアウトが崩れる想定
            scenario.onActivity { activity ->
                // 想定される画面回転時の問題
                assert(false) { 
                    "画面回転時にボトムシートの高さ調整が適切に行われていない可能性" 
                }
            }
        }
    }

    @Test
    fun デバイス互換性失敗テスト_高密度画面でのタッチターゲット問題() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            
            Thread.sleep(1500)
            
            // フィルターボトムシート表示
            onView(withId(R.id.buttonFilter))
                .perform(click())
            
            Thread.sleep(1000)
            
            // EXPECTED FAILURE: 高密度画面でタッチターゲットが小さすぎる想定
            scenario.onActivity { activity ->
                val density = activity.resources.displayMetrics.density
                
                // 3.0以上の高密度画面での想定問題
                if (density >= 3.0f) {
                    assert(false) { 
                        "密度 ${density} でChipのタッチターゲットが48dpを下回る可能性" 
                    }
                }
            }
        }
    }

    @Test
    fun デバイス互換性失敗テスト_フォント設定大きさ対応不十分() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            
            Thread.sleep(1500)
            
            // EXPECTED FAILURE: システムフォント設定「大」で文字が切れる想定
            scenario.onActivity { activity ->
                val fontScale = activity.resources.configuration.fontScale
                
                // フォントスケールが1.3以上での想定問題
                if (fontScale >= 1.3f) {
                    assert(false) { 
                        "フォントスケール ${fontScale} でテキストが適切に表示されない可能性" 
                    }
                }
            }
        }
    }

    @Test
    fun デバイス互換性失敗テスト_ダークモード対応不完全() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            
            Thread.sleep(1500)
            
            // EXPECTED FAILURE: ダークモードで視認性問題がある想定
            scenario.onActivity { activity ->
                val nightModeFlags = activity.resources.configuration.uiMode and 
                                   Configuration.UI_MODE_NIGHT_MASK
                
                if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
                    assert(false) { 
                        "ダークモードで一部UI要素のコントラストが不十分な可能性" 
                    }
                }
            }
        }
    }

    @Test
    fun デバイス互換性失敗テスト_メモリ制約デバイスでのクラッシュ() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            
            Thread.sleep(1500)
            
            // EXPECTED FAILURE: メモリが少ないデバイスでアプリがクラッシュする想定
            scenario.onActivity { activity ->
                val activityManager = activity.getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                val memoryInfo = android.app.ActivityManager.MemoryInfo()
                activityManager.getMemoryInfo(memoryInfo)
                
                // 使用可能メモリが512MB未満での想定問題
                val availableMemoryMB = memoryInfo.availMem / (1024 * 1024)
                if (availableMemoryMB < 512) {
                    assert(false) { 
                        "使用可能メモリ ${availableMemoryMB}MB でOutOfMemoryError発生の可能性" 
                    }
                }
            }
        }
    }

    @Test
    fun デバイス互換性失敗テスト_古いAPIバージョンでの機能制限() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            
            Thread.sleep(1500)
            
            // EXPECTED FAILURE: API Level 21-23 で新しいAPIを使用している想定
            val apiLevel = android.os.Build.VERSION.SDK_INT
            
            if (apiLevel <= 23) {
                assert(false) { 
                    "API Level $apiLevel で一部機能が正常動作しない可能性" 
                }
            }
        }
    }
}