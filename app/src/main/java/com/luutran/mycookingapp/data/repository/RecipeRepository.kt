package com.luutran.mycookingapp.data.repository


import androidx.datastore.core.IOException
import com.luutran.mycookingapp.data.local.dao.RecipeDao
import com.luutran.mycookingapp.data.model.RecipeDetail
import com.luutran.mycookingapp.data.model.RecipeSearchResponse
import com.luutran.mycookingapp.data.network.SpoonacularApiService
import com.luutran.mycookingapp.data.utils.toRecipeDetail
import com.luutran.mycookingapp.data.utils.toRecipeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

import android.util.Log
import androidx.compose.animation.core.copy
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.size
import androidx.core.util.remove
import androidx.core.util.set
import com.luutran.mycookingapp.data.local.dao.FavoriteRecipeDao
import com.luutran.mycookingapp.data.local.entity.FavoriteRecipeEntity
// TODO: Create and import these for Firestore integration
// import com.luutran.mycookingapp.data.network.FirestoreService
// import com.luutran.mycookingapp.domain.CurrentUserIdProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.google.firebase.auth.FirebaseAuth // Import FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.luutran.mycookingapp.ui.favorites.FavoriteRecipeDisplayItem
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await


import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import com.google.firebase.firestore.Query
import com.luutran.mycookingapp.data.local.dao.SuggestedRecipeDao
import com.luutran.mycookingapp.data.local.entity.toRecipeSummary
import com.luutran.mycookingapp.data.local.entity.toSuggestedRecipeSummaryEntity
import com.luutran.mycookingapp.data.model.DailyPlannerNote
import com.luutran.mycookingapp.data.model.PlannedMeal
import com.luutran.mycookingapp.data.model.RecipeSummary
import com.luutran.mycookingapp.ui.home.SuggestionCategoryType


