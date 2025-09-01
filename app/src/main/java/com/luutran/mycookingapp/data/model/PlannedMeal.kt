package com.luutran.mycookingapp.data.model


import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class PlannedMeal(
    val id: String = "",
    val userId: String = "",
    val recipeId: Int = 0,
    val recipeTitle: String = "",
    val recipeImageUrl: String? = null,
    val date: Long = 0L,
    var mealType: String? = null, // e.g. "Breakfast", "Lunch", "Dinner", "Snack" - user can assign this
    @ServerTimestamp
    val addedAt: Date? = null,
)

data class DailyMealPlan(
    val date: Long,
    val meals: List<PlannedMeal>
)