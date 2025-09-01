package com.luutran.mycookingapp.ui.dishmemories

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.luutran.mycookingapp.data.model.CookedDishEntry
import com.luutran.mycookingapp.data.repository.CookedDishRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update


sealed interface MyCookedDishesUiState {
    data object Loading : MyCookedDishesUiState
    data class Success(val displayedDishes: List<CookedDishEntry>) : MyCookedDishesUiState
    data class Error(val message: String) : MyCookedDishesUiState
}

class MyCookedDishesViewModel(
    private val cookedDishRepository: CookedDishRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MyCookedDishesUiState>(MyCookedDishesUiState.Loading)
    val uiState: StateFlow<MyCookedDishesUiState> = _uiState.asStateFlow()

    // Holds the raw list from Firestore
    private val _rawDishes = MutableStateFlow<List<CookedDishEntry>>(emptyList())

    // Holds the current sort option
    private val _sortOption = MutableStateFlow(CookedDishSortOption.defaultSort())
    val sortOption: StateFlow<CookedDishSortOption> = _sortOption.asStateFlow()

    // --- Selection Mode State ---
    private val _isSelectionModeActive = MutableStateFlow(false)
    val isSelectionModeActive: StateFlow<Boolean> = _isSelectionModeActive.asStateFlow()

    private val _selectedDishIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedDishIds: StateFlow<Set<String>> = _selectedDishIds.asStateFlow()

    private val _showDeleteConfirmationDialog = MutableStateFlow(false)
    val showDeleteConfirmationDialog: StateFlow<Boolean> = _showDeleteConfirmationDialog.asStateFlow()

    // --- State for Sort/Filter Bottom Sheet ---
    private val _showSortFilterSheet = MutableStateFlow(false)
    val showSortFilterSheet: StateFlow<Boolean> = _showSortFilterSheet.asStateFlow()


    init {
        // Combine rawDishes and sortOption to produce the uiState
        viewModelScope.launch {
            combine(_rawDishes, _sortOption) { dishes, sort ->
                dishes.applySort(sort)
            }.catch { e ->
                Log.e("ViewModel", "Error in combine operator: ${e.message}", e)
                _uiState.value = MyCookedDishesUiState.Error("Error processing dishes: ${e.message}")
            }.collect { sortedDishes ->
                if (_uiState.value !is MyCookedDishesUiState.Error || _rawDishes.value.isNotEmpty()) {
                    if (_rawDishes.value.isEmpty() && _uiState.value is MyCookedDishesUiState.Loading) {
                        // Wait for fetchMyCookedDishes to complete.
                    } else {
                        _uiState.value = MyCookedDishesUiState.Success(sortedDishes)
                    }
                }
            }
        }
        fetchMyCookedDishes()
    }

    fun fetchMyCookedDishes() {
        viewModelScope.launch {
            Log.d("ViewModel", "fetchMyCookedDishes called")
            if (_isSelectionModeActive.value) {
                exitSelectionMode()
            }
            _uiState.value = MyCookedDishesUiState.Loading
            cookedDishRepository.getAllCookedDishes()
                .onSuccess { dishes ->
                    _rawDishes.value = dishes // Update raw dishes
                    // The `combine` operator will automatically trigger re-sorting and update uiState
                    if (dishes.isEmpty()) { // Handle empty case explicitly after fetch
                        _uiState.value = MyCookedDishesUiState.Success(emptyList())
                    }
                    Log.d("ViewModel", "fetchMyCookedDishes success, raw count: ${dishes.size}")
                }
                .onFailure { exception ->
                    _rawDishes.value = emptyList() // Clear raw dishes on error
                    _uiState.value = MyCookedDishesUiState.Error(exception.message ?: "Unknown error")
                    Log.e("ViewModel", "fetchMyCookedDishes error", exception)
                }
        }
    }

    fun onSortOptionSelected(newSortOption: CookedDishSortOption) {
        _sortOption.value = newSortOption
    }

    fun openSortFilterSheet() {
        _showSortFilterSheet.value = true
    }

    fun closeSortFilterSheet() {
        _showSortFilterSheet.value = false
    }


    // Selection and Deletion Logic
    fun enterSelectionMode() {
        _isSelectionModeActive.value = true
    }

    fun exitSelectionMode() {
        _isSelectionModeActive.value = false
        _selectedDishIds.value = emptySet()
    }

    fun toggleDishSelection(dishId: String) {
        _selectedDishIds.update { currentSelectedIds ->
            if (currentSelectedIds.contains(dishId)) {
                currentSelectedIds - dishId
            } else {
                currentSelectedIds + dishId
            }
        }
    }

    fun requestDeleteConfirmation() {
        if (_selectedDishIds.value.isNotEmpty()) {
            _showDeleteConfirmationDialog.value = true
        }
    }

    fun cancelDeleteConfirmation() {
        _showDeleteConfirmationDialog.value = false
    }

    fun confirmDeleteSelectedDishes() {
        _showDeleteConfirmationDialog.value = false
        if (_selectedDishIds.value.isEmpty()) {
            exitSelectionMode()
            return
        }
        viewModelScope.launch {
            val idsToDelete = _selectedDishIds.value.toList()
            val result = cookedDishRepository.deleteCookedDishEntriesWithCascade(idsToDelete)

            result.onSuccess {
                Log.d("ViewModel", "Deletion successful for IDs: $idsToDelete. Refreshing list.")
                fetchMyCookedDishes()
            }
            result.onFailure { exception ->
                Log.e("ViewModel", "Error deleting dishes", exception)
                _uiState.value = MyCookedDishesUiState.Error("Failed to delete dishes: ${exception.message}")
                exitSelectionMode()
            }
        }
    }
}
class MyCookedDishesViewModelFactory(
    private val cookedDishRepository: CookedDishRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MyCookedDishesViewModel::class.java)) {
            return MyCookedDishesViewModel(cookedDishRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}