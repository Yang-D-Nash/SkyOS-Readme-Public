package com.nash.skyos

import android.content.Intent
import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class StoreScreenshotCaptureTest {
    @get:Rule
    val composeRule = AndroidComposeTestRule(
        activityRule = ActivityScenarioRule<MainActivity>(
            Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_UI_TEST_SKIP_INTRO, true)
                putExtra(MainActivity.EXTRA_UI_TEST_START_ROUTE, "ai")
                putExtra(MainActivity.EXTRA_UI_TEST_SIGNED_IN_USER, true)
                putExtra(MainActivity.EXTRA_UI_TEST_USE_MOCK_MERCH, true)
                putExtra(MainActivity.EXTRA_UI_TEST_USE_MOCK_MUSIC, true)
                putExtra(MainActivity.EXTRA_UI_TEST_USE_MOCK_VIDEO_HUB, true)
                putExtra(MainActivity.EXTRA_UI_TEST_USE_MOCK_AI_VISUAL, true)
            },
        ),
        activityProvider = ::resolveActivity,
    )

    @Test
    fun captureStoreScreenshotSet() {
        prepareOutputDirectory()

        waitForTag("ai.screen.root")
        composeRule.onNodeWithTag("ai.composer.mode.visual").performClick()
        composeRule.onNodeWithTag("ai.composer.input").performTextInput(
            "Cinematic artist portrait with moody lighting",
        )
        composeRule.onNodeWithTag("ai.composer.send").performClick()
        waitForTag("ai.message.visual")
        composeRule.onNodeWithTag("ai.message.list").performScrollToNode(hasTestTag("ai.message.save"))
        waitForTag("ai.message.save")
        captureScreenshot("02-ai")

        composeRule.onNodeWithTag("ai.hub.mode.agent").performClick()
        waitForTag("agent.screen.root")
        captureScreenshot("03-agent")

        composeRule.onNodeWithTag("bottomDock.home").performClick()
        waitForTag("home.root")
        captureScreenshot("01-home")

        composeRule.onNodeWithTag("bottomDock.music").performClick()
        waitForTag("music.hub.songs.open")
        captureScreenshot("04-music")

        composeRule.onNodeWithTag("bottomDock.shop").performClick()
        waitForTag("shop.root")
        captureScreenshot("05-merch")

        composeRule.onNodeWithTag("bottomDock.video").performClick()
        waitForTag("video.hub.root")
        captureScreenshot("06-video")

        composeRule.onNodeWithTag("app.topbar.settings").performClick()
        waitForTag("settings.root")
        composeRule.onNodeWithTag("settings.root").performScrollToNode(hasTestTag("settings.membership.section"))
        waitForTag("settings.membership.section")
        captureScreenshot("07-membership")
    }

    private fun waitForTag(tag: String) {
        composeRule.waitUntilAtLeastOneExists(hasTestTag(tag), timeoutMillis = 15_000)
        composeRule.waitForIdle()
        SystemClock.sleep(700)
    }

    private fun prepareOutputDirectory() {
        // Cleanup is handled host-side before the run so the capture test only writes fresh assets.
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
