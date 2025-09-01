package com.luutran.mycookingapp.ui.favorites

data class FavoriteRecipeDisplayItem(
    val id: Int,
    val title: String,
    val imageUrl: String?,
    val favoritedAtTimestamp: com.google.firebase.Timestamp? = null
)