package com.skydown.android.ui.screen

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.skydown.android.data.mediaAttributionContext

@androidx.media3.common.util.UnstableApi
@Composable
fun IntroScreen(
    onFinished: () -> Unit,
) {
    val context = LocalContext.current
    val mediaContext = remember(context) { context.mediaAttributionContext() }
    val currentOnFinished = rememberUpdatedState(onFinished)
    var hasFinished by remember { mutableStateOf(false) }
    val uri = remember(mediaContext) {
        Uri.parse("android.resource://${mediaContext.packageName}/${com.skydown.android.R.raw.intro_launch}")
    }
    val player = remember(mediaContext, uri) {
        ExoPlayer.Builder(mediaContext).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true,
            )
            volume = 1f
            playWhenReady = true
            repeatMode = ExoPlayer.REPEAT_MODE_OFF
            prepare()
        }
    }
    val finishOnce = remember(currentOnFinished.value) {
        {
            if (!hasFinished) {
                hasFinished = true
                currentOnFinished.value()
            }
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    finishOnce()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                finishOnce()
            }
        }

        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                PlayerView(it).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    this.player = player
                }
            },
        )
    }
}
