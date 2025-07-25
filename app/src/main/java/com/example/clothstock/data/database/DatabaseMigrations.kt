package com.example.clothstock.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * データベースマイグレーション定義
 * 
 * 将来のスキーマ変更に対応するためのマイグレーション戦略
 */
object DatabaseMigrations {
    
    /**
     * バージョン1から2へのマイグレーション例
     * （将来の拡張用）
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // 例: 新しいカラム追加
            // database.execSQL("ALTER TABLE cloth_items ADD COLUMN brand TEXT")
            
            // 例: 新しいテーブル作成
            // database.execSQL("""
            //     CREATE TABLE IF NOT EXISTS favorites (
            //         id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            //         cloth_item_id INTEGER NOT NULL,
            //         created_at INTEGER NOT NULL,
            //         FOREIGN KEY(cloth_item_id) REFERENCES cloth_items(id) ON DELETE CASCADE
            //     )
            // """)
            
            // 例: インデックス追加
            // database.execSQL("CREATE INDEX IF NOT EXISTS index_favorites_cloth_item_id ON favorites(cloth_item_id)")
        }
    }
    
    /**
     * バージョン2から3へのマイグレーション例
     * （将来の拡張用）
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // 例: カラム名変更
            // database.execSQL("ALTER TABLE cloth_items RENAME COLUMN color TO primary_color")
            
            // 例: データ型変更のためのテーブル再作成
            // database.execSQL("""
            //     CREATE TABLE cloth_items_new (
            //         id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            //         imagePath TEXT NOT NULL,
            //         size INTEGER NOT NULL,
            //         primary_color TEXT NOT NULL,
            //         secondary_color TEXT,
            //         category TEXT NOT NULL,
            //         createdAt INTEGER NOT NULL
            //     )
            // """)
            // 
            // database.execSQL("""
            //     INSERT INTO cloth_items_new (id, imagePath, size, primary_color, category, createdAt)
            //     SELECT id, imagePath, size, color, category, createdAt FROM cloth_items
            // """)
            // 
            // database.execSQL("DROP TABLE cloth_items")
            // database.execSQL("ALTER TABLE cloth_items_new RENAME TO cloth_items")
        }
    }
    
    /**
     * 破壊的マイグレーション（データ損失あり）
     * 開発段階でのみ使用
     */
    val DESTRUCTIVE_MIGRATION = object : Migration(1, 1000) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // すべてのテーブルを削除して再作成
            database.execSQL("DROP TABLE IF EXISTS cloth_items")
            
            // 新しいスキーマでテーブル作成
            database.execSQL("""
                CREATE TABLE cloth_items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    imagePath TEXT NOT NULL,
                    size INTEGER NOT NULL,
                    color TEXT NOT NULL,
                    category TEXT NOT NULL,
                    createdAt INTEGER NOT NULL
                )
            """)
            
            // インデックス再作成
            database.execSQL("CREATE INDEX index_cloth_items_category ON cloth_items(category)")
            database.execSQL("CREATE INDEX index_cloth_items_size ON cloth_items(size)")
            database.execSQL("CREATE INDEX index_cloth_items_created_at ON cloth_items(createdAt)")
        }
    }
    
    /**
     * すべてのマイグレーションのリストを取得
     */
    fun getAllMigrations(): Array<Migration> {
        return arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3
            // 将来のマイグレーションをここに追加
        )
    }
    
    /**
     * データベースバージョンの検証
     */
    fun validateMigrationPath(fromVersion: Int, toVersion: Int): Boolean {
        val supportedMigrations = getAllMigrations()
        
        // 連続するマイグレーションパスが存在するかチェック
        var currentVersion = fromVersion
        while (currentVersion < toVersion) {
            val nextMigration = supportedMigrations.find { 
                it.startVersion == currentVersion 
            }
            
            if (nextMigration == null) {
                return false
            }
            
            currentVersion = nextMigration.endVersion
        }
        
        return currentVersion == toVersion
    }
    
    /**
     * マイグレーション戦略の説明
     */
    object MigrationStrategy {
        const val MIGRATION_GUIDE = """
        === データベースマイグレーション戦略 ===
        
        1. スキーマ変更の種類：
           - 新しいカラム追加: ALTER TABLE ADD COLUMN
           - 新しいテーブル追加: CREATE TABLE
           - インデックス追加: CREATE INDEX
           - カラム削除: テーブル再作成が必要
           - データ型変更: テーブル再作成が必要
        
        2. マイグレーション手順：
           - 各バージョン間の明示的なマイグレーション定義
           - データ保持を最優先とする
           - 大きな変更は段階的に実施
        
        3. テスト戦略：
           - 各マイグレーションの単体テスト
           - 連続マイグレーションのテスト
           - データ整合性の検証
        
        4. ロールバック戦略：
           - 下位バージョンへの明示的なマイグレーション
           - 緊急時のデータバックアップ
        """
    }
}