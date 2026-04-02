package com.skydown.android.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skydown.android.data.AppContainer
import com.skydown.android.data.ArtistPageBrand
import com.skydown.android.data.ArtistPagesStore
import com.skydown.android.ui.component.SectionHeader
import com.skydown.android.ui.component.SkydownCard
import com.skydown.android.ui.component.skydownTopBarColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NicmaProducerScreen(
    onBack: () -> Unit,
) {
    val currentUser by AppContainer.currentUser.collectAsStateWithLifecycle()
    val page = ArtistPagesStore.pageFor(brand = ArtistPageBrand.Nicma, artistName = "NICMA MUSIC")
    val canEdit = ArtistPagesStore.canEdit(page, currentUser)
    var showingEditor by rememberSaveable { mutableStateOf(false) }

    if (showingEditor) {
        ArtistPageScreen(
            artistName = "NICMA MUSIC",
            brand = ArtistPageBrand.Nicma,
            onBack = { showingEditor = false },
        )
        return
    }

    BackHandler(onBack = onBack)
    val context = LocalContext.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "NICMA MUSIC",
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = page.tagline ?: "Producing, Preise und direkter Kontakt.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Zurueck",
                        )
                    }
                },
                actions = {
                    if (canEdit) {
                        TextButton(onClick = { showingEditor = true }) {
                            Text("Bearbeiten")
                        }
                    }
                },
                colors = skydownTopBarColors(),
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
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                            MaterialTheme.colorScheme.background,
                        ),
                    ),
                ),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = innerPadding.calculateTopPadding() + com.skydown.android.ui.component.SkydownUiTokens.screenTopPadding,
                    end = 16.dp,
                    bottom = innerPadding.calculateBottomPadding() + 28.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    NicmaHeroCard(
                        headline = page.artistName,
                        body = page.bio ?: "Mixing, Mastering und Recording sind hier als klare Producer-Seite gebuendelt, mit direktem Kontakt und transparenter Preisliste.",
                    )
                }

                item {
                    NicmaPriceListCard()
                }

                item {
                    NicmaContactCard(
                        instagramUrl = page.instagramURL?.takeIf { it.isNotBlank() } ?: "https://www.instagram.com/nicma.music/",
                        spotifyUrl = page.spotifyURL,
                        youtubeUrl = page.youtubeURL,
                        onOpenLink = { openExternalLink(context, it) },
                    )
                }
            }
        }
    }
}

@Composable
private fun NicmaHeroCard(
    headline: String,
    body: String,
) {
    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = headline,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                )
            }

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                )
            }
        }

        Row(
            modifier = Modifier.padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NicmaBadge(text = "Mixing")
            NicmaBadge(text = "Mastering")
            NicmaBadge(text = "Recording")
        }
    }
}

@Composable
private fun NicmaPriceListCard() {
    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        SectionHeader("Preisliste")
        Column(
            modifier = Modifier.padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            nicmaProducerPackages.forEach { packageItem ->
                NicmaPriceRow(packageItem)
            }
        }
    }
}

@Composable
private fun NicmaPriceRow(
    packageItem: NicmaProducerPackage,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = packageItem.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = packageItem.detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
            )
        }

        Text(
            text = packageItem.price,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun NicmaContactCard(
    instagramUrl: String,
    spotifyUrl: String?,
    youtubeUrl: String?,
    onOpenLink: (String) -> Unit,
) {
    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        SectionHeader("Links")
        Text(
            text = "Direkter Kontakt und oeffentliche Plattformen fuer NICMA.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            modifier = Modifier.padding(top = 8.dp),
        )

        OutlinedButton(
            onClick = { onOpenLink(instagramUrl) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text("NICMA MUSIC auf Instagram")
        }

        spotifyUrl?.takeIf { it.isNotBlank() }?.let { url ->
            OutlinedButton(
                onClick = { onOpenLink(url) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                Text("Spotify")
            }
        }

        youtubeUrl?.takeIf { it.isNotBlank() }?.let { url ->
            OutlinedButton(
                onClick = { onOpenLink(url) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                Text("YouTube")
            }
        }
    }
}

@Composable
private fun NicmaBadge(text: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.82f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}

private data class NicmaProducerPackage(
    val title: String,
    val detail: String,
    val price: String,
)

private val nicmaProducerPackages = listOf(
    NicmaProducerPackage("Mixing", "max. 24 Audio Files", "150 €"),
    NicmaProducerPackage("Mastering", "2 stems", "70 €"),
    NicmaProducerPackage("Mastering", "max. 5 stems", "90 €"),
    NicmaProducerPackage("Mixing + Mastering", "max. 24 Audio Files", "200 €"),
    NicmaProducerPackage("Track Recording ohne Mix / Master", "Recording Session", "120 €"),
    NicmaProducerPackage("Track Recording inkl. Mix / Master", "Kompletter Recording-Flow", "250 €"),
    NicmaProducerPackage("8h Studio Zeit + Engineer", "zzgl. Nachbearbeitung", "400 € + Nachbearbeitung"),
)
