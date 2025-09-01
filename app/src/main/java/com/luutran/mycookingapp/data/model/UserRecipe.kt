package com.luutran.mycookingapp.data.model


import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.Timestamp

data class UserRecipe(
    @DocumentId
    val id: String = "", // Firestore document ID, auto-generated

    var title: String = "",
    var description: String? = null,

    var ingredients: List<String> = emptyList(),
    var instructions: List<String> = emptyList(),

    var imageUrls: List<String> = emptyList(), // User can upload multiple images

    var prepTimeMinutes: Int? = null,
    var cookTimeMinutes: Int? = null,
    var totalTimeMinutes: Int? = null, // Can be user-entered or calculated

    var servings: String? = null,

    var category: String? = null, // User selects from a predefined list (e.g., "Dinner") from dropdown menu, haven't implemented
    var cuisine: String? = null,  // User selects from a predefined list (e.g., "Italian") from dropdown menu, haven't implemented
    var tags: List<String> = emptyList(), // User enters free-form tags

    var notes: String? = null,

    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    var updatedAt: Timestamp? = null,

    var dishTypes: List<String>? = null, // User selects multiple from predefined (e.g., "main course") from dropdown menu, haven't implemented
    var diets: List<String>? = null      // User selects multiple from predefined (e.g., "vegetarian") from dropdown menu, haven't implemented
)