package com.luutran.mycookingapp.ui.dishmemories


import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.error
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.luutran.mycookingapp.R
import com.luutran.mycookingapp.data.repository.RecipeRepository
import com.luutran.mycookingapp.data.utils.getCookedDishRepositoryInstance
import com.luutran.mycookingapp.ui.recipedetail.getRecipeRepository
import com.luutran.mycookingapp.ui.theme.LightGreenApp
import com.luutran.mycookingapp.ui.theme.OnLightGreenApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateNewMemoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: CreateNewMemoryViewModel = viewModel(
        factory = CreateNewMemoryViewModelFactory(
            getCookedDishRepositoryInstance(),
            getRecipeRepository()
        )
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val multiplePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                Log.d("CreateNewMemoryScreen", "Selected URIs: $uris")
                viewModel.onImagesSelected(uris)
            }
        }
    )

    LaunchedEffect(uiState.saveError) {
        if (uiState.saveError != null) {
            snackbarHostState.showSnackbar(
                message = uiState.saveError!!,
                duration = SnackbarDuration.Short
            )
            viewModel.resetSaveStatus()
        }
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            val message = if (uiState.isEditMode) "Memory updated successfully!" else "Memory saved successfully!"
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.resetSaveStatus()
            onNavigateBack()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    val titleText = if (uiState.isEditMode) {
                        "Edit Memory for ${uiState.recipeTitle.ifEmpty { "Recipe" }}"
                    } else {
                        "Add Memory for ${uiState.recipeTitle.ifEmpty { "Recipe" }}"
                    }
                    Text(titleText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (uiState.isSaving || uiState.isLoading) { // Show progress if saving or loading existing memory
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = OnLightGreenApp)
                    } else {
                        IconButton(onClick = { viewModel.saveMemory() }) { // Call saveMemory
                            Icon(Icons.Filled.Check, contentDescription = if (uiState.isEditMode) "Update Memory" else "Save Memory", tint = OnLightGreenApp)
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
        if (uiState.isLoading && uiState.isEditMode) { // Show full screen loader when initially loading memory for edit
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
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
                StarRatingInput(
                    currentRating = uiState.rating,
                    onRatingChange = { viewModel.onRatingChange(it) }
                )
                Spacer(Modifier.height(24.dp))

                OutlinedTextField(
                    value = uiState.notes,
                    onValueChange = { viewModel.onNotesChange(it) },
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
                    Text("Add More Photos (${uiState.selectedImageUris.size})")
                }
                Spacer(Modifier.height(16.dp))

                // Display Existing Images (in edit mode)
                if (uiState.isEditMode && uiState.existingImageUrls.isNotEmpty()) {
                    Text("Current Photos:", style = MaterialTheme.typography.titleSmall, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(uiState.existingImageUrls) { imageUrl ->
                            val isMarkedForDeletion = uiState.imagesToDelete.contains(imageUrl)
                            ExistingImagePreviewItem(
                                imageUrl = imageUrl,
                                isMarkedForDeletion = isMarkedForDeletion,
                                onToggleDeletion = { viewModel.onToggleExistingImageForDeletion(imageUrl) }
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // Display Newly Selected Images (URIs)
                if (uiState.selectedImageUris.isNotEmpty()) {
                    Text(if(uiState.isEditMode) "New Photos to Add:" else "Selected Photos:", style = MaterialTheme.typography.titleSmall, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(uiState.selectedImageUris) { uri ->
                            NewImagePreviewItem(uri = uri, onRemoveImage = { viewModel.onRemoveNewImage(uri) })
                        }
                    }
                }

                if (uiState.isEditMode && uiState.existingImageUrls.isEmpty() && uiState.selectedImageUris.isEmpty()) {
                    Text("No photos added yet. Click 'Add More Photos' to include some.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 8.dp))
                } else if (!uiState.isEditMode && uiState.selectedImageUris.isEmpty()) {
                    Text("No photos selected yet. Click 'Add Photos' to include some.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
fun NewImagePreviewItem(uri: Uri, onRemoveImage: (Uri) -> Unit) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
    ) {
        Image(
            painter = rememberAsyncImagePainter(model = uri),
            contentDescription = "New image preview",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        IconButton(
            onClick = { onRemoveImage(uri) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                .size(24.dp)
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Remove new image",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun ExistingImagePreviewItem(
    imageUrl: String,
    isMarkedForDeletion: Boolean,
    onToggleDeletion: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onToggleDeletion)
            .border(
                width = 2.dp,
                color = if (isMarkedForDeletion) Color.Red.copy(alpha = 0.7f) else Color.LightGray,
                shape = RoundedCornerShape(8.dp)
            )

    ) {
        Image(
            painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .placeholder(R.drawable.image_placeholder)
                    .error(R.drawable.image_placeholder)
                    .build()
            ),
            contentDescription = "Existing image preview",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = if (isMarkedForDeletion) 0.5f else 1.0f
        )
        if (isMarkedForDeletion) {
            Icon(
                Icons.Filled.DeleteForever,
                contentDescription = "Marked for deletion",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    .padding(8.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .size(24.dp)
            ){
                Icon(
                    Icons.Filled.RemoveCircleOutline, // Icon indicating can be marked for removal
                    contentDescription = "Tap to mark for deletion",
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(16.dp)
                )
            }
        }
    }
}

@Composable
fun StarRatingInput(
    maxStars: Int = 5,
    currentRating: Int,
    onRatingChange: (Int) -> Unit,
    starSize: Dp = 36.dp,
    starColor: Color = MaterialTheme.colorScheme.primary
) {
    Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
        for (star in 1..maxStars) {
            Icon(
                imageVector = if (star <= currentRating) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = "Star $star",
                tint = starColor,
                modifier = Modifier
                    .size(starSize)
                    .clickable { onRatingChange(star) }
                    .padding(4.dp)
            )
        }
    }
}