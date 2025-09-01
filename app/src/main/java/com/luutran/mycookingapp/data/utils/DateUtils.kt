package com.luutran.mycookingapp.data.utils


import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

object DateUtils {

    fun formatTimestamp(timestamp: Timestamp?, pattern: String = "yyyy-MM-dd HH:mm:ss"): String {
        if (timestamp == null) return "N/A"
        return try {
            val sdf = SimpleDateFormat(pattern, Locale.getDefault())
            sdf.format(timestamp.toDate())
        } catch (e: Exception) {
            "Invalid Date"
        }
    }

    fun timeAgo(timestamp: Timestamp?): String {
        if (timestamp == null) return "N/A"
        val date = timestamp.toDate()
        val now = System.currentTimeMillis()
        val diff = now - date.time

        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 0 -> "$days day(s) ago"
            hours > 0 -> "$hours hour(s) ago"
            minutes > 0 -> "$minutes minute(s) ago"
            else -> "$seconds second(s) ago"
        }
    }
}