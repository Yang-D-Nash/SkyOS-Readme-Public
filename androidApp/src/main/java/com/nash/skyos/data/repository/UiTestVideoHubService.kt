package com.nash.skyos.data.repository

import com.nash.skyos.data.VideoHubService
import com.nash.skyos.ui.model.VideoHubItem
import com.nash.skyos.ui.model.VideoHubPublicConfig

class UiTestVideoHubService : VideoHubService() {
    override fun observeVideos(
        isAdmin: Boolean,
        onChange: (Result<List<VideoHubItem>>) -> Unit,
    ): () -> Unit {
        onChange(
            Result.success(
                listOf(
                    VideoHubItem(
                        id = "ui-test-video",
                        title = "UI Test Reel",
                        projectName = "Skydown",
                        fileName = "ui-test-video.html",
                        downloadUrl = "",
                        notes = "Reproduzierbarer Android-Video-Hub-Flow fuer den echten Geraetetest.",
                        uploaderName = "UI Test",
                        uploaderEmail = "ui-test@skydown.app",
                        uploaderId = "ui-test-user",
                        mimeType = "text/html",
                        storagePath = "",
                        isPublic = true,
                        isHomeFeatured = true,
                        externalUrl = "about:blank",
                        createdAtMillis = 1_710_000_000_000,
                    ),
                ),
            ),
        )
        return {}
    }

    override fun observePublicConfig(
        onChange: (Result<VideoHubPublicConfig>) -> Unit,
    ): () -> Unit {
        onChange(Result.success(VideoHubPublicConfig.default()))
        return {}
    }
}
