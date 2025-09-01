package com.luutran.mycookingapp.ui.searchresults


import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime // More specific icon for time
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.SearchOff // Icon for empty results
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.luutran.mycookingapp.R // Create this if you don't have one
import com.luutran.mycookingapp.data.local.dao.RecipeDao
import com.luutran.mycookingapp.data.local.entity.RecipeEntity
import com.luutran.mycookingapp.data.model.RecipeSearchResponse
import com.luutran.mycookingapp.data.model.RecipeSummary
import com.luutran.mycookingapp.data.network.SpoonacularApiService
import com.luutran.mycookingapp.data.repository.RecipeRepository
import com.luutran.mycookingapp.ui.theme.LightGreenApp
import com.luutran.mycookingapp.ui.theme.OnLightGreenApp
import kotlinx.coroutines.flow.flowOf
import retrofit2.Response
import kotlin.text.filter
import kotlin.text.isDigit
import kotlin.text.isNotEmpty
import kotlin.text.toIntOrNull
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material3.SheetState
import com.luutran.mycookingapp.navigation.NavDestinations
import java.net.URLEncoder
import java.nio.charset.StandardCharsets


// --- SearchResultsScreen ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultsScreen(
    navController: NavController,
    searchQueryFromNav: String,
    recipeRepository: RecipeRepository,
    onRecipeClick: (Int) -> Unit,
) {
    val viewModel: SearchResultsViewModel = viewModel(
        factory = SearchResultsViewModel.provideFactory(recipeRepository)
    )

    val uiState by viewModel.uiState.collectAsState()

    val currentSortOption by viewModel.currentSortOption.collectAsState()
    val currentFilters by viewModel.appliedFilters.collectAsState()

    var showFilterSortSheet by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true) // For ModalBottomSheet

    val actualSearchQuery = remember(searchQueryFromNav) {
        searchQueryFromNav
    }

    LaunchedEffect(actualSearchQuery, viewModel) {
        Log.d("SearchResultsScreen", "LaunchedEffect: searchQueryFromNav='$actualSearchQuery'. Triggering viewModel.performSearch.")
        if (actualSearchQuery.isNotBlank()) {
            viewModel.performSearch(actualSearchQuery)
        }
    }
    Log.d("SearchResultsScreen", "Recomposing. Query from nav: '$searchQueryFromNav'. VM active query: '${viewModel.getCurrentQueryState()}'. UI State: $uiState")


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val displayQuery = viewModel.getCurrentQueryState().takeIf { it.isNotBlank() } ?: actualSearchQuery
                    Text(text = if (displayQuery.isNotBlank()) "\"$displayQuery\"" else "Search Results")
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val currentQueryForEdit = viewModel.getCurrentQueryState().takeIf { it.isNotBlank() } ?: actualSearchQuery
                        val route = "${NavDestinations.DEDICATED_SEARCH_SCREEN}?${NavDestinations.SEARCH_RESULTS_ARG_QUERY}=${URLEncoder.encode(currentQueryForEdit, StandardCharsets.UTF_8.toString())}"
                        navController.navigate(route)
                    }) {
                        Icon(Icons.Filled.Search, "Modify Search")
                    }

                    IconButton(onClick = { /* showFilterSortSheet = true */ }) { // Your existing filter
                        Icon(Icons.Filled.FilterList, contentDescription = "Filter or Sort Results")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LightGreenApp,
                    titleContentColor = OnLightGreenApp,
                    navigationIconContentColor = OnLightGreenApp,
                    actionIconContentColor = OnLightGreenApp
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Display the query that was used for the search
            val displayQuery = viewModel.getCurrentQueryState().takeIf { it.isNotBlank() } ?: actualSearchQuery
            if (displayQuery.isNotBlank()){
                Text(
                    text = "Results for \"$displayQuery\"",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = "Search Results", // Fallback if query is somehow blank
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            when (val state = uiState) {
                is SearchResultsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is SearchResultsUiState.Success -> {
                    Column(modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .weight(1f)) {
                        Text(
                            text = "Found ${state.apiTotalResults} recipe${if (state.apiTotalResults != 1) "s" else ""} (displaying ${state.recipes.size})",
                            style = MaterialTheme.typography.bodyMedium, // Adjusted style
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        if (state.recipes.isEmpty()) {
                            Box(modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Filled.SentimentDissatisfied,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "No recipes match your current filters for \"${state.activeQuery}\".",
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        "Try adjusting your filters or search terms.",
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(state.recipes, key = { it.id }) { recipe ->
                                    RecipeResultItem(
                                        recipe = recipe,
                                        onClick = { onRecipeClick(recipe.id) }
                                    )
                                }
                            }
                        }
                    }
                }
                is SearchResultsUiState.Error -> {
                    //ErrorView(message = state.message, onRetry = { viewModel.retrySearch() })
                }
                is SearchResultsUiState.Empty -> {
                    EmptyResultsView(searchQuery = state.originalQuery)
                }
            }
        }

        if (showFilterSortSheet) {
            FilterSortModalSheet(
                sheetState = sheetState,
                currentSortOption = currentSortOption,
                currentFilters = currentFilters,
                onDismiss = { showFilterSortSheet = false },
                onApply = {
                    viewModel.applySortAndFiltersFromSheet()
                    showFilterSortSheet = false
                },
                onClear = {
                    viewModel.clearAllFiltersAndSort()
                    // Keep sheet open for user to apply or dismiss after clearing
                },
                onSortChange = viewModel::setSortOption,
                onDietChange = viewModel::setDietOption,
                onIntoleranceToggle = viewModel::toggleIntolerance,
                onCuisineToggle = viewModel::toggleCuisine,
                onDishTypeToggle = viewModel::toggleDishType,
                onIncludeIngredientsChange = viewModel::setIncludeIngredients,
                onExcludeIngredientsChange = viewModel::setExcludeIngredients,
                onMaxReadyTimeChange = viewModel::setMaxReadyTime
            )
        }
    }
}

@Composable
fun RecipeResultItem(
    recipe: RecipeSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp) // Slightly more rounded
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(recipe.image)
                    .crossfade(true)
                    .placeholder(R.drawable.image_placeholder_error)
                    .error(R.drawable.image_placeholder_error)
                    .build(),
                contentDescription = recipe.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(88.dp) // Slightly larger image
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recipe.title,
                    style = MaterialTheme.typography.titleMedium, // Larger title
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))

                recipe.readyInMinutes?.let { time ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.AccessTime,
                            contentDescription = "Cooking time",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$time min",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}



@Composable
fun ErrorView(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(
                imageVector = Icons.Filled.BrokenImage,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Oops! Something went wrong.",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )
            Button(onClick = onRetry) {
                Text("Try Again")
            }
        }
    }
}

