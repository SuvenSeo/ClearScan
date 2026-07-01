package com.ardeno.clearscan.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ardeno.clearscan.backup.BackupPassphraseAction
import com.ardeno.clearscan.backup.BackupPassphraseRequest

@Composable
fun PassphraseBackupDialog(
    request: BackupPassphraseRequest,
    onDismiss: () -> Unit,
    onSubmit: (passphrase: CharArray, confirmation: CharArray?) -> Unit
) {
    var passphrase by remember(request) { mutableStateOf("") }
    var confirmation by remember(request) { mutableStateOf("") }

    val title = when (request.action) {
        BackupPassphraseAction.Export -> "Set backup passphrase"
        BackupPassphraseAction.Import -> "Enter backup passphrase"
    }
    val confirmEnabled = if (request.confirmPassphrase) {
        passphrase.isNotBlank() && passphrase == confirmation
    } else {
        passphrase.isNotBlank()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = when (request.action) {
                        BackupPassphraseAction.Export ->
                            "This passphrase unlocks the backup on any device. ClearScan cannot recover it if you forget it."
                        BackupPassphraseAction.Import ->
                            "Enter the passphrase used when this backup was exported."
                    }
                )
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Passphrase") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true
                )
                if (request.confirmPassphrase) {
                    OutlinedTextField(
                        value = confirmation,
                        onValueChange = { confirmation = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Confirm passphrase") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSubmit(
                        passphrase.toCharArray(),
                        confirmation.takeIf { request.confirmPassphrase }?.toCharArray()
                    )
                },
                enabled = confirmEnabled
            ) {
                Text(
                    when (request.action) {
                        BackupPassphraseAction.Export -> "Save backup"
                        BackupPassphraseAction.Import -> "Restore"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
