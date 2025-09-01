package com.luutran.mycookingapp.ui.favorites

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.luutran.mycookingapp.R
import com.luutran.mycookingapp.ui.theme.LightGreenApp
import com.luutran.mycookingapp.ui.theme.OnLightGreenApp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.luutran.mycookingapp.navigation.NavDestinations
import com.luutran.mycookingapp.ui.auth.AuthViewModel
import com.luutran.mycookingapp.ui.auth.EmailVerificationRequiredScreen

@Composable
fun FavoritesScreen(
    navController: NavHostController,
    favoritesViewModel: FavoritesViewModel,
    authViewModel: AuthViewModel,
    onNavigateToRecipeDetail: (recipeId: Int) -> Unit,
    onNavigateUp: () -> Unit
) {
    val uiState by favoritesViewModel.uiState.collectAsStateWithLifecycle()

    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
    val isEmailVerified by authViewModel.isEmailVerified.collectAsStateWithLifecycle()

    val user = currentUser
    val needsVerification = user != null && !isEmailVerified && user.providerData.any { it.providerId == "password" }

    Scaffold(
        topBar = {
            FavoritesTopAppBar(
                selectionModeActive = uiState.selectionModeActive,
                selectedCount = uiState.selectedRecipeIds.size,
                onCloseSelectionMode = { favoritesViewModel.deactivateSelectionMode() },
                onDeleteSelected = { favoritesViewModel.showDeleteConfirmation() },
                onNavigateUp = onNavigateUp,
                currentSortOption = uiState.currentSortOption,
                onSortOptionSelected = { newSortOption ->
                    favoritesViewModel.changeSortOption(newSortOption)
                },
                // Disable sort and other actions if verification is needed
                actionsEnabled = !needsVerification && user != null
            )
        },
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            ) {
                if (user == null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Please Sign In",
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "You need to be signed in to manage your favorites.",
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            navController.navigate(NavDestinations.AUTH_SCREEN) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        }) {
                            Text("Sign In / Sign Up")
                        }
                    }
                } else if (needsVerification) {
                    // Display verification prompt
                    EmailVerificationRequiredScreen(
                        authViewModel = authViewModel,
                        navController = navController,
                        featureName = "Favorites",
                        // No onNavigateUp, so it embeds within this Scaffold's padding
                        modifier = Modifier.padding(paddingValues)
                    )
                } else {
                    // User is logged in and verified (or doesn't need verification, e.g., Google Sign-In)
                    when {
                        uiState.isLoading -> {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }
                        uiState.error != null -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Error: ${uiState.error}", color = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = { favoritesViewModel.clearError() }) {
                                    Text("Dismiss")
                                }
                            }
                        }
                        uiState.favoriteRecipes.isEmpty() && !uiState.isLoading -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_empty_favorites_placeholder),
                                    contentDescription = "No favorites",
                                    modifier = Modifier.size(120.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "No Favorites Yet",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "Tap the heart on a recipe to add it to your favorites.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(
                                    items = uiState.favoriteRecipes,
                                    key = { it.id }
                                ) { recipe ->
                                    FavoriteRecipeItem(
                                        recipe = recipe,
                                        isSelected = uiState.selectedRecipeIds.contains(recipe.id),
                                        selectionModeActive = uiState.selectionModeActive,
                                        onItemClick = {
                                            if (uiState.selectionModeActive) {
                                                favoritesViewModel.toggleSelection(recipe.id)
                                            } else {
                                                onNavigateToRecipeDetail(recipe.id)
                                            }
                                        },
                                        onItemLongClick = {
                                            if (!uiState.selectionModeActive) {
                                                favoritesViewModel.activateSelectionMode()
                                            }
                                            favoritesViewModel.toggleSelection(recipe.id)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                if (uiState.showDeleteConfirmationDialog) {
                    DeleteConfirmationDialog(
                        onConfirm = { favoritesViewModel.deleteSelectedFavorites() },
                        onDismiss = { favoritesViewModel.dismissDeleteConfirmation() },
                        count = uiState.selectedRecipeIds.size
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesTopAppBar(
    selectionModeActive: Boolean,
    selectedCount: Int,
    onCloseSelectionMode: () -> Unit,
    onDeleteSelected: () -> Unit,
    onNavigateUp: () -> Unit,
    currentSortOption: FavoriteSortOption,
    onSortOptionSelected: (FavoriteSortOption) -> Unit,
    actionsEnabled: Boolean
) {
    var showSortMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Text(if (selectionModeActive && actionsEnabled) "$selectedCount selected" else "My Favorites")
        },
        navigationIcon = {
            if (selectionModeActive && actionsEnabled) {
                IconButton(onClick = onCloseSelectionMode) {
                    Icon(Icons.Filled.Close, contentDescription = "Close selection mode")
                }
            } else {
                IconButton(onClick = onNavigateUp) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        },
        actions = {
            if (actionsEnabled) { // Only show actions if enabled
                if (selectionModeActive) {
                    if (selectedCount > 0) {
                        IconButton(onClick = onDeleteSelected) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete selected favorites")
                        }
                    }
                } else {
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort favorites")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            FavoriteSortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.displayName) },
                                    onClick = {
                                        onSortOptionSelected(option)
                                        showSortMenu = false
                                    },
                                    colors = if (option == currentSortOption) {
                                        MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.primary)
                                    } else {
                                        MenuDefaults.itemColors()
                                    }
                                )
                            }
                        }
                    }
                }
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

@Composable
fun FavoriteRecipeItem(
    recipe: FavoriteRecipeDisplayItem,
    isSelected: Boolean,
    selectionModeActive: Boolean,
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
        elevation = CardDefaults.cardElevation(if (isSelected) 4.dp else 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(recipe.imageUrl)
                    .crossfade(true)
                    .error(R.drawable.image_placeholder_error)
                    .placeholder(R.drawable.image_placeholder_error)
                    .build(),
                contentDescription = recipe.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(MaterialTheme.shapes.small)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = recipe.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (selectionModeActive) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onItemClick() }, // Let the combinedClickable's onClick handle logic
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    count: Int
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Favorites?") },
        text = { Text("Are you sure you want to delete $count selected favorite${if (count == 1) "" else "s"}? This action cannot be undone.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}