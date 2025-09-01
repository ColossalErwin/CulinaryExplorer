package com.luutran.mycookingapp.ui.mealplanner

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.luutran.mycookingapp.data.model.PlannedMeal
import com.luutran.mycookingapp.data.repository.RecipeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.text.isNotBlank

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealPlannerDateScreen(
    dateEpochDay: Long,
    recipeRepository: RecipeRepository,
    onNavigateToRecipeDetail: (Int) -> Unit,
    onNavigateUp: () -> Unit,
    mealPlannerViewModel: MealPlannerViewModel
) {
    LaunchedEffect(dateEpochDay) {
        mealPlannerViewModel.onDateSelected(LocalDate.ofEpochDay(dateEpochDay))
    }

    val globalUiState by mealPlannerViewModel.mealPlannerUiState.collectAsState()
    val dailyMealPlan by mealPlannerViewModel.getMealsForSelectedDate().collectAsState(initial = null)

    val dailyNote by mealPlannerViewModel.currentDailyNote.collectAsState() // Collect the note

    val selectedDate = LocalDate.ofEpochDay(dateEpochDay)

    var showAddMealBottomSheet by remember { mutableStateOf(false) }
    var showNoteBottomSheet by remember { mutableStateOf(false) }

    var isInSelectionMode by remember { mutableStateOf(false) }
    val selectedMealIds = remember { mutableStateListOf<String>() }

    // State for showing the delete confirmation dialog
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // --- Logic to determine if the date is in the past ---
    val today = remember { LocalDate.now() }
    val isPastDate = selectedDate.isBefore(today)
    // --- End of past date logic ---

    val toggleSelection = { mealId: String ->
        if (selectedMealIds.contains(mealId)) {
            selectedMealIds.remove(mealId)
        } else {
            selectedMealIds.add(mealId)
        }
        if (selectedMealIds.isEmpty()) {
            isInSelectionMode = false
        }
    }

    val exitSelectionMode = {
        isInSelectionMode = false
        selectedMealIds.clear()
    }

    val attemptDeleteSelectedMeals = {
        if (selectedMealIds.isNotEmpty()) {
            showDeleteConfirmDialog = true // Show dialog instead of direct delete
        }
    }

    val confirmDeleteSelectedMeals = {
        if (selectedMealIds.isNotEmpty()) {
            mealPlannerViewModel.removeMultipleMealsFromPlan(selectedMealIds.toList())
            exitSelectionMode()
        }
        showDeleteConfirmDialog = false
    }

    Scaffold(
        topBar = {
            if (isInSelectionMode) {
                TopAppBar(
                    title = { Text("${selectedMealIds.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = exitSelectionMode) {
                            Icon(Icons.Filled.Close, "Clear Selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = attemptDeleteSelectedMeals) { // Changed to attemptDelete
                            Icon(Icons.Filled.Delete, "Delete Selected Meals", tint = MaterialTheme.colorScheme.error)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            } else {
                TopAppBar(
                    title = { Text(selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))) },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (isInSelectionMode) exitSelectionMode() else onNavigateUp()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showNoteBottomSheet = true }) {
                            Icon(Icons.AutoMirrored.Outlined.NoteAdd, "Add/Edit Note")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        },
        floatingActionButton = {
            if (!isInSelectionMode && !isPastDate) {
                FloatingActionButton(onClick = { showAddMealBottomSheet = true }) {
                    Icon(Icons.Filled.Add, "Add Meal to this Date")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            when (globalUiState) {
                is MealPlannerUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is MealPlannerUiState.Success, is MealPlannerUiState.Error -> {
                    dailyNote?.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clickable { showNoteBottomSheet = true },
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = notes,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                // "Manage Notes" Text
                                Text(
                                    text = "Manage Notes",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    if (dailyMealPlan == null || dailyMealPlan?.meals.isNullOrEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "There's no meal yet, please click the '+' button to add some.",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(dailyMealPlan!!.meals, key = { it.id }) { meal ->
                                val isSelected = selectedMealIds.contains(meal.id)
                                PlannedMealItemCard(
                                    meal = meal,
                                    isSelected = isSelected,
                                    isInSelectionMode = isInSelectionMode,
                                    onMealTypeChange = { newType ->
                                        if (!isInSelectionMode) {
                                            mealPlannerViewModel.updateMealType(meal.id, newType)
                                        }
                                    },
                                    onItemClick = {
                                        if (isInSelectionMode) {
                                            toggleSelection(meal.id)
                                        } else {
                                            onNavigateToRecipeDetail(meal.recipeId)
                                        }
                                    },
                                    onItemLongClick = {
                                        if (!isInSelectionMode) {
                                            isInSelectionMode = true
                                            toggleSelection(meal.id)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddMealBottomSheet) {
        AddMealBottomSheet(
            recipeRepository = recipeRepository,
            onDismiss = { showAddMealBottomSheet = false },
            onAddRecipes = { selectedRecipes ->
                mealPlannerViewModel.addRecipesToSelectedDate(selectedRecipes)
                showAddMealBottomSheet = false
            }
        )
    }

    if (showNoteBottomSheet) {
        DailyNoteBottomSheet(
            initialNote = dailyNote?.notes ?: "",
            onDismiss = { showNoteBottomSheet = false },
            onNoteChange = { newNoteText ->
                mealPlannerViewModel.saveCurrentDateNote(newNoteText)
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Confirm Deletion") },
            text = {
                Text(
                    "Are you sure you want to delete ${selectedMealIds.size} " +
                            if (selectedMealIds.size == 1) "meal?" else "meals?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = confirmDeleteSelectedMeals,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

class Debouncer(
    private val delayMillis: Long,
    private val coroutineScope: CoroutineScope,
    private val action: (String) -> Unit
) {
    private var job: Job? = null
    fun offer(value: String) {
        job?.cancel()
        job = coroutineScope.launch {
            delay(delayMillis)
            action(value)
        }
    }
    fun cancel() {
        job?.cancel()
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyNoteBottomSheet(
    initialNote: String,
    onDismiss: () -> Unit,
    onNoteChange: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // Keep track of the text within the sheet
    var noteTextState by remember(initialNote) { mutableStateOf(initialNote) }

    // Create a CoroutineScope that is lifecycle-aware for the Debouncer
    val localCoroutineScope = rememberCoroutineScope { SupervisorJob() } // SupervisorJob for independent children

    val debouncer = remember(localCoroutineScope) { // Recreate if scope changes (though it shouldn't here)
        Debouncer(
            delayMillis = 700L, // e.g., 700ms
            coroutineScope = localCoroutineScope,
            action = { textToSave ->
                onNoteChange(textToSave)
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            Log.d("DailyNoteBottomSheet", "Disposing DailyNoteBottomSheet.")
            // If the text changed from its initial state when the sheet was opened,
            // and the sheet is disposed without the explicit "Save" or normal dismiss,
            // ensure the last debounced action or an immediate save occurs.
            if (noteTextState != initialNote) {
                Log.d("DailyNoteBottomSheet", "Disposing with changes, attempting final save for: $noteTextState")
                onNoteChange(noteTextState) // Direct save of current text
            }
            debouncer.cancel() // Cancel any pending debounce job
            localCoroutineScope.cancel() // Cancel the scope itself
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            // This is called when dismissing by clicking outside or back gesture
            Log.d("DailyNoteBottomSheet", "onDismissRequest called. Current text: '$noteTextState'")
            if (noteTextState != initialNote) {
                // Immediately save the current state if it changed, bypassing debounce for this explicit dismiss action
                Log.d("DailyNoteBottomSheet", "Dismissing with changes, saving: '$noteTextState'")
                onNoteChange(noteTextState)
            }
            onDismiss() // Then call the lambda to hide the sheet
        },
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets.ime.add(WindowInsets.navigationBars) }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .navigationBarsPadding()
                .imePadding()
        ) {
            Text(
                "Notes for this day",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            OutlinedTextField(
                value = noteTextState,
                onValueChange = { newText ->
                    noteTextState = newText
                    if (newText != initialNote) {
                        debouncer.offer(newText)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 350.dp),
                label = { Text("Type your notes here...") },
                textStyle = MaterialTheme.typography.bodyLarge,
                keyboardOptions = KeyboardOptions.Default.copy(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Default
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    Log.d("DailyNoteBottomSheet", "Save button clicked. Current text: '$noteTextState'")
                    if (noteTextState != initialNote) {
                        onNoteChange(noteTextState)
                    }
                    onDismiss() // Hide the sheet
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Save")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun PlannedMealItemCard(
    meal: PlannedMeal,
    isSelected: Boolean,
    isInSelectionMode: Boolean,
    onMealTypeChange: (String?) -> Unit,
    onItemClick: () -> Unit,
    onItemLongClick: () -> Unit
) {
    var mealTypeDropdownExpanded by remember { mutableStateOf(false) }
    val mealTypes = listOf("Breakfast", "Lunch", "Dinner", "Snack", "Dessert", "Side Dish", "Appetizer", "Other")

    val cardBackgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onItemClick,
                onLongClick = onItemLongClick
            ),
        colors = CardDefaults.elevatedCardColors(containerColor = cardBackgroundColor)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isInSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onItemClick() },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(meal.recipeImageUrl ?: "https://spoonacular.com/recipeImages/${meal.recipeId}-312x231.jpg")
                    .crossfade(true)
                    .build(),
                contentDescription = meal.recipeTitle,
                modifier = Modifier
                    .size(if (isInSelectionMode) 60.dp else 80.dp)
                    .aspectRatio(1f)
                    .padding(end = 12.dp)
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(meal.recipeTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))

                Box {
                    AssistChip(
                        onClick = {
                            if (!isInSelectionMode) mealTypeDropdownExpanded = true
                        },
                        label = { Text(meal.mealType ?: "Choose Dish Type") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (meal.mealType != null) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = if (meal.mealType != null) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        enabled = !isInSelectionMode
                    )
                    if (!isInSelectionMode) {
                        DropdownMenu(
                            expanded = mealTypeDropdownExpanded,
                            onDismissRequest = { mealTypeDropdownExpanded = false }
                        ) {
                            mealTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type) },
                                    onClick = {
                                        onMealTypeChange(type)
                                        mealTypeDropdownExpanded = false
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Clear Type", style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.error)) },
                                onClick = {
                                    onMealTypeChange(null)
                                    mealTypeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            if (isInSelectionMode) {
                val iconImage = if (isSelected) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank
                val contentDesc = if (isSelected) "Selected" else "Not Selected"
                val iconTint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

                Icon(
                    imageVector = iconImage,
                    contentDescription = contentDesc,
                    tint = iconTint,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}