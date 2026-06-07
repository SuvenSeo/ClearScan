package com.ardeno.clearscan.vault

import android.content.Context

class VaultSettings(context: Context) {
    private val preferences = context.getSharedPreferences("clearscan-vault", Context.MODE_PRIVATE)

    val isEnabled: Boolean
        get() = preferences.getBoolean(KEY_ENABLED, false)

    fun setEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    private companion object {
        const val KEY_ENABLED = "vault_enabled"
    }
}
