package com.luutran.mycookingapp.ui.dishmemories

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.luutran.mycookingapp.data.model.DishMemory
import com.luutran.mycookingapp.data.repository.CookedDishRepository
import com.luutran.mycookingapp.navigation.NavDestinations
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.nio.charset.StandardCharsets


sealed interface UserRecipeMemoryDetailUiState {
    data object Loading : UserRecipeMemoryDetailUiState
    data class Success(
        val memory: DishMemory,
        val userRecipeTitle: String,
        val userRecipeId: String
    ) : UserRecipeMemoryDetailUiState
    data class Error(val message: String, val userRecipeTitle: String? = null) : UserRecipeMemoryDetailUiState
}

class UserRecipeMemoryDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val cookedDishRepository: CookedDishRepository
) : ViewModel() {

    // Arguments from navigation
    val userRecipeId: String = savedStateHandle.get<String>(NavDestinations.USER_RECIPE_MEMORY_DETAIL_ARG_USER_RECIPE_ID) ?: ""
    val memoryId: String = savedStateHandle.get<String>(NavDestinations.USER_RECIPE_MEMORY_DETAIL_ARG_MEMORY_ID) ?: ""
    private val encodedUserRecipeTitle: String = savedStateHandle.get<String>(NavDestinations.USER_RECIPE_MEMORY_DETAIL_ARG_USER_RECIPE_TITLE) ?: "Memory Details"

    val userRecipeTitle: String = try {
        URLDecoder.decode(encodedUserRecipeTitle, StandardCharsets.UTF_8.toString())
    } catch (e: Exception) {
        Log.e("UserRecipeMemoryDetailVM", "Failed to decode user recipe title", e)
        "Memory Details" // Fallback
    }

    private val _uiState = MutableStateFlow<UserRecipeMemoryDetailUiState>(UserRecipeMemoryDetailUiState.Loading)
    val uiState: StateFlow<UserRecipeMemoryDetailUiState> = _uiState.asStateFlow()

    init {
        if (userRecipeId.isNotBlank() && memoryId.isNotBlank()) {
            loadUserRecipeMemoryDetails()
        } else {
            _uiState.value = UserRecipeMemoryDetailUiState.Error("Invalid User Recipe or Memory ID.", userRecipeTitle)
            Log.e("UserRecipeMemoryDetailVM", "Invalid IDs in init: userRecipeId=$userRecipeId, memoryId=$memoryId")
        }
    }

    private fun loadUserRecipeMemoryDetails() {
        viewModelScope.launch {
            _uiState.value = UserRecipeMemoryDetailUiState.Loading
            val result = cookedDishRepository.getMemoryForUserRecipeById(userRecipeId, memoryId)

            result.fold(
                onSuccess = { specificMemory ->
                    if (specificMemory != null) {
                        _uiState.value = UserRecipeMemoryDetailUiState.Success(
                            memory = specificMemory,
                            userRecipeTitle = userRecipeTitle, // Use the decoded title
                            userRecipeId = userRecipeId
                        )
                    } else {
                        _uiState.value = UserRecipeMemoryDetailUiState.Error("Memory not found for this User Recipe.", userRecipeTitle)
                    }
                },
                onFailure = { exception ->
                    _uiState.value = UserRecipeMemoryDetailUiState.Error(
                        "Failed to load memory: ${exception.message}",
                        userRecipeTitle
                    )
                    Log.e("UserRecipeMemoryDetailVM", "Error loading memory details", exception)
                }
            )
        }
    }

    fun deleteThisUserRecipeMemory(onFinished: (Boolean) -> Unit) {
        if (userRecipeId.isBlank() || memoryId.isBlank()) {
            Log.w("UserRecipeMemoryDetailVM", "Cannot delete, invalid userRecipeId/memoryId")
            onFinished(false)
            return
        }
        viewModelScope.launch {
            val result = cookedDishRepository.deleteMemoriesForUserRecipe(userRecipeId, listOf(memoryId)) // Example method

            result.fold(
                onSuccess = {
                    Log.d("UserRecipeMemoryDetailVM", "Successfully deleted memory $memoryId for user recipe $userRecipeId")
                    onFinished(true)
                },
                onFailure = { exception ->
                    Log.e("UserRecipeMemoryDetailVM", "Failed to delete memory $memoryId for user recipe $userRecipeId", exception)
                    // Update UI state with error only if it's not already Error (to avoid overwriting other errors)
                    if (_uiState.value !is UserRecipeMemoryDetailUiState.Error) {
                        _uiState.value = UserRecipeMemoryDetailUiState.Error("Failed to delete memory: ${exception.message}", userRecipeTitle)
                    }
                    onFinished(false)
                }
            )
        }
    }
    // Public function to allow the screen to trigger a refresh
    fun refreshMemoryDetails() {
        Log.d("UserRecipeMemoryDetailVM", "Refreshing memory details for $userRecipeId, $memoryId")
        // Only reload if valid IDs are present
        if (userRecipeId.isNotBlank() && memoryId.isNotBlank()) {
            loadUserRecipeMemoryDetails() // Re-use the existing private load function
        } else {
            Log.w("UserRecipeMemoryDetailVM", "Skipping refresh due to invalid IDs.")
        }
    }
}

// ViewModel Factory
class UserRecipeMemoryDetailViewModelFactory(
    private val cookedDishRepository: CookedDishRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (modelClass.isAssignableFrom(UserRecipeMemoryDetailViewModel::class.java)) {
            val savedStateHandle = extras.createSavedStateHandle()
            @Suppress("UNCHECKED_CAST")
            return UserRecipeMemoryDetailViewModel(savedStateHandle, cookedDishRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}