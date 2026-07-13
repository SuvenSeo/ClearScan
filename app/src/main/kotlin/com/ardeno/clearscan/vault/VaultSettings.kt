package com.ardeno.clearscan.vault

import android.content.Context

class VaultSettings(context: Context) {
    private val preferences = context.getSharedPreferences("clearscan-vault", Context.MODE_PRIVATE)

    val isEnabled: Boolean
        get() = preferences.getBoolean(KEY_ENABLED, false)

    fun setEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun getKeyVersion(): Int = preferences.getInt(KEY_VERSION, KEY_VERSION_LEGACY)

    fun setKeyVersion(version: Int) {
        preferences.edit().putInt(KEY_VERSION, version).apply()
    }

    fun getAuthMode(): Int = preferences.getInt(KEY_AUTH_MODE, AUTH_MODE_NONE)

    fun setAuthMode(mode: Int) {
        preferences.edit().putInt(KEY_AUTH_MODE, mode).apply()
    }

    companion object {
        const val KEY_ENABLED = "vault_enabled"
        const val KEY_VERSION = "vault_key_version"
        const val KEY_AUTH_MODE = "vault_auth_mode"

        const val KEY_VERSION_LEGACY = 1
        const val KEY_VERSION_BIOMETRIC = 2

        const val AUTH_MODE_NONE = 0
        const val AUTH_MODE_BIOMETRIC = 1
    }
}