@Composable
fun EmptyResultsView(searchQuery: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(
                imageVector = Icons.Filled.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No recipes found",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            if (searchQuery.isNotBlank()) {
                Text(
                    text = "Try adjusting your search for \"$searchQuery\" or try different keywords.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else {
                Text(
                    text = "Try searching for a dish, ingredient, or cuisine.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSortModalSheet(
    sheetState: SheetState,
    currentSortOption: RecipeSortOption,
    currentFilters: AppliedSearchFilters,
    onDismiss: () -> Unit,
    onApply: () -> Unit,
    onClear: () -> Unit,
    onSortChange: (RecipeSortOption) -> Unit,
    onDietChange: (DietOption) -> Unit,
    onIntoleranceToggle: (IntoleranceOption) -> Unit,
    onCuisineToggle: (CuisineOption) -> Unit,
    onDishTypeToggle: (DishTypeOption) -> Unit,
    onIncludeIngredientsChange: (String) -> Unit,
    onExcludeIngredientsChange: (String) -> Unit,
    onMaxReadyTimeChange: (Int?) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets.navigationBars }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sort & Filter", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = {
                    onClear()
                }) {
                    Text("Clear All")
                }
            }
            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // --- Sort Section ---
            SortSection(currentSortOption, onSortChange)
            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // --- Filter Sections ---
            FilterGroupTitle("Diet")
            DietFilterSection(currentFilters.diet, onDietChange)

            FilterGroupTitle("Intolerances")
            MultiSelectChipGroup(
                options = IntoleranceOption.entries,
                selectedOptions = currentFilters.intolerances,
                onOptionToggle = { onIntoleranceToggle(it as IntoleranceOption) },
                optionToDisplayName = { (it as IntoleranceOption).displayName }
            )

            FilterGroupTitle("Cuisines")
            MultiSelectChipGroup(
                options = CuisineOption.entries,
                selectedOptions = currentFilters.cuisines,
                onOptionToggle = { onCuisineToggle(it) },
                optionToDisplayName = { it.displayName }
            )

            FilterGroupTitle("Dish Types")
            MultiSelectChipGroup(
                options = DishTypeOption.entries,
                selectedOptions = currentFilters.dishTypes,
                onOptionToggle = { onDishTypeToggle(it) },
                optionToDisplayName = { it.displayName }
            )

            FilterGroupTitle("Max Ready Time (minutes)")
            MaxReadyTimeFilter(currentFilters.maxReadyTime, onMaxReadyTimeChange)

            FilterGroupTitle("Include Ingredients (comma separated)")
            IngredientsTextField(currentFilters.includeIngredients, onIncludeIngredientsChange, "e.g., chicken, tomatoes")

            FilterGroupTitle("Exclude Ingredients (comma separated)")
            IngredientsTextField(currentFilters.excludeIngredients, onExcludeIngredientsChange, "e.g., peanuts, shellfish")


            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    onApply()
                    keyboardController?.hide()
                    focusManager.clearFocus()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Done, contentDescription = "Apply")
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Apply Filters")
            }
            Spacer(modifier = Modifier.height(8.dp)) // Space for navigation bar
        }
    }
}

