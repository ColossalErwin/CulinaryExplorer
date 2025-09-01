package com.luutran.mycookingapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.luutran.mycookingapp.data.model.RecipeSummary // Assuming RecipeSummary is in this path
import com.luutran.mycookingapp.ui.home.SuggestionCategoryType

// Define a primary key for the table itself, can be auto-generated
@Entity(
    tableName = "suggested_recipe_summaries",
    primaryKeys = ["featuredRecipeId", "categoryType", "categoryValue", "suggestedRecipeApiId"],
    indices = [Index(value = ["featuredRecipeId", "categoryType", "categoryValue"])]
)
data class SuggestedRecipeSummaryEntity(
    val featuredRecipeId: Int, // Foreign key to the RecipeEntity's recipeApiId (conceptually)
    val categoryType: SuggestionCategoryType,
    val categoryValue: String, // e.g., "Italian,French" or "gluten free"

    val suggestedRecipeApiId: Int, // The ID of the recipe in this suggestion list (from RecipeSummary.id)
    val title: String,
    val imageUrl: String?,
    val imageType: String?,
    val readyInMinutes: Int?,
    // Add any other fields from RecipeSummary you want to store directly

    val listOrder: Int, // To maintain the order of recipes within this specific suggestion list
    val cachedAtTimestamp: Long = System.currentTimeMillis()
)

// Optional: Mapper from network RecipeSummary to this entity
fun RecipeSummary.toSuggestedRecipeSummaryEntity(
    featuredRecipeId: Int,
    categoryType: SuggestionCategoryType,
    categoryValue: String,
    listOrder: Int
): SuggestedRecipeSummaryEntity {
    return SuggestedRecipeSummaryEntity(
        featuredRecipeId = featuredRecipeId,
        categoryType = categoryType,
        categoryValue = categoryValue,
        suggestedRecipeApiId = this.id,
        title = this.title,
        imageUrl = this.image,
        imageType = this.imageType,
        readyInMinutes = this.readyInMinutes,
        listOrder = listOrder,
        cachedAtTimestamp = System.currentTimeMillis()
    )
}

// Optional: Mapper from this entity back to network RecipeSummary
fun SuggestedRecipeSummaryEntity.toRecipeSummary(): RecipeSummary {
    return RecipeSummary(
        id = this.suggestedRecipeApiId,
        title = this.title,
        image = this.imageUrl,
        imageType = this.imageType,
        readyInMinutes = this.readyInMinutes
    )
}