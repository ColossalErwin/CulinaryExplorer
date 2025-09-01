package com.luutran.mycookingapp.ui.recipedetail

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luutran.mycookingapp.data.model.RecipeDetail
import com.luutran.mycookingapp.data.repository.CookedDishRepository
import com.luutran.mycookingapp.data.repository.RecipeRepository
import com.luutran.mycookingapp.navigation.NavDestinations
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn

sealed interface RecipeDetailUiState {
    data object Loading : RecipeDetailUiState
    data class Success(
        val recipe: RecipeDetail,
        val timesCooked: Int = 0,
        val isFavorite: Boolean = false,
        val showFab: Boolean = true
    ) : RecipeDetailUiState

    data class Error(
        val message: String,
        val showFab: Boolean = true
    ) : RecipeDetailUiState
}

class RecipeDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val recipeRepository: RecipeRepository,
    private val cookedDishRepository: CookedDishRepository
) : ViewModel() {

    private val recipeId: Int = savedStateHandle.get<Int>("recipeId") ?: -1
    // Read showFab argument passed via navigation, default to true
    private val showFabFromArgs: Boolean = savedStateHandle.get<Boolean>(NavDestinations.RECIPE_DETAIL_ARG_SHOW_FAB) ?: true

    // Individual state flows for each piece of data
    private val _recipeDetailFlow = MutableStateFlow<RecipeDetail?>(null) // Holds the fetched RecipeDetail
    private val _timesCookedFlow = MutableStateFlow(0)
    private val _errorMessageFlow = MutableStateFlow<String?>(null) // For errors related to fetching main data

    // Flow to observe the favorite status from the repository
    private val isFavoriteFlow: StateFlow<Boolean> = if (recipeId != -1) {
        recipeRepository.isFavorite(recipeId)
            .catch { e ->
                Log.e("RecipeDetailVM", "Error observing favorite status for recipeId $recipeId", e)
                _errorMessageFlow.value = "Failed to load favorite status." // Optionally report this error
                emit(false) // Default to false on error
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), false)
    } else {
        MutableStateFlow(false).asStateFlow() // Default if recipeId is invalid
    }

    // Combine all data sources into a single UI state
    val uiState: StateFlow<RecipeDetailUiState> = combine(
        _recipeDetailFlow,
        _timesCookedFlow,
        isFavoriteFlow,
        _errorMessageFlow
    ) { recipe, timesCooked, isFavorite, errorMsg ->
        if (errorMsg != null && recipe == null) { // If there's an error and no recipe data, show error state
            RecipeDetailUiState.Error(errorMsg, showFab = this.showFabFromArgs)
        } else if (recipe != null) { // If recipe data is available, show success state
            RecipeDetailUiState.Success(recipe, timesCooked, isFavorite, showFab = this.showFabFromArgs)
        } else { // Otherwise, it's loading
            RecipeDetailUiState.Loading
        }
    }
        .distinctUntilChanged() // Only emit when the combined state actually changes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = RecipeDetailUiState.Loading // Initial state before anything loads
        )

    init {
        if (recipeId != -1) {
            Log.d("RecipeDetailVM", "Initializing for recipeId: $recipeId, showFab: $showFabFromArgs")
            fetchRecipeDetails()
            fetchCookCount()
        } else {
            _errorMessageFlow.value = "Recipe ID not found."
            Log.e("RecipeDetailVM", "CRITICAL: 'recipeId' is invalid (-1) from handle.")
        }
    }

    private fun fetchRecipeDetails() {
        viewModelScope.launch {
            // _errorMessageFlow.value = null // Clear previous errors for this specific fetch
            Log.d("RecipeDetailVM", "Fetching recipe details for recipeId: $recipeId")
            try {
                val recipe = recipeRepository.getRecipeById(recipeId) // Assumes this handles cache/network
                if (recipe != null) {
                    _recipeDetailFlow.value = recipe
                    _errorMessageFlow.value = null // Clear error on success
                    Log.d("RecipeDetailVM", "Successfully fetched recipe details: ${recipe.title}")
                } else {
                    _errorMessageFlow.value = "Recipe not found."
                    Log.w("RecipeDetailVM", "Recipe not found for recipeId: $recipeId")
                }
            } catch (e: Exception) {
                _errorMessageFlow.value = "Error fetching recipe details: ${e.message}"
                Log.e("RecipeDetailVM", "Error in fetchRecipeDetails", e)
            }
        }
    }

    private fun fetchCookCount() {
        if (recipeId == -1) return
        viewModelScope.launch {
            Log.d("RecipeDetailVM", "Fetching cook count for recipeId: $recipeId")
            cookedDishRepository.getCookedDishEntry(recipeId)
                .onSuccess { entry ->
                    val count = entry?.timesCooked ?: 0
                    _timesCookedFlow.value = count
                    Log.d("RecipeDetailVM", "Fetched timesCooked = $count for recipeId $recipeId")
                }
                .onFailure { exception ->
                    Log.e("RecipeDetailVM", "Failed to get cook count for $recipeId: ${exception.message}")
                }
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val currentRecipe = _recipeDetailFlow.value
            if (currentRecipe == null) {
                Log.w("RecipeDetailVM", "Cannot toggle favorite, recipe details not loaded.")
                _errorMessageFlow.value = "Recipe details not available to toggle favorite."
                return@launch
            }

            val currentlyFavorite = isFavoriteFlow.value // Get current status from the source of truth
            Log.d("RecipeDetailVM", "Toggling favorite for recipe ${currentRecipe.id}. Current status: $currentlyFavorite")

            try {
                if (currentlyFavorite) {
                    recipeRepository.removeFavorite(currentRecipe.id)
                    Log.i("RecipeDetailVM", "Recipe ${currentRecipe.id} removed from favorites.")
                } else {
                    recipeRepository.addFavorite(currentRecipe)
                    Log.i("RecipeDetailVM", "Recipe ${currentRecipe.id} added to favorites.")
                }
                _errorMessageFlow.value = null // Clear any previous errors if toggle is successful
            } catch (e: Exception) {
                Log.e("RecipeDetailVM", "Error toggling favorite for recipe ${currentRecipe.id}", e)
                _errorMessageFlow.value = "Could not update favorite status: ${e.localizedMessage}"
            }
        }
    }

    fun onCookDishFabClicked(
        recipeId: Int,
        recipeTitle: String,
        recipeImageUrl: String?,
        onSuccessNavigation: (recipeId: Int, recipeTitle: String, recipeImageUrl: String?) -> Unit
    ) {
        if (recipeTitle.isBlank()) {
            _errorMessageFlow.value = "Recipe title is blank, cannot proceed." // This error will update uiState via combine
            Log.e("RecipeDetailVM", "onCookDishFabClicked: Recipe title is blank for recipeId: $recipeId")
            return
        }

        if (recipeId != this.recipeId && this.recipeId != -1) {
            Log.w("RecipeDetailVM", "onCookDishFabClicked: recipeIdArg ($recipeId) differs from ViewModel's recipeId (${this.recipeId}). Proceeding with recipeIdArg.")
        }


        viewModelScope.launch {
            cookedDishRepository.ensureCookedDishEntryExists(recipeId, recipeTitle, recipeImageUrl)
                .onSuccess {
                    Log.d("RecipeDetailVM", "onCookDishFabClicked: Successfully ensured cooked dish entry for: $recipeId. Navigating...")
                    _errorMessageFlow.value = null
                    onSuccessNavigation(recipeId, recipeTitle, recipeImageUrl)
                }
                .onFailure { exception ->
                    Log.e("RecipeDetailVM", "onCookDishFabClicked: Failed to ensure cooked dish entry for: $recipeId", exception)
                    _errorMessageFlow.value = "Failed to prepare dish for memories: ${exception.message}"
                }
        }
    }

    fun refreshCookCountIfNeeded() {
        if (recipeId != -1) {
            Log.d("RecipeDetailVM", "refreshCookCountIfNeeded: Triggering cook count refresh for recipeId: $recipeId")
            fetchCookCount() // Re-fetch the cook count, _timesCookedFlow will update, then combine updates uiState
        }
    }
}