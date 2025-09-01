package com.luutran.mycookingapp.ui.drawer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

sealed class DrawerItem(
    val route: String,
    val icon: ImageVector,
    val title: String
) {
    data object Home : DrawerItem("home", Icons.Filled.Home, "Home")
    data object Favorites : DrawerItem("favorites", Icons.Filled.Star, "Favorites")
    data object Settings : DrawerItem("settings", Icons.Filled.Settings, "Settings")
}

val drawerItems = listOf(
    DrawerItem.Home,
    DrawerItem.Favorites,
    DrawerItem.Settings
)