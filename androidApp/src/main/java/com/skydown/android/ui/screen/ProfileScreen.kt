package com.skydown.android.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.skydown.android.ui.component.SkydownTopBarTitle
import com.skydown.android.ui.component.ToastHost
import com.skydown.android.ui.component.ToastType
import com.skydown.android.ui.component.skydownPressable
import com.skydown.android.ui.component.skydownScreenBrush
import com.skydown.android.ui.component.skydownTopBarColors
import com.skydown.android.ui.model.ProfileGalleryItem
import com.skydown.android.ui.model.ProfileMediaType
import com.skydown.android.ui.theme.InstagramOrange
import com.skydown.android.ui.theme.InstagramPink
import com.skydown.android.ui.theme.InstagramPurple
import com.skydown.android.ui.viewmodel.ProfileViewModel
import com.skydown.shared.model.User
import com.skydown.shared.model.UserQuotaPlan
import com.skydown.shared.model.UserRole
import com.skydown.shared.model.resolvedQuotaPlan
import com.skydown.shared.model.resolvedRole
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var feedbackType by remember { mutableStateOf(ToastType.Info) }
    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            viewModel.uploadAvatar(uri, context.contentResolver)
        }
    }
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            viewModel.uploadMedia(ProfileMediaType.Image, uri, context.contentResolver)
        }
    }

    LaunchedEffect(uiState.toastMessage, uiState.errorMessage) {
        when {
            !uiState.toastMessage.isNullOrBlank() -> {
                feedbackMessage = uiState.toastMessage
                feedbackType = ToastType.Success
                viewModel.clearMessages()
            }

            !uiState.errorMessage.isNullOrBlank() -> {
                feedbackMessage = uiState.errorMessage
                feedbackType = ToastType.Error
                viewModel.clearMessages()
            }
        }
    }

    LaunchedEffect(feedbackMessage) {
        if (!feedbackMessage.isNullOrBlank()) {
            delay(3000)
            feedbackMessage = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    SkydownTopBarTitle(
                        title = "Profil",
                        subtitle = "Account, Bilder und Links.",
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurueck")
                    }
                },
                colors = skydownTopBarColors(),
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(skydownScreenBrush())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ProfileHeroCard(
                    uiState = uiState,
                    onOpenInstagram = {
                        normalizedInstagramUri(uiState.instagramHandle)?.let {
                            openExternalUri(
                                context = context,
                                uri = it,
                                missingMessage = "Instagram konnte nicht geoeffnet werden.",
                            )
                        }
                    },
                    onOpenWhatsApp = {
                        normalizedWhatsAppUri(uiState.whatsApp)?.let {
                            openExternalUri(
                                context = context,
                                uri = it,
                                missingMessage = "WhatsApp konnte nicht geoeffnet werden.",
                            )
                        }
                    },
                    onEditToggle = { viewModel.setEditing(!uiState.isEditing) },
                    onPickAvatar = {
                        avatarPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    onDeleteAvatar = if (!uiState.currentUser?.profileImageURL.isNullOrBlank()) {
                        viewModel::deleteAvatar
                    } else {
                        null
                    },
                    canToggleEditing = !uiState.isSavingProfile && !uiState.isUploadingAvatar && !uiState.isUploadingMedia,
                    onPickGalleryImage = {
                        imagePicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                )

                if (uiState.isUploadingAvatar || uiState.isUploadingMedia) {
                    ProfileUploadStatusCard(
                        title = if (uiState.isUploadingAvatar) {
                            "Profilbild wird hochgeladen"
                        } else {
                            "Galeriebild wird hochgeladen"
                        },
                        detail = if (uiState.isUploadingAvatar) {
                            "Dein Avatar wird vorbereitet, hochgeladen und danach direkt im Profil aktualisiert."
                        } else {
                            "Das Bild landet gleich in deiner Galerie und wird danach automatisch angezeigt."
                        },
                    )
                }

                ProfileGalleryCard(
                    uiState = uiState,
                    onPickImage = {
                        imagePicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    onOpenItem = { item ->
                        openExternalLink(
                            context = context,
                            url = item.mediaUrl,
                            browserMissingMessage = "Medieninhalt konnte nicht geoeffnet werden.",
                        )
                    },
                    onDeleteItem = if (uiState.canEditCurrentProfile) {
                        viewModel::deleteGalleryItem
                    } else {
                        null
                    },
                )

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

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(
                                    onClick = { viewModel.setEditing(false) },
                                    modifier = Modifier.weight(1f),
                                    enabled = !uiState.isSavingProfile,
                                ) {
                                    Text("Abbrechen")
                                }
                                Button(
                                    onClick = viewModel::saveProfile,
                                    modifier = Modifier.weight(1f),
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

            ToastHost(
                message = feedbackMessage,
                type = feedbackType,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
            )
        }
    }
}

@Composable
private fun ProfileHeroCard(
    uiState: com.skydown.android.ui.viewmodel.ProfileUiState,
    onOpenInstagram: () -> Unit,
    onOpenWhatsApp: () -> Unit,
    onEditToggle: () -> Unit,
    onPickAvatar: () -> Unit,
    onDeleteAvatar: (() -> Unit)?,
    canToggleEditing: Boolean,
    onPickGalleryImage: () -> Unit,
) {
    val isUploadingImageFlow = uiState.isUploadingAvatar || uiState.isUploadingMedia

    SkydownCard(contentPadding = PaddingValues(0.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(292.dp)
                .clip(RoundedCornerShape(28.dp)),
        ) {
            val heroImage = uiState.currentUser?.profileImageURL
                ?: uiState.filteredItems.firstOrNull()?.thumbnailUrl
                ?: uiState.filteredItems.firstOrNull()?.mediaUrl

            if (!heroImage.isNullOrBlank()) {
                AsyncImage(
                    model = heroImage,
                    contentDescription = "Profil Header",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.42f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.22f),
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
                            listOf(
                                androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.14f),
                                androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.42f),
                                androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.82f),
                            ),
                        ),
                    ),
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProfileHeroInfoPill(title = "Rolle", value = profileRoleTitle(uiState.currentUser))
                    ProfileHeroInfoPill(title = "Plan", value = profilePlanTitle(uiState.currentUser))
                }

                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        ProfileAvatar(
                            imageUrl = uiState.currentUser?.profileImageURL,
                            fallbackText = uiState.username.ifBlank { "G" },
                            size = 98.dp,
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = uiState.username.ifBlank { "Skydown User" },
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = androidx.compose.ui.graphics.Color.White,
                            )

                            if (uiState.profileTagline.isNotBlank()) {
                                Text(
                                    text = uiState.profileTagline,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.88f),
                                )
                            }

                            if (uiState.profileBio.isNotBlank()) {
                                Text(
                                    text = uiState.profileBio,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.72f),
                                    maxLines = 3,
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (normalizedInstagramUri(uiState.instagramHandle) != null) {
                                    ProfileSocialButton(
                                        title = "Instagram",
                                        icon = Icons.Default.CameraAlt,
                                        colors = listOf(InstagramPurple, InstagramPink, InstagramOrange),
                                        onClick = onOpenInstagram,
                                    )
                                }

                                if (normalizedWhatsAppUri(uiState.whatsApp) != null) {
                                    ProfileSocialButton(
                                        title = "WhatsApp",
                                        icon = Icons.AutoMirrored.Filled.Message,
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.tertiary,
                                        ),
                                        onClick = onOpenWhatsApp,
                                    )
                                }
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProfileHeroInfoPill(title = "Bilder", value = uiState.imageCount.toString(), modifier = Modifier.weight(1f))
                        ProfileHeroInfoPill(
                            title = "Links",
                            value = listOfNotNull(
                                normalizedInstagramUri(uiState.instagramHandle),
                                normalizedWhatsAppUri(uiState.whatsApp),
                            ).size.toString(),
                            modifier = Modifier.weight(1f),
                        )
                        ProfileHeroInfoPill(
                            title = "Status",
                            value = if (uiState.canEditCurrentProfile) "Live" else "Public",
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            if (uiState.canEditCurrentProfile) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    ProfileHeroFab(
                        icon = Icons.Default.Edit,
                        contentDescription = if (uiState.isEditing) "Bearbeitung beenden" else "Profil bearbeiten",
                        onClick = onEditToggle,
                        enabled = canToggleEditing,
                    )
                    ProfileHeroFab(
                        icon = Icons.Default.CameraAlt,
                        contentDescription = "Avatar aendern",
                        onClick = onPickAvatar,
                        enabled = !isUploadingImageFlow,
                    )
                    if (onDeleteAvatar != null) {
                        ProfileHeroFab(
                            icon = Icons.Default.Delete,
                            contentDescription = "Avatar entfernen",
                            onClick = onDeleteAvatar,
                            enabled = !isUploadingImageFlow,
                        )
                    }
                    ProfileHeroFab(
                        icon = Icons.Default.PermMedia,
                        contentDescription = "Bild hochladen",
                        onClick = onPickGalleryImage,
                        enabled = !isUploadingImageFlow,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileGalleryCard(
    uiState: com.skydown.android.ui.viewmodel.ProfileUiState,
    onPickImage: () -> Unit,
    onOpenItem: (ProfileGalleryItem) -> Unit,
    onDeleteItem: ((ProfileGalleryItem) -> Unit)?,
) {
    SkydownCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Galerie",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Persoenliche Bilder direkt aus dem Profil.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (uiState.canEditCurrentProfile) {
                    ProfileGalleryActionButton(
                        title = if (uiState.isUploadingMedia) "Laedt..." else "Bild",
                        icon = Icons.Default.PermMedia,
                        onClick = onPickImage,
                        enabled = !uiState.isUploadingAvatar && !uiState.isUploadingMedia,
                    )
                }
            }

            if (uiState.filteredItems.isEmpty()) {
                ProfileGalleryEmptyState(canEdit = uiState.canEditCurrentProfile)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    uiState.filteredItems.chunked(2).forEach { rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            rowItems.forEach { item ->
                                GalleryTile(
                                    item = item,
                                    modifier = Modifier.weight(1f),
                                    canDelete = uiState.canEditCurrentProfile,
                                    onDelete = if (onDeleteItem != null) {
                                        { onDeleteItem(item) }
                                    } else {
                                        null
                                    },
                                ) {
                                    onOpenItem(item)
                                }
                            }
                            repeat(2 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
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
    size: androidx.compose.ui.unit.Dp = 94.dp,
) {
    Box(
        modifier = Modifier
            .size(size)
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
private fun ProfileHeroInfoPill(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.10f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            color = androidx.compose.ui.graphics.Color.White,
        )
        Text(
            text = title,
            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.68f),
        )
    }
}

@Composable
private fun ProfileSocialButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    colors: List<androidx.compose.ui.graphics.Color>,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            contentColor = androidx.compose.ui.graphics.Color.White,
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Brush.linearGradient(colors)),
    ) {
        Icon(icon, contentDescription = null)
        Spacer(modifier = Modifier.width(6.dp))
        Text(title)
    }
}

