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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ardeno.clearscan.model.OcrStatus
import com.ardeno.clearscan.ui.theme.PillShape

@Composable
fun OcrStatusChip(status: OcrStatus) {
    val (label, tint) = when (status) {
        OcrStatus.NotStarted -> "Pending" to MaterialTheme.colorScheme.onSurfaceVariant
        OcrStatus.Queued -> "Queued" to MaterialTheme.colorScheme.tertiary
        OcrStatus.Processing -> "Scanning" to MaterialTheme.colorScheme.primary
        OcrStatus.Ready -> "Searchable" to MaterialTheme.colorScheme.secondary
        OcrStatus.Failed -> "Failed" to MaterialTheme.colorScheme.error
    }

    Text(
        modifier = Modifier
            .background(tint.copy(alpha = 0.12f), PillShape)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = tint
    )
}

@Composable
fun PrivacyBadgeRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PrivacyBadge(icon = Icons.Outlined.CloudOff, label = "Offline")
        PrivacyBadge(icon = Icons.Outlined.Shield, label = "No ads")
        PrivacyBadge(icon = Icons.Outlined.Lock, label = "Private")
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
            .padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
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
