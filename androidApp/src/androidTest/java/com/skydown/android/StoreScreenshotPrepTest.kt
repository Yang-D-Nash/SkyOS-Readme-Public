package com.skydown.android

import android.content.Intent
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class AgentScreenshotPrepTest {
    @get:Rule
    val composeRule = AndroidComposeTestRule(
        activityRule = ActivityScenarioRule<MainActivity>(
            Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_UI_TEST_SKIP_INTRO, true)
                putExtra(MainActivity.EXTRA_UI_TEST_START_ROUTE, "ai")
                putExtra(MainActivity.EXTRA_UI_TEST_SIGNED_IN_USER, true)
                putExtra(MainActivity.EXTRA_UI_TEST_USE_MOCK_AI_VISUAL, true)
            },
        ),
        activityProvider = ::resolveActivity,
    )

    @Test
    fun agentModeIsReadyForStoreScreenshotCapture() {
        composeRule.waitUntilAtLeastOneExists(hasTestTag("ai.hub.mode.agent"), timeoutMillis = 15_000)
        composeRule.onNodeWithTag("ai.hub.mode.agent").performClick()
        composeRule.waitUntilAtLeastOneExists(hasTestTag("agent.screen.root"), timeoutMillis = 15_000)
        composeRule.onNodeWithTag("agent.screen.root").assertIsDisplayed()
    }

    private fun resolveActivity(rule: ActivityScenarioRule<MainActivity>): MainActivity {
        var activity: MainActivity? = null
        rule.scenario.onActivity { activity = it }
        return requireNotNull(activity)
    }
}

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class SettingsScreenshotPrepTest {
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
    fun membershipSectionIsVisibleForStoreScreenshotCapture() {
        composeRule.waitUntilAtLeastOneExists(hasTestTag("settings.root"), timeoutMillis = 15_000)
        composeRule.onNodeWithTag("settings.root").performScrollToNode(hasTestTag("settings.membership.section"))
        composeRule.waitUntilAtLeastOneExists(hasTestTag("settings.membership.section"), timeoutMillis = 15_000)
        composeRule.onNodeWithTag("settings.membership.section").assertIsDisplayed()
    }

    private fun resolveActivity(rule: ActivityScenarioRule<MainActivity>): MainActivity {
        var activity: MainActivity? = null
        rule.scenario.onActivity { activity = it }
        return requireNotNull(activity)
    }
}
