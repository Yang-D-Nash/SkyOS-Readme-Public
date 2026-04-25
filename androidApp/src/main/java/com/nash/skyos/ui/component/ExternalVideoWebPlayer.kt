package com.nash.skyos.ui.component

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.nash.skyos.data.resolveYouTubeEmbedUrl

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ExternalVideoWebPlayer(
    url: String,
    modifier: Modifier = Modifier,
) {
    val playerSource = remember(url) { externalVideoWebPlayerSource(url) }

    ManagedAndroidWebView(
        modifier = modifier,
        factory = { playerContext ->
            WebView(playerContext).apply {
                webViewClient = SkydownMediaWebViewClient()
                webChromeClient = WebChromeClient()
                applySkydownMediaWebViewDefaults()
                setBackgroundColor(android.graphics.Color.BLACK)
                loadSkydownWebUrl(playerSource.url, playerSource.headers)
                tag = playerSource.renderKey
            }
        },
        update = { webView ->
            if (webView.tag != playerSource.renderKey) {
                webView.tag = playerSource.renderKey
                if (webView.url != playerSource.url) {
                    webView.loadSkydownWebUrl(playerSource.url, playerSource.headers)
                }
            }
        },
    )
}

private data class ExternalVideoWebPlayerSource(
    val url: String,
    val headers: Map<String, String>,
    val renderKey: String,
)

private fun externalVideoWebPlayerSource(rawUrl: String): ExternalVideoWebPlayerSource {
    val youtubeEmbedUrl = resolveYouTubeEmbedUrl(rawUrl)
    if (youtubeEmbedUrl != null) {
        return ExternalVideoWebPlayerSource(
            url = youtubeEmbedUrl,
            headers = mapOf(
                "Referer" to "https://www.youtube.com/",
                "Origin" to "https://www.youtube.com",
            ),
            renderKey = youtubeEmbedUrl,
        )
    }

    return ExternalVideoWebPlayerSource(
        url = rawUrl,
        headers = emptyMap(),
        renderKey = rawUrl,
    )
}
