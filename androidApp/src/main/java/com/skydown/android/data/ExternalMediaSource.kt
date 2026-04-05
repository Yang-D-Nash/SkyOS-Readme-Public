package com.skydown.android.data

import android.net.Uri
import java.util.Locale

enum class ExternalMediaProvider(val rawValue: String) {
    FIREBASE_STORAGE("firebase_storage"),
    GOOGLE_DRIVE("google_drive"),
    MEGA("mega"),
    EXTERNAL_LINK("external_link"),
    ;

    companion object {
        fun from(rawValue: String?): ExternalMediaProvider {
            return entries.firstOrNull { it.rawValue == rawValue } ?: FIREBASE_STORAGE
        }
    }
}

data class ExternalMediaSource(
    val provider: ExternalMediaProvider,
    val normalizedUrl: String,
    val externalUrl: String,
    val embedUrl: String?,
    val downloadUrl: String?,
    val sourceFileId: String?,
    val mimeType: String,
)

fun resolveExternalVideoSource(rawUrl: String): ExternalMediaSource? =
    resolveExternalMediaSource(rawUrl = rawUrl, mediaKind = ExternalMediaKind.VIDEO)

fun resolveExternalAudioSource(rawUrl: String): ExternalMediaSource? =
    resolveExternalMediaSource(rawUrl = rawUrl, mediaKind = ExternalMediaKind.AUDIO)

private enum class ExternalMediaKind {
    VIDEO,
    AUDIO,
}

private fun resolveExternalMediaSource(
    rawUrl: String,
    mediaKind: ExternalMediaKind,
): ExternalMediaSource? {
    val normalizedUrl = normalizeExternalUrl(rawUrl) ?: return null
    val uri = Uri.parse(normalizedUrl)
    val host = uri.host?.lowercase(Locale.ROOT).orEmpty()

    if (mediaKind == ExternalMediaKind.VIDEO) {
        val youtubeVideoId = resolveYouTubeVideoId(normalizedUrl)
        val youtubeEmbedUrl = resolveYouTubeEmbedUrl(normalizedUrl)
        if (youtubeVideoId != null && youtubeEmbedUrl != null) {
            return ExternalMediaSource(
                provider = ExternalMediaProvider.EXTERNAL_LINK,
                normalizedUrl = normalizedUrl,
                externalUrl = resolveYouTubeExternalUrl(normalizedUrl) ?: normalizedUrl,
                embedUrl = youtubeEmbedUrl,
                downloadUrl = null,
                sourceFileId = youtubeVideoId,
                mimeType = "video/external",
            )
        }
    }

    if (host.contains("drive.google.com") || host.contains("docs.google.com")) {
        val fileId = resolveGoogleDriveFileId(uri, rawUrl) ?: return null
        return ExternalMediaSource(
            provider = ExternalMediaProvider.GOOGLE_DRIVE,
            normalizedUrl = normalizedUrl,
            externalUrl = "https://drive.google.com/file/d/$fileId/view",
            embedUrl = if (mediaKind == ExternalMediaKind.VIDEO) {
                "https://drive.google.com/file/d/$fileId/preview"
            } else {
                null
            },
            downloadUrl = null,
            sourceFileId = fileId,
            mimeType = when (mediaKind) {
                ExternalMediaKind.VIDEO -> "video/external"
                ExternalMediaKind.AUDIO -> "audio/external"
            },
        )
    }

    if (host.contains("mega.nz") || host.contains("mega.io")) {
        return ExternalMediaSource(
            provider = ExternalMediaProvider.MEGA,
            normalizedUrl = normalizedUrl,
            externalUrl = normalizedUrl,
            embedUrl = null,
            downloadUrl = normalizedUrl.takeIf { isDirectMediaUrl(uri, mediaKind) },
            sourceFileId = null,
            mimeType = directMimeType(uri, mediaKind)
                ?: when (mediaKind) {
                    ExternalMediaKind.VIDEO -> "video/external"
                    ExternalMediaKind.AUDIO -> "audio/external"
                },
        )
    }

    val directMimeType = directMimeType(uri, mediaKind)
    val isDirectMedia = isDirectMediaUrl(uri, mediaKind)
    return ExternalMediaSource(
        provider = ExternalMediaProvider.EXTERNAL_LINK,
        normalizedUrl = normalizedUrl,
        externalUrl = normalizedUrl,
        embedUrl = null,
        downloadUrl = normalizedUrl.takeIf { isDirectMedia },
        sourceFileId = null,
        mimeType = directMimeType ?: when (mediaKind) {
            ExternalMediaKind.VIDEO -> "video/external"
            ExternalMediaKind.AUDIO -> "audio/external"
        },
    )
}

private fun normalizeExternalUrl(rawUrl: String): String? {
    val trimmed = rawUrl.trim()
    if (trimmed.isBlank()) return null
    val normalized = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        trimmed
    } else {
        "https://$trimmed"
    }
    return Uri.parse(normalized).host?.takeIf { it.isNotBlank() }?.let { normalized }
}

private fun resolveGoogleDriveFileId(
    uri: Uri,
    rawUrl: String,
): String? {
    uri.getQueryParameter("id")?.takeIf { it.isNotBlank() }?.let { return it }

    val pathSegments = uri.pathSegments
    val fileSegmentIndex = pathSegments.indexOfFirst { it == "d" }
    if (fileSegmentIndex >= 0 && fileSegmentIndex + 1 < pathSegments.size) {
        return pathSegments[fileSegmentIndex + 1]
    }

    val pattern = Regex("""/d/([A-Za-z0-9_-]+)""")
    return pattern.find(rawUrl)?.groupValues?.getOrNull(1)
}

private fun isDirectMediaUrl(
    uri: Uri,
    mediaKind: ExternalMediaKind,
): Boolean {
    return directMimeType(uri, mediaKind) != null
}

private fun directMimeType(
    uri: Uri,
    mediaKind: ExternalMediaKind,
): String? {
    val path = uri.path?.lowercase(Locale.ROOT).orEmpty()
    return when (mediaKind) {
        ExternalMediaKind.VIDEO -> when {
            path.endsWith(".mp4") -> "video/mp4"
            path.endsWith(".mov") -> "video/quicktime"
            path.endsWith(".m4v") -> "video/x-m4v"
            path.endsWith(".webm") -> "video/webm"
            else -> null
        }

        ExternalMediaKind.AUDIO -> when {
            path.endsWith(".mp3") -> "audio/mpeg"
            path.endsWith(".wav") -> "audio/wav"
            path.endsWith(".m4a") -> "audio/mp4"
            path.endsWith(".aac") -> "audio/aac"
            path.endsWith(".flac") -> "audio/flac"
            else -> null
        }
    }
}
