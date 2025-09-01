package com.luutran.mycookingapp.ui.mealplanner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.luutran.mycookingapp.data.model.PlannedMeal
import com.luutran.mycookingapp.data.repository.RecipeRepository
import com.luutran.mycookingapp.navigation.NavDestinations
import com.luutran.mycookingapp.ui.auth.AuthViewModel
import com.luutran.mycookingapp.ui.auth.EmailVerificationRequiredScreen
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealPlannerOverviewScreen(
    navController: NavHostController,
    recipeRepository: RecipeRepository,
    authViewModel: AuthViewModel, // Added AuthViewModel
    onNavigateToDateDetails: (Long) -> Unit,
) {
    // ViewModel setup remains the same
    val mealPlannerViewModelFactory = remember(recipeRepository) {
        MealPlannerViewModelFactory(recipeRepository)
    }
    val mealPlannerViewModel: MealPlannerViewModel = viewModel(factory = mealPlannerViewModelFactory)

    // Collect auth state
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
    val isEmailVerified by authViewModel.isEmailVerified.collectAsStateWithLifecycle()

    val user = currentUser
    // Determine if the user needs to verify (is a password user and not verified)
    val needsVerification = user != null && !isEmailVerified && user.providerData.any { it.providerId == "password" }

    // UI state from MealPlannerViewModel
    val uiState by mealPlannerViewModel.mealPlannerUiState.collectAsStateWithLifecycle()
    val selectedDate by mealPlannerViewModel.selectedDate.collectAsStateWithLifecycle()

    var showAddMealBottomSheet by remember { mutableStateOf(false) }
    val today = remember { LocalDate.now() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meal Planner") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            val isFutureOrTodaySelected = selectedDate.isAfter(today) || selectedDate.isEqual(today)
            // Show FAB only if logged in, verified, and a valid date is selected
            if (user != null && !needsVerification && isFutureOrTodaySelected) {
                FloatingActionButton(onClick = { showAddMealBottomSheet = true }) {
                    Icon(Icons.Filled.Add, "Add Meal to Selected Date")
                }
            }
        }
    ) { paddingValues ->
        // Main content decisions based on auth state
        if (user == null) {
            // User not signed in - Prompt to sign in
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Please Sign In",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "You need to be signed in to plan your meals.",
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    navController.navigate(NavDestinations.AUTH_SCREEN) {
                        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                        launchSingleTop = true
                    }
                }) {
                    Text("Sign In / Sign Up")
                }
            }
        } else if (needsVerification) {
            // User needs to verify email - Show verification prompt
            EmailVerificationRequiredScreen(
                authViewModel = authViewModel,
                navController = navController,
                featureName = "Meal Planner",
                modifier = Modifier.padding(paddingValues),
            )
        } else {
            // User is signed in and verified - Show Meal Planner content
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            ) {
                when (val state = uiState) {
                    is MealPlannerUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is MealPlannerUiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    is MealPlannerUiState.Success -> {
                        CalendarHeader(
                            currentMonth = state.currentMonth,
                            onPreviousMonth = { mealPlannerViewModel.changeMonth(-1) },
                            onNextMonth = { mealPlannerViewModel.changeMonth(1) }
                        )
                        CalendarView(
                            yearMonth = state.currentMonth,
                            selectedDate = state.selectedDate,
                            plannedMealsByDay = state.plannedMealsByDay,
                            onDateClick = { date -> mealPlannerViewModel.onDateSelected(date) },
                            onDateDoubleClick = { date -> onNavigateToDateDetails(date.toEpochDay()) }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        SelectedDateActions(
                            selectedDate = state.selectedDate,
                            plannedMealsForSelectedDate = state.plannedMealsByDay[state.selectedDate.toEpochDay()] ?: emptyList(),
                            onAddMealClick = { showAddMealBottomSheet = true },
                            onShowPlannedMealsClick = { onNavigateToDateDetails(state.selectedDate.toEpochDay()) }
                        )
                    }
                }
            }
        }
    }

    // Bottom sheet should only show if user is verified and allowed to interact
    if (user != null && !needsVerification && showAddMealBottomSheet) {
        AddMealBottomSheet(
            recipeRepository = recipeRepository,
            onDismiss = { showAddMealBottomSheet = false },
            onAddRecipes = { selectedRecipes ->
                mealPlannerViewModel.addRecipesToSelectedDate(selectedRecipes)
                showAddMealBottomSheet = false
            }
        )
    }
}

@Composable
fun CalendarHeader(
    currentMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous Month")
        }
        Text(
            text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
            style = MaterialTheme.typography.titleLarge
        )
        IconButton(onClick = onNextMonth) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next Month")
        }
    }
}

