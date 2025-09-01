package com.luutran.mycookingapp.ui.dishmemories

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SortAndFilterBottomSheet(
    currentSortOption: CookedDishSortOption,
    onSortOptionSelected: (CookedDishSortOption) -> Unit,
    onDismiss: () -> Unit,
) {

    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Sort By", style = MaterialTheme.typography.titleLarge)
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close Sort/Filter")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(CookedDishSortOption.values()) { option ->
                SortOptionItem(
                    option = option,
                    isSelected = option == currentSortOption,
                    onSelected = {
                        onSortOptionSelected(option)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Apply")
        }
    }
}

@Composable
private fun SortOptionItem(
    option: CookedDishSortOption,
    isSelected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onSelected)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = option.displayName,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}