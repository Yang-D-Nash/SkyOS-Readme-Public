package com.skydown.android.ui.screen

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.skydown.android.ui.component.SkydownCard
import com.skydown.android.ui.component.skydownScreenBrush
import com.skydown.android.ui.component.skydownTopBarColors
import com.skydown.android.ui.component.skydownSheen
import com.skydown.android.ui.model.ProfileGalleryItem
import com.skydown.android.ui.model.ProfileMediaType
import com.skydown.android.ui.theme.InstagramOrange
import com.skydown.android.ui.theme.InstagramPink
import com.skydown.android.ui.theme.InstagramPurple
import com.skydown.android.ui.theme.TextMutedDark
import com.skydown.android.ui.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            viewModel.uploadAvatar(uri, context.contentResolver.getType(uri))
        }
    }
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            viewModel.uploadMedia(ProfileMediaType.Image, uri, context.contentResolver.getType(uri))
        }
    }

    LaunchedEffect(uiState.toastMessage, uiState.errorMessage) {
        val message = uiState.toastMessage ?: uiState.errorMessage ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        viewModel.clearMessages()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profil") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurueck")
                    }
                },
                actions = {
                    if (uiState.canEditCurrentProfile) {
                        Button(
                            onClick = { viewModel.setEditing(!uiState.isEditing) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        ) {
                            Text(if (uiState.isEditing) "Fertig" else "Bearbeiten")
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
            SkydownCard {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            ProfileAvatar(
                                imageUrl = uiState.currentUser?.profileImageURL,
                                fallbackText = uiState.username.ifBlank { "G" },
                            )

                            if (uiState.canEditCurrentProfile) {
                                IconButton(
                                    onClick = {
                                        avatarPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                        )
                                    },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                ) {
                                    if (uiState.isUploadingAvatar) {
                                        Text("...", color = MaterialTheme.colorScheme.onPrimary)
                                    } else {
                                        Icon(
                                            Icons.Default.CameraAlt,
                                            contentDescription = "Profilbild",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                        )
                                    }
                                }
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = uiState.username.ifBlank { "Skydown User" },
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                            )

                            if (uiState.profileTagline.isNotBlank()) {
                                Text(
                                    text = uiState.profileTagline,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            if (uiState.profileBio.isNotBlank()) {
                                Text(
                                    text = uiState.profileBio,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            if (uiState.instagramHandle.isNotBlank()) {
                                Button(
                                    onClick = {
                                        context.startActivity(
                                            Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse("https://www.instagram.com/${uiState.instagramHandle.removePrefix("@")}"),
                                            ),
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                                        contentColor = androidx.compose.ui.graphics.Color.White,
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    InstagramPurple,
                                                    InstagramPink,
                                                    InstagramOrange,
                                                ),
                                            ),
                                        )
                                        .skydownSheen(accent = androidx.compose.ui.graphics.Color.White, alpha = 0.12f),
                                ) {
                                    Text("@${uiState.instagramHandle.removePrefix("@")}")
                                }
                            }
                        }
                    }

                    ProfileStatChip("Bilder", uiState.imageCount, Modifier.fillMaxWidth())
                }
            }

            SkydownCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Galerie",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProfileMediaType.entries.forEach { type ->
                            Button(
                                onClick = { viewModel.selectMediaType(type) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (uiState.selectedMediaType == type) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    contentColor = if (uiState.selectedMediaType == type) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
                            ) {
                                Text(type.title)
                            }
                        }
                    }

                    if (uiState.canEditCurrentProfile) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ProfileUploadButton(
                                title = "Bild",
                                icon = Icons.Default.PermMedia,
                                modifier = Modifier.weight(1f),
                            ) {
                                imagePicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                )
                            }
                        }
                    }

                    if (uiState.filteredItems.isEmpty()) {
                        Text(
                            text = "Noch nichts drin.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            uiState.filteredItems.chunked(3).forEach { rowItems ->
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    rowItems.forEach { item ->
                                        GalleryTile(
                                            item = item,
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            context.startActivity(
                                                Intent(Intent.ACTION_VIEW, Uri.parse(item.mediaUrl)),
                                            )
                                        }
                                    }
                                    repeat(3 - rowItems.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (uiState.isEditing && uiState.canEditCurrentProfile) {
                SkydownCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Profil bearbeiten",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )

                        OutlinedTextField(
                            value = uiState.username,
                            onValueChange = viewModel::updateUsername,
                            label = { Text("Benutzername") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = uiState.profileTagline,
                            onValueChange = viewModel::updateProfileTagline,
                            label = { Text("Kurzinfo") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = uiState.instagramHandle,
                            onValueChange = viewModel::updateInstagramHandle,
                            label = { Text("Instagram") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = uiState.whatsApp,
                            onValueChange = viewModel::updateWhatsApp,
                            label = { Text("WhatsApp") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = uiState.profileBio,
                            onValueChange = viewModel::updateProfileBio,
                            label = { Text("Bio") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4,
                        )

                        Button(
                            onClick = viewModel::saveProfile,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isSavingProfile,
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (uiState.isSavingProfile) "Speichert..." else "Speichern")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileAvatar(
    imageUrl: String?,
    fallbackText: String,
) {
    Box(
        modifier = Modifier
            .size(94.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f),
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Profilbild",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = fallbackText.take(1).uppercase(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun ProfileStatChip(
    title: String,
    value: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = value.toString(), fontWeight = FontWeight.Bold)
        Text(text = title, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ProfileUploadButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
    ) {
        Icon(icon, contentDescription = null)
        Spacer(modifier = Modifier.width(6.dp))
        Text(title)
    }
}

@Composable
private fun GalleryTile(
    item: ProfileGalleryItem,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.84f))
            .clickable(onClick = onOpen),
        contentAlignment = Alignment.BottomStart,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            AsyncImage(
                model = item.thumbnailUrl ?: item.mediaUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .size(110.dp),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            androidx.compose.ui.graphics.Color.Transparent,
                            androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.58f),
                        ),
                    ),
                )
                .padding(8.dp),
        ) {
            Text(
                text = item.title,
                color = androidx.compose.ui.graphics.Color.White,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun AudioGalleryRow(
    item: ProfileGalleryItem,
    onOpen: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f))
            .clickable(onClick = onOpen)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.MusicNote, contentDescription = null)
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.title, fontWeight = FontWeight.SemiBold)
            Text(
                text = item.caption ?: "Direkt aus deinem Profil.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
