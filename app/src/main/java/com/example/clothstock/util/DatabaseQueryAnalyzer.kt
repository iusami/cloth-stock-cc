package com.example.clothstock.util

import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.example.clothstock.ui.gallery.QueryAnalysisResult

/**
 * データベースクエリ分析クラス
 * SQLクエリのパフォーマンスを分析し、最適化の提案を行う
 */
class DatabaseQueryAnalyzer(private val database: SQLiteDatabase) {
    
    companion object {
        private const val TAG = "DatabaseQueryAnalyzer"
    }
    
    /**
     * クエリを分析してパフォーマンス情報を取得
     * @param query 分析対象のSQLクエリ
     * @return クエリ分析結果
     */
    fun analyzeQuery(query: String): QueryAnalysisResult {
        val startTime = System.currentTimeMillis()
        
        try {
            // EXPLAIN QUERY PLANを使用してクエリプランを取得
            val explainQuery = "EXPLAIN QUERY PLAN $query"
            val cursor = database.rawQuery(explainQuery, null)
            
            var usesIndex = false
            var estimatedRows = 0
            
            cursor.use {
                while (it.moveToNext()) {
                    val detail = it.getString(it.getColumnIndexOrThrow("detail"))
                    Log.d(TAG, "Query plan: $detail")
                    
                    // インデックス使用の確認
                    if (detail.contains("USING INDEX", ignoreCase = true)) {
                        usesIndex = true
                    }
                    
                    // 推定行数の抽出（簡略化）
                    if (detail.contains("SCAN", ignoreCase = true)) {
                        estimatedRows = 10000 // デフォルト値
                    } else if (detail.contains("SEARCH", ignoreCase = true)) {
                        estimatedRows = 100 // インデックス使用時の推定値
                    }
                }
            }
            
            val endTime = System.currentTimeMillis()
            val executionTime = endTime - startTime
            
            return QueryAnalysisResult(
                usesIndex = usesIndex,
                estimatedRows = estimatedRows,
                executionTimeMs = executionTime
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to analyze query: $query", e)
            return QueryAnalysisResult(
                usesIndex = false,
                estimatedRows = -1,
                executionTimeMs = -1
            )
        }
    }
    
    /**
     * インデックス作成の提案を行う
     * @param tableName テーブル名
     * @param columnNames インデックス対象のカラム名リスト
     * @return インデックス作成SQL
     */
    fun suggestIndex(tableName: String, columnNames: List<String>): String {
        val indexName = "idx_${tableName}_${columnNames.joinToString("_")}"
        val columns = columnNames.joinToString(", ")
        return "CREATE INDEX IF NOT EXISTS $indexName ON $tableName ($columns)"
    }
    
    /**
     * 複合インデックスの提案
     * @return 推奨インデックス作成SQLのリスト
     */
    fun suggestOptimalIndexes(): List<String> {
        return listOf(
            // 検索・フィルタリング用の複合インデックス
            suggestIndex("cloth_items", listOf("size", "color", "category")),
            // 作成日時でのソート用インデックス
            suggestIndex("cloth_items", listOf("createdAt")),
            // テキスト検索用インデックス（部分一致は効果が限定的だが、前方一致には有効）
            suggestIndex("cloth_items", listOf("color")),
            suggestIndex("cloth_items", listOf("category"))
        )
    }
    
    /**
     * クエリ最適化の提案
     * @param query 最適化対象のクエリ
     * @return 最適化提案のリスト
     */
    fun suggestOptimizations(query: String): List<String> {
        val suggestions = mutableListOf<String>()
        
        if (query.contains("LIKE '%", ignoreCase = true)) {
            suggestions.add("LIKE演算子の前方一致（'text%'）を使用することでインデックスを活用できます")
        }
        
        if (query.contains("OR", ignoreCase = true)) {
            suggestions.add("OR条件をUNIONに変更することでパフォーマンスが向上する場合があります")
        }
        
        if (!query.contains("LIMIT", ignoreCase = true)) {
            suggestions.add("大量データの場合はLIMITを使用してページネーションを実装してください")
        }
        
        return suggestions
    }
}