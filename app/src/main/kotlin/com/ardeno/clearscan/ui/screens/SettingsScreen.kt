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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.Switch
import androidx.compose.material.icons.outlined.AutoMode
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Tune
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
import com.ardeno.clearscan.BuildConfig
import com.ardeno.clearscan.data.SelfHostConfig
import com.ardeno.clearscan.ocr.OcrLanguage
import com.ardeno.clearscan.ui.components.GroupedRowDivider
import com.ardeno.clearscan.ui.components.GroupedSection
import com.ardeno.clearscan.ui.components.OcrLanguagePicker
import com.ardeno.clearscan.ui.components.PrivacyBadgeRow
import com.ardeno.clearscan.ui.theme.ClearScanSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vaultEnabled: Boolean,
    benchmarkSummary: String?,
    isBackupRunning: Boolean,
    autoPageTurnEnabled: Boolean,
    imageEnhancementEnabled: Boolean,
    defaultOcrLanguage: OcrLanguage,
    selfHostConfig: SelfHostConfig,
    onSelfHostConfigChange: (SelfHostConfig) -> Unit,
    onSaveSelfHostConfig: () -> Unit,
    onToggleVault: () -> Unit,
    onLockVault: () -> Unit,
    onRunOcrBenchmark: () -> Unit,
    onOpenPrivacyDashboard: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onAutoPageTurnChange: (Boolean) -> Unit,
    onImageEnhancementChange: (Boolean) -> Unit,
    onDefaultOcrLanguageChange: (OcrLanguage) -> Unit,
    passphraseBackupEnabled: Boolean,
    wifiOnlySelfHostUpload: Boolean,
    onPassphraseBackupChange: (Boolean) -> Unit,
    onWifiOnlySelfHostUploadChange: (Boolean) -> Unit,
    isUpdateChecking: Boolean,
    onCheckForAppUpdate: () -> Unit,
    onBackClick: () -> Unit = {},
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
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.defaultMinSize(
                            minWidth = ClearScanSpacing.minTouchTarget,
                            minHeight = ClearScanSpacing.minTouchTarget
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back"
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
                .padding(vertical = ClearScanSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(ClearScanSpacing.xxl)
        ) {
            GroupedSection(
                title = "Capture",
                footer = "All intelligence runs on-device. No cloud AI is used."
            ) {
                CaptureSettingsSection(
                    autoPageTurnEnabled = autoPageTurnEnabled,
                    imageEnhancementEnabled = imageEnhancementEnabled,
                    onAutoPageTurnChange = onAutoPageTurnChange,
                    onImageEnhancementChange = onImageEnhancementChange
                )
                GroupedRowDivider(startIndent = 16.dp)
                OcrLanguageSettingsSection(
                    defaultOcrLanguage = defaultOcrLanguage,
                    onDefaultOcrLanguageChange = onDefaultOcrLanguageChange
                )
            }

            GroupedSection(title = "Security") {
                VaultSettingsRow(
                    vaultEnabled = vaultEnabled,
                    onToggleVault = onToggleVault,
                    onLockVault = onLockVault
                )
            }

            GroupedSection(
                title = "Backup",
                footer = "Exports an encrypted backup via the system file picker. Restore replaces local scans on this device."
            ) {
                BackupGroupedRow(
                    isBackupRunning = isBackupRunning,
                    passphraseBackupEnabled = passphraseBackupEnabled,
                    onPassphraseBackupChange = onPassphraseBackupChange,
                    onExportBackup = onExportBackup,
                    onImportBackup = onImportBackup
                )
            }

            GroupedSection(title = "Privacy") {
                PrivacyDashboardEntryRow(onOpenPrivacyDashboard = onOpenPrivacyDashboard)
            }

            GroupedSection(
                title = "Self-host export",
                footer = "Credentials are stored with EncryptedSharedPreferences on this device only."
            ) {
                SelfHostSettingsSection(
                    config = selfHostConfig,
                    wifiOnlySelfHostUpload = wifiOnlySelfHostUpload,
                    onWifiOnlySelfHostUploadChange = onWifiOnlySelfHostUploadChange,
                    onConfigChange = onSelfHostConfigChange,
                    onSave = onSaveSelfHostConfig
                )
            }

            GroupedSection(
                title = "App updates",
                footer = "Checks GitHub Releases only when you tap the button. No background tracking."
            ) {
                AppUpdateSettingsRow(
                    installedVersionName = BuildConfig.VERSION_NAME,
                    isChecking = isUpdateChecking,
                    onCheckForAppUpdate = onCheckForAppUpdate
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
                text = "ClearScan v${BuildConfig.VERSION_NAME} · Local-first · No ads · No subscriptions",
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
                    contentDescription = if (vaultEnabled) "Disable vault" else "Enable vault"
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
private fun BackupGroupedRow(
    isBackupRunning: Boolean,
    passphraseBackupEnabled: Boolean,
    onPassphraseBackupChange: (Boolean) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Encrypted local backup",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Save or restore scans without enabling Android auto-backup.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Passphrase-protected backup",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Restore the same backup on another device with your passphrase.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = passphraseBackupEnabled,
                onCheckedChange = onPassphraseBackupChange,
                enabled = !isBackupRunning
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = onExportBackup,
                enabled = !isBackupRunning,
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(imageVector = Icons.Outlined.Backup, contentDescription = "Export backup")
                Text(modifier = Modifier.padding(start = 8.dp), text = "Export")
            }
            FilledTonalButton(
                onClick = onImportBackup,
                enabled = !isBackupRunning,
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(imageVector = Icons.Outlined.Restore, contentDescription = "Restore backup")
                Text(modifier = Modifier.padding(start = 8.dp), text = "Restore")
            }
        }
    }
}

@Composable
private fun PrivacyDashboardEntryRow(onOpenPrivacyDashboard: () -> Unit) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Privacy dashboard",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Review offline policy, storage location, export audit log, and ad SDK status.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FilledTonalButton(
            onClick = onOpenPrivacyDashboard,
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(imageVector = Icons.Outlined.Shield, contentDescription = "Open privacy dashboard")
            Text(modifier = Modifier.padding(start = 8.dp), text = "Open dashboard")
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
                contentDescription = "Run OCR benchmark"
            )
            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = "Run self-check"
            )
        }
    }
}

