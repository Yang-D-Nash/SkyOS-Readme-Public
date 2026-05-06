package com.nash.skyos.ui.screen

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.nash.skyos.R
import com.nash.skyos.data.AppContainer
import com.nash.skyos.ui.component.BrandActionButton
import com.nash.skyos.ui.component.SkydownCard
import com.nash.skyos.ui.component.SkydownPremiumLinearProgress
import com.nash.skyos.ui.component.SkydownPremiumStatePanel
import com.nash.skyos.ui.component.SkydownPremiumTextField
import com.nash.skyos.ui.component.SkydownTopBarTitle
import com.nash.skyos.ui.component.SkydownUiTokens
import com.nash.skyos.ui.component.ToastHost
import com.nash.skyos.ui.component.ToastType
import com.nash.skyos.ui.component.skydownPressable
import com.nash.skyos.ui.component.skydownScreenBrush
import com.nash.skyos.ui.component.skydownTopBarColors
import com.nash.skyos.ui.model.ProfileGalleryItem
import com.nash.skyos.ui.model.ProfileMediaType
import com.nash.skyos.ui.viewmodel.ProfileViewModel
import com.skydown.shared.model.User
import com.skydown.shared.model.UserQuotaPlan
import com.skydown.shared.model.UserRole
import com.skydown.shared.model.resolvedQuotaPlan
import com.skydown.shared.model.resolvedRole
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val legalSettings by AppContainer.legalContentRepository.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val profileOpenInstagramFailed = stringResource(R.string.profile_open_instagram_failed)
    val profileOpenWhatsAppFailed = stringResource(R.string.profile_open_whatsapp_failed)
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var feedbackType by remember { mutableStateOf(ToastType.Info) }
    var selectedGalleryItem by remember { mutableStateOf<ProfileGalleryItem?>(null) }
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
                        title = stringResource(R.string.profile_title),
                        subtitle = stringResource(R.string.profile_subtitle),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
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
                verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingRelaxed),
            ) {
                ProfileHeroCard(
                    uiState = uiState,
                    onOpenInstagram = {
                        normalizedInstagramUri(uiState.instagramHandle)?.let {
                            openExternalUri(
                                context = context,
                                uri = it,
                                missingMessage = profileOpenInstagramFailed,
                            )
                        }
                    },
                    onOpenWhatsApp = {
                        normalizedWhatsAppUri(uiState.whatsApp)?.let {
                            openExternalUri(
                                context = context,
                                uri = it,
                                missingMessage = profileOpenWhatsAppFailed,
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

                ProfileDashboardCard(uiState = uiState)

                ProfileQuickActionsCard(
                    isEditing = uiState.isEditing,
                    canEdit = uiState.canEditCurrentProfile,
                    canTriggerPrimaryActions = uiState.canEditCurrentProfile
                        && !uiState.isSavingProfile
                        && !uiState.isUploadingAvatar
                        && !uiState.isUploadingMedia,
                    onToggleEdit = { viewModel.setEditing(!uiState.isEditing) },
                )

                ProfileHistoryCard(uiState = uiState)

                if (uiState.isUploadingAvatar || uiState.isUploadingMedia) {
                    ProfileUploadStatusCard(
                        title = if (uiState.isUploadingAvatar) {
                            stringResource(R.string.profile_upload_avatar_title)
                        } else {
                            stringResource(R.string.profile_upload_gallery_title)
                        },
                        detail = if (uiState.isUploadingAvatar) {
                            stringResource(R.string.profile_upload_avatar_detail)
                        } else {
                            stringResource(R.string.profile_upload_gallery_detail)
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
                        selectedGalleryItem = item
                    },
                    onDeleteItem = if (uiState.canEditCurrentProfile) {
                        viewModel::deleteGalleryItem
                    } else {
                        null
                    },
                )

                ProfileTrustCard(
                    supportEmail = legalSettings.resolvedSupportEmail,
                    onSupport = onOpenSettings,
                    onOpenSettings = onOpenSettings,
                )

                if (uiState.isEditing && uiState.canEditCurrentProfile) {
                    SkydownCard {
                        Column(verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingCompact)) {
                            Text(
                                text = stringResource(R.string.profile_edit_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )

                            SkydownPremiumTextField(
                                value = uiState.username,
                                onValueChange = viewModel::updateUsername,
                                label = stringResource(R.string.settings_profile_username),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            SkydownPremiumTextField(
                                value = uiState.profileTagline,
                                onValueChange = viewModel::updateProfileTagline,
                                label = stringResource(R.string.settings_profile_tagline),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            SkydownPremiumTextField(
                                value = uiState.instagramHandle,
                                onValueChange = viewModel::updateInstagramHandle,
                                label = stringResource(R.string.settings_profile_instagram),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            SkydownPremiumTextField(
                                value = uiState.whatsApp,
                                onValueChange = viewModel::updateWhatsApp,
                                label = stringResource(R.string.settings_profile_whatsapp),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            SkydownPremiumTextField(
                                value = uiState.profileBio,
                                onValueChange = viewModel::updateProfileBio,
                                label = stringResource(R.string.settings_profile_bio),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = false,
                                minLines = 4,
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingCompact)) {
                                BrandActionButton(
                                    text = stringResource(R.string.common_cancel),
                                    onClick = { viewModel.setEditing(false) },
                                    accent = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f),
                                    filled = false,
                                    enabled = !uiState.isSavingProfile,
                                )
                                BrandActionButton(
                                    text = if (uiState.isSavingProfile) {
                                        stringResource(R.string.profile_saving)
                                    } else {
                                        stringResource(R.string.common_save)
                                    },
                                    onClick = viewModel::saveProfile,
                                    accent = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Default.Save,
                                    isLoading = uiState.isSavingProfile,
                                )
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

            selectedGalleryItem?.let { item ->
                ProfileImageViewerDialog(
                    item = item,
                    onDismiss = { selectedGalleryItem = null },
                )
            }
        }
    }
}

@Composable
private fun ProfileHeroCard(
    uiState: com.nash.skyos.ui.viewmodel.ProfileUiState,
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
                .clip(RoundedCornerShape(SkydownUiTokens.spotlightRadius)),
        ) {
            val heroImage = uiState.currentUser?.profileImageURL
                ?: uiState.filteredItems.firstOrNull()?.thumbnailUrl
                ?: uiState.filteredItems.firstOrNull()?.mediaUrl

            if (!heroImage.isNullOrBlank()) {
                AsyncImage(
                    model = heroImage,
                    contentDescription = stringResource(R.string.profile_header_cd),
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
                Row(horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro)) {
                    ProfileHeroInfoPill(title = stringResource(R.string.profile_role), value = profileRoleTitle(uiState.currentUser))
                    ProfileHeroInfoPill(title = stringResource(R.string.membership_current_plan), value = profilePlanTitle(uiState.currentUser))
                }

                Column(verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingRelaxed)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingRelaxed),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        ProfileAvatar(
                            imageUrl = uiState.currentUser?.profileImageURL,
                            fallbackText = uiState.username.ifBlank { "G" },
                            size = 98.dp,
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
                        ) {
                            Text(
                                text = uiState.username.ifBlank { "SkyOS User" },
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = androidx.compose.ui.graphics.Color.White,
                                maxLines = 1,
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
                                    maxLines = 2,
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro)) {
                                if (normalizedInstagramUri(uiState.instagramHandle) != null) {
                                    ProfileSocialButton(
                                        title = stringResource(R.string.settings_profile_instagram),
                                        icon = Icons.Default.CameraAlt,
                                        onClick = onOpenInstagram,
                                    )
                                }

                                if (normalizedWhatsAppUri(uiState.whatsApp) != null) {
                                    ProfileSocialButton(
                                        title = stringResource(R.string.settings_profile_whatsapp),
                                        icon = Icons.AutoMirrored.Filled.Message,
                                        onClick = onOpenWhatsApp,
                                    )
                                }
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro)) {
                        ProfileHeroInfoPill(title = stringResource(R.string.profile_images), value = uiState.imageCount.toString(), modifier = Modifier.weight(1f))
                        ProfileHeroInfoPill(
                            title = stringResource(R.string.profile_links),
                            value = listOfNotNull(
                                normalizedInstagramUri(uiState.instagramHandle),
                                normalizedWhatsAppUri(uiState.whatsApp),
                            ).size.toString(),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            if (uiState.canEditCurrentProfile) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(SkydownUiTokens.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill),
                    horizontalAlignment = Alignment.End,
                ) {
                    ProfileHeroFab(
                        icon = Icons.Default.Edit,
                        contentDescription = if (uiState.isEditing) stringResource(R.string.profile_finish_editing) else stringResource(R.string.profile_edit),
                        onClick = onEditToggle,
                        enabled = canToggleEditing,
                    )
                    ProfileHeroFab(
                        icon = Icons.Default.CameraAlt,
                        contentDescription = stringResource(R.string.profile_change_avatar),
                        onClick = onPickAvatar,
                        enabled = !isUploadingImageFlow,
                    )
                    if (onDeleteAvatar != null) {
                        ProfileHeroFab(
                            icon = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.profile_remove_avatar),
                            onClick = onDeleteAvatar,
                            enabled = !isUploadingImageFlow,
                        )
                    }
                    ProfileHeroFab(
                        icon = Icons.Default.PermMedia,
                        contentDescription = stringResource(R.string.profile_upload_image),
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
    uiState: com.nash.skyos.ui.viewmodel.ProfileUiState,
    onPickImage: () -> Unit,
    onOpenItem: (ProfileGalleryItem) -> Unit,
    onDeleteItem: ((ProfileGalleryItem) -> Unit)?,
) {
    SkydownCard {
        Column(verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingRelaxed)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingNano)) {
                    Text(
                        text = stringResource(R.string.profile_gallery),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.profile_gallery_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (uiState.canEditCurrentProfile) {
                    ProfileGalleryActionButton(
                        title = if (uiState.isUploadingMedia) stringResource(R.string.profile_loading) else stringResource(R.string.profile_image_short),
                        icon = Icons.Default.PermMedia,
                        onClick = onPickImage,
                        enabled = !uiState.isUploadingAvatar && !uiState.isUploadingMedia,
                    )
                }
            }

            if (uiState.filteredItems.isEmpty()) {
                ProfileGalleryEmptyState(canEdit = uiState.canEditCurrentProfile)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingCompact)) {
                    uiState.filteredItems.chunked(2).forEach { rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingCompact)) {
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
private fun ProfileDashboardCard(
    uiState: com.nash.skyos.ui.viewmodel.ProfileUiState,
) {
    val user = uiState.currentUser
    val membership = user?.resolvedQuotaPlan?.rawValue?.replace('_', ' ')?.replaceFirstChar { it.uppercase() }
        ?: stringResource(R.string.profile_plan_free)
    val aiStatus = when {
        user == null -> stringResource(R.string.profile_ai_status_guest)
        !user.aiAccessEnabled -> stringResource(R.string.profile_ai_status_paused)
        !user.aiSubscriptionProvider.isNullOrBlank() -> stringResource(R.string.profile_ai_status_premium)
        else -> stringResource(R.string.profile_ai_status_base)
    }

    SkydownCard {
        Column(verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingCompact)) {
            Text(stringResource(R.string.profile_dashboard_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(R.string.profile_dashboard_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill)) {
                ProfileDashboardMetric(stringResource(R.string.profile_membership), membership, Modifier.weight(1f))
                ProfileDashboardMetric(stringResource(R.string.profile_ai), aiStatus, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill)) {
                ProfileDashboardMetric(stringResource(R.string.profile_gallery), "${uiState.imageCount} ${stringResource(R.string.profile_images)}", Modifier.weight(1f))
                ProfileDashboardMetric(stringResource(R.string.profile_role), profileRoleTitle(user), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ProfileQuickActionsCard(
    isEditing: Boolean,
    canEdit: Boolean,
    canTriggerPrimaryActions: Boolean,
    onToggleEdit: () -> Unit,
) {
    SkydownCard {
        Column(verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingCompact)) {
            Text(stringResource(R.string.profile_quick_actions_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(R.string.profile_quick_actions_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (canEdit) {
                BrandActionButton(
                    text = if (isEditing) stringResource(R.string.profile_done) else stringResource(R.string.profile_edit),
                    onClick = onToggleEdit,
                    accent = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canTriggerPrimaryActions,
                )
            }

        }
    }
}

@Composable
private fun ProfileHistoryCard(
    uiState: com.nash.skyos.ui.viewmodel.ProfileUiState,
) {
    val latest = uiState.filteredItems.maxByOrNull { it.createdAtEpochMillis }?.createdAtEpochMillis
    val latestLabel = latest?.let { formatProfileDate(it) } ?: stringResource(R.string.profile_history_none_yet)
    val memberSince = uiState.currentUser?.registrationDateEpochMillis?.let(::formatProfileDate) ?: "-"

    SkydownCard {
        Column(verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill)) {
            Text(stringResource(R.string.profile_history_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(R.string.profile_history_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ProfileHistoryRow(stringResource(R.string.profile_history_latest_created), latestLabel)
            ProfileHistoryRow(stringResource(R.string.profile_history_member_since), memberSince)
            ProfileHistoryRow(
                stringResource(R.string.profile_history_current_plan),
                stringResource(R.string.profile_plan_prefix, profilePlanTitle(uiState.currentUser)),
            )
        }
    }
}

@Composable
private fun ProfileTrustCard(
    supportEmail: String,
    onSupport: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    SkydownCard {
        Column(verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill)) {
            Text(stringResource(R.string.profile_trust_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(R.string.profile_trust_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (supportEmail.isNotBlank()) {
                Text(
                    supportEmail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BrandActionButton(
                text = stringResource(R.string.profile_support),
                onClick = onSupport,
                accent = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                filled = false,
            )
            BrandActionButton(
                text = stringResource(R.string.profile_privacy_account),
                onClick = onOpenSettings,
                accent = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                filled = false,
            )
        }
    }
}

@Composable
private fun ProfileDashboardMetric(title: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(SkydownUiTokens.compactRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingNano),
    ) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ProfileHistoryRow(title: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
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
                contentDescription = stringResource(R.string.profile_avatar_cd),
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
            .clip(RoundedCornerShape(SkydownUiTokens.messageBubbleRadius))
            .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingNano),
    ) {
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            color = androidx.compose.ui.graphics.Color.White,
        )
        Text(
            text = title,
            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.68f),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun ProfileSocialButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    BrandActionButton(
        text = title,
        onClick = onClick,
        accent = Color.White,
        icon = icon,
        filled = false,
        compact = true,
    )
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
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (enabled) 0.88f else 0.72f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp,
        ),
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
    BrandActionButton(
        text = title,
        onClick = onClick,
        accent = MaterialTheme.colorScheme.primary,
        icon = icon,
        compact = true,
        enabled = enabled,
    )
}

@Composable
private fun ProfileUploadStatusCard(
    title: String,
    detail: String,
) {
    SkydownCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingRelaxed),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.PermMedia,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingDense),
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
                SkydownPremiumLinearProgress(
                    modifier = Modifier.fillMaxWidth(),
                    accent = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun ProfileGalleryEmptyState(
    canEdit: Boolean,
) {
    SkydownPremiumStatePanel(
        title = if (canEdit) {
            stringResource(R.string.profile_gallery_empty_editable_title)
        } else {
            stringResource(R.string.profile_gallery_empty_readonly_title)
        },
        body = if (canEdit) {
            stringResource(R.string.profile_gallery_empty_editable_subtitle)
        } else {
            stringResource(R.string.profile_gallery_empty_readonly_subtitle)
        },
        icon = Icons.Default.PermMedia,
        accent = MaterialTheme.colorScheme.primary,
    )
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
            .clip(RoundedCornerShape(SkydownUiTokens.messageBubbleRadius))
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
            Column(verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingHairline)) {
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
                    contentDescription = stringResource(R.string.common_remove),
                    tint = androidx.compose.ui.graphics.Color.White,
                )
            }
        }
    }
}

@Composable
private fun ProfileImageViewerDialog(
    item: ProfileGalleryItem,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.94f)),
        ) {
            AsyncImage(
                model = item.mediaUrl,
                contentDescription = item.title,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 48.dp),
                contentScale = ContentScale.Fit,
            )

            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingComfortable),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    if (!item.caption.isNullOrBlank()) {
                        Text(
                            text = item.caption.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.72f),
                            maxLines = 1,
                        )
                    }
                }

                BrandActionButton(
                    text = stringResource(R.string.common_close),
                    onClick = onDismiss,
                    accent = MaterialTheme.colorScheme.onPrimary,
                    icon = Icons.Default.Close,
                    filled = false,
                    compact = true,
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

private fun formatProfileDate(epochMillis: Long): String {
    return runCatching {
        SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(epochMillis))
    }.getOrDefault("-")
}
