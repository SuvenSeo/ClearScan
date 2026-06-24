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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ardeno.clearscan.data.SelfHostConfig
import com.ardeno.clearscan.model.SelfHostTargetType

@Composable
fun SelfHostSettingsSection(
    config: SelfHostConfig,
    onConfigChange: (SelfHostConfig) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Self-host export",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Optional upload to your Nextcloud, WebDAV folder, or paperless-ngx. Nothing leaves this device unless you tap Upload.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Enable self-host export")
            Switch(
                checked = config.enabled,
                onCheckedChange = { enabled -> onConfigChange(config.copy(enabled = enabled)) }
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
                        SelfHostTargetType.WebDav -> "WebDAV base URL"
                        SelfHostTargetType.PaperlessNgx -> "Paperless server URL"
                    }
                )
            },
            placeholder = {
                Text(
                    when (config.targetType) {
                        SelfHostTargetType.WebDav ->
                            "https://cloud.example.com/remote.php/dav/files/you/"
                        SelfHostTargetType.PaperlessNgx ->
                            "https://paperless.example.com"
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
                label = { Text("Remote folder (optional)") },
                placeholder = { Text("ClearScan") },
                shape = MaterialTheme.shapes.medium
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = config.username,
                onValueChange = { value -> onConfigChange(config.copy(username = value)) },
                singleLine = true,
                label = { Text("Username") },
                shape = MaterialTheme.shapes.medium
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = config.password,
                onValueChange = { value -> onConfigChange(config.copy(password = value)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                label = { Text("Password") },
                shape = MaterialTheme.shapes.medium
            )
        } else {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = config.apiToken,
                onValueChange = { value -> onConfigChange(config.copy(apiToken = value)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                label = { Text("API token") },
                placeholder = { Text("paperless API token") },
                shape = MaterialTheme.shapes.medium
            )
        }

        FilledTonalButton(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(Icons.Outlined.CloudUpload, contentDescription = null)
            Text(modifier = Modifier.padding(start = 8.dp), text = "Save self-host settings")
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
            Text("WebDAV")
        }
        FilledTonalButton(
            onClick = { onSelect(SelfHostTargetType.PaperlessNgx) },
            modifier = Modifier.weight(1f),
            shape = MaterialTheme.shapes.medium,
            enabled = selected != SelfHostTargetType.PaperlessNgx
        ) {
            Text("paperless-ngx")
        }
    }
}
