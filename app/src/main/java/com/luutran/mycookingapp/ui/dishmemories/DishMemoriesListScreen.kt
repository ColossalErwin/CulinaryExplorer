package com.luutran.mycookingapp.ui.dishmemories

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.luutran.mycookingapp.R
import com.luutran.mycookingapp.data.model.DishMemory
import com.luutran.mycookingapp.ui.theme.LightGreenApp
import com.luutran.mycookingapp.ui.theme.OnLightGreenApp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavBackStackEntry
import com.luutran.mycookingapp.data.utils.DateUtils
import com.luutran.mycookingapp.data.repository.CookedDishRepository
import com.luutran.mycookingapp.data.utils.getCookedDishRepositoryInstance
import kotlin.text.isNotBlank

class DishMemoriesListViewModelFactory(
    private val cookedDishRepository: CookedDishRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DishMemoriesListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DishMemoriesListViewModel(savedStateHandle, cookedDishRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

// --- RecipeInfoTile Composable ---
@Composable
fun RecipeInfoTile(
    recipeTitle: String,
    recipeImageUrl: String?,
    onNavigateToRecipeDetail: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp) // Add consistent padding
            .clickable(onClick = onNavigateToRecipeDetail),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium // Consistent shape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(recipeImageUrl)
                    .crossfade(true)
                    .placeholder(R.drawable.placeholder_dish)
                    .error(R.drawable.placeholder_dish)
                    .build(),
                contentDescription = recipeTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp) // Consistent size
                    .clip(MaterialTheme.shapes.small)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recipeTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap to view full recipe details",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

// --- Delete Confirmation Dialog ---
@Composable
fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    selectedItemCount: Int
) {
    if (selectedItemCount == 0) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Memories") },
        text = { Text("Are you sure you want to delete $selectedItemCount selected ${if (selectedItemCount == 1) "memory" else "memories"}? This action cannot be undone.") },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
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


// --- Sort Memories Dialog Composable ---
@Composable
fun SortMemoriesDialog(
    currentSortOption: DishMemorySortOption,
    onDismiss: () -> Unit,
    onSortOptionSelected: (DishMemorySortOption) -> Unit
) {
    val sortOptions = DishMemorySortOption.entries.toTypedArray()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sort Memories By") },
        text = {
            Column {
                sortOptions.forEach { option ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSortOptionSelected(option) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (option == currentSortOption),
                            onClick = { onSortOptionSelected(option) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(option.displayName)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DishMemoriesListScreen(
    // Arguments from NavHost
    recipeIdFromNav: Int,
    encodedRecipeTitleFromNav: String,
    encodedRecipeImageUrlFromNav: String?,
    // Navigation Callbacks
    onNavigateUp: () -> Unit,
    onNavigateToCreateNewMemory: (recipeId: Int) -> Unit,
    onNavigateToDishMemoryDetail: (recipeId: Int, recipeTitle: String, memoryId: String) -> Unit,
    onNavigateToRecipeDetail: (recipeId: Int) -> Unit,
    viewModel: DishMemoriesListViewModel = viewModel(
        viewModelStoreOwner = LocalViewModelStoreOwner.current as NavBackStackEntry,
        factory = DishMemoriesListViewModelFactory(
            cookedDishRepository = getCookedDishRepositoryInstance(),
            savedStateHandle = (LocalViewModelStoreOwner.current as NavBackStackEntry).savedStateHandle
        )
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // --- State for showing sort dialog ---
    var showSortDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        val currentUiState = uiState
        if (currentUiState is DishMemoriesUiState.Error && currentUiState.message.isNotBlank()) {
            snackbarHostState.showSnackbar(
                message = currentUiState.message,
                duration = SnackbarDuration.Short
            )
        }
    }

    // --- Handle Delete Confirmation Dialog ---
    val currentSuccessState = uiState as? DishMemoriesUiState.Success
    if (currentSuccessState?.showDeleteConfirmationDialog == true) {
        DeleteConfirmationDialog(
            onConfirm = { viewModel.confirmDeleteSelectedMemories() },
            onDismiss = { viewModel.cancelDeleteConfirmation() },
            selectedItemCount = currentSuccessState.selectedMemoryIds.size
        )
    }

    if (showSortDialog) {
        val currentSuccessStateForSort = uiState as? DishMemoriesUiState.Success
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
            LaunchedEffect(Unit) { showSortDialog = false }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            val state = uiState
            val selectionModeActive = (state as? DishMemoriesUiState.Success)?.selectionMode == true
            val selectedCount = (state as? DishMemoriesUiState.Success)?.selectedMemoryIds?.size ?: 0

            val appBarTitle = when (state) {
                is DishMemoriesUiState.Success -> if (selectionModeActive) "$selectedCount Selected" else "Memories: ${state.recipeTitle}"
                is DishMemoriesUiState.Error -> state.recipeTitle?.let { "Memories: $it" } ?: "Recipe Memories"
                is DishMemoriesUiState.Loading -> "Loading Memories..."
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
                    if (state is DishMemoriesUiState.Success) {
                        if (state.selectionMode) {
                            // Delete Icon
                            IconButton(
                                onClick = { viewModel.requestDeleteSelectedMemories() }, // Request confirmation
                                enabled = state.selectedMemoryIds.isNotEmpty()
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete Selected")
                            }
                        } else {
                            // --- NEW: Sort Button ---
                            if (state.memories.isNotEmpty()) { // Only show if there are memories to sort
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
            val successStateFAB = uiState as? DishMemoriesUiState.Success
            if (successStateFAB != null && !successStateFAB.selectionMode) { // Hide FAB in selection mode
                FloatingActionButton(
                    onClick = { onNavigateToCreateNewMemory(viewModel.recipeId) },
                    containerColor = LightGreenApp
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add New Memory", tint = OnLightGreenApp)
                }
            }
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is DishMemoriesUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is DishMemoriesUiState.Error -> {
                if (state.recipeTitle != null) {
                    RecipeInfoTile(
                        recipeTitle = state.recipeTitle,
                        recipeImageUrl = state.recipeImageUrl,
                        onNavigateToRecipeDetail = {
                            if (viewModel.recipeId != -1) onNavigateToRecipeDetail(viewModel.recipeId)
                        }
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Failed to load memories.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
            is DishMemoriesUiState.Success -> {
                if (state.memories.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No memories yet. Tap the '+' button to add your first one!",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            RecipeInfoTile(
                                recipeTitle = state.recipeTitle,
                                recipeImageUrl = state.recipeImageUrl,
                                onNavigateToRecipeDetail = { onNavigateToRecipeDetail(state.recipeId) }
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
                            DishMemoryListItem(
                                memory = memory,
                                isSelected = state.selectedMemoryIds.contains(memory.id),
                                selectionMode = state.selectionMode,
                                onItemClick = {
                                    // If in selection mode, toggle selection.
                                    // If not, navigate to detail.
                                    if (state.selectionMode) {
                                        viewModel.toggleMemorySelection(memory.id)
                                    } else {
                                        onNavigateToDishMemoryDetail(state.recipeId, state.recipeTitle, memory.id)
                                    }
                                },
                                onItemLongClick = {
                                    viewModel.enterSelectionMode(memory.id)
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
fun DishMemoryListItem(
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
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 12.dp), // Consistent padding
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
fun StarRatingDisplay(rating: Int, starCount: Int = 5, size: androidx.compose.ui.unit.Dp = 24.dp) {
    Row {
        repeat(starCount) { index ->
            Icon(
                painter = painterResource(id = if (index < rating) R.drawable.ic_star_filled else R.drawable.ic_star_outline),
                contentDescription = null,
                tint = if (index < rating) MaterialTheme.colorScheme.primary else Color.Gray,
                modifier = Modifier.size(size)
            )
        }
    }
}