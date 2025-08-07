package com.example.clothstock.util

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.example.clothstock.data.database.ClothDatabase
import com.example.clothstock.data.model.ClothItem
import com.example.clothstock.data.model.TagData
import kotlinx.coroutines.runBlocking
import java.util.Date

/**
 * Espressoテスト用のデータヘルパークラス
 * テストデータベースへのデータ注入とクリーンアップを提供
 */
object TestDataHelper {

    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext

    /**
     * テスト用のClothItemデータを作成
     */
    fun createTestClothItem(
        id: Long = 0,
        imagePath: String = "content://media/external/images/media/test_image.jpg",
        tagData: TagData = createTestTagData(),
        createdAt: Date = Date()
    ): ClothItem {
        return ClothItem(
            id = id,
            imagePath = imagePath,
            tagData = tagData,
            createdAt = createdAt
        )
    }

    /**
     * テスト用のTagDataを作成
     */
    fun createTestTagData(
        size: Int = 100,
        color: String = "ブルー",
        category: String = "シャツ"
    ): TagData {
        return TagData(
            size = size,
            color = color,
            category = category
        )
    }

    /**
     * 複数のテストアイテムを作成
     */
    fun createMultipleTestItems(count: Int = 5): List<ClothItem> {
        val categories = listOf("シャツ", "パンツ", "ジャケット", "ドレス", "スカート")
        val colors = listOf("ブルー", "レッド", "グリーン", "イエロー", "ブラック")
        val sizes = listOf(80, 90, 100, 110, 120)

        return (1..count).map { index ->
            createTestClothItem(
                id = index.toLong(),
                imagePath = "content://media/external/images/media/test_image_$index.jpg",
                tagData = createTestTagData(
                    size = sizes[index % sizes.size],
                    color = colors[index % colors.size],
                    category = categories[index % categories.size]
                ),
                createdAt = Date(System.currentTimeMillis() - (index * 86400000L)) // 1日ずつ過去
            )
        }
    }

    /**
     * データベースにテストデータを注入
     */
    suspend fun injectTestData(items: List<ClothItem>) {
        val database = ClothDatabase.getDatabase(context)
        val dao = database.clothDao()
        
        items.forEach { item ->
            dao.insertItem(item)
        }
    }

    /**
     * テストデータベースをクリアアップ
     */
    suspend fun clearTestDatabase() {
        val database = ClothDatabase.getDatabase(context)
        val dao = database.clothDao()
        dao.deleteAllItems()
    }

    /**
     * 同期版のデータ注入（runBlocking使用）
     */
    fun injectTestDataSync(items: List<ClothItem>) {
        runBlocking {
            injectTestData(items)
        }
    }

    /**
     * 同期版のデータベースクリアアップ（runBlocking使用）
     */
    fun clearTestDatabaseSync() {
        runBlocking {
            clearTestDatabase()
        }
    }

    /**
     * 大量データ生成（スクロールテスト用）
     */
    fun createLargeTestDataSet(count: Int = 20): List<ClothItem> {
        return createMultipleTestItems(count)
    }

    /**
     * カテゴリ別データ生成（フィルタリングテスト用）
     */
    fun createCategorySpecificData(): List<ClothItem> {
        return listOf(
            createTestClothItem(1, tagData = createTestTagData(category = "シャツ", color = "ブルー")),
            createTestClothItem(2, tagData = createTestTagData(category = "シャツ", color = "レッド")),
            createTestClothItem(3, tagData = createTestTagData(category = "パンツ", color = "ブルー")),
            createTestClothItem(4, tagData = createTestTagData(category = "パンツ", color = "ブラック")),
            createTestClothItem(5, tagData = createTestTagData(category = "ジャケット", color = "グレー"))
        )
    }

