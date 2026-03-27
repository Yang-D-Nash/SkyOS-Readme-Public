package com.skydown.android.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.skydown.android.ui.component.AppTopBarSessionActions
import com.skydown.android.ui.component.SectionHeader
import com.skydown.android.ui.component.SkydownCard
import com.skydown.android.ui.model.HomeUiState
import com.skydown.android.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenCart: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = "Home",
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    AppTopBarSessionActions(
                        onOpenCart = onOpenCart,
                        onOpenSettings = onOpenSettings,
                    ) {
                        IconButton(onClick = viewModel::refresh) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Home aktualisieren",
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.94f),
                    scrolledContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.98f),
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.07f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f),
                            MaterialTheme.colorScheme.background,
                        ),
                    ),
                ),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    end = 16.dp,
                    bottom = innerPadding.calculateBottomPadding() + 28.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    HomeHeroCard()
                }

                item {
                    HomeLatestReleaseCard(uiState = uiState)
                }

                item {
                    HomeLatestVideoCard(uiState = uiState)
                }

                item {
                    HomeStoryCard()
                }
            }
        }
    }
}

@Composable
private fun HomeHeroCard() {
    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "Skydown x 22",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Hip Hop, Music, Video.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                )
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        Row(
            modifier = Modifier.padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HomeBadge(text = "Hip Hop", icon = Icons.Default.MusicNote, isActive = true)
            HomeBadge(text = "22 Hamburg", icon = Icons.Default.CheckCircle, isActive = false)
            HomeBadge(text = "Music & Video", icon = Icons.Default.Movie, isActive = false)
        }
    }
}

@Composable
private fun HomeLatestReleaseCard(
    uiState: HomeUiState,
) {
    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        SectionHeader("Neuester Release")
        val track = uiState.featuredTrack

        if (track == null) {
            Text(
                text = uiState.homeTrackMessage ?: "Neuer Song erscheint hier.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                modifier = Modifier.padding(top = 8.dp),
            )
            return@SkydownCard
        }

        Row(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = track.artworkUrl100,
                contentDescription = track.trackName,
                modifier = Modifier
                    .size(82.dp)
                    .clip(MaterialTheme.shapes.large),
                contentScale = ContentScale.Crop,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = track.trackName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = track.artistName ?: "Skydown x 22",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                )
                Text(
                    text = buildString {
                        append(track.collectionName ?: "Spotify Release")
                        track.releaseDate?.takeIf { it.isNotBlank() }?.let {
                            append(" • ")
                            append(it)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
                )
            }
        }

        Row(
            modifier = Modifier.padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HomeBadge(text = "Neuester Song", icon = Icons.Default.MusicNote, isActive = true)
            HomeBadge(
                text = when {
                    !track.previewUrl.isNullOrBlank() -> "Preview in App"
                    !track.externalUrl.isNullOrBlank() -> "Spotify Suche"
                    else -> "Release"
                },
                icon = Icons.Default.CheckCircle,
                isActive = !track.previewUrl.isNullOrBlank(),
            )
        }
    }
}

@Composable
private fun HomeLatestVideoCard(
    uiState: HomeUiState,
) {
    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        SectionHeader("Video Highlight")
        val video = uiState.featuredVideo

        if (video == null) {
            Text(
                text = uiState.homeVideoMessage ?: "Neues Video erscheint hier.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                modifier = Modifier.padding(top = 8.dp),
            )
            return@SkydownCard
        }

        Row(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(82.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(34.dp),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = video.projectName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                )
                if (video.notes.isNotBlank()) {
                    Text(
                        text = video.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
                    )
                }
            }
        }

        Row(
            modifier = Modifier.padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HomeBadge(text = "Videography", icon = Icons.Default.Movie, isActive = true)
            HomeBadge(text = "Home Auswahl", icon = Icons.Default.CheckCircle, isActive = false)
        }
    }
}

@Composable
private fun HomeStoryCard() {
    val context = LocalContext.current
    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        SectionHeader("Developer")
        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HomeBadge(text = "Yang D. Nash", icon = Icons.Default.Person, isActive = true)
            HomeBadge(text = "Skydown", icon = Icons.Default.CheckCircle, isActive = false)
            HomeBadge(text = "Entwickler", icon = Icons.Default.CheckCircle, isActive = false)
        }

        Text(
            text = "Yang D. Nash entwickelt die App und bildet den Kern von Skydown x 22. Skydown Entertainment verbindet Hip Hop, Music und Video in einer eigenen mobilen Plattform.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
            modifier = Modifier.padding(top = 14.dp),
        )

        Column(
            modifier = Modifier.padding(top = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = {
                    openExternalLink(context, "https://www.instagram.com/y.d.nash/")
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                )
                Text(
                    text = "Yang D. Nash auf Instagram",
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            OutlinedButton(
                onClick = {
                    openExternalLink(context, "https://www.instagram.com/skydown_entertainment/")
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                )
                Text(
                    text = "Skydown auf Instagram",
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            OutlinedButton(
                onClick = {
                    val emailIntent = Intent(
                        Intent.ACTION_SENDTO,
                        Uri.parse("mailto:skydownent@gmail.com?subject=Skydown%20x%2022%20Kontakt"),
                    )
                    context.startActivity(emailIntent)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                )
                Text(
                    text = "Kontakt per E-Mail",
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun HomeBadge(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
) {
    val backgroundColor = if (isActive) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    } else {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.70f)
    }
    val contentColor = if (isActive) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
