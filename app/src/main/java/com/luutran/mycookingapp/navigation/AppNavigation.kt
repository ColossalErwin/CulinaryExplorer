import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.luutran.mycookingapp.navigation.NavDestinations
import com.luutran.mycookingapp.ui.home.HomeScreen
import com.luutran.mycookingapp.ui.home.HomeViewModel
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.luutran.mycookingapp.data.repository.RecipeRepository
import com.luutran.mycookingapp.data.utils.getCookedDishRepositoryInstance
import com.luutran.mycookingapp.data.utils.getUserRecipeRepositoryInstance
import com.luutran.mycookingapp.ui.auth.AuthScreen
import com.luutran.mycookingapp.ui.auth.AuthViewModel
import com.luutran.mycookingapp.ui.auth.ForgotPasswordScreen
import com.luutran.mycookingapp.ui.auth.LoginScreen
import com.luutran.mycookingapp.ui.auth.NavigationEvent
import com.luutran.mycookingapp.ui.auth.VerifyEmailScreen
import com.luutran.mycookingapp.ui.dishmemories.CreateNewMemoryScreen
import com.luutran.mycookingapp.ui.dishmemories.DishMemoriesListScreen
import com.luutran.mycookingapp.ui.dishmemories.DishMemoryScreen
import com.luutran.mycookingapp.ui.dishmemories.MyCookedDishesOverviewScreen
import com.luutran.mycookingapp.ui.dishmemories.MyCookedDishesViewModel
import com.luutran.mycookingapp.ui.dishmemories.MyCookedDishesViewModelFactory
import com.luutran.mycookingapp.ui.dishmemories.MyUserCookedDishesViewModel
import com.luutran.mycookingapp.ui.dishmemories.MyUserCookedDishesViewModelFactory
import com.luutran.mycookingapp.ui.dishmemories.UserRecipeCreateNewMemoryScreen
import com.luutran.mycookingapp.ui.dishmemories.UserRecipeMemoriesListScreen
import com.luutran.mycookingapp.ui.dishmemories.UserRecipeMemoryDetailScreen
import com.luutran.mycookingapp.ui.editprofile.EditProfileScreen
import com.luutran.mycookingapp.ui.editprofile.ProfileViewModel
import com.luutran.mycookingapp.ui.favorites.FavoritesScreen
import com.luutran.mycookingapp.ui.favorites.FavoritesViewModel
import com.luutran.mycookingapp.ui.favorites.FavoritesViewModelFactory
import com.luutran.mycookingapp.ui.mealplanner.MealPlannerDateScreen
import com.luutran.mycookingapp.ui.mealplanner.MealPlannerOverviewScreen
import com.luutran.mycookingapp.ui.mealplanner.MealPlannerViewModel
import com.luutran.mycookingapp.ui.mealplanner.MealPlannerViewModelFactory
import com.luutran.mycookingapp.ui.recipedetail.RecipeDetailScreen
import com.luutran.mycookingapp.ui.recipedetail.UserRecipeDetailScreen
import com.luutran.mycookingapp.ui.search.SearchScreen
import com.luutran.mycookingapp.ui.searchresults.SearchResultsScreen
import com.luutran.mycookingapp.ui.settings.SettingsScreen
import com.luutran.mycookingapp.ui.userrecipe.CreateUserRecipeScreen
import com.luutran.mycookingapp.ui.userrecipe.CreateUserRecipeViewModel
import com.luutran.mycookingapp.ui.userrecipe.CreateUserRecipeViewModelFactory

