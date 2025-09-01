package com.luutran.mycookingapp.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luutran.mycookingapp.data.repository.RecipeRepository // << IMPORT YOUR REPOSITORY
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface SuggestionsUiState {
    object Idle : SuggestionsUiState
    object Loading : SuggestionsUiState
    data class Success(val suggestions: List<String>) : SuggestionsUiState
    object NoSuggestionsFound : SuggestionsUiState // Query was valid, but no results
    data class Error(val message: String) : SuggestionsUiState
}

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val recipeRepository: RecipeRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _suggestionsUiState = MutableStateFlow<SuggestionsUiState>(SuggestionsUiState.Idle)
    val suggestionsUiState: StateFlow<SuggestionsUiState> = _suggestionsUiState.asStateFlow()

    private var suggestionJob: Job? = null
    internal val minQueryLength = 2 // Minimum characters to trigger suggestion search
    private val debouncePeriodMillis = 700L // Wait 700ms after user stops typing

    init {
        viewModelScope.launch {
            _searchQuery
                .debounce(debouncePeriodMillis)
                .distinctUntilChanged()
                .collectLatest { query ->
                    if (query.length >= minQueryLength) {
                        fetchSuggestionsInternal(query)
                    } else {
                        _suggestionsUiState.value = SuggestionsUiState.Idle
                    }
                }
        }
    }

    fun setInitialQuery(query: String) {
        // Only update if the new initial query is different from the current one
        // to avoid redundant updates or re-triggering flows unnecessarily.
        if (_searchQuery.value != query) {
            _searchQuery.value = query
            // The existing `init` block's collectLatest on _searchQuery
            // will automatically pick up this change and trigger
            // fetchSuggestionsInternal if the query meets the criteria.
            // No need to explicitly call fetchSuggestionsInternal here
        }
    }

    fun onQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    private fun fetchSuggestionsInternal(query: String) {
        suggestionJob?.cancel()
        suggestionJob = viewModelScope.launch {
            _suggestionsUiState.value = SuggestionsUiState.Loading
            try {
                val suggestions = recipeRepository.getRecipeSuggestions(query)
                if (suggestions.isNotEmpty()) {
                    _suggestionsUiState.value = SuggestionsUiState.Success(suggestions)
                } else {
                    // Query met min length, but API returned no suggestions
                    _suggestionsUiState.value = SuggestionsUiState.NoSuggestionsFound
                }
            } catch (e: Exception) {
                _suggestionsUiState.value = SuggestionsUiState.Error("Failed to load suggestions.")
            }
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _suggestionsUiState.value = SuggestionsUiState.Idle
        suggestionJob?.cancel()
    }

    fun clearSuggestionsState() {
        _suggestionsUiState.value = SuggestionsUiState.Idle
    }
}