package com.luutran.mycookingapp.navigation

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object NavDestinations {
    const val VERIFY_EMAIL_SCREEN = "verifyEmailScreen"
    const val FORGOT_PASSWORD_SCREEN = "forgotPasswordScreen"

    // --- Existing Destinations ---
    const val HOME_SCREEN = "home"
    const val FAVORITES = "favorites"
    const val SETTINGS = "settings"



    private const val RECIPE_DETAIL_SCREEN_BASE = "recipeDetail"
    const val RECIPE_DETAIL_ARG_ID = "recipeId"
    const val RECIPE_DETAIL_ARG_SHOW_FAB = "showFab"
    const val RECIPE_DETAIL_SCREEN =
        "$RECIPE_DETAIL_SCREEN_BASE/{$RECIPE_DETAIL_ARG_ID}?$RECIPE_DETAIL_ARG_SHOW_FAB={$RECIPE_DETAIL_ARG_SHOW_FAB}"


    // Updated function to create the route
    fun recipeDetailRoute(recipeId: Int, showFab: Boolean = true): String {
        return "$RECIPE_DETAIL_SCREEN_BASE/$recipeId?$RECIPE_DETAIL_ARG_SHOW_FAB=$showFab"
    }

    // --- Search Results ---
    private const val SEARCH_RESULTS_BASE_ROUTE = "searchResults"
    const val SEARCH_RESULTS_ARG_QUERY = "searchQuery"
    const val SEARCH_RESULTS_SCREEN = "$SEARCH_RESULTS_BASE_ROUTE/{$SEARCH_RESULTS_ARG_QUERY}"

    fun searchResultsRoute(query: String): String {
        val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
        return "$SEARCH_RESULTS_BASE_ROUTE/$encodedQuery"
    }

    const val DEDICATED_SEARCH_SCREEN = "dedicatedSearchScreen"
    const val CART_SCREEN = "cart"
    const val AUTH_SCREEN = "authScreen"
    const val PROFILE_SCREEN = "profileDisplay"
    const val EDIT_PROFILE_SCREEN = "editProfile"
    const val LOGIN_SCREEN = "login"

    // --- Dish Memories List Screen ---
    private const val DISH_MEMORIES_LIST_BASE_ROUTE = "dishMemoriesList"
    const val DISH_MEMORIES_LIST_ARG_RECIPE_ID = "recipeId"
    const val DISH_MEMORIES_LIST_ARG_RECIPE_TITLE = "recipeTitle"
    const val DISH_MEMORIES_LIST_ARG_RECIPE_IMAGE_URL = "recipeImageUrl"

    // Route: "dishMemoriesList/{recipeId}/{recipeTitle}?recipeImageUrl={recipeImageUrl}"
    const val DISH_MEMORIES_LIST_SCREEN =
        "$DISH_MEMORIES_LIST_BASE_ROUTE/{$DISH_MEMORIES_LIST_ARG_RECIPE_ID}/{$DISH_MEMORIES_LIST_ARG_RECIPE_TITLE}" +
                "?$DISH_MEMORIES_LIST_ARG_RECIPE_IMAGE_URL={$DISH_MEMORIES_LIST_ARG_RECIPE_IMAGE_URL}"

    fun dishMemoriesListRoute(recipeId: Int, recipeTitle: String, recipeImageUrl: String?): String {
        val encodedTitle = URLEncoder.encode(recipeTitle, StandardCharsets.UTF_8.toString())
        val encodedImageUrl = recipeImageUrl?.let { URLEncoder.encode(it, StandardCharsets.UTF_8.toString()) } ?: "null_image_url"
        return "$DISH_MEMORIES_LIST_BASE_ROUTE/$recipeId/$encodedTitle" +
                "?$DISH_MEMORIES_LIST_ARG_RECIPE_IMAGE_URL=$encodedImageUrl"
    }

    // --- Create New Memory Screen ---
    private const val CREATE_NEW_MEMORY_BASE_ROUTE = "createNewMemory"
    const val CREATE_NEW_MEMORY_ARG_RECIPE_ID = "recipeId"
    const val CREATE_NEW_MEMORY_ARG_MEMORY_ID = "memoryId"
    // Route: "createNewMemory/{recipeId}?memoryId={memoryId}" (memoryId is optional)
    const val CREATE_NEW_MEMORY_SCREEN =
        "$CREATE_NEW_MEMORY_BASE_ROUTE/{$CREATE_NEW_MEMORY_ARG_RECIPE_ID}" +
                "?$CREATE_NEW_MEMORY_ARG_MEMORY_ID={$CREATE_NEW_MEMORY_ARG_MEMORY_ID}"

    // Updated function to build the route
    fun createNewMemoryRoute(recipeId: Int, memoryId: String? = null): String {
        val baseRoute = "$CREATE_NEW_MEMORY_BASE_ROUTE/$recipeId"
        return if (memoryId != null) {
            "$baseRoute?$CREATE_NEW_MEMORY_ARG_MEMORY_ID=$memoryId"
        } else {
            baseRoute
        }
    }

    // --- Dish Memory Detail Screen ---
    private const val DISH_MEMORY_DETAIL_BASE_ROUTE = "dishMemoryDetail"
    const val DISH_MEMORY_DETAIL_ARG_RECIPE_ID = "recipeId"
    const val DISH_MEMORY_DETAIL_ARG_MEMORY_ID = "memoryId"
    const val DISH_MEMORY_DETAIL_ARG_RECIPE_TITLE = "recipeTitle"
    const val DISH_MEMORY_DETAIL_SCREEN =
        "$DISH_MEMORY_DETAIL_BASE_ROUTE/{$DISH_MEMORY_DETAIL_ARG_RECIPE_ID}/{$DISH_MEMORY_DETAIL_ARG_RECIPE_TITLE}/{$DISH_MEMORY_DETAIL_ARG_MEMORY_ID}"

    fun dishMemoryDetailRoute(recipeId: Int, recipeTitle: String, memoryId: String): String {
        val encodedTitle = URLEncoder.encode(recipeTitle, StandardCharsets.UTF_8.toString())
        return "$DISH_MEMORY_DETAIL_BASE_ROUTE/$recipeId/$encodedTitle/$memoryId"
    }

    const val MY_COOKED_DISHES_OVERVIEW_SCREEN = "myCookedDishesOverview"


    // --- Meal Planner ---
    const val MEAL_PLANNER_OVERVIEW_SCREEN = "mealPlannerOverview"

    private const val MEAL_PLANNER_DATE_SCREEN_BASE = "mealPlannerDate"
    const val MEAL_PLANNER_DATE_ARG_EPOCH_DAY = "dateEpochDay"
    // Route: "mealPlannerDate/{dateEpochDay}"
    const val MEAL_PLANNER_DATE_SCREEN = "$MEAL_PLANNER_DATE_SCREEN_BASE/{$MEAL_PLANNER_DATE_ARG_EPOCH_DAY}"

    fun mealPlannerDateRoute(dateEpochDay: Long): String {
        return "$MEAL_PLANNER_DATE_SCREEN_BASE/$dateEpochDay"
    }

    // --- Cooking News ---
    const val COOKING_NEWS_SCREEN = "cookingNews"

    // --- User Recipe Creation/Editing ---
    private const val USER_RECIPE_CREATE_EDIT_BASE_ROUTE = "userRecipeCreateEdit"
    const val USER_RECIPE_ARG_ID = "userRecipeId" // Argument for existing recipe ID (optional)
    // Route: "userRecipeCreateEdit?userRecipeId={userRecipeId}" (userRecipeId is optional for creation)
    const val USER_RECIPE_CREATE_EDIT_SCREEN =
        "$USER_RECIPE_CREATE_EDIT_BASE_ROUTE?$USER_RECIPE_ARG_ID={$USER_RECIPE_ARG_ID}"

    fun userRecipeCreateEditRoute(userRecipeId: String? = null): String {
        return if (userRecipeId != null) {
            "$USER_RECIPE_CREATE_EDIT_BASE_ROUTE?$USER_RECIPE_ARG_ID=$userRecipeId"
        } else {
            USER_RECIPE_CREATE_EDIT_BASE_ROUTE
        }
    }

    // --- User Recipe Memories List ---
    const val USER_RECIPE_MEMORIES_LIST_SCREEN = "userRecipeMemoriesList"
    const val USER_RECIPE_MEMORIES_LIST_ARG_USER_RECIPE_ID = "userRecipeId"
    const val USER_RECIPE_MEMORIES_LIST_ARG_USER_RECIPE_TITLE = "userRecipeTitle" // Encoded
    const val USER_RECIPE_MEMORIES_LIST_ARG_USER_RECIPE_IMAGE_URL = "userRecipeImageUrl" // Encoded

    // routeBase/{pathArg}?queryArgName1={queryArgValue1}&queryArgName2={queryArgValue2}
    val USER_RECIPE_MEMORIES_LIST_ROUTE_FULL =
        "$USER_RECIPE_MEMORIES_LIST_SCREEN/" +
                "{${USER_RECIPE_MEMORIES_LIST_ARG_USER_RECIPE_ID}}" + // Path argument
                "?${USER_RECIPE_MEMORIES_LIST_ARG_USER_RECIPE_TITLE}={${USER_RECIPE_MEMORIES_LIST_ARG_USER_RECIPE_TITLE}}" + // Query parameter 1
                "&${USER_RECIPE_MEMORIES_LIST_ARG_USER_RECIPE_IMAGE_URL}={${USER_RECIPE_MEMORIES_LIST_ARG_USER_RECIPE_IMAGE_URL}}"  // Query parameter 2

    // Helper function to build the navigation route with proper encoding
    fun createUserRecipeMemoriesListRoute(
        userRecipeId: String,
        userRecipeTitle: String,
        userRecipeImageUrl: String?
    ): String {
        val encodedTitle = URLEncoder.encode(userRecipeTitle, StandardCharsets.UTF_8.toString())
        val encodedImageUrl = userRecipeImageUrl?.let {
            if (it.isBlank()) "null_image_url" else URLEncoder.encode(it, StandardCharsets.UTF_8.toString())
        } ?: "null_image_url"

        return "$USER_RECIPE_MEMORIES_LIST_SCREEN/" +
                userRecipeId +
                "?${USER_RECIPE_MEMORIES_LIST_ARG_USER_RECIPE_TITLE}=$encodedTitle" +
                "&${USER_RECIPE_MEMORIES_LIST_ARG_USER_RECIPE_IMAGE_URL}=$encodedImageUrl"
    }

    //User recipe detail screen

    const val USER_RECIPE_DETAIL_SCREEN_ROUTE = "userRecipeDetail"
    const val USER_RECIPE_DETAIL_ARG_ID = "userRecipeId" // Argument name for the ID
    const val USER_RECIPE_DETAIL_FULL_ROUTE = "$USER_RECIPE_DETAIL_SCREEN_ROUTE/{$USER_RECIPE_DETAIL_ARG_ID}"

    fun userRecipeDetailRoute(userRecipeId: String) = "$USER_RECIPE_DETAIL_SCREEN_ROUTE/$userRecipeId"

    object NavigationResultKeys {
        const val RECIPE_UPDATED_FOR_DETAIL_KEY = "recipe_updated_for_detail" // From Create/Edit to Detail
        const val RECIPE_ID_KEY = "recipe_id" // To pass the ID of the updated recipe
        const val RECIPE_UPDATED_FOR_MEMORIES_KEY = "recipe_updated_for_memories" // From Detail to MemoriesList
    }

    // Route for creating/editing a user recipe memory
    //const val USER_RECIPE_CREATE_EDIT_MEMORY = "userRecipeCreateEditMemory/{userRecipeId}?memoryId={memoryId}"
    fun userRecipeCreateNewMemoryRoute(userRecipeId: String) = "userRecipeCreateEditMemory/$userRecipeId" // memoryId will be null
    fun userRecipeEditMemoryRoute(userRecipeId: String, memoryId: String) = "userRecipeCreateEditMemory/$userRecipeId?memoryId=$memoryId"


    const val USER_RECIPE_MEMORY_DETAIL_ROUTE = "userRecipeMemoryDetail"
    const val USER_RECIPE_MEMORY_DETAIL_ARG_USER_RECIPE_ID = "userRecipeId"
    const val USER_RECIPE_MEMORY_DETAIL_ARG_MEMORY_ID = "memoryId"
    const val USER_RECIPE_MEMORY_DETAIL_ARG_USER_RECIPE_TITLE = "userRecipeTitle" // Optional, for display

    fun userRecipeMemoryDetailRoute(userRecipeId: String, userRecipeTitle: String, memoryId: String): String {
        val encodedTitle = URLEncoder.encode(userRecipeTitle, StandardCharsets.UTF_8.toString())
        return "$USER_RECIPE_MEMORY_DETAIL_ROUTE/$userRecipeId/$memoryId?${USER_RECIPE_MEMORY_DETAIL_ARG_USER_RECIPE_TITLE}=${encodedTitle}"
    }

    const val USER_RECIPE_CREATE_NEW_MEMORY_ARG_USER_RECIPE_ID = "userRecipeId"
    const val USER_RECIPE_CREATE_NEW_MEMORY_ARG_MEMORY_ID = "memoryId"
    const val USER_RECIPE_CREATE_EDIT_MEMORY = "userRecipeCreateEditMemory/{$USER_RECIPE_CREATE_NEW_MEMORY_ARG_USER_RECIPE_ID}?$USER_RECIPE_CREATE_NEW_MEMORY_ARG_MEMORY_ID={$USER_RECIPE_CREATE_NEW_MEMORY_ARG_MEMORY_ID}"

    fun userRecipeCreateNewMemoryRoute(userRecipeId: String, memoryId: String? = null): String {
        return if (memoryId != null) {
            "userRecipeCreateEditMemory/$userRecipeId?$USER_RECIPE_CREATE_NEW_MEMORY_ARG_MEMORY_ID=$memoryId"
        } else {
            "userRecipeCreateEditMemory/$userRecipeId"
        }
    }
}