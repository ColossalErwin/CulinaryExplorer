package com.luutran.mycookingapp.ui.favorites

import com.google.firebase.Timestamp

enum class FavoriteSortOption(val displayName: String) {
    DATE_FAVORITED_NEWEST("Date Favorited (Newest)"),
    DATE_FAVORITED_OLDEST("Date Favorited (Oldest)"),
    NAME_ASCENDING("Name (A-Z)"),
    NAME_DESCENDING("Name (Z-A)");

    companion object {
        fun defaultSort() = DATE_FAVORITED_NEWEST
    }
}

internal val distantPastTimestamp =
    Timestamp(-62135596800L, 0) // Represents 0001-01-01T00:00:00Z
// A very far future date, but valid for Timestamp
internal val distantFutureTimestamp =
    Timestamp(253402300799L, 999999999) // Approx Year 9999

/**
 * Applies the specified sorting option to a list of FavoriteRecipeDisplayItem.
 */
fun List<FavoriteRecipeDisplayItem>.applySort(option: FavoriteSortOption): List<FavoriteRecipeDisplayItem> {
    return when (option) {
        FavoriteSortOption.DATE_FAVORITED_NEWEST ->
            sortedWith(compareByDescending { it.favoritedAtTimestamp ?: distantPastTimestamp })
        FavoriteSortOption.DATE_FAVORITED_OLDEST ->
            sortedWith(compareBy { it.favoritedAtTimestamp ?: distantFutureTimestamp })
        FavoriteSortOption.NAME_ASCENDING ->
            sortedBy { it.title.lowercase() }
        FavoriteSortOption.NAME_DESCENDING ->
            sortedByDescending { it.title.lowercase() }
    }
}