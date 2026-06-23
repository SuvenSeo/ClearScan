package com.ardeno.clearscan.data

import android.content.Context
import com.ardeno.clearscan.model.LibraryViewMode

class AppPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val hasCompletedOnboarding: Boolean
        get() = preferences.getBoolean(KEY_ONBOARDING_COMPLETE, false)

    val libraryViewMode: LibraryViewMode
        get() = when (preferences.getString(KEY_VIEW_MODE, LibraryViewMode.List.name)) {
            LibraryViewMode.Grid.name -> LibraryViewMode.Grid
            else -> LibraryViewMode.List
        }

    fun setOnboardingComplete() {
        preferences.edit().putBoolean(KEY_ONBOARDING_COMPLETE, true).apply()
    }

    fun setLibraryViewMode(mode: LibraryViewMode) {
        preferences.edit().putString(KEY_VIEW_MODE, mode.name).apply()
    }

    private companion object {
        const val PREFS_NAME = "clearscan-app"
        const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        const val KEY_VIEW_MODE = "library_view_mode"
    }
}
