package com.luutran.mycookingapp

import AppNavigation
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.luutran.mycookingapp.data.datastore.UserPreferencesRepository
import com.luutran.mycookingapp.data.local.AppDatabase
import com.luutran.mycookingapp.data.repository.RecipeRepository
import com.luutran.mycookingapp.navigation.NavDestinations
import com.luutran.mycookingapp.ui.bottombar.BottomNavItem
import com.luutran.mycookingapp.ui.drawer.AppDrawerContent
import com.luutran.mycookingapp.ui.home.ApiServiceInstance
import com.luutran.mycookingapp.ui.home.HomeViewModel
import com.luutran.mycookingapp.ui.home.HomeViewModelFactory
import com.luutran.mycookingapp.ui.auth.AuthViewModel
import com.luutran.mycookingapp.ui.theme.LightGreenApp
import com.luutran.mycookingapp.ui.theme.MyCookingAppTheme
import com.luutran.mycookingapp.ui.theme.OnLightGreenApp
import kotlinx.coroutines.launch

import androidx.compose.runtime.getValue
import androidx.compose.ui.text.style.TextAlign
import com.luutran.mycookingapp.ui.editprofile.ProfileViewModel

import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {
    private val recipeRepository: RecipeRepository by lazy {
        val apiService = ApiServiceInstance.instance
        val appDatabase = AppDatabase.getDatabase(applicationContext)
        val recipeDao = appDatabase.recipeDao()
        val suggestionDao = appDatabase.suggestedRecipeDao()
        val favoriteRecipeDao = appDatabase.favoriteRecipeDao()
        val firebaseAuth = Firebase.auth
        val firestore = Firebase.firestore
        RecipeRepository(
            apiService = apiService,
            recipeDao = recipeDao,
            suggestedRecipeDao = suggestionDao,
            favoriteRecipeDao = favoriteRecipeDao,
            firebaseAuth = firebaseAuth,
            firestore = firestore,
        )
    }

    private val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            MyCookingAppTheme {
                val authViewModel: AuthViewModel = viewModel()
                val profileViewModel: ProfileViewModel = viewModel()
                val isAuthenticated by authViewModel.isUserAuthenticated.collectAsStateWithLifecycle(initialValue = null)


                Log.d("MainActivity", "isAuthenticated collected: $isAuthenticated")

                // Determine start destination ONLY when isAuthenticated is no longer null
                when (isAuthenticated) {
                    true -> {
                        Log.d("MainActivity", "User is Authenticated. Navigating to HOME_SCREEN.")
                        MainAppScreen(
                            userPreferencesRepository = userPreferencesRepository,
                            recipeRepository = recipeRepository,
                            initialStartDestination = NavDestinations.HOME_SCREEN,
                            authViewModel = authViewModel,
                            profileViewModel = profileViewModel
                        )
                    }
                    false -> {
                        Log.d("MainActivity", "User is NOT Authenticated. Navigating to AUTH_SCREEN.")
                        MainAppScreen(
                            userPreferencesRepository = userPreferencesRepository,
                            recipeRepository = recipeRepository,
                            initialStartDestination = NavDestinations.AUTH_SCREEN,
                            authViewModel = authViewModel,
                            profileViewModel = profileViewModel
                        )
                    }
                    null -> {
                        Log.d("MainActivity", "Authentication state is NULL (loading). Showing loading indicator.")
                        // Show a loading indicator while auth state is being determined
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                            Text("Checking authentication...", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 16.dp))
                        }
                    }
                }
            }
        }
    }
    override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "onResume called. AuthStateListener in AuthViewModel should handle user state updates.")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    userPreferencesRepository: UserPreferencesRepository,
    recipeRepository: RecipeRepository,
    initialStartDestination: String,
    authViewModel: AuthViewModel,
    profileViewModel: ProfileViewModel
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = navBackStackEntry?.destination?.route

    Log.d("MainAppScreen", "Current Route: $currentRoute, Start Destination: $initialStartDestination")

    val application = LocalContext.current.applicationContext as Application
    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(
            application = application,
            recipeRepository = recipeRepository,
            userPreferencesRepository = userPreferencesRepository
        )
    )

    val bottomNavItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.MyCookedDishes,
        BottomNavItem.Favorites,
        BottomNavItem.MealPlanner,
    )

    val screensWithoutBottomBarOrDefaultTopAppBar = listOf(
        NavDestinations.AUTH_SCREEN,
        NavDestinations.RECIPE_DETAIL_SCREEN,
        NavDestinations.DEDICATED_SEARCH_SCREEN
    )

    val shouldShowBottomBar = currentRoute !in screensWithoutBottomBarOrDefaultTopAppBar &&
            currentRoute in listOf(
        NavDestinations.HOME_SCREEN,
        NavDestinations.FAVORITES,
        NavDestinations.MY_COOKED_DISHES_OVERVIEW_SCREEN,
        NavDestinations.MEAL_PLANNER_OVERVIEW_SCREEN,
        NavDestinations.EDIT_PROFILE_SCREEN,
        NavDestinations.SEARCH_RESULTS_SCREEN,
        NavDestinations.DISH_MEMORIES_LIST_SCREEN,
    )

    val screensThatHaveTheirOwnTopAppBarOrNoTopAppBar = listOf(
        NavDestinations.AUTH_SCREEN,
        NavDestinations.FORGOT_PASSWORD_SCREEN,
        NavDestinations.RECIPE_DETAIL_SCREEN,
        NavDestinations.DEDICATED_SEARCH_SCREEN,
        NavDestinations.HOME_SCREEN,
        NavDestinations.SEARCH_RESULTS_SCREEN,
        NavDestinations.EDIT_PROFILE_SCREEN,
        NavDestinations.MY_COOKED_DISHES_OVERVIEW_SCREEN,
        NavDestinations.DISH_MEMORIES_LIST_SCREEN,
        NavDestinations.DISH_MEMORY_DETAIL_SCREEN,
        NavDestinations.CREATE_NEW_MEMORY_SCREEN,
        NavDestinations.FAVORITES,
        NavDestinations.MEAL_PLANNER_OVERVIEW_SCREEN,
        NavDestinations.MEAL_PLANNER_DATE_SCREEN,
        NavDestinations.USER_RECIPE_CREATE_EDIT_SCREEN,
        NavDestinations.USER_RECIPE_MEMORIES_LIST_SCREEN,
        NavDestinations.USER_RECIPE_DETAIL_FULL_ROUTE,
        NavDestinations.USER_RECIPE_CREATE_EDIT_MEMORY,
        NavDestinations.USER_RECIPE_MEMORY_DETAIL_ROUTE,
        NavDestinations.VERIFY_EMAIL_SCREEN,
        NavDestinations.SETTINGS,
    )

    val shouldShowDefaultTopAppBar = currentRoute?.let { route ->
        !screensThatHaveTheirOwnTopAppBarOrNoTopAppBar.any { baseRoute ->
            route.startsWith(baseRoute)
        }
    } ?: false // If currentRoute is null, don't show default top app bar

    ModalNavigationDrawer(
        drawerState = drawerState,
        // Only enable gestures if not on AuthScreen
        gesturesEnabled = currentRoute != NavDestinations.AUTH_SCREEN,
        drawerContent = {
            AppDrawerContent(
                currentRoute = currentRoute ?: initialStartDestination,
                onNavigate = { drawerItem ->
                    navController.navigate(drawerItem.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                    scope.launch { drawerState.close() }
                },
                closeDrawer = { scope.launch { drawerState.close() } },
                authViewModel = authViewModel,
                profileViewModel = profileViewModel,
                modifier = Modifier,
                onEditProfileIconClick = {
                    navController.navigate(NavDestinations.EDIT_PROFILE_SCREEN)
                    scope.launch {
                        drawerState.close()
                    }
                },

            )
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                if (shouldShowDefaultTopAppBar) {
                    TopAppBar(
                        title = {
                            val title = when (currentRoute) {
                                NavDestinations.COOKING_NEWS_SCREEN -> "Cooking News"
                                else -> "My Cooking App" // Fallback
                            }
                            Text(text = title)
                        },
                        navigationIcon = {
                            if (currentRoute in listOf(NavDestinations.FAVORITES, NavDestinations.SETTINGS)) {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Filled.Menu, "Open Navigation Drawer")
                                }
                            } else { // For other screens using this default TopAppBar that are not top-level
                                IconButton(onClick = { navController.popBackStack() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = LightGreenApp,
                            titleContentColor = OnLightGreenApp,
                            navigationIconContentColor = OnLightGreenApp,
                            actionIconContentColor = OnLightGreenApp
                        )
                    )
                }
            },
            bottomBar = {
                if (shouldShowBottomBar) {
                    NavigationBar {
                        bottomNavItems.forEach { bottomNavItem ->
                            NavigationBarItem(
                                icon = { Icon(bottomNavItem.icon, contentDescription = bottomNavItem.title) },
                                label = { Text(bottomNavItem.title, textAlign = TextAlign.Center) },
                                selected = currentDestination?.hierarchy?.any { it.route == bottomNavItem.route } == true,
                                onClick = {
                                    navController.navigate(bottomNavItem.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = (bottomNavItem.route != NavDestinations.HOME_SCREEN) // Save state for non-home
                                            inclusive = (bottomNavItem.route == NavDestinations.HOME_SCREEN && currentRoute == NavDestinations.HOME_SCREEN)
                                        }
                                        launchSingleTop = true
                                        restoreState = (bottomNavItem.route != NavDestinations.HOME_SCREEN) // Restore for non-home
                                    }
                                }
                            )
                        }
                    }
                }
            }


        ) { innerPadding ->
            AppNavigation(
                navController = navController,
                modifier = Modifier.padding(innerPadding),
                homeViewModel = homeViewModel,
                recipeRepository = recipeRepository,
                onOpenDrawer = { scope.launch { drawerState.open() } },
                initialStartDestination = initialStartDestination,
                authViewModel = authViewModel
            )
        }
    }
}