package com.example.clothstock.data.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import java.io.IOException

/**
 * データベースマイグレーションのInstrumentedTest
 * 
 * TDD アプローチに従い、まず失敗するテストを作成してから実装を行う
 * Room の MigrationTestHelper を使用してマイグレーションテストを実行
 */
@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
    
    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ClothDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    // ===== 🔴 TDD Red: データベースマイグレーションテスト（失敗するテスト） =====

    @Test
    fun migration_1_to_2_memoカラム追加_成功する() {
        // Given - バージョン1のデータベースを作成
        var db = helper.createDatabase(TEST_DB, 1).apply {
            // バージョン1のテストデータを挿入
            execSQL("""
                INSERT INTO cloth_items (imagePath, size, color, category, createdAt) 
                VALUES ('/path/test.jpg', 100, '青', 'シャツ', ${System.currentTimeMillis()})
            """)
            close()
        }

        // When - バージョン2へマイグレーション実行
        db = helper.runMigrationsAndValidate(
            TEST_DB, 
            2, 
            true, 
            DatabaseMigrations.MIGRATION_1_2
        )

        // Then - memoカラムが追加されていることを確認
        val cursor = db.query("SELECT memo FROM cloth_items WHERE id = 1")
        assertTrue("memoカラムが存在する", cursor.moveToFirst())
        
        val memoColumnIndex = cursor.getColumnIndex("memo")
        assertTrue("memoカラムインデックスが有効", memoColumnIndex >= 0)
        
        val memoValue = cursor.getString(memoColumnIndex)
        assertEquals("初期memoは空文字列", "", memoValue)
        
        cursor.close()
    }

    @Test
    fun migration_1_to_2_memoインデックス作成_成功する() {
        // Given - バージョン1のデータベースを作成
        helper.createDatabase(TEST_DB, 1).apply {
            close()
        }

        // When - バージョン2へマイグレーション実行
        val db = helper.runMigrationsAndValidate(
            TEST_DB, 
            2, 
            true, 
            DatabaseMigrations.MIGRATION_1_2
        )

        // Then - memoインデックスが作成されていることを確認
        val cursor = db.query("""
            SELECT name FROM sqlite_master 
            WHERE type='index' AND name='index_cloth_items_memo'
        """)
        
        assertTrue("memoインデックスが作成されている", cursor.moveToFirst())
        assertEquals("正しいインデックス名", "index_cloth_items_memo", cursor.getString(0))
        
        cursor.close()
    }

    @Test
    fun migration_1_to_2_既存データ保持_成功する() {
        // Given - バージョン1のデータベースにテストデータを挿入
        val originalImagePath = "/storage/test/shirt.jpg"
        val originalSize = 120
        val originalColor = "赤"
        val originalCategory = "パンツ"
        val originalCreatedAt = System.currentTimeMillis()
        
        var db = helper.createDatabase(TEST_DB, 1).apply {
            execSQL("""
                INSERT INTO cloth_items (imagePath, size, color, category, createdAt) 
                VALUES (?, ?, ?, ?, ?)
            """, arrayOf(originalImagePath, originalSize, originalColor, originalCategory, originalCreatedAt))
            close()
        }

        // When - バージョン2へマイグレーション実行
        db = helper.runMigrationsAndValidate(
            TEST_DB, 
            2, 
            true, 
            DatabaseMigrations.MIGRATION_1_2
        )

        // Then - 既存データが正しく保持されていることを確認
        val cursor = db.query("SELECT * FROM cloth_items WHERE id = 1")
        assertTrue("データが存在する", cursor.moveToFirst())

        assertEquals("imagePathが保持されている", originalImagePath, cursor.getString(cursor.getColumnIndexOrThrow("imagePath")))
        assertEquals("sizeが保持されている", originalSize, cursor.getInt(cursor.getColumnIndexOrThrow("size")))
        assertEquals("colorが保持されている", originalColor, cursor.getString(cursor.getColumnIndexOrThrow("color")))
        assertEquals("categoryが保持されている", originalCategory, cursor.getString(cursor.getColumnIndexOrThrow("category")))
        assertEquals("createdAtが保持されている", originalCreatedAt, cursor.getLong(cursor.getColumnIndexOrThrow("createdAt")))
        assertEquals("新しいmemoは空文字列", "", cursor.getString(cursor.getColumnIndexOrThrow("memo")))

        cursor.close()
    }

    @Test
    fun migration_1_to_2_複数データ保持_成功する() {
        // Given - 複数のテストデータを挿入
        var db = helper.createDatabase(TEST_DB, 1).apply {
            execSQL("""
                INSERT INTO cloth_items (imagePath, size, color, category, createdAt) 
                VALUES ('/path/shirt1.jpg', 100, '青', 'シャツ', ${System.currentTimeMillis()})
            """)
            execSQL("""
                INSERT INTO cloth_items (imagePath, size, color, category, createdAt) 
                VALUES ('/path/pants1.jpg', 110, '黒', 'パンツ', ${System.currentTimeMillis() + 1000})
            """)
            execSQL("""
                INSERT INTO cloth_items (imagePath, size, color, category, createdAt) 
                VALUES ('/path/jacket1.jpg', 120, '白', 'アウター', ${System.currentTimeMillis() + 2000})
            """)
            close()
        }

        // When - マイグレーション実行
        db = helper.runMigrationsAndValidate(
            TEST_DB, 
            2, 
            true, 
            DatabaseMigrations.MIGRATION_1_2
        )

        // Then - 全データが保持され、memoカラムが追加されていることを確認
        val cursor = db.query("SELECT COUNT(*) FROM cloth_items")
        assertTrue("データが存在する", cursor.moveToFirst())
        assertEquals("全3件のデータが保持されている", 3, cursor.getInt(0))
        cursor.close()

        // 各データのmemoカラムをチェック
        val allDataCursor = db.query("SELECT id, memo FROM cloth_items ORDER BY id")
        var recordCount = 0
        while (allDataCursor.moveToNext()) {
            recordCount++
            val memo = allDataCursor.getString(allDataCursor.getColumnIndexOrThrow("memo"))
            assertEquals("レコード${recordCount}のmemoは空文字列", "", memo)
        }
        assertEquals("3件すべてを処理した", 3, recordCount)
        allDataCursor.close()
    }

    @Test
    fun migration_1_to_2_スキーマ検証_成功する() {
        // Given - バージョン1のデータベース
        helper.createDatabase(TEST_DB, 1).apply {
            close()
        }

        // When - マイグレーション実行
        val db = helper.runMigrationsAndValidate(
            TEST_DB, 
            2, 
            true, 
            DatabaseMigrations.MIGRATION_1_2
        )

        // Then - テーブル構造が正しいことを確認
        val cursor = db.query("PRAGMA table_info(cloth_items)")
        val columns = mutableMapOf<String, String>()
        
        while (cursor.moveToNext()) {
            val columnName = cursor.getString(cursor.getColumnIndexOrThrow("name"))
            val columnType = cursor.getString(cursor.getColumnIndexOrThrow("type"))
            columns[columnName] = columnType
        }
        cursor.close()

        // 必要なカラムが存在することを確認
        assertTrue("idカラムが存在する", columns.containsKey("id"))
        assertTrue("imagePathカラムが存在する", columns.containsKey("imagePath"))
        assertTrue("sizeカラムが存在する", columns.containsKey("size"))
        assertTrue("colorカラムが存在する", columns.containsKey("color"))
        assertTrue("categoryカラムが存在する", columns.containsKey("category"))
        assertTrue("createdAtカラムが存在する", columns.containsKey("createdAt"))
        assertTrue("memoカラムが存在する", columns.containsKey("memo"))

        // memoカラムの型が正しいことを確認
        assertEquals("memoカラムの型はTEXT", "TEXT", columns["memo"])
    }

    @Test 
    fun migration_存在しないバージョン_エラーになる() {
        // Given - バージョン1のデータベース
        helper.createDatabase(TEST_DB, 1).apply {
            close()
        }

        // When & Then - 存在しないマイグレーション実行でエラー
        try {
            helper.runMigrationsAndValidate(
                TEST_DB, 
                3, // バージョン3は存在しない
                true, 
                DatabaseMigrations.MIGRATION_1_2
            )
            fail("存在しないバージョンへのマイグレーションは失敗すべき")
        } catch (e: IllegalStateException) {
            assertTrue("適切なエラーメッセージが含まれる", e.message!!.contains("Migration") || e.message!!.contains("version"))
        }
    }
}