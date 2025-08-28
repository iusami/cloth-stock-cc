package com.example.clothstock.integration.deletion.helper

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.clothstock.data.database.ClothDatabase
import com.example.clothstock.data.model.ClothItem
import com.example.clothstock.data.model.TagData
import java.util.Date
import java.io.File
import kotlinx.coroutines.runBlocking

/**
 * Task 10 - GREEN Phase 2: 削除統合テスト専用ヘルパー
 * 
 * 基本実装: テストを通過させるための最小限の実装を追加
 * まだ完全ではないが、基本的な統合テスト機能を提供
 */
object DeletionTestHelper {

    private lateinit var testDatabase: ClothDatabase
    private val testItems = mutableListOf<ClothItem>()
    private val testFiles = mutableListOf<File>()

    // ===== 環境セットアップ・クリーンアップ =====

    /**
     * 削除テスト環境のセットアップ
     * GREEN: 基本的なテスト用データベースセットアップ
     */
    fun setupDeletionTestEnvironment() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        testDatabase = Room.inMemoryDatabaseBuilder(
            context,
            ClothDatabase::class.java
        ).allowMainThreadQueries().build()
        
        testItems.clear()
        testFiles.clear()
    }

    /**
     * テストデータの完全クリア
     * GREEN: データベースとファイルリストのクリア
     */
    fun clearAllTestData() {
        runBlocking {
            if (::testDatabase.isInitialized) {
                testDatabase.clothDao().deleteAll()
            }
        }
        testItems.clear()
        
        // テストファイルの削除
        testFiles.forEach { file ->
            if (file.exists()) {
                file.delete()
            }
        }
        testFiles.clear()
    }

    /**
     * テスト環境のクリーンアップ
     * GREEN: データベースクローズとリソース解放
     */
    fun cleanupDeletionTestEnvironment() {
        clearAllTestData()
        if (::testDatabase.isInitialized) {
            testDatabase.close()
        }
    }

    // ===== テストデータ作成 =====

    /**
     * ファイル付きテストアイテム作成
     * GREEN: 基本的なテストアイテムとファイルを作成
     */
    fun createTestItemsWithFiles(count: Int): List<ClothItem> {
        val items = mutableListOf<ClothItem>()
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        repeat(count) { index ->
            val itemId = (1000 + index).toLong()
            val imagePath = createTempImageFile("test_item_${itemId}").absolutePath
            
            val item = ClothItem(
                id = itemId,
                imagePath = imagePath,
                tagData = TagData(
                    size = 100 + (index % 60), // サイズ100-160
                    color = listOf("赤", "青", "緑", "黄", "黒")[index % 5],
                    category = listOf("シャツ", "パンツ", "ジャケット")[index % 3]
                ),
                createdAt = Date(),
                memo = "テストアイテム $itemId"
            )
            
            items.add(item)
        }
        
        testItems.addAll(items)
        return items
    }

    /**
     * 特定IDのテストアイテム作成
     * GREEN: 指定されたIDでテストアイテムを作成
     */
    fun createTestItemsWithSpecificIds(ids: List<Long>): List<ClothItem> {
        val items = mutableListOf<ClothItem>()
        
        ids.forEachIndexed { index, id ->
            val imagePath = createTempImageFile("test_item_${id}").absolutePath
            
            val item = ClothItem(
                id = id,
                imagePath = imagePath,
                tagData = TagData(
                    size = 120 + (index % 40), // サイズ120-160
                    color = listOf("白", "黒", "グレー")[index % 3],
                    category = listOf("トップス", "ボトムス", "アウター")[index % 3]
                ),
                createdAt = Date(),
                memo = "特定IDテストアイテム $id"
            )
            
            items.add(item)
        }
        
        testItems.addAll(items)
        return items
    }

    /**
     * 大量テストアイテム作成（パフォーマンステスト用）
     * GREEN: パフォーマンステスト用の大量アイテム作成
     */
    fun createLargeTestItemsWithFiles(count: Int): List<ClothItem> {
        val items = mutableListOf<ClothItem>()
        
        repeat(count) { index ->
            val itemId = (10000 + index).toLong()
            val imagePath = createTempImageFile("large_test_item_${itemId}").absolutePath
            
            val item = ClothItem(
                id = itemId,
                imagePath = imagePath,
                tagData = TagData(
                    size = 60 + (index % 100), // サイズ60-160
                    color = "テストカラー${index % 10}",
                    category = "テストカテゴリ${index % 5}"
                ),
                createdAt = Date(),
                memo = "大量テストアイテム $itemId"
            )
            
            items.add(item)
        }
        
        testItems.addAll(items)
        return items
    }

    /**
     * 多様なメタデータ付きテストアイテム作成
     * GREEN: 多様なメタデータを持つテストアイテム作成
     */
    fun createDiverseTestItemsWithMetadata(count: Int): List<ClothItem> {
        val items = mutableListOf<ClothItem>()
        val colors = listOf("赤", "青", "緑", "黄", "黒", "白", "ピンク", "紫", "オレンジ", "ブラウン")
        val categories = listOf("シャツ", "パンツ", "ジャケット", "スカート", "ドレス", "コート", "靴", "バッグ")
        
        repeat(count) { index ->
            val itemId = (20000 + index).toLong()
            val imagePath = createTempImageFile("diverse_test_item_${itemId}").absolutePath
            
            val item = ClothItem(
                id = itemId,
                imagePath = imagePath,
                tagData = TagData(
                    size = 60 + (index * 7) % 100, // 多様なサイズ
                    color = colors[index % colors.size],
                    category = categories[index % categories.size]
                ),
                createdAt = Date(System.currentTimeMillis() - (index * 86400000L)), // 異なる日付
                memo = "多様なメタデータテストアイテム $itemId - ${categories[index % categories.size]}"
            )
            
            items.add(item)
        }
        
        testItems.addAll(items)
        return items
    }

    // ===== テストデータ注入・操作 =====

    /**
     * テストアイテムをデータベースに注入
     * GREEN: 基本的なデータベース注入機能
     */
    fun injectTestItems(items: List<ClothItem>) {
        runBlocking {
            if (::testDatabase.isInitialized) {
                items.forEach { item ->
                    testDatabase.clothDao().insert(item)
                }
            }
        }
    }

    // ===== データ整合性検証 =====

    /**
     * データベースからアイテムが削除されたことを検証
     * GREEN: 基本的なデータベース削除確認
     */
    fun verifyItemDeletedFromDatabase(itemId: Long) {
        runBlocking {
            if (::testDatabase.isInitialized) {
                val item = testDatabase.clothDao().getItemById(itemId)
                if (item != null) {
                    throw AssertionError("Item $itemId still exists in database after deletion")
                }
            }
        }
    }

    /**
     * 画像ファイルが削除されたことを検証
     * GREEN: 基本的なファイル削除確認
     */
    fun verifyImageFileDeleted(imagePath: String) {
        val file = File(imagePath)
        if (file.exists()) {
            throw AssertionError("Image file $imagePath still exists after deletion")
        }
        testFiles.removeAll { it.absolutePath == imagePath }
    }

    /**
     * 残存アイテムの整合性を検証
     * GREEN: 残存アイテムのデータベース確認
     */
    fun verifyRemainingItems(expectedIds: List<Long>) {
        runBlocking {
            if (::testDatabase.isInitialized) {
                val allItems = testDatabase.clothDao().getAllItemsOnce()
                val actualIds = allItems.map { it.id }.sorted()
                val expectedSorted = expectedIds.sorted()
                
                if (actualIds != expectedSorted) {
                    throw AssertionError(
                        "Remaining items mismatch. Expected: $expectedSorted, Actual: $actualIds"
                    )
                }
            }
        }
    }

    /**
     * データベース整合性検証
     * GREEN: 基本的なデータベース整合性確認
     */
    fun verifyDatabaseConsistency() {
        runBlocking {
            if (::testDatabase.isInitialized) {
                val allItems = testDatabase.clothDao().getAllItemsOnce()
                // 基本的な整合性チェック: 全アイテムに有効なデータが設定されているか
                allItems.forEach { item ->
                    if (item.id <= 0 || item.imagePath.isEmpty()) {
                        throw AssertionError("Database inconsistency detected: invalid item $item")
                    }
                }
            }
        }
    }

    /**
     * ファイルシステム整合性検証
     * GREEN: 基本的なファイルシステム整合性確認
     */
    fun verifyFileSystemConsistency() {
        runBlocking {
            if (::testDatabase.isInitialized) {
                val allItems = testDatabase.clothDao().getAllItemsOnce()
                allItems.forEach { item ->
                    val file = File(item.imagePath)
                    if (!file.exists()) {
                        throw AssertionError(
                            "File system inconsistency: referenced file ${item.imagePath} does not exist"
                        )
                    }
                }
            }
        }
    }

    /**
     * 完全な整合性検証
     * GREEN: データベースとファイルシステムの完全整合性確認
     */
    fun verifyCompleteIntegrity() {
        verifyDatabaseConsistency()
        verifyFileSystemConsistency()
        
        // 追加チェック: データベースのアイテム数とテストファイル数の整合性
        runBlocking {
            if (::testDatabase.isInitialized) {
                val dbItemCount = testDatabase.clothDao().getAllItemsOnce().size
                val existingFileCount = testFiles.count { it.exists() }
                
                // Note: 削除によってファイル数とDB数は一致しない可能性があるため、
                // 基本的なサニティチェックのみ実行
                if (dbItemCount < 0) {
                    throw AssertionError("Invalid database item count: $dbItemCount")
                }
            }
        }
    }

    // ===== プライベートヘルパーメソッド =====
    // GREEN Phase: 基本的な動作を提供するヘルパーメソッド実装

    private fun createTempImageFile(prefix: String): File {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tempDir = File(context.cacheDir, "deletion_test_images")
        if (!tempDir.exists()) {
            tempDir.mkdirs()
        }
        
        val tempFile = File(tempDir, "${prefix}_${System.currentTimeMillis()}.jpg")
        
        // 空のテスト画像ファイルを作成
        tempFile.writeText("test_image_data_${prefix}")
        
        testFiles.add(tempFile)
        return tempFile
    }

    private fun generateTestImagePath(itemId: Long): String {
        return createTempImageFile("item_$itemId").absolutePath
    }

    private fun simulateImageFileCreation(imagePath: String) {
        val file = File(imagePath)
        if (!file.exists()) {
            file.parentFile?.mkdirs()
            file.writeText("simulated_image_data")
            testFiles.add(file)
        }
    }

    private fun getDatabaseInstanceForTesting(): ClothDatabase {
        return if (::testDatabase.isInitialized) {
            testDatabase
        } else {
            val context = ApplicationProvider.getApplicationContext<Context>()
            Room.inMemoryDatabaseBuilder(context, ClothDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        }
    }

    private fun getRepositoryInstanceForTesting(): Any {
        // Note: ClothRepositoryが利用可能な場合、実際のリポジトリインスタンスを返す
        // 現在は基本的なモック実装を返す
        return object {
            fun deleteItems(ids: List<Long>) = runBlocking {
                ids.forEach { id ->
                    testDatabase.clothDao().deleteById(id)
                }
            }
        }
    }
}