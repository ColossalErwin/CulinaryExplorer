package com.luutran.mycookingapp.ui.mealplanner

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.luutran.mycookingapp.data.model.RecipeSummary
import com.luutran.mycookingapp.data.repository.RecipeRepository
import com.luutran.mycookingapp.ui.searchresults.AppliedSearchFilters
import com.luutran.mycookingapp.ui.searchresults.CuisineOption
import com.luutran.mycookingapp.ui.searchresults.DietOption
import com.luutran.mycookingapp.ui.searchresults.DishTypeOption
import com.luutran.mycookingapp.ui.searchresults.RecipeSortOption
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMealBottomSheet(
    recipeRepository: RecipeRepository,
    onDismiss: () -> Unit,
    onAddRecipes: (List<RecipeSummary>) -> Unit // Callback with selected recipes
) {
    val mealPlannerSearchViewModelFactory = remember(recipeRepository) {
        MealPlannerSearchViewModelFactory(recipeRepository)
    }
    val searchViewModel: MealPlannerSearchViewModel = viewModel(factory = mealPlannerSearchViewModelFactory)

    val searchQuery by searchViewModel.searchQuery.collectAsState()
    val appliedFilters by searchViewModel.appliedFilters.collectAsState()
    val currentSortOption by searchViewModel.sortOption.collectAsState()
    val searchResultsUiState by searchViewModel.searchResultsUiState.collectAsState()

    var selectedRecipesForPlan by remember { mutableStateOf<Set<RecipeSummary>>(emptySet()) }
    var showFilters by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    // LaunchedEffect to request focus when the sheet appears and query is empty
    LaunchedEffect(Unit) {
        if (searchQuery.isEmpty()) {
            focusRequester.requestFocus()
        }
    }


    ModalBottomSheet(
        onDismissRequest = {
            searchViewModel.clearSearchAndFilters() // Clear state on dismiss
            onDismiss()
        },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp)
        ) {
            // Header: Search Bar and Filter Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchViewModel.onQueryChanged(it) },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    placeholder = { Text("Search recipes...") },
                    leadingIcon = { Icon(Icons.Filled.Search, "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchViewModel.onQueryChanged("") }) {
                                Icon(Icons.Filled.Clear, "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        searchViewModel.performSearch(isNewSearch = true)
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    })
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilterToggleButton(
                    isFiltersVisible = showFilters,
                    hasActiveFilters = appliedFilters.hasActiveFilters(),
                    onClick = { showFilters = !showFilters }
                )
            }

            // Expandable Filter Section
            AnimatedVisibility(
                visible = showFilters,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                SearchFilterControls(
                    appliedFilters = appliedFilters,
                    currentSortOption = currentSortOption,
                    onFiltersChanged = { searchViewModel.onFiltersChanged(it) },
                    onSortChanged = { searchViewModel.onSortChanged(it) },
                    onApplyFilters = {
                        showFilters = false
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        searchViewModel.performSearch(isNewSearch = true) // Ensure search re-triggers
                    }
                )
            }
            HorizontalDivider(modifier = Modifier.padding(top = 4.dp))


            // Search Results Area
            Box(modifier = Modifier
                .weight(1f)
                .fillMaxWidth()) {
                when (val state = searchResultsUiState) {
                    is MealPlannerSearchResultsUiState.Idle -> {
                        Box(modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp), contentAlignment = Alignment.Center) {
                            Text(
                                if (searchQuery.isBlank() && !appliedFilters.hasActiveFilters()) "Type to search or apply filters."
                                else "Perform a search to see results.",
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    is MealPlannerSearchResultsUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is MealPlannerSearchResultsUiState.Success -> {
                        if (state.recipes.isEmpty()) {
                            Box(modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp), contentAlignment = Alignment.Center) {
                                Text("No recipes found matching your criteria.", textAlign = TextAlign.Center)
                            }
                        } else {
                            RecipeSearchResultList(
                                recipes = state.recipes,
                                selectedRecipes = selectedRecipesForPlan,
                                onRecipeToggle = { recipe ->
                                    selectedRecipesForPlan = if (selectedRecipesForPlan.contains(recipe)) {
                                        selectedRecipesForPlan - recipe
                                    } else {
                                        selectedRecipesForPlan + recipe
                                    }
                                },
                                canLoadMore = state.canLoadMore,
                                onLoadMore = { searchViewModel.loadMoreResults() }
                            )
                        }
                    }
                    is MealPlannerSearchResultsUiState.NoResults -> { // Explicit no results after a search
                        Box(modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp), contentAlignment = Alignment.Center) {
                            Text("No recipes found matching your criteria.", textAlign = TextAlign.Center)
                        }
                    }
                    is MealPlannerSearchResultsUiState.Error -> {
                        Box(modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp), contentAlignment = Alignment.Center) {
                            Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                        }
                    }
                }
            }

            // Bottom Action Bar: Selection Count and Add Button
            if (selectedRecipesForPlan.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp // Add shadow to make it pop
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${selectedRecipesForPlan.size} recipe(s) selected",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(
                            onClick = {
                                onAddRecipes(selectedRecipesForPlan.toList())
                                searchViewModel.clearSearchAndFilters() // Clear state after adding
                            },
                            enabled = selectedRecipesForPlan.isNotEmpty()
                        ) {
                            Text("Add to Plan")
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun FilterToggleButton(
    isFiltersVisible: Boolean,
    hasActiveFilters: Boolean,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        BadgedBox(
            badge = {
                if (hasActiveFilters) {
                    Badge { Text("!") } // Simple indicator for active filters
                }
            }
        ) {
            Icon(
                imageVector = if (isFiltersVisible) Icons.Filled.FilterListOff else Icons.Filled.FilterList,
                contentDescription = if (isFiltersVisible) "Hide Filters" else "Show Filters"
            )
        }
    }
}

