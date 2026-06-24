package com.ardeno.clearscan.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.ardeno.clearscan.model.SelfHostTargetType

data class SelfHostConfig(
    val enabled: Boolean = false,
    val targetType: SelfHostTargetType = SelfHostTargetType.WebDav,
    val baseUrl: String = "",
    val remoteFolder: String = "",
    val username: String = "",
    val password: String = "",
    val apiToken: String = ""
) {
    val isConfigured: Boolean
        get() = when (targetType) {
            SelfHostTargetType.WebDav ->
                baseUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()
            SelfHostTargetType.PaperlessNgx ->
                baseUrl.isNotBlank() && apiToken.isNotBlank()
        }
}

class SelfHostSettings(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val preferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun load(): SelfHostConfig = SelfHostConfig(
        enabled = preferences.getBoolean(KEY_ENABLED, false),
        targetType = when (preferences.getString(KEY_TARGET_TYPE, SelfHostTargetType.WebDav.name)) {
            SelfHostTargetType.PaperlessNgx.name -> SelfHostTargetType.PaperlessNgx
            else -> SelfHostTargetType.WebDav
        },
        baseUrl = preferences.getString(KEY_BASE_URL, "").orEmpty(),
        remoteFolder = preferences.getString(KEY_REMOTE_FOLDER, "").orEmpty(),
        username = preferences.getString(KEY_USERNAME, "").orEmpty(),
        password = preferences.getString(KEY_PASSWORD, "").orEmpty(),
        apiToken = preferences.getString(KEY_API_TOKEN, "").orEmpty()
    )

    fun save(config: SelfHostConfig) {
        preferences.edit()
            .putBoolean(KEY_ENABLED, config.enabled)
            .putString(KEY_TARGET_TYPE, config.targetType.name)
            .putString(KEY_BASE_URL, config.baseUrl.trim())
            .putString(KEY_REMOTE_FOLDER, config.remoteFolder.trim().trim('/'))
            .putString(KEY_USERNAME, config.username)
            .putString(KEY_PASSWORD, config.password)
            .putString(KEY_API_TOKEN, config.apiToken)
            .apply()
    }

    fun clearCredentials() {
        preferences.edit()
            .remove(KEY_PASSWORD)
            .remove(KEY_API_TOKEN)
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "clearscan-selfhost"
        const val KEY_ENABLED = "enabled"
        const val KEY_TARGET_TYPE = "target_type"
        const val KEY_BASE_URL = "base_url"
        const val KEY_REMOTE_FOLDER = "remote_folder"
        const val KEY_USERNAME = "username"
        const val KEY_PASSWORD = "password"
        const val KEY_API_TOKEN = "api_token"
    }
}
