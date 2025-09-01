package com.luutran.mycookingapp.ui.dishmemories

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.luutran.mycookingapp.R
import com.luutran.mycookingapp.data.utils.DateUtils
import com.luutran.mycookingapp.data.utils.getCookedDishRepositoryInstance
import com.luutran.mycookingapp.ui.theme.LightGreenApp
import com.luutran.mycookingapp.ui.theme.OnLightGreenApp

@Composable
fun getUserRecipeMemoryDetailViewModelFactory(): UserRecipeMemoryDetailViewModelFactory {
    return UserRecipeMemoryDetailViewModelFactory(getCookedDishRepositoryInstance())
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserRecipeMemoryDetailScreen(
    onNavigateUp: () -> Unit,
    onNavigateToEditUserRecipeMemory: (userRecipeId: String, memoryId: String) -> Unit,
    viewModel: UserRecipeMemoryDetailViewModel = viewModel(
        factory = getUserRecipeMemoryDetailViewModelFactory()
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (viewModel.uiState.value is UserRecipeMemoryDetailUiState.Success ||
                    viewModel.uiState.value is UserRecipeMemoryDetailUiState.Error) {
                    Log.d("UserRecipeDetailScreen", "ON_RESUME: Triggering ViewModel refresh.")
                    viewModel.refreshMemoryDetails()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // LaunchedEffect for snackbar messages (e.g., delete errors)
    LaunchedEffect(uiState) {
        val currentUiState = uiState
        if (currentUiState is UserRecipeMemoryDetailUiState.Error && currentUiState.message.isNotBlank()) {
            snackbarHostState.showSnackbar(
                message = currentUiState.message,
                duration = SnackbarDuration.Short
            )
        }
    }

    if (showDeleteConfirmationDialog && uiState is UserRecipeMemoryDetailUiState.Success) {
        val successState = uiState as UserRecipeMemoryDetailUiState.Success
        AlertDialog(
            onDismissRequest = { showDeleteConfirmationDialog = false },
            title = { Text("Delete Memory?") },
            text = { Text("Are you sure you want to delete this memory from '${successState.userRecipeTitle}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteThisUserRecipeMemory { success ->
                            if (success) {
                                onNavigateUp() // Navigate back after successful deletion
                            }
                            // Error is handled by LaunchedEffect above showing snackbar
                        }
                        showDeleteConfirmationDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (val state = uiState) {
                            is UserRecipeMemoryDetailUiState.Success -> state.userRecipeTitle
                            is UserRecipeMemoryDetailUiState.Error -> state.userRecipeTitle ?: "Error"
                            UserRecipeMemoryDetailUiState.Loading -> "Loading Memory..."
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (uiState is UserRecipeMemoryDetailUiState.Success) {
                        val successState = uiState as UserRecipeMemoryDetailUiState.Success
                        // Edit Icon
                        IconButton(onClick = {
                            onNavigateToEditUserRecipeMemory(successState.userRecipeId, successState.memory.id)
                        }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit Memory")
                        }
                        // Delete Icon
                        IconButton(onClick = { showDeleteConfirmationDialog = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete Memory")
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
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (val state = uiState) {
                UserRecipeMemoryDetailUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is UserRecipeMemoryDetailUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        state.userRecipeTitle?.let {
                            Text(
                                "Details for User Recipe: $it",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
                is UserRecipeMemoryDetailUiState.Success -> {
                    val memory = state.memory
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "Memory for: ${state.userRecipeTitle}", // Display user recipe title
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = DateUtils.formatTimestamp(memory.timestamp, "MMMM dd, yyyy 'at' hh:mma"),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Spacer(Modifier.height(8.dp))

                        if (memory.rating > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Rating: ", style = MaterialTheme.typography.titleMedium)
                                StarRatingDisplay(rating = memory.rating, size = 20.dp)
                            }
                            Spacer(Modifier.height(16.dp))
                        }

                        if (memory.notes.isNotBlank()) {
                            Text("Notes:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(4.dp))
                            Text(memory.notes, style = MaterialTheme.typography.bodyLarge, lineHeight = 22.sp)
                            Spacer(Modifier.height(16.dp))
                        }

                        if (memory.imageUrls.isNotEmpty()) {
                            Text("Photos:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(memory.imageUrls) { imageUrl ->
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(imageUrl)
                                            .crossfade(true)
                                            .placeholder(R.drawable.image_placeholder)
                                            .error(R.drawable.image_placeholder)
                                            .build(),
                                        contentDescription = "User recipe memory image",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .height(200.dp)
                                            .aspectRatio(3f / 4f)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                        }

                        if (memory.imageUrls.isEmpty() && memory.notes.isBlank() && memory.rating == 0) {
                            Text(
                                "No details recorded for this memory.",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(top = 20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}