package com.luutran.mycookingapp.ui.dishmemories

import android.net.Uri
import android.util.Log
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luutran.mycookingapp.data.model.DishMemory
import com.luutran.mycookingapp.data.repository.CookedDishRepository // << CORRECT REPOSITORY
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UserRecipeMemoryUiState(
    val userRecipeId: String = "",
    val memoryId: String? = null, // ID of the memory if editing, null if creating
    val rating: Int = 0,
    val notes: String = "",
    val existingImageUrls: List<String> = emptyList(), // URLs loaded from an existing memory
    val localImageUrisToAdd: List<Uri> = emptyList(), // New images selected by the user
    val existingImageUrlsToDelete: List<String> = emptyList(), // Existing images marked for deletion
    val isLoading: Boolean = false, // For loading existing memory
    val isSaving: Boolean = false, // When save operation is in progress
    val error: String? = null, // For any error messages (load or save)
    val saveSuccess: Boolean = false,
    val initialMemoryLoaded: Boolean = false // True once initial state (new or loaded) is ready
)

class UserRecipeMemoryViewModelFactory(
    private val cookedDishRepository: CookedDishRepository,
    owner: androidx.savedstate.SavedStateRegistryOwner,
    defaultArgs: android.os.Bundle? = null
) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle
    ): T {
        if (modelClass.isAssignableFrom(UserRecipeMemoryViewModel::class.java)) {
            return UserRecipeMemoryViewModel(cookedDishRepository, handle) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

class UserRecipeMemoryViewModel(
    private val cookedDishRepository: CookedDishRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserRecipeMemoryUiState())
    val uiState: StateFlow<UserRecipeMemoryUiState> = _uiState.asStateFlow()

    private val currentRecipeId: String = savedStateHandle.get<String>("userRecipeId") ?: ""
    private val currentMemoryIdForEditing: String? = savedStateHandle.get<String>("memoryId")

    init {
        _uiState.update {
            it.copy(
                userRecipeId = this.currentRecipeId,
                memoryId = this.currentMemoryIdForEditing
            )
        }

        if (this.currentRecipeId.isBlank()) {
            _uiState.update { it.copy(error = "User Recipe ID is missing.", initialMemoryLoaded = true, isLoading = false) }
        } else if (!this.currentMemoryIdForEditing.isNullOrBlank()) {
            loadExistingMemory(this.currentRecipeId, this.currentMemoryIdForEditing)
        } else {
            _uiState.update { it.copy(initialMemoryLoaded = true, isLoading = false) }
        }
    }

    private fun loadExistingMemory(recipeId: String, memId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = cookedDishRepository.getMemoryForUserRecipeById(recipeId, memId)
            result.fold(
                onSuccess = { memory ->
                    if (memory != null) {
                        _uiState.update {
                            it.copy(
                                rating = memory.rating,
                                notes = memory.notes,
                                existingImageUrls = memory.imageUrls,
                                localImageUrisToAdd = emptyList(),
                                existingImageUrlsToDelete = emptyList(),
                                isLoading = false,
                                initialMemoryLoaded = true,
                                error = null
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                error = "Memory not found.",
                                isLoading = false,
                                initialMemoryLoaded = true
                            )
                        }
                    }
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            error = "Failed to load memory: ${exception.message}",
                            isLoading = false,
                            initialMemoryLoaded = true
                        )
                    }
                }
            )
        }
    }

    fun onRatingChanged(newRating: Int) {
        if (!_uiState.value.isSaving && _uiState.value.initialMemoryLoaded) {
            _uiState.update { it.copy(rating = newRating.coerceIn(0, 5)) }
        }
    }

    fun onNotesChanged(newNotes: String) {
        if (!_uiState.value.isSaving && _uiState.value.initialMemoryLoaded) {
            _uiState.update { it.copy(notes = newNotes) }
        }
    }

    fun addLocalImageUri(uri: Uri) {
        if (!_uiState.value.isSaving && _uiState.value.initialMemoryLoaded) {
            // Avoid duplicates
            if (!_uiState.value.localImageUrisToAdd.contains(uri)) {
                _uiState.update {
                    it.copy(localImageUrisToAdd = it.localImageUrisToAdd + uri)
                }
            }
        }
    }

    fun removeLocalImageUri(uri: Uri) {
        if (!_uiState.value.isSaving && _uiState.value.initialMemoryLoaded) {
            _uiState.update {
                it.copy(localImageUrisToAdd = it.localImageUrisToAdd - uri)
            }
        }
    }

    fun markExistingImageUrlForDeletion(imageUrl: String) {
        if (!_uiState.value.isSaving && _uiState.value.initialMemoryLoaded) {
            val alreadyMarked = _uiState.value.existingImageUrlsToDelete.contains(imageUrl)
            if (!alreadyMarked) {
                _uiState.update {
                    it.copy(
                        existingImageUrlsToDelete = it.existingImageUrlsToDelete + imageUrl,
                    )
                }
            }
        }
    }

    fun unmarkExistingImageUrlForDeletion(imageUrl: String) {
        if (!_uiState.value.isSaving && _uiState.value.initialMemoryLoaded) {
            val wasMarked = _uiState.value.existingImageUrlsToDelete.contains(imageUrl)
            if (wasMarked) {
                _uiState.update {
                    it.copy(
                        // Remove from deletion list
                        existingImageUrlsToDelete = it.existingImageUrlsToDelete - imageUrl,
                    )
                }
            }
        }
    }

    fun saveMemory() {
        val currentRecipeIdState = _uiState.value.userRecipeId // Renamed for clarity within this function
        if (currentRecipeIdState.isBlank()) {
            _uiState.update { it.copy(error = "User Recipe ID is missing. Cannot save.") }
            return
        }

        // Prevent saving if initial load (for edit) isn't complete or failed
        if (_uiState.value.isLoading || (!_uiState.value.initialMemoryLoaded && !_uiState.value.memoryId.isNullOrBlank())) {
            _uiState.update { it.copy(error = "Cannot save, memory data is not ready.") }
            return
        }

        _uiState.update { it.copy(isSaving = true, error = null, saveSuccess = false) }

        viewModelScope.launch {
            val currentState = _uiState.value // Capture the current state once
            val isUpdating = !currentState.memoryId.isNullOrBlank()

            val operationResult: Result<Unit> = if (isUpdating) {
                // UPDATE existing memory
                val memoryToUpdate = DishMemory(
                    id = currentState.memoryId!!,
                    rating = currentState.rating,
                    notes = currentState.notes,
                    imageUrls = currentState.existingImageUrls.filterNot { existingUrl ->
                        currentState.existingImageUrlsToDelete.contains(existingUrl)
                    }
                )
                Log.d("ViewModelSave", "Updating. Kept existing URLs: ${memoryToUpdate.imageUrls}")
                Log.d("ViewModelSave", "Updating. URLs to delete from storage: ${currentState.existingImageUrlsToDelete}")
                Log.d("ViewModelSave", "Updating. New local URIs to upload: ${currentState.localImageUrisToAdd}")

                cookedDishRepository.updateMemoryForUserRecipe(
                    userRecipeId = currentRecipeIdState,
                    memory = memoryToUpdate, // Contains ID, new data, and image URLs to KEEP
                    newImageUris = currentState.localImageUrisToAdd,
                    existingImageUrlsToDelete = currentState.existingImageUrlsToDelete // Pass the full list of those to delete
                )
            } else {
                // ADD NEW memory
                val memoryDraft = DishMemory(
                    rating = currentState.rating,
                    notes = currentState.notes,
                    imageUrls = emptyList()
                )
                cookedDishRepository.addMemoryToUserRecipe(
                    userRecipeId = currentRecipeIdState,
                    memoryDraft = memoryDraft,
                    imageUris = currentState.localImageUrisToAdd
                ).map { newMemoryId ->
                    Log.d("ViewModelSave", "New memory added with ID: $newMemoryId")
                    Unit // Transform String to Unit for Result<Unit>
                }
            }

            operationResult.fold(
                onSuccess = {
                    Log.d("ViewModelSave", "Save/Update operation successful in repository.")
                    _uiState.update { it.copy(isSaving = false, saveSuccess = true, error = null) }
                },
                onFailure = { exception ->
                    Log.e("ViewModelSave", "Save/Update operation failed: ${exception.message}", exception)
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            saveSuccess = false,
                            error = "Failed to save memory: ${exception.message}"
                        )
                    }
                }
            )
        }
    }

    fun resetSaveStatus() {
        _uiState.update { it.copy(saveSuccess = false, error = null) }
    }
}