package com.luutran.mycookingapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.luutran.mycookingapp.data.local.entity.RecipeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: RecipeEntity)

    @Query("SELECT * FROM recipes WHERE recipeApiId = :apiId")
    suspend fun getRecipeById(apiId: Int): RecipeEntity?

    @Query("DELETE FROM recipes WHERE recipeApiId = :apiId")
    suspend fun deleteRecipeByApiId(apiId: Int)

    @Query("DELETE FROM recipes")
    suspend fun clearAllRecipes()
}