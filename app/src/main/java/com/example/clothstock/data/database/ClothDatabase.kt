package com.example.clothstock.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import android.util.Log
import com.example.clothstock.data.model.ClothItem

/**
 * cloth-stock アプリケーションのメインデータベース
 * 
 * Roomデータベースの設定と初期化を行う
 * 
 * データベース変更履歴:
 * - v1: 基本的な衣服アイテム情報（画像パス、タグ情報、作成日時）
 * - v2: メモ機能追加（memoカラム追加、検索用インデックス追加）
 * 
 * 対応エンティティ:
 * - ClothItem: 衣服アイテム情報とメモ情報を保存
 * 
 * マイグレーション対応:
 * - MIGRATION_1_2: メモ機能追加対応
 */
@Database(
    entities = [ClothItem::class],
    version = 2,
    exportSchema = false // テスト用にfalse、本番では適切なスキーマ管理を実装
)
@TypeConverters(Converters::class)
abstract class ClothDatabase : RoomDatabase() {

    /**
     * ClothDaoインスタンスを取得
     */
    abstract fun clothDao(): ClothDao

    companion object {
        private const val TAG = "ClothDatabase"
        
        /**
         * データベース名定数
         */
        private const val DATABASE_NAME = "cloth_stock_database"

        /**
         * Singletonパターンのデータベースインスタンス
         */
        @Volatile
        private var INSTANCE: ClothDatabase? = null

        /**
         * データベースインスタンスを取得
         * 
         * Singletonパターンでインスタンスを管理し、初回作成時に
         * 適切なマイグレーション設定とコールバック設定を行う
         * 
         * 設定内容:
         * - DatabaseCallback: 初期インデックス作成とPRAGMA設定
         * - MIGRATION_1_2: v1→v2メモ機能追加マイグレーション
         * 
         * @param context アプリケーションコンテキスト
         * @return ClothDatabaseインスタンス（スレッドセーフ）
         */
        fun getInstance(context: Context): ClothDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ClothDatabase::class.java,
                    DATABASE_NAME
                )
                    .addCallback(DatabaseCallback())
                    .addMigrations(DatabaseMigrations.MIGRATION_1_2)
                    .build()
                
                INSTANCE = instance
                instance
            }
        }

        /**
         * テスト用のインメモリデータベースを作成
         * 
         * @param context テストコンテキスト
         * @return テスト用データベースインスタンス
         */
        fun getInMemoryDatabase(context: Context): ClothDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                ClothDatabase::class.java
            )
                .allowMainThreadQueries() // テスト用のみ
                .build()
        }

        /**
         * データベースインスタンスをクリア（主にテスト用）
         */
        @Synchronized
        fun clearInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }

    /**
     * データベース作成・マイグレーション時のコールバック
     */
    private class DatabaseCallback : RoomDatabase.Callback() {
        
        override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            super.onCreate(db)
            
            // データベース作成時の初期化処理
            // 必要に応じてデフォルトデータの挿入やインデックス作成を実装
            
            // パフォーマンス向上のためのインデックス作成
            db.execSQL("""
                CREATE INDEX IF NOT EXISTS index_cloth_items_category 
                ON cloth_items(category)
            """)
            
            db.execSQL("""
                CREATE INDEX IF NOT EXISTS index_cloth_items_size 
                ON cloth_items(size)
            """)
            
            db.execSQL("""
                CREATE INDEX IF NOT EXISTS index_cloth_items_created_at 
                ON cloth_items(createdAt)
            """)
        }

        override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            super.onOpen(db)
            Log.d(TAG, "DatabaseCallback.onOpen called - setting PRAGMA options")
            
            try {
                // データベースオープン時の処理
                // PRAGMA文はqueryメソッドを使用する必要がある
                
                // WAL モードを有効化（読み書きパフォーマンス向上）
                Log.d(TAG, "Setting PRAGMA journal_mode=WAL")
                db.query("PRAGMA journal_mode=WAL").use { cursor ->
                    if (cursor.moveToFirst()) {
                        Log.d(TAG, "journal_mode set to: ${cursor.getString(0)}")
                    }
                }
                
                // 同期モード設定（パフォーマンス重視）
                Log.d(TAG, "Setting PRAGMA synchronous=NORMAL")
                db.query("PRAGMA synchronous=NORMAL").use { cursor ->
                    if (cursor.moveToFirst()) {
                        Log.d(TAG, "synchronous set to: ${cursor.getString(0)}")
                    }
                }
                
                // 外部キー制約を有効化
                Log.d(TAG, "Setting PRAGMA foreign_keys=ON")
                db.query("PRAGMA foreign_keys=ON").use { cursor ->
                    if (cursor.moveToFirst()) {
                        Log.d(TAG, "foreign_keys set to: ${cursor.getString(0)}")
                    }
                }
                
                Log.d(TAG, "Database onOpen completed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error in DatabaseCallback.onOpen: ${e.message}", e)
                // PRAGMA設定エラーは致命的ではないので、ログに記録して続行
            }
        }
    }

    /**
     * データベースの健全性チェック
     * 
     * @return チェック結果
     */
    suspend fun checkDatabaseIntegrity(): Boolean {
        return try {
            // 簡単な整合性チェッククエリ
            clothDao().getItemCount()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * データベースサイズを取得（KB単位）
     * 
     * @param context アプリケーションコンテキスト
     * @return データベースファイルサイズ
     */
    fun getDatabaseSize(context: Context): Long {
        val dbFile = context.getDatabasePath(DATABASE_NAME)
        return if (dbFile.exists()) {
            dbFile.length() / 1024 // KB単位
        } else {
            0L
        }
    }

    /**
     * データベースの統計情報を取得
     * 
     * @return 統計情報
     */
    suspend fun getDatabaseStats(): DatabaseStats {
        val dao = clothDao()
        return DatabaseStats(
            totalItems = dao.getItemCount(),
            categoryCounts = dao.getItemCountByCategory().associate { it.category to it.count },
            sizeCounts = dao.getItemCountBySize().associate { it.size to it.count }
        )
    }

    /**
     * データベース統計情報を表すデータクラス
     */
    data class DatabaseStats(
        val totalItems: Int,
        val categoryCounts: Map<String, Int>,
        val sizeCounts: Map<Int, Int>
    )
}