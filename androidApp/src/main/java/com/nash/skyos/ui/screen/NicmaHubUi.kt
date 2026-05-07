package com.nash.skyos.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nash.skyos.R
import com.nash.skyos.data.StudioPriceItemUi
import com.nash.skyos.ui.component.BrandActionButton
import com.nash.skyos.ui.component.BrandHeroCard
import com.nash.skyos.ui.component.BrandPill
import com.nash.skyos.ui.component.SectionHeader
import com.nash.skyos.ui.component.SkydownCard
import com.nash.skyos.ui.component.SkydownPremiumIconAction
import com.nash.skyos.ui.component.SkydownPremiumTextField
import com.nash.skyos.ui.component.SkydownUiTokens
import com.nash.skyos.ui.theme.SpotifyGreen

const val nicmaProfileMusic = "NICMA MUSIC"
const val nicmaProfileStudio = "NICMA STUDIO"

val nicmaProfileOptions = listOf(nicmaProfileMusic, nicmaProfileStudio)

fun isNicmaStudioProfile(artistName: String): Boolean =
    artistName.trim().equals(nicmaProfileStudio, ignoreCase = true)

fun nicmaTopBarSubtitle(artistName: String, pageTagline: String?): String {
    val t = pageTagline?.trim().orEmpty()
    if (t.isNotEmpty()) return t
    return if (isNicmaStudioProfile(artistName)) {
        "Preise · Recording"
    } else {
        "Songs · Links"
    }
}

fun nicmaHubTagline(artistName: String, pageTagline: String?): String {
    val t = pageTagline?.trim().orEmpty()
    if (t.isNotEmpty()) return t
    return if (isNicmaStudioProfile(artistName)) {
        "Preisliste & Recording"
    } else {
        "Katalog & Links"
    }
}

fun nicmaProfileFallbackBio(artistName: String): String =
    if (isNicmaStudioProfile(artistName)) {
        "Preise, Production, Recording."
    } else {
        "Katalog, Links, Profil."
    }

data class NicmaProducerPackage(
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

const val nicmaDefaultInstagramUrl = "https://www.instagram.com/nicma.music/"

/** Einheitlich mit [com.nash.skyos.data.repository.AndroidMusicRepository] / Spotify-Maps fuer NICMA MUSIC. */
const val nicmaDefaultSpotifyArtistUrl = "https://open.spotify.com/artist/0OoRIo7pJjtLgg3qyf1oDS"

fun resolvedNicmaProducerPackages(customPriceList: List<StudioPriceItemUi>): List<NicmaProducerPackage> {
    if (customPriceList.isEmpty()) return nicmaProducerPackages
    return customPriceList.map { NicmaProducerPackage(it.title, it.detail, it.price) }
}

/** Gleiche Zeilen wie die sichtbaren Vorschau-Preise, wenn in Firestore noch kein [ArtistPageUi.studioPriceList] steht. */
fun nicmaDefaultStudioPriceItems(): List<StudioPriceItemUi> =
    nicmaProducerPackages.map { StudioPriceItemUi(title = it.title, detail = it.detail, price = it.price) }

@Composable
fun NicmaProfileSelectorRow(
    selectedProfile: String,
    onSelectProfile: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill),
    ) {
        nicmaProfileOptions.forEach { profile ->
            val selected = profile == selectedProfile
            Surface(
                onClick = { onSelectProfile(profile) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(SkydownUiTokens.fullCapsuleRadius),
                color = if (selected) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)
                } else {
                    MaterialTheme.colorScheme.surface
                },
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = if (selected) 0.34f else 0.22f),
                ),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = profile,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    )
                    if (selected) {
                        Row(
                            modifier = Modifier.padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingDense),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(SpotifyGreen, CircleShape),
                            )
                            Text(
                                text = stringResource(R.string.nicma_status_active),
                                style = MaterialTheme.typography.labelSmall,
                                color = SpotifyGreen,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NicmaHubHeroCard(
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
        Row(horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro)) {
            if (isStudioProfile) {
                BrandPill(text = stringResource(R.string.nicma_pill_mix), tint = MaterialTheme.colorScheme.tertiary)
                BrandPill(text = stringResource(R.string.nicma_pill_master), tint = MaterialTheme.colorScheme.primary)
                BrandPill(text = stringResource(R.string.nicma_pill_rec), tint = MaterialTheme.colorScheme.secondary)
            } else {
                BrandPill(text = stringResource(R.string.nicma_pill_artist), tint = MaterialTheme.colorScheme.tertiary)
                BrandPill(text = stringResource(R.string.nicma_pill_catalog), tint = MaterialTheme.colorScheme.primary)
                BrandPill(text = stringResource(R.string.nicma_pill_links), tint = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
fun NicmaPriceListCard(
    priceList: List<NicmaProducerPackage>,
) {
    SkydownCard {
        SectionHeader(stringResource(R.string.nicma_price_list_title))
        Column(
            modifier = Modifier.padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill),
        ) {
            priceList.forEach { item ->
                NicmaPriceRow(item)
            }
        }
    }
}

@Composable
fun NicmaPriceListEditorCard(
    items: List<StudioPriceItemUi>,
    enabled: Boolean,
    onAdd: () -> Unit,
    onEdit: (Int) -> Unit,
    onDelete: (Int) -> Unit,
) {
    SkydownCard {
        SectionHeader(stringResource(R.string.nicma_price_list_title))
        if (items.isEmpty()) {
            Text(
                text = stringResource(R.string.nicma_price_list_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth(),
            )
        } else {
            Column(
                modifier = Modifier.padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill),
            ) {
                items.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(SkydownUiTokens.messageBubbleRadius))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingNano),
                        ) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = item.detail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                            )
                        }
                        Text(
                            text = item.price,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingNano)) {
                            SkydownPremiumIconAction(
                                icon = Icons.Filled.Edit,
                                contentDescription = stringResource(R.string.artist_action_edit),
                                onClick = { onEdit(index) },
                                enabled = enabled,
                                accent = MaterialTheme.colorScheme.primary,
                                size = 40.dp,
                                iconSize = 18.dp,
                            )
                            SkydownPremiumIconAction(
                                icon = Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.common_remove),
                                onClick = { onDelete(index) },
                                enabled = enabled,
                                accent = MaterialTheme.colorScheme.error,
                                size = 40.dp,
                                iconSize = 18.dp,
                            )
                        }
                    }
                }
            }
        }
        BrandActionButton(
            text = stringResource(R.string.nicma_price_entry_add),
            onClick = onAdd,
            accent = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            icon = Icons.Filled.Add,
            enabled = enabled,
        )
    }
}

