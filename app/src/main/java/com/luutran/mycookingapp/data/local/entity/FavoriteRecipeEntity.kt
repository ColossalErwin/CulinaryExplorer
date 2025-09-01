package com.luutran.mycookingapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.luutran.mycookingapp.data.model.RecipeDetail // Your existing RecipeDetail model

@Entity(tableName = "favorite_recipes")
data class FavoriteRecipeEntity(
    @PrimaryKey
    val id: Int, // Recipe ID from the API, serves as the primary key
    val title: String?,
    val remoteImageUrl: String?, // URL for the image, Coil will handle caching
    val readyInMinutes: Int?,
    val servings: Int?,
    val sourceName: String?,
    val addedDate: Long, // Timestamp when the recipe was added to favorites
    val userId: String? // To associate with a Firebase user, nullable if favorites can be local-only before login
) {
    // Companion object or extension function to map from your RecipeDetail to FavoriteRecipeEntity
    companion object {
        fun fromRecipeDetail(recipeDetail: RecipeDetail, currentUserId: String?): FavoriteRecipeEntity {
            return FavoriteRecipeEntity(
                id = recipeDetail.id,
                title = recipeDetail.title,
                remoteImageUrl = recipeDetail.image,
                readyInMinutes = recipeDetail.readyInMinutes,
                servings = recipeDetail.servings,
                sourceName = recipeDetail.sourceName,
                addedDate = System.currentTimeMillis(),
                userId = currentUserId
            )
        }
    }
}