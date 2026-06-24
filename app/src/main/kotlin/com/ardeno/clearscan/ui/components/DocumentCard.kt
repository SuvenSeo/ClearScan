package com.ardeno.clearscan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.ardeno.clearscan.model.ScanDocument
import com.ardeno.clearscan.ui.theme.ClearScanElevation
import com.ardeno.clearscan.ui.theme.ClearScanMotion
import com.ardeno.clearscan.ui.theme.ClearScanSpacing
import com.ardeno.clearscan.ui.theme.CardShape
import java.io.File
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

@Composable
fun DocumentCard(
    document: ScanDocument,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = false,
    selectionMode: Boolean = false,
    isSelected: Boolean = false,
    isDuplicate: Boolean = false,
    onSelectionToggle: (() -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = ClearScanSpacing.minTouchTarget)
                .animatedClickable(
                    haptic = ClearScanHaptic.Selection,
                    scaleDown = ClearScanMotion.cardPressScale,
                    onClick = {
                        if (selectionMode) {
                            onSelectionToggle?.invoke()
                        } else {
                            onClick()
                        }
                    }
                )
                .padding(horizontal = ClearScanSpacing.lg, vertical = ClearScanSpacing.rowVertical),
            horizontalArrangement = Arrangement.spacedBy(ClearScanSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onSelectionToggle?.invoke() }
                )
            }

            DocumentThumbnail(
                document = document,
                modifier = Modifier
                    .size(56.dp)
                    .clip(MaterialTheme.shapes.small),
                placeholderIconSize = 28.dp
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = document.title,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (document.isFavorite) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Favorite",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = "${document.pageCount} page${if (document.pageCount == 1) "" else "s"} · ${dateFormatter.format(document.createdAt.atZone(ZoneId.systemDefault()))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OcrStatusChip(status = document.ocrStatus)
                    if (isDuplicate) {
                        DuplicateBadge()
                    }
                }
                if (document.tags.isNotEmpty()) {
                    TagChipRow(tags = document.tags.take(3))
                }
            }

            if (!selectionMode) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (showDivider) {
            GroupedRowDivider()
        }
    }
}

@Composable
private fun DuplicateBadge() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.ContentCopy,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Text(
            text = "Possible duplicate",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
internal fun DocumentThumbnail(
    document: ScanDocument,
    modifier: Modifier = Modifier,
    placeholderIconSize: Dp = 32.dp
) {
    val context = LocalContext.current
    val thumbnailPath = document.pageImagePaths.firstOrNull()

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (thumbnailPath != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(File(thumbnailPath))
                    .crossfade(true)
                    .build(),
                contentDescription = "Preview of ${document.title}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(placeholderIconSize)
            )
        }
    }
}

@Composable
fun DocumentThumbnailHero(
    document: ScanDocument,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = ClearScanElevation.raised
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f),
            contentAlignment = Alignment.Center
        ) {
            DocumentThumbnail(
                document = document,
                modifier = Modifier.fillMaxSize(),
                placeholderIconSize = 48.dp
            )
        }
    }
}