@Composable
fun CalendarView(
    yearMonth: YearMonth,
    selectedDate: LocalDate,
    plannedMealsByDay: Map<Long, List<PlannedMeal>>,
    onDateClick: (LocalDate) -> Unit,
    onDateDoubleClick: (LocalDate) -> Unit, // New parameter
    today: LocalDate = LocalDate.now() // For highlighting today
) {
    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDayOfMonth = yearMonth.atDay(1)
    val firstDayOfWeekValue = firstDayOfMonth.dayOfWeek.value % 7 // MON=1..SUN=7 -> 0..6 for array
    val emptyPrecedingCells = firstDayOfWeekValue

    val calendarDays = mutableListOf<LocalDate?>()
    repeat(emptyPrecedingCells) { calendarDays.add(null) } // Blank cells for previous month
    for (dayNum in 1..daysInMonth) {
        calendarDays.add(yearMonth.atDay(dayNum))
    }

    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        // Day of week headers
        Row(Modifier.fillMaxWidth()) {
            val daysOfWeek = DayOfWeek.entries.toTypedArray() // Default is MON-SUN
            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
                Text(
                    text = day,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            contentPadding = PaddingValues(0.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(calendarDays) { date ->
                if (date != null) {
                    val hasMeals = plannedMealsByDay[date.toEpochDay()]?.isNotEmpty() == true
                    val isPast = date.isBefore(today)

                    // Determine if the cell should be clickable for selection/navigation
                    val isSelectableOrNavigable = when {
                        !isPast -> true // Future or today dates are always selectable/navigable
                        isPast && hasMeals -> true // Past dates are selectable/navigable ONLY if they have meals
                        else -> false // Other past dates (no meals) are not
                    }

                    CalendarDayCell(
                        date = date,
                        isSelected = date == selectedDate,
                        isToday = date == today,
                        hasPlannedMeals = hasMeals,
                        isPastDateWithNoMeals = isPast && !hasMeals, // New state for distinct visual
                        isSelectable = isSelectableOrNavigable,     // Pass selectability
                        onClick = {
                            if (isSelectableOrNavigable) { // Only allow clicking today or future dates
                                onDateClick(date)
                            }
                        },
                        onDoubleClick = { // Pass the double click action
                            if (isSelectableOrNavigable) {
                                onDateDoubleClick(date)
                            }
                        }
                    )
                } else {
                    Spacer(Modifier.aspectRatio(1f)) // Empty cell
                }
            }
        }
    }
}

@Composable
fun CalendarDayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    hasPlannedMeals: Boolean,
    isPastDateWithNoMeals: Boolean,
    isSelectable: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit
) {
    val cellColor = when {
        isPastDateWithNoMeals -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        isSelected -> MaterialTheme.colorScheme.primary // Prominent selection
        isToday -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.surfaceVariant // Default cell
    }
    val contentColor = when {
        isPastDateWithNoMeals -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val borderColor = if (isToday && !isSelected) MaterialTheme.colorScheme.secondary else Color.Transparent

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.small)
            .background(cellColor)
            .border(2.dp, borderColor, MaterialTheme.shapes.small)
            .then(
                // Only apply click listeners if the cell is deemed selectable
                if (isSelectable) {
                    Modifier.pointerInput(date, isSelectable) {
                        detectTapGestures(
                            onTap = { onClick() },
                            onDoubleTap = { onDoubleClick() }
                        )
                    }
                } else {
                    Modifier // No click listeners if not selectable
                }
            )
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date.dayOfMonth.toString(),
                color = contentColor,
                fontSize = 16.sp,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
            )
            if (hasPlannedMeals) {
                Spacer(modifier = Modifier.height(2.dp))
                Icon(
                    imageVector = Icons.Filled.Circle, // Simple indicator for planned meals
                    contentDescription = "Has planned meals",
                    tint = if (isSelected) contentColor.copy(alpha = 0.7f)
                    else if (isPastDateWithNoMeals) contentColor.copy(alpha = 0.7f) // So it's visible on dim background
                    else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(8.dp)
                )
            }
        }
    }
}


@Composable
fun SelectedDateActions(
    selectedDate: LocalDate,
    plannedMealsForSelectedDate: List<PlannedMeal>,
    onAddMealClick: () -> Unit,
    onShowPlannedMealsClick: () -> Unit
) {
    val today = LocalDate.now()
    val isPastDate = selectedDate.isBefore(today)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Selected: ${selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"))}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (isPastDate) {
            Text(
                "Cannot add meals to past dates.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = onShowPlannedMealsClick,
                enabled = plannedMealsForSelectedDate.isNotEmpty() || !isPastDate
            ) {
                Icon(Icons.Filled.MenuBook, contentDescription = "View Meals", modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(if (plannedMealsForSelectedDate.isNotEmpty()) "View/Edit Meals" else "View Date")
            }

            if (!isPastDate) {
                OutlinedButton(
                    onClick = onAddMealClick,
                ) {
                    Icon(Icons.Filled.EditCalendar, contentDescription = "Add Meal", modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Add Meal")
                }
            }
        }
        if (plannedMealsForSelectedDate.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "${plannedMealsForSelectedDate.size} meal(s) planned for this day.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}