package com.luutran.mycookingapp.data.model // Assuming this is your package

import com.google.gson.annotations.SerializedName

data class RecipeSummary(
    @SerializedName("id")
    val id: Int,
    @SerializedName("title")
    val title: String,
    @SerializedName("image")
    val image: String?,
    @SerializedName("imageType")
    val imageType: String?,
    @SerializedName("readyInMinutes")
    val readyInMinutes: Int? = null,
)

data class RecipeDetail(
    @SerializedName("id")
    val id: Int,
    @SerializedName("title")
    val title: String?,

    @SerializedName("image")
    val image: String?,
    @SerializedName("imageType")
    val imageType: String?,

    @SerializedName("servings")
    val servings: Int?,
    @SerializedName("readyInMinutes")
    val readyInMinutes: Int?,
    @SerializedName("sourceName")
    val sourceName: String?,
    @SerializedName("sourceUrl")
    val sourceUrl: String?,

    @SerializedName("extendedIngredients")
    val extendedIngredients: List<Ingredient>?,

    @SerializedName("analyzedInstructions")
    val analyzedInstructions: List<InstructionStepParent>?,

    @SerializedName("summary")
    val summary: String?,

    @SerializedName("dishTypes")
    val dishTypes: List<String>?,

    @SerializedName("diets")
    val diets: List<String>?,

    @SerializedName("cuisines")
    val cuisines: List<String>? = null,

    @SerializedName("aggregateLikes")
    val aggregateLikes: Int? = null,

    @SerializedName("healthScore")
    val healthScore: Double? = null,

    @SerializedName("spoonacularScore")
    val spoonacularScore: Double? = null,

    @SerializedName("pricePerServing")
    val pricePerServing: Double? = null,

    @SerializedName("winePairing")
    val winePairing: WinePairing? = null,

    @SerializedName("license")
    val license: String? = null,

    @SerializedName("spoonacularSourceUrl")
    val spoonacularSourceUrl: String? = null
)

data class Ingredient(
    @SerializedName("id")
    val id: Int?,
    @SerializedName("aisle")
    val aisle: String?,
    @SerializedName("image")
    val image: String?,
    @SerializedName("consistency")
    val consistency: String?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("nameClean")
    val nameClean: String?,
    @SerializedName("original")
    val original: String?,
    @SerializedName("originalName")
    val originalName: String?,
    @SerializedName("amount")
    val amount: Double?,
    @SerializedName("unit")
    val unit: String?,
    @SerializedName("meta")
    val meta: List<String>? = null,
    @SerializedName("measures")
    val measures: Measures?
)

data class Measures(
    @SerializedName("us")
    val us: MeasureUnit?,
    @SerializedName("metric")
    val metric: MeasureUnit?
)

data class MeasureUnit(
    @SerializedName("amount")
    val amount: Double?,
    @SerializedName("unitShort")
    val unitShort: String?,
    @SerializedName("unitLong")
    val unitLong: String?
)

data class InstructionStepParent(
    @SerializedName("name")
    val name: String?,
    @SerializedName("steps")
    val steps: List<InstructionStepDetail>?
)

data class InstructionStepDetail(
    @SerializedName("number")
    val number: Int,
    @SerializedName("step")
    val step: String?, // Changed to nullable
    @SerializedName("ingredients")
    val ingredients: List<StepIngredientOrEquipment>?,
    @SerializedName("equipment")
    val equipment: List<StepIngredientOrEquipment>?
)

data class StepIngredientOrEquipment(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String, // Assuming API guarantees name for these refs
    @SerializedName("localizedName")
    val localizedName: String, // Assuming API guarantees
    @SerializedName("image")
    val image: String?
)

// There's no WinePairing for the api, just a placeholder
data class WinePairing(
    @SerializedName("pairedWines")
    val pairedWines: List<String>? = null,
    @SerializedName("pairingText")
    val pairingText: String? = null,
    @SerializedName("productMatches")
    val productMatches: List<ProductMatch>? = null
)

data class ProductMatch(
    @SerializedName("id")
    val id: Int,
    @SerializedName("title")
    val title: String?,
    @SerializedName("description")
    val description: String?,
    @SerializedName("price")
    val price: String?,
    @SerializedName("imageUrl")
    val imageUrl: String?,
    @SerializedName("averageRating")
    val averageRating: Double?,
    @SerializedName("ratingCount")
    val ratingCount: Double?,
    @SerializedName("score")
    val score: Double?,
    @SerializedName("link")
    val link: String?
)

// Wrapper for search results
data class RecipeSearchResponse(
    @SerializedName("results")
    val results: List<RecipeSummary>,
    @SerializedName("offset")
    val offset: Int,
    @SerializedName("number")
    val number: Int,
    @SerializedName("totalResults")
    val totalResults: Int
)