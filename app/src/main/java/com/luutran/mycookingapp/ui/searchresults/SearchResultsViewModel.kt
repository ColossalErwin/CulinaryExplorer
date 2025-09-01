package com.luutran.mycookingapp.ui.searchresults

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.luutran.mycookingapp.data.model.RecipeSummary
import com.luutran.mycookingapp.data.repository.RecipeRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface SearchResultsUiState {
    data object Loading : SearchResultsUiState
    data class Success(
        val recipes: List<RecipeSummary>,
        val resultCount: Int,
        val apiTotalResults: Int,
        val activeQuery: String,
        val appliedSortOption: RecipeSortOption,
        val appliedFilters: AppliedSearchFilters
    ) : SearchResultsUiState
    data class Error(val message: String, val activeQuery: String?) : SearchResultsUiState
    data class Empty(val originalQuery: String) : SearchResultsUiState
}

class SearchResultsViewModel(
    private val recipeRepository: RecipeRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val initialQuery: String = savedStateHandle.get<String>("searchQuery") ?: ""

    private val _currentSortOption = MutableStateFlow(RecipeSortOption.RELEVANCE)
    val currentSortOption: StateFlow<RecipeSortOption> = _currentSortOption.asStateFlow()

    private val _activeSearchQuery = MutableStateFlow(initialQuery)

    private val _appliedFilters = MutableStateFlow(AppliedSearchFilters())
    val appliedFilters: StateFlow<AppliedSearchFilters> = _appliedFilters.asStateFlow()

    private val _uiState = MutableStateFlow<SearchResultsUiState>(SearchResultsUiState.Loading)
    val uiState: StateFlow<SearchResultsUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private val useDemoData = false // Set to true for demo, false for real API

    init {
        Log.d("SearchResultsVM", "Initializing with initialQuery from SavedStateHandle: '$initialQuery'")
        if (initialQuery.isNotBlank()) {
            Log.d("SearchResultsVM", "init: SavedStateHandle has query '$initialQuery'. Triggering performSearch.")
            performSearch(initialQuery)
        } else {
            Log.d("SearchResultsVM", "init: No initialQuery from SavedStateHandle. Waiting for Screen's LaunchedEffect.")
        }
    }

    fun performSearch(query: String) {
        _activeSearchQuery.value = query // Update the active query immediately
        Log.d("SearchResultsVM", "performSearch called with query: '$query', current sort: ${_currentSortOption.value}, current filters: ${_appliedFilters.value}")
        // Save the latest query to SavedStateHandle so if process dies, it's there
        savedStateHandle["searchQuery"] = query
        executeSearch(query, _currentSortOption.value, _appliedFilters.value)
    }

    fun getCurrentQueryState(): String = _activeSearchQuery.value

    fun setSortOption(sortOption: RecipeSortOption) {
        _currentSortOption.value = sortOption
        // No immediate re-search here; wait for "Apply"
    }

    fun setDietOption(dietOption: DietOption) {
        _appliedFilters.update { it.copy(diet = dietOption) }
    }

    fun toggleIntolerance(intoleranceOption: IntoleranceOption) {
        _appliedFilters.update { currentFilters ->
            val newIntolerances = currentFilters.intolerances.toMutableSet()
            if (newIntolerances.contains(intoleranceOption)) newIntolerances.remove(intoleranceOption)
            else newIntolerances.add(intoleranceOption)
            currentFilters.copy(intolerances = newIntolerances)
        }
    }

    fun toggleCuisine(cuisineOption: CuisineOption) {
        _appliedFilters.update { currentFilters ->
            val newCuisines = currentFilters.cuisines.toMutableSet()
            if (newCuisines.contains(cuisineOption)) newCuisines.remove(cuisineOption)
            else newCuisines.add(cuisineOption)
            currentFilters.copy(cuisines = newCuisines)
        }
    }

    fun toggleDishType(dishTypeOption: DishTypeOption) {
        _appliedFilters.update { currentFilters ->
            val newDishTypes = currentFilters.dishTypes.toMutableSet()
            if (newDishTypes.contains(dishTypeOption)) newDishTypes.remove(dishTypeOption)
            else newDishTypes.add(dishTypeOption)
            currentFilters.copy(dishTypes = newDishTypes)
        }
    }

    fun setIncludeIngredients(ingredients: String) {
        _appliedFilters.update { it.copy(includeIngredients = ingredients.trim()) }
    }

    fun setExcludeIngredients(ingredients: String) {
        _appliedFilters.update { it.copy(excludeIngredients = ingredients.trim()) }
    }

    fun setMaxReadyTime(time: Int?) {
        _appliedFilters.update { it.copy(maxReadyTime = time) }
    }

    fun applySortAndFiltersFromSheet() {
        Log.d("SearchResultsVM", "Applying filters and sort. Query: '${_activeSearchQuery.value}', Sort: ${_currentSortOption.value}, Filters: ${_appliedFilters.value}")
        executeSearch(
            query = _activeSearchQuery.value.ifBlank { initialQuery },
            sortOption = _currentSortOption.value,
            filters = _appliedFilters.value
        )
    }

    fun clearAllFiltersAndSort() {
        _currentSortOption.value = RecipeSortOption.RELEVANCE // Reset sort
        _appliedFilters.value = AppliedSearchFilters() // Reset filters
        Log.d("SearchResultsVM", "Cleared all filters and sort options.")
    }


    private fun executeSearch(query: String, sortOption: RecipeSortOption, filters: AppliedSearchFilters) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (query.isBlank()) {
                Log.d("SearchResultsVM", "executeSearch: Query is blank. Setting to Empty state.")
                _activeSearchQuery.value = "" // Ensure active query reflects this
                _uiState.value = SearchResultsUiState.Empty(query) // Pass original blank query
                return@launch
            }

            _uiState.value = SearchResultsUiState.Loading
            // Use query directly. If it's blank, API might handle it or return error/empty.
            val effectiveQuery = query.ifBlank { " " } // API might need at least a space or non-empty for `query`
            Log.d("SearchResultsVM", "Executing search. Effective Query: '$effectiveQuery', Sort: $sortOption, Filters: $filters")


            if (useDemoData) {
                delay(1000)
                val demoRecipes = getDemoRecipeSummaries(effectiveQuery, sortOption, filters)
                _uiState.value = when {
                    demoRecipes.isEmpty() && (effectiveQuery.isNotBlank() || filters.hasActiveFilters()) ->
                        SearchResultsUiState.Empty(effectiveQuery)
                    demoRecipes.isEmpty() -> SearchResultsUiState.Empty(effectiveQuery)
                    else -> SearchResultsUiState.Success(
                        recipes = demoRecipes,
                        resultCount = demoRecipes.size,
                        apiTotalResults = demoRecipes.size,
                        activeQuery = effectiveQuery,
                        appliedSortOption = sortOption,
                        appliedFilters = filters
                    )
                }
            } else {
                try {
                    val response = recipeRepository.searchRecipes(
                        query = effectiveQuery, // Send the query even if it's just a space
                        sort = sortOption.spoonacularSortValue,
                        sortDirection = if (sortOption.spoonacularSortValue != null) {
                            if (sortOption.defaultDirectionAsc) "asc" else "desc"
                        } else null,
                        diet = filters.diet.takeIf { it != DietOption.NONE }?.spoonacularValue,
                        intolerances = filters.intolerances.takeIf { it.isNotEmpty() }
                            ?.joinToString(",") { it.spoonacularValue },
                        cuisine = filters.cuisines.takeIf { it.isNotEmpty() }
                            ?.joinToString(",") { it.spoonacularValue },
                        type = filters.dishTypes.takeIf { it.isNotEmpty() }
                            ?.joinToString(",") { it.spoonacularValue },
                        includeIngredients = filters.includeIngredients.takeIf { it.isNotBlank() },
                        excludeIngredients = filters.excludeIngredients.takeIf { it.isNotBlank() },
                        maxReadyTime = filters.maxReadyTime
                    )

                    if (response != null) {
                        if (response.results.isEmpty()) {
                            // If API returns results but the list is empty, it's an "Empty" state for that query/filter combo
                            Log.d("SearchResultsVM", "API returned 0 results for query: '$effectiveQuery'")
                            _uiState.value = SearchResultsUiState.Empty(effectiveQuery)
                        } else {
                            Log.d("SearchResultsVM", "API returned ${response.totalResults} total results, ${response.results.size} in current page for query: '$effectiveQuery'")
                            _uiState.value = SearchResultsUiState.Success(
                                recipes = response.results,
                                resultCount = response.results.size, // Count for the current page
                                apiTotalResults = response.totalResults, // Total available from API
                                activeQuery = effectiveQuery,
                                appliedSortOption = sortOption,
                                appliedFilters = filters
                            )
                        }
                    } else {
                        Log.e("SearchResultsVM", "API call failed or returned null response for query: '$effectiveQuery'")
                        _uiState.value = SearchResultsUiState.Error("Failed to fetch recipes.", effectiveQuery)
                    }
                } catch (e: Exception) {
                    Log.e("SearchResultsVM", "Exception during API call for query: '$effectiveQuery'", e)
                    _uiState.value = SearchResultsUiState.Error(e.message ?: "An unknown error occurred", effectiveQuery)
                }
            }
        }
    }
    private fun getDemoRecipeSummaries(
        query: String,
        sortOption: RecipeSortOption,
        filters: AppliedSearchFilters
    ): List<RecipeSummary> {
        if (query.contains("empty", ignoreCase = true) && !filters.hasActiveFilters()) return emptyList()

        val imageUrlBase = "https://spoonacular.com/recipeImages/"
        val allDemoRecipes = listOf(
            RecipeSummary(1, "Classic Spaghetti Bolognese", "${imageUrlBase}639114-312x231.jpg", "jpg", 45),
            RecipeSummary(2, "Quick Chicken Stir-Fry", "${imageUrlBase}716330-312x231.jpg", "jpg", 25),
            RecipeSummary(3, "Vegetarian Lentil Soup", "${imageUrlBase}664327-312x231.jpg", "jpg", 60),
            RecipeSummary(4, "Gourmet Avocado Toast", null, null, 10),
            RecipeSummary(5, "Berry Smoothie Bowl", "${imageUrlBase}635350-312x231.jpg", "jpg", 7),
            RecipeSummary(6, "Spicy Shrimp Tacos", "${imageUrlBase}660261-312x231.jpg", "jpg", 20),
            RecipeSummary(7, "Caprese Salad", "${imageUrlBase}637111-312x231.jpg", "jpg", 10)
        )

        var filteredList = allDemoRecipes.filter { // Filter by query first
            query.isBlank() || query == " " || it.title.contains(query, ignoreCase = true)
        }

        if (filters.diet != DietOption.NONE && filters.diet == DietOption.VEGETARIAN) {
            filteredList = filteredList.filter { it.id == 3 || it.id == 4 || it.id == 5 || it.id == 7 }
        }
        if (filters.intolerances.contains(IntoleranceOption.GLUTEN)) { // Example intolerance
            filteredList = filteredList.filterNot { it.title.contains("Spaghetti") || it.title.contains("Toast")}
        }
        if (filters.maxReadyTime != null) {
            filteredList = filteredList.filter { (it.readyInMinutes ?: Int.MAX_VALUE) <= filters.maxReadyTime }
        }
        if (filters.includeIngredients.isNotBlank()) {
            filteredList = filteredList.filter { it.title.contains(filters.includeIngredients.split(",").first().trim(), ignoreCase = true) } // Simple check
        }
        if (filters.excludeIngredients.isNotBlank()) {
            filteredList = filteredList.filterNot { it.title.contains(filters.excludeIngredients.split(",").first().trim(), ignoreCase = true) } // Simple check
        }

        return when (sortOption) {
            RecipeSortOption.TIME_QUICKEST -> filteredList.sortedBy { it.readyInMinutes ?: Int.MAX_VALUE }
            RecipeSortOption.POPULARITY -> filteredList.shuffled()
            else -> filteredList
        }
    }

    companion object {
        // Updated Factory to accept RecipeRepository and provide SavedStateHandle
        fun provideFactory(
            recipeRepository: RecipeRepository,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                // How to get SavedStateHandle here depends on how you call this.
                // If using navigation-compose, it's often implicit.
                // For manual factory creation, you might need to pass it or the NavController
                // to extract it. For simplicity, let's assume `createSavedStateHandle()` works
                // in the context where this factory is used by `viewModel<>()`.
                val savedStateHandle = createSavedStateHandle()
                SearchResultsViewModel(recipeRepository, savedStateHandle)
            }
        }
    }
}