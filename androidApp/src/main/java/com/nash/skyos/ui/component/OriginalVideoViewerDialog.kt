package com.nash.skyos.ui.component

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.nash.skyos.data.mediaAttributionContext
import com.nash.skyos.ui.screen.openExternalLink

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
    var isVideoPlaying by remember(url) { mutableStateOf(false) }

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
            isVideoPlaying = true
        } else {
            player.stop()
            player.clearMediaItems()
            isVideoPlaying = false
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                isVideoPlaying = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    isVideoPlaying = false
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    DisposableEffect(url, directVideoUrl) {
        onDispose {
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
                            useController = false
                            this.player = player
                        }
                    },
                    update = { view ->
                        view.player = player
                    },
                )
            } else {
                key(url) {
                    ManagedAndroidWebView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { webContext ->
                            WebView(webContext).apply {
                                browserView = this
                                webViewClient = object : SkydownMediaWebViewClient() {
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
                                applySkydownMediaWebViewDefaults()
                                setBackgroundColor(android.graphics.Color.BLACK)
                                loadSkydownWebUrl(url)
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
                    .height(176.dp)
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

            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = title.ifBlank { "Original" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.94f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (directVideoUrl) "SkyOS Video" else "SkyOS Web",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.68f),
                        maxLines = 1,
                    )
                }

                SkydownFullscreenChromeIconButton(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Video schliessen",
                    onClick = onDismiss,
                    modifier = Modifier.testTag("video.original.viewer.close"),
                )
            }

            if (directVideoUrl) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(end = 18.dp, bottom = 18.dp),
                ) {
                    SkydownFullscreenVideoControlBar(
                        isPlaying = isVideoPlaying,
                        playbackControlsEnabled = true,
                        showsClipNavigation = false,
                        onRewind = { player.seekByAppOffset(-10_000L) },
                        onPlayPause = {
                            if (isVideoPlaying) {
                                player.pause()
                            } else {
                                player.play()
                            }
                            isVideoPlaying = player.isPlaying
                        },
                        onForward = { player.seekByAppOffset(10_000L) },
                        onClose = onDismiss,
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 18.dp, vertical = 18.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.Black.copy(alpha = 0.58f))
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.20f),
                            shape = RoundedCornerShape(999.dp),
                        )
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SkydownFullscreenChromeIconButton(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Zurueck",
                        isEnabled = canGoBack,
                        onClick = {
                            browserView?.goBack()
                            syncBrowserActions(browserView)
                        },
                    )
                    SkydownFullscreenChromeIconButton(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Weiter",
                        isEnabled = canGoForward,
                        onClick = {
                            browserView?.goForward()
                            syncBrowserActions(browserView)
                        },
                    )
                    Spacer(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(Color.White.copy(alpha = 0.18f)),
                    )
                    SkydownFullscreenChromeIconButton(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Extern oeffnen",
                        onClick = { openExternalLink(context, browserView?.url ?: url) },
                    )
                    SkydownFullscreenChromeIconButton(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Video schliessen",
                        isProminent = true,
                        onClick = onDismiss,
                        modifier = Modifier.testTag("video.original.viewer.close.bottom"),
                    )
                }
            }
        }
    }
}

@Composable
internal fun SkydownFullscreenVideoControlBar(
    isPlaying: Boolean,
    playbackControlsEnabled: Boolean,
    showsClipNavigation: Boolean,
    onRewind: () -> Unit,
    onPlayPause: () -> Unit,
    onForward: () -> Unit,
    onClose: () -> Unit,
    canGoToPreviousClip: Boolean = false,
    canGoToNextClip: Boolean = false,
    onPreviousClip: () -> Unit = {},
    onNextClip: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.Black.copy(alpha = 0.58f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.20f),
                shape = RoundedCornerShape(999.dp),
            )
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showsClipNavigation) {
            SkydownFullscreenChromeIconButton(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = "Vorheriger Clip",
                isEnabled = canGoToPreviousClip,
                onClick = onPreviousClip,
            )
        }

        SkydownFullscreenChromeIconButton(
            imageVector = Icons.Default.Replay10,
            contentDescription = "10 Sekunden zurueck",
            isEnabled = playbackControlsEnabled,
            onClick = onRewind,
        )
        SkydownFullscreenChromeIconButton(
            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) "Video pausieren" else "Video abspielen",
            isEnabled = playbackControlsEnabled,
            isProminent = true,
            onClick = onPlayPause,
        )
        SkydownFullscreenChromeIconButton(
            imageVector = Icons.Default.Forward10,
            contentDescription = "10 Sekunden vor",
            isEnabled = playbackControlsEnabled,
            onClick = onForward,
        )

        if (showsClipNavigation) {
            SkydownFullscreenChromeIconButton(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Naechster Clip",
                isEnabled = canGoToNextClip,
                onClick = onNextClip,
            )
        }

        Spacer(
            modifier = Modifier
                .width(1.dp)
                .height(24.dp)
                .background(Color.White.copy(alpha = 0.18f)),
        )
        SkydownFullscreenChromeIconButton(
            imageVector = Icons.Default.Close,
            contentDescription = "Video schliessen",
            isProminent = true,
            onClick = onClose,
        )
    }
}

@Composable
internal fun SkydownFullscreenChromeIconButton(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isProminent: Boolean = false,
    isEnabled: Boolean = true,
) {
    IconButton(
        onClick = onClick,
        enabled = isEnabled,
        modifier = modifier
            .size(if (isProminent) 42.dp else 38.dp)
            .clip(CircleShape)
            .background(
                Color.White.copy(
                    alpha = when {
                        !isEnabled -> 0.05f
                        isProminent -> 0.16f
                        else -> 0.09f
                    },
                ),
            ),
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = Color.White.copy(alpha = if (isEnabled) 0.96f else 0.38f),
            modifier = Modifier.size(if (isProminent) 20.dp else 18.dp),
        )
    }
}

internal fun ExoPlayer.seekByAppOffset(offsetMillis: Long) {
    val rawTarget = currentPosition + offsetMillis
    val durationMillis = duration.takeIf { it > 0L }
    val boundedTarget = if (durationMillis != null) {
        rawTarget.coerceIn(0L, durationMillis)
    } else {
        rawTarget.coerceAtLeast(0L)
    }
    seekTo(boundedTarget)
}

/** Shared with screens that need the same heuristics as the in-app original player (e.g. Video hub hero). */
internal fun isLikelyDirectVideoUrl(rawValue: String): Boolean {
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
