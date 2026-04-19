package com.skydown.android.ui.component

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.skydown.android.data.mediaAttributionContext
import com.skydown.android.ui.screen.openExternalLink

@Composable
@SuppressLint("SetJavaScriptEnabled")
fun OriginalVideoViewerDialog(
    url: String,
    title: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val mediaContext = remember(context) { context.mediaAttributionContext() }
    val directVideoUrl = remember(url) { isLikelyDirectVideoUrl(url) }
    val player = remember(mediaContext, url, directVideoUrl) {
        ExoPlayer.Builder(mediaContext).build().apply {
            playWhenReady = directVideoUrl
        }
    }
    var browserView by remember(url) { mutableStateOf<WebView?>(null) }
    var canGoBack by remember(url) { mutableStateOf(false) }
    var canGoForward by remember(url) { mutableStateOf(false) }

    fun syncBrowserActions(view: WebView?) {
        canGoBack = view?.canGoBack() == true
        canGoForward = view?.canGoForward() == true
    }

    BackHandler(enabled = !directVideoUrl && canGoBack) {
        browserView?.goBack()
    }

    LaunchedEffect(url, directVideoUrl, player) {
        if (directVideoUrl) {
            player.setMediaItem(MediaItem.fromUri(url))
            player.prepare()
            player.play()
        } else {
            player.stop()
            player.clearMediaItems()
        }
    }

    DisposableEffect(player) {
        onDispose {
            player.release()
        }
    }

    DisposableEffect(url, directVideoUrl) {
        onDispose {
            browserView?.destroy()
            browserView = null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag("video.original.viewer.root")
                .background(Color.Black),
        ) {
            if (directVideoUrl) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { playerContext ->
                        PlayerView(playerContext).apply {
                            useController = true
                            this.player = player
                        }
                    },
                    update = { view ->
                        view.player = player
                    },
                )
            } else {
                key(url) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { webContext ->
                            WebView(webContext).apply {
                                browserView = this
                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        syncBrowserActions(view)
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        syncBrowserActions(view)
                                    }

                                    override fun doUpdateVisitedHistory(
                                        view: WebView?,
                                        url: String?,
                                        isReload: Boolean,
                                    ) {
                                        syncBrowserActions(view)
                                    }
                                }
                                webChromeClient = WebChromeClient()
                                CookieManager.getInstance().setAcceptCookie(true)
                                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                                // Drive/MEGA provider pages require JavaScript to render sign-in and playback flows.
                                settings.javaScriptEnabled = true
                                settings.mediaPlaybackRequiresUserGesture = false
                                settings.domStorageEnabled = true
                                settings.javaScriptCanOpenWindowsAutomatically = true
                                settings.loadsImagesAutomatically = true
                                settings.useWideViewPort = true
                                settings.loadWithOverviewMode = true
                                setBackgroundColor(android.graphics.Color.BLACK)
                                loadUrl(url)
                            }
                        },
                        update = { view ->
                            browserView = view
                            syncBrowserActions(view)
                        },
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.82f),
                                Color.Black.copy(alpha = 0.52f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )

            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 12.dp)
                    .background(Color.Black.copy(alpha = 0.34f), RoundedCornerShape(28.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = title.ifBlank { "Original" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Text(
                    text = if (directVideoUrl) {
                        "Direkt in der In-App-Ansicht. Schliessen bringt dich direkt zurueck."
                    } else {
                        "Web-Ansicht mit sichtbaren Aktionen fuer Zurueck, Weiter und Schliessen."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f),
                )

                if (!directVideoUrl) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        BrandActionButton(
                            text = "Zurueck",
                            onClick = {
                                browserView?.goBack()
                                syncBrowserActions(browserView)
                            },
                            accent = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.weight(1f),
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            enabled = canGoBack,
                            filled = false,
                            compact = true,
                        )
                        BrandActionButton(
                            text = "Weiter",
                            onClick = {
                                browserView?.goForward()
                                syncBrowserActions(browserView)
                            },
                            accent = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.weight(1f),
                            icon = Icons.AutoMirrored.Filled.ArrowForward,
                            enabled = canGoForward,
                            filled = false,
                            compact = true,
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    BrandActionButton(
                        text = "Extern",
                        onClick = { openExternalLink(context, browserView?.url ?: url) },
                        accent = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Language,
                        filled = false,
                        compact = true,
                    )
                    BrandActionButton(
                        text = "Schliessen",
                        onClick = onDismiss,
                        accent = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("video.original.viewer.close"),
                        icon = Icons.Default.Close,
                        compact = true,
                    )
                }
            }
        }
    }
}

private fun isLikelyDirectVideoUrl(rawValue: String): Boolean {
    val normalized = rawValue
        .substringBefore('?')
        .substringBefore('#')
        .lowercase()
    return normalized.endsWith(".mp4") ||
        normalized.endsWith(".mov") ||
        normalized.endsWith(".m4v") ||
        normalized.endsWith(".webm") ||
        normalized.endsWith(".m3u8")
}
