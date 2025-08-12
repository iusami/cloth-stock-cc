package com.example.clothstock.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.clothstock.data.repository.ClothRepository
import com.example.clothstock.data.preferences.DetailPreferencesManager

/**
 * DetailViewModel用のViewModelFactory
 * 
 * TDD Greenフェーズ実装
 * Repository依存性注入
 */
class DetailViewModelFactory(
    private val repository: ClothRepository,
    private val preferencesManager: DetailPreferencesManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DetailViewModel::class.java)) {
            return DetailViewModel(repository, preferencesManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}