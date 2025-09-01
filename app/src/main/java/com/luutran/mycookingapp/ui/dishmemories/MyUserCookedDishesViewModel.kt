package com.luutran.mycookingapp.ui.dishmemories

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.luutran.mycookingapp.data.model.UserRecipe
import com.luutran.mycookingapp.data.repository.UserRecipeRepository
import com.luutran.mycookingapp.data.repository.RepositoryResult
import com.luutran.mycookingapp.data.repository.fold
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// UI State specific to User Recipes
sealed interface MyUserCookedDishesUiState {
    data object Loading : MyUserCookedDishesUiState
    data class Success(val userRecipes: List<UserRecipe>) : MyUserCookedDishesUiState
    data class Error(val message: String) : MyUserCookedDishesUiState
}

class MyUserCookedDishesViewModel(
    private val userRecipeRepository: UserRecipeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MyUserCookedDishesUiState>(MyUserCookedDishesUiState.Loading)
    val uiState: StateFlow<MyUserCookedDishesUiState> = _uiState.asStateFlow()

    private val _sortOption = MutableStateFlow(CookedDishSortOption.defaultSort())
    val sortOption: StateFlow<CookedDishSortOption> = _sortOption.asStateFlow()

    // --- Selection Mode State ---
    private val _isSelectionModeActive = MutableStateFlow(false)
    val isSelectionModeActive: StateFlow<Boolean> = _isSelectionModeActive.asStateFlow()

    private val _selectedUserRecipeIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedUserRecipeIds: StateFlow<Set<String>> = _selectedUserRecipeIds.asStateFlow()

    private val _showDeleteConfirmationDialog = MutableStateFlow(false)
    val showDeleteConfirmationDialog: StateFlow<Boolean> = _showDeleteConfirmationDialog.asStateFlow()

    private val _showSortFilterSheet = MutableStateFlow(false)
    val showSortFilterSheet: StateFlow<Boolean> = _showSortFilterSheet.asStateFlow()


    init {
        Log.d("UserRecipesVM", "ViewModel Initialized. Setting up recipe collection.")
        userRecipeRepository.getUserRecipesFlow()
            .combine(_sortOption) { result, currentSortOption ->
                when (result) {
                    is RepositoryResult.Success -> {
                        Log.d("UserRecipesVM", "Flow emitted ${result.data.size} recipes. Sorting by $currentSortOption")
                        MyUserCookedDishesUiState.Success(result.data.applyUserRecipeSort(currentSortOption))
                    }
                    is RepositoryResult.Error -> {
                        Log.e("UserRecipesVM", "Error from recipes flow: ${result.exception.message}", result.exception)
                        MyUserCookedDishesUiState.Error(result.exception.message ?: "Failed to load your recipes")
                    }
                }
            }
            .onEach { newState -> _uiState.value = newState } // Update the UI state
            .catch { e ->
                Log.e("UserRecipesVM", "Critical error in recipe flow processing: ${e.message}", e)
                _uiState.value = MyUserCookedDishesUiState.Error("An unexpected error occurred: ${e.message}")
            }
            .launchIn(viewModelScope)
    }
    fun fetchMyUserCookedDishes() {
        viewModelScope.launch {
            Log.d("UserRecipesVM", "fetchMyUserCookedDishes called (manual refresh or retry)")
            if (_isSelectionModeActive.value) {
                exitSelectionMode()
            }
            _uiState.value = MyUserCookedDishesUiState.Loading
            userRecipeRepository.getUserRecipesForCurrentUserOnce()
                .fold(
                    onSuccess = { recipes ->
                        Log.d("UserRecipesVM", "Manual fetch success, count: ${recipes.size}. Flow should update UI.")
                        if (_uiState.value is MyUserCookedDishesUiState.Loading) {
                            _uiState.value = MyUserCookedDishesUiState.Success(recipes.applyUserRecipeSort(_sortOption.value))
                        }
                    },
                    onFailure = { exception ->
                        _uiState.value = MyUserCookedDishesUiState.Error(exception.message ?: "Failed to load your recipes")
                        Log.e("UserRecipesVM", "Manual fetch error", exception)
                    }
                )
        }
    }

    fun onSortOptionSelected(newSortOption: CookedDishSortOption) {
        _sortOption.value = newSortOption
    }

    fun openSortFilterSheet() { _showSortFilterSheet.value = true }
    fun closeSortFilterSheet() { _showSortFilterSheet.value = false }

    fun enterSelectionMode() { _isSelectionModeActive.value = true }

    fun exitSelectionMode() {
        _isSelectionModeActive.value = false
        _selectedUserRecipeIds.value = emptySet()
    }

    fun toggleUserRecipeSelection(recipeId: String) {
        _selectedUserRecipeIds.update { currentSelectedIds ->
            if (currentSelectedIds.contains(recipeId)) {
                currentSelectedIds - recipeId
            } else {
                currentSelectedIds + recipeId
            }
        }
    }

    fun requestDeleteConfirmation() {
        if (_selectedUserRecipeIds.value.isNotEmpty()) {
            _showDeleteConfirmationDialog.value = true
        }
    }

    fun cancelDeleteConfirmation() { _showDeleteConfirmationDialog.value = false }

    fun confirmDeleteSelectedUserRecipes() {
        _showDeleteConfirmationDialog.value = false
        if (_selectedUserRecipeIds.value.isEmpty()) {
            exitSelectionMode()
            return
        }
        viewModelScope.launch {
            val idsToDelete = _selectedUserRecipeIds.value.toList()
            val results = idsToDelete.map { userRecipeRepository.deleteUserRecipe(it) }

            val allSucceeded = results.all { it is RepositoryResult.Success }
            results.filterIsInstance<RepositoryResult.Error>().forEach { errorResult ->
                Log.e("UserRecipesVM", "Error deleting a user recipe: ${errorResult.exception.message}")
            }

            if (allSucceeded) {
                Log.d("UserRecipesVM", "Deletion successful for user recipe IDs: $idsToDelete")
            } else {
                _uiState.value = MyUserCookedDishesUiState.Error("Failed to delete some of your recipes.")
            }
            fetchMyUserCookedDishes()
        }
    }
}

fun List<UserRecipe>.applyUserRecipeSort(sort: CookedDishSortOption): List<UserRecipe> {
    val distantPast = Timestamp(-62135596800L, 0)
    val distantFutureSeconds = 253402300799L
    val distantFuture = Timestamp(distantFutureSeconds, 999999999)

    return when (sort) {
        CookedDishSortOption.NAME_ASCENDING -> sortedBy { it.title.lowercase() }
        CookedDishSortOption.NAME_DESCENDING -> sortedByDescending { it.title.lowercase() }
        CookedDishSortOption.FIRST_ADDED_NEWEST -> sortedWith(compareByDescending { it.createdAt ?: distantPast })
        CookedDishSortOption.FIRST_ADDED_OLDEST -> sortedWith(compareBy { it.createdAt ?: distantFuture })
        CookedDishSortOption.LAST_COOKED_NEWEST,
        CookedDishSortOption.LAST_COOKED_OLDEST,
        CookedDishSortOption.TIMES_COOKED_MOST,
        CookedDishSortOption.TIMES_COOKED_LEAST -> sortedBy { it.title.lowercase() }
    }
}

class MyUserCookedDishesViewModelFactory(
    private val userRecipeRepository: UserRecipeRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MyUserCookedDishesViewModel::class.java)) {
            return MyUserCookedDishesViewModel(userRecipeRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}