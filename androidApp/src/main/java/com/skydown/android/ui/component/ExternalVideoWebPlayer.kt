package com.skydown.android.ui.component

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.skydown.android.data.resolveYouTubeEmbedUrl

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ExternalVideoWebPlayer(
    url: String,
    modifier: Modifier = Modifier,
) {
    val playerSource = remember(url) { externalVideoWebPlayerSource(url) }

    AndroidView(
        modifier = modifier,
        factory = { playerContext ->
            WebView(playerContext).apply {
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
                settings.javaScriptEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.domStorageEnabled = true
                settings.javaScriptCanOpenWindowsAutomatically = true
                settings.loadsImagesAutomatically = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                setBackgroundColor(android.graphics.Color.BLACK)
                loadUrl(playerSource.url)
                tag = playerSource.renderKey
            }
        },
        update = { webView ->
            if (webView.tag != playerSource.renderKey) {
                webView.tag = playerSource.renderKey
                if (webView.url != playerSource.url) {
                    webView.loadUrl(playerSource.url)
                }
            }
        },
    )
}

private data class ExternalVideoWebPlayerSource(
    val url: String,
    val renderKey: String,
)

private fun externalVideoWebPlayerSource(rawUrl: String): ExternalVideoWebPlayerSource {
    val youtubeEmbedUrl = resolveYouTubeEmbedUrl(rawUrl)
    if (youtubeEmbedUrl != null) {
        return ExternalVideoWebPlayerSource(
            url = youtubeEmbedUrl,
            renderKey = youtubeEmbedUrl,
        )
    }

    return ExternalVideoWebPlayerSource(
        url = rawUrl,
        renderKey = rawUrl,
    )
}
