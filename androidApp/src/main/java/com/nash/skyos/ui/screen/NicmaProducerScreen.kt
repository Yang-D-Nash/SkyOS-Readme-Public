package com.nash.skyos.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nash.skyos.data.ArtistPageBrand
import com.nash.skyos.data.ArtistPagesStore
import com.nash.skyos.data.StudioPriceItemUi
import com.nash.skyos.ui.component.BrandHeroCard
import com.nash.skyos.ui.component.BrandPill
import com.nash.skyos.ui.component.LocalSessionUser
import com.nash.skyos.ui.component.SectionHeader
import com.nash.skyos.ui.component.SkydownCard
import com.nash.skyos.ui.component.SkydownTopBarTitle
import com.nash.skyos.ui.component.skydownContentPadding
import com.nash.skyos.ui.component.skydownScreenBrush
import com.nash.skyos.ui.component.skydownTopBarColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NicmaProducerScreen(
    onBack: () -> Unit,
) {
    val currentUser = LocalSessionUser.current
    val profileOptions = remember { listOf("NICMA MUSIC", "NICMA STUDIO") }
    var selectedProfile by rememberSaveable { mutableStateOf("NICMA MUSIC") }
    var viewingProfile by rememberSaveable { mutableStateOf<String?>(null) }
    var editingProfile by rememberSaveable { mutableStateOf<String?>(null) }
    val page = ArtistPagesStore.pageFor(brand = ArtistPageBrand.Nicma, artistName = selectedProfile)
    val isStudioProfile = selectedProfile == "NICMA STUDIO"
    val canEdit = ArtistPagesStore.canEdit(page, currentUser)
    val hubTagline = page.tagline?.takeIf { it.isNotBlank() }
        ?: if (isStudioProfile) {
            "Studio, Preisliste, Production, Recording – NICMA STUDIO."
        } else {
            "Künstlerseite, Katalog, Links – NICMA MUSIC."
        }
    val topBarSubtitle = page.tagline?.takeIf { it.isNotBlank() }
        ?: if (isStudioProfile) {
            "Producing, Preise, Recording und direkter Kontakt."
        } else {
            "Artist, Songs, Links und oeffentliche Kanaele."
        }
    val profileFallbackBio = if (isStudioProfile) {
        "Studio Page: Preise, Production und Recording."
    } else {
        "Artist Page: NICMA MUSIC, Links und Profil."
    }

    viewingProfile?.let { profile ->
        ArtistPageScreen(
            artistName = profile,
            brand = ArtistPageBrand.Nicma,
            onBack = { viewingProfile = null },
        )
        return
    }

    editingProfile?.let { profile ->
        ArtistPageScreen(
            artistName = profile,
            brand = ArtistPageBrand.Nicma,
            onBack = { editingProfile = null },
        )
        return
    }

    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val listState = rememberLazyListState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    SkydownTopBarTitle(
                        title = selectedProfile,
                        subtitle = topBarSubtitle,
                    )
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
                        IconButton(onClick = { editingProfile = selectedProfile }) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Bearbeiten",
                            )
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
                    skydownScreenBrush(
                        primaryColor = MaterialTheme.colorScheme.tertiary,
                        secondaryColor = MaterialTheme.colorScheme.primary,
                        primaryAlpha = 0.08f,
                        secondaryAlpha = 0.06f,
                    ),
                ),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = skydownContentPadding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        profileOptions.forEach { profile ->
                            OutlinedButton(
                                onClick = { selectedProfile = profile },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(999.dp),
                            ) {
                                Text(if (profile == selectedProfile) "$profile aktiv" else profile)
                            }
                        }
                    }
                }

                item {
                    NicmaHeroCard(
                        isStudioProfile = isStudioProfile,
                        headline = page.artistName,
                        body = page.bio ?: profileFallbackBio,
                        tagline = hubTagline,
                        heroImageUrl = page.heroImageURL,
                        onSurfaceClick = { viewingProfile = selectedProfile },
                    )
                }

                item {
                    SkydownCard {
                        SectionHeader(
                            if (isStudioProfile) "NICMA STUDIO Seite" else "NICMA MUSIC Seite",
                        )
                        Text(
                            text = page.tagline?.takeIf { it.isNotBlank() }
                                ?: if (isStudioProfile) {
                                    "Studio-Profil, Preisliste, Production und Links."
                                } else {
                                    "Artist Profil und Links."
                                },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }

                item {
                    NicmaPriceListCard(priceList = resolvedNicmaProducerPackages(page.studioPriceList))
                }

                item {
                    NicmaContactCard(
                        instagramUrl = page.instagramURL?.takeIf { it.isNotBlank() } ?: nicmaInstagramUrl,
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
    isStudioProfile: Boolean,
    headline: String,
    body: String,
    tagline: String,
    heroImageUrl: String?,
    onSurfaceClick: (() -> Unit)? = null,
) {
    BrandHeroCard(
        eyebrow = "NICMA",
        title = headline,
        subtitle = tagline,
        detail = body,
        backgroundImageUrl = heroImageUrl?.takeIf { it.isNotBlank() },
        accent = MaterialTheme.colorScheme.tertiary,
        secondaryAccent = MaterialTheme.colorScheme.primary,
        onSurfaceClick = onSurfaceClick,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isStudioProfile) {
                BrandPill(text = "Mixing", tint = MaterialTheme.colorScheme.tertiary)
                BrandPill(text = "Mastering", tint = MaterialTheme.colorScheme.primary)
                BrandPill(text = "Recording", tint = MaterialTheme.colorScheme.secondary)
            } else {
                BrandPill(text = "Artist", tint = MaterialTheme.colorScheme.tertiary)
                BrandPill(text = "Katalog", tint = MaterialTheme.colorScheme.primary)
                BrandPill(text = "Links", tint = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
private fun NicmaPriceListCard(priceList: List<NicmaProducerPackage>) {
    SkydownCard {
        SectionHeader("Preisliste")
        Column(
            modifier = Modifier.padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            priceList.forEach { packageItem ->
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
    SkydownCard {
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

private const val nicmaInstagramUrl = "https://www.instagram.com/nicma.music/"

private fun resolvedNicmaProducerPackages(customPriceList: List<StudioPriceItemUi>): List<NicmaProducerPackage> {
    if (customPriceList.isEmpty()) return nicmaProducerPackages
    return customPriceList.map { NicmaProducerPackage(it.title, it.detail, it.price) }
}
