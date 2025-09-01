package com.luutran.mycookingapp.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.util.Log
import com.luutran.mycookingapp.data.repository.RecipeRepository

data class FavoritesScreenUiState(
    val isLoading: Boolean = true,
    val favoriteRecipes: List<FavoriteRecipeDisplayItem> = emptyList(),
    val error: String? = null,
    val selectionModeActive: Boolean = false,
    val selectedRecipeIds: Set<Int> = emptySet(),
    val showDeleteConfirmationDialog: Boolean = false,
    val currentSortOption: FavoriteSortOption = FavoriteSortOption.defaultSort()
)

class FavoritesViewModel(
    private val recipeRepository: RecipeRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    private val _error = MutableStateFlow<String?>(null)
    private val _selectionModeActive = MutableStateFlow(false)
    private val _selectedRecipeIds = MutableStateFlow<Set<Int>>(emptySet())
    private val _showDeleteConfirmationDialog = MutableStateFlow(false)

    // NEW: StateFlow for the selected sort option
    private val _selectedSortOption = MutableStateFlow(FavoriteSortOption.defaultSort())
    val selectedSortOption: StateFlow<FavoriteSortOption> = _selectedSortOption

    // Flow of favorite recipes from the repository
    private val _favoriteRecipesFlow: StateFlow<List<FavoriteRecipeDisplayItem>> =
        recipeRepository.getFirestoreFavoriteRecipes() // Use the Firestore version
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = emptyList()
            )

    val uiState: StateFlow<FavoritesScreenUiState> = combine(
        _favoriteRecipesFlow,
        _error,
        _selectionModeActive,
        _selectedRecipeIds,
        _showDeleteConfirmationDialog,
        _selectedSortOption
    ) { emissions ->
        // 'emissions' is an Array<Any?> where each element is the latest value from the corresponding flow.
        // We need to safely cast them to their expected types.

        @Suppress("UNCHECKED_CAST")
        val recipes = emissions[0] as List<FavoriteRecipeDisplayItem>
        val errorMsg = emissions[1] as String?
        val selectionActive = emissions[2] as Boolean
        @Suppress("UNCHECKED_CAST")
        val selectedIds = emissions[3] as Set<Int>
        val showDialog = emissions[4] as Boolean
        val sortOption = emissions[5] as FavoriteSortOption

        val stillInInitialLoadPhase = _isLoading.value
        val sortedRecipes = recipes.applySort(sortOption)

        FavoritesScreenUiState(
            isLoading = stillInInitialLoadPhase && recipes.isEmpty() && errorMsg == null,
            favoriteRecipes = sortedRecipes,
            error = errorMsg,
            selectionModeActive = selectionActive,
            selectedRecipeIds = selectedIds,
            showDeleteConfirmationDialog = showDialog,
            currentSortOption = sortOption
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = FavoritesScreenUiState(isLoading = true)
    )

    init {
        // Observe the flow to manage loading state more granularly
        viewModelScope.launch {
            _favoriteRecipesFlow.collect {
                // Consider loading finished once we get any emission (even empty after initial)
                // or if an error has occurred.
                if (_isLoading.value) { // Only update if still in initial loading phase
                    _isLoading.value = false
                }
                // If recipes list becomes empty NOT due to an error, it means user deleted all, or has no favorites.
                // This is fine. Loading state is primarily for the very first fetch.
            }
        }
    }

    // Function to update the sort option
    fun changeSortOption(newSortOption: FavoriteSortOption) {
        _selectedSortOption.value = newSortOption
    }

    fun toggleSelection(recipeId: Int) {
        _selectedRecipeIds.update { currentSelected ->
            if (currentSelected.contains(recipeId)) {
                currentSelected - recipeId
            } else {
                currentSelected + recipeId
            }
        }
        // If selection mode is active and this action de-selects the last item,
        // automatically turn off selection mode.
        if (_selectionModeActive.value && _selectedRecipeIds.value.isEmpty()){
            _selectionModeActive.value = false
        }
    }


    fun activateSelectionMode() {
        _selectionModeActive.value = true
    }

    fun deactivateSelectionMode() {
        _selectionModeActive.value = false
        _selectedRecipeIds.value = emptySet()
    }

    fun showDeleteConfirmation() {
        if (_selectedRecipeIds.value.isNotEmpty()) {
            _showDeleteConfirmationDialog.value = true
        }
    }

    fun dismissDeleteConfirmation() {
        _showDeleteConfirmationDialog.value = false
    }

    fun deleteSelectedFavorites() {
        val idsToDelete = _selectedRecipeIds.value
        if (idsToDelete.isEmpty()) {
            dismissDeleteConfirmation()
            deactivateSelectionMode()
            return
        }

        viewModelScope.launch {
            Log.d("FavoritesViewModel", "Attempting to delete favorites: $idsToDelete")
            try {
                // Assuming recipeRepository.removeFavorite is suspend and handles one ID
                idsToDelete.forEach { recipeId ->
                    recipeRepository.removeFavorite(recipeId) // This should update Firestore
                }
                Log.d("FavoritesViewModel", "Successfully initiated deletion of ${idsToDelete.size} favorites.")
                // The _favoriteRecipesFlow will automatically update from Firestore changes
            } catch (e: Exception) {
                Log.e("FavoritesViewModel", "Error deleting favorites: $idsToDelete", e)
                _error.value = "Failed to delete some favorites." // Show error to user
            } finally {
                dismissDeleteConfirmation()
                deactivateSelectionMode() // Clear selection and exit selection mode
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}