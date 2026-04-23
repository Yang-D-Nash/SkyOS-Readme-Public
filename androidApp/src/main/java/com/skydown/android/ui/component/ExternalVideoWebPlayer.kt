package com.skydown.android.ui.component

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.skydown.android.data.resolveYouTubeEmbedUrl

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
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                settings.javaScriptEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.domStorageEnabled = true
                settings.javaScriptCanOpenWindowsAutomatically = true
                settings.loadsImagesAutomatically = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                setBackgroundColor(android.graphics.Color.BLACK)
                if (playerSource.headers.isEmpty()) {
                    loadUrl(playerSource.url)
                } else {
                    loadUrl(playerSource.url, playerSource.headers)
                }
                tag = playerSource.renderKey
            }
        },
        update = { webView ->
            if (webView.tag != playerSource.renderKey) {
                webView.tag = playerSource.renderKey
                if (webView.url != playerSource.url) {
                    if (playerSource.headers.isEmpty()) {
                        webView.loadUrl(playerSource.url)
                    } else {
                        webView.loadUrl(playerSource.url, playerSource.headers)
                    }
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
