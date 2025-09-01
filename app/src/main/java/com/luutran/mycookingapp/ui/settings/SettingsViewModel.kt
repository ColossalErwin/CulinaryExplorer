package com.luutran.mycookingapp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.luutran.mycookingapp.data.datastore.ThemeOption
import com.luutran.mycookingapp.data.datastore.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val selectedTheme: ThemeOption = ThemeOption.SYSTEM,
    val fontScale: Float = 1.0f
)

class SettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> =
        kotlinx.coroutines.flow.combine(
            userPreferencesRepository.themeOption,
            userPreferencesRepository.fontScale
        ) { theme, scale ->
            SettingsUiState(theme, scale)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState() // Initial default state
        )

    fun updateThemeOption(themeOption: ThemeOption) {
        viewModelScope.launch {
            userPreferencesRepository.setThemeOption(themeOption)
        }
    }

    fun updateFontScale(scale: Float) {
        viewModelScope.launch {
            userPreferencesRepository.setFontScale(scale)
        }
    }
}

class SettingsViewModelFactory(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(userPreferencesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}