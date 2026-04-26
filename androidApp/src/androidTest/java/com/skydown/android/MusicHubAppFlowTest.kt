package com.nash.skyos

import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class MusicHubAppFlowTest {
    @get:Rule
    val composeRule = AndroidComposeTestRule(
        activityRule = ActivityScenarioRule<MainActivity>(
            Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_UI_TEST_SKIP_INTRO, true)
                putExtra(MainActivity.EXTRA_UI_TEST_START_ROUTE, "music")
                putExtra(MainActivity.EXTRA_UI_TEST_USE_MOCK_MUSIC, true)
            },
        ),
        activityProvider = ::resolveActivity,
    )

    @Test
    fun musicHubScrollSurfaceReachesEnd() {
        composeRule.waitUntilAtLeastOneExists(hasTestTag("music.hub.root"), timeoutMillis = 15_000)

        composeRule.onNodeWithTag("music.hub.root").performScrollToNode(hasTestTag("music.hub.scroll.end"))
        composeRule.waitUntilAtLeastOneExists(hasTestTag("music.hub.scroll.end"), timeoutMillis = 15_000)

        assertTrue(semanticsCount("music.hub.root") > 0)
        assertTrue(semanticsCount("music.hub.scroll.end") > 0)
    }

    @Test
    fun musicHubSpotifyDialogCanBeClosedFromAppFlow() {
        composeRule.waitUntilAtLeastOneExists(hasTestTag("music.hub.songs.open"), timeoutMillis = 15_000)
        composeRule.onNodeWithTag("music.hub.songs.open").performClick()

        composeRule.waitUntilAtLeastOneExists(hasTestTag("music.screen.root"), timeoutMillis = 15_000)
        composeRule.onNodeWithTag("music.screen.root").performScrollToNode(hasTestTag("music.track.spotify.open"))
        composeRule.waitUntilAtLeastOneExists(hasTestTag("music.track.spotify.open"), timeoutMillis = 15_000)

        val spotifyPlayerButton = waitForNode(timeoutMillis = 15_000) { node ->
            node.contentDescription?.toString() == "Spotify Player oeffnen"
        }
        assertNotNull("Spotify Player button should be visible", spotifyPlayerButton)
        assertTrue(
            "Spotify Player button should be clickable",
            spotifyPlayerButton!!.findClickableNode()
                ?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true,
        )

        composeRule.waitUntilAtLeastOneExists(hasTestTag("music.track.spotify.dialog"), timeoutMillis = 15_000)
        composeRule.onNodeWithTag("music.track.spotify.close").performClick()
        composeRule.waitUntilDoesNotExist(hasTestTag("music.track.spotify.dialog"), timeoutMillis = 10_000)

        assertEquals(0, semanticsCount("music.track.spotify.dialog"))
        assertTrue(semanticsCount("music.screen.root") > 0)
    }

    private fun semanticsCount(tag: String): Int = composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().size

    private fun waitForNode(
        timeoutMillis: Long,
        predicate: (AccessibilityNodeInfo) -> Boolean,
    ): AccessibilityNodeInfo? {
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val deadline = SystemClock.uptimeMillis() + timeoutMillis

        while (SystemClock.uptimeMillis() < deadline) {
            uiAutomation.rootInActiveWindow
                ?.findMatchingNode(predicate)
                ?.let { return it }
            SystemClock.sleep(250)
        }

        return null
    }

    private fun AccessibilityNodeInfo.findMatchingNode(
        predicate: (AccessibilityNodeInfo) -> Boolean,
    ): AccessibilityNodeInfo? {
        if (predicate(this)) return this

        for (index in 0 until childCount) {
            getChild(index)?.findMatchingNode(predicate)?.let { return it }
        }

        return null
    }

    private fun AccessibilityNodeInfo.findClickableNode(): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = this
        while (current != null) {
            if (current.isClickable) {
                return current
            }
            current = current.parent
        }

        return null
    }

    private fun resolveActivity(rule: ActivityScenarioRule<MainActivity>): MainActivity {
        var activity: MainActivity? = null
        rule.scenario.onActivity { activity = it }
        return requireNotNull(activity)
    }
}
