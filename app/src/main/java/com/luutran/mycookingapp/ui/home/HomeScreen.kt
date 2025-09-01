package com.luutran.mycookingapp.ui.home

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.luutran.mycookingapp.R
import com.luutran.mycookingapp.data.model.RecipeDetail
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import com.luutran.mycookingapp.navigation.NavDestinations
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.isEmpty
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.request.ImageRequest
import com.luutran.mycookingapp.data.local.AppDatabase
import com.luutran.mycookingapp.data.model.RecipeSummary
import com.luutran.mycookingapp.data.utils.getUserPreferencesRepositoryInstanceComposable
import com.luutran.mycookingapp.ui.auth.AuthUiState
import com.luutran.mycookingapp.ui.auth.AuthViewModel
import com.luutran.mycookingapp.ui.recipedetail.getRecipeRepository
import com.luutran.mycookingapp.ui.theme.LightGreenApp
import com.luutran.mycookingapp.ui.theme.OnLightGreenApp
import kotlinx.coroutines.flow.distinctUntilChanged


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier,
    featuredRecipeUiState: FeaturedRecipeUiState,
    onRecipeClick: (Int) -> Unit,
    onRetryFeaturedRecipe: () -> Unit,
    onOpenDrawer: () -> Unit
) {
    Log.d("HomeScreen", "Composing HomeScreen. Current nav route: ${navController.currentDestination?.route}")


    val application = LocalContext.current.applicationContext as Application

    val recipeRepository = getRecipeRepository()
    val userPreferencesRepository = getUserPreferencesRepositoryInstanceComposable()

    val homeViewModelFactory = HomeViewModelFactory(
        application,
        recipeRepository,
        userPreferencesRepository
    )
    val homeViewModel: HomeViewModel = viewModel(factory = homeViewModelFactory)

    val suggestionSections by homeViewModel.suggestionSections.collectAsStateWithLifecycle()

    // States for Email Verification Banner
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
    val isEmailVerified by authViewModel.isEmailVerified.collectAsStateWithLifecycle()
    val authScreenUiState by authViewModel.uiState.collectAsStateWithLifecycle() // For loading state of resend

    var showVerificationBanner by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser, isEmailVerified) {
        val user = currentUser
        showVerificationBanner = user != null && !isEmailVerified && user.providerData.any { it.providerId == "password" }
        Log.d("HomeScreen", "User: ${user?.email}, Verified: $isEmailVerified, ShowBanner: $showVerificationBanner")
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    FakeSearchBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 16.dp),
                        onClick = {
                            navController.navigate(NavDestinations.DEDICATED_SEARCH_SCREEN)
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, contentDescription = "Open Navigation Drawer")
                    }
                },
                actions = {  },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LightGreenApp,
                    titleContentColor = OnLightGreenApp,
                    navigationIconContentColor = OnLightGreenApp,
                    actionIconContentColor = OnLightGreenApp
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedVisibility(
                visible = showVerificationBanner,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                EmailVerificationBanner(
                    onVerifyEmailClick = {
                        Log.d("HomeScreen", "Verify Email Clicked from banner")
                        navController.navigate(NavDestinations.VERIFY_EMAIL_SCREEN)
                    },
                    onResendEmailClick = {
                        Log.d("HomeScreen", "Resend Email Clicked from banner")
                        authViewModel.resendVerificationEmail()
                    },
                    onDismissClick = {
                        showVerificationBanner = false
                        Log.d("HomeScreen", "Verification banner dismissed by user")
                    },
                    isLoading = authScreenUiState is AuthUiState.Loading
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Spacer(modifier = Modifier.height(if (showVerificationBanner) 8.dp else 16.dp))
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        when (featuredRecipeUiState) {
                            is FeaturedRecipeUiState.Loading -> {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 50.dp)
                                        .align(Alignment.Center)
                                )
                            }
                            is FeaturedRecipeUiState.Success -> {
                                Column {
                                    Text(
                                        text = "Today's Featured Recipe",
                                        style = MaterialTheme.typography.titleLarge,
                                        modifier = Modifier
                                            .padding(top = 8.dp, bottom = 16.dp)
                                            .fillMaxWidth()
                                            .align(Alignment.Start)
                                    )
                                    FeaturedRecipeCard(
                                        recipe = featuredRecipeUiState.recipe,
                                        onClick = {
                                            val idToNavigate = featuredRecipeUiState.recipe.id
                                            Log.d("HomeScreen", "FeaturedRecipeCard onClick - recipeId: $idToNavigate")
                                            onRecipeClick(idToNavigate) // Use the passed lambda
                                        }
                                    )

                                }
                            }
                            is FeaturedRecipeUiState.Error -> {
                                NoFeaturedRecipeDisplay(
                                    message = "Oops! Couldn't load a recipe.",
                                    showRetryButton = true,
                                    onRetry = onRetryFeaturedRecipe // Use the passed lambda
                                )
                            }
                            is FeaturedRecipeUiState.Empty -> {
                                NoFeaturedRecipeDisplay(message = "No featured recipe today. Check back later!")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                itemsIndexed(
                    items = suggestionSections, // This is the StateFlow<List<SuggestionSection>>
                    key = { _, section -> "suggestion-section-${section.categoryType}-${section.categoryValue}" } // Unique key for each section
                ) { _, section ->
                    // Only compose the carousel if it has recipes, is loading initially, or has an error to display
                    // or is currently loading more. This avoids empty carousels if a category truly has zero results
                    // and isn't in an error/loading state.
                    if (section.recipes.isNotEmpty() || section.isLoading || (section.error != null && section.isLoadingMore) || section.isLoadingMore) {
                        SuggestionCarousel(
                            sectionData = section,
                            onRecipeClick = { recipeId ->
                                Log.d("HomeScreen", "SuggestionRecipeCard onClick - recipeId: $recipeId")
                                onRecipeClick(recipeId)
                            },
                            onLoadMore = {
                                Log.d("HomeScreen", "onLoadMore triggered for ${section.title}")
                                homeViewModel.loadMoreRecipesForSuggestionSection(section)
                            },
                            onRetryInitialLoad = {
                                Log.d("HomeScreen", "onRetryInitialLoad triggered for ${section.title}")
                                homeViewModel.retryLoadInitialSuggestionSection(section)
                            },
                            modifier = Modifier.padding(bottom = 20.dp) // Space between carousels
                        )
                    }
                }

            }
        }
    }
}

