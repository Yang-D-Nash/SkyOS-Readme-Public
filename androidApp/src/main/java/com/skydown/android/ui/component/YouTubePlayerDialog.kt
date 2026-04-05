package com.skydown.android.ui.component

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import com.skydown.android.data.resolveYouTubeEmbedUrl
import com.skydown.android.data.resolveYouTubeExternalUrl
import com.skydown.android.data.resolveYouTubeVideoId
import com.skydown.android.ui.model.VideoYouTubeItem

@SuppressLint("SetJavaScriptEnabled")
@androidx.compose.runtime.Composable
fun YouTubePlayerDialog(
    item: VideoYouTubeItem,
    onDismiss: () -> Unit,
    onOpenExternal: (String) -> Unit,
) {
    val playerSource = androidx.compose.runtime.remember(item.url) { youtubePlayerSource(item.url) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        SkydownCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    )
                    if (item.subtitle.isNotBlank()) {
                        Text(
                            text = item.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dialog schliessen",
                    )
                }
            }

            if (playerSource != null) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .padding(top = 12.dp),
                    factory = { playerContext ->
                        WebView(playerContext).apply {
                            webViewClient = WebViewClient()
                            webChromeClient = WebChromeClient()
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.mediaPlaybackRequiresUserGesture = false
                            settings.javaScriptCanOpenWindowsAutomatically = true
                            settings.loadsImagesAutomatically = true
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            setBackgroundColor(android.graphics.Color.BLACK)
                            loadDataWithBaseURL(
                                playerSource.baseUrl,
                                playerSource.html,
                                "text/html",
                                "utf-8",
                                null,
                            )
                            tag = playerSource.embedKey
                        }
                    },
                    update = { webView ->
                        if (webView.tag != playerSource.embedKey) {
                            webView.tag = playerSource.embedKey
                            webView.loadDataWithBaseURL(
                                playerSource.baseUrl,
                                playerSource.html,
                                "text/html",
                                "utf-8",
                                null,
                            )
                        }
                    },
                )
            } else {
                Text(
                    text = "Der YouTube Player konnte fuer dieses Video nicht aufgebaut werden.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FilledTonalButton(
                    onClick = { onOpenExternal(playerSource?.externalUrl ?: item.url) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("In YouTube oeffnen")
                }

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Schliessen")
                }
            }
        }
    }
}

private data class YouTubePlayerSource(
    val html: String,
    val baseUrl: String,
    val externalUrl: String,
    val embedKey: String,
)

private fun youtubePlayerSource(rawUrl: String): YouTubePlayerSource? {
    val videoId = resolveYouTubeVideoId(rawUrl) ?: return null
    val embedUrl = resolveYouTubeEmbedUrl(rawUrl) ?: return null
    val html = """
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
              title="YouTube video player"
              allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
              referrerpolicy="origin"
              allowfullscreen>
            </iframe>
          </body>
        </html>
    """.trimIndent()

    return YouTubePlayerSource(
        html = html,
        baseUrl = "https://www.youtube.com",
        externalUrl = resolveYouTubeExternalUrl(rawUrl) ?: "https://www.youtube.com/watch?v=$videoId",
        embedKey = videoId,
    )
}
