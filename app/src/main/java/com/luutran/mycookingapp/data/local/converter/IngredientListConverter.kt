package com.luutran.mycookingapp.data.local.converter

import androidx.room.TypeConverter
import com.luutran.mycookingapp.data.model.Ingredient
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class IngredientListConverter {
    private val gson = Gson()

    /**
     * Converts a list of Ingredient objects to a JSON String.
     */
    @TypeConverter
    fun fromIngredientList(ingredients: List<Ingredient>?): String? {
        return ingredients?.let { gson.toJson(it) }
    }

    /**
     * Converts a JSON String back to a list of Ingredient objects.
     */
    @TypeConverter
    fun toIngredientList(ingredientsJson: String?): List<Ingredient>? {
        return ingredientsJson?.let {
            val type = object : TypeToken<List<Ingredient>>() {}.type
            gson.fromJson(it, type)
        }
    }
}