package com.ardeno.clearscan.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ardeno.clearscan.ui.components.GroupedRowDivider
import com.ardeno.clearscan.ui.components.GroupedSection
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
                        text = "Privacy",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back"
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
            GroupedSection(title = "Network") {
                PrivacyStatusRow(
                    icon = Icons.Outlined.CloudOff,
                    title = "No background network",
                    detail = status.networkPolicy,
                    positive = true
                )
            }

            GroupedSection(title = "Storage") {
                PrivacyStatusRow(
                    icon = Icons.Outlined.Folder,
                    title = "App-private storage",
                    detail = status.storageLocation,
                    positive = true
                )
                GroupedRowDivider(startIndent = 16.dp)
                PrivacyStatusRow(
                    icon = Icons.Outlined.Shield,
                    title = "Encryption at rest",
                    detail = if (status.encryptionAtRestEnabled) {
                        "Document files are encrypted with Android Keystore AES-GCM."
                    } else {
                        "Vault encryption is unavailable on this device."
                    },
                    positive = status.encryptionAtRestEnabled
                )
                GroupedRowDivider(startIndent = 16.dp)
                PrivacyStatusRow(
                    icon = Icons.Outlined.Shield,
                    title = "System backup",
                    detail = if (status.systemBackupExcluded) {
                        "Android auto-backup is disabled. Use explicit backup in Settings."
                    } else {
                        "Review backup rules before release."
                    },
                    positive = status.systemBackupExcluded
                )
            }

            GroupedSection(title = "SDK posture") {
                PrivacyStatusRow(
                    icon = if (status.adSdkFree) Icons.Outlined.CheckCircle else Icons.Outlined.Warning,
                    title = if (status.adSdkFree) "Ad SDK free" else "Ad SDK check failed",
                    detail = status.adSdkNotes,
                    positive = status.adSdkFree
                )
            }

            GroupedSection(
                title = "Export audit log",
                footer = "Only explicit share/export actions are recorded. Nothing leaves the device unless you choose to."
            ) {
                if (status.exportAuditEntries.isEmpty()) {
                    Text(
                        text = "No exports recorded yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    status.exportAuditEntries.forEachIndexed { index, entry ->
                        if (index > 0) {
                            GroupedRowDivider(startIndent = 16.dp)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = entry,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
