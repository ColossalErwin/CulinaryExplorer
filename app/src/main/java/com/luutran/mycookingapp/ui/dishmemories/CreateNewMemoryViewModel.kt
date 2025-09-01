package com.luutran.mycookingapp.ui.dishmemories

import android.net.Uri
import android.util.Log
import androidx.lifecycle.*
import com.luutran.mycookingapp.data.model.DishMemory
import com.luutran.mycookingapp.data.repository.CookedDishRepository
import com.luutran.mycookingapp.data.repository.RecipeRepository // To fetch recipe title/image if needed
import com.luutran.mycookingapp.navigation.NavDestinations
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.collections.filter
import kotlin.collections.remove
import kotlin.collections.toMutableList


data class CreateMemoryUiState(
    val recipeId: Int = -1,
    val memoryId: String? = null, // <-- ADDED: For edit mode
    val recipeTitle: String = "",
    val recipeImageUrl: String? = null,
    val rating: Int = 0,
    val notes: String = "",
    val existingImageUrls: List<String> = emptyList(),
    val selectedImageUris: List<Uri> = emptyList(),
    val imagesToDelete: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val saveSuccess: Boolean = false,
    val isEditMode: Boolean = false
)

class CreateNewMemoryViewModel(
    savedStateHandle: SavedStateHandle,
    private val cookedDishRepository: CookedDishRepository,
    private val recipeRepository: RecipeRepository
) : ViewModel() {

    val recipeId: Int = savedStateHandle.get<Int>(NavDestinations.CREATE_NEW_MEMORY_ARG_RECIPE_ID) ?: -1
    private val memoryId: String? = savedStateHandle.get<String>(NavDestinations.CREATE_NEW_MEMORY_ARG_MEMORY_ID)

    private val _uiState = MutableStateFlow(CreateMemoryUiState(recipeId = recipeId, memoryId = memoryId, isEditMode = memoryId != null))
    val uiState: StateFlow<CreateMemoryUiState> = _uiState.asStateFlow()

    init {
        Log.d("CreateNewMemoryVM", "Init - Recipe ID: $recipeId, Memory ID: $memoryId, Edit Mode: ${memoryId != null}")
        if (recipeId != -1) {
            fetchRecipeDetails(recipeId) // Always fetch recipe title
            if (memoryId != null) {
                loadMemoryForEditing(recipeId, memoryId)
            }
        } else {
            _uiState.update { it.copy(saveError = "Recipe ID not found.") }
            Log.e("CreateNewMemoryVM", "Recipe ID is -1 in init.")
        }
    }

    private fun fetchRecipeDetails(id: Int) {
        viewModelScope.launch {
            try {
                val recipe = recipeRepository.getRecipeById(id)
                if (recipe != null) {
                    _uiState.update {
                        it.copy(
                            recipeTitle = recipe.title ?: "",
                            recipeImageUrl = recipe.image
                        )
                    }
                } else {
                    _uiState.update { it.copy(saveError = (it.saveError ?: "") + " Could not load recipe details.") }
                }
            } catch (e: Exception) {
                Log.e("CreateNewMemoryVM", "Error fetching recipe details", e)
                _uiState.update { it.copy(saveError = (it.saveError ?: "") + " Error loading recipe details: ${e.message}") }
            }
        }
    }

    private fun loadMemoryForEditing(currentRecipeId: Int, currentMemoryId: String) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = cookedDishRepository.getDishMemoryById(currentRecipeId, currentMemoryId)
            result
                .onSuccess { memory ->
                    if (memory != null) {
                        _uiState.update {
                            it.copy(
                                rating = memory.rating,
                                notes = memory.notes,
                                existingImageUrls = memory.imageUrls,
                                selectedImageUris = emptyList(), // Reset any newly selected ones from previous state
                                imagesToDelete = emptyList(), // Reset
                                isLoading = false
                            )
                        }
                        Log.d("CreateNewMemoryVM", "Loaded memory for editing: $memory")
                    } else {
                        _uiState.update { it.copy(isLoading = false, saveError = "Failed to load memory for editing.") }
                        Log.e("CreateNewMemoryVM", "Memory $currentMemoryId not found for recipe $currentRecipeId")
                    }
                }
                .onFailure { exception ->
                    _uiState.update { it.copy(isLoading = false, saveError = "Error loading memory: ${exception.message}") }
                    Log.e("CreateNewMemoryVM", "Error loading memory $currentMemoryId", exception)
                }
        }
    }


    fun onRatingChange(newRating: Int) {
        _uiState.update { it.copy(rating = newRating) }
    }

    fun onNotesChange(newNotes: String) {
        _uiState.update { it.copy(notes = newNotes) }
    }

    fun onImagesSelected(uris: List<Uri>) {
        _uiState.update { it.copy(selectedImageUris = it.selectedImageUris + uris) }
    }

    fun onRemoveNewImage(uriToRemove: Uri) {
        _uiState.update { currentState ->
            currentState.copy(selectedImageUris = currentState.selectedImageUris.filter { it != uriToRemove })
        }
    }

    fun onToggleExistingImageForDeletion(imageUrl: String) {
        _uiState.update { currentState ->
            val currentToDelete = currentState.imagesToDelete.toMutableList()
            if (currentToDelete.contains(imageUrl)) {
                currentToDelete.remove(imageUrl)
            } else {
                currentToDelete.add(imageUrl)
            }
            currentState.copy(imagesToDelete = currentToDelete)
        }
    }


    fun saveMemory() {
        if (recipeId == -1) {
            _uiState.update { it.copy(saveError = "Cannot save: Recipe ID is missing.") }
            return
        }
        val currentState = _uiState.value
        if (currentState.isSaving || currentState.isLoading) return

        _uiState.update { it.copy(isSaving = true, saveError = null) }

        val memoryDraft = DishMemory(
            id = currentState.memoryId ?: "",
            rating = currentState.rating,
            notes = currentState.notes,
            imageUrls = if (currentState.isEditMode) {
                currentState.existingImageUrls.filter { it !in currentState.imagesToDelete }
            } else {
                emptyList()
            }
        )

        viewModelScope.launch {
            val result: Result<Any> = if (currentState.isEditMode && currentState.memoryId != null) {
                // UPDATE EXISTING MEMORY
                Log.d("CreateNewMemoryVM", "Attempting to update memory ID: ${currentState.memoryId}")
                Log.d("CreateNewMemoryVM", "New images to upload: ${currentState.selectedImageUris.size}")
                Log.d("CreateNewMemoryVM", "Existing images to delete: ${currentState.imagesToDelete.size}")

                cookedDishRepository.updateDishMemory(
                    recipeId = recipeId,
                    memory = memoryDraft.copy(id = currentState.memoryId), // Ensure ID is correct
                    newImageUris = currentState.selectedImageUris,
                    existingImageUrlsToDelete = currentState.imagesToDelete
                )

            } else {
                // ADD NEW MEMORY
                Log.d("CreateNewMemoryVM", "Attempting to add new memory for recipe ID: $recipeId")
                cookedDishRepository.addDishMemory(
                    recipeId = recipeId,
                    recipeTitle = currentState.recipeTitle,
                    recipeImageUrl = currentState.recipeImageUrl,
                    memoryDraft = memoryDraft, // ID will be generated by repo
                    imageUris = currentState.selectedImageUris
                )
            }

            result
                .onSuccess {
                    val successMessage = if (currentState.isEditMode) "Memory updated" else "Memory saved"
                    Log.d("CreateNewMemoryVM", "$successMessage successfully. Result: $it")
                    _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
                }
                .onFailure { exception ->
                    val action = if (currentState.isEditMode) "update" else "save"
                    Log.e("CreateNewMemoryVM", "Failed to $action memory", exception)
                    _uiState.update { it.copy(isSaving = false, saveError = "${action.replaceFirstChar { it.uppercase() }} failed: ${exception.message}") }
                }
        }
    }

    fun resetSaveStatus() {
        _uiState.update { it.copy(saveError = null, saveSuccess = false) }
    }
}

class CreateNewMemoryViewModelFactory(
    private val cookedDishRepository: CookedDishRepository,
    private val recipeRepository: RecipeRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: androidx.lifecycle.viewmodel.CreationExtras): T {
        if (modelClass.isAssignableFrom(CreateNewMemoryViewModel::class.java)) {
            val savedStateHandle = extras.createSavedStateHandle()
            @Suppress("UNCHECKED_CAST")
            return CreateNewMemoryViewModel(savedStateHandle, cookedDishRepository, recipeRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}