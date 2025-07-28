package com.example.clothstock.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.clothstock.data.repository.ClothRepository

/**
 * DetailViewModel用のViewModelFactory
 * 
 * TDD Greenフェーズ実装
 * Repository依存性注入
 */
class DetailViewModelFactory(
    private val repository: ClothRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DetailViewModel::class.java)) {
            return DetailViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}