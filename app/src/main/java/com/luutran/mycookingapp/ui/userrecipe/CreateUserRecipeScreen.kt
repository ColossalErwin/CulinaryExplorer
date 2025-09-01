package com.luutran.mycookingapp.ui.userrecipe

import android.net.Uri
import android.util.Log
// import android.util.Log // Keep if you need for local debugging
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Clear // For removing new images
import androidx.compose.material.icons.filled.DeleteForever // For marked existing images
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Delete // For unmarked existing images
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.luutran.mycookingapp.navigation.NavDestinations
import androidx.compose.ui.res.colorResource
import com.luutran.mycookingapp.R

@Composable
fun BorderedNumberInputWithStepper(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    isDecrementEnabled: Boolean,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Next,
    defaultLabelColor: Color = Color.Gray,
    focusedLabelColor: Color = Color.Blue,
    defaultBorderColor: Color = Color.LightGray,
    focusedBorderColor: Color = Color.Blue,
    defaultTextColor: Color = Color.Black,
    cursorColorValue: Color = Color.Blue
) {
    var isFocused by remember { mutableStateOf(false) }

    //val currentLabelColor = if (isFocused) focusedLabelColor else defaultLabelColor
    //val currentBorderColor = if (isFocused) focusedBorderColor else defaultBorderColor

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall, // Or another appropriate style
            //color = currentLabelColor,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = if (isFocused) 2.dp else 1.dp,
                    color = Color.Gray,
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 16.dp, vertical = 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp)
                    .onFocusChanged { focusState ->
                        isFocused = focusState.isFocused
                    },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = imeAction
                ),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(color = defaultTextColor),
                cursorBrush = SolidColor(cursorColorValue)
            )
            StepperButtons(
                onIncrement = onIncrement,
                onDecrement = onDecrement,
                isDecrementEnabled = isDecrementEnabled
            )
        }
    }
}
@Composable
fun StepperButtons(
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    isDecrementEnabled: Boolean,
    modifier: Modifier = Modifier,
    incrementContentDescription: String = "Increment",
    decrementContentDescription: String = "Decrement"
) {
    Column(modifier = modifier) {
        IconButton(onClick = onIncrement) {
            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = incrementContentDescription)
        }
        IconButton(
            onClick = onDecrement,
            enabled = isDecrementEnabled
        ) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = decrementContentDescription)
        }
    }
}
@Composable
fun NumberStepper(
    label: String,
    value: String, // The current value to display (as String)
    onValueChange: (String) -> Unit, // For direct text input
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier,
    minValue: Int = 1, // Minimum allowed value
    isDecrementEnabled: Boolean // Control whether decrement button is active
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next // Or Done, depending on context
            ),
        )
        Column {
            IconButton(onClick = onIncrement) {
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Increment $label")
            }
            IconButton(
                onClick = onDecrement,
                enabled = isDecrementEnabled // Use the passed-in enabled state
            ) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Decrement $label")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateUserRecipeScreen(
    navController: NavController,
    onNavigateUp: () -> Unit,
    viewModel: CreateUserRecipeViewModel
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    // val context = LocalContext.current // Keep if needed for other purposes

    // Launcher for picking multiple images using the Photo Picker
    val multiplePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5), // Max 5 images
        onResult = { uris: List<Uri> ->
            if (uris.isNotEmpty()) {
                Log.d("CreateUserRecipeScreen", "Selected URIs: $uris")
                viewModel.onNewImagesSelected(uris)
            }
        }
    )

    LaunchedEffect(uiState) {
        val currentFormState = uiState
        if (currentFormState is CreateUserRecipeUiState.EditForm) {
            if (currentFormState.saveSuccess) {
                snackbarHostState.showSnackbar(
                    message = if (currentFormState.isNewRecipe) "Recipe created!" else "Recipe updated!",
                    duration = SnackbarDuration.Short
                ).let {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set(NavDestinations.NavigationResultKeys.RECIPE_UPDATED_FOR_DETAIL_KEY, true)
                    navController.previousBackStackEntry // Also pass the ID
                        ?.savedStateHandle
                        ?.set(NavDestinations.NavigationResultKeys.RECIPE_ID_KEY, currentFormState.recipe.id)

                    Log.d("CreateUserRecipeScreen", "Save success, recipe ID: ${currentFormState.recipe.id}. Navigating up.")
                    navController.popBackStack() // Navigate back
                    viewModel.operationCompletedOrCancelled() // Reset state in VM after navigating
                }
            } else if (currentFormState.error != null) {
                snackbarHostState.showSnackbar(
                    message = "Error: ${currentFormState.error}",
                    duration = SnackbarDuration.Long
                )
            }
        } else if (currentFormState is CreateUserRecipeUiState.Error) {
            snackbarHostState.showSnackbar(
                message = "Error: ${currentFormState.message}",
                duration = SnackbarDuration.Long
            )
        }
    }

    // Call operationCompletedOrCancelled if the user navigates away using the back button
    // without saving, or if the screen is disposed for other reasons.
    DisposableEffect(Unit) {
        onDispose {
            val currentFormState = viewModel.uiState.value
            if (currentFormState is CreateUserRecipeUiState.EditForm && !currentFormState.saveSuccess) {
                viewModel.operationCompletedOrCancelled()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState is CreateUserRecipeUiState.EditForm && !(uiState as CreateUserRecipeUiState.EditForm).isNewRecipe) {
                            "Edit Your Recipe"
                        } else {
                            "Create New Recipe"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.operationCompletedOrCancelled()
                        navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState is CreateUserRecipeUiState.EditForm) {
                        val formState = uiState as CreateUserRecipeUiState.EditForm
                        Button(
                            onClick = { viewModel.saveRecipe() },
                            enabled = !formState.isLoading,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            if (formState.isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Save")
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        when (val currentUiState = uiState) {
            is CreateUserRecipeUiState.Idle, CreateUserRecipeUiState.LoadingRecipe -> {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is CreateUserRecipeUiState.Error -> {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues), contentAlignment = Alignment.Center) {
                    Text("Error: ${currentUiState.message}")
                }
            }
            is CreateUserRecipeUiState.EditForm -> {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = currentUiState.recipe.title,
                        onValueChange = { viewModel.onTitleChanged(it) },
                        label = { Text("Recipe Title*") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = currentUiState.error?.contains("Title", ignoreCase = true) == true,
                        keyboardOptions = KeyboardOptions.Default.copy(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next)
                    )

                    OutlinedTextField(
                        value = currentUiState.recipe.description ?: "",
                        onValueChange = { viewModel.onDescriptionChanged(it) },
                        label = { Text("Description") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 80.dp),
                        keyboardOptions = KeyboardOptions.Default.copy(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next)
                    )

                    // --- Image Management Section ---
                    Text("Recipe Photos", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))

                    Button(
                        onClick = {
                            multiplePhotoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.AddAPhoto, contentDescription = "Add Photos Icon", modifier = Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Add/Change Photos (${currentUiState.newImageUris.size} new)")
                    }

                    // Display Existing Images (if editing)
                    if (!currentUiState.isNewRecipe && currentUiState.recipe.imageUrls.isNotEmpty()) {
                        Text("Current Photos:", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(currentUiState.recipe.imageUrls, key = { it }) { imageUrl ->
                                Box(contentAlignment = Alignment.TopEnd) {
                                    AsyncImage(
                                        model = imageUrl,
                                        contentDescription = "Existing recipe image",
                                        modifier = Modifier
                                            .height(100.dp)
                                            .width(100.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentScale = ContentScale.Crop
                                    )
                                    val isMarkedForDeletion = currentUiState.existingImageUrlsMarkedForDeletion.contains(imageUrl)
                                    IconButton(
                                        onClick = { viewModel.onToggleDeleteExistingImage(imageUrl) },
                                        modifier = Modifier
                                            .padding(4.dp)
                                            .background(
                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                                CircleShape
                                            )
                                            .size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isMarkedForDeletion) Icons.Filled.DeleteForever else Icons.Outlined.Delete,
                                            contentDescription = if (isMarkedForDeletion) "Unmark for deletion" else "Mark to delete existing image",
                                            tint = if (isMarkedForDeletion) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Display Selected Images
                    if (currentUiState.newImageUris.isNotEmpty()) {
                        Text(
                            if (!currentUiState.isNewRecipe && currentUiState.recipe.imageUrls.isNotEmpty()) "New Photos to Add:"
                            else if (!currentUiState.isNewRecipe) "Photos to Add:"
                            else "Selected Photos:",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(currentUiState.newImageUris, key = { it.toString() }) { uri ->
                                Box(contentAlignment = Alignment.TopEnd) {
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = "New recipe image preview",
                                        modifier = Modifier
                                            .height(100.dp)
                                            .width(100.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentScale = ContentScale.Crop
                                    )
                                    IconButton(
                                        onClick = { viewModel.onRemoveNewImage(uri) },
                                        modifier = Modifier
                                            .padding(4.dp)
                                            .background(
                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                                CircleShape
                                            )
                                            .size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Clear,
                                            contentDescription = "Remove new image",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Helper text if no images are present at all
                    if (currentUiState.recipe.imageUrls.isEmpty() && currentUiState.newImageUris.isEmpty()) {
                        Text(
                            text = if (!currentUiState.isNewRecipe) "No photos added yet. Click 'Add/Change Photos' to include some."
                            else "No photos selected yet. Click 'Add/Change Photos' to include some.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .fillMaxWidth()
                        )
                    }
                    // --- End Image Management Section ---

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = currentUiState.recipe.category ?: "",
                            onValueChange = { viewModel.onCategoryChanged(it) },
                            label = { Text("Category") },
                            modifier = Modifier.weight(1f), singleLine = true,
                            keyboardOptions = KeyboardOptions.Default.copy(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next)
                        )
                        OutlinedTextField(
                            value = currentUiState.recipe.cuisine ?: "",
                            onValueChange = { viewModel.onCuisineChanged(it) },
                            label = { Text("Cuisine") },
                            modifier = Modifier.weight(1f), singleLine = true,
                            keyboardOptions = KeyboardOptions.Default.copy(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next)
                        )
                    }
                    // --- Servings Field ---
                    BorderedNumberInputWithStepper(
                        value = currentUiState.servingsInput,
                        onValueChange = { viewModel.onServingsChanged(it) },
                        label = "Servings (e.g., 4)",
                        onIncrement = { viewModel.incrementServings() },
                        onDecrement = { viewModel.decrementServings() },
                        isDecrementEnabled = (currentUiState.servingsInput.toIntOrNull() ?: 1) > 1,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // --- Prep, Cook, Total Time ---
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        BorderedNumberInputWithStepper(
                            value = currentUiState.prepTimeInput,
                            onValueChange = { viewModel.onPrepTimeChanged(it) },
                            label = "Prep Time (min)",
                            onIncrement = { viewModel.incrementPrepTime() },
                            onDecrement = { viewModel.decrementPrepTime() },
                            isDecrementEnabled = (currentUiState.prepTimeInput.toIntOrNull() ?: 1) > 1 || currentUiState.prepTimeInput.isEmpty(),
                            modifier = Modifier.weight(1f)
                        )
                        BorderedNumberInputWithStepper(
                            value = currentUiState.cookTimeInput,
                            onValueChange = { viewModel.onCookTimeChanged(it) },
                            label = "Cook Time (min)",
                            onIncrement = { viewModel.incrementCookTime() },
                            onDecrement = { viewModel.decrementCookTime() },
                            isDecrementEnabled = (currentUiState.cookTimeInput.toIntOrNull() ?: 1) > 1 || currentUiState.cookTimeInput.isEmpty(),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    BorderedNumberInputWithStepper(
                        value = currentUiState.totalTimeInput,
                        onValueChange = { viewModel.onTotalTimeChanged(it) },
                        label = "Total Time (min)",
                        onIncrement = { viewModel.incrementTotalTime() },
                        onDecrement = { viewModel.decrementTotalTime() },
                        isDecrementEnabled = (currentUiState.totalTimeInput.toIntOrNull() ?: 1) > 1 || currentUiState.totalTimeInput.isEmpty(),
                        modifier = Modifier.padding(bottom = 16.dp) // Add consistent bottom padding
                    )

                    // --- Ingredients Text Field ---
                    OutlinedTextField(
                        value = currentUiState.ingredientsInput,
                        onValueChange = { viewModel.onIngredientsChanged(it) },
                        label = { Text("Ingredients (one per line)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp),
                        keyboardOptions = KeyboardOptions.Default.copy(capitalization = KeyboardCapitalization.Sentences),
                        singleLine = false
                    )

                    // --- Instructions Text Field ---
                    OutlinedTextField(
                        value = currentUiState.instructionsInput,
                        onValueChange = { viewModel.onInstructionsChanged(it) },
                        label = { Text("Instructions (one step per line)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        keyboardOptions = KeyboardOptions.Default.copy(capitalization = KeyboardCapitalization.Sentences),
                        singleLine = false
                    )

                    OutlinedTextField(
                        value = currentUiState.tagsInput,
                        onValueChange = { viewModel.onTagsInputChanged(it) },
                        label = { Text("Tags (comma or semicolon separated)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next)
                    )

                    OutlinedTextField(
                        value = currentUiState.recipe.notes ?: "",
                        onValueChange = { viewModel.onNotesChanged(it) },
                        label = { Text("Notes") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 80.dp),
                        keyboardOptions = KeyboardOptions.Default.copy(capitalization = KeyboardCapitalization.Sentences)
                    )

                    if (currentUiState.isLoading) {
                        Box(Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    Spacer(modifier = Modifier.height(64.dp))
                }
            }
        }
    }
}