@Composable
fun RecipeSearchResultList(
    recipes: List<RecipeSummary>,
    selectedRecipes: Set<RecipeSummary>,
    onRecipeToggle: (RecipeSummary) -> Unit,
    canLoadMore: Boolean,
    onLoadMore: () -> Unit
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Observer for scroll position to trigger load more
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .collect { visibleItems ->
                if (canLoadMore && visibleItems.isNotEmpty()) {
                    val lastVisibleItem = visibleItems.last()
                    if (lastVisibleItem.index >= recipes.size - 5) { // Load when 5 items from end
                        coroutineScope.launch {
                            onLoadMore()
                        }
                    }
                }
            }
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(recipes, key = { it.id }) { recipe ->
            val isSelected = selectedRecipes.any { it.id == recipe.id }
            RecipeSelectableItem(
                recipe = recipe,
                isSelected = isSelected,
                onToggle = { onRecipeToggle(recipe) }
            )
        }
        if (canLoadMore) {
            item {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun RecipeSelectableItem(
    recipe: RecipeSummary,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Card(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(recipe.image ?: "https://spoonacular.com/recipeImages/${recipe.id}-312x231.jpg") // Fallback
                    .crossfade(true)
                    .build(),
                contentDescription = recipe.title,
                modifier = Modifier
                    .size(64.dp)
                    .aspectRatio(1f)
                    .clip(MaterialTheme.shapes.small),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = recipe.title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Checkbox(
                checked = isSelected,
                onCheckedChange = null,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null, // No visual ripple for the checkbox itself
                    onClick = onToggle
                )
            )
        }
    }
}

// --- Filter Controls Composable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchFilterControls(
    appliedFilters: AppliedSearchFilters,
    currentSortOption: RecipeSortOption,
    onFiltersChanged: (AppliedSearchFilters) -> Unit,
    onSortChanged: (RecipeSortOption) -> Unit,
    onApplyFilters: () -> Unit
) {
    var internalFilters by remember(appliedFilters) { mutableStateOf(appliedFilters) }
    var internalSort by remember(currentSortOption) { mutableStateOf(currentSortOption) }

    var dietDropdownExpanded by remember { mutableStateOf(false) }
    var sortDropdownExpanded by remember { mutableStateOf(false) }

    // Debounce or explicit apply for text fields if needed
    var maxReadyTimeInput by remember(internalFilters.maxReadyTime) {
        mutableStateOf(internalFilters.maxReadyTime?.toString() ?: "")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()) // Make filter section scrollable if it gets too long
            .padding(16.dp)
    ) {
        Text("Sort & Filter", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))

        // Sort By
        OutlinedCard(modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text("Sort by:", style = MaterialTheme.typography.labelLarge)
                ExposedDropdownMenuBox(
                    expanded = sortDropdownExpanded,
                    onExpandedChange = { sortDropdownExpanded = !sortDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = internalSort.displayName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sortDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = sortDropdownExpanded,
                        onDismissRequest = { sortDropdownExpanded = false }
                    ) {
                        RecipeSortOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.displayName) },
                                onClick = {
                                    internalSort = option
                                    onSortChanged(option) // Update VM immediately
                                    sortDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }


        // Diet
        OutlinedCard(modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text("Diet:", style = MaterialTheme.typography.labelLarge)
                ExposedDropdownMenuBox(
                    expanded = dietDropdownExpanded,
                    onExpandedChange = { dietDropdownExpanded = !dietDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = internalFilters.diet.displayName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dietDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()

                    )
                    ExposedDropdownMenu(
                        expanded = dietDropdownExpanded,
                        onDismissRequest = { dietDropdownExpanded = false }
                    ) {
                        DietOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.displayName) },
                                onClick = {
                                    internalFilters = internalFilters.copy(diet = option)
                                    onFiltersChanged(internalFilters) // Update VM
                                    dietDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }


        // Max Ready Time
        OutlinedCard(modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text("Max Ready Time (minutes):", style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = maxReadyTimeInput,
                    onValueChange = { newValue ->
                        maxReadyTimeInput = newValue
                        val newTime = newValue.toIntOrNull()
                        internalFilters = internalFilters.copy(maxReadyTime = newTime)
                        onFiltersChanged(internalFilters)
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done, keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    keyboardActions = KeyboardActions(onDone = { /* FocusManager.clearFocus() */ }),
                    placeholder = { Text("e.g., 30") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        FilterChipGroup(
            title = "Cuisines:",
            options = CuisineOption.entries,
            selectedOptions = internalFilters.cuisines,
            onSelectionChanged = { newSelection ->
                internalFilters = internalFilters.copy(cuisines = newSelection)
                onFiltersChanged(internalFilters)
            },
            displayMapper = { it.displayName }
        )

        FilterChipGroup(
            title = "Dish Types:",
            options = DishTypeOption.entries,
            selectedOptions = internalFilters.dishTypes,
            onSelectionChanged = { newSelection ->
                internalFilters = internalFilters.copy(dishTypes = newSelection)
                onFiltersChanged(internalFilters)
            },
            displayMapper = { it.displayName }
        )

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onApplyFilters,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Apply & Close Filters")
        }
    }
}

@Composable
fun <T> FilterChipGroup(
    title: String,
    options: List<T>,
    selectedOptions: Set<T>,
    onSelectionChanged: (Set<T>) -> Unit,
    displayMapper: (T) -> String
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow( // From ExperimentalLayoutApi for wrapping chips
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp) // Spacing between rows of chips
            ) {
                options.forEach { option ->
                    val isSelected = selectedOptions.contains(option)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            val newSelection = if (isSelected) {
                                selectedOptions - option
                            } else {
                                selectedOptions + option
                            }
                            onSelectionChanged(newSelection)
                        },
                        label = { Text(displayMapper(option)) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Filled.Done, "Selected", modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                        } else {
                            null
                        }
                    )
                }
            }
        }
    }
}