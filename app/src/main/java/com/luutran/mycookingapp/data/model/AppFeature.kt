package com.luutran.mycookingapp.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.ui.graphics.vector.ImageVector
import com.luutran.mycookingapp.navigation.NavDestinations

data class AppFeature(
    val name: String,
    val icon: ImageVector, // Using Material Icons
    val route: String // For navigation when clicked
)

// Example features (replace with your actual features and routes)
val appFeaturesList = listOf(
    AppFeature("Meal Planner", Icons.Filled.EditCalendar, NavDestinations.MEAL_PLANNER_OVERVIEW_SCREEN),
    AppFeature(
        "Cooked Dishes",
        Icons.Filled.PhotoLibrary,
        NavDestinations.MY_COOKED_DISHES_OVERVIEW_SCREEN
    ),
    AppFeature("Cooking News", Icons.Filled.Explore, NavDestinations.COOKING_NEWS_SCREEN),
)