@Composable
fun EmailVerificationBanner(
    onVerifyEmailClick: () -> Unit,
    onResendEmailClick: () -> Unit,
    onDismissClick: () -> Unit,
    isLoading: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 3.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Verify Your Email Address",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "Please check your inbox and click the verification link.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Row {
                    TextButton(onClick = onVerifyEmailClick, enabled = !isLoading) {
                        Text("CHECK STATUS")
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = onResendEmailClick, enabled = !isLoading) {
                        Text(if (isLoading) "SENDING..." else "RESEND EMAIL")
                    }
                }
            }
            IconButton(onClick = onDismissClick, enabled = !isLoading) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Dismiss verification banner",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
fun FakeSearchBar(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface( // Use Surface for elevation and shape, similar to a real TextField
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp) // Typical TextField height
            .clip(MaterialTheme.shapes.extraLarge) // Or medium, small
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.extraLarge, // Or medium, small
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), // A slightly different background
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search Icon",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Search recipes...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
@Composable
fun SuggestionCarousel(
    sectionData: SuggestionSection,
    onRecipeClick: (Int) -> Unit,
    onLoadMore: () -> Unit,
    onRetryInitialLoad: () -> Unit, // For retrying the first page of this section
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = sectionData.title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)
                .fillMaxWidth(), // Ensure title takes full width for alignment
            color = MaterialTheme.colorScheme.onSurface
        )

        when {
            // 1. Initial loading state for the section (no recipes yet)
            sectionData.isLoading && sectionData.recipes.isEmpty() -> {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(3) { // Show a few placeholders
                        SuggestionRecipeCardPlaceholder()
                    }
                }
            }
            // 2. Error state for initial load (no recipes yet, and not currently trying to load more)
            sectionData.error != null && sectionData.recipes.isEmpty() && !sectionData.isLoadingMore -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = sectionData.error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Button(onClick = onRetryInitialLoad) { // Use the specific retry for initial load
                        Text("RETRY")
                    }
                }
            }
            // 3. Recipes are available (or we are loading more to add to existing recipes)
            sectionData.recipes.isNotEmpty() || sectionData.isLoadingMore -> {
                LazyRow(
                    state = listState,
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp), // Consistent padding
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = sectionData.recipes,
                        key = { recipe -> "suggestion-${recipe.id}" } // Unique key for items
                    ) { recipe ->
                        SuggestionRecipeCard(
                            recipe = recipe,
                            onClick = { onRecipeClick(recipe.id) }
                        )
                    }

                    // Show loading more indicator at the end if applicable
                    if (sectionData.isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .padding(start = 8.dp, end = 8.dp) // Padding around the spinner
                                    .height(150.dp), // Match approx card height for alignment
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }

                // Scroll detection logic to trigger onLoadMore
                val currentListState = listState
                LaunchedEffect(currentListState, sectionData.canLoadMore, sectionData.isLoadingMore) {
                    snapshotFlow { currentListState.layoutInfo }
                        .distinctUntilChanged() // Only react to actual changes in layoutInfo
                        .collect { layoutInfo ->
                            val visibleItemsInfo = layoutInfo.visibleItemsInfo
                            if (visibleItemsInfo.isNotEmpty() && !sectionData.isLoadingMore && sectionData.canLoadMore) {
                                val lastVisibleItem = visibleItemsInfo.last()
                                val totalItems = layoutInfo.totalItemsCount

                                // Trigger load more if the last visible item is close to the end (e.g., index is totalItems - 1 or -2)
                                // and not already loading and more items can be loaded.
                                // The threshold (e.g., 2) means when the 2nd to last item is visible, start loading.
                                val loadMoreThreshold = 2
                                if (totalItems > 0 && lastVisibleItem.index >= totalItems - loadMoreThreshold) {
                                    Log.d("SuggestionCarousel", "Load more triggered for: ${sectionData.title}")
                                    onLoadMore()
                                }
                            }
                        }
                }
            }
        }
    }
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun SuggestionRecipeCard(
    recipe: RecipeSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    // Aim for 2 to 2.5 cards visible on average screens, adjust as needed
    val cardWidth = screenWidth / 2.4f // Makes cards a bit wider

    Card(
        modifier = modifier
            .width(cardWidth)
            .height(IntrinsicSize.Min) // Allow card to wrap content height, good for text
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp), // Consistent corner rounding
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(recipe.image)
                    .crossfade(true)
                    .placeholder(R.drawable.image_placeholder_error)
                    .error(R.drawable.image_placeholder_error)
                    .build(),
                contentDescription = recipe.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp), // Fixed height for image consistency
                contentScale = ContentScale.Crop,
                onError = { error -> Log.e("SuggestionRecipeCard", "Image load error for ${recipe.id}: ${error.result.throwable}") },
                onSuccess = { success -> Log.d("SuggestionRecipeCard", "Image loaded for ${recipe.id}") }
            )
            androidx.wear.compose.material.Text(
                text = recipe.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .fillMaxWidth(), // Ensure text uses available width for wrapping
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun SuggestionRecipeCardPlaceholder(modifier: Modifier = Modifier) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val cardWidth = screenWidth / 2.4f

    Card(
        modifier = modifier
            .width(cardWidth)
            .height(170.dp), // Approximate height of SuggestionRecipeCard
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(MaterialTheme.typography.titleSmall.fontSize.value.dp + 4.dp) // approx line height
                    .padding(start = 10.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(MaterialTheme.typography.titleSmall.fontSize.value.dp + 4.dp) // approx line height
                    .padding(start = 10.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            )
        }
    }
}

@Composable
fun FeaturedRecipeCard(
    recipe: RecipeDetail,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            AsyncImage(
                model = recipe.image,
                contentDescription = recipe.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.image_placeholder_error), // Generic placeholder while loading
                error = painterResource(id = R.drawable.image_placeholder_error) // Placeholder if image fails to load
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    recipe.title.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Tap to see the full recipe!",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun NoFeaturedRecipeDisplay(
    message: String,
    modifier: Modifier = Modifier,
    showRetryButton: Boolean = false,
    onRetry: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp) // Give it a similar height to the recipe card for layout consistency
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(
                painter = painterResource(id = R.drawable.image_placeholder_error), // Create a suitable placeholder icon
                contentDescription = null, // Decorative
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (showRetryButton) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onRetry) {
                    Text("Try Again")
                }
            }
        }
    }
}
