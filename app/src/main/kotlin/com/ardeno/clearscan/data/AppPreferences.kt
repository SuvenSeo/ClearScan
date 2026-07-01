package com.ardeno.clearscan.data

import android.content.Context
import com.ardeno.clearscan.model.LibraryViewMode
import com.ardeno.clearscan.ocr.OcrLanguage

class AppPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val hasCompletedOnboarding: Boolean
        get() = preferences.getBoolean(KEY_ONBOARDING_COMPLETE, false)

    val libraryViewMode: LibraryViewMode
        get() = when (preferences.getString(KEY_VIEW_MODE, LibraryViewMode.List.name)) {
            LibraryViewMode.Grid.name -> LibraryViewMode.Grid
            else -> LibraryViewMode.List
        }

    val defaultOcrLanguage: OcrLanguage
        get() = OcrLanguage.fromName(preferences.getString(KEY_OCR_LANGUAGE, OcrLanguage.Latin.name))

    val autoPageTurnEnabled: Boolean
        get() = preferences.getBoolean(KEY_AUTO_PAGE_TURN, false)

    val imageEnhancementEnabled: Boolean
        get() = preferences.getBoolean(KEY_IMAGE_ENHANCEMENT, true)

    fun setOnboardingComplete() {
        preferences.edit().putBoolean(KEY_ONBOARDING_COMPLETE, true).apply()
    }

    fun setLibraryViewMode(mode: LibraryViewMode) {
        preferences.edit().putString(KEY_VIEW_MODE, mode.name).apply()
    }

    fun setAutoPageTurnEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_AUTO_PAGE_TURN, enabled).apply()
    }

    fun setImageEnhancementEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_IMAGE_ENHANCEMENT, enabled).apply()
    }

    fun setDefaultOcrLanguage(language: OcrLanguage) {
        preferences.edit().putString(KEY_OCR_LANGUAGE, language.name).apply()
    }

    val passphraseBackupEnabled: Boolean
        get() = preferences.getBoolean(KEY_PASSPHRASE_BACKUP, false)

    val wifiOnlySelfHostUpload: Boolean
        get() = preferences.getBoolean(KEY_WIFI_ONLY_SELF_HOST, true)

    fun setPassphraseBackupEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_PASSPHRASE_BACKUP, enabled).apply()
    }

    fun setWifiOnlySelfHostUpload(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_WIFI_ONLY_SELF_HOST, enabled).apply()
    }

    private companion object {
        const val PREFS_NAME = "clearscan-app"
        const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        const val KEY_VIEW_MODE = "library_view_mode"
        const val KEY_AUTO_PAGE_TURN = "auto_page_turn_enabled"
        const val KEY_IMAGE_ENHANCEMENT = "image_enhancement_enabled"
        const val KEY_OCR_LANGUAGE = "default_ocr_language"
        const val KEY_PASSPHRASE_BACKUP = "passphrase_backup_enabled"
        const val KEY_WIFI_ONLY_SELF_HOST = "wifi_only_self_host_upload"
    }
}
