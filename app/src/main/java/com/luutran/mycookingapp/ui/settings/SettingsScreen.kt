package com.luutran.mycookingapp.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luutran.mycookingapp.data.datastore.ThemeOption
import com.luutran.mycookingapp.data.datastore.UserPreferencesRepository
import java.util.Locale
import kotlin.text.isLowerCase
import kotlin.text.lowercase
import kotlin.text.replace
import kotlin.text.replaceFirstChar
import kotlin.text.titlecase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(
            UserPreferencesRepository.getInstance(LocalContext.current.applicationContext)
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // --- Theme Setting ---
            SettingGroupTitle("Appearance")

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // --- Font Size Setting ---
            FontSizeSettingRow(
                currentScale = uiState.fontScale,
                onScaleChange = { viewModel.updateFontScale(it) }
            )
        }
    }
}

@Composable
fun SettingGroupTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun ThemeSettingRow(
    selectedTheme: ThemeOption,
    onThemeSelected: (ThemeOption) -> Unit
) {
    Text(
        "Theme",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    Column(Modifier.selectableGroup()) {
        ThemeOption.entries.forEach { theme ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = (theme == selectedTheme),
                        onClick = { onThemeSelected(theme) },
                        role = Role.RadioButton
                    )
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (theme == selectedTheme),
                    onClick = null // null recommended for radio buttons to make row clickable
                )
                Text(
                    text = theme.name
                        .lowercase(Locale.getDefault())
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                        .replace("_", " "), // Format enum name nicely
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }
}


@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class) // Added ExperimentalMaterial3Api if not already top-level
@Composable
fun FontSizeSettingRow(
    currentScale: Float,
    onScaleChange: (Float) -> Unit
) {
    // State for the Slider's position (Float)
    var sliderPosition by remember(currentScale) { mutableFloatStateOf(currentScale) }

    var textFieldValue by remember(currentScale) {
        mutableStateOf(String.format(Locale.US, "%.2f", currentScale))
    }

    LaunchedEffect(currentScale) {
        val formattedScale = String.format(Locale.US, "%.2f", currentScale)
        if (textFieldValue != formattedScale) { // Avoid unnecessary recompositions
            textFieldValue = formattedScale
        }
        if (sliderPosition != currentScale) {
            sliderPosition = currentScale
        }
    }


    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val valueRange = 0.7f..1.5f
    val steps = 7 // (1.5 - 0.7) / 0.1 - 1 = 7 intervals for 0.125 steps. Adjust as needed.


    fun applyTextFieldValue() {
        val newScale = textFieldValue.toFloatOrNull()
        if (newScale != null && newScale in valueRange) {
            val validScale = newScale.coerceIn(valueRange.start, valueRange.endInclusive)
            sliderPosition = validScale
            textFieldValue = String.format(Locale.US, "%.2f", validScale) // Re-format to maintain consistency
            onScaleChange(validScale)
        } else {
            // Revert to last known good slider position if input is invalid
            textFieldValue = String.format(Locale.US, "%.2f", sliderPosition)
        }
    }

    Column {
        Text(
            "Font Size",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    textFieldValue = newValue
                },
                modifier = Modifier.width(80.dp),
                label = { Text("Scale") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        applyTextFieldValue()
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }
                ),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = androidx.compose.ui.text.style.TextAlign.End),
                isError = textFieldValue.toFloatOrNull() == null || (textFieldValue.toFloatOrNull() ?: 0f) !in valueRange
            )

            Slider(
                value = sliderPosition,
                onValueChange = { newSliderValue ->
                    sliderPosition = newSliderValue
                    // Update TextField when slider moves
                    textFieldValue = String.format(Locale.US, "%.2f", newSliderValue)
                },
                onValueChangeFinished = {
                    val finalScale = textFieldValue.toFloatOrNull()?.coerceIn(valueRange.start, valueRange.endInclusive)
                    if (finalScale != null) {
                        onScaleChange(finalScale)
                        textFieldValue = String.format(Locale.US, "%.2f", finalScale)
                        sliderPosition = finalScale // Ensure slider snaps to the applied value
                    }
                },
                valueRange = valueRange,
                steps = steps,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            )
        }
        if (textFieldValue.toFloatOrNull() == null || (textFieldValue.toFloatOrNull() ?: 0f) !in valueRange) {
            Text(
                "Enter a value between ${valueRange.start} and ${valueRange.endInclusive}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
        }
        Text(
            "Applies to text throughout the app. Some text may not scale.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}