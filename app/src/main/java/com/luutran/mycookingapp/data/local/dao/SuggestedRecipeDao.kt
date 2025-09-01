package com.luutran.mycookingapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.luutran.mycookingapp.data.local.entity.SuggestedRecipeSummaryEntity
import com.luutran.mycookingapp.ui.home.SuggestionCategoryType

@Dao
interface SuggestedRecipeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuggestedRecipes(recipes: List<SuggestedRecipeSummaryEntity>)

    // Get all suggested recipes for a specific featured recipe and category
    @Query(
        "SELECT * FROM suggested_recipe_summaries " +
                "WHERE featuredRecipeId = :featuredRecipeId " +
                "AND categoryType = :categoryType " +
                "AND categoryValue = :categoryValue " +
                "ORDER BY listOrder ASC" // Ensure recipes are returned in their original fetched order
    )
    suspend fun getSuggestions(
        featuredRecipeId: Int,
        categoryType: SuggestionCategoryType,
        categoryValue: String
    ): List<SuggestedRecipeSummaryEntity>

    // Clear suggestions for a specific featured recipe (e.g., when it changes)
    @Query("DELETE FROM suggested_recipe_summaries WHERE featuredRecipeId = :featuredRecipeId")
    suspend fun clearSuggestionsForFeaturedRecipe(featuredRecipeId: Int)

    // Clear suggestions for a specific category of a featured recipe
    @Query(
        "DELETE FROM suggested_recipe_summaries " +
                "WHERE featuredRecipeId = :featuredRecipeId " +
                "AND categoryType = :categoryType " +
                "AND categoryValue = :categoryValue"
    )
    suspend fun clearSuggestionsForCategory(
        featuredRecipeId: Int,
        categoryType: SuggestionCategoryType,
        categoryValue: String
    )

    // Clear all suggestions (e.g. for full cache clear)
    @Query("DELETE FROM suggested_recipe_summaries")
    suspend fun clearAllSuggestions()

    @Query(
        "SELECT cachedAtTimestamp FROM suggested_recipe_summaries " +
                "WHERE featuredRecipeId = :featuredRecipeId " +
                "AND categoryType = :categoryType " +
                "AND categoryValue = :categoryValue " +
                "ORDER BY cachedAtTimestamp DESC LIMIT 1"
    )
    suspend fun getCacheTimestampForSuggestions(
        featuredRecipeId: Int,
        categoryType: SuggestionCategoryType,
        categoryValue: String
    ): Long?
}