package com.ardeno.clearscan.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.ardeno.clearscan.ui.components.PrivacyBadgeRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vaultEnabled: Boolean,
    benchmarkSummary: String?,
    onToggleVault: () -> Unit,
    onLockVault: () -> Unit,
    onRunOcrBenchmark: () -> Unit,
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
                        text = "Settings",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
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
            GroupedSection(title = "Security") {
                VaultSettingsRow(
                    vaultEnabled = vaultEnabled,
                    onToggleVault = onToggleVault,
                    onLockVault = onLockVault
                )
            }

            GroupedSection(title = "About ClearScan") {
                AboutGroupedContent()
                GroupedRowDivider(startIndent = 16.dp)
                PrivacyBadgeRow(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            GroupedSection(
                title = "Developer",
                footer = "Run the harness self-check, then add labeled scans for measured OCR accuracy."
            ) {
                BenchmarkGroupedRow(
                    summary = benchmarkSummary,
                    onRunOcrBenchmark = onRunOcrBenchmark
                )
            }

            Text(
                text = "ClearScan v0.2.0 · Local-first · No ads · No subscriptions",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun VaultSettingsRow(
    vaultEnabled: Boolean,
    onToggleVault: () -> Unit,
    onLockVault: () -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Biometric vault",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = if (vaultEnabled) {
                "Your library locks when you leave. Unlock with fingerprint or device PIN."
            } else {
                "Protect your scans behind biometric or device-credential authentication."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(
                onClick = onToggleVault,
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(
                    imageVector = if (vaultEnabled) Icons.Outlined.LockOpen else Icons.Outlined.Lock,
                    contentDescription = null
                )
                Text(
                    modifier = Modifier.padding(start = 8.dp),
                    text = if (vaultEnabled) "Disable vault" else "Enable vault"
                )
            }
            if (vaultEnabled) {
                IconButton(onClick = onLockVault) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = "Lock vault now"
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutGroupedContent() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Built to stay free",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "No ads. No subscriptions. No watermarks. No forced accounts or cloud uploads.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PromiseChip(
                modifier = Modifier.weight(1f),
                label = "Local-first",
                icon = Icons.Outlined.CloudOff
            )
            PromiseChip(
                modifier = Modifier.weight(1f),
                label = "Private",
                icon = Icons.Outlined.Lock
            )
        }
    }
}

@Composable
private fun PromiseChip(
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BenchmarkGroupedRow(
    summary: String?,
    onRunOcrBenchmark: () -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Sinhala / Tamil OCR benchmark",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
        if (summary != null) {
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        FilledTonalButton(
            onClick = onRunOcrBenchmark,
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(
                imageVector = Icons.Outlined.TextFields,
                contentDescription = null
            )
            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = "Run self-check"
            )
        }
    }
}
