package com.nash.skyos

import android.content.Intent
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class OwnerHubAppFlowTest {
    @get:Rule
    val composeRule = AndroidComposeTestRule(
        activityRule = ActivityScenarioRule<MainActivity>(
            Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_UI_TEST_SKIP_INTRO, true)
                putExtra(MainActivity.EXTRA_UI_TEST_START_ROUTE, "settings")
                putExtra(MainActivity.EXTRA_UI_TEST_SIGNED_IN_USER, true)
                putExtra(MainActivity.EXTRA_UI_TEST_PLATFORM_OWNER, true)
            },
        ),
        activityProvider = ::resolveActivity,
    )

    @Test
    fun settingsOwnerRowOpensOwnerHub() {
        composeRule.waitUntilAtLeastOneExists(hasTestTag("settings.root"), timeoutMillis = 20_000)
        composeRule.onNodeWithTag("settings.root").performScrollToNode(hasTestTag("settings.open_owner_hub"))
        composeRule.onNodeWithTag("settings.open_owner_hub").performClick()

        composeRule.waitUntilAtLeastOneExists(hasTestTag("owner.hub.root"), timeoutMillis = 15_000)
        composeRule.onNodeWithTag("owner.hub.root").assertIsDisplayed()
        composeRule.onNodeWithTag("owner.hub.briefing.title").assertIsDisplayed()
        composeRule.onNodeWithTag("owner.hub.briefing.cta").assertIsDisplayed()

        composeRule.onNodeWithTag("owner.hub.briefing.cta").performClick()
        composeRule.waitForIdle()

        composeRule.waitUntilAtLeastOneExists(hasTestTag("agent.screen.root"), timeoutMillis = 25_000)
        composeRule.onNodeWithTag("agent.screen.root").assertIsDisplayed()

        composeRule.waitUntilAtLeastOneExists(hasTestTag("agent.prompt.sheet"), timeoutMillis = 20_000)
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodesWithTag("agent.prompt.draft", useUnmergedTree = true)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .any { node ->
                    val text = node.config.getOrNull(SemanticsProperties.EditableText)?.text.orEmpty()
                    text.contains("SkyOS Daily Briefing")
                }
        }
        val draftText = composeRule.onNodeWithTag("agent.prompt.draft", useUnmergedTree = true)
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsProperties.EditableText)
            ?.text
            .orEmpty()
        assertTrue(draftText.contains("SkyOS Daily Briefing"))
    }

    private fun resolveActivity(rule: ActivityScenarioRule<MainActivity>): MainActivity {
        var activity: MainActivity? = null
        rule.scenario.onActivity { activity = it }
        return requireNotNull(activity)
    }
}
