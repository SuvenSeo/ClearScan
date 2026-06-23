package com.ardeno.clearscan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ardeno.clearscan.model.OcrStatus
import com.ardeno.clearscan.model.ScanDocument
import com.ardeno.clearscan.ui.components.DocumentThumbnailHero
import com.ardeno.clearscan.ui.components.OcrStatusChip
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val detailDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy · HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailSheet(
    document: ScanDocument,
    signatureText: String,
    pdfPassword: String,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onRetryOcr: () -> Unit,
    onSignatureTextChange: (String) -> Unit,
    onPdfPasswordChange: (String) -> Unit,
    onSplit: () -> Unit,
    onRotate: () -> Unit,
    onSign: () -> Unit,
    onRedact: () -> Unit,
    onPasswordProtect: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete document?") },
            text = { Text("\"${document.title}\" will be permanently removed from this device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp, bottom = 4.dp)
                        .width(36.dp)
                        .height(5.dp)
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DocumentThumbnailHero(document = document)

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = document.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = detailDateFormatter.format(document.createdAt.atZone(ZoneId.systemDefault())),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OcrStatusChip(status = document.ocrStatus)
                    Text(
                        text = document.exportLabel(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onShare,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = null)
                    Text(modifier = Modifier.padding(start = 8.dp), text = "Share")
                }
                FilledTonalButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = null)
                    Text(modifier = Modifier.padding(start = 8.dp), text = "Delete")
                }
            }

            if (document.ocrStatus == OcrStatus.Failed) {
                FilledTonalButton(
                    onClick = onRetryOcr,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null)
                    Text(modifier = Modifier.padding(start = 8.dp), text = "Retry OCR")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Text(
                text = "Recognized text",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = document.ocrText.ifBlank {
                    "OCR text will appear here after the local recognizer finishes processing this document."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Text(
                text = "PDF tools",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PdfToolButton(modifier = Modifier.weight(1f), label = "Split", onClick = onSplit)
                PdfToolButton(modifier = Modifier.weight(1f), label = "Rotate", onClick = onRotate)
                PdfToolButton(modifier = Modifier.weight(1f), label = "Redact", onClick = onRedact)
            }

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = signatureText,
                onValueChange = onSignatureTextChange,
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                label = { Text("Signature text") }
            )
            PdfToolButton(
                modifier = Modifier.fillMaxWidth(),
                label = "Add signature",
                onClick = onSign
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = pdfPassword,
                onValueChange = onPdfPasswordChange,
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                visualTransformation = PasswordVisualTransformation(),
                label = { Text("PDF password") }
            )
            PdfToolButton(
                modifier = Modifier.fillMaxWidth(),
                label = "Password protect",
                onClick = onPasswordProtect
            )
        }
    }
}

@Composable
private fun PdfToolButton(
    modifier: Modifier = Modifier,
    label: String,
    onClick: () -> Unit
) {
    FilledTonalButton(
        modifier = modifier,
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(text = label)
    }
}

private fun ScanDocument.exportLabel(): String = when {
    searchablePdfReady -> "Searchable PDF ready"
    pdfPath != null -> "Original PDF ready"
    pageImagePaths.isNotEmpty() -> "Image export ready"
    else -> "No export available"
}