    /**
     * 色別データ生成（色フィルタリングテスト用）
     */
    fun createColorSpecificData(): List<ClothItem> {
        return listOf(
            createTestClothItem(1, tagData = createTestTagData(color = "ブルー", category = "シャツ")),
            createTestClothItem(2, tagData = createTestTagData(color = "ブルー", category = "パンツ")),
            createTestClothItem(3, tagData = createTestTagData(color = "レッド", category = "ドレス")),
            createTestClothItem(4, tagData = createTestTagData(color = "レッド", category = "スカート")),
            createTestClothItem(5, tagData = createTestTagData(color = "グリーン", category = "ジャケット"))
        )
    }

    // ===== Task 13: エンドツーエンドテスト用データセット =====

    /**
     * カテゴリバランスの取れたテストデータ生成
     * 各カテゴリ5件ずつ、サイズと色も均等に配置
     */
    fun createCategoryBalancedTestData(): List<ClothItem> {
        val categories = listOf("シャツ", "パンツ", "ジャケット", "ドレス", "スカート")
        val colors = listOf("ブルー", "レッド", "グリーン", "イエロー", "ブラック")
        val sizes = listOf(80, 90, 100, 110, 120)
        
        val items = mutableListOf<ClothItem>()
        var id = 1L
        
        categories.forEachIndexed { categoryIndex, category ->
            repeat(5) { itemIndex ->
                items.add(
                    createTestClothItem(
                        id = id++,
                        tagData = createTestTagData(
                            size = sizes[itemIndex % sizes.size],
                            color = colors[itemIndex % colors.size], 
                            category = category
                        ),
                        createdAt = Date(System.currentTimeMillis() - (id * 3600000L)) // 1時間ずつ過去
                    )
                )
            }
        }
        
        return items
    }

    /**
     * 検索可能なテストデータ生成
     * 検索キーワードに対応した名前とカテゴリを持つデータ
     */
    fun createSearchableTestData(): List<ClothItem> {
        return listOf(
            createTestClothItem(1, tagData = createTestTagData(category = "シャツ", color = "ブルー", size = 100)),
            createTestClothItem(2, tagData = createTestTagData(category = "シャツ", color = "レッド", size = 110)),
            createTestClothItem(3, tagData = createTestTagData(category = "パンツ", color = "ブルー", size = 120)),
            createTestClothItem(4, tagData = createTestTagData(category = "ジャケット", color = "ブラック", size = 100)),
            createTestClothItem(5, tagData = createTestTagData(category = "ドレス", color = "グリーン", size = 90)),
            createTestClothItem(6, tagData = createTestTagData(category = "スカート", color = "イエロー", size = 80)),
            createTestClothItem(7, tagData = createTestTagData(category = "シャツ", color = "ホワイト", size = 110)),
            createTestClothItem(8, tagData = createTestTagData(category = "パンツ", color = "グレー", size = 100))
        )
    }

    /**
     * リアルなボリュームを想定した大量データセット
     * パフォーマンステスト用
     */
    fun createRealisticLargeDataSet(count: Int): List<ClothItem> {
        val categories = listOf(
            "シャツ", "ブラウス", "Tシャツ", "ポロシャツ", "タンクトップ",
            "パンツ", "ジーンズ", "ショーツ", "レギンス", "チノパン",
            "ジャケット", "コート", "カーディガン", "パーカー", "ベスト",
            "ドレス", "ワンピース", "スカート", "キュロット"
        )
        val colors = listOf(
            "ブラック", "ホワイト", "グレー", "ネイビー", "ベージュ",
            "ブルー", "レッド", "グリーン", "イエロー", "ピンク",
            "パープル", "オレンジ", "ブラウン", "カーキ", "マゼンタ"
        )
        val sizes = listOf(70, 80, 85, 90, 95, 100, 105, 110, 115, 120, 125, 130)
        
        return (1..count).map { index ->
            createTestClothItem(
                id = index.toLong(),
                imagePath = "content://media/external/images/media/realistic_test_$index.jpg",
                tagData = createTestTagData(
                    size = sizes[index % sizes.size],
                    color = colors[index % colors.size],
                    category = categories[index % categories.size]
                ),
                createdAt = Date(System.currentTimeMillis() - (index * 1800000L)) // 30分ずつ過去
            )
        }
    }
}