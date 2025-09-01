package com.luutran.mycookingapp.data.utils // Or a more specific sub-package like com.luutran.mycookingapp.data.mapper

import com.luutran.mycookingapp.data.local.entity.RecipeEntity
import com.luutran.mycookingapp.data.model.RecipeDetail


// --- RecipeDetail (Network Model) to RecipeEntity (Database Model) ---
fun RecipeDetail.toRecipeEntity(): RecipeEntity {
    return RecipeEntity(
        recipeApiId = this.id,
        title = this.title,
        imageUrl = this.image,
        servings = this.servings,
        readyInMinutes = this.readyInMinutes,
        sourceName = this.sourceName,
        sourceUrl = this.sourceUrl,
        extendedIngredients = this.extendedIngredients,
        analyzedInstructions = this.analyzedInstructions,
        summary = this.summary,
        dishTypes = this.dishTypes,
        diets = this.diets,
        cuisines = this.cuisines,
        aggregateLikes = this.aggregateLikes,
        healthScore = this.healthScore,
        spoonacularScore = this.spoonacularScore,
        pricePerServing = this.pricePerServing,
        winePairing = this.winePairing,
        license = this.license,
        spoonacularSourceUrl = this.spoonacularSourceUrl,
        lastUpdated = System.currentTimeMillis()
    )
}

// --- RecipeEntity (Database Model) to RecipeDetail (UI/Domain Model) ---
fun RecipeEntity.toRecipeDetail(): RecipeDetail {
    return RecipeDetail(
        id = this.recipeApiId,
        title = this.title,
        image = this.imageUrl,
        imageType = null,
        servings = this.servings,
        readyInMinutes = this.readyInMinutes,
        sourceName = this.sourceName,
        sourceUrl = this.sourceUrl,
        extendedIngredients = this.extendedIngredients,
        analyzedInstructions = this.analyzedInstructions,
        summary = this.summary,
        dishTypes = this.dishTypes,
        diets = this.diets,
        cuisines = this.cuisines,
        aggregateLikes = this.aggregateLikes,
        healthScore = this.healthScore,
        spoonacularScore = this.spoonacularScore,
        pricePerServing = this.pricePerServing,
        winePairing = this.winePairing,
        license = this.license,
        spoonacularSourceUrl = this.spoonacularSourceUrl
    )
}