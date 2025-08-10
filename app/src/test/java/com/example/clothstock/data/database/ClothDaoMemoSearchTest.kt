package com.example.clothstock.data.database

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.clothstock.data.model.ClothItem
import com.example.clothstock.data.model.TagData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.*
import org.junit.runner.RunWith
import java.io.IOException
import java.util.Date

/**
 * Task 7: ClothDao メモ検索機能の検証テスト
 * 
 * 既存のメモ検索機能が Requirements 3.1-3.4 を満たしているか確認
 * - 3.1: 検索クエリでタグとメモ内容の両方を検索
 * - 3.2: メモテキストにマッチした場合に結果に含める 
 * - 3.3: メモ内容の部分一致検索対応
 * - 3.4: 大文字小文字を区別しない検索
 */
@RunWith(AndroidJUnit4::class)
class ClothDaoMemoSearchTest {

    private lateinit var clothDao: ClothDao
    private lateinit var db: ClothDatabase

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, ClothDatabase::class.java
        ).build()
        clothDao = db.clothDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun searchItemsByText_メモ内容で検索できる() = runBlocking {
        // Given: メモを含むアイテムをデータベースに挿入
        val itemWithMemo = ClothItem(
            id = 1, 
            imagePath = "/path/to/image1.jpg",
            tagData = TagData(size = 100, color = "赤", category = "シャツ"),
            createdAt = Date(),
            memo = "購入場所：渋谷のセレクトショップ"
        )
        val itemWithoutMemo = ClothItem(
            id = 2, 
            imagePath = "/path/to/image2.jpg",
            tagData = TagData(size = 110, color = "青", category = "パンツ"),
            createdAt = Date(),
            memo = ""
        )
        clothDao.insertAll(listOf(itemWithMemo, itemWithoutMemo))

        // When: メモ内容で検索を実行
        val results = clothDao.searchItemsByText("渋谷").first()

        // Then: メモ内容にマッチするアイテムが1件取得される
        assertThat(results.size, `is`(1))
        assertThat(results[0].id, `is`(1))
        assertThat(results[0].memo, `is`("購入場所：渋谷のセレクトショップ"))
    }

    @Test
    fun searchItemsByText_大文字小文字を区別しない検索() = runBlocking {
        // Given: メモを含むアイテムをデータベースに挿入
        val item = ClothItem(
            id = 1, 
            imagePath = "/path/to/image.jpg",
            tagData = TagData(size = 100, color = "黒", category = "シャツ"),
            createdAt = Date(),
            memo = "Purchase in Tokyo"
        )
        clothDao.insert(item)

        // When: 大文字小文字が異なる検索テキストで検索を実行
        val resultsUpperCase = clothDao.searchItemsByText("PURCHASE").first()
        val resultsLowerCase = clothDao.searchItemsByText("purchase").first()

        // Then: 大文字小文字に関わらず同じ結果が取得される
        assertThat(resultsUpperCase.size, `is`(1))
        assertThat(resultsLowerCase.size, `is`(1))
        assertThat(resultsUpperCase[0].id, `is`(resultsLowerCase[0].id))
    }

    @Test
    fun searchItemsByText_タグとメモの複合検索() = runBlocking {
        // Given: メモとカテゴリを含むアイテムをデータベースに挿入
        val items = listOf(
            ClothItem(
                id = 1, 
                imagePath = "/path/to/image1.jpg",
                tagData = TagData(size = 100, color = "赤", category = "シャツ"),
                createdAt = Date(),
                memo = "渋谷で購入"
            ),
            ClothItem(
                id = 2,
                imagePath = "/path/to/image2.jpg", 
                tagData = TagData(size = 110, color = "青", category = "シャツ"),
                createdAt = Date(),
                memo = ""
            ),
            ClothItem(
                id = 3,
                imagePath = "/path/to/image3.jpg",
                tagData = TagData(size = 120, color = "黒", category = "パンツ"),
                createdAt = Date(),
                memo = "銀座のセレクトショップ"
            )
        )
        clothDao.insertAll(items)

        // When: カテゴリとメモの両方を検索
        val results = clothDao.searchItemsByText("シャツ").first()

        // Then: カテゴリとメモの両方にマッチするアイテムが取得される
        assertThat(results.size, `is`(2))
        assertThat(results.map { it.id }.contains(1), `is`(true))
        assertThat(results.map { it.id }.contains(2), `is`(true))
    }
}
