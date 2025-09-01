package com.luutran.mycookingapp.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class DishMemory(
    @DocumentId val id: String = "",
    @ServerTimestamp
    val timestamp: Timestamp? = null,
    val rating: Int = 0,
    val notes: String = "",
    val imageUrls: List<String> = emptyList()
) {
    constructor() : this(id = "", timestamp = null, rating = 0, notes = "", imageUrls = emptyList())
}