package com.luutran.mycookingapp.ui.mealplanner

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.luutran.mycookingapp.data.model.DailyMealPlan
import com.luutran.mycookingapp.data.model.DailyPlannerNote
import com.luutran.mycookingapp.data.model.PlannedMeal
import com.luutran.mycookingapp.data.model.RecipeSummary // For adding to plan
import com.luutran.mycookingapp.data.repository.RecipeRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

sealed interface MealPlannerUiState {
    object Loading : MealPlannerUiState
    data class Success(
        val plannedMealsByDay: Map<Long, List<PlannedMeal>>, // Key is EpochDay
        val currentMonth: YearMonth,
        val selectedDate: LocalDate,
        val dailyNote: DailyPlannerNote? // Add daily note to UI state
    ) : MealPlannerUiState
    data class Error(val message: String) : MealPlannerUiState
}

class MealPlannerViewModel(private val recipeRepository: RecipeRepository) : ViewModel() {

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _monthlyPlannedMeals = MutableStateFlow<Map<Long, List<PlannedMeal>>>(emptyMap())

    // State for the daily note
    private val _currentDailyNote = MutableStateFlow<DailyPlannerNote?>(
        DailyPlannerNote(id = _selectedDate.value.toEpochDay().toString(), date = _selectedDate.value.toEpochDay())
    )
    val currentDailyNote: StateFlow<DailyPlannerNote?> = _currentDailyNote.asStateFlow()
    private var dailyNoteJob: Job? = null

    val mealPlannerUiState: StateFlow<MealPlannerUiState> = combine(
        _currentMonth, _selectedDate, _monthlyPlannedMeals, _currentDailyNote
    ) { month, selected, mealsMap, note ->
        MealPlannerUiState.Success(mealsMap, month, selected, note)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MealPlannerUiState.Loading
    )

    init {
        viewModelScope.launch {
            _currentMonth.collectLatest { month ->
                loadPlannedMealsForMonth(month)
            }
        }
        fetchNotesForSelectedDate(_selectedDate.value)

        viewModelScope.launch {
            _selectedDate.collectLatest { date ->
                fetchNotesForSelectedDate(date)
            }
        }
    }

    private fun loadPlannedMealsForMonth(yearMonth: YearMonth) {
        viewModelScope.launch {
            val startDate = yearMonth.atDay(1).toEpochDay()
            val endDate = yearMonth.atEndOfMonth().toEpochDay()

            recipeRepository.getPlannedMealsForDateRange(startDate, endDate)
                .catch { e ->
                    Log.e("MealPlannerVM", "Error loading planned meals: ${e.message}")
                }
                .collect { meals ->
                    _monthlyPlannedMeals.value = meals.groupBy { it.date }
                }
        }
    }

    private fun fetchNotesForSelectedDate(date: LocalDate) {
        dailyNoteJob?.cancel() // Cancel previous job if any
        dailyNoteJob = viewModelScope.launch {
            recipeRepository.getDailyNoteForDate(date.toEpochDay())
                .catch { e ->
                    Log.e("MealPlannerVM", "Error loading daily note: ${e.message}")
                    _currentDailyNote.value = _currentDailyNote.value?.copy(notes = "Error loading notes.")
                        ?: DailyPlannerNote(id = date.toEpochDay().toString(), date = date.toEpochDay(), notes = "Error loading notes.")
                }
                .collect { note ->
                    _currentDailyNote.value = note ?: DailyPlannerNote(
                        id = date.toEpochDay().toString(),
                        date = date.toEpochDay(),
                        notes = "" // Default if null is returned
                    )
                }
        }
    }

    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
        // If the selected date moves to a different month, update currentMonth
        val newMonth = YearMonth.from(date)
        if (newMonth != _currentMonth.value) {
            _currentMonth.value = newMonth // This will trigger re-loading meals for the new month
        }
    }

    fun changeMonth(offset: Long) { // +1 for next month, -1 for previous
        _currentMonth.value = _currentMonth.value.plusMonths(offset)
        _selectedDate.value = _currentMonth.value.atDay(1)
    }

    fun addRecipesToSelectedDate(recipes: List<RecipeSummary>) {
        val dateEpochDay = _selectedDate.value.toEpochDay()
        val newPlannedMeals = recipes.map { summary ->
            PlannedMeal(
                recipeId = summary.id,
                recipeTitle = summary.title,
                recipeImageUrl = summary.image,
                date = dateEpochDay
            )
        }
        viewModelScope.launch {
            val addedIds = recipeRepository.addMultiplePlannedMeals(newPlannedMeals)
            if (addedIds.isNotEmpty()) {
                loadPlannedMealsForMonth(_currentMonth.value)
            } else {
                // Handle error
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getMealsForSelectedDate(): Flow<DailyMealPlan?> {
        return _selectedDate.flatMapLatest { date ->
            val epochDay = date.toEpochDay()
            _monthlyPlannedMeals.map { allMealsMap ->
                allMealsMap[epochDay]?.let { meals ->
                    DailyMealPlan(epochDay, meals)
                }
            }
        }.distinctUntilChanged()
    }


    fun updateMealType(mealId: String, mealType: String?) {
        viewModelScope.launch {
            recipeRepository.updatePlannedMealType(mealId, mealType)
        }
    }

    fun removeMealFromPlan(mealId: String) {
        viewModelScope.launch {
            recipeRepository.removePlannedMeal(mealId)
        }
    }
    fun removeMultipleMealsFromPlan(mealIds: List<String>) {
        viewModelScope.launch {
            mealIds.forEach { mealId ->
                recipeRepository.removePlannedMeal(mealId)
            }
        }
    }
    // --- Daily Note ---
    fun saveCurrentDateNote(noteText: String) {
        val dateEpochDay = _selectedDate.value.toEpochDay()
        viewModelScope.launch {
            recipeRepository.saveDailyNote(dateEpochDay, noteText)
        }
    }
}

class MealPlannerViewModelFactory(
    private val recipeRepository: RecipeRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MealPlannerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MealPlannerViewModel(recipeRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}