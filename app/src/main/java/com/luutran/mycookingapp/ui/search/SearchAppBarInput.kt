package com.luutran.mycookingapp.ui.search

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchAppBarInput(
    query: String,
    onQueryChanged: (String) -> Unit,
    onSearchExecute: () -> Unit,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier,
    placeholderText: String = "Search Recipes",
    autoFocus: Boolean = true
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .padding(vertical = 2.dp, horizontal = 0.dp),
        placeholder = { Text(placeholderText) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClearSearch) {
                    Icon(Icons.Filled.Clear, contentDescription = "Clear Search")
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = {
                if (query.isNotBlank()) {
                    onSearchExecute()
                }
            }
        ),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        shape = MaterialTheme.shapes.extraLarge
    )

    if (autoFocus) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }
}