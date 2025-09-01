package com.luutran.mycookingapp.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class DailyPlannerNote(
    val id: String = "",
    val date: Long = 0L,
    val notes: String = "",
    @ServerTimestamp
    val firstCreatedAt: Timestamp? = null,
    @ServerTimestamp
    val lastUpdatedAt: Timestamp? = null
)