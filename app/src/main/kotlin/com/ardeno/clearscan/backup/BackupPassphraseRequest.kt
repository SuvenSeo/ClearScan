package com.ardeno.clearscan.backup

import android.net.Uri

enum class BackupPassphraseAction {
    Export,
    Import
}

data class BackupPassphraseRequest(
    val action: BackupPassphraseAction,
    val uri: Uri,
    val confirmPassphrase: Boolean = false
)
