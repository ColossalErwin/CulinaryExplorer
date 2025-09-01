package com.luutran.mycookingapp.ui.dishmemories

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.savedstate.SavedStateRegistryOwner
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.luutran.mycookingapp.R
import com.luutran.mycookingapp.data.model.DishMemory
import com.luutran.mycookingapp.data.repository.UserRecipeRepository
import com.luutran.mycookingapp.data.utils.DateUtils
import com.luutran.mycookingapp.data.utils.getUserRecipeRepositoryInstance
import com.luutran.mycookingapp.navigation.NavDestinations
import com.luutran.mycookingapp.ui.theme.LightGreenApp
import com.luutran.mycookingapp.ui.theme.OnLightGreenApp


// --- ViewModel Factory ---
class UserRecipeMemoriesViewModelFactory(
    private val userRecipeRepository: UserRecipeRepository,
    owner: SavedStateRegistryOwner,
    defaultArgs: Bundle? = null
) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {

    override fun <T : ViewModel> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle
    ): T {
        if (modelClass.isAssignableFrom(UserRecipeMemoriesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UserRecipeMemoriesViewModel(handle, userRecipeRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

// UserRecipeInfoTile (if different from API RecipeInfoTile)
@Composable
fun UserRecipeInfoTile(
    userRecipeTitle: String,
    userRecipeImageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(userRecipeImageUrl)
                    .crossfade(true)
                    .placeholder(R.drawable.placeholder_dish)
                    .error(R.drawable.placeholder_dish)
                    .build(),
                contentDescription = userRecipeTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .clip(MaterialTheme.shapes.small)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = userRecipeTitle,
                    style = MaterialTheme.typography.titleLarge, // Slightly larger title for emphasis
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap to view full recipe details",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant // Subdued color
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "View recipe details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserRecipeMemoriesListScreen(
    navController: NavController,
    onNavigateUp: () -> Unit,
    onNavigateToCreateNewMemory: (userRecipeId: String) -> Unit,
    onNavigateToUserRecipeMemoryDetail: (userRecipeId: String, userRecipeTitle: String, memoryId: String) -> Unit,
    onNavigateToUserRecipeDetail: (userRecipeId: String) -> Unit,
    viewModel: UserRecipeMemoriesViewModel = viewModel(
        factory = UserRecipeMemoriesViewModelFactory(
            userRecipeRepository = getUserRecipeRepositoryInstance(),
            owner = LocalSavedStateRegistryOwner.current,
            defaultArgs = navController.currentBackStackEntry?.arguments
                ?: LocalActivity.current?.intent?.extras
        )
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSortDialog by remember { mutableStateOf(false) }

    // --- Listen for the result from UserRecipeDetailScreen ---
    val currentBackStackEntry = navController.currentBackStackEntry
    val recipeUpdatedForMemories = currentBackStackEntry
        ?.savedStateHandle
        ?.getLiveData<Boolean>(NavDestinations.NavigationResultKeys.RECIPE_UPDATED_FOR_MEMORIES_KEY)
        ?.observeAsState()
    val updatedRecipeIdFromResult = currentBackStackEntry
        ?.savedStateHandle
        ?.getLiveData<String>(NavDestinations.NavigationResultKeys.RECIPE_ID_KEY)
        ?.observeAsState()

    LaunchedEffect(recipeUpdatedForMemories?.value, updatedRecipeIdFromResult?.value) {
        if (recipeUpdatedForMemories?.value == true && updatedRecipeIdFromResult?.value != null) {
            val recipeIdFromDetail = updatedRecipeIdFromResult.value!!
            Log.d("UserRecipeMemoriesScreen", "Result received: Recipe (ID: $recipeIdFromDetail) might be updated.")

            if (viewModel.userRecipeId == recipeIdFromDetail) {
                Log.d("UserRecipeMemoriesScreen", "Refreshing recipe details for $recipeIdFromDetail.")
                viewModel.refreshRecipeDetailsIfNeeded()
            } else {
                Log.d("UserRecipeMemoriesScreen", "Update was for $recipeIdFromDetail, but this screen shows ${viewModel.userRecipeId}.")
            }

            currentBackStackEntry.savedStateHandle.remove<Boolean>(NavDestinations.NavigationResultKeys.RECIPE_UPDATED_FOR_MEMORIES_KEY)
            currentBackStackEntry.savedStateHandle.remove<String>(NavDestinations.NavigationResultKeys.RECIPE_ID_KEY)
        }
    }
    // --- End of result listener ---

    LaunchedEffect(uiState) {
        val currentUiState = uiState
        if (currentUiState is UserRecipeMemoriesUiState.Error && currentUiState.message.isNotBlank()) {
            val result = snackbarHostState.showSnackbar(
                message = currentUiState.message,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.Dismissed) {
                viewModel.clearError()
            }
        }
    }

    val currentSuccessState = uiState as? UserRecipeMemoriesUiState.Success
    if (currentSuccessState?.showDeleteConfirmationDialog == true) {
        DeleteConfirmationDialog(
            onConfirm = { viewModel.confirmDeleteSelectedMemories() },
            onDismiss = { viewModel.cancelDeleteConfirmation() },
            selectedItemCount = currentSuccessState.selectedMemoryIds.size
        )
    }

    if (showSortDialog) {
        val currentSuccessStateForSort = uiState as? UserRecipeMemoriesUiState.Success
        if (currentSuccessStateForSort != null) {
            SortMemoriesDialog(
                currentSortOption = currentSuccessStateForSort.currentSortOption,
                onDismiss = { showSortDialog = false },
                onSortOptionSelected = { selectedOption ->
                    viewModel.setSortOption(selectedOption)
                    showSortDialog = false
                }
            )
        } else {
            // If state is not success when dialog is requested, dismiss it.
            // This is unlikely if button to show is only enabled in success state.
            LaunchedEffect(Unit) { showSortDialog = false }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            val state = uiState
            val selectionModeActive = (state as? UserRecipeMemoriesUiState.Success)?.selectionMode == true
            val selectedCount = (state as? UserRecipeMemoriesUiState.Success)?.selectedMemoryIds?.size ?: 0

            val appBarTitle = when (state) {
                is UserRecipeMemoriesUiState.Success -> if (selectionModeActive) "$selectedCount Selected" else "Memories: ${state.userRecipeTitle.take(30) + if (state.userRecipeTitle.length > 30) "..." else ""}"
                is UserRecipeMemoriesUiState.Error -> state.userRecipeTitle?.let { "Memories: $it" } ?: "Recipe Memories"
                is UserRecipeMemoriesUiState.Loading -> "Loading Memories..."
            }

            TopAppBar(
                title = { Text(text = appBarTitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    if (selectionModeActive) {
                        IconButton(onClick = { viewModel.cancelSelectionMode() }) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancel Selection")
                        }
                    } else {
                        IconButton(onClick = onNavigateUp) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (state is UserRecipeMemoriesUiState.Success) {
                        if (state.selectionMode) {
                            IconButton(
                                onClick = { viewModel.requestDeleteSelectedMemories() },
                                enabled = state.selectedMemoryIds.isNotEmpty()
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete Selected")
                            }
                        } else {
                            if (state.memories.isNotEmpty()) {
                                IconButton(onClick = { showSortDialog = true }) {
                                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort Memories")
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
        },
        floatingActionButton = {
            val successStateFAB = uiState as? UserRecipeMemoriesUiState.Success
            if (successStateFAB != null && !successStateFAB.selectionMode) {
                FloatingActionButton(
                    onClick = {
                        // Ensure userRecipeId from ViewModel is used, which is from SavedStateHandle
                        onNavigateToCreateNewMemory(viewModel.userRecipeId)
                    },
                    containerColor = LightGreenApp,
                    contentColor = OnLightGreenApp
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add New Memory")
                }
            }
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is UserRecipeMemoriesUiState.Loading -> {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is UserRecipeMemoriesUiState.Error -> {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Error: ${state.message}",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            is UserRecipeMemoriesUiState.Success -> {
                Log.d(
                    "UserRecipeMemoriesScreen",
                    "Displaying Success state. " +
                            "Recipe Title: '${state.userRecipeTitle}', " +
                            "Recipe Image URL for Tile: '${state.userRecipeImageUrl}', " +
                            "Number of memories: ${state.memories.size}"
                )
                if (state.memories.isEmpty()) {
                    Column(modifier = Modifier.padding(paddingValues)) {
                        UserRecipeInfoTile( // Consumes state.userRecipeTitle and state.userRecipeImageUrl
                            userRecipeTitle = state.userRecipeTitle,
                            userRecipeImageUrl = state.userRecipeImageUrl, // Will be the first image URL
                            onClick = {
                                onNavigateToUserRecipeDetail(state.userRecipeId)
                            }
                        )
                        EmptyMemoriesView(
                            recipeName = state.userRecipeTitle,
                            onAddFirstMemoryClick = { onNavigateToCreateNewMemory(state.userRecipeId) }
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            UserRecipeInfoTile( // Consumes state.userRecipeTitle and state.userRecipeImageUrl
                                userRecipeTitle = state.userRecipeTitle,
                                userRecipeImageUrl = state.userRecipeImageUrl, // Will be the first image URL
                                onClick = {
                                    onNavigateToUserRecipeDetail(state.userRecipeId)
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        item {
                            Text(
                                text = "Your cooking memories for this dish:",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .fillMaxWidth(),
                                textAlign = TextAlign.Start
                            )
                        }
                        items(state.memories, key = { it.id }) { memory ->
                            val isSelected = state.selectedMemoryIds.contains(memory.id)
                            UserRecipeMemoryListItem(
                                memory = memory,
                                isSelected = isSelected,
                                selectionMode = state.selectionMode,
                                onItemClick = {
                                    if (state.selectionMode) {
                                        viewModel.toggleMemorySelection(memory.id)
                                    } else {
                                        onNavigateToUserRecipeMemoryDetail(
                                            state.userRecipeId,
                                            state.userRecipeTitle,
                                            memory.id
                                        )
                                    }
                                },
                                onItemLongClick = {
                                    if (!state.selectionMode) {
                                        viewModel.enterSelectionMode(memory.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserRecipeMemoryListItem(
    memory: DishMemory,
    isSelected: Boolean,
    selectionMode: Boolean,
    onItemClick: () -> Unit,
    onItemLongClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onItemClick,
                onLongClick = onItemLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onItemClick() },
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
            }

            if (memory.imageUrls.isNotEmpty() && memory.imageUrls.first().isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(memory.imageUrls.first())
                        .crossfade(true)
                        .placeholder(R.drawable.image_placeholder)
                        .error(R.drawable.image_placeholder)
                        .build(),
                    contentDescription = "Memory image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(if (selectionMode) 56.dp else 64.dp)
                        .clip(MaterialTheme.shapes.small)
                )
                Spacer(Modifier.width(12.dp))
            } else if (selectionMode) {
                Spacer(Modifier.width(64.dp + 12.dp))
            }


            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = DateUtils.formatTimestamp(memory.timestamp, "MMM dd, yyyy 'at' hh:mma"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (memory.rating > 0) {
                    StarRatingDisplay(rating = memory.rating, size = 16.dp)
                    Spacer(Modifier.height(4.dp))
                }
                if (memory.notes.isNotBlank()) {
                    Text(
                        text = memory.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyMemoriesView(
    recipeName: String,
    onAddFirstMemoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_empty_memories_box),
                contentDescription = "No memories icon",
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "No Memories Yet for",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = recipeName,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Tap the '+' button to add your first cooking experience or memory related to this recipe.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onAddFirstMemoryClick) {
                Icon(Icons.Filled.Add, contentDescription = "Add memory icon", modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Add First Memory")
            }
        }
    }
}