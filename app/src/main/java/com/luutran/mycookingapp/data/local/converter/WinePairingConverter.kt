package com.luutran.mycookingapp.data.local.converter

import androidx.room.TypeConverter
import com.luutran.mycookingapp.data.model.WinePairing
import com.google.gson.Gson

class WinePairingConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromWinePairing(winePairing: WinePairing?): String? {
        // If winePairing is null, gson.toJson(null) will return the string "null"
        // which is fine as gson.fromJson can handle it back to a null object.
        // winePairing is actually null
        return gson.toJson(winePairing)
    }

    @TypeConverter
    fun toWinePairing(winePairingJson: String?): WinePairing? {
        return winePairingJson?.let {
            // Handle the case where the JSON string is "null" explicitly
            if (it.equals("null", ignoreCase = true)) {
                null
            } else {
                gson.fromJson(it, WinePairing::class.java)
            }
        }
    }
}