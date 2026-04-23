package com.skydown.android

import android.content.Intent
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class AiHubAppFlowTest {
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
    fun aiHubVisualPromptShowsGeneratedImageFromAppFlow() {
        composeRule.waitUntilAtLeastOneExists(hasTestTag("ai.screen.root"), timeoutMillis = 15_000)

        composeRule.onNodeWithTag("ai.composer.mode.visual").performClick()
        composeRule.onNodeWithTag("ai.composer.input").performTextInput(
            "Cinematic artist portrait with moody lighting",
        )
        composeRule.onNodeWithTag("ai.composer.send").performClick()

        composeRule.waitUntilAtLeastOneExists(hasTestTag("ai.message.visual"), timeoutMillis = 15_000)
        composeRule.onNodeWithTag("ai.message.list").performScrollToNode(hasTestTag("ai.message.save"))

        composeRule.onNodeWithTag("ai.message.visual").assertIsDisplayed()
        composeRule.onNodeWithTag("ai.message.save").assertIsDisplayed()
    }

    private fun resolveActivity(rule: ActivityScenarioRule<MainActivity>): MainActivity {
        var activity: MainActivity? = null
        rule.scenario.onActivity { activity = it }
        return requireNotNull(activity)
    }
}
