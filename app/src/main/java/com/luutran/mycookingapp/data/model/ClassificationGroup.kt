package com.luutran.mycookingapp.data.model

import androidx.compose.ui.graphics.Color

data class ClassificationGroup(
    val id: String,
    val title: String, // e.g., "Cuisine", "Intolerance"
    val descriptionExamples: String, // e.g., "Mexican, Chinese, Mediterranean..."
    val backgroundColor: Color,
    val navigationTarget: ClassificationNavigationTarget
)

// To determine what to do when a classification group card is clicked
sealed interface ClassificationNavigationTarget {
    data class CuisineList(val availableCuisines: List<String> = emptyList()) : ClassificationNavigationTarget // You might pre-populate this or fetch later
    data class IntoleranceList(val availableIntolerances: List<String> = emptyList()) : ClassificationNavigationTarget
    data class DietList(val availableDiets: List<String> = emptyList()) : ClassificationNavigationTarget
    data class MealTypeList(val availableMealTypes: List<String> = emptyList()) : ClassificationNavigationTarget
}

object ClassificationOptions {
    val cuisines = listOf("African", "Asian", "American", "British", "Cajun", "Caribbean", "Chinese", "Eastern European", "European", "French", "German", "Greek", "Indian", "Irish", "Italian", "Japanese", "Jewish", "Korean", "Latin American", "Mediterranean", "Mexican", "Middle Eastern", "Nordic", "Southern", "Spanish", "Thai", "Vietnamese")
    val intolerances = listOf("Dairy", "Egg", "Gluten", "Grain", "Peanut", "Seafood", "Sesame", "Shellfish", "Soy", "Sulfite", "Tree Nut", "Wheat")
    val diets = listOf("Gluten Free", "Ketogenic", "Vegetarian", "Lacto-Vegetarian", "Ovo-Vegetarian", "Vegan", "Pescetarian", "Paleo", "Primal", "Low FODMAP", "Whole30")
    val mealTypes = listOf("Main course", "Side dish", "Dessert", "Appetizer", "Salad", "Bread", "Breakfast", "Soup", "Beverage", "Sauce", "Marinade", "Fingerfood", "Snack", "Drink")
}