@Composable
fun StudioPriceItemEditorDialog(
    isAdd: Boolean,
    item: StudioPriceItemUi,
    onDismiss: () -> Unit,
    onConfirm: (StudioPriceItemUi) -> Unit,
) {
    var titleField by remember { mutableStateOf(item.title) }
    var detailField by remember { mutableStateOf(item.detail) }
    var priceField by remember { mutableStateOf(item.price) }

    LaunchedEffect(item, isAdd) {
        titleField = item.title
        detailField = item.detail
        priceField = item.price
    }

    val canSave = titleField.trim().isNotEmpty() &&
        detailField.trim().isNotEmpty() &&
        priceField.trim().isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isAdd) {
                    stringResource(R.string.nicma_price_dialog_new_title)
                } else {
                    stringResource(R.string.nicma_price_dialog_edit_title)
                },
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro)) {
                SkydownPremiumTextField(
                    value = titleField,
                    onValueChange = { titleField = it },
                    label = stringResource(R.string.nicma_price_dialog_label_title),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                SkydownPremiumTextField(
                    value = detailField,
                    onValueChange = { detailField = it },
                    label = stringResource(R.string.nicma_price_dialog_label_details),
                    singleLine = false,
                    minLines = 1,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                SkydownPremiumTextField(
                    value = priceField,
                    onValueChange = { priceField = it },
                    label = stringResource(R.string.nicma_price_dialog_label_price),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            BrandActionButton(
                text = stringResource(R.string.common_apply),
                onClick = {
                    onConfirm(
                        StudioPriceItemUi(
                            title = titleField.trim(),
                            detail = detailField.trim(),
                            price = priceField.trim(),
                        ),
                    )
                },
                accent = MaterialTheme.colorScheme.primary,
                enabled = canSave,
                compact = true,
            )
        },
        dismissButton = {
            BrandActionButton(
                text = stringResource(R.string.common_cancel),
                onClick = onDismiss,
                accent = MaterialTheme.colorScheme.primary,
                filled = false,
                compact = true,
            )
        },
    )
}

@Composable
private fun NicmaPriceRow(
    packageItem: NicmaProducerPackage,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SkydownUiTokens.messageBubbleRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingCompact),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingNano),
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

/**
 * Eine schmale Leiste (kein grosser Kartenblock) – fuer die STUDIO-Vollansicht unter der Preisliste.
 */
@Composable
fun NicmaStudioInlineLinkRow(
    instagramUrl: String,
    spotifyUrl: String?,
    youtubeUrl: String?,
    onOpenLink: (String) -> Unit,
) {
    data class Link(val label: String, val url: String)
    val links = buildList {
        if (instagramUrl.isNotBlank()) {
            add(Link("Instagram", instagramUrl))
        }
        spotifyUrl?.trim()?.takeIf { it.isNotEmpty() }?.let { add(Link("Spotify", it)) }
        youtubeUrl?.trim()?.takeIf { it.isNotEmpty() }?.let { add(Link("YouTube", it)) }
    }
    if (links.isEmpty()) {
        return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingDense),
    ) {
        links.forEach { link ->
            BrandActionButton(
                text = link.label,
                onClick = { onOpenLink(link.url) },
                accent = MaterialTheme.colorScheme.primary,
                filled = false,
                compact = true,
            )
        }
    }
}

@Composable
fun NicmaContactCard(
    instagramUrl: String,
    spotifyUrl: String?,
    youtubeUrl: String?,
    onOpenLink: (String) -> Unit,
) {
    SkydownCard {
        SectionHeader(stringResource(R.string.nicma_links_title))
        Text(
            text = stringResource(R.string.nicma_contact_platforms),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            modifier = Modifier.padding(top = 8.dp),
        )

        BrandActionButton(
            text = stringResource(R.string.nicma_instagram_label),
            onClick = { onOpenLink(instagramUrl) },
            accent = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            filled = false,
        )

        spotifyUrl?.takeIf { it.isNotBlank() }?.let { url ->
            BrandActionButton(
                text = stringResource(R.string.common_spotify),
                onClick = { onOpenLink(url) },
                accent = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                filled = false,
            )
        }

        youtubeUrl?.takeIf { it.isNotBlank() }?.let { url ->
            BrandActionButton(
                text = stringResource(R.string.common_youtube),
                onClick = { onOpenLink(url) },
                accent = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                filled = false,
            )
        }
    }
}
