package com.luutran.mycookingapp.ui.home

import android.app.Application
import android.util.Log
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luutran.mycookingapp.data.datastore.UserPreferencesRepository
import com.luutran.mycookingapp.data.model.RecipeDetail
import com.luutran.mycookingapp.data.model.RecipeSummary
import com.luutran.mycookingapp.data.repository.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

sealed class FeaturedRecipeUiState {
    data object Loading : FeaturedRecipeUiState()
    data class Success(val recipe: RecipeDetail) : FeaturedRecipeUiState()
    data object Error : FeaturedRecipeUiState()
    data object Empty : FeaturedRecipeUiState()
}

// --- State for Suggestions ---
enum class SuggestionCategoryType {
    CUISINE,
    DIET_OR_INTOLERANCE
}
data class SuggestionSection(
    val title: AnnotatedString,
    val recipes: List<RecipeSummary> = emptyList(),
    val isLoading: Boolean = true,       // For initial load of the section
    val isLoadingMore: Boolean = false,  // For loading subsequent pages
    val error: String? = null,
    val categoryType: SuggestionCategoryType,
    val categoryValue: String,           // e.g., "American", "gluten free"
    val currentOffset: Int = 0,          // Current offset for pagination for this section
    val canLoadMore: Boolean = true,     // If more items might be available to load
    /** itemsPerPage
     * this is the number of items each time we do a complex search (spoonacular api call).
     * this number should be large
     * every time we see the lazy list loads again, an api call would be made
     * for itemsPerPage = 100, each time the list loads it would use 2 pts out of 150 pts.
     * If the app is uninstalled then reinstalled it would use 5 pts (1 pt for the Featured Recipe and 4 pts for the complex search)
     * if user close the app and rerun, the 1 pt for the featured recipe won't be wasted since featured recipe is stored, but 4 pts would be lost
     * so we should definitely cache the suggested list
     * then it would be 2 pts for each time the list load. So the optimal value could be 50 instead of 100???
     * if the itemsPerPage value is 5 then if the users use the suggestion feature too much then we it would run out of the free 150 pts
     * so should keep this value big
     * so actually tried to change the value to 50 and now each time the list loads (50 items) it uses only 1 point but the first time it loads it uses 2 pts (when open the app)
     * so when we open the app for the first time of the day it would use 1 pt (featured recipe) + 2 pts for complex search (50 items)
     * when we go past 50 items the list load again and we would use only 1 point to load another 50 items
     * if we shut down the app and open again we would need 2 instead of 3 pts since the featured recipe is stored.
     * Correction: It actually use 1.5 pts each time it load 50 items!!! not 1 pt
     *
     * So CONCLUSION:
     * 100 is the best since each time it loads it uses 2pts (50 uses 1.5 pts)
     * if uninstall then reinstall. There are at least 2 calls (1 for recipe detail and 1 for 100 items), and it would take 4 pts
  */
    val itemsPerPage: Int = 100
)