@Composable
private fun AppUpdateSettingsRow(
    installedVersionName: String,
    isChecking: Boolean,
    onCheckForAppUpdate: () -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Installed version $installedVersionName",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Download newer APKs from GitHub Releases when a version.json entry is published.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FilledTonalButton(
            onClick = onCheckForAppUpdate,
            enabled = !isChecking,
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(imageVector = Icons.Outlined.SystemUpdate, contentDescription = "Check for updates")
            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = if (isChecking) "Checking…" else "Check for updates"
            )
        }
    }
}

@Composable
private fun CaptureSettingsSection(
    autoPageTurnEnabled: Boolean,
    imageEnhancementEnabled: Boolean,
    onAutoPageTurnChange: (Boolean) -> Unit,
    onImageEnhancementChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CaptureToggleRow(
            title = "Auto page-turn capture",
            description = "Uses CameraX frame analysis to auto-capture when a page flip settles.",
            icon = Icons.Outlined.AutoMode,
            checked = autoPageTurnEnabled,
            onCheckedChange = onAutoPageTurnChange
        )
        CaptureToggleRow(
            title = "Shadow & glare reduction",
            description = "Lightweight on-device tone adjustment before pages are saved.",
            icon = Icons.Outlined.Tune,
            checked = imageEnhancementEnabled,
            onCheckedChange = onImageEnhancementChange
        )
    }
}

@Composable
private fun OcrLanguageSettingsSection(
    defaultOcrLanguage: OcrLanguage,
    onDefaultOcrLanguageChange: (OcrLanguage) -> Unit
) {
    OcrLanguagePicker(
        modifier = Modifier.padding(16.dp),
        selectedLanguage = defaultOcrLanguage,
        onLanguageSelected = onDefaultOcrLanguageChange,
        title = "Default OCR language"
    )
}

@Composable
private fun CaptureToggleRow(
    title: String,
    description: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
