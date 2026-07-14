package com.ardeno.clearscan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ardeno.clearscan.R
import com.ardeno.clearscan.model.OcrStatus
import com.ardeno.clearscan.ui.theme.ClearScanSpacing
import com.ardeno.clearscan.ui.theme.PillShape

@Composable
fun OcrStatusChip(status: OcrStatus) {
    val label = stringResource(
        when (status) {
            OcrStatus.NotStarted -> R.string.ocr_status_pending
            OcrStatus.Queued -> R.string.ocr_status_queued
            OcrStatus.Processing -> R.string.ocr_status_scanning
            OcrStatus.Ready -> R.string.ocr_status_searchable
            OcrStatus.Failed -> R.string.ocr_status_failed
        }
    )
    val tint = when (status) {
        OcrStatus.NotStarted -> MaterialTheme.colorScheme.onSurfaceVariant
        OcrStatus.Queued -> MaterialTheme.colorScheme.tertiary
        OcrStatus.Processing -> MaterialTheme.colorScheme.primary
        OcrStatus.Ready -> MaterialTheme.colorScheme.secondary
        OcrStatus.Failed -> MaterialTheme.colorScheme.error
    }

    Text(
        modifier = Modifier
            .background(tint.copy(alpha = 0.12f), PillShape)
            .padding(horizontal = ClearScanSpacing.chipHorizontal, vertical = ClearScanSpacing.chipVertical),
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = tint
    )
}

@Composable
fun PrivacyBadgeRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(ClearScanSpacing.sm)
    ) {
        PrivacyBadge(icon = Icons.Outlined.CloudOff, label = stringResource(R.string.badge_offline))
        PrivacyBadge(icon = Icons.Outlined.Shield, label = stringResource(R.string.badge_no_ads))
        PrivacyBadge(icon = Icons.Outlined.Lock, label = stringResource(R.string.badge_private))
    }
}

@Composable
private fun PrivacyBadge(
    icon: ImageVector,
    label: String
) {
    val tint = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .background(tint.copy(alpha = 0.08f), PillShape)
            .padding(horizontal = ClearScanSpacing.chipHorizontal, vertical = ClearScanSpacing.xs + 1.dp),
        horizontalArrangement = Arrangement.spacedBy(ClearScanSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = tint
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint
        )
    }
}
