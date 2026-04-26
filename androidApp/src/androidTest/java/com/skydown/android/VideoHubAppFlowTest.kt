package com.nash.skyos

import android.content.Intent
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class VideoHubAppFlowTest {
    @get:Rule
    val composeRule = AndroidComposeTestRule(
        activityRule = ActivityScenarioRule<MainActivity>(
            Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_UI_TEST_SKIP_INTRO, true)
                putExtra(MainActivity.EXTRA_UI_TEST_START_ROUTE, "video")
                putExtra(MainActivity.EXTRA_UI_TEST_USE_MOCK_VIDEO_HUB, true)
            },
        ),
        activityProvider = ::resolveActivity,
    )

    @Test
    fun videoHubPlayerCanBeOpenedFromAppFlow() {
        composeRule.waitUntilAtLeastOneExists(hasTestTag("video.hub.root"), timeoutMillis = 15_000)
        composeRule.onNodeWithTag("video.hub.root").performScrollToNode(hasTestTag("video.hub.player.open"))
        composeRule.waitUntilAtLeastOneExists(hasTestTag("video.hub.player.open"), timeoutMillis = 15_000)

        composeRule.onNodeWithTag("video.hub.player.open").performClick()

        composeRule.waitUntilAtLeastOneExists(hasTestTag("video.reel.viewer.root"), timeoutMillis = 15_000)

        assertTrue(semanticsCount("video.reel.viewer.root") > 0)
        assertTrue(semanticsCount("video.hub.root") > 0)
    }

    private fun semanticsCount(tag: String): Int = composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().size

    private fun resolveActivity(rule: ActivityScenarioRule<MainActivity>): MainActivity {
        var activity: MainActivity? = null
        rule.scenario.onActivity { activity = it }
        return requireNotNull(activity)
    }
}
