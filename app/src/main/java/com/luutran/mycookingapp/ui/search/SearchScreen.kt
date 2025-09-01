package com.luutran.mycookingapp.ui.search


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.luutran.mycookingapp.data.repository.RecipeRepository
import com.luutran.mycookingapp.navigation.NavDestinations

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavHostController,
    recipeRepository: RecipeRepository,
    initialQuery: String?
) {
    val searchViewModelFactory = remember(recipeRepository) {
        SearchViewModelFactory(recipeRepository)
    }
    val searchViewModel: SearchViewModel = viewModel(factory = searchViewModelFactory)

    LaunchedEffect(initialQuery) {
        if (initialQuery != null) {
            searchViewModel.setInitialQuery(initialQuery) // You'll need to add this method to SearchViewModel
        }
    }

    val searchQuery by searchViewModel.searchQuery.collectAsState()
    val suggestionsUiState by searchViewModel.suggestionsUiState.collectAsState()

    val localFocusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val onSearchSubmitted = { queryToSubmit: String ->
        if (queryToSubmit.isNotBlank()) {
            localFocusManager.clearFocus()
            keyboardController?.hide()

            val trimmedQuery = queryToSubmit.trim()
            navController.navigate(NavDestinations.searchResultsRoute(trimmedQuery)) {
                // Pop SearchScreen itself off the back stack.
                // When user goes back from SearchResultsScreen, they'll land on HomeScreen (or whatever was before SearchScreen).
                popUpTo(NavDestinations.DEDICATED_SEARCH_SCREEN) {
                    inclusive = true
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    SearchAppBarInput(
                        query = searchQuery,
                        onQueryChanged = { searchViewModel.onQueryChanged(it) },
                        onSearchExecute = { onSearchSubmitted(searchQuery) },
                        onClearSearch = { searchViewModel.clearSearch() },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        localFocusManager.clearFocus()
                        keyboardController?.hide()
                        searchViewModel.clearSearch()
                        navController.popBackStack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },

                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface, // Or surfaceVariant, etc.
                    scrolledContainerColor = MaterialTheme.colorScheme.surface // For consistency on scroll
                ),
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()) {
            when (val state = suggestionsUiState) {
                is SuggestionsUiState.Idle -> {
                    if (searchQuery.isEmpty()) {
                        Box(modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp), contentAlignment = Alignment.Center) {
                        }
                    }
                }
                is SuggestionsUiState.Loading -> {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 0.dp, vertical = 0.dp)
                    )
                }
                is SuggestionsUiState.Success -> {
                    if (state.suggestions.isNotEmpty()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp),
                            shadowElevation = 2.dp,
                            shape = RectangleShape
                        ) {
                            LazyColumn {
                                items(state.suggestions) { suggestion ->
                                    Text(
                                        text = suggestion,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                searchViewModel.onQueryChanged(suggestion)
                                                onSearchSubmitted(suggestion)
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp)
                                    )
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
                is SuggestionsUiState.NoSuggestionsFound -> {
                    if (searchQuery.length >= searchViewModel.minQueryLength) {
                        Box(modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp), contentAlignment = Alignment.Center) {
                            Text("No suggestions found for \"$searchQuery\"")
                        }
                    }
                }
                is SuggestionsUiState.Error -> {
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}