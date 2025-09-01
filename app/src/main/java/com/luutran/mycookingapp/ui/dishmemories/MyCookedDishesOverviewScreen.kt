package com.luutran.mycookingapp.ui.dishmemories

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luutran.mycookingapp.data.model.CookedDishEntry
import com.luutran.mycookingapp.ui.theme.LightGreenApp
import com.luutran.mycookingapp.ui.theme.OnLightGreenApp

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.luutran.mycookingapp.R
import androidx.navigation.NavController
import com.luutran.mycookingapp.data.model.UserRecipe
import com.luutran.mycookingapp.data.utils.DateUtils
import com.luutran.mycookingapp.data.utils.getCookedDishRepositoryInstance
import com.luutran.mycookingapp.data.utils.getUserRecipeRepositoryInstance
import com.luutran.mycookingapp.navigation.NavDestinations
import com.luutran.mycookingapp.ui.auth.AuthViewModel
import com.luutran.mycookingapp.ui.auth.EmailVerificationRequiredScreen
import kotlinx.coroutines.launch


enum class RecipeSourceType {
    LOGGED_ONLINE,
    MY_RECIPES
}

@Composable
fun MyCookedDishOverviewItem(
    cookedDish: CookedDishEntry,
    isSelected: Boolean,
    isSelectionModeActive: Boolean,
    onItemClick: () -> Unit,
    onItemLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .combinedClickable(
                onClick = onItemClick,
                onLongClick = onItemLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionModeActive) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onItemClick() }, // Use onItemClick to toggle selection
                    modifier = Modifier.size(40.dp) // Increase touch target
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(cookedDish.imageUrl)
                    .crossfade(true)
                    .error(R.drawable.placeholder_dish)
                    .placeholder(R.drawable.placeholder_dish)
                    .build(),
                contentDescription = cookedDish.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .clip(MaterialTheme.shapes.small)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cookedDish.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Cooked: ${cookedDish.timesCooked} times",
                    style = MaterialTheme.typography.bodySmall
                )
                if (cookedDish.lastCookedAt != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Last cooked: ${DateUtils.timeAgo(cookedDish.lastCookedAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun MyUserRecipeOverviewItem(
    userRecipe: UserRecipe,
    isSelected: Boolean,
    isSelectionModeActive: Boolean,
    onItemClick: () -> Unit,
    onItemLongClick: () -> Unit,
    onNavigateToUserRecipeMemories: (userRecipeId: String, userRecipeTitle: String, userRecipeImageUrl: String?) -> Unit,
    onNavigateToEditUserRecipe: (recipeId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .combinedClickable(
                onClick = {
                    if (isSelectionModeActive) {
                        onItemClick()
                    } else {
                        val imageUrlToSend = userRecipe.imageUrls.firstOrNull()
                        Log.d(
                            "MyUserRecipeItemClick",
                            "Navigating to memories. UserRecipe ID: ${userRecipe.id}, " +
                                    "Title: '${userRecipe.title}', " +
                                    "ImageURL selected: '$imageUrlToSend', " +
                                    "Full imageUrls list: ${userRecipe.imageUrls}"
                        )
                        onNavigateToUserRecipeMemories(
                            userRecipe.id,
                            userRecipe.title,
                            userRecipe.imageUrls.firstOrNull()
                        )
                    }
                },
                onLongClick = onItemLongClick
            ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelectionModeActive && isSelected) 6.dp else 4.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelectionModeActive && isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionModeActive) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onItemClick() },
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(userRecipe.imageUrls.firstOrNull())
                    .crossfade(true)
                    .error(R.drawable.placeholder_dish)
                    .placeholder(R.drawable.placeholder_dish)
                    .build(),
                contentDescription = userRecipe.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .clip(MaterialTheme.shapes.small)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = userRecipe.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (userRecipe.createdAt != null) {
                    Text(
                        text = "Created: ${DateUtils.timeAgo(userRecipe.createdAt)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = userRecipe.description?.let { desc ->
                        if (desc.length > 100) desc.take(100) + "..." else desc
                    } ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isSelectionModeActive) {
                IconButton(
                    onClick = {
                        onNavigateToEditUserRecipe(userRecipe.id)
                    },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit ${userRecipe.title}",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    showDialog: Boolean,
    selectedItemCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (showDialog && selectedItemCount > 0) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Confirm Deletion") },
            text = {
                Text(
                    if (selectedItemCount == 1) "Are you sure you want to delete this item?"
                    else "Are you sure you want to delete these $selectedItemCount items?"
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyCookedDishesOverviewScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    onNavigateUp: () -> Unit,
    onNavigateToDishMemories: (recipeId: Int, recipeTitle: String, recipeImageUrl: String?) -> Unit,
    onNavigateToUserRecipeMemories: (userRecipeId: String, userRecipeTitle: String, userRecipeImageUrl: String?) -> Unit,
    onNavigateToCreateUserRecipe: () -> Unit,
    onNavigateToViewUserRecipe: (recipeId: String) -> Unit,
    loggedDishesViewModel: MyCookedDishesViewModel = viewModel(
        factory = MyCookedDishesViewModelFactory(getCookedDishRepositoryInstance())
    ),
    userRecipesViewModel: MyUserCookedDishesViewModel = viewModel(
        factory = MyUserCookedDishesViewModelFactory(getUserRecipeRepositoryInstance())
    )
) {
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
    val isEmailVerified by authViewModel.isEmailVerified.collectAsStateWithLifecycle()
    val user = currentUser
    val needsVerification = user != null && !isEmailVerified && user.providerData.any { it.providerId == "password" }

    // --- State for selected recipe source ---
    var selectedRecipeSource by rememberSaveable { mutableStateOf(RecipeSourceType.LOGGED_ONLINE) }

    // --- State from MyCookedDishesViewModel (for Spoonacular/Logged dishes) ---
    val loggedDishesUiState by loggedDishesViewModel.uiState.collectAsStateWithLifecycle()
    val isLoggedDishesSelectionModeActive by loggedDishesViewModel.isSelectionModeActive.collectAsStateWithLifecycle()
    val selectedLoggedDishIds by loggedDishesViewModel.selectedDishIds.collectAsStateWithLifecycle()
    val showLoggedDishesDeleteDialog by loggedDishesViewModel.showDeleteConfirmationDialog.collectAsStateWithLifecycle()
    val showLoggedDishesSortFilterSheet by loggedDishesViewModel.showSortFilterSheet.collectAsStateWithLifecycle()
    val currentLoggedDishesSortOption by loggedDishesViewModel.sortOption.collectAsStateWithLifecycle()

    // --- State from MyUserRecipesViewModel (for user-created recipes) ---
    val userRecipesUiState by userRecipesViewModel.uiState.collectAsStateWithLifecycle()
    val isUserRecipesSelectionModeActive by userRecipesViewModel.isSelectionModeActive.collectAsStateWithLifecycle()
    val selectedUserRecipeIds by userRecipesViewModel.selectedUserRecipeIds.collectAsStateWithLifecycle()
    val showUserRecipesDeleteDialog by userRecipesViewModel.showDeleteConfirmationDialog.collectAsStateWithLifecycle()
    val showUserRecipesSortFilterSheet by userRecipesViewModel.showSortFilterSheet.collectAsStateWithLifecycle() // If applicable
    val currentUserRecipesSortOption by userRecipesViewModel.sortOption.collectAsStateWithLifecycle() // If applicable


    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // --- Determine active VM based on selectedRecipeSource ---
    val isActiveVmLoggedDishes = selectedRecipeSource == RecipeSourceType.LOGGED_ONLINE
    val isSelectionModeActive = if (isActiveVmLoggedDishes) isLoggedDishesSelectionModeActive else isUserRecipesSelectionModeActive
    val selectedIdsCount = if (isActiveVmLoggedDishes) selectedLoggedDishIds.size else selectedUserRecipeIds.size
    val showDeleteDialog = if (isActiveVmLoggedDishes) showLoggedDishesDeleteDialog else showUserRecipesDeleteDialog
    val showSortFilterSheet = if (isActiveVmLoggedDishes) showLoggedDishesSortFilterSheet else showUserRecipesSortFilterSheet
    val currentSortOption = if (isActiveVmLoggedDishes) currentLoggedDishesSortOption else currentUserRecipesSortOption

    LaunchedEffect(showSortFilterSheet, isActiveVmLoggedDishes) {
        if (showSortFilterSheet) {
            scope.launch { sheetState.show() }
        } else {
            if (sheetState.isVisible) {
                scope.launch { sheetState.hide() }
            }
        }
    }

    LaunchedEffect(sheetState.isVisible, showSortFilterSheet, isActiveVmLoggedDishes) {
        if (!sheetState.isVisible && showSortFilterSheet) {
            if (isActiveVmLoggedDishes) {
                loggedDishesViewModel.closeSortFilterSheet()
            } else {
                userRecipesViewModel.closeSortFilterSheet()
            }
        }
    }

    LaunchedEffect(loggedDishesUiState, userRecipesUiState, selectedRecipeSource) {
        when(selectedRecipeSource) {
            RecipeSourceType.LOGGED_ONLINE -> {
                when(loggedDishesUiState) {
                    is MyCookedDishesUiState.Loading -> Log.d("OverviewScreen", "LoggedDishes: Loading")
                    is MyCookedDishesUiState.Success -> Log.d("OverviewScreen", "LoggedDishes: Success, count: ${(loggedDishesUiState as MyCookedDishesUiState.Success).displayedDishes.size}")
                    is MyCookedDishesUiState.Error -> Log.d("OverviewScreen", "LoggedDishes: Error: ${(loggedDishesUiState as MyCookedDishesUiState.Error).message}")
                }
            }
            RecipeSourceType.MY_RECIPES -> {
                when(userRecipesUiState) {
                    is MyUserCookedDishesUiState.Loading -> Log.d("OverviewScreen", "UserRecipes: Loading")
                    is MyUserCookedDishesUiState.Success -> Log.d("OverviewScreen", "UserRecipes: Success, count: ${(userRecipesUiState as MyUserCookedDishesUiState.Success).userRecipes.size}")
                    is MyUserCookedDishesUiState.Error -> Log.d("OverviewScreen", "UserRecipes: Error: ${(userRecipesUiState as MyUserCookedDishesUiState.Error).message}")
                }
            }
        }
    }



    if (user != null && !needsVerification) {
        DeleteConfirmationDialog(
            showDialog = showDeleteDialog,
            selectedItemCount = selectedIdsCount,
            onConfirm = {
                if (isActiveVmLoggedDishes) {
                    loggedDishesViewModel.confirmDeleteSelectedDishes()
                } else {
                    userRecipesViewModel.confirmDeleteSelectedUserRecipes()
                }
            },
            onDismiss = {
                if (isActiveVmLoggedDishes) {
                    loggedDishesViewModel.cancelDeleteConfirmation()
                } else {
                    userRecipesViewModel.cancelDeleteConfirmation()
                }
            }
        )
    }

    Scaffold(
        topBar = {
            if (user != null && !needsVerification && isSelectionModeActive) {
                TopAppBar(
                    title = { Text("$selectedIdsCount selected") },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (isActiveVmLoggedDishes) loggedDishesViewModel.exitSelectionMode()
                            else userRecipesViewModel.exitSelectionMode()
                        }) { Icon(Icons.Filled.Close, "Exit selection mode") }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                if (isActiveVmLoggedDishes) loggedDishesViewModel.requestDeleteConfirmation()
                                else userRecipesViewModel.requestDeleteConfirmation()
                            },
                            enabled = selectedIdsCount > 0
                        ) { Icon(Icons.Filled.Delete, "Delete selected") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = LightGreenApp,
                        titleContentColor = OnLightGreenApp,
                        navigationIconContentColor = OnLightGreenApp,
                        actionIconContentColor = OnLightGreenApp
                    )
                )
            } else if (user != null && !needsVerification) {
                TopAppBar(
                    title = { Text(if(isActiveVmLoggedDishes) "Logged Dishes" else "My Recipes") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateUp) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { navController.navigate(NavDestinations.DEDICATED_SEARCH_SCREEN) }) {
                            Icon(Icons.Filled.Search, "Search All Dishes")
                        }
                        IconButton(onClick = {
                            if (isActiveVmLoggedDishes) loggedDishesViewModel.openSortFilterSheet()
                            else userRecipesViewModel.openSortFilterSheet() // If MyUserRecipes has sort/filter
                        }) { Icon(Icons.AutoMirrored.Filled.Sort, "Sort/Filter") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = LightGreenApp,
                        titleContentColor = OnLightGreenApp,
                        navigationIconContentColor = OnLightGreenApp,
                        actionIconContentColor = OnLightGreenApp
                    )
                )
            } else {
                TopAppBar(
                    title = { Text(if (user == null) "Sign In Required" else "Email Verification") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateUp) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = LightGreenApp,
                        titleContentColor = OnLightGreenApp,
                        navigationIconContentColor = OnLightGreenApp
                    )
                )
            }
        },
        floatingActionButton = {
            // Show FAB only for "My Recipes" and if not in selection mode, and user is verified
            if (selectedRecipeSource == RecipeSourceType.MY_RECIPES &&
                user != null && !needsVerification && !isUserRecipesSelectionModeActive) {
                FloatingActionButton(
                    onClick = onNavigateToCreateUserRecipe,
                    containerColor = LightGreenApp,
                    contentColor = OnLightGreenApp
                ) {
                    Icon(Icons.Filled.Add, "Create new recipe")
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)) {
            // --- Filter Chips ---
            if (user != null && !needsVerification) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    FilterChip(
                        selected = selectedRecipeSource == RecipeSourceType.LOGGED_ONLINE,
                        onClick = { selectedRecipeSource = RecipeSourceType.LOGGED_ONLINE },
                        label = { Text("Logged Dishes") },
                        leadingIcon = if (selectedRecipeSource == RecipeSourceType.LOGGED_ONLINE) {
                            { Icon(Icons.Filled.Check, contentDescription = "Selected") }
                        } else null
                    )
                    FilterChip(
                        selected = selectedRecipeSource == RecipeSourceType.MY_RECIPES,
                        onClick = { selectedRecipeSource = RecipeSourceType.MY_RECIPES },
                        label = { Text("My Recipes") },
                        leadingIcon = if (selectedRecipeSource == RecipeSourceType.MY_RECIPES) {
                            { Icon(Icons.Filled.Check, contentDescription = "Selected") }
                        } else null
                    )
                }
            }
            // Main content decisions based on auth state
            if (user == null) {
                // User not signed in - Prompt to sign in
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Please Sign In", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Sign in to track the dishes you've cooked and build your culinary memory lane.", textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { navController.navigate(NavDestinations.AUTH_SCREEN) /* { popUpTo ... } */ }) {
                        Text("Sign In / Sign Up")
                    }
                }
            } else if (needsVerification) {
                // User needs to verify email - Show verification prompt
                EmailVerificationRequiredScreen(
                    modifier = Modifier.fillMaxSize(),
                    authViewModel = authViewModel,
                    navController = navController,
                    featureName = "Cooked Dishes History"
                )
            } else {
                // User is signed in and verified - Show Cooked Dishes content
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    when (selectedRecipeSource) {
                        RecipeSourceType.LOGGED_ONLINE -> {
                            when (val currentUiState = loggedDishesUiState) { // Use specific VM state
                                is MyCookedDishesUiState.Loading -> {
                                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                                }
                                is MyCookedDishesUiState.Error -> {
                                    Column(
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text("Error: ${currentUiState.message}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(onClick = { loggedDishesViewModel.fetchMyCookedDishes() }) { Text("Retry") }
                                    }
                                }
                                is MyCookedDishesUiState.Success -> {
                                    if (currentUiState.displayedDishes.isEmpty()) {
                                        Text(
                                            text = "You haven't logged any Spoonacular dishes yet.",
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .padding(horizontal = 32.dp),
                                            style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center
                                        )
                                    } else {
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            contentPadding = PaddingValues(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            items(currentUiState.displayedDishes, key = { it.recipeId }) { dish ->
                                                MyCookedDishOverviewItem(
                                                    cookedDish = dish,
                                                    isSelected = selectedLoggedDishIds.contains(dish.recipeId),
                                                    isSelectionModeActive = isLoggedDishesSelectionModeActive,
                                                    onItemClick = {
                                                        if (isLoggedDishesSelectionModeActive) {
                                                            loggedDishesViewModel.toggleDishSelection(dish.recipeId)
                                                        } else {
                                                            try {
                                                                onNavigateToDishMemories(
                                                                    dish.recipeId.toInt(), dish.title, dish.imageUrl
                                                                )
                                                            } catch (e: NumberFormatException) {
                                                                Log.e("OverviewScreen", "Invalid recipeId for navigation: ${dish.recipeId}", e)
                                                            }
                                                        }
                                                    },
                                                    onItemLongClick = {
                                                        if (!isLoggedDishesSelectionModeActive) {
                                                            loggedDishesViewModel.enterSelectionMode()
                                                        }
                                                        loggedDishesViewModel.toggleDishSelection(dish.recipeId)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        RecipeSourceType.MY_RECIPES -> {
                            when (val currentUiState = userRecipesUiState) {
                                is MyUserCookedDishesUiState.Loading -> {
                                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                                }
                                is MyUserCookedDishesUiState.Error -> {
                                    Column(
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text("Error: ${currentUiState.message}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(onClick = { userRecipesViewModel.fetchMyUserCookedDishes() }) { Text("Retry") }
                                    }
                                }
                                is MyUserCookedDishesUiState.Success -> {
                                    if (currentUiState.userRecipes.isEmpty()) {
                                        Text(
                                            text = "You haven't created any recipes yet. Tap the '+' button to add one!",
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .padding(horizontal = 32.dp),
                                            style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center
                                        )
                                    } else {
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            contentPadding = PaddingValues(16.dp),
                                        ) {
                                            items(currentUiState.userRecipes, key = { it.id }) { recipe ->
                                                MyUserRecipeOverviewItem(
                                                    userRecipe = recipe,
                                                    isSelected = selectedUserRecipeIds.contains(
                                                        recipe.id
                                                    ),
                                                    isSelectionModeActive = isUserRecipesSelectionModeActive,
                                                    onItemClick = {
                                                        if (isUserRecipesSelectionModeActive) {
                                                            userRecipesViewModel.toggleUserRecipeSelection(
                                                                recipe.id
                                                            )
                                                        }
                                                    },
                                                    onItemLongClick = {
                                                        if (!isUserRecipesSelectionModeActive) {
                                                            userRecipesViewModel.enterSelectionMode()
                                                        }
                                                        userRecipesViewModel.toggleUserRecipeSelection(
                                                            recipe.id
                                                        )
                                                    },
                                                    onNavigateToUserRecipeMemories = onNavigateToUserRecipeMemories,
                                                    onNavigateToEditUserRecipe = { userRecipeIdToEdit ->
                                                        onNavigateToViewUserRecipe(userRecipeIdToEdit) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet for Sort & Filter (only if user is verified)
    if (user != null && !needsVerification && showSortFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { loggedDishesViewModel.closeSortFilterSheet() },
            sheetState = sheetState,
        ) {
            SortAndFilterBottomSheet(
                currentSortOption = currentSortOption,
                onSortOptionSelected = { selectedOption ->
                    loggedDishesViewModel.onSortOptionSelected(selectedOption)
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) loggedDishesViewModel.closeSortFilterSheet()
                    }
                },
                onDismiss = { loggedDishesViewModel.closeSortFilterSheet() }
            )
        }
    }
}