class HomeViewModel(application: Application, // Needed for UserPreferencesRepository context
                    private val recipeRepository: RecipeRepository,
                    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _featuredRecipeState =
        MutableStateFlow<FeaturedRecipeUiState>(FeaturedRecipeUiState.Loading)
    val featuredRecipeState: StateFlow<FeaturedRecipeUiState> = _featuredRecipeState.asStateFlow()

    private val _suggestionSections = MutableStateFlow<List<SuggestionSection>>(emptyList())
    val suggestionSections: StateFlow<List<SuggestionSection>> = _suggestionSections.asStateFlow()

    private var lastLoadedFeaturedRecipeId: Int? = null

    init {
        fetchFeaturedRecipe()
    }


    fun fetchFeaturedRecipe() {
        viewModelScope.launch {
            _featuredRecipeState.value = FeaturedRecipeUiState.Loading
            Log.d("HomeViewModel", "Fetching featured recipe...")

            try {
                val previousFeaturedRecipeApiId = lastLoadedFeaturedRecipeId // Store ID of recipe currently displayed/cached
                var recipeToDisplay: RecipeDetail? = null
                var newFeaturedRecipeHasBeenSet = false // Flag to ensure suggestions are loaded/reloaded

                // 1. Try to get today's recipe ID from preferences
                val validTodaysApiIdFromPrefs: Int? = userPreferencesRepository.getValidTodaysRecipeId()

                if (validTodaysApiIdFromPrefs != null) {
                    Log.d("HomeViewModel", "Found valid recipe ID for today from Prefs: $validTodaysApiIdFromPrefs. Trying cache.")
                    recipeToDisplay = recipeRepository.getCachedRecipeByApiId(validTodaysApiIdFromPrefs)

                    if (recipeToDisplay != null) {
                        Log.d("HomeViewModel", "Successfully loaded recipe $validTodaysApiIdFromPrefs from local cache.")
                        // Check if this cached recipe is different from what was previously loaded *this session*
                        if (previousFeaturedRecipeApiId != null && previousFeaturedRecipeApiId != recipeToDisplay.id) {
                            Log.d("HomeViewModel", "Featured recipe ID changed from $previousFeaturedRecipeApiId to ${recipeToDisplay.id} (from cache). Clearing old suggestions.")
                            recipeRepository.clearOldSuggestionsForFeaturedRecipe(previousFeaturedRecipeApiId)
                            newFeaturedRecipeHasBeenSet = true
                        } else if (previousFeaturedRecipeApiId == null) { // First valid recipe this session
                            newFeaturedRecipeHasBeenSet = true
                        }
                    } else {
                        Log.d("HomeViewModel", "Recipe $validTodaysApiIdFromPrefs NOT IN CACHE. Fetching specific from network.")
                        recipeToDisplay = recipeRepository.getRecipeByIdFromNetworkAndCache(validTodaysApiIdFromPrefs)
                        if (recipeToDisplay != null) {
                            Log.d("HomeViewModel", "Successfully fetched specific recipe $validTodaysApiIdFromPrefs from network.")
                            if (previousFeaturedRecipeApiId != null && previousFeaturedRecipeApiId != recipeToDisplay.id) {
                                Log.d("HomeViewModel", "Featured recipe ID changed from $previousFeaturedRecipeApiId to ${recipeToDisplay.id} (from network after pref). Clearing old suggestions.")
                                recipeRepository.clearOldSuggestionsForFeaturedRecipe(previousFeaturedRecipeApiId)
                            }
                            newFeaturedRecipeHasBeenSet = true // It's new or confirmed for today
                        } else {
                            Log.w("HomeViewModel", "Failed to fetch specific recipe $validTodaysApiIdFromPrefs. Clearing pref.")
                            userPreferencesRepository.clearTodaysFeaturedRecipe()
                            // No valid recipe for today from prefs, will try random next
                        }
                    }
                } else {
                    Log.d("HomeViewModel", "No valid recipe ID for today in Prefs or it was cleared.")
                }

                // 2. If no recipe yet (either no valid pref or failed to load it), fetch a new random one
                if (recipeToDisplay == null) {
                    Log.d("HomeViewModel", "Fetching new random recipe from network.")
                    val newRandomRecipe = recipeRepository.fetchAndCacheRandomRecipe()
                    if (newRandomRecipe != null) {
                        Log.d("HomeViewModel", "Successfully fetched and cached new random recipe ${newRandomRecipe.id}.")
                        userPreferencesRepository.saveTodaysFeaturedRecipe(newRandomRecipe.id)
                        recipeToDisplay = newRandomRecipe // Assign the new random recipe

                        // If there was a previous featured recipe, and this new random one is different, clear old suggestions.
                        if (previousFeaturedRecipeApiId != null && previousFeaturedRecipeApiId != recipeToDisplay.id) { // Safe now, recipeToDisplay is non-null
                            Log.d("HomeViewModel", "New random recipe ${recipeToDisplay.id} is different from previous $previousFeaturedRecipeApiId. Clearing old suggestions.")
                            recipeRepository.clearOldSuggestionsForFeaturedRecipe(previousFeaturedRecipeApiId)
                        }
                        newFeaturedRecipeHasBeenSet = true
                    } else {
                        Log.w("HomeViewModel", "Failed to fetch a new random recipe from network.")
                    }
                }

                // 3. Update UI and load suggestions
                if (recipeToDisplay != null) {
                    _featuredRecipeState.value = FeaturedRecipeUiState.Success(recipeToDisplay) // recipeToDisplay is non-null here
                    Log.d("HomeViewModel", "Emitting Success state with recipe ID: ${recipeToDisplay.id}")
                    lastLoadedFeaturedRecipeId = recipeToDisplay.id // Update the tracker

                    // Load suggestions if a new recipe was set or if suggestions are currently empty
                    if (newFeaturedRecipeHasBeenSet || _suggestionSections.value.isEmpty()) {
                        Log.d("HomeViewModel", "Loading initial suggestions for recipe ID: ${recipeToDisplay.id}")
                        _suggestionSections.value = emptyList() // Clear UI for suggestions before loading new ones
                        loadInitialSuggestionsForFeaturedRecipe(recipeToDisplay) // recipeToDisplay is non-null here
                    } else {
                        Log.d("HomeViewModel", "Featured recipe ${recipeToDisplay.id} is the same and suggestions already exist. Not reloading initial suggestions.")
                    }
                } else {
                    _featuredRecipeState.value = FeaturedRecipeUiState.Empty
                    _suggestionSections.value = emptyList() // Ensure suggestions are cleared
                    Log.d("HomeViewModel", "No recipe could be loaded. Emitting Empty state.")
                    // If there was a previous one, and now it failed, clear its suggestions
                    if (previousFeaturedRecipeApiId != null) {
                        recipeRepository.clearOldSuggestionsForFeaturedRecipe(previousFeaturedRecipeApiId)
                    }
                    lastLoadedFeaturedRecipeId = null
                }

            } catch (e: IOException) {
                _featuredRecipeState.value = FeaturedRecipeUiState.Error
                _suggestionSections.value = emptyList()
                if (lastLoadedFeaturedRecipeId != null) { // If an error occurs, clear suggestions for the recipe that might have been partially processed
                    recipeRepository.clearOldSuggestionsForFeaturedRecipe(lastLoadedFeaturedRecipeId!!) // Can use !! if sure lastLoaded is set before error
                }
                Log.e("HomeViewModel", "IOException in fetchFeaturedRecipe: ${e.message}", e)
            } catch (e: Exception) {
                _featuredRecipeState.value = FeaturedRecipeUiState.Error
                _suggestionSections.value = emptyList()
                if (lastLoadedFeaturedRecipeId != null) {
                    recipeRepository.clearOldSuggestionsForFeaturedRecipe(lastLoadedFeaturedRecipeId!!)
                }
                Log.e("HomeViewModel", "Generic Exception in fetchFeaturedRecipe: ${e.message}", e)
            }
        }
    }

    private fun generateTitleForSuggestionCategory(
        type: SuggestionCategoryType,
        combinedValue: String
    ): AnnotatedString { // Return AnnotatedString
        fun String.capitalizeWords(): String = split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }

        val individualValues = combinedValue.split(",").map { it.trim().capitalizeWords() }
        val joinedValues = individualValues.joinToString(", ") // This is the part to make italic

        return buildAnnotatedString {
            when (type) {
                SuggestionCategoryType.CUISINE -> {
                    append("Similar ")
                    withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(joinedValues)
                    }
                    append(" Dishes")
                }
                SuggestionCategoryType.DIET_OR_INTOLERANCE -> {
                    append("Similar ")
                    withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(joinedValues)
                    }
                    append(" Options")
                }
            }
        }
    }

    /*
    private fun generateTitleForSuggestionCategory(type: SuggestionCategoryType, value: String): String {
        fun String.capitalizeWords(): String = split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
        return when (type) {
            SuggestionCategoryType.CUISINE -> "Similar ${value.capitalizeWords()} Dishes"
            SuggestionCategoryType.DIET_OR_INTOLERANCE -> "Similar ${value.capitalizeWords()} Options"
        }
    }
    */
    private fun loadInitialSuggestionsForFeaturedRecipe(featuredRecipe: RecipeDetail) {
        if (featuredRecipe.id == 0) { // Assuming 0 is not a valid recipe ID
            _suggestionSections.value = emptyList()
            Log.d("HomeViewModel", "Featured recipe ID is 0, not loading suggestions.")
            return
        }

        val initialItemsPerPageForCache = 100
        val sectionsToCreate = mutableListOf<SuggestionSection>()

        // --- Combine Cuisines ---
        val relevantCuisines = featuredRecipe.cuisines
            ?.filter { it.isNotBlank() }
            ?.distinct()
            ?: emptyList()

        if (relevantCuisines.isNotEmpty()) {
            val combinedCuisineValue = relevantCuisines.joinToString(",") // e.g., "Italian,French"
            sectionsToCreate.add(
                SuggestionSection(
                    title = generateTitleForSuggestionCategory(SuggestionCategoryType.CUISINE, combinedCuisineValue),
                    isLoading = true,
                    isLoadingMore = false,
                    error = null,
                    categoryType = SuggestionCategoryType.CUISINE,
                    categoryValue = combinedCuisineValue, // Store the combined string
                    itemsPerPage = initialItemsPerPageForCache,
                    currentOffset = 0,
                    canLoadMore = true,
                )
            )
        }


        // --- Combine Diets/Intolerances ---
        val relevantDiets = featuredRecipe.diets
            ?.filter { it.isNotBlank() }
            ?.distinct()
            ?: emptyList()

        if (relevantDiets.isNotEmpty()) {
            val combinedDietValue = relevantDiets.joinToString(",") // e.g., "gluten free,dairy free"
            sectionsToCreate.add(
                SuggestionSection(
                    title = generateTitleForSuggestionCategory(SuggestionCategoryType.DIET_OR_INTOLERANCE, combinedDietValue),
                    isLoading = true,
                    isLoadingMore = false,
                    error = null,
                    categoryType = SuggestionCategoryType.DIET_OR_INTOLERANCE,
                    categoryValue = combinedDietValue, // Store the combined string
                    itemsPerPage = initialItemsPerPageForCache,
                    currentOffset = 0,
                    canLoadMore = true,
                )
            )
        }
        if (sectionsToCreate.isEmpty()) {
            _suggestionSections.value = emptyList()
            Log.d("HomeViewModel", "No relevant cuisines or diets found to form combined suggestions for recipe ID: ${featuredRecipe.id}")
            return
        }

        _suggestionSections.value = sectionsToCreate
        Log.d("HomeViewModel", "Initialized ${sectionsToCreate.size} combined suggestion sections with loading state.")


        sectionsToCreate.forEachIndexed { index, section ->
            viewModelScope.launch {
                Log.d("HomeViewModel", "Fetching initial suggestions for section: ${section.title} (Value: ${section.categoryValue}) for Featured ID: ${featuredRecipe.id}")
                try {
                    // The 'excludeRecipeId' here is the featuredRecipe.id, providing context for the cache
                    val suggestedRecipes = when (section.categoryType) {
                        SuggestionCategoryType.CUISINE ->
                            recipeRepository.getRecipesByCuisine(
                                cuisine = section.categoryValue,
                                excludeRecipeId = featuredRecipe.id, // Context for caching
                                number = initialItemsPerPageForCache, // Fetch full amount for cache
                                offset = 0
                            )
                        SuggestionCategoryType.DIET_OR_INTOLERANCE ->
                            recipeRepository.getRecipesByDiet(
                                diet = section.categoryValue,
                                excludeRecipeId = featuredRecipe.id, // Context for caching
                                number = initialItemsPerPageForCache, // Fetch full amount for cache
                                offset = 0
                            )
                    }
                    // The repository now returns the list (either from cache or network)
                    // If from network, it's also cached by the repository.
                    // The list returned will be up to 'initialItemsPerPageForCache' items.

                    Log.d("HomeViewModel", "Fetched ${suggestedRecipes.size} recipes for ${section.title}")

                    _suggestionSections.update { currentSections ->
                        currentSections.toMutableList().also { mutableSections ->
                            val sectionIndexToUpdate = mutableSections.indexOfFirst {
                                it.categoryType == section.categoryType && it.categoryValue == section.categoryValue
                            }
                            if (sectionIndexToUpdate != -1) {
                                mutableSections[sectionIndexToUpdate] = mutableSections[sectionIndexToUpdate].copy(
                                    recipes = suggestedRecipes, // Display the full cached list (or part of it if UI limits)
                                    isLoading = false,
                                    error = if (suggestedRecipes.isEmpty() && section.itemsPerPage > 0) "No suggestions found." else null,
                                    canLoadMore = suggestedRecipes.size == initialItemsPerPageForCache
                                )
                            } else { Log.w("HomeViewModel", "Could not find section to update: ${section.title}") }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("HomeViewModel", "Error fetching initial suggestions for ${section.title}", e)
                    _suggestionSections.update { currentSections ->
                        currentSections.toMutableList().also { mutableSections ->
                            val sectionIndexToUpdate = mutableSections.indexOfFirst {
                                it.categoryType == section.categoryType && it.categoryValue == section.categoryValue
                            }
                            if (sectionIndexToUpdate != -1) {
                                mutableSections[sectionIndexToUpdate] = mutableSections[sectionIndexToUpdate].copy(
                                    isLoading = false,
                                    error = "Could not load suggestions"
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun loadMoreRecipesForSuggestionSection(sectionToLoadMore: SuggestionSection) {
        if (sectionToLoadMore.isLoadingMore || !sectionToLoadMore.canLoadMore || sectionToLoadMore.isLoading) {
            Log.d("HomeViewModel", "Cannot load more for ${sectionToLoadMore.title}: isLoadingMore=${sectionToLoadMore.isLoadingMore}, canLoadMore=${sectionToLoadMore.canLoadMore}, isLoading=${sectionToLoadMore.isLoading}")
            return
        }

        // Featured recipe ID is not needed for exclusion in "load more" if handled correctly in repo (offset > 0 means no exclusion)
        val featuredRecipeId = (_featuredRecipeState.value as? FeaturedRecipeUiState.Success)?.recipe?.id

        _suggestionSections.update { currentSections ->
            currentSections.map { section ->
                if (section.categoryType == sectionToLoadMore.categoryType && section.categoryValue == sectionToLoadMore.categoryValue) {
                    section.copy(isLoadingMore = true, error = null)
                } else {
                    section
                }
            }
        }
        Log.d("HomeViewModel", "Loading more suggestions for: ${sectionToLoadMore.title}")

        viewModelScope.launch {
            try {
                val nextOffset = sectionToLoadMore.recipes.size // Offset is the number of items already loaded

                val newRecipes = when (sectionToLoadMore.categoryType) {
                    SuggestionCategoryType.CUISINE ->
                        recipeRepository.getRecipesByCuisine(
                            cuisine = sectionToLoadMore.categoryValue,
                            number = sectionToLoadMore.itemsPerPage,
                            offset = nextOffset,
                            excludeRecipeId = featuredRecipeId // Can pass for consistency, repo might ignore for offset > 0
                        )
                    SuggestionCategoryType.DIET_OR_INTOLERANCE ->
                        recipeRepository.getRecipesByDiet(
                            diet = sectionToLoadMore.categoryValue,
                            number = sectionToLoadMore.itemsPerPage,
                            offset = nextOffset,
                            excludeRecipeId = featuredRecipeId
                        )
                }
                Log.d("HomeViewModel", "Fetched ${newRecipes.size} MORE recipes for ${sectionToLoadMore.title}")

                _suggestionSections.update { currentSections ->
                    currentSections.map { section ->
                        if (section.categoryType == sectionToLoadMore.categoryType && section.categoryValue == sectionToLoadMore.categoryValue) {
                            section.copy(
                                recipes = section.recipes + newRecipes,
                                isLoadingMore = false,
                                currentOffset = nextOffset, // Although offset is based on list size, explicitly tracking could be useful if API differs
                                canLoadMore = newRecipes.isNotEmpty() && newRecipes.size == section.itemsPerPage
                            )
                        } else {
                            section
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading more suggestions for ${sectionToLoadMore.title}", e)
                _suggestionSections.update { currentSections ->
                    currentSections.map { section ->
                        if (section.categoryType == sectionToLoadMore.categoryType && section.categoryValue == sectionToLoadMore.categoryValue) {
                            section.copy(isLoadingMore = false, error = "Could not load more.")
                        } else {
                            section
                        }
                    }
                }
            }
        }
    }

    fun retryLoadInitialSuggestionSection(sectionToRetry: SuggestionSection) {
        val featuredRecipe = (_featuredRecipeState.value as? FeaturedRecipeUiState.Success)?.recipe
        if (featuredRecipe == null || featuredRecipe.id == 0) {
            Log.w("HomeViewModel", "Cannot retry suggestion: No valid featured recipe.")
            return
        }

        // Find the index to update the correct section
        val sectionIndex = _suggestionSections.value.indexOfFirst {
            it.categoryType == sectionToRetry.categoryType && it.categoryValue == sectionToRetry.categoryValue
        }

        if (sectionIndex == -1) {
            Log.w("HomeViewModel", "Cannot retry suggestion: Section not found for ${sectionToRetry.title}")
            return
        }

        _suggestionSections.update { currentSections ->
            currentSections.toMutableList().also { mutableSections ->
                mutableSections[sectionIndex] = mutableSections[sectionIndex].copy(
                    isLoading = true,
                    isLoadingMore = false,
                    error = null,
                    recipes = emptyList(), // Clear old recipes on retry of initial load
                    currentOffset = 0,
                    canLoadMore = true
                )
            }
        }
        Log.d("HomeViewModel", "Retrying initial suggestions for: ${sectionToRetry.title}")

        viewModelScope.launch {
            try {
                val suggestedRecipes = when (sectionToRetry.categoryType) {
                    SuggestionCategoryType.CUISINE ->
                        recipeRepository.getRecipesByCuisine(
                            cuisine = sectionToRetry.categoryValue,
                            excludeRecipeId = featuredRecipe.id,
                            number = sectionToRetry.itemsPerPage,
                            offset = 0
                        )
                    SuggestionCategoryType.DIET_OR_INTOLERANCE ->
                        recipeRepository.getRecipesByDiet(
                            diet = sectionToRetry.categoryValue,
                            excludeRecipeId = featuredRecipe.id,
                            number = sectionToRetry.itemsPerPage,
                            offset = 0
                        )
                }
                Log.d("HomeViewModel", "Retry fetched ${suggestedRecipes.size} recipes for ${sectionToRetry.title}")

                _suggestionSections.update { currentSections ->
                    currentSections.toMutableList().also { mutableSections ->
                        if (mutableSections.indices.contains(sectionIndex)) {
                            mutableSections[sectionIndex] = mutableSections[sectionIndex].copy(
                                recipes = suggestedRecipes,
                                isLoading = false,
                                error = if (suggestedRecipes.isEmpty()) "No suggestions found." else null,
                                canLoadMore = suggestedRecipes.size == sectionToRetry.itemsPerPage
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error retrying initial suggestions for ${sectionToRetry.title}", e)
                _suggestionSections.update { currentSections ->
                    currentSections.toMutableList().also { mutableSections ->
                        if (mutableSections.indices.contains(sectionIndex)) {
                            mutableSections[sectionIndex] = mutableSections[sectionIndex].copy(
                                isLoading = false,
                                error = "Could not load suggestions"
                            )
                        }
                    }
                }
            }
        }
    }
}