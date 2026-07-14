package com.ardeno.clearscan.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ardeno.clearscan.BuildConfig
import com.ardeno.clearscan.R
import com.ardeno.clearscan.data.SelfHostConfig
import com.ardeno.clearscan.image.ScanColorFilter
import com.ardeno.clearscan.ocr.OcrLanguage
import com.ardeno.clearscan.ui.components.GroupedRowDivider
import com.ardeno.clearscan.ui.components.GroupedSection
import com.ardeno.clearscan.ui.components.OcrLanguagePicker
import com.ardeno.clearscan.ui.components.PrivacyBadgeRow
import com.ardeno.clearscan.ui.theme.ClearScanSpacing
import androidx.compose.material3.ButtonDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vaultEnabled: Boolean,
    benchmarkSummary: String?,
    isBackupRunning: Boolean,
    autoPageTurnEnabled: Boolean,
    imageEnhancementEnabled: Boolean,
    scanColorFilter: ScanColorFilter,
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
    onScanColorFilterChange: (ScanColorFilter) -> Unit,
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
                        text = stringResource(R.string.settings_title),
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
                .padding(vertical = ClearScanSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(ClearScanSpacing.xxl)
        ) {
            GroupedSection(
                title = stringResource(R.string.settings_section_capture),
                footer = stringResource(R.string.settings_capture_footer)
            ) {
                CaptureSettingsSection(
                    autoPageTurnEnabled = autoPageTurnEnabled,
                    imageEnhancementEnabled = imageEnhancementEnabled,
                    scanColorFilter = scanColorFilter,
                    onAutoPageTurnChange = onAutoPageTurnChange,
                    onImageEnhancementChange = onImageEnhancementChange,
                    onScanColorFilterChange = onScanColorFilterChange
                )
                GroupedRowDivider(startIndent = 16.dp)
                OcrLanguageSettingsSection(
                    defaultOcrLanguage = defaultOcrLanguage,
                    onDefaultOcrLanguageChange = onDefaultOcrLanguageChange
                )
            }

            GroupedSection(title = stringResource(R.string.settings_section_security)) {
                VaultSettingsRow(
                    vaultEnabled = vaultEnabled,
                    onToggleVault = onToggleVault,
                    onLockVault = onLockVault
                )
            }

            GroupedSection(
                title = stringResource(R.string.backup_title),
                footer = stringResource(R.string.backup_section_footer)
            ) {
                BackupGroupedRow(
                    isBackupRunning = isBackupRunning,
                    passphraseBackupEnabled = passphraseBackupEnabled,
                    onPassphraseBackupChange = onPassphraseBackupChange,
                    onExportBackup = onExportBackup,
                    onImportBackup = onImportBackup
                )
            }

            GroupedSection(title = stringResource(R.string.privacy_title)) {
                PrivacyDashboardEntryRow(onOpenPrivacyDashboard = onOpenPrivacyDashboard)
            }

            GroupedSection(
                title = stringResource(R.string.self_host_title),
                footer = stringResource(R.string.self_host_section_footer)
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
                title = stringResource(R.string.settings_section_app_updates),
                footer = stringResource(R.string.settings_app_updates_footer)
            ) {
                AppUpdateSettingsRow(
                    installedVersionName = BuildConfig.VERSION_NAME,
                    isChecking = isUpdateChecking,
                    onCheckForAppUpdate = onCheckForAppUpdate
                )
            }

            GroupedSection(title = stringResource(R.string.settings_section_about)) {
                AboutGroupedContent()
                GroupedRowDivider(startIndent = 16.dp)
                PrivacyBadgeRow(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            GroupedSection(
                title = stringResource(R.string.settings_section_developer),
                footer = stringResource(R.string.settings_developer_footer)
            ) {
                BenchmarkGroupedRow(
                    summary = benchmarkSummary,
                    onRunOcrBenchmark = onRunOcrBenchmark
                )
            }

            Text(
                text = stringResource(R.string.settings_version_footer, BuildConfig.VERSION_NAME),
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
            text = stringResource(R.string.settings_vault_title),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = if (vaultEnabled) {
                stringResource(R.string.settings_vault_enabled_description)
            } else {
                stringResource(R.string.settings_vault_disabled_description)
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
                    contentDescription = if (vaultEnabled) {
                        stringResource(R.string.settings_vault_disable)
                    } else {
                        stringResource(R.string.settings_vault_enable)
                    }
                )
                Text(
                    modifier = Modifier.padding(start = 8.dp),
                    text = if (vaultEnabled) {
                        stringResource(R.string.settings_vault_disable)
                    } else {
                        stringResource(R.string.settings_vault_enable)
                    }
                )
            }
            if (vaultEnabled) {
                IconButton(onClick = onLockVault) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = stringResource(R.string.settings_vault_lock_now)
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
            text = stringResource(R.string.settings_about_built_free),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = stringResource(R.string.settings_about_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PromiseChip(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.settings_about_local_first),
                icon = Icons.Outlined.CloudOff
            )
            PromiseChip(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.settings_about_private),
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
            text = stringResource(R.string.backup_encrypted_title),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = stringResource(R.string.backup_description),
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
                    text = stringResource(R.string.backup_passphrase_protected),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = stringResource(R.string.backup_passphrase_description),
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
                Icon(
                    imageVector = Icons.Outlined.Backup,
                    contentDescription = stringResource(R.string.backup_export)
                )
                Text(
                    modifier = Modifier.padding(start = 8.dp),
                    text = stringResource(R.string.backup_export_action)
                )
            }
            FilledTonalButton(
                onClick = onImportBackup,
                enabled = !isBackupRunning,
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(
                    imageVector = Icons.Outlined.Restore,
                    contentDescription = stringResource(R.string.backup_restore_content_desc)
                )
                Text(
                    modifier = Modifier.padding(start = 8.dp),
                    text = stringResource(R.string.backup_restore_action)
                )
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
            text = stringResource(R.string.privacy_dashboard_title),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = stringResource(R.string.privacy_dashboard_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FilledTonalButton(
            onClick = onOpenPrivacyDashboard,
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(
                imageVector = Icons.Outlined.Shield,
                contentDescription = stringResource(R.string.privacy_dashboard_open_content_desc)
            )
            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = stringResource(R.string.privacy_dashboard_open)
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
            text = stringResource(R.string.settings_benchmark_title),
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
                contentDescription = stringResource(R.string.settings_benchmark_run_content_desc)
            )
            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = stringResource(R.string.settings_benchmark_run)
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
            text = stringResource(R.string.settings_installed_version, installedVersionName),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = stringResource(R.string.settings_app_update_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FilledTonalButton(
            onClick = onCheckForAppUpdate,
            enabled = !isChecking,
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(
                imageVector = Icons.Outlined.SystemUpdate,
                contentDescription = stringResource(R.string.settings_check_updates_content_desc)
            )
            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = if (isChecking) {
                    stringResource(R.string.settings_checking_updates)
                } else {
                    stringResource(R.string.settings_check_for_updates)
                }
            )
        }
    }
}

@Composable
private fun CaptureSettingsSection(
    autoPageTurnEnabled: Boolean,
    imageEnhancementEnabled: Boolean,
    scanColorFilter: ScanColorFilter,
    onAutoPageTurnChange: (Boolean) -> Unit,
    onImageEnhancementChange: (Boolean) -> Unit,
    onScanColorFilterChange: (ScanColorFilter) -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CaptureToggleRow(
            title = stringResource(R.string.settings_auto_page_turn),
            description = stringResource(R.string.settings_auto_page_turn_description),
            icon = Icons.Outlined.AutoMode,
            checked = autoPageTurnEnabled,
            onCheckedChange = onAutoPageTurnChange
        )
        CaptureToggleRow(
            title = stringResource(R.string.settings_image_enhancement),
            description = stringResource(R.string.settings_image_enhancement_description),
            icon = Icons.Outlined.Tune,
            checked = imageEnhancementEnabled,
            onCheckedChange = onImageEnhancementChange
        )
        if (imageEnhancementEnabled) {
            Text(
                text = stringResource(R.string.settings_scan_color_filter),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.settings_scan_color_filter_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ScanColorFilter.entries.forEach { filter ->
                    val selected = filter == scanColorFilter
                    FilledTonalButton(
                        onClick = { onScanColorFilterChange(filter) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = ClearScanSpacing.minTouchTarget),
                        shape = MaterialTheme.shapes.medium,
                        colors = if (selected) {
                            ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        } else {
                            ButtonDefaults.filledTonalButtonColors()
                        }
                    ) {
                        Text(text = stringResource(filter.labelRes()), modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

private fun ScanColorFilter.labelRes(): Int = when (this) {
    ScanColorFilter.Auto -> R.string.scan_color_filter_auto
    ScanColorFilter.Original -> R.string.scan_color_filter_original
    ScanColorFilter.Grayscale -> R.string.scan_color_filter_grayscale
    ScanColorFilter.HighContrast -> R.string.scan_color_filter_high_contrast
    ScanColorFilter.MagicColor -> R.string.scan_color_filter_magic_color
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
        titleRes = R.string.settings_default_ocr_language
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
