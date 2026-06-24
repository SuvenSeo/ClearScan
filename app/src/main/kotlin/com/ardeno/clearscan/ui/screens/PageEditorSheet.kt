package com.ardeno.clearscan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.ardeno.clearscan.model.ScanDocument
import java.io.File

enum class PageEditorMode {
    Reorder,
    Delete
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageEditorSheet(
    document: ScanDocument,
    mode: PageEditorMode,
    onDismiss: () -> Unit,
    onConfirmReorder: (List<Int>) -> Unit,
    onConfirmDelete: (List<Int>) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val pagePaths = document.pageImagePaths.filter { File(it).exists() }
    val orderedIndices = remember(document.id, mode) {
        mutableStateListOf<Int>().apply { addAll(pagePaths.indices) }
    }
    val keptPages = remember(document.id, mode) {
        mutableStateListOf<Int>().apply { addAll(pagePaths.indices) }
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = when (mode) {
                    PageEditorMode.Reorder -> "Reorder pages"
                    PageEditorMode.Delete -> "Delete pages"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = when (mode) {
                    PageEditorMode.Reorder -> "Move pages up or down, then save a new copy."
                    PageEditorMode.Delete -> "Uncheck pages to remove. At least one page must remain."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (mode) {
                    PageEditorMode.Reorder -> {
                        itemsIndexed(
                            items = orderedIndices,
                            key = { displayIndex, pageIndex -> "reorder-$displayIndex-$pageIndex" }
                        ) { displayIndex, pageIndex ->
                            PageRow(
                                pagePath = pagePaths[pageIndex],
                                pageLabel = "Page ${pageIndex + 1}",
                                trailing = {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = {
                                                if (displayIndex > 0) {
                                                    val current = orderedIndices[displayIndex]
                                                    orderedIndices[displayIndex] = orderedIndices[displayIndex - 1]
                                                    orderedIndices[displayIndex - 1] = current
                                                }
                                            },
                                            enabled = displayIndex > 0
                                        ) {
                                            Icon(Icons.Outlined.ArrowUpward, contentDescription = "Move up")
                                        }
                                        IconButton(
                                            onClick = {
                                                if (displayIndex < orderedIndices.lastIndex) {
                                                    val current = orderedIndices[displayIndex]
                                                    orderedIndices[displayIndex] = orderedIndices[displayIndex + 1]
                                                    orderedIndices[displayIndex + 1] = current
                                                }
                                            },
                                            enabled = displayIndex < orderedIndices.lastIndex
                                        ) {
                                            Icon(Icons.Outlined.ArrowDownward, contentDescription = "Move down")
                                        }
                                    }
                                }
                            )
                        }
                    }
                    PageEditorMode.Delete -> {
                        itemsIndexed(
                            items = pagePaths,
                            key = { pageIndex, _ -> "delete-$pageIndex" }
                        ) { pageIndex, pagePath ->
                            val isKept = pageIndex in keptPages
                            PageRow(
                                pagePath = pagePath,
                                pageLabel = "Page ${pageIndex + 1}",
                                onClick = {
                                    if (isKept) {
                                        if (keptPages.size <= 1) {
                                            errorMessage = "Keep at least one page."
                                            return@PageRow
                                        }
                                        keptPages.remove(pageIndex)
                                    } else {
                                        keptPages.add(pageIndex)
                                        keptPages.sort()
                                    }
                                    errorMessage = null
                                },
                                trailing = {
                                    Icon(
                                        imageVector = if (isKept) {
                                            Icons.Outlined.CheckBox
                                        } else {
                                            Icons.Outlined.CheckBoxOutlineBlank
                                        },
                                        contentDescription = if (isKept) "Keep page" else "Remove page",
                                        tint = if (isKept) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            }

            errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        when (mode) {
                            PageEditorMode.Reorder -> onConfirmReorder(orderedIndices.toList())
                            PageEditorMode.Delete -> {
                                if (keptPages.isEmpty()) {
                                    errorMessage = "Keep at least one page."
                                } else {
                                    onConfirmDelete(keptPages.sorted())
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = pagePaths.isNotEmpty()
                ) {
                    Text("Save copy")
                }
            }
        }
    }
}

@Composable
private fun PageRow(
    pagePath: String,
    pageLabel: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .width(56.dp)
                .aspectRatio(3f / 4f)
                .clip(MaterialTheme.shapes.small)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(File(pagePath))
                    .crossfade(true)
                    .build(),
                contentDescription = pageLabel,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Text(
            text = pageLabel,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge
        )
        Box(
            modifier = Modifier.size(96.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            trailing()
        }
    }
}
