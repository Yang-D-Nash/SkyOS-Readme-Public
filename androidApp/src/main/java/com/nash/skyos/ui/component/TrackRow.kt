package com.nash.skyos.ui.component

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import com.nash.skyos.R
import com.nash.skyos.ui.screen.openExternalLink
import com.skydown.shared.model.Track
import com.nash.skyos.ui.theme.SpotifyGreen

/**
 * [Featured] = erster Titel, volle Hierarchie.
 * [Secondary] = zweiter Titel, leicht abgestuft, noch „Set“-Charakter.
 * [Catalog] = ruhiger Katalog-Rest.
 */
enum class TrackRowPresentation {
    Featured,
    Secondary,
    Catalog,
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TrackRow(
    track: Track,
    isPlaying: Boolean,
    isSelected: Boolean,
    onSelectTrack: () -> Unit,
    onPlayToggle: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    presentation: TrackRowPresentation = TrackRowPresentation.Featured,
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var showSpotifyPlayer by rememberSaveable(track.trackId) { mutableStateOf(false) }
    val hasPreview = !track.previewUrl.isNullOrBlank()
    val allowInAppPreview = onPlayToggle != null
    val hasPreviewCta = hasPreview && allowInAppPreview
    val hasExternalLink = !track.externalUrl.isNullOrBlank()
    val hasDirectSpotifyTrack = resolvedSpotifyTrackId(track.spotifyTrackId, track.externalUrl) != null
    val hasSpotifyArtistLink = resolvedSpotifyArtistId(track.spotifyArtistId, track.externalUrl) != null && !hasDirectSpotifyTrack
    val hasSpotifySearch = hasExternalLink && !hasDirectSpotifyTrack && !hasSpotifyArtistLink
    val isFeatured = presentation == TrackRowPresentation.Featured
    val isSecondary = presentation == TrackRowPresentation.Secondary
    val isCatalog = presentation == TrackRowPresentation.Catalog
    val hasCtaOrExternalOptions =
        hasPreviewCta || hasDirectSpotifyTrack || hasSpotifyArtistLink || hasSpotifySearch
    val openSpotifyPlayerContentDescription = stringResource(R.string.track_cd_open_spotify_player)
    val hideStatusPillDuplicates = isFeatured && hasCtaOrExternalOptions
    val artSize = when (presentation) {
        TrackRowPresentation.Catalog -> 52.dp
        TrackRowPresentation.Secondary -> 58.dp
        TrackRowPresentation.Featured -> 64.dp
    }
    val artCorner = when (presentation) {
        TrackRowPresentation.Catalog -> 13.dp
        TrackRowPresentation.Secondary -> 15.dp
        TrackRowPresentation.Featured -> 16.dp
    }
    val selectionTween = remember {
        tween<Color>(durationMillis = SkydownMotionTokens.selectionCrossFadeMillis, easing = FastOutSlowInEasing)
    }
    val playingSurface = isPlaying && allowInAppPreview
    val containerColor by animateColorAsState(
        targetValue = if (playingSurface) {
            MaterialTheme.colorScheme.primaryContainer
        } else if (isSelected) {
            MaterialTheme.colorScheme.secondaryContainer.copy(
                alpha = when (presentation) {
                    TrackRowPresentation.Catalog -> 0.40f
                    TrackRowPresentation.Secondary -> 0.52f
                    TrackRowPresentation.Featured -> 0.62f
                },
            )
        } else {
            MaterialTheme.colorScheme.surface.copy(
                alpha = when (presentation) {
                    TrackRowPresentation.Catalog -> 0.84f
                    TrackRowPresentation.Secondary -> 0.90f
                    TrackRowPresentation.Featured -> 0.96f
                },
            )
        },
        animationSpec = selectionTween,
        label = "track_container",
    )
    val contentColor = if (playingSurface) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val borderColor by animateColorAsState(
        targetValue = if (playingSurface) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
        } else if (isSelected) {
            MaterialTheme.colorScheme.secondary.copy(
                alpha = when (presentation) {
                    TrackRowPresentation.Catalog -> 0.14f
                    TrackRowPresentation.Secondary -> 0.20f
                    TrackRowPresentation.Featured -> 0.28f
                },
            )
        } else {
            MaterialTheme.colorScheme.outline.copy(
                alpha = when (presentation) {
                    TrackRowPresentation.Catalog -> 0.07f
                    TrackRowPresentation.Secondary -> 0.10f
                    TrackRowPresentation.Featured -> 0.14f
                },
            )
        },
        animationSpec = selectionTween,
        label = "track_border",
    )
    val cardShape = when (presentation) {
        TrackRowPresentation.Catalog -> RoundedCornerShape(17.dp)
        TrackRowPresentation.Secondary -> RoundedCornerShape(20.dp)
        TrackRowPresentation.Featured -> RoundedCornerShape(22.dp)
    }
    val innerPad = when (presentation) {
        TrackRowPresentation.Catalog -> 9.dp
        TrackRowPresentation.Secondary -> 11.dp
        TrackRowPresentation.Featured -> 13.dp
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .testTag("music.track.row")
            .clickable(onClick = onSelectTrack),
        shape = cardShape,
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = if (playingSurface) {
            when (presentation) {
                TrackRowPresentation.Catalog -> 0.dp
                TrackRowPresentation.Secondary -> 1.dp
                TrackRowPresentation.Featured -> 2.dp
            }
        } else {
            0.dp
        },
        shadowElevation = if (playingSurface) {
            when (presentation) {
                TrackRowPresentation.Catalog -> 0.dp
                TrackRowPresentation.Secondary -> 2.dp
                TrackRowPresentation.Featured -> 3.dp
            }
        } else {
            0.dp
        },
        border = BorderStroke(
            width = if (isCatalog) 0.5.dp else if (isSecondary) 0.75.dp else 1.dp,
            color = borderColor,
        ),
    ) {
        Column(modifier = Modifier.padding(innerPad)) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(
                    if (isCatalog) 9.dp else 11.dp,
                ),
            ) {
                AsyncImage(
                    model = track.artworkUrl100,
                    contentDescription = track.trackName,
                    modifier = Modifier
                        .size(artSize)
                        .clip(RoundedCornerShape(artCorner))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                    contentScale = ContentScale.Crop,
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(
                        when (presentation) {
                            TrackRowPresentation.Catalog -> 3.dp
                            TrackRowPresentation.Secondary -> 5.dp
                            TrackRowPresentation.Featured -> 6.dp
                        },
                    ),
                ) {
                    track.artistName?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it.uppercase(),
                            style = if (isCatalog) {
                                MaterialTheme.typography.labelSmall
                            } else if (isSecondary) {
                                MaterialTheme.typography.labelSmall
                            } else {
                                MaterialTheme.typography.labelMedium
                            },
                            color = if (playingSurface) {
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
                        style = if (isCatalog) {
                            MaterialTheme.typography.titleSmall
                        } else {
                            MaterialTheme.typography.titleMedium
                        },
                        color = if (isCatalog) {
                            contentColor.copy(alpha = 0.88f)
                        } else {
                            contentColor
                        },
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (isFeatured) {
                        Text(
                            text = when {
                                hasPreviewCta && hasDirectSpotifyTrack -> "Preview ruhig hier oder direkt weiter in Spotify."
                                hasPreviewCta && hasSpotifyArtistLink -> "Preview hier oder direkt zum Artist auf Spotify."
                                hasPreviewCta && hasSpotifySearch -> "Preview hier oder den Track in Spotify suchen."
                                hasPreviewCta -> "Preview direkt in der App."
                                hasDirectSpotifyTrack -> "Spotify Player direkt in der App."
                                hasSpotifyArtistLink -> "Direkt zum Artist auf Spotify."
                                hasSpotifySearch -> "Track in Spotify suchen."
                                else -> "Gerade kein externer Link verfuegbar."
                            },
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColor.copy(alpha = 0.72f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (isFeatured) {
                        track.collectionName?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = contentColor.copy(alpha = 0.72f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    if (!isCatalog) {
                    val showNonCatalogPillRow = (hasPreviewCta && (playingSurface || !hideStatusPillDuplicates)) ||
                        (isSelected && !playingSurface) ||
                        (!hideStatusPillDuplicates && (hasDirectSpotifyTrack || hasSpotifyArtistLink || hasSpotifySearch))
                    if (showNonCatalogPillRow) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (hasPreviewCta) {
                            if (playingSurface) {
                                TrackPill(
                                    text = stringResource(R.string.track_pill_playing),
                                    isHighlighted = true,
                                )
                            } else if (!hideStatusPillDuplicates) {
                                TrackPill(
                                    text = stringResource(R.string.track_pill_preview),
                                    isHighlighted = false,
                                )
                            }
                        }
                        if (isSelected && !playingSurface) {
                            TrackPill(
                                text = stringResource(R.string.track_pill_in_player),
                                isHighlighted = false,
                            )
                        }
                        if (hasDirectSpotifyTrack) {
                            if (!hideStatusPillDuplicates) {
                                TrackPill(
                                    text = stringResource(R.string.track_pill_spotify_player),
                                    isHighlighted = false,
                                    accentColor = SpotifyGreen,
                                )
                            }
                        } else if (hasSpotifyArtistLink) {
                            if (!hideStatusPillDuplicates) {
                                TrackPill(
                                    text = stringResource(R.string.track_pill_spotify_artist),
                                    isHighlighted = false,
                                    accentColor = SpotifyGreen,
                                )
                            }
                        } else if (hasSpotifySearch) {
                            if (!hideStatusPillDuplicates) {
                                TrackPill(
                                    text = stringResource(R.string.track_pill_spotify_search),
                                    isHighlighted = false,
                                    accentColor = SpotifyGreen,
                                )
                            }
                        }
                    }
                    }
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (playingSurface) {
                                TrackPill(
                                    text = stringResource(R.string.track_pill_playing),
                                    isHighlighted = true,
                                )
                            } else if (isSelected) {
                                TrackPill(
                                    text = stringResource(R.string.track_pill_in_set),
                                    isHighlighted = false,
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (hasPreviewCta) {
                            val toggle = requireNotNull(onPlayToggle)
                            Button(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    toggle()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                            ) {
                                Icon(
                                    imageVector = if (playingSurface) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                )
                                Text(
                                    text = if (playingSurface) "Pause" else "Anhoeren",
                                    modifier = Modifier.padding(start = 6.dp),
                                )
                            }
                        }

                        if (hasDirectSpotifyTrack) {
                            OutlinedButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    showSpotifyPlayer = true
                                },
                                modifier = if (hasPreviewCta) {
                                    Modifier
                                        .weight(1f)
                                        .testTag("music.track.spotify.open")
                                        .semantics { contentDescription = openSpotifyPlayerContentDescription }
                                } else {
                                    Modifier
                                        .fillMaxWidth()
                                        .testTag("music.track.spotify.open")
                                        .semantics { contentDescription = openSpotifyPlayerContentDescription }
                                },
                                border = BorderStroke(1.dp, SpotifyGreen.copy(alpha = 0.48f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = SpotifyGreen,
                                ),
                            ) {
                                Text(
                                    text = stringResource(R.string.track_action_open_spotify),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        } else if (hasSpotifyArtistLink || hasSpotifySearch) {
                            OutlinedButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    openTrackInSpotify(
                                        context = context,
                                        spotifyArtistId = track.spotifyArtistId,
                                        spotifyTrackId = track.spotifyTrackId,
                                        externalUrl = track.externalUrl,
                                    )
                                },
                                modifier = if (hasPreviewCta) Modifier.weight(1f) else Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, SpotifyGreen.copy(alpha = 0.48f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = SpotifyGreen,
                                ),
                            ) {
                                Text(
                                    text = if (hasSpotifyArtistLink) {
                                        stringResource(R.string.track_pill_spotify_artist)
                                    } else {
                                        stringResource(R.string.track_pill_spotify_search)
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                    }

                    if (!hasCtaOrExternalOptions) {
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
                                text = stringResource(R.string.track_no_spotify_link),
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
    spotifyArtistId: String? = null,
    spotifyTrackId: String?,
    externalUrl: String?,
) {
    val spotifyAppUri = spotifyAppUri(spotifyTrackId, spotifyArtistId, externalUrl)
    if (spotifyAppUri != null) {
        val spotifyIntent = Intent(Intent.ACTION_VIEW, spotifyAppUri).setPackage("com.spotify.music")
        try {
            context.startActivity(spotifyIntent)
            return
        } catch (_: ActivityNotFoundException) {
            // Fall through to the public web URL when the Spotify app is not installed.
        } catch (_: SecurityException) {
            // Fall through to the public web URL when launching the external app is blocked.
        } catch (_: RuntimeException) {
            // Fall through to the public web URL when the Spotify app is not installed.
        }
    }

    val externalUrl = externalUrl ?: return
    openExternalLink(
        context = context,
        url = externalUrl,
        browserMissingMessage = "Spotify-Link konnte nicht geoeffnet werden.",
    )
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun SpotifyEmbedDialog(
    track: Track,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val embedUrl = remember(track.spotifyTrackId, track.externalUrl) {
        spotifyEmbedUri(track.spotifyTrackId, track.externalUrl)
    }
    val spotifyDialogContentDescription = stringResource(R.string.track_spotify_dialog_cd)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .testTag("music.track.spotify.dialog")
                .semantics { contentDescription = spotifyDialogContentDescription },
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

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("music.track.spotify.close"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.common_close),
                        )
                    }
                }

                if (embedUrl != null) {
                    ManagedAndroidWebView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp)
                            .clip(RoundedCornerShape(22.dp)),
                        factory = { playerContext ->
                            WebView(playerContext).apply {
                                webViewClient = SkydownMediaWebViewClient()
                                applySkydownMediaWebViewDefaults()
                                loadSkydownWebUrl(embedUrl.toString())
                            }
                        },
                        update = { webView ->
                            if (webView.url != embedUrl.toString()) {
                                webView.loadSkydownWebUrl(embedUrl.toString())
                            }
                        },
                    )
                } else {
                    Text(
                        text = stringResource(R.string.track_spotify_player_unavailable),
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
                                spotifyArtistId = track.spotifyArtistId,
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
                            text = stringResource(R.string.track_action_spotify_app),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.common_close))
                    }
                }
            }
        }
    }
}

private fun spotifyAppUri(
    spotifyTrackId: String?,
    spotifyArtistId: String?,
    externalUrl: String?,
): Uri? {
    val artistId = resolvedSpotifyArtistId(spotifyArtistId, externalUrl)
    if (artistId != null && resolvedSpotifyTrackId(spotifyTrackId, externalUrl) == null) {
        return Uri.parse("spotify:artist:$artistId")
    }
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

private fun resolvedSpotifyArtistId(
    spotifyArtistId: String?,
    externalUrl: String?,
): String? {
    if (!spotifyArtistId.isNullOrBlank()) return spotifyArtistId
    if (externalUrl.isNullOrBlank()) return null
    val parsed = Uri.parse(externalUrl)
    val segments = parsed.pathSegments
    val artistIndex = segments.indexOf("artist")
    return if (artistIndex != -1 && artistIndex + 1 < segments.size) {
        segments[artistIndex + 1]
    } else {
        null
    }
}
