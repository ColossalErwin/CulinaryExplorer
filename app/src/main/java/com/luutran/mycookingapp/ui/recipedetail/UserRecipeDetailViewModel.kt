package com.luutran.mycookingapp.ui.recipedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.luutran.mycookingapp.data.model.UserRecipe // Your UserRecipe path
import com.luutran.mycookingapp.data.repository.RepositoryResult // Your RepositoryResult path
import com.luutran.mycookingapp.data.repository.UserRecipeRepository // Your UserRecipeRepository path
import com.luutran.mycookingapp.navigation.NavDestinations // Your NavDestinations path
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log

// --- UI State ---
sealed interface UserRecipeDetailUiState {
    data object Loading : UserRecipeDetailUiState
    data class Success(val userRecipe: UserRecipe) : UserRecipeDetailUiState
    data class Error(val message: String) : UserRecipeDetailUiState
}

// --- ViewModel ---
class UserRecipeDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val userRecipeRepository: UserRecipeRepository
) : ViewModel() {

    val userRecipeId: String = savedStateHandle[NavDestinations.USER_RECIPE_DETAIL_ARG_ID] ?: ""

    private val _uiState = MutableStateFlow<UserRecipeDetailUiState>(UserRecipeDetailUiState.Loading)
    val uiState: StateFlow<UserRecipeDetailUiState> = _uiState.asStateFlow()

    init {
        if (userRecipeId.isNotBlank()) {
            Log.d("UserRecipeDetailVM", "Initializing for UserRecipe ID: $userRecipeId")
            fetchUserRecipeDetails()
        } else {
            Log.e("UserRecipeDetailVM", "UserRecipe ID is blank in init.")
            _uiState.value = UserRecipeDetailUiState.Error("Recipe ID not found.")
        }
    }

    fun fetchUserRecipeDetails() {
        if (userRecipeId.isBlank()) {
            _uiState.value = UserRecipeDetailUiState.Error("Cannot fetch details: Recipe ID is blank.")
            return
        }
        viewModelScope.launch {
            _uiState.value = UserRecipeDetailUiState.Loading
            when (val result = userRecipeRepository.getUserRecipe(userRecipeId)) {
                is RepositoryResult.Success -> {
                    Log.d("UserRecipeDetailVM", "Successfully fetched UserRecipe: ${result.data.title}")
                    _uiState.value = UserRecipeDetailUiState.Success(result.data)
                }
                is RepositoryResult.Error -> {
                    val errorMsg = "Error fetching user recipe: ${result.exception.message}"
                    Log.e("UserRecipeDetailVM", errorMsg, result.exception)
                    _uiState.value = UserRecipeDetailUiState.Error(errorMsg)
                }
            }
        }
    }

    // Companion object for ViewModel Factory
    companion object {
        fun provideFactory(
            userRecipeRepository: UserRecipeRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                if (modelClass.isAssignableFrom(UserRecipeDetailViewModel::class.java)) {
                    val savedStateHandle = extras.createSavedStateHandle()
                    return UserRecipeDetailViewModel(savedStateHandle, userRecipeRepository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}