class RecipeRepository(
    private val apiService: SpoonacularApiService,
    private val recipeDao: RecipeDao,
    private val favoriteRecipeDao: FavoriteRecipeDao,
    private val suggestedRecipeDao: SuggestedRecipeDao,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO) // For fire-and-forget Firestore updates
) {
    // --- Suggestion Caching Logic ---

    private suspend fun getCachedSuggestions(
        featuredRecipeId: Int,
        categoryType: SuggestionCategoryType,
        categoryValue: String
    ): List<RecipeSummary>? {
        return withContext(Dispatchers.IO) {

            val cachedEntities = suggestedRecipeDao.getSuggestions(featuredRecipeId, categoryType, categoryValue)
            if (cachedEntities.isNotEmpty()) {
                Log.d("RecipeRepository", "Found ${cachedEntities.size} cached suggestions for $featuredRecipeId, $categoryType, $categoryValue")
                cachedEntities.map { it.toRecipeSummary() }
            } else {
                Log.d("RecipeRepository", "No cached suggestions for $featuredRecipeId, $categoryType, $categoryValue")
                null // Explicitly return null if not found or empty, to trigger network fetch
            }
        }
    }

    private suspend fun cacheSuggestions(
        featuredRecipeId: Int,
        categoryType: SuggestionCategoryType,
        categoryValue: String,
        suggestions: List<RecipeSummary>
    ) {
        withContext(Dispatchers.IO) {
            if (suggestions.isEmpty()) {
                Log.d("RecipeRepository", "Not caching empty suggestions for $featuredRecipeId, $categoryType, $categoryValue")
                return@withContext
            }
            Log.d("RecipeRepository", "Caching ${suggestions.size} suggestions for $featuredRecipeId, $categoryType, $categoryValue")
            // Clear old suggestions for this specific category before inserting new ones
            suggestedRecipeDao.clearSuggestionsForCategory(featuredRecipeId, categoryType, categoryValue)
            val entities = suggestions.mapIndexed { index, summary ->
                summary.toSuggestedRecipeSummaryEntity(featuredRecipeId, categoryType, categoryValue, index)
            }
            suggestedRecipeDao.insertSuggestedRecipes(entities)
        }
    }

    suspend fun clearOldSuggestionsForFeaturedRecipe(oldFeaturedRecipeId: Int) {
        withContext(Dispatchers.IO) {
            Log.d("RecipeRepository", "Clearing all suggestions for old featured recipe ID: $oldFeaturedRecipeId")
            suggestedRecipeDao.clearSuggestionsForFeaturedRecipe(oldFeaturedRecipeId)
        }
    }

    suspend fun getRecipesByCuisine(
        cuisine: String,
        number: Int = 100,
        offset: Int = 0,
        excludeRecipeId: Int? = null,
        forceRefresh: Boolean = false // Add for pull-to-refresh scenarios
    ): List<RecipeSummary> {
        return withContext(Dispatchers.IO) {
            if (cuisine.isBlank() || excludeRecipeId == null) { // excludeRecipeId is now our featuredRecipeId context
                Log.w("RecipeRepository", "Cuisine is blank or featuredRecipeId (excludeRecipeId) is null. Cannot use cache for suggestions.")
            }

            val categoryType = SuggestionCategoryType.CUISINE

            // 1. Try cache if it's the first page (offset == 0) and not forcing refresh
            if (offset == 0 && excludeRecipeId != null && !forceRefresh) {
                val cached = getCachedSuggestions(excludeRecipeId, categoryType, cuisine)
                if (cached != null) { // Not null means cache hit (could be empty list from cache)
                    Log.d("RecipeRepository", "GetByCuisine: Using CACHED data for '$cuisine' (Featured ID: $excludeRecipeId)")
                    return@withContext cached.take(number) // Ensure we respect the 'number' for the current page
                }
            }

            // 2. Network Fetch (if cache miss, not first page, or forceRefresh)
            Log.d("RecipeRepository", "GetByCuisine: Fetching from NETWORK for '$cuisine' (Featured ID: $excludeRecipeId, Offset: $offset)")
            if (cuisine.isBlank()) {
                return@withContext emptyList()
            }
            try {
                val effectiveNumber = number // When fetching for cache, fetch the full page size

                val response = apiService.searchRecipesByCuisine(
                    cuisine = cuisine,
                    number = effectiveNumber,
                    offset = offset // Pass the offset for pagination
                )
                Log.d("RecipeRepository", "GetByCuisine - Network Response: ${response.code()}")

                if (response.isSuccessful) {
                    val searchResponse = response.body()
                    val recipes = searchResponse?.results ?: emptyList()

                    // If it's the first page fetch and we have a featured recipe context, cache it
                    if (offset == 0 && excludeRecipeId != null) {
                        cacheSuggestions(excludeRecipeId, categoryType, cuisine, recipes)
                    }
                    recipes.take(number)
                } else {
                    Log.e("RecipeRepository", "GetByCuisine - API Error: ${response.code()} - ${response.message()}")
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e("RecipeRepository", "GetByCuisine - Exception: ${e.message}", e)
                emptyList()
            }
        }
    }

    suspend fun getRecipesByDiet(
        diet: String,
        number: Int = 100,
        offset: Int = 0,
        excludeRecipeId: Int? = null,
        forceRefresh: Boolean = false
    ): List<RecipeSummary> {
        return withContext(Dispatchers.IO) {
            if (diet.isBlank() || excludeRecipeId == null) {
                Log.w("RecipeRepository", "Diet is blank or featuredRecipeId (excludeRecipeId) is null. Cannot use cache for suggestions.")
            }
            val categoryType = SuggestionCategoryType.DIET_OR_INTOLERANCE

            if (offset == 0 && excludeRecipeId != null && !forceRefresh) {
                val cached = getCachedSuggestions(excludeRecipeId, categoryType, diet)
                if (cached != null) {
                    Log.d("RecipeRepository", "GetByDiet: Using CACHED data for '$diet' (Featured ID: $excludeRecipeId)")
                    return@withContext cached.take(number)
                }
            }

            Log.d("RecipeRepository", "GetByDiet: Fetching from NETWORK for '$diet' (Featured ID: $excludeRecipeId, Offset: $offset)")
            if (diet.isBlank()) {
                return@withContext emptyList()
            }
            try {
                val effectiveNumber = number
                val response = apiService.searchRecipesByDiet(
                    diet = diet,
                    number = effectiveNumber,
                    offset = offset
                )
                Log.d("RecipeRepository", "GetByDiet - Network Response: ${response.code()}")

                if (response.isSuccessful) {
                    val searchResponse = response.body()
                    var recipes = searchResponse?.results ?: emptyList()
                    if (offset == 0 && excludeRecipeId != null) {
                        cacheSuggestions(excludeRecipeId, categoryType, diet, recipes)
                    }
                    recipes.take(number)
                } else {
                    Log.e("RecipeRepository", "GetByDiet - API Error: ${response.code()} - ${response.message()}")
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e("RecipeRepository", "GetByDiet - Exception: ${e.message}", e)
                emptyList()
            }
        }
    }
    suspend fun searchRecipes(
        query: String,
        sort: String? = null,
        sortDirection: String? = null,
        diet: String? = null,
        intolerances: String? = null,
        cuisine: String? = null,
        type: String? = null,
        includeIngredients: String? = null,
        excludeIngredients: String? = null,
        maxReadyTime: Int? = null,
        number: Int = 20, // Default number of results
        offset: Int? = null // For pagination
    ): RecipeSearchResponse? {
        return withContext(Dispatchers.IO) {
            // It's good practice to ensure the base query isn't blank if filters are also sparse
            if (query.isBlank() && diet.isNullOrBlank() && intolerances.isNullOrBlank() &&
                cuisine.isNullOrBlank() && type.isNullOrBlank() && includeIngredients.isNullOrBlank()
            ) {
            }

            try {
                println("RecipeRepository: Searching recipes. Query: '$query', Sort: '$sort $sortDirection', Diet: '$diet', Intolerances: '$intolerances', Cuisine: '$cuisine', Type: '$type', MaxTime: $maxReadyTime, InclIng: '$includeIngredients', ExclIng: '$excludeIngredients'")
                val response = apiService.searchRecipesComplex(
                    // apiKey is handled by the interface default or interceptor
                    query = query.takeIf { it.isNotBlank() }, // Send query only if not blank
                    number = number,
                    offset = offset,
                    sort = sort,
                    sortDirection = sortDirection,
                    diet = diet,
                    intolerances = intolerances,
                    cuisine = cuisine,
                    type = type,
                    includeIngredients = includeIngredients,
                    excludeIngredients = excludeIngredients,
                    maxReadyTime = maxReadyTime
                    // Pass other parameters here
                )
                println("RecipeRepository: Search NETWORK API Response Code: ${response.code()}, Successful: ${response.isSuccessful}")

                if (response.isSuccessful) {
                    val searchResponse = response.body()
                    if (searchResponse != null) {
                        println("RecipeRepository: Found ${searchResponse.totalResults} recipes. Returning ${searchResponse.results.size}.")
                    } else {
                        println("RecipeRepository: Search response body is null.")
                    }
                    searchResponse
                } else {
                    println("RecipeRepository: NETWORK API Error (Search): ${response.code()} - ${response.message()}. Body: ${response.errorBody()?.string()}")
                    null // Or throw custom exception
                }
            } catch (e: IOException) { // More specific network error
                println("RecipeRepository: Network IOException during API call (Search): ${e.message}")
                e.printStackTrace()
                null // Or throw
            } catch (e: Exception) { // Generic exception
                println("RecipeRepository: Generic Exception during API call (Search): ${e.message}")
                e.printStackTrace()
                null // Or throw
            }
        }
    }

    // --- Method to get from CACHE ---
    suspend fun getCachedRecipeByApiId(recipeApiId: Int): RecipeDetail? {
        return withContext(Dispatchers.IO) {
            println("RecipeRepository: Attempting to fetch recipe $recipeApiId from CACHE (Room)...")
            val entity = recipeDao.getRecipeById(recipeApiId)
            if (entity != null) {
                println("RecipeRepository: Recipe $recipeApiId FOUND in CACHE.")
            } else {
                println("RecipeRepository: Recipe $recipeApiId NOT FOUND in CACHE.")
            }
            entity?.toRecipeDetail()
        }
    }

    // --- Method to save to CACHE ---
    private suspend fun cacheRecipe(recipe: RecipeDetail) {
        withContext(Dispatchers.IO) {
            println("RecipeRepository: Caching recipe ${recipe.id} to CACHE (Room)...")
            val entity = recipe.toRecipeEntity()
            recipeDao.insertRecipe(entity)
            println("RecipeRepository: Recipe ${recipe.id} cached.")
        }
    }

    // --- Methods to get from NETWORK ---

    // Fetches a single random recipe (FROM NETWORK)
    private suspend fun getRandomRecipeFromNetwork(): RecipeDetail? {
        return withContext(Dispatchers.IO) {
            try {
                println("RecipeRepository: Attempting to fetch random recipe from NETWORK...")
                val response = apiService.getRandomRecipes(apiKey = SpoonacularApiService.API_KEY, number = 1)
                println("RecipeRepository: NETWORK API Response Code: ${response.code()}, Successful: ${response.isSuccessful}")

                if (response.isSuccessful) {
                    val randomRecipeResponse = response.body()
                    val firstRecipe = randomRecipeResponse?.recipes?.firstOrNull()
                    if (firstRecipe != null) {
                        println("RecipeRepository: Random recipe ${firstRecipe.id} fetched successfully from NETWORK.")
                    } else {
                        println("RecipeRepository: Network responded successfully but no recipes found in random list.")
                    }
                    firstRecipe
                } else {
                    println("RecipeRepository: NETWORK API Error (Random): ${response.code()} - ${response.message()}")
                    null
                }
            } catch (e: Exception) {
                println("RecipeRepository: Exception during NETWORK API call (Random): ${e.message}")
                e.printStackTrace()
                null
            }
        }
    }

    // Fetches recipe details by ID (FROM NETWORK)
    private suspend fun getRecipeDetailsByIdFromNetwork(recipeApiId: Int): RecipeDetail? {
        return withContext(Dispatchers.IO) {
            try {
                println("RecipeRepository: Attempting to fetch recipe by ID $recipeApiId from NETWORK...")
                val response = apiService.getRecipeInformation(
                    recipeId = recipeApiId,
                    apiKey = SpoonacularApiService.API_KEY
                )
                println("RecipeRepository: NETWORK API Response Code (By ID $recipeApiId): ${response.code()}, Successful: ${response.isSuccessful}")

                if (response.isSuccessful) {
                    val recipeDetail = response.body()
                    if (recipeDetail != null) {
                        println("RecipeRepository: Recipe details for $recipeApiId fetched successfully from NETWORK.")
                    } else {
                        println("RecipeRepository: Network responded successfully but no recipe details for $recipeApiId.")
                    }
                    recipeDetail
                } else {
                    println("RecipeRepository:  NETWORK API Error (By ID $recipeApiId): ${response.code()} - ${response.message()}")
                    null
                }
            } catch (e: Exception) {
                println("RecipeRepository: NETWORK Exception (By ID $recipeApiId): ${e.message}")
                e.printStackTrace()
                null
            }
        }
    }

    // --- CONVENIENCE METHOD: Get random recipe, fetch from network AND cache it ---
    suspend fun fetchAndCacheRandomRecipe(): RecipeDetail? {
        println("RecipeRepository: Attempting to fetch a new random recipe from network AND cache it...")
        val newRecipe = getRandomRecipeFromNetwork()
        if (newRecipe != null) {
            println("RecipeRepository: New random recipe ${newRecipe.id} fetched. Caching it...")
            cacheRecipe(newRecipe) // Cache the freshly fetched recipe
        } else {
            println("RecipeRepository: Failed to fetch new random recipe from network. Nothing to cache.")
        }
        return newRecipe
    }

    // Get recipe by ID, try cache first, then network, then cache network result ---
    suspend fun getRecipeById(recipeApiId: Int): RecipeDetail? {
        println("RecipeRepository: Getting recipe by ID $recipeApiId (try cache, then network)...")
        // 1. Try to get from cache
        var recipe = getCachedRecipeByApiId(recipeApiId)

        if (recipe != null) {
            println("RecipeRepository: Recipe $recipeApiId found in CACHE. Returning cached version.")
            return recipe
        }

        // 2. If not in cache, fetch from network
        println("RecipeRepository: Recipe $recipeApiId not in cache. Fetching from NETWORK...")
        recipe = getRecipeDetailsByIdFromNetwork(recipeApiId)

        if (recipe != null) {
            println("RecipeRepository: Recipe $recipeApiId fetched from NETWORK. Caching it...")
            // 3. If fetched from network successfully, cache it
            cacheRecipe(recipe)
        } else {
            println("RecipeRepository: Failed to fetch recipe $recipeApiId from NETWORK.")
        }
        return recipe
    }

    suspend fun getRecipeByIdFromNetworkAndCache(recipeApiId: Int): RecipeDetail? {
        println("RecipeRepository: Fetching recipe $recipeApiId from NETWORK and caching it...")
        val recipe = getRecipeDetailsByIdFromNetwork(recipeApiId)
        if (recipe != null) {
            cacheRecipe(recipe)
            println("RecipeRepository: Successfully fetched and cached recipe $recipeApiId.")
        } else {
            println("RecipeRepository: Failed to fetch recipe $recipeApiId from network.")
        }
        return recipe
    }
    suspend fun getRecipeDetailById(recipeId: Int): RecipeDetail? {
        return withContext(Dispatchers.IO) {
            try {
                println("RecipeRepository: Attempting to fetch recipe by ID: $recipeId")
                val response = apiService.getRecipeInformation(
                    recipeId = recipeId,
                    apiKey = SpoonacularApiService.API_KEY
                )
                if (response.isSuccessful) {
                    response.body()
                } else {
                    println("RecipeRepository: API Error (By ID): ${response.code()} - ${response.message()}")
                    null
                }
            } catch (e: Exception) {
                println("RecipeRepository: Network Exception (By ID): ${e.message}")
                e.printStackTrace()
                null
            }
        }
    }
    suspend fun getRecipeSuggestions(query: String): List<String> {
        // Return a list of strings (recipe titles) for suggestions
        return withContext(Dispatchers.IO) {
            if (query.isBlank()) {
                return@withContext emptyList()
            }
            try {
                println("RecipeRepository: Attempting to fetch suggestions for '$query' from NETWORK...")
                val response = apiService.autocompleteRecipeSearch(
                    apiKey = SpoonacularApiService.API_KEY, // Make sure API_KEY is accessible
                    query = query,
                    number = 7 // Request a decent number of suggestions
                )
                println("RecipeRepository: Suggestions NETWORK API Response Code: ${response.code()}, Successful: ${response.isSuccessful}")

                if (response.isSuccessful) {
                    val suggestions = response.body()?.map { it.title } ?: emptyList()
                    if (suggestions.isNotEmpty()) {
                        println("RecipeRepository: Suggestions for '$query' fetched: $suggestions")
                    } else {
                        println("RecipeRepository: No suggestions found for '$query' from network.")
                    }
                    suggestions
                } else {
                    println("RecipeRepository: NETWORK API Error (Suggestions): ${response.code()} - ${response.message()}")
                    emptyList()
                }
            } catch (e: Exception) {
                println("RecipeRepository: Exception during NETWORK API call (Suggestions): ${e.message}")
                e.printStackTrace()
                emptyList()
            }
        }
    }
    // --- Method to search recipes from NETWORK ---
    // Option 1: Returning a suspend function (simpler to start with)
    suspend fun searchRecipes(query: String): RecipeSearchResponse? {
        return withContext(Dispatchers.IO) {
            if (query.isBlank()) {
                return@withContext RecipeSearchResponse(emptyList(), 0, 0, 0)
            }
            try {
                println("RecipeRepository: Searching recipes for '$query' from NETWORK...")
                val response = apiService.searchRecipesComplex(
                    apiKey = SpoonacularApiService.API_KEY,
                    query = query,
                    number = 20
                )
                println("RecipeRepository: Search NETWORK API Response Code: ${response.code()}, Successful: ${response.isSuccessful}")

                if (response.isSuccessful) {
                    val searchResponse = response.body()
                    if (searchResponse != null && searchResponse.results.isNotEmpty()) {
                        println("RecipeRepository: Found ${searchResponse.totalResults} recipes for '$query'. Returning ${searchResponse.results.size}.")
                    } else {
                        println("RecipeRepository: No recipes found for '$query' or empty response body.")
                    }
                    searchResponse
                } else {
                    println("RecipeRepository: NETWORK API Error (Search): ${response.code()} - ${response.message()}")
                    null
                }
            } catch (e: Exception) {
                println("RecipeRepository: Exception during NETWORK API call (Search): ${e.message}")
                e.printStackTrace()
                null // Or an empty RecipeSearchResponse
            }
        }
    }

    // --- FAVORITES Methods ---

    // --- Get Current User ID from FirebaseAuth ---
    private fun getCurrentUserId(): String? {
        val userId = firebaseAuth.currentUser?.uid
        // Log.d("RecipeRepository", "Current Firebase User ID: $userId") // Optional: for debugging
        return userId
    }

    fun getFirestoreFavoriteRecipes(): Flow<List<FavoriteRecipeDisplayItem>> {
        val userId = getCurrentUserId()
        if (userId == null) {
            Log.w("RecipeRepository", "getFirestoreFavoriteRecipes: User not logged in. Returning empty flow.")
            return flowOf(emptyList())
        }

        val favoritesCollectionRef = firestore.collection("users").document(userId)
            .collection("favorites")

        return callbackFlow<List<FavoriteRecipeDisplayItem>> {
            Log.d("RecipeRepository", "getFirestoreFavoriteRecipes: Setting up Firestore listener for user $userId")

            // Order by "favoritedAt" in descending order to show newest first
            val listenerRegistration = favoritesCollectionRef
                .orderBy("favoritedAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.e("RecipeRepository", "getFirestoreFavoriteRecipes: Listen failed for user $userId", error)
                        trySend(emptyList()) // Emit empty list on error
                        // Consider closing the flow on critical errors: close(error)
                        return@addSnapshotListener
                    }

                    if (snapshots != null) {
                        val favoriteItems = snapshots.documents.mapNotNull { document ->
                            try {
                                val recipeId = document.getLong("id")?.toInt()
                                val title = document.getString("title")
                                if (recipeId != null && title != null) {
                                    FavoriteRecipeDisplayItem(
                                        id = recipeId,
                                        title = title,
                                        imageUrl = document.getString("image"),
                                        favoritedAtTimestamp = document.getTimestamp("favoritedAt")
                                    )
                                } else {
                                    Log.w("RecipeRepository", "Skipping favorite document ${document.id} due to missing id or title.")
                                    null
                                }
                            } catch (e: Exception) {
                                Log.e("RecipeRepository", "Error parsing favorite document ${document.id} for user $userId", e)
                                null // Skip problematic documents
                            }
                        }
                        Log.d("RecipeRepository", "getFirestoreFavoriteRecipes: Fetched ${favoriteItems.size} items for user $userId")
                        trySend(favoriteItems)
                    } else {
                        // This case (snapshots == null && error == null) is unlikely but good to handle.
                        Log.d("RecipeRepository", "getFirestoreFavoriteRecipes: Snapshots were null, no error for user $userId")
                        trySend(emptyList())
                    }
                }

            awaitClose {
                Log.d("RecipeRepository", "getFirestoreFavoriteRecipes: Closing Firestore listener for user $userId")
                listenerRegistration.remove()
            }
        }.flowOn(Dispatchers.IO) // Perform Firestore operations on IO dispatcher
    }

    fun isFavorite(recipeId: Int): Flow<Boolean> {
        val currentUserId = getCurrentUserId() // Get UID for the current device's logged-in user

        // If no user is logged in on this device, it can't be a favorite from Firestore's perspective
        if (currentUserId == null) {
            Log.d("RecipeRepository", "isFavorite: User not logged in on this device. Emitting false for recipe $recipeId.")
            return flow { emit(false) }
        }

        // Path to the specific favorite document in Firestore
        val favoriteDocRef = firestore.collection("users").document(currentUserId)
            .collection("favorites").document(recipeId.toString())

        return callbackFlow {
            Log.d("RecipeRepository", "isFavorite: Setting up Firestore listener for recipe $recipeId, user $currentUserId, path ${favoriteDocRef.path}")

            val listenerRegistration = favoriteDocRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("RecipeRepository", "isFavorite: Listen failed for recipe $recipeId, user $currentUserId.", error)
                    trySend(false) // Emit false on error or handle error appropriately
                    // close(error) // Optionally close the flow with an error
                    return@addSnapshotListener
                }

                val isCurrentlyFavorite = snapshot != null && snapshot.exists()
                Log.d("RecipeRepository", "isFavorite: Snapshot received for recipe $recipeId, user $currentUserId. Exists: $isCurrentlyFavorite")
                trySend(isCurrentlyFavorite) // Send the current favorite status
            }

            awaitClose {
                Log.d("RecipeRepository", "isFavorite: Closing Firestore listener for recipe $recipeId, user $currentUserId.")
                listenerRegistration.remove()
            }
        }.distinctUntilChanged() // Only emit when the value actually changes
            .flowOn(Dispatchers.IO) // Perform Firestore operations on IO dispatcher
    }

    suspend fun addFavorite(recipeDetail: RecipeDetail) {
        val userId = getCurrentUserId() // Get current user's ID

        val favoriteEntity = FavoriteRecipeEntity.fromRecipeDetail(recipeDetail, userId)
        withContext(Dispatchers.IO) { // Keep DB operations on IO dispatcher
            favoriteRecipeDao.addFavorite(favoriteEntity)
            Log.d("RecipeRepository", "Added recipe ${recipeDetail.id} to LOCAL favorites for userId: $userId.")
        }

        // --- Firestore Integration ---
        if (userId != null) {
            withContext(Dispatchers.IO) {
                try {
                    // Path: /users/{userId}/favorites/{recipeId}
                    val favoriteDocRef = firestore.collection("users").document(userId)
                        .collection("favorites").document(recipeDetail.id.toString())

                    val firestoreFavoriteData = mapOf(
                        "id" to recipeDetail.id, // Good to have the ID explicitly
                        "title" to recipeDetail.title,
                        "image" to recipeDetail.image,
                        // Add any other essential fields you want to sync or query by
                        // e.g., "readyInMinutes" to recipeDetail.readyInMinutes,
                        "favoritedAt" to FieldValue.serverTimestamp() // Timestamp of when it was favorited
                    )

                    favoriteDocRef.set(firestoreFavoriteData).await() // .await() suspends until complete
                    Log.d("RecipeRepository", "Successfully added recipe ${recipeDetail.id} to FIRESTORE for userId: $userId.")

                } catch (e: Exception) {
                    Log.e("RecipeRepository", "Failed to add recipe ${recipeDetail.id} to FIRESTORE for userId: $userId.", e)
                }
            }
        } else {
            Log.w("RecipeRepository", "User not logged in. Recipe ${recipeDetail.id} only added to local favorites.")
        }
    }

    suspend fun removeFavorite(recipeId: Int) {
        val userId = getCurrentUserId()

        // 1. Remove from local Room database

        withContext(Dispatchers.IO) {
            favoriteRecipeDao.removeFavoriteById(recipeId, userId)
            Log.d("RecipeRepository", "Removed recipe $recipeId from LOCAL favorites for userId: $userId.")
        }

        // 2. Remove from Firestore
        if (userId != null) {
            withContext(Dispatchers.IO) {
                try {
                    // Path: /users/{userId}/favorites/{recipeId}
                    val favoriteDocRef = firestore.collection("users").document(userId)
                        .collection("favorites").document(recipeId.toString())

                    favoriteDocRef.delete().await() // .await() suspends until the delete operation is complete
                    Log.d("RecipeRepository", "Successfully removed recipe $recipeId from FIRESTORE for userId: $userId.")

                } catch (e: Exception) {
                    Log.e("RecipeRepository", "Failed to remove recipe $recipeId from FIRESTORE for userId: $userId.", e)
                }
            }
        } else {
            Log.w("RecipeRepository", "User not logged in. Cannot remove recipe $recipeId from Firestore. (Already removed locally if present).")
        }
    }

    // --- Daily Planner Notes ---

    private fun getDailyPlannerNotesCollectionRef(userId: String): CollectionReference {
        return firestore.collection("users").document(userId).collection("dailyPlannerNotes")
    }

    fun getDailyNoteForDate(dateEpochDay: Long): Flow<DailyPlannerNote?> {
        val userId = getCurrentUserId()
        if (userId == null) {
            Log.w("RecipeRepository", "User not logged in. Returning empty flow for daily note.")
            return flowOf(
                DailyPlannerNote(
                    id = dateEpochDay.toString(),
                    date = dateEpochDay,
                    notes = ""
                )
            )
        }

        val documentId = dateEpochDay.toString()

        return callbackFlow {
            val docRef = getDailyPlannerNotesCollectionRef(userId).document(documentId)
            val listenerRegistration = docRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("RecipeRepository", "Listen failed for daily note $documentId for user $userId", error)
                    trySend(null) // Or a specific error state / default empty note
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    try {
                        val note = snapshot.toObject(DailyPlannerNote::class.java)?.copy(id = snapshot.id)
                        trySend(note)
                    } catch (e: Exception) {
                        Log.e("RecipeRepository", "Error converting snapshot to DailyPlannerNote for $documentId", e)
                        trySend(DailyPlannerNote(id = documentId, date = dateEpochDay, notes = "Error loading note."))
                    }
                } else {
                    // Document doesn't exist, send a default structure for the UI
                    trySend(DailyPlannerNote(id = documentId, date = dateEpochDay, notes = ""))
                }
            }
            awaitClose {
                Log.d("RecipeRepository", "Closing Firestore listener for daily note $documentId for user $userId")
                listenerRegistration.remove()
            }
        }.flowOn(Dispatchers.IO)
    }

    suspend fun saveDailyNote(dateEpochDay: Long, noteText: String) {
        val userId = getCurrentUserId()
        if (userId == null) {
            Log.w("RecipeRepository", "User not logged in. Cannot save daily note.")
            return
        }

        val documentId = dateEpochDay.toString()
        val docRef = getDailyPlannerNotesCollectionRef(userId).document(documentId)

        withContext(Dispatchers.IO) {
            try {
                firestore.runTransaction { transaction ->
                    val snapshot = transaction.get(docRef)
                    val noteDataMap: Map<String, Any>

                    if (!snapshot.exists()) {
                        // Document does not exist, create it with firstCreatedAt and lastUpdatedAt
                        noteDataMap = mapOf(
                            "date" to dateEpochDay,
                            "notes" to noteText,
                            "firstCreatedAt" to FieldValue.serverTimestamp(), // Set on creation
                            "lastUpdatedAt" to FieldValue.serverTimestamp()  // Also set on creation
                        )
                        transaction.set(docRef, noteDataMap)
                    } else {
                        // Document exists, update notes and lastUpdatedAt
                        noteDataMap = mapOf(
                            "notes" to noteText,
                            "lastUpdatedAt" to FieldValue.serverTimestamp() // Update on modification
                            // "date" and "firstCreatedAt" are not updated here
                        )
                        transaction.update(docRef, noteDataMap)
                    }
                    null // Transaction must return null or a result if needed
                }.await()
                Log.d("RecipeRepository", "Successfully saved daily note for $documentId for user $userId")
            } catch (e: Exception) {
                Log.e("RecipeRepository", "Failed to save daily note for $documentId for user $userId", e)
            }
        }
    }

    // --- Plan meal feature ---

    private fun getPlannedMealsCollectionRef(userId: String): CollectionReference {
        return firestore.collection("users").document(userId).collection("plannedMeals")
    }

    suspend fun addMultiplePlannedMeals(meals: List<PlannedMeal>): List<String> {
        val userId = getCurrentUserId()
        if (userId == null) {
            Log.w("RecipeRepository", "User not logged in. Cannot add multiple planned meals.")
            return emptyList()
        }
        val addedMealIds = mutableListOf<String>()
        withContext(Dispatchers.IO) {
            val collectionRef = getPlannedMealsCollectionRef(userId)
            val batch = firestore.batch()

            meals.forEach { meal ->
                val documentRef = collectionRef.document() // Auto-generate ID for each meal
                val mealToSave = meal.copy(
                    id = documentRef.id,
                    userId = userId
                )
                batch.set(documentRef, mealToSave)
                addedMealIds.add(documentRef.id)
            }
            try {
                batch.commit().await()
                Log.d("RecipeRepository", "Successfully batch-added ${meals.size} planned meals for user $userId.")
            } catch (e: Exception) {
                Log.e("RecipeRepository", "Failed to batch-add planned meals for user $userId.", e)
                addedMealIds.clear() // Clear if batch failed
            }
        }
        return addedMealIds
    }


    fun getPlannedMealsForDateRange(
        startDateEpochDay: Long,
        endDateEpochDay: Long
    ): Flow<List<PlannedMeal>> {
        val userId = getCurrentUserId()
        if (userId == null) {
            Log.w("RecipeRepository", "User not logged in. Returning empty flow for planned meals.")
            return flowOf(emptyList())
        }
        val plannedMealsCollectionRef = getPlannedMealsCollectionRef(userId)

        return callbackFlow<List<PlannedMeal>> {
            Log.d("RecipeRepository", "Setting up Firestore listener for planned meals between $startDateEpochDay and $endDateEpochDay for user $userId")

            val listenerRegistration = plannedMealsCollectionRef
                .whereGreaterThanOrEqualTo("date", startDateEpochDay)
                .whereLessThanOrEqualTo("date", endDateEpochDay)
                .orderBy("date", Query.Direction.ASCENDING) // Order by date
                // .orderBy("addedAt", Query.Direction.ASCENDING) // Optional: secondary sort by time added
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.e("RecipeRepository", "Listen failed for planned meals user $userId", error)
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    if (snapshots != null) {
                        val plannedMeals = snapshots.toObjects(PlannedMeal::class.java)
                        Log.d("RecipeRepository", "Fetched ${plannedMeals.size} planned meals for user $userId in range.")
                        trySend(plannedMeals)
                    } else {
                        trySend(emptyList())
                    }
                }
            awaitClose {
                Log.d("RecipeRepository", "Closing Firestore listener for planned meals user $userId")
                listenerRegistration.remove()
            }
        }.flowOn(Dispatchers.IO)
    }

    suspend fun updatePlannedMealType(mealId: String, newMealType: String?) {
        val userId = getCurrentUserId()
        if (userId == null) {
            Log.w("RecipeRepository", "User not logged in. Cannot update meal type.")
            return
        }
        withContext(Dispatchers.IO) {
            try {
                getPlannedMealsCollectionRef(userId).document(mealId)
                    .update("mealType", newMealType) // Pass null to remove the field or set to specific value
                    .await()
                Log.d("RecipeRepository", "Successfully updated meal type for $mealId for user $userId")
            } catch (e: Exception) {
                Log.e("RecipeRepository", "Failed to update meal type for $mealId for user $userId", e)
            }
        }
    }

    suspend fun removePlannedMeal(mealId: String) {
        val userId = getCurrentUserId()
        if (userId == null) {
            Log.w("RecipeRepository", "User not logged in. Cannot remove planned meal.")
            return
        }
        withContext(Dispatchers.IO) {
            try {
                getPlannedMealsCollectionRef(userId).document(mealId).delete().await()
                Log.d("RecipeRepository", "Successfully removed planned meal $mealId from Firestore for user $userId")
            } catch (e: Exception) {
                Log.e("RecipeRepository", "Failed to remove planned meal $mealId from Firestore for user $userId.", e)
            }
        }
    }
}