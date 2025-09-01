package com.luutran.mycookingapp.ui.dishmemories

import android.util.Log
import androidx.activity.result.launch
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.get
import androidx.lifecycle.viewModelScope
import com.luutran.mycookingapp.data.model.DishMemory
import com.luutran.mycookingapp.data.repository.CookedDishRepository
import com.luutran.mycookingapp.navigation.NavDestinations
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

sealed interface DishMemoryUiState {
    data object Loading : DishMemoryUiState
    data class Success(
        val memory: DishMemory,
        val recipeTitle: String,
        val recipeId: Int
    ) : DishMemoryUiState
    data class Error(val message: String, val recipeTitle: String? = null) : DishMemoryUiState
}

class DishMemoryViewModel(
    savedStateHandle: SavedStateHandle,
    private val cookedDishRepository: CookedDishRepository
) : ViewModel() {

    val recipeId: Int = savedStateHandle.get<Int>(NavDestinations.DISH_MEMORY_DETAIL_ARG_RECIPE_ID) ?: -1
    val memoryId: String = savedStateHandle.get<String>(NavDestinations.DISH_MEMORY_DETAIL_ARG_MEMORY_ID) ?: ""
    private val encodedRecipeTitle: String = savedStateHandle.get<String>(NavDestinations.DISH_MEMORY_DETAIL_ARG_RECIPE_TITLE) ?: ""

    val recipeTitle: String = try {
        URLDecoder.decode(encodedRecipeTitle, StandardCharsets.UTF_8.toString())
    } catch (e: Exception) {
        Log.e("DishMemoryVM", "Failed to decode recipe title", e)
        "Memory Details" // Fallback
    }

    private val _uiState = MutableStateFlow<DishMemoryUiState>(DishMemoryUiState.Loading)
    val uiState: StateFlow<DishMemoryUiState> = _uiState.asStateFlow()

    init {
        if (recipeId != -1 && memoryId.isNotBlank()) {
            loadMemoryDetails()
        } else {
            _uiState.value = DishMemoryUiState.Error("Invalid recipe or memory ID.", recipeTitle)
            Log.e("DishMemoryVM", "Invalid IDs in init: recipeId=$recipeId, memoryId=$memoryId")
        }
    }

    private fun loadMemoryDetails() {
        viewModelScope.launch {
            _uiState.value = DishMemoryUiState.Loading
            cookedDishRepository.getDishMemories(recipeId)
                .map { result ->
                    result.fold(
                        onSuccess = { memories ->
                            val specificMemory = memories.find { it.id == memoryId }
                            if (specificMemory != null) {
                                DishMemoryUiState.Success(specificMemory, recipeTitle, recipeId)
                            } else {
                                DishMemoryUiState.Error("Memory not found.", recipeTitle)
                            }
                        },
                        onFailure = {
                            DishMemoryUiState.Error("Failed to load memory: ${it.message}", recipeTitle)
                        }
                    )
                }
                .catch { e ->
                    Log.e("DishMemoryVM", "Error loading memory details", e)
                    emit(DishMemoryUiState.Error("An unexpected error occurred: ${e.message}", recipeTitle))
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    fun deleteThisMemory(onFinished: (Boolean) -> Unit) {
        if (recipeId == -1 || memoryId.isBlank()) {
            Log.w("DishMemoryVM", "Cannot delete, invalid recipe/memory ID")
            onFinished(false)
            return
        }
        viewModelScope.launch {
            val result = cookedDishRepository.deleteDishMemories(recipeId, listOf(memoryId))
            result.onSuccess {
                Log.d("DishMemoryVM", "Successfully deleted memory $memoryId")
                // Navigate back or signal success
                onFinished(true)
            }.onFailure {
                Log.e("DishMemoryVM", "Failed to delete memory $memoryId", it)
                _uiState.value = DishMemoryUiState.Error("Failed to delete memory: ${it.message}", recipeTitle)
                onFinished(false)
            }
        }
    }
}

class DishMemoryViewModelFactory(
    private val cookedDishRepository: CookedDishRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: androidx.lifecycle.viewmodel.CreationExtras): T {
        if (modelClass.isAssignableFrom(DishMemoryViewModel::class.java)) {
            val savedStateHandle = extras.createSavedStateHandle()
            @Suppress("UNCHECKED_CAST")
            return DishMemoryViewModel(savedStateHandle, cookedDishRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}