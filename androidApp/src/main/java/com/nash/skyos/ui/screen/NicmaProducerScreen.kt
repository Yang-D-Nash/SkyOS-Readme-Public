package com.nash.skyos.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.nash.skyos.data.ArtistPageBrand

/**
 * Nicma: direkt die vollständige [ArtistPageScreen]-Ansicht (MUSIC: Hero, Motion, Katalog, Links;
 * STUDIO: Preisliste & STUDIO-Form). Tabs wechseln [selectedProfile] und damit `artistName`.
 */
@Composable
fun NicmaProducerScreen(
    onBack: () -> Unit,
) {
    var selectedProfile by rememberSaveable { mutableStateOf(nicmaProfileMusic) }
    BackHandler(onBack = onBack)
    ArtistPageScreen(
        artistName = selectedProfile,
        brand = ArtistPageBrand.Nicma,
        onBack = onBack,
        onNicmaProfileChange = { selectedProfile = it },
    )
}
