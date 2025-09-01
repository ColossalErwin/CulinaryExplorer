package com.luutran.mycookingapp.data.datastore

import android.annotation.SuppressLint
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Calendar

enum class ThemeOption {
    LIGHT, DARK, SYSTEM
}
private const val USER_PREFERENCES_NAME = "my_cooking_app_user_preferences"

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = USER_PREFERENCES_NAME)

data class TodaysRecipePreferences(
    val recipeId: Int?,
    val timestamp: Long?
)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val FEATURED_RECIPE_ID = intPreferencesKey("featured_recipe_id")
        val FEATURED_RECIPE_TIMESTAMP = longPreferencesKey("featured_recipe_timestamp")

        val THEME_OPTION = stringPreferencesKey("theme_option")
        val FONT_SCALE = floatPreferencesKey("font_scale")
    }

    private val todaysRecipePreferencesFlow: Flow<TodaysRecipePreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            val recipeId = preferences[PreferencesKeys.FEATURED_RECIPE_ID]
            val timestamp = preferences[PreferencesKeys.FEATURED_RECIPE_TIMESTAMP]

            //check is timestamp is valid for today
            var isValidForToday = false
            if (timestamp != null && recipeId != null) {
                val todayCal = Calendar.getInstance()
                val prefCal = Calendar.getInstance().apply { timeInMillis = timestamp }
                if (todayCal.get(Calendar.YEAR) == prefCal.get(Calendar.YEAR) &&
                    todayCal.get(Calendar.DAY_OF_YEAR) == prefCal.get(Calendar.DAY_OF_YEAR)
                ) {
                    isValidForToday = true
                }
            }

            if (isValidForToday) {
                TodaysRecipePreferences(recipeId, timestamp)
            } else {
                // If not valid for today, treat as if no preference is set
                TodaysRecipePreferences(null, null)
            }
        }

    suspend fun saveTodaysFeaturedRecipe(recipeId: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FEATURED_RECIPE_ID] = recipeId
            preferences[PreferencesKeys.FEATURED_RECIPE_TIMESTAMP] = System.currentTimeMillis()
        }
    }

    suspend fun clearTodaysFeaturedRecipe() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.FEATURED_RECIPE_ID)
            preferences.remove(PreferencesKeys.FEATURED_RECIPE_TIMESTAMP)
        }
    }

    suspend fun getValidTodaysRecipeId(): Int? {
        val prefs = todaysRecipePreferencesFlow.first() // Gets the latest value
        return prefs.recipeId // Will be null if not valid for today
    }

    // Theme Preference
    val themeOption: Flow<ThemeOption> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            ThemeOption.valueOf(
                preferences[PreferencesKeys.THEME_OPTION] ?: ThemeOption.SYSTEM.name
            )
        }

    suspend fun setThemeOption(themeOption: ThemeOption) {
        context.dataStore.edit { settings ->
            settings[PreferencesKeys.THEME_OPTION] = themeOption.name
        }
    }

    // Font Scale Preference
    val fontScale: Flow<Float> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.FONT_SCALE] ?: 1.0f // Default to 1.0f (normal scale)
        }

    suspend fun setFontScale(scale: Float) {
        val clampedScale = scale.coerceIn(0.75f, 1.5f)
        context.dataStore.edit { settings ->
            settings[PreferencesKeys.FONT_SCALE] = clampedScale
        }
    }

    companion object {
    @SuppressLint("StaticFieldLeak")
    @Volatile
    private var INSTANCE: UserPreferencesRepository? = null

        fun getInstance(context: Context): UserPreferencesRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = UserPreferencesRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}