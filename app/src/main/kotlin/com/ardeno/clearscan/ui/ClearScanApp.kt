package com.ardeno.clearscan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ardeno.clearscan.ClearScanUiState
import com.ardeno.clearscan.model.OcrStatus
import com.ardeno.clearscan.model.ScanDocument
import com.ardeno.clearscan.model.matchesQuery
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ClearScanApp(
    state: ClearScanUiState,
    onScanClick: () -> Unit,
    onImportClick: () -> Unit,
    onQueryChange: (String) -> Unit,
    onToggleDocumentExpanded: (ScanDocument) -> Unit,
    onShareDocument: (ScanDocument) -> Unit,
    onDeleteDocument: (ScanDocument) -> Unit,
    onRetryOcr: (ScanDocument) -> Unit,
    onDismissMessage: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val visibleDocuments = remember(state.documents, state.query) {
        state.documents.filter { it.matchesQuery(state.query) }
    }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onDismissMessage()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            IconButton(
                onClick = onScanClick,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Scan",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Header(
                    isWorking = state.isSaving || state.isOcrRunning,
                    onScanClick = onScanClick,
                    onImportClick = onImportClick
                )
            }

            item {
                PrivacyStrip()
            }

            item {
                WhyClearScan()
            }

            item {
                ToolGrid()
            }

            item {
                SearchBox(
                    query = state.query,
                    onQueryChange = onQueryChange
                )
            }

            item {
                SectionTitle(
                    title = "Documents",
                    count = visibleDocuments.size
                )
            }

            if (state.documents.isEmpty()) {
                item {
                    EmptyLibrary(onScanClick = onScanClick)
                }
            } else if (visibleDocuments.isEmpty()) {
                item {
                    EmptySearch()
                }
            } else {
                items(
                    items = visibleDocuments,
                    key = { it.id }
                ) { document ->
                    DocumentRow(
                        document = document,
                        isExpanded = state.expandedDocumentId == document.id,
                        onToggle = { onToggleDocumentExpanded(document) },
                        onShare = { onShareDocument(document) },
                        onDelete = { onDeleteDocument(document) },
                        onRetryOcr = { onRetryOcr(document) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(76.dp))
            }
        }
    }
}

@Composable
private fun Header(
    isWorking: Boolean,
    onScanClick: () -> Unit,
    onImportClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ClearScan",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Private document scanner",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isWorking) {
                CircularProgressIndicator(modifier = Modifier.size(30.dp))
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onScanClick,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = Icons.Rounded.DocumentScanner,
                    contentDescription = null
                )
                Text(
                    modifier = Modifier.padding(start = 8.dp),
                    text = "Scan"
                )
            }
            FilledTonalButton(
                onClick = onImportClick,
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.UploadFile,
                    contentDescription = null
                )
                Text(
                    modifier = Modifier.padding(start = 8.dp),
                    text = "Import"
                )
            }
        }
    }
}

@Composable
private fun PrivacyStrip() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AssistChip(
            onClick = {},
            label = { Text("Offline") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.CloudOff,
                    contentDescription = null
                )
            }
        )
        AssistChip(
            onClick = {},
            label = { Text("No ads") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = null
                )
            }
        )
        AssistChip(
            onClick = {},
            label = { Text("App vault") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null
                )
            }
        )
    }
}

@Composable
private fun WhyClearScan() {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Built to stay free",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                text = "No ads. No subscriptions. No watermark. No account gate.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PromisePill(
                    modifier = Modifier.weight(1f),
                    label = "Local-first",
                    icon = Icons.Outlined.CloudOff
                )
                PromisePill(
                    modifier = Modifier.weight(1f),
                    label = "Private",
                    icon = Icons.Outlined.Lock
                )
            }
        }
    }
}

@Composable
private fun PromisePill(
    modifier: Modifier = Modifier,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ToolGrid() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ToolCard(
                modifier = Modifier.weight(1f),
                title = "Searchable PDF",
                icon = Icons.Outlined.PictureAsPdf
            )
            ToolCard(
                modifier = Modifier.weight(1f),
                title = "OCR text",
                icon = Icons.Outlined.TextFields
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ToolCard(
                modifier = Modifier.weight(1f),
                title = "Share export",
                icon = Icons.Outlined.Share
            )
            ToolCard(
                modifier = Modifier.weight(1f),
                title = "Find text",
                icon = Icons.Outlined.Search
            )
        }
    }
}

@Composable
private fun ToolCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SearchBox(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        shape = RoundedCornerShape(8.dp),
        label = { Text("Search documents") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null
            )
        }
    )
}

@Composable
private fun SectionTitle(
    title: String,
    count: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                text = "$count",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun EmptySearch() {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "No matching scans",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Try a title, tag, or OCR text from a saved page.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyLibrary(onScanClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "No scans yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Button(
                onClick = onScanClick,
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.DocumentScanner,
                    contentDescription = null
                )
                Text(
                    modifier = Modifier.padding(start = 8.dp),
                    text = "Start scan"
                )
            }
        }
    }
}

@Composable
private fun DocumentRow(
    document: ScanDocument,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onRetryOcr: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PictureAsPdf,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = document.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${document.pageCount} pages · ${document.ocrStatus.label()} · ${dateFormatter.format(document.createdAt.atZone(ZoneId.systemDefault()))}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onShare) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Share ${document.title}"
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete ${document.title}"
                    )
                }
            }

            if (isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = document.exportLabel(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = document.ocrText.ifBlank { "OCR text will appear here after the local recognizer finishes." },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = onShare,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = null
                            )
                            Text(
                                modifier = Modifier.padding(start = 8.dp),
                                text = "Share"
                            )
                        }
                        if (document.ocrStatus == OcrStatus.Failed) {
                            TextButton(onClick = onRetryOcr) {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = null
                                )
                                Text(
                                    modifier = Modifier.padding(start = 6.dp),
                                    text = "Retry OCR"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun ScanDocument.exportLabel(): String = when {
    searchablePdfReady -> "Searchable PDF ready"
    pdfPath != null -> "Original scanner PDF ready"
    pageImagePaths.isNotEmpty() -> "Page image export ready"
    else -> "No export file available"
}

private fun OcrStatus.label(): String = when (this) {
    OcrStatus.NotStarted -> "OCR pending"
    OcrStatus.Queued -> "OCR queued"
    OcrStatus.Processing -> "OCR running"
    OcrStatus.Ready -> "Search ready"
    OcrStatus.Failed -> "OCR failed"
}

private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM")
