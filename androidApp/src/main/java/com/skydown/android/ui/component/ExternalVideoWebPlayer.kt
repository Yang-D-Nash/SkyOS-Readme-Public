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

                if (playerSource.html != null) {
                    loadDataWithBaseURL(
                        playerSource.baseUrl,
                        playerSource.html,
                        "text/html",
                        "utf-8",
                        null,
                    )
                    tag = playerSource.renderKey
                } else if (playerSource.url != null) {
                    loadUrl(playerSource.url)
                    tag = playerSource.renderKey
                }
            }
        },
        update = { webView ->
            if (webView.tag != playerSource.renderKey) {
                webView.tag = playerSource.renderKey
                if (playerSource.html != null) {
                    webView.loadDataWithBaseURL(
                        playerSource.baseUrl,
                        playerSource.html,
                        "text/html",
                        "utf-8",
                        null,
                    )
                } else if (playerSource.url != null && webView.url != playerSource.url) {
                    webView.loadUrl(playerSource.url)
                }
            }
        },
    )
}

private data class ExternalVideoWebPlayerSource(
    val url: String?,
    val html: String?,
    val baseUrl: String?,
    val renderKey: String,
)

private fun externalVideoWebPlayerSource(rawUrl: String): ExternalVideoWebPlayerSource {
    val youtubeEmbedUrl = resolveYouTubeEmbedUrl(rawUrl)
    if (youtubeEmbedUrl != null) {
        return ExternalVideoWebPlayerSource(
            url = null,
            html = embeddedVideoHtml(youtubeEmbedUrl),
            baseUrl = "https://www.youtube-nocookie.com",
            renderKey = youtubeEmbedUrl,
        )
    }

    return ExternalVideoWebPlayerSource(
        url = rawUrl,
        html = null,
        baseUrl = null,
        renderKey = rawUrl,
    )
}

private fun embeddedVideoHtml(embedUrl: String): String = """
    <!doctype html>
    <html>
      <head>
        <meta charset="utf-8" />
        <meta
          name="viewport"
          content="width=device-width, initial-scale=1, maximum-scale=1, viewport-fit=cover"
        />
        <style>
          html, body {
            margin: 0;
            padding: 0;
            width: 100%;
            height: 100%;
            background: #000;
            overflow: hidden;
          }
          iframe {
            width: 100%;
            height: 100%;
            border: 0;
            background: #000;
          }
        </style>
      </head>
      <body>
        <iframe
          src="$embedUrl"
          title="Embedded video player"
          allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
          referrerpolicy="origin"
          allowfullscreen>
        </iframe>
      </body>
    </html>
""".trimIndent()
