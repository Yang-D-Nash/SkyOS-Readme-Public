package com.skydown.android

import android.content.Intent
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class MerchFlowTest {
    @get:Rule
    val composeRule = AndroidComposeTestRule(
        activityRule = ActivityScenarioRule<MainActivity>(
            Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_UI_TEST_SKIP_INTRO, true)
                putExtra(MainActivity.EXTRA_UI_TEST_START_ROUTE, "shop")
                putExtra(MainActivity.EXTRA_UI_TEST_USE_MOCK_MERCH, true)
            },
        ),
        activityProvider = ::resolveActivity,
    )

    @Test
    fun merchFullscreenCanBeClosedAndReturnsToShop() {
        waitForTag("shop.root")
        composeRule.onNodeWithTag("shop.root").performScrollToNode(hasTestTag("shop.merch.row"))
        waitForTag("shop.merch.row")

        composeRule.onNodeWithTag("shop.merch.row").performClick()

        waitForTag("shop.merch.detail.root")
        waitForTag("shop.merch.fullscreen.open")

        composeRule.onNodeWithTag("shop.merch.fullscreen.open").performClick()

        waitForTag("shop.merch.fullscreen.root")
        waitForTag("shop.merch.fullscreen.close")

        composeRule.onNodeWithTag("shop.merch.fullscreen.close").performClick()
        composeRule.waitUntilDoesNotExist(hasTestTag("shop.merch.fullscreen.root"), timeoutMillis = 10_000)
        assertEquals(0, semanticsCount("shop.merch.fullscreen.root"))

        waitForTag("shop.merch.detail.root")
        waitForTag("shop.merch.detail.close")

        composeRule.onNodeWithTag("shop.merch.detail.close").performClick()
        composeRule.waitUntilDoesNotExist(hasTestTag("shop.merch.detail.root"), timeoutMillis = 10_000)
        assertEquals(0, semanticsCount("shop.merch.detail.root"))

        assertTrue(semanticsCount("shop.root") > 0)
    }

    private fun waitForTag(tag: String) {
        composeRule.waitUntilAtLeastOneExists(hasTestTag(tag), timeoutMillis = 15_000)
        assertTrue("Expected semantics tag $tag to exist.", semanticsCount(tag) > 0)
    }

    private fun semanticsCount(tag: String): Int = composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().size

    private fun resolveActivity(rule: ActivityScenarioRule<MainActivity>): MainActivity {
        var activity: MainActivity? = null
        rule.scenario.onActivity { activity = it }
        return requireNotNull(activity)
    }
}
