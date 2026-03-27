package com.skydown.android.ui.component

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import com.skydown.shared.model.Track
import com.skydown.android.ui.theme.SpotifyGreen

@Composable
fun TrackRow(
    track: Track,
    isPlaying: Boolean,
    isSelected: Boolean,
    onSelectTrack: () -> Unit,
    onPlayToggle: () -> Unit,
) {
    val context = LocalContext.current
    var showSpotifyPlayer by rememberSaveable(track.trackId) { mutableStateOf(false) }
    val hasPreview = !track.previewUrl.isNullOrBlank()
    val hasExternalLink = !track.externalUrl.isNullOrBlank()
    val hasDirectSpotifyTrack = resolvedSpotifyTrackId(track.spotifyTrackId, track.externalUrl) != null
    val hasSpotifySearch = hasExternalLink && !hasDirectSpotifyTrack
    val containerColor by animateColorAsState(
        targetValue = if (isPlaying) {
            MaterialTheme.colorScheme.primaryContainer
        } else if (isSelected) {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.82f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
        },
        label = "track_container",
    )
    val contentColor = if (isPlaying) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val borderColor by animateColorAsState(
        targetValue = if (isPlaying) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
        } else if (isSelected) {
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.28f)
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
        },
        label = "track_border",
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable(onClick = onSelectTrack),
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = if (isPlaying) 4.dp else 0.dp,
        shadowElevation = if (isPlaying) 6.dp else 0.dp,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AsyncImage(
                    model = track.artworkUrl100,
                    contentDescription = track.trackName,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                    contentScale = ContentScale.Crop,
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    track.artistName?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it.uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isPlaying) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = track.trackName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = when {
                            hasPreview && hasDirectSpotifyTrack -> "Preview hier oder Spotify Player direkt in der App."
                            hasPreview && hasSpotifySearch -> "Preview hier oder den Song in Spotify suchen."
                            hasPreview -> "Preview direkt in der App."
                            hasDirectSpotifyTrack -> "Spotify Player direkt in der App."
                            hasSpotifySearch -> "Song in Spotify suchen."
                            else -> "Aktuell kein Spotify-Link verfuegbar."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    track.collectionName?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColor.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (hasPreview) {
                            TrackPill(
                                text = if (isPlaying) "Laeuft" else "Preview",
                                isHighlighted = isPlaying,
                            )
                        }
                        if (isSelected && !isPlaying) {
                            TrackPill(
                                text = "Im Player",
                                isHighlighted = false,
                            )
                        }
                        if (hasDirectSpotifyTrack) {
                            TrackPill(
                                text = "Spotify Player",
                                isHighlighted = false,
                                accentColor = SpotifyGreen,
                            )
                        } else if (hasSpotifySearch) {
                            TrackPill(
                                text = "Spotify Suche",
                                isHighlighted = false,
                                accentColor = SpotifyGreen,
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (hasPreview) {
                            FilledTonalButton(
                                onClick = onPlayToggle,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                    contentColor = MaterialTheme.colorScheme.primary,
                                ),
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                )
                                Text(
                                    text = if (isPlaying) "Pause" else "Preview",
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                        }

                        if (hasDirectSpotifyTrack) {
                            OutlinedButton(
                                onClick = {
                                    showSpotifyPlayer = true
                                },
                                modifier = if (hasPreview) Modifier.weight(1f) else Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, SpotifyGreen.copy(alpha = 0.48f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = SpotifyGreen,
                                ),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                )
                                Text(
                                    text = "In App",
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                        } else if (hasSpotifySearch) {
                            OutlinedButton(
                                onClick = {
                                    openTrackInSpotify(
                                        context = context,
                                        spotifyTrackId = track.spotifyTrackId,
                                        externalUrl = track.externalUrl,
                                    )
                                },
                                modifier = if (hasPreview) Modifier.weight(1f) else Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, SpotifyGreen.copy(alpha = 0.48f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = SpotifyGreen,
                                ),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                )
                                Text(
                                    text = "Spotify",
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                        }
                    }

                    if (!hasPreview && !hasExternalLink) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                            Text(
                                text = "Aktuell keine Vorschau verfugbar",
                                color = contentColor.copy(alpha = 0.75f),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.size(0.dp))
        }
    }

    if (showSpotifyPlayer && hasDirectSpotifyTrack) {
        SpotifyEmbedDialog(
            track = track,
            onDismiss = { showSpotifyPlayer = false },
        )
    }
}

@Composable
private fun TrackPill(
    text: String,
    isHighlighted: Boolean,
    accentColor: androidx.compose.ui.graphics.Color? = null,
) {
    val resolvedAccentColor = accentColor ?: MaterialTheme.colorScheme.primary
    val backgroundColor = if (isHighlighted) {
        resolvedAccentColor
    } else {
        MaterialTheme.colorScheme.surface
    }
    val contentColor = if (isHighlighted) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        resolvedAccentColor.copy(alpha = 0.82f)
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(backgroundColor)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
        )
    }
}

