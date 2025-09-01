package com.luutran.mycookingapp.ui.dishmemories

import kotlin.text.lowercase
import com.google.firebase.Timestamp // Import Firebase Timestamp
import java.util.Date // Keep for Date(0) or Date(Long.MAX_VALUE) as fallbacks if needed
import com.luutran.mycookingapp.data.model.CookedDishEntry

enum class CookedDishSortOption(val displayName: String) {
    LAST_COOKED_NEWEST("Date Cooked (Newest)"),
    LAST_COOKED_OLDEST("Date Cooked (Oldest)"),
    FIRST_ADDED_NEWEST("Date Added (Newest)"),
    FIRST_ADDED_OLDEST("Date Added (Oldest)"),
    TIMES_COOKED_MOST("Frequency (Most Cooked)"),
    TIMES_COOKED_LEAST("Frequency (Least Cooked)"),
    NAME_ASCENDING("Name (A-Z)"),
    NAME_DESCENDING("Name (Z-A)");

    companion object {
        fun defaultSort() = LAST_COOKED_NEWEST
    }
}


fun List<CookedDishEntry>.applySort(option: CookedDishSortOption): List<CookedDishEntry> {
    // A very early date, but valid for Timestamp
    val distantPastTimestamp = Timestamp(-62135596800L, 0) // Represents 0001-01-01T00:00:00Z

    // A very far future date, but valid for Timestamp
    // Max Timestamp is 9999-12-31T23:59:59.999999999Z which is 253,402,300,799 seconds
    val distantFutureTimestampSeconds = 253402300799L // Approx Year 9999
    val distantFutureTimestamp = Timestamp(distantFutureTimestampSeconds, 999999999)


    return when (option) {
        CookedDishSortOption.LAST_COOKED_NEWEST -> sortedWith(compareByDescending { it.lastCookedAt ?: distantPastTimestamp })
        CookedDishSortOption.LAST_COOKED_OLDEST -> sortedWith(compareBy { it.lastCookedAt ?: distantFutureTimestamp })
        CookedDishSortOption.FIRST_ADDED_NEWEST -> sortedWith(compareByDescending { it.firstAddedAt ?: distantPastTimestamp })
        CookedDishSortOption.FIRST_ADDED_OLDEST -> sortedWith(compareBy { it.firstAddedAt ?: distantFutureTimestamp })
        CookedDishSortOption.TIMES_COOKED_MOST -> sortedByDescending { it.timesCooked }
        CookedDishSortOption.TIMES_COOKED_LEAST -> sortedBy { it.timesCooked }
        CookedDishSortOption.NAME_ASCENDING -> sortedBy { it.title.lowercase() }
        CookedDishSortOption.NAME_DESCENDING -> sortedByDescending { it.title.lowercase() }
    }
}