package com.ardeno.clearscan.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ardeno.clearscan.R
import com.ardeno.clearscan.data.SelfHostConfig
import com.ardeno.clearscan.model.SelfHostTargetType

@Composable
fun SelfHostSettingsSection(
    config: SelfHostConfig,
    wifiOnlySelfHostUpload: Boolean,
    onWifiOnlySelfHostUploadChange: (Boolean) -> Unit,
    onConfigChange: (SelfHostConfig) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.self_host_title),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = stringResource(R.string.self_host_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = stringResource(R.string.self_host_enable))
            Switch(
                checked = config.enabled,
                onCheckedChange = { enabled -> onConfigChange(config.copy(enabled = enabled)) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(R.string.self_host_wifi_only))
                Text(
                    text = stringResource(R.string.self_host_wifi_only_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = wifiOnlySelfHostUpload,
                onCheckedChange = onWifiOnlySelfHostUploadChange
            )
        }

        TargetTypeRow(
            selected = config.targetType,
            onSelect = { type -> onConfigChange(config.copy(targetType = type)) }
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = config.baseUrl,
            onValueChange = { value -> onConfigChange(config.copy(baseUrl = value)) },
            singleLine = true,
            label = {
                Text(
                    when (config.targetType) {
                        SelfHostTargetType.WebDav -> stringResource(R.string.self_host_webdav_url_label)
                        SelfHostTargetType.PaperlessNgx -> stringResource(R.string.self_host_paperless_url_label)
                    }
                )
            },
            placeholder = {
                Text(
                    when (config.targetType) {
                        SelfHostTargetType.WebDav ->
                            stringResource(R.string.self_host_webdav_url_placeholder)
                        SelfHostTargetType.PaperlessNgx ->
                            stringResource(R.string.self_host_paperless_url_placeholder)
                    }
                )
            },
            shape = MaterialTheme.shapes.medium
        )

        if (config.targetType == SelfHostTargetType.WebDav) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = config.remoteFolder,
                onValueChange = { value -> onConfigChange(config.copy(remoteFolder = value)) },
                singleLine = true,
                label = { Text(stringResource(R.string.self_host_remote_folder_label)) },
                placeholder = { Text(stringResource(R.string.self_host_remote_folder_placeholder)) },
                shape = MaterialTheme.shapes.medium
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = config.username,
                onValueChange = { value -> onConfigChange(config.copy(username = value)) },
                singleLine = true,
                label = { Text(stringResource(R.string.self_host_username)) },
                shape = MaterialTheme.shapes.medium
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = config.password,
                onValueChange = { value -> onConfigChange(config.copy(password = value)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                label = { Text(stringResource(R.string.self_host_password)) },
                shape = MaterialTheme.shapes.medium
            )
        } else {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = config.apiToken,
                onValueChange = { value -> onConfigChange(config.copy(apiToken = value)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                label = { Text(stringResource(R.string.self_host_api_token)) },
                placeholder = { Text(stringResource(R.string.self_host_api_token_placeholder)) },
                shape = MaterialTheme.shapes.medium
            )
        }

        FilledTonalButton(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(
                Icons.Outlined.CloudUpload,
                contentDescription = stringResource(R.string.self_host_save_content_desc)
            )
            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = stringResource(R.string.self_host_save)
            )
        }
    }
}

@Composable
private fun TargetTypeRow(
    selected: SelfHostTargetType,
    onSelect: (SelfHostTargetType) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilledTonalButton(
            onClick = { onSelect(SelfHostTargetType.WebDav) },
            modifier = Modifier.weight(1f),
            shape = MaterialTheme.shapes.medium,
            enabled = selected != SelfHostTargetType.WebDav
        ) {
            Text(stringResource(R.string.self_host_target_webdav))
        }
        FilledTonalButton(
            onClick = { onSelect(SelfHostTargetType.PaperlessNgx) },
            modifier = Modifier.weight(1f),
            shape = MaterialTheme.shapes.medium,
            enabled = selected != SelfHostTargetType.PaperlessNgx
        ) {
            Text(stringResource(R.string.self_host_target_paperless))
        }
    }
}