fun openTrackInSpotify(
    context: android.content.Context,
    spotifyTrackId: String?,
    externalUrl: String?,
) {
    val spotifyAppUri = spotifyAppUri(spotifyTrackId, externalUrl)
    if (spotifyAppUri != null) {
        val spotifyIntent = Intent(Intent.ACTION_VIEW, spotifyAppUri).setPackage("com.spotify.music")
        try {
            context.startActivity(spotifyIntent)
            return
        } catch (_: ActivityNotFoundException) {
            // Fall through to the public web URL when the Spotify app is not installed.
        }
    }

    val externalUrl = externalUrl ?: return
    context.startActivity(
        Intent(Intent.ACTION_VIEW, Uri.parse(externalUrl)),
    )
}

@Composable
private fun SpotifyEmbedDialog(
    track: Track,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val embedUrl = remember(track.spotifyTrackId, track.externalUrl) {
        spotifyEmbedUri(track.spotifyTrackId, track.externalUrl)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 8.dp,
            shadowElevation = 10.dp,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
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
                            text = track.trackName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = track.artistName ?: "Spotify",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dialog schliessen",
                        )
                    }
                }

                if (embedUrl != null) {
                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp)
                            .clip(RoundedCornerShape(22.dp)),
                        factory = { playerContext ->
                            WebView(playerContext).apply {
                                webViewClient = WebViewClient()
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.mediaPlaybackRequiresUserGesture = false
                                loadUrl(embedUrl.toString())
                            }
                        },
                        update = { webView ->
                            if (webView.url != embedUrl.toString()) {
                                webView.loadUrl(embedUrl.toString())
                            }
                        },
                    )
                } else {
                    Text(
                        text = "Der Spotify Player konnte fuer diesen Track nicht aufgebaut werden.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    FilledTonalButton(
                        onClick = {
                            openTrackInSpotify(
                                context = context,
                                spotifyTrackId = track.spotifyTrackId,
                                externalUrl = track.externalUrl,
                            )
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                        )
                        Text(
                            text = "Spotify App",
                            modifier = Modifier.padding(start = 8.dp),
                        )
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
}

private fun spotifyAppUri(
    spotifyTrackId: String?,
    externalUrl: String?,
): Uri? {
    val trackId = resolvedSpotifyTrackId(spotifyTrackId, externalUrl) ?: return null
    return Uri.parse("spotify:track:$trackId")
}

private fun spotifyEmbedUri(
    spotifyTrackId: String?,
    externalUrl: String?,
): Uri? {
    val trackId = resolvedSpotifyTrackId(spotifyTrackId, externalUrl) ?: return null
    return Uri.parse("https://open.spotify.com/embed/track/$trackId?utm_source=generator")
}

private fun resolvedSpotifyTrackId(
    spotifyTrackId: String?,
    externalUrl: String?,
): String? {
    if (!spotifyTrackId.isNullOrBlank()) return spotifyTrackId
    if (externalUrl.isNullOrBlank()) return null
    val parsed = Uri.parse(externalUrl)
    val segments = parsed.pathSegments
    val trackIndex = segments.indexOf("track")
    return if (trackIndex != -1 && trackIndex + 1 < segments.size) {
        segments[trackIndex + 1]
    } else {
        null
    }
}