@Composable
private fun ProfileHeroFab(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    FloatingActionButton(
        onClick = {
            if (enabled) {
                onClick()
            }
        },
        modifier = Modifier
            .size(48.dp)
            .alpha(if (enabled) 1f else 0.58f),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (enabled) 0.92f else 0.78f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Icon(icon, contentDescription = contentDescription)
    }
}

@Composable
private fun ProfileGalleryActionButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Icon(icon, contentDescription = null)
        Spacer(modifier = Modifier.width(6.dp))
        Text(title)
    }
}

@Composable
private fun ProfileUploadStatusCard(
    title: String,
    detail: String,
) {
    SkydownCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.PermMedia,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun ProfileGalleryEmptyState(
    canEdit: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.74f))
            .padding(horizontal = 18.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.PermMedia,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = if (canEdit) "Mach dein Profil mit den ersten Bildern lebendig." else "Hier erscheinen die Bilder dieses Profils.",
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = if (canEdit) "Ueber den Button oben kannst du direkt vom Handy hochladen." else "Sobald Bilder hinterlegt sind, wird die Galerie hier sichtbar.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun GalleryTile(
    item: ProfileGalleryItem,
    modifier: Modifier = Modifier,
    canDelete: Boolean = false,
    onDelete: (() -> Unit)? = null,
    onOpen: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .skydownPressable(interactionSource, pressedScale = 0.986f)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.84f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onOpen,
            ),
        contentAlignment = Alignment.BottomStart,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            AsyncImage(
                model = item.thumbnailUrl ?: item.mediaUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(156.dp),
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
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = item.title,
                    color = androidx.compose.ui.graphics.Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
                if (!item.caption.isNullOrBlank()) {
                    Text(
                        text = item.caption.orEmpty(),
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.74f),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                }
            }
        }

        if (canDelete && onDelete != null) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.52f)),
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Bild entfernen",
                    tint = androidx.compose.ui.graphics.Color.White,
                )
            }
        }
    }
}

