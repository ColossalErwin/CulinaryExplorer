package com.luutran.mycookingapp.ui.dishmemories

import com.google.firebase.Timestamp
import com.luutran.mycookingapp.data.model.DishMemory // Make sure this path is correct

enum class DishMemorySortOption(val displayName: String) {
    DATE_COOKED_NEWEST("Date Cooked (Newest First)"),
    DATE_COOKED_OLDEST("Date Cooked (Oldest First)"),
    RATING_HIGHEST("Rating (Highest First)"),
    RATING_LOWEST("Rating (Lowest First)");
    // Add more options here if needed in the future

    companion object {
        fun defaultSort() = DATE_COOKED_NEWEST
    }
}

fun List<DishMemory>.applyMemorySort(option: DishMemorySortOption): List<DishMemory> {
    val distantPastTimestamp = Timestamp(-62135596800L, 0) // 0001-01-01T00:00:00Z
    val distantFutureTimestampSeconds = 253402300799L // Approx Year 9999
    val distantFutureTimestamp = Timestamp(distantFutureTimestampSeconds, 999999999) // 9999-12-31T23:59:59.999...Z

    return when (option) {
        DishMemorySortOption.DATE_COOKED_NEWEST ->
            sortedWith(compareByDescending { it.timestamp ?: distantPastTimestamp })
        DishMemorySortOption.DATE_COOKED_OLDEST ->
            sortedWith(compareBy { it.timestamp ?: distantFutureTimestamp })

        DishMemorySortOption.RATING_HIGHEST ->
            sortedWith(compareByDescending<DishMemory> { it.rating }.thenByDescending { it.timestamp ?: distantPastTimestamp })
        DishMemorySortOption.RATING_LOWEST ->
            sortedWith(compareBy<DishMemory> { it.rating }.thenByDescending { it.timestamp ?: distantPastTimestamp })
    }
}