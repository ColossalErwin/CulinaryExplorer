package com.luutran.mycookingapp.data.local.converter

import androidx.room.TypeConverter
import com.luutran.mycookingapp.data.model.InstructionStepParent
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


class InstructionStepParentListConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromInstructionStepParentList(instructions: List<InstructionStepParent>?): String? {
        return instructions?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toInstructionStepParentList(instructionsJson: String?): List<InstructionStepParent>? {
        return instructionsJson?.let {
            val type = object : TypeToken<List<InstructionStepParent>>() {}.type
            gson.fromJson(it, type)
        }
    }
}