package com.luutran.mycookingapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.luutran.mycookingapp.data.local.converter.IngredientListConverter
import com.luutran.mycookingapp.data.local.converter.InstructionStepParentListConverter
import com.luutran.mycookingapp.data.local.converter.StringListConverter
import com.luutran.mycookingapp.data.local.converter.WinePairingConverter
import com.luutran.mycookingapp.data.model.Ingredient
import com.luutran.mycookingapp.data.model.InstructionStepParent
import com.luutran.mycookingapp.data.model.WinePairing

@Entity(tableName = "recipes")
@TypeConverters(
    StringListConverter::class,
    IngredientListConverter::class,
    InstructionStepParentListConverter::class, // For analyzedInstructions
    WinePairingConverter::class // For winePairing
)
data class RecipeEntity(
    @PrimaryKey
    val recipeApiId: Int, // Maps to RecipeDetail.id

    val title: String?, // Maps to RecipeDetail.title (already nullable)
    val imageUrl: String?, // Maps to RecipeDetail.image
    // val imageType: String?, // Often not needed for caching if imageUrl is absolute

    val servings: Int?,
    val readyInMinutes: Int?,
    val sourceName: String?,
    val sourceUrl: String?,

    // Using TypeConverters for these complex types
    val extendedIngredients: List<Ingredient>?,
    val analyzedInstructions: List<InstructionStepParent>?,
    val dishTypes: List<String>?,
    val diets: List<String>?,
    val cuisines: List<String>?,

    val summary: String?, // Often HTML content

    // Optional fields from RecipeDetail you might want to cache:
    val aggregateLikes: Int? = null,
    val healthScore: Double? = null,
    val spoonacularScore: Double? = null,
    val pricePerServing: Double? = null,
    val winePairing: WinePairing? = null,
    val license: String? = null,
    val spoonacularSourceUrl: String? = null,
    // val nutrition: NutritionInfo?, // If you decide to cache this

    val lastUpdated: Long = System.currentTimeMillis()
)