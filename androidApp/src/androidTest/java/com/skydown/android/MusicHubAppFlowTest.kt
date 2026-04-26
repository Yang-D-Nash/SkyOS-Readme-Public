package com.nash.skyos

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
    fun musicCatalogArtistButtonsVisibleFromHub() {
        composeRule.waitUntilAtLeastOneExists(hasTestTag("music.hub.songs.open"), timeoutMillis = 15_000)
        composeRule.onNodeWithTag("music.hub.songs.open").performClick()

        composeRule.waitUntilAtLeastOneExists(hasTestTag("music.catalog.root"), timeoutMillis = 15_000)
        composeRule.waitUntilAtLeastOneExists(hasTestTag("music.artist.open_page.JANNO"), timeoutMillis = 15_000)
        composeRule.onNodeWithTag("music.artist.open_page.JANNO").performClick()

        assertTrue(semanticsCount("music.catalog.root") > 0)
        assertTrue(semanticsCount("music.artist.open_page.JANNO") > 0)
    }

    private fun semanticsCount(tag: String): Int = composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().size

    private fun resolveActivity(rule: ActivityScenarioRule<MainActivity>): MainActivity {
        var activity: MainActivity? = null
        rule.scenario.onActivity { activity = it }
        return requireNotNull(activity)
    }
}