@Composable
fun FilterGroupTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun SortSection(
    selectedSort: RecipeSortOption,
    onSortChange: (RecipeSortOption) -> Unit
) {
    Column {
        RecipeSortOption.entries.forEach { option ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = (option == selectedSort),
                        onClick = { onSortChange(option) }
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (option == selectedSort),
                    onClick = null // onClick handled by Row's selectable
                )
                Text(
                    text = option.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> MultiSelectChipGroup(
    options: List<T>,
    selectedOptions: Set<T>,
    onOptionToggle: (T) -> Unit,
    optionToDisplayName: (T) -> String
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { option ->
            FilterChip(
                selected = selectedOptions.contains(option),
                onClick = { onOptionToggle(option) },
                label = { Text(optionToDisplayName(option)) },
                leadingIcon = if (selectedOptions.contains(option)) {
                    { Icon(Icons.Filled.Done, contentDescription = "Selected") }
                } else null
            )
        }
    }
}


@Composable
fun DietFilterSection(
    selectedDiet: DietOption,
    onDietChange: (DietOption) -> Unit
) {
    Column {
        DietOption.entries.forEach { option ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = (option == selectedDiet),
                        onClick = { onDietChange(option) }
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (option == selectedDiet),
                    onClick = null
                )
                Text(
                    text = option.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }
}

@Composable
fun MaxReadyTimeFilter(
    currentTime: Int?,
    onTimeChange: (Int?) -> Unit
) {
    var sliderPosition by remember(currentTime) { mutableFloatStateOf(currentTime?.toFloat() ?: 0f) }
    var textValue by remember(currentTime) { mutableStateOf(currentTime?.toString() ?: "") }
    val focusManager = LocalFocusManager.current

    Column {
        OutlinedTextField(
            value = textValue,
            onValueChange = { newText ->
                textValue = newText.filter { it.isDigit() }
                val newTime = textValue.toIntOrNull()
                sliderPosition = newTime?.toFloat() ?: 0f
                onTimeChange(newTime)
            },
            label = { Text("Time in minutes (e.g., 30)") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = if (textValue.isNotEmpty()) {
                {
                    IconButton(onClick = {
                        textValue = ""
                        sliderPosition = 0f
                        onTimeChange(null)
                        focusManager.clearFocus()
                    }) {
                        Icon(Icons.Filled.Clear, "Clear time")
                    }
                }
            } else null
        )
    }
}

@Composable
fun IngredientsTextField(
    currentValue: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = currentValue,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    )
}