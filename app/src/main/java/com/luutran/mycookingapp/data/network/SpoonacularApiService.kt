package com.luutran.mycookingapp.data.network

import com.luutran.mycookingapp.data.model.RecipeDetail
import com.luutran.mycookingapp.data.model.RecipeSearchResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Path

data class RecipeAutocompleteResult(
    val id: Int,
    val title: String,
    val imageType: String?
)

interface SpoonacularApiService {
    companion object {
        const val BASE_URL = "https://api.spoonacular.com/"
        const val API_KEY = "your_api_key_here"
    }

    // --- Get Random Recipes ---
    @GET("recipes/random")
    suspend fun getRandomRecipes(
        @Query("apiKey") apiKey: String = API_KEY,
        @Query("number") number: Int = 1,
        @Query("tags") tags: String? = null
    ): Response<RandomRecipeResponse>

    // --- Complex Recipe Search ---
    @GET("recipes/complexSearch")
    suspend fun searchRecipesComplex(
        @Query("apiKey") apiKey: String = API_KEY,
        @Query("query") query: String?,
        @Query("number") number: Int = 20,
        @Query("offset") offset: Int? = null,

        // --- FILTER AND SORT PARAMETERS ---
        @Query("sort") sort: String? = null,
        @Query("sortDirection") sortDirection: String? = null,
        @Query("diet") diet: String? = null,
        @Query("intolerances") intolerances: String? = null,
        @Query("cuisine") cuisine: String? = null,
        @Query("type") type: String? = null,
        @Query("includeIngredients") includeIngredients: String? = null,
        @Query("excludeIngredients") excludeIngredients: String? = null,
        @Query("maxReadyTime") maxReadyTime: Int? = null,

        @Query("addRecipeInformation") addRecipeInformation: Boolean = false,
        @Query("fillIngredients") fillIngredients: Boolean = false
    ): Response<RecipeSearchResponse>

    // --- Autocomplete Recipe Search ---
    @GET("recipes/autocomplete")
    suspend fun autocompleteRecipeSearch(
        @Query("apiKey") apiKey: String = API_KEY,
        @Query("query") query: String,
        @Query("number") number: Int = 5
    ): Response<List<RecipeAutocompleteResult>> // Uses the RecipeAutocompleteResult at the top

    // --- Get Recipe Information by ID ---
    @GET("recipes/{id}/information")
    suspend fun getRecipeInformation(
        @Path("id") recipeId: Int,
        @Query("apiKey") apiKey: String = API_KEY,
        @Query("includeNutrition") includeNutrition: Boolean = false
    ): Response<RecipeDetail>

    // Simplified Search by Cuisine
    @GET("recipes/complexSearch")
    suspend fun searchRecipesByCuisine(
        @Query("apiKey") apiKey: String = API_KEY,
        @Query("cuisine") cuisine: String,
        @Query("number") number: Int = 5,
        @Query("offset") offset: Int = 0,
        @Query("addRecipeInformation") addRecipeInformation: Boolean = false, // Ensure summaries
        @Query("fillIngredients") fillIngredients: Boolean = false
    ): Response<RecipeSearchResponse>

    // Simplified Search by Diet
    @GET("recipes/complexSearch")
    suspend fun searchRecipesByDiet(
        @Query("apiKey") apiKey: String = API_KEY,
        @Query("diet") diet: String, // Spoonacular uses 'diet' for both diets and intolerances
        @Query("number") number: Int = 5,
        @Query("offset") offset: Int = 0,
        @Query("addRecipeInformation") addRecipeInformation: Boolean = false, // Ensure summaries
        @Query("fillIngredients") fillIngredients: Boolean = false
    ): Response<RecipeSearchResponse>
}

data class RandomRecipeResponse(
    val recipes: List<RecipeDetail> // Or RecipeSummary if the random endpoint is simpler
)