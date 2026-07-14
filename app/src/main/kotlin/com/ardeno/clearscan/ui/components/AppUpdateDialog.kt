package com.ardeno.clearscan.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.ardeno.clearscan.R
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
                text = stringResource(R.string.update_available),
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(
                text = stringResource(
                    R.string.update_version_available,
                    update.versionName,
                    update.releaseNotes
                ),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(
                onClick = onDownload,
                enabled = !isDownloading
            ) {
                Text(
                    if (isDownloading) {
                        stringResource(R.string.update_downloading)
                    } else {
                        stringResource(R.string.update_download_install)
                    }
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDownloading
            ) {
                Text(stringResource(R.string.update_not_now))
            }
        }
    )
}
