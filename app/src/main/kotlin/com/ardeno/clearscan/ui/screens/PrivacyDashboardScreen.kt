package com.ardeno.clearscan.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ardeno.clearscan.R
import com.ardeno.clearscan.ui.components.GroupedRowDivider
import com.ardeno.clearscan.ui.components.GroupedSection
import com.ardeno.clearscan.ui.theme.ClearScanMotion
import com.ardeno.clearscan.ui.theme.SystemGreen
import com.ardeno.clearscan.ui.theme.SystemRed
import com.ardeno.clearscan.vault.ExportAuditEntry
import com.ardeno.clearscan.vault.PrivacyStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyDashboardScreen(
    status: PrivacyStatus,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.privacy_title),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.action_go_back)
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
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            GroupedSection(title = stringResource(R.string.privacy_section_network)) {
                PrivacyStatusRow(
                    icon = Icons.Outlined.CloudOff,
                    title = stringResource(R.string.privacy_no_background_network),
                    detail = status.networkPolicy,
                    positive = true
                )
            }

            GroupedSection(title = stringResource(R.string.privacy_section_storage)) {
                PrivacyStatusRow(
                    icon = Icons.Outlined.Folder,
                    title = stringResource(R.string.privacy_app_private_storage),
                    detail = status.storageLocation,
                    positive = true
                )
                GroupedRowDivider(startIndent = 16.dp)
                PrivacyStatusRow(
                    icon = Icons.Outlined.Shield,
                    title = stringResource(R.string.privacy_encryption_at_rest),
                    detail = status.encryptionHealthDetails,
                    positive = status.encryptionAtRestEnabled
                )
                GroupedRowDivider(startIndent = 16.dp)
                PrivacyStatusRow(
                    icon = Icons.Outlined.Shield,
                    title = stringResource(R.string.privacy_system_backup),
                    detail = if (status.systemBackupExcluded) {
                        stringResource(R.string.privacy_system_backup_disabled)
                    } else {
                        stringResource(R.string.privacy_system_backup_review)
                    },
                    positive = status.systemBackupExcluded
                )
                GroupedRowDivider(startIndent = 16.dp)
                StorageUsageBar(
                    usedBytes = status.storageUsedBytes,
                    totalBytes = status.storageTotalBytes
                )
            }

            GroupedSection(title = stringResource(R.string.privacy_section_encryption_health)) {
                EncryptionHealthStatus(
                    healthy = status.encryptionAtRestEnabled,
                    details = status.encryptionHealthDetails
                )
            }

            GroupedSection(title = stringResource(R.string.privacy_section_sdk_posture)) {
                AnimatedPrivacyStatusRow(
                    positive = status.adSdkFree,
                    iconPositive = Icons.Outlined.CheckCircle,
                    iconNegative = Icons.Outlined.Warning,
                    titlePositive = stringResource(R.string.privacy_ad_sdk_free),
                    titleNegative = stringResource(R.string.privacy_ad_sdk_check_failed),
                    detail = status.adSdkNotes
                )
            }

            GroupedSection(
                title = stringResource(R.string.privacy_audit_log),
                footer = stringResource(R.string.privacy_audit_log_footer)
            ) {
                if (status.exportAuditEntries.isEmpty()) {
                    Text(
                        text = stringResource(R.string.privacy_no_exports),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    status.exportAuditEntries.forEachIndexed { index, entry ->
                        if (index > 0) {
                            GroupedRowDivider(startIndent = 16.dp)
                        }
                        ExpandableAuditEntry(entry = entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacyStatusRow(
    icon: ImageVector,
    title: String,
    detail: String,
    positive: Boolean
) {
    val tint = if (positive) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(
            text = detail,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StorageUsageBar(
    usedBytes: Long,
    totalBytes: Long
) {
    val fraction = if (totalBytes > 0) {
        (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val usedFormatted = formatBytes(usedBytes)
    val totalFormatted = formatBytes(totalBytes)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Storage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.privacy_document_storage),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = "$usedFormatted / $totalFormatted",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(MaterialTheme.shapes.small),
            color = if (fraction < 0.85f) SystemGreen else SystemRed,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

@Composable
private fun EncryptionHealthStatus(
    healthy: Boolean,
    details: String
) {
    val infiniteTransition = rememberInfiniteTransition(label = "healthPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.privacy_encryption_status),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .alpha(pulseAlpha)
                    .clip(CircleShape)
                    .background(if (healthy) SystemGreen else SystemRed)
            )
            Text(
                text = if (healthy) {
                    stringResource(R.string.privacy_status_active)
                } else {
                    stringResource(R.string.privacy_status_inactive)
                },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (healthy) SystemGreen else SystemRed
            )
        }
        Text(
            text = details,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ExpandableAuditEntry(
    entry: ExportAuditEntry
) {
    var expanded by remember { mutableStateOf(false) }
    val formatter = remember {
        java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
    }
    val timestamp = remember(entry.timestamp) {
        formatter.format(entry.timestamp.atZone(java.time.ZoneId.systemDefault()))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = if (expanded) {
                    stringResource(R.string.action_collapse)
                } else {
                    stringResource(R.string.action_expand)
                },
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.documentTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timestamp,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "\u00B7",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = entry.exportKind,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Icon(
                imageVector = Icons.Outlined.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(animationSpec = ClearScanMotion.springSnappySize) + fadeIn(ClearScanMotion.fadeFast),
            exit = shrinkVertically(animationSpec = ClearScanMotion.springSnappySize) + fadeOut(ClearScanMotion.fadeFast)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp, top = 4.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.privacy_audit_document_id, entry.documentId),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.privacy_audit_export_type, entry.exportKind),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.privacy_audit_timestamp, timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1_000_000_000 -> stringResource(R.string.storage_bytes_gb, bytes / 1_000_000_000.0)
        bytes >= 1_000_000 -> stringResource(R.string.storage_bytes_mb, bytes / 1_000_000.0)
        bytes >= 1_000 -> stringResource(R.string.storage_bytes_kb, bytes / 1_000.0)
        else -> stringResource(R.string.storage_bytes_b, bytes)
    }
}

@Composable
private fun AnimatedPrivacyStatusRow(
    positive: Boolean,
    iconPositive: ImageVector,
    iconNegative: ImageVector,
    titlePositive: String,
    titleNegative: String,
    detail: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Crossfade(
                targetState = positive,
                animationSpec = ClearScanMotion.fadeMedium,
                label = "statusIcon"
            ) { isPositive ->
                Icon(
                    imageVector = if (isPositive) iconPositive else iconNegative,
                    contentDescription = if (isPositive) {
                        stringResource(R.string.privacy_healthy)
                    } else {
                        stringResource(R.string.privacy_warning)
                    },
                    tint = if (isPositive) SystemGreen else SystemRed,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = if (positive) titlePositive else titleNegative,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(
            text = detail,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
