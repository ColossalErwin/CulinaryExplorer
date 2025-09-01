package com.luutran.mycookingapp.ui.mealplanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.luutran.mycookingapp.data.model.RecipeSummary
import com.luutran.mycookingapp.data.repository.RecipeRepository
import com.luutran.mycookingapp.ui.searchresults.AppliedSearchFilters
import com.luutran.mycookingapp.ui.searchresults.DietOption
import com.luutran.mycookingapp.ui.searchresults.RecipeSortOption
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.text.isNotBlank
import kotlin.text.isNotEmpty

sealed interface MealPlannerSearchResultsUiState {
    data object Idle : MealPlannerSearchResultsUiState // No search performed yet
    data object Loading : MealPlannerSearchResultsUiState
    data class Success(
        val recipes: List<RecipeSummary>,
        val totalResults: Int,
        val canLoadMore: Boolean
    ) : MealPlannerSearchResultsUiState
    data object NoResults : MealPlannerSearchResultsUiState // Search performed, but no results
    data class Error(val message: String) : MealPlannerSearchResultsUiState
}

@OptIn(FlowPreview::class)
class MealPlannerSearchViewModel(
    private val recipeRepository: RecipeRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _appliedFilters = MutableStateFlow(AppliedSearchFilters())
    val appliedFilters: StateFlow<AppliedSearchFilters> = _appliedFilters.asStateFlow()

    private val _sortOption = MutableStateFlow(RecipeSortOption.RELEVANCE) // Default sort
    val sortOption: StateFlow<RecipeSortOption> = _sortOption.asStateFlow()

    private val _searchResultsUiState =
        MutableStateFlow<MealPlannerSearchResultsUiState>(MealPlannerSearchResultsUiState.Idle)
    val searchResultsUiState: StateFlow<MealPlannerSearchResultsUiState> = _searchResultsUiState.asStateFlow()

    private var currentSearchJob: Job? = null
    private var currentOffset = 0
    private val resultsPerPage = 20 // Spoonacular default, good for pagination

    private val debouncePeriodMillis = 700L

    init {
        // Combine query, filters, and sort to trigger search
        viewModelScope.launch {
            combine(
                _searchQuery.debounce(debouncePeriodMillis),
                _appliedFilters,
                _sortOption
            ) { query, filters, sort ->
                Triple(query, filters, sort)
            }
                .distinctUntilChanged()
                .collectLatest { (query, filters, sort) ->
                    // Only search if query is not blank OR active filters are present
                    if (query.isNotBlank() || filters.hasActiveFilters()) {
                        performSearch(query, filters, sort, isNewSearch = true)
                    } else {
                        _searchResultsUiState.value = MealPlannerSearchResultsUiState.Idle
                        currentOffset = 0 // Reset offset if search criteria cleared
                    }
                }
        }
    }

    fun onQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onFiltersChanged(newFilters: AppliedSearchFilters) {
        _appliedFilters.value = newFilters
    }

    fun onSortChanged(newSort: RecipeSortOption) {
        _sortOption.value = newSort
    }

    fun performSearch(
        query: String = _searchQuery.value,
        filters: AppliedSearchFilters = _appliedFilters.value,
        sort: RecipeSortOption = _sortOption.value,
        isNewSearch: Boolean = false // True if filters/query changed, false if loading more
    ) {
        currentSearchJob?.cancel()
        currentSearchJob = viewModelScope.launch {
            if (isNewSearch) {
                currentOffset = 0 // Reset for new search
                _searchResultsUiState.value = MealPlannerSearchResultsUiState.Loading
            } else {
                // If loading more, ensure previous state was Success to append
                if (_searchResultsUiState.value !is MealPlannerSearchResultsUiState.Success) {
                    _searchResultsUiState.value = MealPlannerSearchResultsUiState.Loading // Show loading if previous state wasn't success
                }
            }

            // Small delay to ensure UI updates to Loading before network call
            delay(50)

            try {
                val response = recipeRepository.searchRecipes(
                    query = query,
                    sort = sort.spoonacularSortValue,
                    diet = filters.diet.takeIf { it != DietOption.NONE }?.spoonacularValue,
                    intolerances = filters.intolerances.takeIf { it.isNotEmpty() }
                        ?.joinToString(",") { it.spoonacularValue },
                    cuisine = filters.cuisines.takeIf { it.isNotEmpty() }
                        ?.joinToString(",") { it.spoonacularValue },
                    type = filters.dishTypes.takeIf { it.isNotEmpty() }
                        ?.joinToString(",") { it.spoonacularValue },
                    includeIngredients = filters.includeIngredients.takeIf { it.isNotBlank() },
                    excludeIngredients = filters.excludeIngredients.takeIf { it.isNotBlank() },
                    maxReadyTime = filters.maxReadyTime,
                    number = resultsPerPage,
                    offset = if (isNewSearch) 0 else currentOffset
                )

                if (response != null) {
                    val currentRecipes = if (isNewSearch || _searchResultsUiState.value !is MealPlannerSearchResultsUiState.Success) {
                        emptyList()
                    } else {
                        (_searchResultsUiState.value as MealPlannerSearchResultsUiState.Success).recipes
                    }
                    val newRecipes = currentRecipes + response.results

                    if (newRecipes.isEmpty() && isNewSearch) {
                        _searchResultsUiState.value = MealPlannerSearchResultsUiState.NoResults
                    } else {
                        currentOffset = response.offset + response.number
                        _searchResultsUiState.value = MealPlannerSearchResultsUiState.Success(
                            recipes = newRecipes,
                            totalResults = response.totalResults,
                            canLoadMore = newRecipes.size < response.totalResults && response.results.isNotEmpty()
                        )
                    }
                } else {
                    _searchResultsUiState.value = MealPlannerSearchResultsUiState.Error("Search failed to load results.")
                }
            } catch (e: Exception) {
                _searchResultsUiState.value = MealPlannerSearchResultsUiState.Error("Error: ${e.localizedMessage}")
            }
        }
    }

    fun loadMoreResults() {
        val currentState = _searchResultsUiState.value
        if (currentState is MealPlannerSearchResultsUiState.Success && currentState.canLoadMore && currentSearchJob?.isActive != true) {
            performSearch(isNewSearch = false)
        }
    }

    fun clearSearchAndFilters() {
        _searchQuery.value = ""
        _appliedFilters.value = AppliedSearchFilters()
        _sortOption.value = RecipeSortOption.RELEVANCE
        _searchResultsUiState.value = MealPlannerSearchResultsUiState.Idle
        currentOffset = 0
        currentSearchJob?.cancel()
    }
}

class MealPlannerSearchViewModelFactory(
    private val recipeRepository: RecipeRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MealPlannerSearchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MealPlannerSearchViewModel(recipeRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}