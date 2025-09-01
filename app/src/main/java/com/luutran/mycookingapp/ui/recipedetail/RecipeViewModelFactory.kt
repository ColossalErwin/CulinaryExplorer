package com.luutran.mycookingapp.ui.recipedetail

import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.luutran.mycookingapp.data.repository.CookedDishRepository
import com.luutran.mycookingapp.data.repository.RecipeRepository


class RecipeDetailViewModelFactory(
    owner: SavedStateRegistryOwner,
    private val recipeRepository: RecipeRepository,
    private val cookedDishRepository: CookedDishRepository, // Added
    defaultArgs: Bundle?
) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle
    ): T {
        if (modelClass.isAssignableFrom(RecipeDetailViewModel::class.java)) {
            return RecipeDetailViewModel(handle, recipeRepository, cookedDishRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}