package com.luutran.mycookingapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luutran.mycookingapp.data.model.ClassificationGroup
import com.luutran.mycookingapp.data.model.ClassificationNavigationTarget

@Composable
fun ClassificationGroupItem(
    group: ClassificationGroup,
    onGroupClick: (ClassificationNavigationTarget) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onGroupClick(group.navigationTarget) },
        shape = RoundedCornerShape(12.dp), // Rounded corners
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .background(group.backgroundColor.copy(alpha = 0.3f))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = group.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = group.backgroundColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = group.descriptionExamples,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun BrowseClassificationGroups(
    classificationGroups: List<ClassificationGroup>,
    onGroupClick: (ClassificationNavigationTarget) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (classificationGroups.isEmpty()) {
            Text("Loading options...", modifier.padding(16.dp))
            return
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2), // Two columns
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(classificationGroups, key = { it.id }) { group ->
                ClassificationGroupItem(
                    group = group,
                    onGroupClick = onGroupClick
                )
            }
        }
    }
}
