package com.ardeno.clearscan.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import com.ardeno.clearscan.update.AppUpdateInfo

@Composable
fun AppUpdateDialog(
    update: AppUpdateInfo,
    isDownloading: Boolean,
    onDismiss: () -> Unit,
    onDownload: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isDownloading) onDismiss() },
        title = {
            Text(
                text = "Update available",
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(
                text = buildString {
                    append("Version ")
                    append(update.versionName)
                    append(" is available.\n\n")
                    append(update.releaseNotes)
                },
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(
                onClick = onDownload,
                enabled = !isDownloading
            ) {
                Text(if (isDownloading) "Downloading…" else "Download & install")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDownloading
            ) {
                Text("Not now")
            }
        }
    )
}
