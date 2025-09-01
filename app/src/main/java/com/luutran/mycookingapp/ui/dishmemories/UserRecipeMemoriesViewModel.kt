package com.luutran.mycookingapp.ui.dishmemories

import android.util.Log
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.CreationExtras
import com.luutran.mycookingapp.data.model.DishMemory
import com.luutran.mycookingapp.data.repository.UserRecipeRepository
import com.luutran.mycookingapp.data.repository.RepositoryResult
import com.luutran.mycookingapp.data.repository.fold
import com.luutran.mycookingapp.navigation.NavDestinations
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

sealed interface UserRecipeMemoriesUiState {
    data object Loading : UserRecipeMemoriesUiState
    data class Success(
        val memories: List<DishMemory>,
        val userRecipeTitle: String,
        val userRecipeId: String, // String ID for user recipes
        val userRecipeImageUrl: String?,
        val selectionMode: Boolean = false,
        val selectedMemoryIds: Set<String> = emptySet(),
        val showDeleteConfirmationDialog: Boolean = false,
        val currentSortOption: DishMemorySortOption
    ) : UserRecipeMemoriesUiState

    data class Error(
        val message: String,
        val userRecipeTitle: String? = null,
        val userRecipeImageUrl: String? = null
    ) : UserRecipeMemoriesUiState
}

