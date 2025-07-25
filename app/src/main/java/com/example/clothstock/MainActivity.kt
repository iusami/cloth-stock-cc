package com.example.clothstock

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.clothstock.databinding.ActivityMainBinding

/**
 * cloth-stock アプリケーションのメインアクティビティ
 * 
 * 衣服管理アプリの中央ハブとして機能し、以下の機能への導線を提供：
 * - カメラ機能（写真撮影）
 * - ギャラリー（衣服アイテム一覧）
 * - 詳細表示・タグ編集
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // ViewBinding セットアップ
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // ナビゲーションの初期化
        setupNavigation()
    }
    
    /**
     * アプリ内ナビゲーションの初期化
     */
    private fun setupNavigation() {
        // TODO: Navigation Componentを使用したフラグメント間の遷移を実装予定
        // 現在は基本的なActivity構造のみ設定
    }
}