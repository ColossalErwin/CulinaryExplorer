package com.luutran.mycookingapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.luutran.mycookingapp.data.local.entity.FavoriteRecipeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteRecipeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favoriteRecipe: FavoriteRecipeEntity)

    @Delete
    suspend fun removeFavorite(favoriteRecipe: FavoriteRecipeEntity)

    @Query("DELETE FROM favorite_recipes WHERE id = :recipeId AND ((:userId IS NULL AND userId IS NULL) OR userId = :userId)")
    suspend fun removeFavoriteById(recipeId: Int, userId: String?)

    @Query("SELECT * FROM favorite_recipes WHERE ((:userId IS NULL AND userId IS NULL) OR userId = :userId) ORDER BY addedDate DESC")
    fun getFavoriteRecipes(userId: String?): Flow<List<FavoriteRecipeEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_recipes WHERE id = :recipeId AND ((:userId IS NULL AND userId IS NULL) OR userId = :userId) LIMIT 1)")
    fun isFavorite(recipeId: Int, userId: String?): Flow<Boolean>

    @Query("SELECT * FROM favorite_recipes WHERE userId = :userId")
    suspend fun getAllFavoritesForUser(userId: String): List<FavoriteRecipeEntity>

    @Query("DELETE FROM favorite_recipes WHERE userId = :userId")
    suspend fun clearUserFavorites(userId: String)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(favoriteRecipes: List<FavoriteRecipeEntity>)
}