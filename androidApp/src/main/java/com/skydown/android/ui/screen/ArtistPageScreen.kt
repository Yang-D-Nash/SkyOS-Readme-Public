package com.skydown.android.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.skydown.android.data.AppContainer
import com.skydown.android.data.ArtistPageBrand
import com.skydown.android.data.ArtistPageUi
import com.skydown.android.data.ArtistPagesStore
import com.skydown.android.ui.component.SkydownCard
import com.skydown.android.ui.component.skydownScreenBrush
import com.skydown.android.ui.component.skydownTopBarColors
import com.skydown.android.ui.theme.InstagramPink
import com.skydown.android.ui.theme.SpotifyGreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistPageScreen(
    artistName: String,
    brand: ArtistPageBrand,
    onBack: () -> Unit,
) {
    val currentUser by AppContainer.currentUser.collectAsStateWithLifecycle()
    val pages by ArtistPagesStore.pages.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val page = remember(pages, artistName, brand) {
        ArtistPagesStore.pageFor(brand = brand, artistName = artistName)
    }
    val canEdit = ArtistPagesStore.canEdit(page, currentUser)

    var isEditing by rememberSaveable(page.slug) { mutableStateOf(false) }
    var taglineDraft by rememberSaveable(page.slug) { mutableStateOf(page.tagline.orEmpty()) }
    var bioDraft by rememberSaveable(page.slug) { mutableStateOf(page.bio.orEmpty()) }
    var profileImageDraft by rememberSaveable(page.slug) { mutableStateOf(page.profileImageURL.orEmpty()) }
    var heroImageDraft by rememberSaveable(page.slug) { mutableStateOf(page.heroImageURL.orEmpty()) }
    var instagramDraft by rememberSaveable(page.slug) { mutableStateOf(page.instagramURL.orEmpty()) }
    var spotifyDraft by rememberSaveable(page.slug) { mutableStateOf(page.spotifyURL.orEmpty()) }
    var youtubeDraft by rememberSaveable(page.slug) { mutableStateOf(page.youtubeURL.orEmpty()) }
    var isSaving by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(page.slug, page.updatedAtEpochMillis, isEditing) {
        if (!isEditing) {
            taglineDraft = page.tagline.orEmpty()
            bioDraft = page.bio.orEmpty()
            profileImageDraft = page.profileImageURL.orEmpty()
            heroImageDraft = page.heroImageURL.orEmpty()
            instagramDraft = page.instagramURL.orEmpty()
            spotifyDraft = page.spotifyURL.orEmpty()
            youtubeDraft = page.youtubeURL.orEmpty()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(page.artistName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurueck")
                    }
                },
                actions = {
                    if (canEdit) {
                        Button(
                            onClick = {
                                if (isEditing) {
                                    coroutineScope.launch {
                                        isSaving = true
                                        val updatedPage = page.copy(
                                            tagline = taglineDraft.trimmedOrNull(),
                                            bio = bioDraft.trimmedOrNull(),
                                            profileImageURL = profileImageDraft.trimmedOrNull(),
                                            heroImageURL = heroImageDraft.trimmedOrNull(),
                                            instagramURL = instagramDraft.trimmedOrNull(),
                                            spotifyURL = spotifyDraft.trimmedOrNull(),
                                            youtubeURL = youtubeDraft.trimmedOrNull(),
                                            updatedAtEpochMillis = System.currentTimeMillis(),
                                            isPlaceholder = false,
                                        )
                                        val result = ArtistPagesStore.save(updatedPage)
                                        isSaving = false
                                        if (result.isSuccess) {
                                            isEditing = false
                                            Toast.makeText(context, "Artist-Seite gespeichert.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                result.exceptionOrNull()?.message ?: "Artist-Seite konnte nicht gespeichert werden.",
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                        }
                                    }
                                } else {
                                    isEditing = true
                                }
                            },
                            enabled = !isSaving,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        ) {
                            Text(if (isEditing) "Speichern" else "Bearbeiten")
                        }
                    }
                },
                colors = skydownTopBarColors(),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(skydownScreenBrush())
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ArtistPageHeroCard(page = page, brand = brand)
            ArtistPageLinksCard(page = page)

            if (canEdit && isEditing) {
                SkydownCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Artist-Seite bearbeiten",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )

                        ArtistPageInput(title = "Kurzzeile", value = taglineDraft, onValueChange = { taglineDraft = it })
                        ArtistPageInput(title = "Bio", value = bioDraft, onValueChange = { bioDraft = it }, singleLine = false)
                        ArtistPageInput(title = "Profilbild-URL", value = profileImageDraft, onValueChange = { profileImageDraft = it })
                        ArtistPageInput(title = "Hero-Bild-URL", value = heroImageDraft, onValueChange = { heroImageDraft = it })
                        ArtistPageInput(title = "Instagram", value = instagramDraft, onValueChange = { instagramDraft = it })
                        ArtistPageInput(title = "Spotify", value = spotifyDraft, onValueChange = { spotifyDraft = it })
                        ArtistPageInput(title = "YouTube", value = youtubeDraft, onValueChange = { youtubeDraft = it })
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistPageHeroCard(
    page: ArtistPageUi,
    brand: ArtistPageBrand,
) {
    SkydownCard(contentPadding = PaddingValues(0.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(24.dp)),
        ) {
            if (!page.heroImageURL.isNullOrBlank()) {
                AsyncImage(
                    model = page.heroImageURL,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    SpotifyGreen.copy(alpha = 0.88f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.82f),
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                                ),
                            ),
                        ),
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f)),
                        ),
                    ),
            )

            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Box(
                    modifier = Modifier
                        .size(82.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!page.profileImageURL.isNullOrBlank()) {
                        AsyncImage(
                            model = page.profileImageURL,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Text(
                            text = page.artistName.take(1).uppercase(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = page.artistName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                    )
                    Text(
                        text = page.tagline ?: "${brand.displayTitle} Profil",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.84f),
                    )
                }
            }
        }

        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = page.bio ?: "Noch keine Artist-Seite hinterlegt. Owner oder zugewiesene Editoren koennen hier eine repraesentative Kurzbeschreibung anlegen.",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f),
            )
            if (page.editorUids.isNotEmpty()) {
                ArtistEditorBadge(count = page.editorUids.size)
            }
        }
    }
}

