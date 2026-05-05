package com.nash.skyos

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.nash.skyos.ui.component.OriginalVideoViewerDialog
import com.nash.skyos.ui.component.TrackRow
import com.nash.skyos.ui.model.AiMessage
import com.nash.skyos.ui.model.AiMessageRole
import com.nash.skyos.ui.screen.AiMessageBubble
import com.nash.skyos.ui.theme.SkydownTheme
import com.skydown.shared.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class PlaybackAndAiComponentTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun originalVideoViewerCanBeClosed() {
        var showDialog by mutableStateOf(true)

        composeRule.setContent {
            SkydownTheme(darkTheme = false) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("video.root"),
                ) {
                    if (showDialog) {
                        OriginalVideoViewerDialog(
                            url = "about:blank",
                            title = "Original",
                            onDismiss = { showDialog = false },
                        )
                    }
                }
            }
        }

        composeRule.waitUntilAtLeastOneExists(hasTestTag("video.original.viewer.root"), timeoutMillis = 15_000)
        composeRule.onNodeWithTag("video.original.viewer.close").performClick()
        composeRule.waitUntilDoesNotExist(hasTestTag("video.original.viewer.root"), timeoutMillis = 10_000)

        assertEquals(0, semanticsCount("video.original.viewer.root"))
        assertTrue(semanticsCount("video.root") > 0)
    }

    @Test
    fun spotifyPlayerDialogCanBeClosed() {
        val track = Track(
            trackId = 22,
            artistId = 7,
            spotifyTrackId = "73HRKI6lyEEdBUFpHybBi9",
            artistName = "Skydown",
            trackName = "Night Drive",
            collectionName = "Test Session",
            externalUrl = "https://open.spotify.com/track/73HRKI6lyEEdBUFpHybBi9",
        )

        composeRule.setContent {
            SkydownTheme(darkTheme = false) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("music.root"),
                ) {
                    TrackRow(
                        track = track,
                        isPlaying = false,
                        isSelected = false,
                        onSelectTrack = {},
                    )
                }
            }
        }

        composeRule.waitUntilAtLeastOneExists(hasTestTag("music.track.spotify.open"), timeoutMillis = 15_000)
        composeRule.onNodeWithTag("music.track.spotify.open").performClick()
        composeRule.waitUntilAtLeastOneExists(hasTestTag("music.track.spotify.dialog"), timeoutMillis = 15_000)
        composeRule.onNodeWithTag("music.track.spotify.dialog").assertIsDisplayed()

        composeRule.onNodeWithTag("music.track.spotify.close").performClick()
        composeRule.waitUntilDoesNotExist(hasTestTag("music.track.spotify.dialog"), timeoutMillis = 10_000)

        assertEquals(0, semanticsCount("music.track.spotify.dialog"))
        assertTrue(semanticsCount("music.root") > 0)
    }

    @Test
    fun aiVisualMessageShowsGeneratedImage() {
        val message = AiMessage(
            role = AiMessageRole.Assistant,
            text = "Cinematic frame ready.",
            imageBytes = tinyPngBytes(),
            imageMimeType = "image/png",
        )

        composeRule.setContent {
            SkydownTheme(darkTheme = false) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("ai.root"),
                ) {
                    AiMessageBubble(
                        message = message,
                        compactLayout = true,
                        onFeedback = { _, _ -> },
                        onSaveImage = { _, _ -> },
                    )
                }
            }
        }

        composeRule.waitUntilAtLeastOneExists(hasTestTag("ai.message.bubble"), timeoutMillis = 15_000)
        composeRule.waitUntilAtLeastOneExists(hasTestTag("ai.message.visual"), timeoutMillis = 15_000)
        composeRule.onNodeWithTag("ai.message.visual").assertIsDisplayed()
        composeRule.onNodeWithTag("ai.message.save").assertIsDisplayed()
        composeRule.onNodeWithTag("ai.message.visual").performClick()
        composeRule.waitUntilAtLeastOneExists(hasTestTag("ai.generated_visual.fullscreen"), timeoutMillis = 15_000)
        composeRule.onNodeWithTag("ai.generated_visual.fullscreen").assertIsDisplayed()
    }

    private fun semanticsCount(tag: String): Int = composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().size

    private fun tinyPngBytes(): ByteArray {
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.WHITE)
        }
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        return output.toByteArray()
    }
}
