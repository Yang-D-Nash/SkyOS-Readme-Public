package com.nash.skyos.ui.model

import com.nash.skyos.data.ExternalMediaProvider
import com.nash.skyos.data.resolveYouTubeVideoId
import com.skydown.shared.model.Track
import java.util.Date

data class FeaturedVideoHighlight(
    val id: String,
    val title: String,
    val projectName: String,
    val notes: String,
    val downloadUrl: String,
    val externalUrl: String,
    val embedUrl: String,
    val sourceProvider: String,
) {
    val provider: ExternalMediaProvider
        get() = ExternalMediaProvider.from(sourceProvider)

    val nativePlaybackUrl: String
        get() = downloadUrl.takeIf { it.isNotBlank() }
            ?: externalUrl.takeIf(::isDirectHomeVideoPlaybackUrl)
            ?: ""

    val usesEmbeddedPreview: Boolean
        get() = provider != ExternalMediaProvider.FIREBASE_STORAGE &&
            inlineEmbedUrl.isNotBlank() &&
            nativePlaybackUrl.isBlank()

    val supportsInlinePlayback: Boolean
        get() = usesEmbeddedPreview || nativePlaybackUrl.isNotBlank()

    val isYouTubeSource: Boolean
        get() = resolveYouTubeVideoId(embedUrl.ifBlank { externalUrl }) != null

    val inlineEmbedUrl: String
        get() = if (isYouTubeSource) {
            ""
        } else {
            embedUrl.takeIf { it.isNotBlank() }.orEmpty()
        }

    val openUrl: String
        get() = externalUrl.ifBlank { downloadUrl }

    val opensOriginalInApp: Boolean
        get() = openUrl.isNotBlank()

    val originalActionLabel: String
        get() = provider.originalVideoActionLabel

    val originalDestinationDescription: String
        get() = when {
            openUrl.isBlank() -> "Kein Original-Link verfuegbar."
            nativePlaybackUrl.isNotBlank() -> "Dieser Clip startet direkt in der In-App-Ansicht."
            else -> "Dieser Link startet in einer In-App-Webansicht mit Zurueck und Schliessen."
        }
}

private fun isDirectHomeVideoPlaybackUrl(rawValue: String): Boolean {
    val normalizedPath = runCatching {
        android.net.Uri.parse(rawValue.trim()).path.orEmpty().lowercase()
    }.getOrDefault("")
    return normalizedPath.endsWith(".mp4") ||
        normalizedPath.endsWith(".mov") ||
        normalizedPath.endsWith(".m4v") ||
        normalizedPath.endsWith(".webm") ||
        normalizedPath.endsWith(".m3u8")
}

data class HomeUiState(
    val featuredTrack: Track? = null,
    val featuredVideo: FeaturedVideoHighlight? = null,
    val homeTrackMessage: String? = null,
    val homeVideoMessage: String? = null,
    val aiUsageWarning: String? = null,
    val creatorLimitZone: Boolean = false,
    val agentRunning: Boolean = false,
    val workflowWaiting: Boolean = false,
    val commerceSignal: String? = null,
    val syncPaused: Boolean = false,
    val recoverableError: String? = null,
    val newDataAvailable: Boolean = false,
    val contentSignal: String? = null,
    val dueTodayReminders: List<ProductivityReminderItem> = emptyList(),
    val upcomingReminders: List<ProductivityReminderItem> = emptyList(),
    val openTasks: List<ProductivityTaskItem> = emptyList(),
    val recentNotes: List<ProductivityNoteItem> = emptyList(),
)

data class ProductivityReminderItem(
    val id: String,
    val title: String,
    val dueAt: Date?,
)

data class ProductivityTaskItem(
    val id: String,
    val title: String,
    val dueAt: Date?,
)

data class ProductivityNoteItem(
    val id: String,
    val title: String,
    val updatedAt: Date?,
)