@Composable
private fun ArtistPageLinksCard(page: ArtistPageUi) {
    val context = LocalContext.current
    val youtubeTint = MaterialTheme.colorScheme.error
    val links = buildList {
        page.instagramURL?.trimmedOrNull()?.let {
            add(Triple("Instagram", it, Icons.Default.CameraAlt to InstagramPink))
        }
        page.spotifyURL?.trimmedOrNull()?.let {
            add(Triple("Spotify", it, Icons.Default.MusicNote to SpotifyGreen))
        }
        page.youtubeURL?.trimmedOrNull()?.let {
            add(Triple("YouTube", it, Icons.Default.PlayCircleFilled to youtubeTint))
        }
    }

    SkydownCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Links",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            if (links.isEmpty()) {
                Text(
                    text = "Noch keine Links hinterlegt.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                )
            } else {
                links.forEach { link ->
                    Button(
                        onClick = { openExternalLink(context, link.second) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    ) {
                        Icon(link.third.first, contentDescription = null, tint = link.third.second)
                        Text(
                            text = link.first,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 10.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistEditorBadge(count: Int) {
    Text(
        text = "$count Editor${if (count == 1) "" else "en"}",
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.labelMedium,
    )
}

@Composable
private fun ArtistPageInput(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(title) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 4,
        shape = RoundedCornerShape(18.dp),
    )
}

private fun String?.trimmedOrNull(): String? {
    val trimmed = this?.trim().orEmpty()
    return trimmed.ifBlank { null }
}