private fun normalizedInstagramUri(handle: String): Uri? {
    val trimmed = handle.trim()
    if (trimmed.isBlank()) return null
    return if (trimmed.contains("instagram.com", ignoreCase = true)) {
        Uri.parse(trimmed)
    } else {
        Uri.parse("https://www.instagram.com/${trimmed.removePrefix("@")}")
    }
}

private fun normalizedWhatsAppUri(rawValue: String): Uri? {
    val trimmed = rawValue.trim()
    if (trimmed.isBlank()) return null
    if (trimmed.startsWith("http", ignoreCase = true)) {
        return Uri.parse(trimmed)
    }

    val digits = trimmed.filter(Char::isDigit)
    if (digits.isBlank()) return null
    return Uri.parse("https://wa.me/$digits")
}

private fun profileRoleTitle(user: User?): String {
    return when (user?.resolvedRole) {
        UserRole.Owner -> "Owner"
        UserRole.Admin -> "Admin"
        UserRole.Subadmin -> "Creator"
        else -> "User"
    }
}

private fun profilePlanTitle(user: User?): String {
    return when (user?.resolvedQuotaPlan) {
        UserQuotaPlan.OwnerUnlimited -> "Unlimited"
        UserQuotaPlan.InternalTeam -> "Team"
        UserQuotaPlan.Creator -> "Creator"
        UserQuotaPlan.Studio -> "Studio"
        else -> "Free"
    }
}
