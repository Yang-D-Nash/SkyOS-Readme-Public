package com.nash.skyos

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class SettingsLegalSmokeTest {
    @get:Rule
    val composeRule = AndroidComposeTestRule(
        activityRule = ActivityScenarioRule<MainActivity>(
            Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_UI_TEST_SKIP_INTRO, true)
                putExtra(MainActivity.EXTRA_UI_TEST_START_ROUTE, "settings")
                putExtra(MainActivity.EXTRA_UI_TEST_SIGNED_IN_USER, true)
            },
        ),
        activityProvider = ::resolveActivity,
    )

    @Test
    fun imprintInfoOpensWithoutDeveloperPlaceholders() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val legalEntry = context.getString(R.string.settings_legal_imprint)
        val transparencyNote = context.getString(R.string.legal_ui_transparency_note)

        waitForTag("settings.root")
        composeRule.onNodeWithTag("settings.root").performScrollToNode(hasText(legalEntry))
        composeRule.onNodeWithText(legalEntry).performClick()

        composeRule.waitUntilAtLeastOneExists(hasText(transparencyNote), timeoutMillis = 15_000)
        composeRule.waitForIdle()
        SystemClock.sleep(800)

        assertVisibleTextMissing("TODO")
        assertVisibleTextMissing("Legal sign-off")
        assertVisibleTextMissing("Rechtliche Freigabe")
        assertVisibleTextMissing("before relying")
        assertVisibleTextMissing("support@example.com", substring = false)

        captureScreenshot("08-android-legal-imprint")
    }

    private fun waitForTag(tag: String) {
        composeRule.waitUntilAtLeastOneExists(hasTestTag(tag), timeoutMillis = 15_000)
        composeRule.waitForIdle()
        SystemClock.sleep(700)
    }

    private fun assertVisibleTextMissing(text: String, substring: Boolean = true) {
        val nodes = composeRule
            .onAllNodesWithText(text, substring = substring, useUnmergedTree = true)
            .fetchSemanticsNodes(atLeastOneRootRequired = false)
        assertTrue("Visible legal content should not contain \"$text\".", nodes.isEmpty())
    }

    private fun captureScreenshot(name: String) {
        composeRule.waitForIdle()
        SystemClock.sleep(900)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$name.png")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                "${Environment.DIRECTORY_DOWNLOADS}/SkyOSStoreScreenshots",
            )
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = requireNotNull(
            context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues),
        ) {
            "Failed to create MediaStore entry for $name."
        }

        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        }

        val finalizeValues = ContentValues().apply {
            put(MediaStore.MediaColumns.IS_PENDING, 0)
        }
        context.contentResolver.update(uri, finalizeValues, null, null)
    }

    private fun resolveActivity(rule: ActivityScenarioRule<MainActivity>): MainActivity {
        var activity: MainActivity? = null
        rule.scenario.onActivity { activity = it }
        return requireNotNull(activity)
    }
}
