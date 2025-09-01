package com.luutran.mycookingapp.ui.userrecipe

import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.luutran.mycookingapp.data.model.UserRecipe
import com.luutran.mycookingapp.data.repository.RepositoryResult
import com.luutran.mycookingapp.data.repository.UserRecipeRepository
import com.luutran.mycookingapp.navigation.NavDestinations
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.collections.remove
import kotlin.collections.toMutableList
import kotlin.text.isNotBlank

sealed interface CreateUserRecipeUiState {
    data object Idle : CreateUserRecipeUiState
    data object LoadingRecipe : CreateUserRecipeUiState
    data class EditForm(
        val recipe: UserRecipe,
        val isNewRecipe: Boolean,
        val newImageUris: List<Uri> = emptyList(),
        val existingImageUrlsMarkedForDeletion: List<String> = emptyList(), // For edit mode
        val ingredientsInput: String = recipe.ingredients.joinToString("\n"),
        val instructionsInput: String = recipe.instructions.joinToString("\n"),
        val tagsInput: String = recipe.tags.joinToString(", "),
        val servingsInput: String = recipe.servings ?: "",
        val prepTimeInput: String = recipe.prepTimeMinutes?.toString() ?: "",
        val cookTimeInput: String = recipe.cookTimeMinutes?.toString() ?: "",
        val totalTimeInput: String = recipe.totalTimeMinutes?.toString() ?: "",

        val isLoading: Boolean = false,
        val error: String? = null,
        val saveSuccess: Boolean = false
    ) : CreateUserRecipeUiState
    data class Error(val message: String) : CreateUserRecipeUiState
}

class CreateUserRecipeViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val userRecipeRepository: UserRecipeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CreateUserRecipeUiState>(CreateUserRecipeUiState.Idle)
    val uiState: StateFlow<CreateUserRecipeUiState> = _uiState.asStateFlow()

    private val passedRecipeId: String? = savedStateHandle[NavDestinations.USER_RECIPE_ARG_ID] // Adjusted for example
    private val firebaseAuth = FirebaseAuth.getInstance()

    init {
        Log.d("CreateUserRecipeVM", "SavedStateHandle keys: ${savedStateHandle.keys().joinToString()}")
        val logId : String? = savedStateHandle[NavDestinations.USER_RECIPE_ARG_ID]
        Log.d("CreateUserRecipeVM", "Attempting to retrieve recipeId with key '${NavDestinations.USER_RECIPE_ARG_ID}'. Value: $logId")
        initializeScreen()
    }

    private fun initializeScreen() {
        viewModelScope.launch {
            val currentUserId = firebaseAuth.currentUser?.uid
            if (currentUserId == null) {
                _uiState.value = CreateUserRecipeUiState.Error("User not authenticated. Cannot load or create recipe.")
                return@launch
            }

            if (passedRecipeId != null) {
                _uiState.value = CreateUserRecipeUiState.LoadingRecipe
                when (val result = userRecipeRepository.getUserRecipe(passedRecipeId)) {
                    is RepositoryResult.Success -> {
                        _uiState.value = CreateUserRecipeUiState.EditForm(
                            recipe = result.data,
                            isNewRecipe = false,
                            tagsInput = result.data.tags.joinToString(", "),
                            servingsInput = result.data.servings ?: "",
                            prepTimeInput = result.data.prepTimeMinutes?.toString() ?: "",
                            cookTimeInput = result.data.cookTimeMinutes?.toString() ?: "",
                            totalTimeInput = result.data.totalTimeMinutes?.toString() ?: ""
                        )
                    }
                    is RepositoryResult.Error -> {
                        _uiState.value = CreateUserRecipeUiState.Error("Failed to load recipe: ${result.exception.message}")
                    }
                }
            } else {
                val newRecipeId = userRecipeRepository.getNewRecipeId(currentUserId)
                _uiState.value = CreateUserRecipeUiState.EditForm(
                    recipe = UserRecipe(id = newRecipeId), // Pre-fill ID
                    isNewRecipe = true,
                    tagsInput = "",
                    servingsInput = "",
                    prepTimeInput = "",
                    cookTimeInput = "",
                    totalTimeInput = ""
                )
            }
        }
    }

    fun onTitleChanged(newTitle: String) { updateRecipeInState { it.copy(title = newTitle) } }
    fun onDescriptionChanged(newDescription: String) { updateRecipeInState { it.copy(description = newDescription.takeIf { desc -> desc.isNotBlank() }) } } // Keep takeIfBlank for optional fields
    fun onCategoryChanged(newCategory: String) { updateRecipeInState { it.copy(category = newCategory.takeIf { cat -> cat.isNotBlank() }) } }
    fun onCuisineChanged(newCuisine: String) { updateRecipeInState { it.copy(cuisine = newCuisine.takeIf { cui -> cui.isNotBlank() }) } }

    fun onServingsChanged(newServingsText: String) {
        val currentState = _uiState.value
        if (currentState is CreateUserRecipeUiState.EditForm) {
            val filteredInput = newServingsText.filter { it.isDigit() }

            _uiState.value = currentState.copy(
                servingsInput = filteredInput,
                error = null,
                saveSuccess = false
            )
        }
    }

    fun incrementServings() {
        val currentState = _uiState.value
        if (currentState is CreateUserRecipeUiState.EditForm) {
            val currentServings = currentState.servingsInput.toIntOrNull() ?: 0
            _uiState.value = currentState.copy(servingsInput = (currentServings + 1).toString())
        }
    }

    fun decrementServings() {
        val currentState = _uiState.value
        if (currentState is CreateUserRecipeUiState.EditForm) {
            val currentServings = currentState.servingsInput.toIntOrNull() ?: 1
            if (currentServings > 1) {
                _uiState.value = currentState.copy(servingsInput = (currentServings - 1).toString())
            }
        }
    }

    // --- Prep Time ---
    fun onPrepTimeChanged(newPrepTimeText: String) {
        val currentState = _uiState.value
        if (currentState is CreateUserRecipeUiState.EditForm) {
            val filteredInput = newPrepTimeText.filter { it.isDigit() }
            _uiState.value = currentState.copy(
                prepTimeInput = filteredInput,
                error = null, saveSuccess = false
            )
        }
    }

    fun incrementPrepTime() {
        val currentState = _uiState.value
        if (currentState is CreateUserRecipeUiState.EditForm) {
            val currentTime = currentState.prepTimeInput.toIntOrNull() ?: 0
            _uiState.value = currentState.copy(prepTimeInput = (currentTime + 1).toString())
        }
    }

    fun decrementPrepTime() {
        val currentState = _uiState.value
        if (currentState is CreateUserRecipeUiState.EditForm) {
            val currentTime = currentState.prepTimeInput.toIntOrNull() ?: 1
            if (currentTime > 1) {
                _uiState.value = currentState.copy(prepTimeInput = (currentTime - 1).toString())
            } else if (currentTime == 1) {
                _uiState.value = currentState.copy(prepTimeInput = "")
            }
        }
    }

    // --- Cook Time (similar to Prep Time) ---
    fun onCookTimeChanged(newCookTimeText: String) {
        val currentState = _uiState.value
        if (currentState is CreateUserRecipeUiState.EditForm) {
            val filteredInput = newCookTimeText.filter { it.isDigit() }
            _uiState.value = currentState.copy(
                cookTimeInput = filteredInput,
                error = null, saveSuccess = false
            )
        }
    }
    fun incrementCookTime() {
        val currentState = _uiState.value
        if (currentState is CreateUserRecipeUiState.EditForm) {
            val currentTime = currentState.cookTimeInput.toIntOrNull() ?: 0
            _uiState.value = currentState.copy(cookTimeInput = (currentTime + 1).toString())
        }
    }
    fun decrementCookTime() {
        val currentState = _uiState.value
        if (currentState is CreateUserRecipeUiState.EditForm) {
            val currentTime = currentState.cookTimeInput.toIntOrNull() ?: 1
            if (currentTime > 1) {
                _uiState.value = currentState.copy(cookTimeInput = (currentTime - 1).toString())
            } else if (currentTime == 1) {
                _uiState.value = currentState.copy(cookTimeInput = "")
            }
        }
    }


    // --- Total Time (similar to Prep Time) ---
    fun onTotalTimeChanged(newTotalTimeText: String) {
        val currentState = _uiState.value
        if (currentState is CreateUserRecipeUiState.EditForm) {
            val filteredInput = newTotalTimeText.filter { it.isDigit() }
            _uiState.value = currentState.copy(
                totalTimeInput = filteredInput,
                error = null, saveSuccess = false
            )
        }
    }
    fun incrementTotalTime() {
        val currentState = _uiState.value
        if (currentState is CreateUserRecipeUiState.EditForm) {
            val currentTime = currentState.totalTimeInput.toIntOrNull() ?: 0
            _uiState.value = currentState.copy(totalTimeInput = (currentTime + 1).toString())
        }
    }
    fun decrementTotalTime() {
        val currentState = _uiState.value
        if (currentState is CreateUserRecipeUiState.EditForm) {
            val currentTime = currentState.totalTimeInput.toIntOrNull() ?: 1
            if (currentTime > 1) {
                _uiState.value = currentState.copy(totalTimeInput = (currentTime - 1).toString())
            } else if (currentTime == 1) {
                _uiState.value = currentState.copy(totalTimeInput = "")
            }
        }
    }


    // MODIFIED: Let the text field directly update these raw strings
    fun onIngredientsChanged(text: String) {
        val currentState = _uiState.value
        if (currentState is CreateUserRecipeUiState.EditForm) {
            _uiState.value = currentState.copy(
                ingredientsInput = text,
                error = null,
                saveSuccess = false
            )
        }
    }

    fun onInstructionsChanged(text: String) {
        val currentState = _uiState.value
        if (currentState is CreateUserRecipeUiState.EditForm) {
            _uiState.value = currentState.copy(
                instructionsInput = text,
                error = null,
                saveSuccess = false
            )
        }
    }

    fun onTagsInputChanged(newTagsText: String) {
        val currentState = _uiState.value
        if (currentState is CreateUserRecipeUiState.EditForm) {
            _uiState.value = currentState.copy(
                tagsInput = newTagsText,
                error = null,
                saveSuccess = false
            )
        }
    }

    fun onNotesChanged(newNotes: String) { updateRecipeInState { it.copy(notes = newNotes.takeIf { notes -> notes.isNotBlank() }) } }

    // --- Image Handling ---
    fun onNewImageSelected(uri: Uri) {
        val currentState = _uiState.value
        if (currentState is CreateUserRecipeUiState.EditForm) {
            _uiState.value = currentState.copy(
                newImageUris = currentState.newImageUris + uri,
                error = null,
                saveSuccess = false
            )
        }
    }

    fun onNewImagesSelected(uris: List<Uri>) {
        val currentState = _uiState.value
        if (currentState is CreateUserRecipeUiState.EditForm) {
            if (uris.isNotEmpty()) {
                // Add new URIs to the existing list, ensuring no duplicates are added
                val combinedUris = (currentState.newImageUris + uris).distinct()

                // Enforce a limit on the total number of new images.
                val maxNewImages = 5
                val finalUris = if (combinedUris.size > maxNewImages) {
                    combinedUris.take(maxNewImages)
                } else {
                    combinedUris
                }

                _uiState.value = currentState.copy(
                    newImageUris = finalUris,
                    error = null, // Clear any previous error
                    saveSuccess = false
                )
                Log.d("ViewModel", "New images updated. Total new: ${finalUris.size}")
            }
        }
    }

    /**
     * Removes a new image (identified by its URI) from the list of images
     * selected by the user but not yet uploaded.
     */
    fun onRemoveNewImage(uriToRemove: Uri) {
        val currentState = _uiState.value
        if (currentState is CreateUserRecipeUiState.EditForm) {
            _uiState.value = currentState.copy(
                newImageUris = currentState.newImageUris.filter { it != uriToRemove },
                error = null,
                saveSuccess = false
            )
            Log.d("ViewModel", "Removed new image: $uriToRemove. Remaining new: ${(_uiState.value as? CreateUserRecipeUiState.EditForm)?.newImageUris?.size}")
        }
    }

    /**
     * Toggles an existing image (identified by its URL) for deletion.
     * If it's already marked for deletion, it's unmarked, and vice-versa.
     * This is used when editing a recipe that already has images.
     */
    fun onToggleDeleteExistingImage(imageUrl: String) {
        val currentState = _uiState.value
        if (currentState is CreateUserRecipeUiState.EditForm) {
            val currentDeletions = currentState.existingImageUrlsMarkedForDeletion.toMutableList()
            if (currentDeletions.contains(imageUrl)) {
                currentDeletions.remove(imageUrl)
            } else {
                currentDeletions.add(imageUrl)
            }
            _uiState.value = currentState.copy(
                existingImageUrlsMarkedForDeletion = currentDeletions, // Use the correct field name here
                error = null,
                saveSuccess = false
            )
            Log.d("ViewModel", "Toggled deletion for existing image: $imageUrl. Marked for deletion: ${currentDeletions.size}")
        }
    }

    // --- Save Action ---
    fun saveRecipe() {
        val currentState = _uiState.value
        if (currentState !is CreateUserRecipeUiState.EditForm) return

        val parsedTags = currentState.tagsInput.split(",", ";")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        // --- Servings Validation and Conversion ---
        val servingsInput = currentState.servingsInput
        val servingsInt = servingsInput.toIntOrNull()
        val finalServings: String?

        if (servingsInput.isEmpty()) {
            finalServings = null
        } else if (servingsInt != null && servingsInt > 0) {
            finalServings = servingsInput
        } else {
            _uiState.value = currentState.copy(error = "Servings must be a positive number greater than 0, or empty.")
            return // Stop the save process
        }
        // --- End Servings Validation ---

        val parsedIngredients = currentState.ingredientsInput.split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val parsedInstructions = currentState.instructionsInput.split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        // --- Time Fields Validation and Conversion ---
        val prepTimeMinutes = currentState.prepTimeInput.toIntOrNull()?.takeIf { it > 0 }
        val cookTimeMinutes = currentState.cookTimeInput.toIntOrNull()?.takeIf { it > 0 }
        val totalTimeMinutes = currentState.totalTimeInput.toIntOrNull()?.takeIf { it > 0 }

        // specific error messages if time fields are invalid but not empty
        if (currentState.prepTimeInput.isNotEmpty() && prepTimeMinutes == null) {
            _uiState.value = currentState.copy(error = "Prep time must be a positive number or empty.")
            return
        }
        if (currentState.cookTimeInput.isNotEmpty() && cookTimeMinutes == null) {
            _uiState.value = currentState.copy(error = "Cook time must be a positive number or empty.")
            return
        }
        if (currentState.totalTimeInput.isNotEmpty() && totalTimeMinutes == null) {
            _uiState.value = currentState.copy(error = "Total time must be a positive number or empty.")
            return
        }
        // ---

        val recipeToSave = currentState.recipe.copy(
            tags = parsedTags,
            servings = finalServings,
            instructions = parsedInstructions,
            ingredients = parsedIngredients,
            prepTimeMinutes = prepTimeMinutes,
            cookTimeMinutes = cookTimeMinutes,
            totalTimeMinutes = totalTimeMinutes
        )

        if (recipeToSave.title.isBlank()) {
            _uiState.value = currentState.copy(error = "Title cannot be empty.")
            return
        }

        _uiState.value = currentState.copy(isLoading = true, error = null, saveSuccess = false)
        viewModelScope.launch {
            // User authentication is checked by the repository for each operation
            Log.d("CreateVM", "Attempting to save recipe: ${recipeToSave.title}, ID: ${recipeToSave.id}")

            val result = if (currentState.isNewRecipe) {
                userRecipeRepository.addUserRecipe(recipeToSave, currentState.newImageUris)
            } else {
                userRecipeRepository.updateUserRecipe(recipeToSave, currentState.newImageUris, currentState.existingImageUrlsMarkedForDeletion)
            }
            when (result) {
                is RepositoryResult.Success -> {
                    Log.d("CreateVM", "Recipe save successful. ID: ${result.data}")
                    // Clear temporary image states on successful save
                    _uiState.value = currentState.copy(
                        recipe = recipeToSave, // Ensure the state has the final version of the recipe
                        isLoading = false,
                        saveSuccess = true,
                        error = null,
                        newImageUris = emptyList(), // Clear newly added URIs
                        existingImageUrlsMarkedForDeletion = emptyList() // Clear deletion marks
                    )
                }
                is RepositoryResult.Error -> {
                    Log.e("CreateVM", "Failed to save recipe", result.exception)
                    _uiState.value = currentState.copy(
                        isLoading = false,
                        error = "Save failed: ${result.exception.message}",
                        saveSuccess = false
                    )
                }
            }
        }
    }

    fun resetSaveStatusAndForm() {
        val currentState = _uiState.value
        if (currentState is CreateUserRecipeUiState.EditForm) {
            if (currentState.isNewRecipe && currentState.saveSuccess) {
                // For a new recipe that was successfully saved, reset to a completely new form
                val currentUserId = firebaseAuth.currentUser?.uid
                if (currentUserId != null) {
                    val newRecipeId = userRecipeRepository.getNewRecipeId(currentUserId)
                    _uiState.value = CreateUserRecipeUiState.EditForm(
                        recipe = UserRecipe(id = newRecipeId),
                        isNewRecipe = true
                    )
                } else {
                    _uiState.value = CreateUserRecipeUiState.Error("User session lost. Cannot create new recipe form.")
                }
            } else {
                _uiState.value = currentState.copy(
                    saveSuccess = false,
                    error = null,
                    isLoading = false
                )
            }
        }
    }

    /**
     * Called by the Screen after saveSuccess is true and navigation has occurred,
     * or if the user navigates away before saving.
     * For new recipes, it prepares the form for another new entry.
     * For edited recipes, it simply clears flags.
     */
    fun operationCompletedOrCancelled() {
        val currentState = _uiState.value
        if (currentState is CreateUserRecipeUiState.EditForm) {
            if (currentState.isNewRecipe && currentState.saveSuccess) {
                // If a NEW recipe was successfully saved and we are done with it (navigated away),
                // then reset to a completely new form for potential next creation.
                val currentUserId = firebaseAuth.currentUser?.uid
                if (currentUserId != null) {
                    val newRecipeId = userRecipeRepository.getNewRecipeId(currentUserId)
                    _uiState.value = CreateUserRecipeUiState.EditForm(
                        recipe = UserRecipe(id = newRecipeId),
                        isNewRecipe = true
                        // All other fields like isLoading, error, saveSuccess are default false/null
                    )
                } else {
                    _uiState.value = CreateUserRecipeUiState.Error("User session lost. Cannot prepare new recipe form.")
                }
            } else {
                _uiState.value = currentState.copy(
                    saveSuccess = false,
                    error = null,
                    isLoading = false
                )
            }
        }
    }

    private fun updateRecipeInState(update: (UserRecipe) -> UserRecipe) {
        val currentState = _uiState.value
        if (currentState is CreateUserRecipeUiState.EditForm) {
            _uiState.value = currentState.copy(
                recipe = update(currentState.recipe),
                error = null, // Clear error on edit
                saveSuccess = false // Clear success on edit
            )
        }
    }
}

class CreateUserRecipeViewModelFactory(
    private val savedStateHandle: SavedStateHandle,
    private val userRecipeRepository: UserRecipeRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreateUserRecipeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CreateUserRecipeViewModel(savedStateHandle, userRecipeRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}