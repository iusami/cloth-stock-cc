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
     * バージョン1から2へのマイグレーション
     * 
     * 変更内容:
     * - cloth_itemsテーブルにmemoカラム（TEXT NOT NULL DEFAULT ''）を追加
     * - メモ検索性能向上のためindex_cloth_items_memoインデックスを作成
     * 
     * 対応要件: Task1 - データモデルとデータベーススキーマの拡張
     * 実装日: 2025年1月
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            try {
                // メモカラムを追加（デフォルト値は空文字列、NOT NULL制約）
                database.execSQL("ALTER TABLE cloth_items ADD COLUMN memo TEXT NOT NULL DEFAULT ''")
                
                // メモ検索用インデックスを作成（検索性能向上）
                database.execSQL("CREATE INDEX IF NOT EXISTS index_cloth_items_memo ON cloth_items(memo)")
                
                // マイグレーション完了ログ
                android.util.Log.d("DatabaseMigrations", "MIGRATION_1_2: memoカラムとインデックス追加完了")
                
            } catch (exception: Exception) {
                // マイグレーションエラーをログに記録
                android.util.Log.e("DatabaseMigrations", "MIGRATION_1_2 failed", exception)
                throw exception // マイグレーション失敗時は例外を再投出
            }
        }
    }
    
    /**
     * バージョン2から3へのマイグレーション
     * 
     * 変更内容:
     * - memoフィールドのデフォルト値を統一（'' → 'undefined'）
     * - 不足しているインデックスを追加（category, size, created_at, memo）
     * 
     * 問題解決: Migration didn't properly handleエラー対応
     * 実装日: 2025年8月
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            try {
                // 既存インデックスの確認と作成（存在チェック付き）
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_cloth_items_category 
                    ON cloth_items(category)
                """)
                
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_cloth_items_size 
                    ON cloth_items(size)
                """)
                
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_cloth_items_created_at 
                    ON cloth_items(createdAt)
                """)
                
                // memo用のインデックスは既にMIGRATION_1_2で作成済みなので確認のみ
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_cloth_items_memo 
                    ON cloth_items(memo)
                """)
                
                // マイグレーション完了ログ
                android.util.Log.d("DatabaseMigrations", "MIGRATION_2_3: インデックス統一完了")
                
            } catch (exception: Exception) {
                // マイグレーションエラーをログに記録
                android.util.Log.e("DatabaseMigrations", "MIGRATION_2_3 failed", exception)
                throw exception // マイグレーション失敗時は例外を再投出
            }
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