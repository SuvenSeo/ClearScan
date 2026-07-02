package com.ardeno.clearscan

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ardeno.clearscan.data.AppPreferences
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibrarySmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun skipOnboarding() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        AppPreferences(context).setOnboardingComplete()
        composeRule.activityRule.scenario.recreate()
    }

    @Test
    fun libraryScreen_displaysOnLaunch() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("library_screen").assertIsDisplayed()
        composeRule.onNodeWithText("Library").assertIsDisplayed()
    }
}
