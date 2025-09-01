package com.luutran.mycookingapp.ui.dishmemories

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luutran.mycookingapp.data.model.DishMemory
import com.luutran.mycookingapp.data.repository.CookedDishRepository
import com.luutran.mycookingapp.navigation.NavDestinations
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

sealed interface DishMemoriesUiState {
    data object Loading : DishMemoriesUiState
    data class Success(
        val memories: List<DishMemory>,
        val recipeTitle: String,
        val recipeId: Int,
        val recipeImageUrl: String?,
        val selectionMode: Boolean = false,
        val selectedMemoryIds: Set<String> = emptySet(),
        val showDeleteConfirmationDialog: Boolean = false,
        val currentSortOption: DishMemorySortOption
    ) : DishMemoriesUiState

    data class Error(
        val message: String,
        val recipeTitle: String? = null,
        val recipeImageUrl: String? = null
    ) : DishMemoriesUiState
}

class DishMemoriesListViewModel(
    savedStateHandle: SavedStateHandle,
    private val cookedDishRepository: CookedDishRepository
) : ViewModel() {

    val recipeId: Int = savedStateHandle.get<Int>(NavDestinations.DISH_MEMORIES_LIST_ARG_RECIPE_ID) ?: -1
    private val encodedRecipeTitle: String = savedStateHandle.get<String>(NavDestinations.DISH_MEMORIES_LIST_ARG_RECIPE_TITLE) ?: ""
    val recipeTitle: String = try {
        URLDecoder.decode(encodedRecipeTitle, StandardCharsets.UTF_8.toString())
    } catch (e: Exception) {
        Log.e("DishMemoriesVM", "Failed to decode recipe title: '$encodedRecipeTitle'", e)
        "Recipe"
    }

    private val encodedRecipeImageUrl: String? = savedStateHandle.get<String>(NavDestinations.DISH_MEMORIES_LIST_ARG_RECIPE_IMAGE_URL)
    val recipeImageUrl: String? = try {
        encodedRecipeImageUrl?.let {
            if (it == "null_image_url") null else URLDecoder.decode(it, StandardCharsets.UTF_8.toString())
        }
    } catch (e: Exception) {
        Log.e("DishMemoriesVM", "Failed to decode recipe image URL: '$encodedRecipeImageUrl'", e)
        null
    }

    private val _error = MutableStateFlow<String?>(null)
    private val _selectionMode = MutableStateFlow(false)
    private val _selectedMemoryIds = MutableStateFlow<Set<String>>(emptySet())
    private val _showDeleteConfirmationDialog = MutableStateFlow(false) // <-- NEW: Internal dialog state


    // --- Sort Option State ---
    private val _sortOption = MutableStateFlow(DishMemorySortOption.defaultSort())
    val currentSortOption: StateFlow<DishMemorySortOption> = _sortOption.asStateFlow()

    private val _rawMemoriesFlow: Flow<Result<List<DishMemory>>> = // Renamed for clarity
        if (recipeId != -1) {
            cookedDishRepository.getDishMemories(recipeId)
        } else {
            flowOf(Result.failure(IllegalArgumentException("Recipe ID is invalid for fetching memories.")))
        }

    val uiState: StateFlow<DishMemoriesUiState> = combine(
        _rawMemoriesFlow,
        _sortOption,
        _error,
        _selectionMode,
        _selectedMemoryIds,
        _showDeleteConfirmationDialog
    ) { flows ->
        val memoriesResult = flows[0] as? Result<*>
        val sortOption = flows[1] as? DishMemorySortOption
        val error = flows[2] as? String?
        val selectionMode = flows[3] as? Boolean
        val selectedIds = flows[4] as? Set<*>
        val showDialog = flows[5] as? Boolean

        // Check if all essential non-nullable casts succeeded
        if (memoriesResult == null || sortOption == null || selectionMode == null || selectedIds == null || showDialog == null) {
            Log.e("DishMemoriesVM", "Combine operator received unexpected null after safe cast.")
            DishMemoriesUiState.Error("Internal error processing data.", this.recipeTitle, this.recipeImageUrl)
        } else {

            @Suppress("UNCHECKED_CAST")
            val typedMemoriesResult = memoriesResult as Result<List<DishMemory>>
            @Suppress("UNCHECKED_CAST")
            val typedSelectedIds = selectedIds as Set<String>

            if (error != null) {
                DishMemoriesUiState.Error(error, recipeTitle, recipeImageUrl)
            } else {
                typedMemoriesResult.fold(
                    onSuccess = { rawMemories ->
                        val sortedMemories = rawMemories.applyMemorySort(sortOption)
                        DishMemoriesUiState.Success(
                            memories = sortedMemories,
                            recipeTitle = this.recipeTitle,
                            recipeId = this.recipeId,
                            recipeImageUrl = this.recipeImageUrl,
                            selectionMode = selectionMode,
                            selectedMemoryIds = typedSelectedIds, // Use the more safely casted version
                            showDeleteConfirmationDialog = showDialog,
                            currentSortOption = sortOption
                        )
                    },
                    onFailure = { exception ->
                        Log.e("DishMemoriesVM", "Error from _rawMemoriesFlow: ${exception.message}")
                        DishMemoriesUiState.Error(
                            exception.message ?: "Failed to load memories.",
                            this.recipeTitle,
                            this.recipeImageUrl
                        )
                    }
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DishMemoriesUiState.Loading
    )

    // --- Function to update sort option ---
    fun setSortOption(sortOption: DishMemorySortOption) {
        _sortOption.value = sortOption
    }

    init {
        if (recipeId == -1) {
            _error.value = "Recipe ID not found. Cannot load memories."
            Log.e("DishMemoriesListVM", "Recipe ID is -1 in init.")
        }
        Log.d("DishMemoriesVM", "Init - Recipe ID: $recipeId, Encoded Title: '$encodedRecipeTitle', Decoded Title: '$recipeTitle'")
        Log.d("DishMemoriesVM", "Init - Encoded Image URL: '$encodedRecipeImageUrl', Decoded Image URL: '$recipeImageUrl'")
    }

    // --- SELECTION MODE ---
    fun enterSelectionMode(initialMemoryId: String? = null) {
        _selectionMode.value = true
        initialMemoryId?.let {
            _selectedMemoryIds.value = setOf(it)
        }
    }

    fun exitSelectionMode() {
        _selectionMode.value = false
        _selectedMemoryIds.value = emptySet()
    }


    fun cancelSelectionMode() {
        exitSelectionMode()
    }

    fun toggleMemorySelection(memoryId: String) {
        if (!_selectionMode.value) {
            enterSelectionMode(memoryId)
        } else {
            val currentSelected = _selectedMemoryIds.value.toMutableSet()
            if (currentSelected.contains(memoryId)) {
                currentSelected.remove(memoryId)
            } else {
                currentSelected.add(memoryId)
            }
            _selectedMemoryIds.value = currentSelected
        }
    }

    // --- DELETE LOGIC ---
    fun requestDeleteSelectedMemories() {
        if (_selectedMemoryIds.value.isNotEmpty()) {
            _showDeleteConfirmationDialog.value = true
        }
    }

    fun confirmDeleteSelectedMemories() {
        val idsToDelete = _selectedMemoryIds.value.toList()
        if (idsToDelete.isEmpty() || recipeId == -1) {
            _showDeleteConfirmationDialog.value = false
            return
        }

        viewModelScope.launch {
            _error.value = null
            val result = cookedDishRepository.deleteDishMemories(recipeId, idsToDelete)
            result
                .onSuccess {
                    Log.d("DishMemoriesListVM", "Successfully deleted memories: $idsToDelete")
                }
                .onFailure { exception ->
                    Log.e("DishMemoriesListVM", "Failed to delete memories", exception)
                    _error.value = "Error deleting memories: ${exception.message}"
                }
            _showDeleteConfirmationDialog.value = false
            exitSelectionMode()
        }
    }

    fun cancelDeleteConfirmation() {
        _showDeleteConfirmationDialog.value = false
    }

    fun clearError() {
        _error.value = null
    }
}