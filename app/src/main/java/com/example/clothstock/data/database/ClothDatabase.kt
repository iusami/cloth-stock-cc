package com.example.clothstock.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.example.clothstock.data.model.ClothItem

/**
 * cloth-stock アプリケーションのメインデータベース
 * 
 * Roomデータベースの設定と初期化を行う
 */
@Database(
    entities = [ClothItem::class],
    version = 1,
    exportSchema = false // テスト用にfalse、本番では適切なスキーマ管理を実装
)
@TypeConverters(Converters::class)
abstract class ClothDatabase : RoomDatabase() {

    /**
     * ClothDaoインスタンスを取得
     */
    abstract fun clothDao(): ClothDao

    companion object {
        
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
         * @param context アプリケーションコンテキスト
         * @return ClothDatabaseインスタンス
         */
        fun getInstance(context: Context): ClothDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ClothDatabase::class.java,
                    DATABASE_NAME
                )
                    .addCallback(DatabaseCallback())
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
            
            // データベースオープン時の処理
            // WAL モードを有効化（読み書きパフォーマンス向上）
            db.execSQL("PRAGMA journal_mode=WAL")
            
            // 同期モード設定（パフォーマンス重視）
            db.execSQL("PRAGMA synchronous=NORMAL")
            
            // 外部キー制約を有効化
            db.execSQL("PRAGMA foreign_keys=ON")
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
            categoryCounts = emptyMap(), // 実装予定
            sizeCounts = emptyMap()       // 実装予定
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