package com.example.clothstock.integration.deletion

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.example.clothstock.MainActivity
import com.example.clothstock.R
import com.example.clothstock.integration.deletion.helper.DeletionTestHelper
import com.example.clothstock.util.IdlingResourceHelper
import org.hamcrest.Matchers.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Task 10 - RED Phase 1: 削除ワークフロー統合テスト
 * 
 * End-to-Endの削除ワークフロー統合テスト:
 * - 選択モード起動から削除完了までの完全フロー
 * - UI連携、データ整合性、エラーハンドリングの統合検証
 * - Requirements 1.1-5.5 の全要件検証
 * 
 * 初期状態: 全テスト失敗（統合機能未実装のため）
 */
@RunWith(AndroidJUnit4::class)
class DeletionWorkflowIntegrationTest {

    companion object {
        private const val INITIAL_UI_WAIT_MS = 1500L
        private const val DELETION_WAIT_MS = 2000L
        private const val BATCH_DELETION_WAIT_MS = 3000L
        private const val UI_TRANSITION_WAIT_MS = 500L
        private const val MIN_GALLERY_ITEMS = 3
        private const val BATCH_TEST_ITEMS = 5
        private const val LARGE_TEST_ITEMS = 10
        private const val DIVERSE_TEST_ITEMS = 8
        private const val EXPECTED_ITEMS_AFTER_SINGLE_DELETE = 2
        private const val EXPECTED_ITEMS_AFTER_BATCH_DELETE = 2
        private const val EXPECTED_ITEMS_AFTER_LARGE_DELETE = 4
        private const val EXPECTED_ITEMS_AFTER_DIVERSE_DELETE = 5
        private const val SELECTION_COUNT_THREE = 3
        private const val SELECTION_COUNT_SIX = 6
    }

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.READ_MEDIA_IMAGES,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    @Before
    fun setUp() {
        // RED Phase: テストヘルパーがまだ存在しないため失敗する
        DeletionTestHelper.setupDeletionTestEnvironment()
        DeletionTestHelper.clearAllTestData()
    }

    @After
    fun tearDown() {
        // RED Phase: クリーンアップヘルパーも存在しない
        IdlingResourceHelper.unregisterAllIdlingResources()
        DeletionTestHelper.cleanupDeletionTestEnvironment()
    }

    // ===== Requirements 1.1-1.5: Selection Mode & Multi-Selection Tests =====

