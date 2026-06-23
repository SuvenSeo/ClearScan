package com.ardeno.clearscan.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.MergeType
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ardeno.clearscan.model.LibraryViewMode
import com.ardeno.clearscan.model.ScanDocument
import com.ardeno.clearscan.ui.components.DocumentGridItem
import com.ardeno.clearscan.ui.components.GroupedSection
import com.ardeno.clearscan.ui.components.IosSearchField
import com.ardeno.clearscan.ui.components.SwipeableDocumentRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    documents: List<ScanDocument>,
    query: String,
    viewMode: LibraryViewMode,
    isWorking: Boolean,
    showEmptySearch: Boolean = false,
    onQueryChange: (String) -> Unit,
    onViewModeChange: (LibraryViewMode) -> Unit,
    onScanClick: () -> Unit,
    onDocumentClick: (ScanDocument) -> Unit,
    onDeleteDocument: (ScanDocument) -> Unit,
    onMergeAllDocuments: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Library",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (documents.isEmpty()) {
                                "No documents yet"
                            } else {
                                "${documents.size} document${if (documents.size == 1) "" else "s"}"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onScanClick) {
                        Icon(
                            imageVector = Icons.Rounded.DocumentScanner,
                            contentDescription = "Scan document"
                        )
                    }
                    IconButton(
                        onClick = {
                            val nextMode = when (viewMode) {
                                LibraryViewMode.List -> LibraryViewMode.Grid
                                LibraryViewMode.Grid -> LibraryViewMode.List
                            }
                            onViewModeChange(nextMode)
                        }
                    ) {
                        Icon(
                            imageVector = when (viewMode) {
                                LibraryViewMode.List -> Icons.Outlined.GridView
                                LibraryViewMode.Grid -> Icons.Outlined.ViewList
                            },
                            contentDescription = when (viewMode) {
                                LibraryViewMode.List -> "Switch to grid view"
                                LibraryViewMode.Grid -> "Switch to list view"
                            }
                        )
                    }
                    if (isWorking) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            IosSearchField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = "Search titles or OCR text"
            )

            when {
                documents.isEmpty() -> {
                    if (showEmptySearch) {
                        EmptySearchState()
                    } else {
                        EmptyLibraryState(onScanClick = onScanClick)
                    }
                }
                viewMode == LibraryViewMode.Grid -> {
                    LibraryGridContent(
                        documents = documents,
                        onDocumentClick = onDocumentClick,
                        onMergeAllDocuments = onMergeAllDocuments
                    )
                }
                else -> {
                    LibraryListContent(
                        documents = documents,
                        onDocumentClick = onDocumentClick,
                        onDeleteDocument = onDeleteDocument,
                        onMergeAllDocuments = onMergeAllDocuments
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryListContent(
    documents: List<ScanDocument>,
    onDocumentClick: (ScanDocument) -> Unit,
    onDeleteDocument: (ScanDocument) -> Unit,
    onMergeAllDocuments: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (documents.size >= 2) {
            item {
                FilledTonalButton(
                    onClick = onMergeAllDocuments,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MergeType,
                        contentDescription = null
                    )
                    Text(
                        modifier = Modifier.padding(start = 8.dp),
                        text = "Merge all ${documents.size} documents"
                    )
                }
            }
        }

        item {
            GroupedSection {
                documents.forEachIndexed { index, document ->
                    SwipeableDocumentRow(
                        document = document,
                        onClick = { onDocumentClick(document) },
                        onDelete = { onDeleteDocument(document) },
                        showDivider = index < documents.lastIndex
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun LibraryGridContent(
    documents: List<ScanDocument>,
    onDocumentClick: (ScanDocument) -> Unit,
    onMergeAllDocuments: () -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 140.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (documents.size >= 2) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                FilledTonalButton(
                    onClick = onMergeAllDocuments,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MergeType,
                        contentDescription = null
                    )
                    Text(
                        modifier = Modifier.padding(start = 8.dp),
                        text = "Merge all ${documents.size} documents"
                    )
                }
            }
        }

        items(
            items = documents,
            key = { it.id }
        ) { document ->
            DocumentGridItem(
                document = document,
                onClick = { onDocumentClick(document) }
            )
        }
    }
}

@Composable
private fun EmptySearchState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "No matching documents",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Try searching by title, tag, or text recognized from a scan.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun EmptyLibraryState(onScanClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Description,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Text(
            text = "Your document library is empty",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Scan a receipt, contract, or note. OCR runs locally so you can search text later.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Button(
            onClick = onScanClick,
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(
                imageVector = Icons.Rounded.DocumentScanner,
                contentDescription = null
            )
            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = "Scan your first document"
            )
        }
    }
}
