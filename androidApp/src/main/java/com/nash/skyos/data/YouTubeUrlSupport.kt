package com.nash.skyos.data

import android.net.Uri
import java.util.Locale

fun normalizeYouTubeUrl(rawUrl: String): String? {
    val trimmed = rawUrl.trim()
    if (trimmed.isBlank()) return null

    val normalized = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        trimmed
    } else {
        "https://$trimmed"
    }

    val host = Uri.parse(normalized).host?.lowercase(Locale.ROOT).orEmpty()
    return normalized.takeIf { isYouTubeHost(host) }
}

fun resolveYouTubeVideoId(rawUrl: String): String? {
    val normalizedUrl = normalizeYouTubeUrl(rawUrl) ?: return null
    val uri = Uri.parse(normalizedUrl)
    val host = uri.host?.lowercase(Locale.ROOT).orEmpty()

    when {
        "youtu.be" in host -> {
            uri.pathSegments.firstOrNull()?.takeIfYouTubeId()?.let { return it }
        }
        uri.path?.contains("/embed/") == true ||
            uri.path?.contains("/shorts/") == true ||
            uri.path?.contains("/live/") == true -> {
            uri.lastPathSegment?.takeIfYouTubeId()?.let { return it }
        }
    }

    uri.getQueryParameter("v")?.takeIfYouTubeId()?.let { return it }
    uri.getQueryParameter("vi")?.takeIfYouTubeId()?.let { return it }

    val pattern = Regex("""(?:(?<=v=)|(?<=vi=)|(?<=/embed/)|(?<=/shorts/)|(?<=youtu\.be/)|(?<=/live/))([A-Za-z0-9_-]{11})""")
    return pattern.find(rawUrl.trim())?.groupValues?.getOrNull(1)?.takeIfYouTubeId()
}

fun resolveYouTubeEmbedUrl(rawUrl: String): String? {
    val videoId = resolveYouTubeVideoId(rawUrl) ?: return null
    return "https://www.youtube-nocookie.com/embed/$videoId?playsinline=1&rel=0&modestbranding=1&controls=1"
}

fun resolveYouTubeExternalUrl(rawUrl: String): String? {
    val videoId = resolveYouTubeVideoId(rawUrl) ?: return null
    return "https://www.youtube.com/watch?v=$videoId"
}

fun resolveYouTubeThumbnailUrl(rawUrl: String): String? {
    val videoId = resolveYouTubeVideoId(rawUrl) ?: return null
    return "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
}

private fun isYouTubeHost(host: String): Boolean {
    return host == "youtu.be" ||
        host.endsWith(".youtu.be") ||
        host == "youtube.com" ||
        host.endsWith(".youtube.com") ||
        host == "youtube-nocookie.com" ||
        host.endsWith(".youtube-nocookie.com")
}

private fun String.takeIfYouTubeId(): String? {
    val trimmed = trim()
    return trimmed.takeIf { it.length == 11 }
}
