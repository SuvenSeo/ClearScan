package com.ardeno.clearscan.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ardeno.clearscan.R
import com.ardeno.clearscan.model.DocumentFolder
import com.ardeno.clearscan.ui.theme.ClearScanMotion

@Composable
fun FolderFilterRow(
    folders: List<DocumentFolder>,
    selectedFolderId: String?,
    showFavoritesOnly: Boolean,
    onSelectAll: () -> Unit,
    onSelectFavorites: () -> Unit,
    onSelectFolder: (String) -> Unit,
    onCreateFolder: () -> Unit,
    onRenameFolder: (String, String) -> Unit,
    onDeleteFolder: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var menuFolder by remember { mutableStateOf<DocumentFolder?>(null) }
    var renameTarget by remember { mutableStateOf<DocumentFolder?>(null) }
    var deleteTarget by remember { mutableStateOf<DocumentFolder?>(null) }

    renameTarget?.let { folder ->
        RenameFolderDialog(
            initialName = folder.name,
            onDismiss = { renameTarget = null },
            onRename = { name ->
                onRenameFolder(folder.id, name)
                renameTarget = null
            }
        )
    }

    deleteTarget?.let { folder ->
        DeleteFolderDialog(
            folderName = folder.name,
            onDismiss = { deleteTarget = null },
            onConfirm = {
                onDeleteFolder(folder.id)
                deleteTarget = null
            }
        )
    }

    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AnimatedFilterChip(
            selected = !showFavoritesOnly && selectedFolderId == null,
            onClick = onSelectAll,
            label = { Text("All") }
        )
        AnimatedFilterChip(
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
            Box {
                AnimatedFilterChip(
                    selected = selectedFolderId == folder.id,
                    onClick = { onSelectFolder(folder.id) },
                    onLongClick = { menuFolder = folder },
                    label = { Text(folder.name) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Folder,
                            contentDescription = null,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                )
                DropdownMenu(
                    expanded = menuFolder?.id == folder.id,
                    onDismissRequest = { menuFolder = null }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_rename)) },
                        onClick = {
                            menuFolder = null
                            renameTarget = folder
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_delete)) },
                        onClick = {
                            menuFolder = null
                            deleteTarget = folder
                        }
                    )
                }
            }
        }
        AnimatedFilterChip(
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
private fun RenameFolderDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    var folderName by remember(initialName) { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.folders_rename)) },
        text = {
            OutlinedTextField(
                value = folderName,
                onValueChange = { folderName = it },
                singleLine = true,
                label = { Text(stringResource(R.string.library_folder_name)) }
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onRename(folderName) },
                enabled = folderName.isNotBlank()
            ) {
                Text(stringResource(R.string.action_rename))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun DeleteFolderDialog(
    folderName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.folders_delete)) },
        text = {
            Text("\"$folderName\" will be removed. Documents in this folder stay in your library.")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.action_delete),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AnimatedFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.05f else 1f,
        animationSpec = ClearScanMotion.springSnappy,
        label = "chipScale"
    )
    val chipModifier = if (onLongClick != null) {
        modifier
            .scale(scale)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    } else {
        modifier.scale(scale)
    }

    FilterChip(
        selected = selected,
        // When long-press is enabled, clicks are handled by combinedClickable above.
        onClick = { if (onLongClick == null) onClick() },
        label = label,
        leadingIcon = leadingIcon,
        modifier = chipModifier,
        colors = FilterChipDefaults.filterChipColors().copy(
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outline,
            selectedBorderColor = MaterialTheme.colorScheme.secondary,
            disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
            disabledSelectedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            selectedBorderWidth = 1.5.dp
        )
    )
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
