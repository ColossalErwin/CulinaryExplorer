package com.luutran.mycookingapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.luutran.mycookingapp.data.local.dao.FavoriteRecipeDao
import com.luutran.mycookingapp.data.local.dao.RecipeDao
import com.luutran.mycookingapp.data.local.entity.FavoriteRecipeEntity
import com.luutran.mycookingapp.data.local.entity.RecipeEntity
import com.luutran.mycookingapp.data.local.dao.SuggestedRecipeDao
import com.luutran.mycookingapp.data.local.entity.SuggestedRecipeSummaryEntity
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        RecipeEntity::class,
        FavoriteRecipeEntity::class,
        SuggestedRecipeSummaryEntity::class
    ],
    version = 3,
    exportSchema = true
)

abstract class AppDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao
    abstract fun favoriteRecipeDao(): FavoriteRecipeDao
    abstract fun suggestedRecipeDao(): SuggestedRecipeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "my_cooking_app_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // Migration from version 1 to 2: Create the favorite_recipes table
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `favorite_recipes` (" +
                            "`id` INTEGER NOT NULL, " +
                            "`title` TEXT, " +
                            "`remoteImageUrl` TEXT, " +
                            "`readyInMinutes` INTEGER, " +
                            "`servings` INTEGER, " +
                            "`sourceName` TEXT, " +
                            "`addedDate` INTEGER NOT NULL, " +
                            "`userId` TEXT, " + // TEXT for Firebase UID
                            "PRIMARY KEY(`id`))"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `suggested_recipe_summaries` (" +
                            "`featuredRecipeId` INTEGER NOT NULL, " +
                            "`categoryType` TEXT NOT NULL, " +
                            "`categoryValue` TEXT NOT NULL, " +
                            "`suggestedRecipeApiId` INTEGER NOT NULL, " +
                            "`title` TEXT NOT NULL, " +
                            "`imageUrl` TEXT, " +
                            "`imageType` TEXT, " +
                            "`readyInMinutes` INTEGER, " +
                            "`listOrder` INTEGER NOT NULL, " +
                            "`cachedAtTimestamp` INTEGER NOT NULL, " +
                            "PRIMARY KEY(`featuredRecipeId`, `categoryType`, `categoryValue`, `suggestedRecipeApiId`)" +
                            ")"
                )
                // Add index for faster lookups by the primary components of the suggestion list
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS " +
                            "`index_suggested_recipe_summaries_key` ON `suggested_recipe_summaries` " +
                            "(`featuredRecipeId`, `categoryType`, `categoryValue`)"
                )
            }
        }
    }
}