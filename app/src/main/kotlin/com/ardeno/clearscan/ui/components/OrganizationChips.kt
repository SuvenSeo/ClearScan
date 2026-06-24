package com.ardeno.clearscan.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ardeno.clearscan.model.DocumentFolder

@Composable
fun FolderFilterRow(
    folders: List<DocumentFolder>,
    selectedFolderId: String?,
    showFavoritesOnly: Boolean,
    onSelectAll: () -> Unit,
    onSelectFavorites: () -> Unit,
    onSelectFolder: (String) -> Unit,
    onCreateFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = !showFavoritesOnly && selectedFolderId == null,
            onClick = onSelectAll,
            label = { Text("All") }
        )
        FilterChip(
            selected = showFavoritesOnly,
            onClick = onSelectFavorites,
            label = { Text("Favorites") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Star,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        )
        folders.forEach { folder ->
            FilterChip(
                selected = selectedFolderId == folder.id,
                onClick = { onSelectFolder(folder.id) },
                label = { Text(folder.name) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Folder,
                        contentDescription = null,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            )
        }
        FilterChip(
            selected = false,
            onClick = onCreateFolder,
            label = { Text("New folder") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        )
    }
}

@Composable
fun TagChipRow(
    tags: List<String>,
    modifier: Modifier = Modifier
) {
    if (tags.isEmpty()) return

    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        tags.forEach { tag ->
            Text(
                modifier = Modifier
                    .padding(vertical = 2.dp)
                    .padding(horizontal = 8.dp),
                text = tag,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