    @Test
    fun `完全削除ワークフロー_単一アイテム選択から削除完了まで`() {
        // Given: テストデータを準備（複数のClothItem）
        val testItems = DeletionTestHelper.createTestItemsWithFiles(3)
        DeletionTestHelper.injectTestItems(testItems)

        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->

            // Step 1: ギャラリー表示確認（Requirements 1.1）
            IdlingResourceHelper.waitForRecyclerView(
                R.id.recyclerViewGallery,
                minItemCount = MIN_GALLERY_ITEMS,
                timeout = INITIAL_UI_WAIT_MS
            )
            onView(withId(R.id.recyclerViewGallery))
                .check(matches(isDisplayed()))
                .check(matches(hasMinimumChildCount(MIN_GALLERY_ITEMS)))

            // Step 2: 長押しで選択モード起動（Requirements 1.2）
            // RED: 長押しによる選択モード機能が統合されていないため失敗
            onView(withId(R.id.recyclerViewGallery))
                .perform(RecyclerViewActions.actionOnItemAtPosition<androidx.recyclerview.widget.RecyclerView.ViewHolder>(0, longClick()))

            // RED: 選択モードUI（チェックボックス、ハイライト）が表示されないため失敗
            onView(withId(R.id.checkboxSelection))
                .check(matches(isDisplayed()))

            // Step 3: 削除ボタンがアクションバーに表示（Requirements 1.3）
            // RED: 削除ボタンのUI統合が未実装のため失敗
            onView(withId(R.id.actionDelete))
                .check(matches(isDisplayed()))
                .check(matches(isEnabled()))

            // Step 4: 削除ボタンをタップ（Requirements 2.1）
            onView(withId(R.id.actionDelete))
                .perform(click())

            // Step 5: 削除確認ダイアログ表示（Requirements 2.1-2.2）
            // RED: 確認ダイアログの統合が未実装のため失敗
            onView(withId(R.id.dialogDeleteConfirmation))
                .check(matches(isDisplayed()))

            onView(withText(containsString("1個のアイテム")))
                .check(matches(isDisplayed()))

            // Step 6: 削除確認（Requirements 2.3）
            onView(withId(R.id.buttonConfirmDelete))
                .perform(click())

            // Step 7: 削除処理実行とプログレス表示（Requirements 4.1-4.2）
            // RED: プログレス表示の統合が未実装のため失敗
            onView(withId(R.id.progressDeletion))
                .check(matches(isDisplayed()))

            // Step 8: 削除完了後のギャラリー更新確認（Requirements 3.3）
            IdlingResourceHelper.waitForRecyclerView(
                R.id.recyclerViewGallery,
                minItemCount = EXPECTED_ITEMS_AFTER_SINGLE_DELETE,
                timeout = DELETION_WAIT_MS
            )
            // RED: ギャラリーの自動更新統合が未実装のため失敗
            onView(withId(R.id.recyclerViewGallery))
                .check(matches(hasChildCount(EXPECTED_ITEMS_AFTER_SINGLE_DELETE))) // 3-1=2

            // Step 9: 削除成功フィードバック（Requirements 4.3）
            // RED: 成功メッセージ表示統合が未実装のため失敗
            onView(withText(containsString("削除が完了しました")))
                .check(matches(isDisplayed()))

            // Step 10: 選択モード自動終了（Requirements 4.4）
            // RED: 自動モード終了が統合されていないため失敗
            onView(withId(R.id.actionDelete))
                .check(doesNotExist())
        }
    }

    @Test
    fun `完全削除ワークフロー_複数アイテム選択バッチ削除処理`() {
        // Given: 複数テストデータ
        val testItems = DeletionTestHelper.createTestItemsWithFiles(BATCH_TEST_ITEMS)
        DeletionTestHelper.injectTestItems(testItems)

        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->

            IdlingResourceHelper.waitForRecyclerView(
                R.id.recyclerViewGallery,
                minItemCount = 5,
                timeout = INITIAL_UI_WAIT_MS
            )

            // Step 1: 第一アイテム長押し選択
            // RED: マルチ選択統合が未実装のため失敗
            onView(withId(R.id.recyclerViewGallery))
                .perform(RecyclerViewActions.actionOnItemAtPosition<androidx.recyclerview.widget.RecyclerView.ViewHolder>(0, longClick()))

            // Step 2: 追加アイテム選択（Requirements 1.4）
            // RED: 追加選択の統合UI機能が未実装のため失敗
            onView(withId(R.id.recyclerViewGallery))
                .perform(RecyclerViewActions.actionOnItemAtPosition<androidx.recyclerview.widget.RecyclerView.ViewHolder>(1, click()))

            onView(withId(R.id.recyclerViewGallery))
                .perform(RecyclerViewActions.actionOnItemAtPosition<androidx.recyclerview.widget.RecyclerView.ViewHolder>(2, click()))

            // Step 3: 選択カウント表示確認
            // RED: 選択カウント表示統合が未実装のため失敗
            onView(withId(R.id.textSelectionCount))
                .check(matches(withText("${SELECTION_COUNT_THREE}個のアイテムが選択されています")))

            // Step 4: バッチ削除実行
            onView(withId(R.id.actionDelete))
                .perform(click())

            // Step 5: バッチ削除確認ダイアログ（Requirements 2.2）
            // RED: バッチ削除ダイアログ統合が未実装のため失敗
            onView(withText(containsString("${SELECTION_COUNT_THREE}個のアイテム")))
                .check(matches(isDisplayed()))

            onView(withId(R.id.buttonConfirmDelete))
                .perform(click())

            // Step 6: バッチ削除プログレス（Requirements 4.2）
            // RED: バッチ削除プログレス統合が未実装のため失敗
            onView(withId(R.id.textDeletionProgress))
                .check(matches(withText(containsString("削除中"))))

            IdlingResourceHelper.waitForRecyclerView(
                R.id.recyclerViewGallery,
                minItemCount = EXPECTED_ITEMS_AFTER_BATCH_DELETE,
                timeout = BATCH_DELETION_WAIT_MS
            )

            // Step 7: バッチ削除完了確認
            // RED: バッチ削除後のギャラリー更新統合が未実装のため失敗
            onView(withId(R.id.recyclerViewGallery))
                .check(matches(hasChildCount(EXPECTED_ITEMS_AFTER_BATCH_DELETE))) // 5-3=2
        }
    }

    @Test
    fun `削除確認ダイアログ統合テスト_キャンセル処理フロー`() {
        // Given: テストデータ準備
        val testItems = DeletionTestHelper.createTestItemsWithFiles(2)
        DeletionTestHelper.injectTestItems(testItems)

        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->

            IdlingResourceHelper.waitForRecyclerView(
                R.id.recyclerViewGallery,
                minItemCount = 5,
                timeout = INITIAL_UI_WAIT_MS
            )

            // Step 1: アイテム選択
            // RED: 選択機能の統合が未実装のため失敗
            onView(withId(R.id.recyclerViewGallery))
                .perform(RecyclerViewActions.actionOnItemAtPosition<androidx.recyclerview.widget.RecyclerView.ViewHolder>(0, longClick()))

            // Step 2: 削除ボタンタップ
            onView(withId(R.id.actionDelete))
                .perform(click())

            // Step 3: キャンセルボタンタップ（Requirements 2.4）
            // RED: キャンセル処理統合が未実装のため失敗
            onView(withId(R.id.buttonCancelDelete))
                .perform(click())

            // Step 4: ダイアログクローズ確認
            onView(withId(R.id.dialogDeleteConfirmation))
                .check(doesNotExist())

            // Step 5: 選択状態維持確認（Requirements 2.4）
            // RED: キャンセル後の選択状態維持統合が未実装のため失敗
            onView(withId(R.id.checkboxSelection))
                .check(matches(isChecked()))

            // Step 6: データ変更なし確認
            onView(withId(R.id.recyclerViewGallery))
                .check(matches(hasChildCount(EXPECTED_ITEMS_AFTER_SINGLE_DELETE))) // 変更なし
        }
    }

    @Test
    fun `削除後のギャラリー更新統合テスト_データ同期確認`() {
        // Given: 特定IDのテストデータ
        val testItems = DeletionTestHelper.createTestItemsWithSpecificIds(listOf(100L, 200L, 300L))
        DeletionTestHelper.injectTestItems(testItems)

        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->

            IdlingResourceHelper.waitForRecyclerView(
                R.id.recyclerViewGallery,
                minItemCount = 5,
                timeout = INITIAL_UI_WAIT_MS
            )

            // Step 1: 特定アイテム選択・削除
            // RED: ID指定削除統合が未実装のため失敗
            onView(withId(R.id.recyclerViewGallery))
                .perform(RecyclerViewActions.actionOnItemAtPosition<androidx.recyclerview.widget.RecyclerView.ViewHolder>(1, longClick())) // ID=200L

            onView(withId(R.id.actionDelete))
                .perform(click())

            onView(withId(R.id.buttonConfirmDelete))
                .perform(click())

            IdlingResourceHelper.waitForRecyclerView(
                R.id.recyclerViewGallery,
                minItemCount = EXPECTED_ITEMS_AFTER_SINGLE_DELETE,
                timeout = DELETION_WAIT_MS
            )

            // Step 2: データベース同期確認
            // RED: データベース同期検証統合が未実装のため失敗
            DeletionTestHelper.verifyItemDeletedFromDatabase(200L)

            // Step 3: ファイルシステム同期確認（Requirements 3.2）
            // RED: ファイルシステム同期検証統合が未実装のため失敗
            DeletionTestHelper.verifyImageFileDeleted(testItems[1].imagePath)

            // Step 4: ギャラリーUI更新確認（Requirements 3.3）
            onView(withId(R.id.recyclerViewGallery))
                .check(matches(hasChildCount(2)))

            // Step 5: 残存アイテムの整合性確認
            DeletionTestHelper.verifyRemainingItems(listOf(100L, 300L))
        }
    }

    @Test
    fun `選択モード終了統合テスト_バックボタン処理`() {
        // Given: テストデータ
        val testItems = DeletionTestHelper.createTestItemsWithFiles(3)
        DeletionTestHelper.injectTestItems(testItems)

        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->

            IdlingResourceHelper.waitForRecyclerView(
                R.id.recyclerViewGallery,
                minItemCount = 5,
                timeout = INITIAL_UI_WAIT_MS
            )

            // Step 1: 選択モード開始
            // RED: 選択モード統合が未実装のため失敗
            onView(withId(R.id.recyclerViewGallery))
                .perform(RecyclerViewActions.actionOnItemAtPosition<androidx.recyclerview.widget.RecyclerView.ViewHolder>(0, longClick()))

            // Step 2: バックボタン押下（Requirements 1.5）
            // RED: バックボタンによる選択モード終了統合が未実装のため失敗
            onView(isRoot()).perform(pressBack())

            // Step 3: 選択モード終了確認
            onView(withId(R.id.actionDelete))
                .check(doesNotExist())

            // Step 4: 選択状態クリア確認（Requirements 1.5）
            // RED: 選択状態クリア統合が未実装のため失敗
            onView(withId(R.id.checkboxSelection))
                .check(doesNotExist())

            // Step 5: 通常モードに戻る確認
            onView(withId(R.id.recyclerViewGallery))
                .check(matches(isDisplayed()))
                .check(matches(hasChildCount(3)))
        }
    }

    @Test
    fun `削除処理中の状態管理統合テスト_UIブロッキング`() {
        // Given: 削除処理に時間がかかるテストデータ
        val testItems = DeletionTestHelper.createLargeTestItemsWithFiles(LARGE_TEST_ITEMS)
        DeletionTestHelper.injectTestItems(testItems)

        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->

            IdlingResourceHelper.waitForRecyclerView(
                R.id.recyclerViewGallery,
                minItemCount = 5,
                timeout = INITIAL_UI_WAIT_MS
            )

            // Step 1: 複数アイテム選択
            // RED: 大量アイテム選択統合が未実装のため失敗
            onView(withId(R.id.recyclerViewGallery))
                .perform(RecyclerViewActions.actionOnItemAtPosition<androidx.recyclerview.widget.RecyclerView.ViewHolder>(0, longClick()))

            repeat(SELECTION_COUNT_SIX - 1) { index ->
                onView(withId(R.id.recyclerViewGallery))
                    .perform(RecyclerViewActions.actionOnItemAtPosition<androidx.recyclerview.widget.RecyclerView.ViewHolder>(index + 1, click()))
            }

            // Step 2: 削除開始
            onView(withId(R.id.actionDelete))
                .perform(click())

            onView(withId(R.id.buttonConfirmDelete))
                .perform(click())

            // Step 3: 削除処理中のUIブロッキング確認（Requirements 4.5）
            // RED: 削除中のUIブロッキング統合が未実装のため失敗
            IdlingResourceHelper.waitForUiUpdate(UI_TRANSITION_WAIT_MS) // 削除処理開始直後

            onView(withId(R.id.recyclerViewGallery))
                .check(matches(not(isClickable())))

            // Step 4: プログレス情報表示確認（Requirements 4.2）
            // RED: プログレス情報統合が未実装のため失敗
            onView(withId(R.id.textDeletionProgress))
                .check(matches(isDisplayed()))
                .check(matches(withText(matchesRegex("削除中.*[0-9]+.*個"))))

            IdlingResourceHelper.waitFor(3000) // 削除処理完了待機

            // Step 5: 処理完了後のUIアクティブ化確認
            onView(withId(R.id.recyclerViewGallery))
                .check(matches(isClickable()))

            // Step 6: 削除結果確認
            onView(withId(R.id.recyclerViewGallery))
                .check(matches(hasChildCount(EXPECTED_ITEMS_AFTER_LARGE_DELETE))) // 10-6=4
        }
    }

    @Test
    fun `削除ワークフロー統合テスト_Requirements全要件検証`() {
        // Given: 包括的テストシナリオのためのテストデータ
        val testItems = DeletionTestHelper.createDiverseTestItemsWithMetadata(DIVERSE_TEST_ITEMS)
        DeletionTestHelper.injectTestItems(testItems)

        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->

            IdlingResourceHelper.waitForRecyclerView(
                R.id.recyclerViewGallery,
                minItemCount = 5,
                timeout = INITIAL_UI_WAIT_MS
            )

            // === Requirements 1.1-1.5: 選択機能統合検証 ===
            // RED: すべての選択機能統合が未実装のため失敗

            // Requirements 1.1: ギャラリー表示
            onView(withId(R.id.recyclerViewGallery))
                .check(matches(isDisplayed()))
                .check(matches(hasChildCount(8)))

            // Requirements 1.2: 長押し選択モード
            onView(withId(R.id.recyclerViewGallery))
                .perform(RecyclerViewActions.actionOnItemAtPosition<androidx.recyclerview.widget.RecyclerView.ViewHolder>(0, longClick()))

            // Requirements 1.3: 削除ボタン表示
            onView(withId(R.id.actionDelete))
                .check(matches(isDisplayed()))

            // Requirements 1.4: マルチ選択
            onView(withId(R.id.recyclerViewGallery))
                .perform(RecyclerViewActions.actionOnItemAtPosition<androidx.recyclerview.widget.RecyclerView.ViewHolder>(1, click()))
            onView(withId(R.id.recyclerViewGallery))
                .perform(RecyclerViewActions.actionOnItemAtPosition<androidx.recyclerview.widget.RecyclerView.ViewHolder>(2, click()))

            // === Requirements 2.1-2.5: 削除確認統合検証 ===
            // RED: 削除確認統合が未実装のため失敗

            // Requirements 2.1: 確認ダイアログ
            onView(withId(R.id.actionDelete))
                .perform(click())

            onView(withId(R.id.dialogDeleteConfirmation))
                .check(matches(isDisplayed()))

            // Requirements 2.2: アイテム数表示
            onView(withText(containsString("${SELECTION_COUNT_THREE}個のアイテム")))
                .check(matches(isDisplayed()))

            // Requirements 2.3: 削除実行
            onView(withId(R.id.buttonConfirmDelete))
                .perform(click())

            IdlingResourceHelper.waitForRecyclerView(
                R.id.recyclerViewGallery,
                minItemCount = EXPECTED_ITEMS_AFTER_DIVERSE_DELETE,
                timeout = DELETION_WAIT_MS
            )

            // === Requirements 3.1-3.5 & 4.1-4.5: 削除実行とフィードバック統合検証 ===
            // RED: 削除実行統合が未実装のため失敗

            // Requirements 3.1: データベース削除
            DeletionTestHelper.verifyDatabaseConsistency()

            // Requirements 3.2: ファイル削除
            DeletionTestHelper.verifyFileSystemConsistency()

            // Requirements 3.3: ギャラリー更新
            onView(withId(R.id.recyclerViewGallery))
                .check(matches(hasChildCount(EXPECTED_ITEMS_AFTER_DIVERSE_DELETE))) // 8-3=5

            // Requirements 4.3: 成功フィードバック
            onView(withText(containsString("${SELECTION_COUNT_THREE}個のアイテムの削除が完了しました")))
                .check(matches(isDisplayed()))

            // Requirements 4.4: 選択モード終了
            onView(withId(R.id.actionDelete))
                .check(doesNotExist())

            // === 最終整合性確認 ===
            DeletionTestHelper.verifyCompleteIntegrity()
        }
    }
}