package com.luutran.mycookingapp.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class CookedDishEntry(
    @DocumentId val recipeId: String = "",
    val title: String = "",
    val imageUrl: String? = null,
    var timesCooked: Int = 0,
    @ServerTimestamp val firstAddedAt: Timestamp? = null,
    @ServerTimestamp var lastCookedAt: Timestamp? = null
) {
    // No-argument constructor for Firestore deserialization
    constructor() : this("", "", null, 0, null, null)
}
