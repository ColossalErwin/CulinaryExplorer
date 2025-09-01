package com.luutran.mycookingapp.ui.dishmemories

import androidx.activity.result.PickVisualMediaRequest

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack // Corrected import
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.luutran.mycookingapp.R
import com.luutran.mycookingapp.data.utils.getCookedDishRepositoryInstance


val LightGreenApp = Color(0xFFC8E6C9)
val OnLightGreenApp = Color(0xFF003D00)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserRecipeCreateNewMemoryScreen(
    onNavigateUp: () -> Unit,
    navController: NavController,
    userRecipeId: String,
    memoryId: String? = null
) {
    val cookedDishRepository = getCookedDishRepositoryInstance()
    val savedStateRegistryOwner = LocalSavedStateRegistryOwner.current
    val focusManager = LocalFocusManager.current

    val viewModel: UserRecipeMemoryViewModel = viewModel(
        factory = UserRecipeMemoryViewModelFactory(
            cookedDishRepository = cookedDishRepository,
            owner = savedStateRegistryOwner,
            defaultArgs = android.os.Bundle().apply {
                putString("userRecipeId", userRecipeId)
                memoryId?.let { putString("memoryId", it) }
            }
        )
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val multiplePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                Log.d("UserRecipeCreateMemory", "Selected URIs: $uris")
                uris.forEach { viewModel.addLocalImageUri(it) }
            }
        }
    )

    LaunchedEffect(uiState.error) {
        uiState.error?.let { errorMessage ->
            snackbarHostState.showSnackbar(
                message = errorMessage,
                duration = SnackbarDuration.Short
            )
            viewModel.resetSaveStatus()
        }
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            val message = if (uiState.memoryId != null) "Memory updated successfully!" else "Memory saved successfully!"
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.resetSaveStatus()
            navController.popBackStack()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    val titleText = if (uiState.memoryId != null) {
                        "Edit User Recipe Memory"
                    } else {
                        "Add User Recipe Memory"
                    }
                    Text(titleText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) { // Use NavController
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (uiState.isSaving || (uiState.isLoading && uiState.memoryId != null && !uiState.initialMemoryLoaded)) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = OnLightGreenApp)
                    } else {
                        IconButton(onClick = {
                            focusManager.clearFocus()
                            viewModel.saveMemory()
                        }) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = if (uiState.memoryId != null) "Update Memory" else "Save Memory",
                                tint = OnLightGreenApp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LightGreenApp,
                    titleContentColor = OnLightGreenApp,
                    navigationIconContentColor = OnLightGreenApp
                )
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading && uiState.memoryId != null && !uiState.initialMemoryLoaded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.userRecipeId.isBlank() && uiState.error?.contains("User Recipe ID is missing") == true){
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("Error: User Recipe ID is missing. Cannot create memory.", color = MaterialTheme.colorScheme.error)
            }
        }
        else {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Rate your experience:", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                StarRatingInput( // Using your StarRatingInput
                    currentRating = uiState.rating,
                    onRatingChange = { viewModel.onRatingChanged(it) }
                )
                Spacer(Modifier.height(24.dp))

                OutlinedTextField(
                    value = uiState.notes,
                    onValueChange = { viewModel.onNotesChanged(it) },
                    label = { Text("Your Notes (optional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        multiplePhotoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Filled.AddAPhoto, contentDescription = "Add Photos", modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Add Photos (${uiState.localImageUrisToAdd.size})")
                }
                Spacer(Modifier.height(16.dp))

                // Display Existing Images (in edit mode) - from uiState.existingImageUrls
                if (uiState.memoryId != null && uiState.existingImageUrls.isNotEmpty()) {
                    Text("Current Photos:", style = MaterialTheme.typography.titleSmall, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(uiState.existingImageUrls) { imageUrl ->
                            ExistingImagePreviewItem(
                                imageUrl = imageUrl,
                                isMarkedForDeletion = uiState.existingImageUrlsToDelete.contains(imageUrl),
                                // This toggles between marking and unmarking
                                onToggleDeletion = {
                                    if (uiState.existingImageUrlsToDelete.contains(imageUrl)) {
                                        viewModel.unmarkExistingImageUrlForDeletion(imageUrl)
                                    } else {
                                        viewModel.markExistingImageUrlForDeletion(imageUrl)
                                    }
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                if (uiState.localImageUrisToAdd.isNotEmpty()) {
                    Text(
                        if (uiState.memoryId != null) "New Photos to Add:" else "Selected Photos:",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(uiState.localImageUrisToAdd) { uri ->
                            NewImagePreviewItem(uri = uri, onRemoveImage = { viewModel.removeLocalImageUri(uri) })
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // Empty state messages
                val noImagesYet = (uiState.memoryId != null && uiState.existingImageUrls.isEmpty() && uiState.localImageUrisToAdd.isEmpty()) ||
                        (uiState.memoryId == null && uiState.localImageUrisToAdd.isEmpty())

                if (noImagesYet) {
                    Text(
                        "No photos added yet. Click 'Add Photos' to include some.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

