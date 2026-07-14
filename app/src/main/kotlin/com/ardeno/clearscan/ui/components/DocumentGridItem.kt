package com.ardeno.clearscan.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ardeno.clearscan.R
import com.ardeno.clearscan.model.ScanDocument
import com.ardeno.clearscan.ui.theme.CardShape
import com.ardeno.clearscan.ui.theme.ClearScanElevation
import com.ardeno.clearscan.ui.theme.ClearScanMotion
import com.ardeno.clearscan.ui.theme.ClearScanSpacing

@Composable
fun DocumentGridItem(
    document: ScanDocument,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectionMode: Boolean = false,
    isSelected: Boolean = false,
    isDuplicate: Boolean = false,
    onSelectionToggle: (() -> Unit)? = null
) {
    val pageCountLabel = pluralStringResource(
        R.plurals.document_page_count,
        document.pageCount,
        document.pageCount
    )
    val isDark = isSystemInDarkTheme()
    val cardElevation = if (isDark) ClearScanElevation.none else ClearScanElevation.card

    Column(
        modifier = modifier
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
            .padding(ClearScanSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(ClearScanSpacing.sm)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = CardShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = cardElevation
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f),
                contentAlignment = Alignment.Center
            ) {
                DocumentThumbnail(
                    document = document,
                    modifier = Modifier.fillMaxSize(),
                    placeholderIconSize = 40.dp
                )
                if (selectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onSelectionToggle?.invoke() },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(ClearScanSpacing.xs)
                    )
                }
                if (document.isFavorite) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = stringResource(R.string.document_favorite),
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(ClearScanSpacing.sm)
                            .size(18.dp)
                    )
                }
                if (isDuplicate) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = stringResource(R.string.document_possible_duplicate),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(ClearScanSpacing.sm)
                            .size(16.dp)
                    )
                }
            }
        }

        Text(
            text = document.title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = pageCountLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
