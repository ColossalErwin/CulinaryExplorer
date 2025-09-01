package com.luutran.mycookingapp.ui.bottombar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector
import com.luutran.mycookingapp.navigation.NavDestinations

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Home : BottomNavItem(
        route = NavDestinations.HOME_SCREEN,
        title = "Home",
        icon = Icons.Filled.Home
    )

    data object Profile : BottomNavItem(
        route = NavDestinations.EDIT_PROFILE_SCREEN,
        title = "Profile",
        icon = Icons.Filled.Person
    )

    data object Cart : BottomNavItem(
        route = "cart",
        title = "Cart",
        icon = Icons.Filled.ShoppingCart
    )

    data object Favorites : BottomNavItem(
        route = NavDestinations.FAVORITES,
        title = "Favorites",
        icon = Icons.Filled.Favorite
    )

    data object MyCookedDishes : BottomNavItem(
        route = NavDestinations.MY_COOKED_DISHES_OVERVIEW_SCREEN,
        title = "Cooked Dishes",
        icon = Icons.Filled.PhotoLibrary
    )

    data object MealPlanner : BottomNavItem(
        route = NavDestinations.MEAL_PLANNER_OVERVIEW_SCREEN,
        title = "Meal Planner",
        icon = Icons.Filled.EditCalendar
    )
}