@Composable
fun AppNavigation(
    navController: NavHostController,
    recipeRepository: RecipeRepository,
    homeViewModel: HomeViewModel,
    modifier: Modifier = Modifier,
    onOpenDrawer: () -> Unit,
    initialStartDestination: String,
    authViewModel: AuthViewModel
) {

    val mealPlannerViewModelFactory = MealPlannerViewModelFactory(recipeRepository)

    NavHost(
        navController = navController,
        startDestination = initialStartDestination,
        modifier = modifier
    ) {

        composable(NavDestinations.AUTH_SCREEN) {
            AuthScreen(
                authViewModel = authViewModel,
                onNavigateToHome = {
                    navController.navigate(NavDestinations.HOME_SCREEN) {
                        popUpTo(NavDestinations.AUTH_SCREEN) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToVerifyEmail = {
                    Log.d("AppNavigation", "AuthScreen -> Navigating to VerifyEmailScreen")
                    navController.navigate(NavDestinations.VERIFY_EMAIL_SCREEN) {
                        launchSingleTop = true
                    }
                },
                onNavigateToForgotPassword = { emailFromAuthScreen ->
                    Log.d("AppNavigation", "AuthScreen -> Preparing to navigate to ForgotPasswordScreen with email: $emailFromAuthScreen")
                    navController.navigate(NavDestinations.FORGOT_PASSWORD_SCREEN)
                }
            )
        }

        composable(NavDestinations.FORGOT_PASSWORD_SCREEN) {
            val navEvent by authViewModel.navigationEvent.collectAsState()
            val initialEmail = (navEvent as? NavigationEvent.NavigateToForgotPassword)?.email

            ForgotPasswordScreen(
                authViewModel = authViewModel,
                initialEmail = initialEmail,
                onNavigateBackToAuth = {
                    Log.d("AppNavigation", "ForgotPasswordScreen -> Navigating back to AuthScreen")
                    navController.popBackStack()
                }
            )
        }

        // VERIFY_EMAIL_SCREEN
        composable(NavDestinations.VERIFY_EMAIL_SCREEN) {
            VerifyEmailScreen(
                authViewModel = authViewModel,
                onNavigateToHome = {
                    Log.d("AppNavigation", "VerifyEmailScreen -> Navigating to Home")
                    navController.navigate(NavDestinations.HOME_SCREEN) {
                        popUpTo(NavDestinations.VERIFY_EMAIL_SCREEN) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToAuth = { // If user signs out from VerifyEmailScreen
                    Log.d("AppNavigation", "VerifyEmailScreen -> Navigating to Auth (Sign Out)")
                    navController.navigate(NavDestinations.AUTH_SCREEN) {
                        popUpTo(NavDestinations.HOME_SCREEN) { inclusive = true } // Clear home if it was reached
                        popUpTo(NavDestinations.VERIFY_EMAIL_SCREEN) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(NavDestinations.LOGIN_SCREEN) {
            LoginScreen(navController = navController)
        }


        composable(NavDestinations.EDIT_PROFILE_SCREEN) {
            val profileViewModel: ProfileViewModel = viewModel()
            EditProfileScreen(
                profileViewModel = profileViewModel,
                onNavigateBack = { navController.popBackStack() },
                navController = navController,
                authViewModel = authViewModel,
            )
        }


        composable(NavDestinations.HOME_SCREEN) {
            val featuredRecipeUiState by homeViewModel.featuredRecipeState.collectAsStateWithLifecycle()
            HomeScreen(
                featuredRecipeUiState = featuredRecipeUiState,
                onRecipeClick = { recipeId ->
                    val route = NavDestinations.recipeDetailRoute(recipeId)
                    navController.navigate(route)
                },
                onRetryFeaturedRecipe = { homeViewModel.fetchFeaturedRecipe() },
                modifier = Modifier.fillMaxSize(),
                navController = navController,
                onOpenDrawer = onOpenDrawer,
                authViewModel = authViewModel,
            )
        }

        composable(
            route = NavDestinations.RECIPE_DETAIL_SCREEN,
            arguments = listOf(
                navArgument(NavDestinations.RECIPE_DETAIL_ARG_ID) { type = NavType.IntType },
                navArgument(NavDestinations.RECIPE_DETAIL_ARG_SHOW_FAB) {
                    type = NavType.BoolType
                    defaultValue = true
                }
            )

        ) { backStackEntry ->
            val recipeIdArg = backStackEntry.arguments?.getInt(NavDestinations.RECIPE_DETAIL_ARG_ID)
            if (recipeIdArg != null) {
                RecipeDetailScreen(
                    authViewModel = authViewModel,
                    onNavigateUp = { navController.popBackStack() },
                    onNavigateToDishMemories = { recipeId, recipeTitle, recipeImageUrl ->
                        navController.navigate(
                            NavDestinations.dishMemoriesListRoute(
                                recipeId,
                                recipeTitle,
                                recipeImageUrl
                            )
                        )
                    }
                )
            } else {
                Log.e("AppNavigation", "RecipeId is null for RECIPE_DETAIL_SCREEN")
                navController.popBackStack()
            }
        }

        composable(
            route = "${NavDestinations.DEDICATED_SEARCH_SCREEN}?${NavDestinations.SEARCH_RESULTS_ARG_QUERY}={${NavDestinations.SEARCH_RESULTS_ARG_QUERY}}", // Modified route
            arguments = listOf(
                navArgument(NavDestinations.SEARCH_RESULTS_ARG_QUERY) {
                    type = NavType.StringType
                    nullable = true // Query is optional here
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val initialQuery = backStackEntry.arguments?.getString(NavDestinations.SEARCH_RESULTS_ARG_QUERY)
            SearchScreen(
                navController = navController,
                recipeRepository = recipeRepository,
                initialQuery = initialQuery // Pass the initial query
            )
        }

        composable(
            route = NavDestinations.SEARCH_RESULTS_SCREEN,
            arguments = listOf(navArgument(NavDestinations.SEARCH_RESULTS_ARG_QUERY) { type = NavType.StringType })
        ) { backStackEntry ->
            val searchQuery = backStackEntry.arguments?.getString(NavDestinations.SEARCH_RESULTS_ARG_QUERY) ?: ""
            SearchResultsScreen(
                searchQueryFromNav = searchQuery,
                recipeRepository = recipeRepository,
                onRecipeClick = { recipeId ->
                    val route = NavDestinations.recipeDetailRoute(recipeId = recipeId, showFab = true)
                    navController.navigate(route)
                },
                navController = navController
            )
        }

        composable(NavDestinations.MY_COOKED_DISHES_OVERVIEW_SCREEN) {
            val loggedDishesViewModel: MyCookedDishesViewModel = viewModel(
                factory = MyCookedDishesViewModelFactory(
                    cookedDishRepository = getCookedDishRepositoryInstance()
                )
            )

            val userRecipesViewModel: MyUserCookedDishesViewModel = viewModel(
                factory = MyUserCookedDishesViewModelFactory(
                    userRecipeRepository = getUserRecipeRepositoryInstance()
                )
            )
            MyCookedDishesOverviewScreen(
                onNavigateToDishMemories = { recipeId, recipeTitle, recipeImageUrl ->
                    navController.navigate(
                        NavDestinations.dishMemoriesListRoute(
                            recipeId,
                            recipeTitle,
                            recipeImageUrl
                        )
                    )
                },

                onNavigateToUserRecipeMemories = { userRecipeId, userRecipeTitle, userRecipeImageUrl ->
                    Log.d(
                        "AppNavigation",
                        "Preparing to navigate to UserRecipeMemoriesList. " +
                                "Received ID: '$userRecipeId', Title: '$userRecipeTitle', ImageURL: '$userRecipeImageUrl'"
                    )
                    val route = NavDestinations.createUserRecipeMemoriesListRoute(
                        userRecipeId = userRecipeId,
                        userRecipeTitle = userRecipeTitle,
                        userRecipeImageUrl = userRecipeImageUrl
                    )
                    Log.d("AppNavigation", "Built route: '$route'")
                    navController.navigate(route)
                },
                onNavigateUp = { navController.popBackStack() },
                authViewModel = authViewModel,
                navController = navController,
                onNavigateToCreateUserRecipe = {
                    navController.navigate(NavDestinations.userRecipeCreateEditRoute())
                },
                onNavigateToViewUserRecipe = { recipeId ->
                    Log.d("AppNavigation", "onNavigateToViewUserRecipe in NavHost. ID received: '$recipeId'")
                    val route = NavDestinations.userRecipeCreateEditRoute(userRecipeId = recipeId)
                    Log.d("AppNavigation", "Attempting to navigate to route: '$route'")
                    navController.navigate(route)
                },
                loggedDishesViewModel = loggedDishesViewModel,
                userRecipesViewModel = userRecipesViewModel // Pass the newly instantiated ViewModel
            )
        }
        composable(
            route = NavDestinations.USER_RECIPE_CREATE_EDIT_SCREEN,
            arguments = listOf(
                navArgument(NavDestinations.USER_RECIPE_ARG_ID) {
                    type = NavType.StringType
                    nullable = true // ID is optional (for create mode)
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val userRecipeIdFromBackStack = backStackEntry.arguments?.getString(NavDestinations.USER_RECIPE_ARG_ID)
            Log.d(
                "AppNavigation_CreateScreen",
                "Recipe ID directly from backStackEntry.arguments for key '${NavDestinations.USER_RECIPE_ARG_ID}': $userRecipeIdFromBackStack"
            )
            val createUserRecipeViewModel: CreateUserRecipeViewModel =
                viewModel(
                modelClass = CreateUserRecipeViewModel::class.java,
                viewModelStoreOwner = backStackEntry, // Scope to the NavBackStackEntry
                factory = CreateUserRecipeViewModelFactory(
                    savedStateHandle = backStackEntry.savedStateHandle,
                    userRecipeRepository = getUserRecipeRepositoryInstance()
                )
            )

            CreateUserRecipeScreen(
                onNavigateUp = { navController.popBackStack() },
                viewModel = createUserRecipeViewModel,
                navController = navController
            )
        }

        // USER RECIPE MEMORIES LIST SCREEN
        composable(
            route = NavDestinations.USER_RECIPE_MEMORIES_LIST_ROUTE_FULL,
            arguments = listOf(
                navArgument(NavDestinations.USER_RECIPE_MEMORIES_LIST_ARG_USER_RECIPE_ID) {
                    type = NavType.StringType
                },
                navArgument(NavDestinations.USER_RECIPE_MEMORIES_LIST_ARG_USER_RECIPE_TITLE) {
                    type = NavType.StringType
                },
                navArgument(NavDestinations.USER_RECIPE_MEMORIES_LIST_ARG_USER_RECIPE_IMAGE_URL) {
                    type = NavType.StringType
                    nullable = true
                }
            )
        ) {
            UserRecipeMemoriesListScreen(
                onNavigateUp = { navController.popBackStack() },
                onNavigateToCreateNewMemory = { currentRecipeId ->
                    navController.navigate(NavDestinations.userRecipeCreateNewMemoryRoute(currentRecipeId))
                },
                onNavigateToUserRecipeMemoryDetail = { userRecipeId, userRecipeTitle, memoryId ->
                    Log.d(
                        "AppNavigation",
                        "Preparing to navigate to UserRecipeMemoryDetail. UserRecipeID: '$userRecipeId', Title: '$userRecipeTitle', MemoryID: '$memoryId'"
                    )
                    val route = NavDestinations.userRecipeMemoryDetailRoute(
                        userRecipeId = userRecipeId,
                        userRecipeTitle = userRecipeTitle,
                        memoryId = memoryId
                    )
                    Log.d("AppNavigation", "Navigating to route: $route")
                    navController.navigate(route)
                },
                onNavigateToUserRecipeDetail = { userRecipeId ->
                    navController.navigate(NavDestinations.userRecipeDetailRoute(userRecipeId))
                },
                navController = navController
            )
        }



        composable(
            route = NavDestinations.USER_RECIPE_CREATE_EDIT_MEMORY,
            arguments = listOf(
                navArgument(NavDestinations.USER_RECIPE_CREATE_NEW_MEMORY_ARG_USER_RECIPE_ID) {
                    type = NavType.StringType
                    // Non-nullable if it's a path parameter and always required
                },
                navArgument(NavDestinations.USER_RECIPE_CREATE_NEW_MEMORY_ARG_MEMORY_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val userRecipeIdArg = backStackEntry.arguments?.getString(NavDestinations.USER_RECIPE_CREATE_NEW_MEMORY_ARG_USER_RECIPE_ID)
            val memoryIdArg = backStackEntry.arguments?.getString(NavDestinations.USER_RECIPE_CREATE_NEW_MEMORY_ARG_MEMORY_ID)

            Log.d(
                "AppNavigation",
                "Navigating to USER_RECIPE_CREATE_EDIT_MEMORY. UserRecipeID: $userRecipeIdArg, MemoryID: $memoryIdArg"
            )

            if (userRecipeIdArg != null) {
                UserRecipeCreateNewMemoryScreen(
                    navController = navController,
                    userRecipeId = userRecipeIdArg,
                    memoryId = memoryIdArg,
                    onNavigateUp = { navController.popBackStack() }
                )
            } else {
                Log.e(
                    "AppNavigation",
                    "Error: User Recipe ID is missing for USER_RECIPE_CREATE_EDIT_MEMORY. Popping back stack."
                )
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Error: User Recipe ID is missing. Please try again.",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        composable(
            route = "${NavDestinations.USER_RECIPE_MEMORY_DETAIL_ROUTE}/{${NavDestinations.USER_RECIPE_MEMORY_DETAIL_ARG_USER_RECIPE_ID}}/{${NavDestinations.USER_RECIPE_MEMORY_DETAIL_ARG_MEMORY_ID}}?${NavDestinations.USER_RECIPE_MEMORY_DETAIL_ARG_USER_RECIPE_TITLE}={${NavDestinations.USER_RECIPE_MEMORY_DETAIL_ARG_USER_RECIPE_TITLE}}",
            arguments = listOf(
                navArgument(NavDestinations.USER_RECIPE_MEMORY_DETAIL_ARG_USER_RECIPE_ID) { type = NavType.StringType },
                navArgument(NavDestinations.USER_RECIPE_MEMORY_DETAIL_ARG_MEMORY_ID) { type = NavType.StringType },
                navArgument(NavDestinations.USER_RECIPE_MEMORY_DETAIL_ARG_USER_RECIPE_TITLE) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = "Memory Details"
                }
            )
        ) {
            UserRecipeMemoryDetailScreen(
                onNavigateUp = { navController.popBackStack() },
                onNavigateToEditUserRecipeMemory = { userRecipeIdArg, memoryIdArg ->
                    navController.navigate(
                        NavDestinations.userRecipeCreateNewMemoryRoute(
                            userRecipeId = userRecipeIdArg,
                            memoryId = memoryIdArg
                        )
                    )
                }
            )
        }


        composable(
            route = NavDestinations.USER_RECIPE_DETAIL_FULL_ROUTE,
            arguments = listOf(
                navArgument(NavDestinations.USER_RECIPE_DETAIL_ARG_ID) { type = NavType.StringType }
            )
        ) {
            UserRecipeDetailScreen(
                onNavigateUp = { navController.popBackStack() },
                onNavigateToEditUserRecipe = { userRecipeId ->
                    navController.navigate(NavDestinations.userRecipeCreateEditRoute(userRecipeId = userRecipeId))
                },
                navController = navController
            )
        }


        composable(
            route = NavDestinations.DISH_MEMORIES_LIST_SCREEN,
            arguments = listOf(
                navArgument(NavDestinations.DISH_MEMORIES_LIST_ARG_RECIPE_ID) { type = NavType.IntType },
                navArgument(NavDestinations.DISH_MEMORIES_LIST_ARG_RECIPE_TITLE) { type = NavType.StringType },
                navArgument(NavDestinations.DISH_MEMORIES_LIST_ARG_RECIPE_IMAGE_URL) {
                    type = NavType.StringType
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val recipeId = backStackEntry.arguments?.getInt(NavDestinations.DISH_MEMORIES_LIST_ARG_RECIPE_ID)
            val encodedRecipeTitle = backStackEntry.arguments?.getString(NavDestinations.DISH_MEMORIES_LIST_ARG_RECIPE_TITLE)
            val encodedRecipeImageUrl = backStackEntry.arguments?.getString(NavDestinations.DISH_MEMORIES_LIST_ARG_RECIPE_IMAGE_URL)

            if (recipeId != null && encodedRecipeTitle != null) {
                val actualImageUrl = if (encodedRecipeImageUrl == "null_image_url") null else encodedRecipeImageUrl

                DishMemoriesListScreen(
                    recipeIdFromNav = recipeId,
                    encodedRecipeTitleFromNav = encodedRecipeTitle,
                    encodedRecipeImageUrlFromNav = actualImageUrl,
                    onNavigateUp = { navController.popBackStack() },
                    onNavigateToCreateNewMemory = { currentRecipeId ->
                        navController.navigate(NavDestinations.createNewMemoryRoute(currentRecipeId))
                    },
                    onNavigateToDishMemoryDetail = { navRecipeId, navRecipeTitle, navMemoryId ->
                        navController.navigate(NavDestinations.dishMemoryDetailRoute(navRecipeId, navRecipeTitle, navMemoryId))
                    },
                    onNavigateToRecipeDetail = { detailRecipeId ->
                        val route = NavDestinations.recipeDetailRoute(recipeId = detailRecipeId, showFab = false)
                        navController.navigate(route)
                    }
                )
            } else {
                Log.e("AppNavigation", "Missing critical arguments for DISH_MEMORIES_LIST_SCREEN")
                navController.popBackStack()
            }
        }

        composable(
            route = NavDestinations.CREATE_NEW_MEMORY_SCREEN,
            arguments = listOf(
                navArgument(NavDestinations.CREATE_NEW_MEMORY_ARG_RECIPE_ID) {
                    type = NavType.IntType
                },
                navArgument(NavDestinations.CREATE_NEW_MEMORY_ARG_MEMORY_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            CreateNewMemoryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = NavDestinations.DISH_MEMORY_DETAIL_SCREEN,
            arguments = listOf(
                navArgument(NavDestinations.DISH_MEMORY_DETAIL_ARG_RECIPE_ID) { type = NavType.IntType },
                navArgument(NavDestinations.DISH_MEMORY_DETAIL_ARG_RECIPE_TITLE) { type = NavType.StringType },
                navArgument(NavDestinations.DISH_MEMORY_DETAIL_ARG_MEMORY_ID) { type = NavType.StringType }
            )
        ) {
            DishMemoryScreen(
                onNavigateUp = { navController.popBackStack() },
                onNavigateToEditMemory = { recipeId, memoryId ->
                    navController.navigate(NavDestinations.createNewMemoryRoute(recipeId, memoryId))
                }
            )
        }

        composable(NavDestinations.PROFILE_SCREEN) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Profile Screen (Placeholder)") }
        }

        composable(NavDestinations.CART_SCREEN) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Cart Screen (Placeholder)") }
        }

        composable(NavDestinations.FAVORITES) {
            val favoritesViewModel: FavoritesViewModel = viewModel(
                factory = FavoritesViewModelFactory(recipeRepository = recipeRepository)
            )
            FavoritesScreen(
                favoritesViewModel = favoritesViewModel,
                authViewModel = authViewModel,
                onNavigateToRecipeDetail = { recipeId ->
                    navController.navigate(NavDestinations.recipeDetailRoute(recipeId))
                },
                onNavigateUp = { navController.popBackStack() },
                navController = navController
            )
        }

        composable(NavDestinations.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NavDestinations.COOKING_NEWS_SCREEN) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Cooking News Screen (Placeholder)") }
        }

        // MEAL PLANNER SCREENS

        composable(NavDestinations.MEAL_PLANNER_OVERVIEW_SCREEN) {

            MealPlannerOverviewScreen(
                recipeRepository = recipeRepository,
                onNavigateToDateDetails = { dateEpochDay ->
                    navController.navigate(NavDestinations.mealPlannerDateRoute(dateEpochDay))
                },
                authViewModel = authViewModel,
                navController = navController
            )
        }

        composable(
            route = NavDestinations.MEAL_PLANNER_DATE_SCREEN,
            arguments = listOf(
                navArgument(NavDestinations.MEAL_PLANNER_DATE_ARG_EPOCH_DAY) { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val dateEpochDay = backStackEntry.arguments?.getLong(NavDestinations.MEAL_PLANNER_DATE_ARG_EPOCH_DAY)

            if (dateEpochDay != null) {
                val mealPlannerViewModel: MealPlannerViewModel = viewModel(factory = mealPlannerViewModelFactory)

                MealPlannerDateScreen(
                    dateEpochDay = dateEpochDay,
                    recipeRepository = recipeRepository,
                    onNavigateUp = { navController.popBackStack() },
                    onNavigateToRecipeDetail = { recipeId ->
                        navController.navigate(
                            NavDestinations.recipeDetailRoute(
                                recipeId = recipeId,
                                showFab = true
                            )
                        )
                    },
                    mealPlannerViewModel = mealPlannerViewModel,
                )
            } else {
                Log.e("AppNavigation", "dateEpochDay is null for MEAL_PLANNER_DATE_SCREEN")
                navController.popBackStack() // Or navigate to an error screen
            }
        }
    }
}