class UserRecipeMemoriesViewModel(
    savedStateHandle: SavedStateHandle,
    private val userRecipeRepository: UserRecipeRepository
) : ViewModel() {

    val userRecipeId: String = savedStateHandle.get<String>(NavDestinations.USER_RECIPE_MEMORIES_LIST_ARG_USER_RECIPE_ID) ?: ""

    // Use List<String> for image URLs
    private val _dynamicUserRecipeTitle = MutableStateFlow("")
    private val _dynamicUserRecipeImageUrls = MutableStateFlow<List<String>>(emptyList())

    // ... _error, _selectionMode, _selectedMemoryIds, _showDeleteConfirmationDialog, _sortOption ...
    private val _error = MutableStateFlow<String?>(null)
    private val _selectionMode = MutableStateFlow(false)
    private val _selectedMemoryIds = MutableStateFlow<Set<String>>(emptySet())
    private val _showDeleteConfirmationDialog = MutableStateFlow(false)
    private val _sortOption = MutableStateFlow(DishMemorySortOption.defaultSort())

    private val _rawMemoriesFlow: Flow<RepositoryResult<List<DishMemory>>> =
        if (userRecipeId.isNotBlank()) {
            userRecipeRepository.getMemoriesForUserRecipe(userRecipeId)
        } else {
            flowOf(RepositoryResult.Error(IllegalArgumentException("User Recipe ID is invalid.")))
        }

    val uiState: StateFlow<UserRecipeMemoriesUiState> = combine(
        _rawMemoriesFlow,
        _sortOption,
        _error,
        _selectionMode,
        _selectedMemoryIds,
        _showDeleteConfirmationDialog,
        _dynamicUserRecipeTitle,
        _dynamicUserRecipeImageUrls // Use the flow for List<String>
    ) { flows ->
        val memoriesRepoResult = flows[0] as RepositoryResult<List<DishMemory>>
        val sortOption = flows[1] as DishMemorySortOption
        val currentError = flows[2] as? String
        val selectionMode = flows[3] as Boolean
        val selectedIds = flows[4] as Set<String>
        val showDialog = flows[5] as Boolean
        val recipeTitleFromFlow = flows[6] as String
        val recipeImageUrlsFromFlow = flows[7] as List<String>

        if (currentError != null) {
            UserRecipeMemoriesUiState.Error(
                currentError,
                recipeTitleFromFlow,
                recipeImageUrlsFromFlow.firstOrNull()
            )
        } else {
            memoriesRepoResult.fold(
                onSuccess = { rawMemories ->
                    val sortedMemories = rawMemories.applyMemorySort(sortOption)
                    UserRecipeMemoriesUiState.Success(
                        memories = sortedMemories,
                        userRecipeTitle = recipeTitleFromFlow,
                        userRecipeId = this.userRecipeId,
                        userRecipeImageUrl = recipeImageUrlsFromFlow.firstOrNull(),
                        selectionMode = selectionMode,
                        selectedMemoryIds = selectedIds,
                        showDeleteConfirmationDialog = showDialog,
                        currentSortOption = sortOption
                    )
                },
                onFailure = { exception ->
                    Log.e("UserRecipeMemoriesVM", "Error from _rawMemoriesFlow for $userRecipeId: ${exception.message}")
                    UserRecipeMemoriesUiState.Error(
                        exception.message ?: "Failed to load memories.",
                        recipeTitleFromFlow,
                        recipeImageUrlsFromFlow.firstOrNull()
                    )
                }
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserRecipeMemoriesUiState.Loading
    )

    init {
        val initialEncodedTitle: String = savedStateHandle.get<String>(NavDestinations.USER_RECIPE_MEMORIES_LIST_ARG_USER_RECIPE_TITLE) ?: ""
        val initialTitle = try {
            URLDecoder.decode(initialEncodedTitle, StandardCharsets.UTF_8.toString())
        } catch (e: Exception) {
            Log.e("UserRecipeMemoriesVM", "Failed to decode recipe title: '$initialEncodedTitle'", e)
            "User Recipe"
        }
        _dynamicUserRecipeTitle.value = initialTitle

        val imageUrlsStringFromArgs: String? = savedStateHandle.get<String>(NavDestinations.USER_RECIPE_MEMORIES_LIST_ARG_USER_RECIPE_IMAGE_URL)
        val initialImageUrls = if (imageUrlsStringFromArgs == "null_image_url" || imageUrlsStringFromArgs.isNullOrBlank()) {
            emptyList()
        } else {
            listOf(imageUrlsStringFromArgs)
        }
        _dynamicUserRecipeImageUrls.value = initialImageUrls

        if (userRecipeId.isBlank()) {
            Log.e("UserRecipeMemoriesVM", "User Recipe ID is blank in init.")
        }
        Log.d("UserRecipeMemoriesVM", "Init - UserRecipe ID: $userRecipeId, Title: '$initialTitle', Initial ImageURLs: $initialImageUrls")

        if (userRecipeId.isNotBlank()) {
            refreshRecipeDetailsIfNeeded()
        }
    }

    fun refreshRecipeDetailsIfNeeded() {
        if (userRecipeId.isBlank()) return

        viewModelScope.launch {
            Log.d("UserRecipeMemoriesVM", "refreshRecipeDetailsIfNeeded called for $userRecipeId")
            when (val result = userRecipeRepository.getUserRecipe(userRecipeId)) {
                is RepositoryResult.Success -> {
                    val updatedRecipe = result.data
                    Log.d("UserRecipeMemoriesVM", "Refreshed recipe details: Title='${updatedRecipe.title}', ImageURLs='${updatedRecipe.imageUrls}'")

                    if (_dynamicUserRecipeTitle.value != updatedRecipe.title) {
                        _dynamicUserRecipeTitle.value = updatedRecipe.title
                    }

                    if (_dynamicUserRecipeImageUrls.value != updatedRecipe.imageUrls) {
                        _dynamicUserRecipeImageUrls.value = updatedRecipe.imageUrls
                    }
                }
                is RepositoryResult.Error -> {
                    Log.e("UserRecipeMemoriesVM", "Failed to refresh recipe details for $userRecipeId", result.exception)
                }
            }
        }
    }

    fun setSortOption(sortOption: DishMemorySortOption) {
        _sortOption.value = sortOption
    }

    fun enterSelectionMode(initialMemoryId: String? = null) {
        _selectionMode.value = true
        initialMemoryId?.let { _selectedMemoryIds.value = setOf(it) }
    }

    fun exitSelectionMode() {
        _selectionMode.value = false
        _selectedMemoryIds.value = emptySet()
    }

    fun cancelSelectionMode() = exitSelectionMode()

    fun toggleMemorySelection(memoryId: String) {
        if (!_selectionMode.value) {
            enterSelectionMode(memoryId)
        } else {
            val currentSelected = _selectedMemoryIds.value.toMutableSet()
            if (currentSelected.contains(memoryId)) currentSelected.remove(memoryId)
            else currentSelected.add(memoryId)
            _selectedMemoryIds.value = currentSelected
        }
    }

    fun requestDeleteSelectedMemories() {
        if (_selectedMemoryIds.value.isNotEmpty()) {
            _showDeleteConfirmationDialog.value = true
        }
    }

    fun confirmDeleteSelectedMemories() {
        val idsToDelete = _selectedMemoryIds.value.toList()
        if (idsToDelete.isEmpty() || userRecipeId.isBlank()) {
            _showDeleteConfirmationDialog.value = false
            return
        }

        viewModelScope.launch {
            _error.value = null
            val result = userRecipeRepository.deleteMemoriesForUserRecipe(userRecipeId, idsToDelete)
            result.fold(
                onSuccess = {
                    Log.d("UserRecipeMemoriesVM", "Successfully deleted memories for $userRecipeId: $idsToDelete")
                },
                onFailure = { exception ->
                    Log.e("UserRecipeMemoriesVM", "Failed to delete memories for $userRecipeId", exception)
                    _error.value = "Error deleting memories: ${exception.message}"
                }
            )
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


    companion object {
        fun factory(
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                val userRecipeRepository = UserRecipeRepository()
                val savedStateHandle = extras.createSavedStateHandle()
                return UserRecipeMemoriesViewModel(savedStateHandle, userRecipeRepository) as T
            }
        }
    }
}