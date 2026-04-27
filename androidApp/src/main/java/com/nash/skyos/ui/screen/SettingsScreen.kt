package com.nash.skyos.ui.screen

import android.Manifest
import androidx.annotation.StringRes
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nash.skyos.R
import com.nash.skyos.data.AppContainer
import com.nash.skyos.data.AiFaqOwnerReviewLoop
import com.nash.skyos.data.AiRuntimeAgentProvider
import com.nash.skyos.data.ArtistPageBrand
import com.nash.skyos.data.ArtistPageUi
import com.nash.skyos.data.ArtistPagesStore
import com.nash.skyos.data.LegalContentSettings
import com.nash.skyos.data.MembershipOpsExperimentDraft
import com.nash.skyos.data.MembershipOpsRecommendation
import com.nash.skyos.data.NotificationPermissionCoordinator
import com.nash.skyos.data.ScreenHeaderSettings
import com.nash.skyos.ui.component.EditableImageFieldCard
import com.nash.skyos.ui.component.SectionHeader
import com.nash.skyos.ui.component.SkydownCard
import com.nash.skyos.ui.component.SkydownTopBarTitle
import com.nash.skyos.ui.component.SkydownUiTokens
import com.nash.skyos.ui.component.ToastHost
import com.nash.skyos.ui.component.ToastType
import com.nash.skyos.ui.component.rememberSkydownScreenSectionSpacing
import com.nash.skyos.ui.component.skydownContentPadding
import com.nash.skyos.ui.component.skydownPressable
import com.nash.skyos.ui.component.skydownScreenBrush
import com.nash.skyos.ui.component.skydownTopBarColors
import com.nash.skyos.ui.model.SettingsLegalDocumentType
import com.nash.skyos.ui.model.SettingsUiState
import com.nash.skyos.ui.model.resolve
import com.nash.skyos.ui.theme.AppearanceMode
import com.nash.skyos.ui.viewmodel.SettingsViewModel
import com.skydown.shared.model.User
import com.skydown.shared.model.UserQuotaPlan
import com.skydown.shared.model.UserRole
import com.skydown.shared.model.isPlatformOwner
import com.skydown.shared.model.resolvedAiAgentRequestsPerDay
import com.skydown.shared.model.resolvedAiHistoryRetentionDays
import com.skydown.shared.model.resolvedAiTextRequestsPerDay
import com.skydown.shared.model.resolvedAiVisualRequestsPerDay
import com.skydown.shared.model.resolvedQuotaPlan
import com.skydown.shared.model.resolvedRole
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
private fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val resolvedInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    androidx.compose.material3.Button(
        onClick = onClick,
        modifier = modifier.skydownPressable(
            interactionSource = resolvedInteractionSource,
            pressedScale = 0.984f,
        ),
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = resolvedInteractionSource,
        content = content,
    )
}

@Composable
private fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.outlinedShape,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val resolvedInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        modifier = modifier.skydownPressable(
            interactionSource = resolvedInteractionSource,
            pressedScale = 0.986f,
        ),
        enabled = enabled,
        shape = shape,
        colors = colors,
        border = border ?: ButtonDefaults.outlinedButtonBorder(enabled),
        contentPadding = contentPadding,
        interactionSource = resolvedInteractionSource,
        content = content,
    )
}

@Composable
private fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val resolvedInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    androidx.compose.material3.TextButton(
        onClick = onClick,
        modifier = modifier.skydownPressable(
            interactionSource = resolvedInteractionSource,
            pressedScale = 0.988f,
        ),
        enabled = enabled,
        colors = colors,
        contentPadding = contentPadding,
        interactionSource = resolvedInteractionSource,
        content = content,
    )
}

@Composable
private fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val resolvedInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    androidx.compose.material3.IconButton(
        onClick = onClick,
        modifier = modifier.skydownPressable(
            interactionSource = resolvedInteractionSource,
            pressedScale = 0.97f,
        ),
        enabled = enabled,
        colors = colors,
        interactionSource = resolvedInteractionSource,
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    initialAdminWorkspaceKey: String? = null,
    onClose: (() -> Unit)? = null,
    onOpenLogin: () -> Unit = {},
    onOpenRegistration: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onOpenOrders: () -> Unit = {},
    onOpenOwnerHub: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val screenHeaderSettingsRepository = remember { AppContainer.screenHeaderSettingsRepository }
    val screenHeaderSettings by screenHeaderSettingsRepository.settings.collectAsStateWithLifecycle()
    val artistPages by ArtistPagesStore.pages.collectAsStateWithLifecycle()
    val artistPagesError by ArtistPagesStore.lastErrorMessage.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val editableImageAssetRepository = remember { AppContainer.editableImageAssetRepository }
    val membershipOpsRepository = remember { AppContainer.membershipOpsAdminRepository }
    val faqOwnerReviewRepository = remember { AppContainer.aiFaqOwnerReviewRepository }
    val adminHeadersOwnerOnlyMessage = stringResource(R.string.settings_admin_headers_owner_only)
    val membershipOpsExperimentStartedMessage = stringResource(R.string.settings_membership_ops_experiment_started)
    val membershipOpsRecommendationRejectedMessage = stringResource(R.string.settings_membership_ops_recommendation_rejected)
    val membershipOpsExperimentCompletedMessage = stringResource(R.string.settings_membership_ops_experiment_completed)
    val membershipOpsHygieneSavedMessage = stringResource(R.string.settings_membership_ops_hygiene_saved)
    val membershipOpsHygieneSaveFailedMessage = stringResource(R.string.settings_membership_ops_hygiene_save_failed)
    val membershipOpsHygieneDefaultsRestoredMessage = stringResource(R.string.settings_membership_ops_hygiene_defaults_restored)
    val membershipOpsTimelineLoadFailedMessage = stringResource(R.string.settings_membership_ops_timeline_load_failed)
    val membershipOpsExperimentCompleteFailedMessage =
        stringResource(R.string.settings_membership_ops_experiment_complete_failed)
    val membershipOpsResetDefaultsFailedMessage =
        stringResource(R.string.settings_membership_ops_reset_defaults_failed)
    val membershipOpsExperimentStartNotes = stringResource(R.string.settings_membership_ops_notes_started)
    val membershipOpsRejectNotes = stringResource(R.string.settings_membership_ops_notes_rejected)
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var feedbackType by remember { mutableStateOf(ToastType.Info) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val sectionSpacing = rememberSkydownScreenSectionSpacing()
    var stripeAccountHintDraft by rememberSaveable { mutableStateOf("") }
    var stripeSecretKeyDraft by rememberSaveable { mutableStateOf("") }
    var stripeWebhookSecretDraft by rememberSaveable { mutableStateOf("") }
    var paypalAccountHintDraft by rememberSaveable { mutableStateOf("") }
    var klarnaAccountHintDraft by rememberSaveable { mutableStateOf("") }
    var bankAccountHolderDraft by rememberSaveable { mutableStateOf("") }
    var bankIbanDraft by rememberSaveable { mutableStateOf("") }
    var bankBicDraft by rememberSaveable { mutableStateOf("") }
    var bankNameDraft by rememberSaveable { mutableStateOf("") }
    var bankInstructionsDraft by rememberSaveable { mutableStateOf("") }
    var domesticShippingDraft by rememberSaveable { mutableStateOf("") }
    var euShippingDraft by rememberSaveable { mutableStateOf("") }
    var internationalShippingDraft by rememberSaveable { mutableStateOf("") }
    var freeShippingThresholdDraft by rememberSaveable { mutableStateOf("") }
    var shippingNotesDraft by rememberSaveable { mutableStateOf("") }
    var invoiceCompanyNameDraft by rememberSaveable { mutableStateOf("") }
    var invoiceCompanyAddressDraft by rememberSaveable { mutableStateOf("") }
    var invoiceTaxNumberDraft by rememberSaveable { mutableStateOf("") }
    var invoiceVatIdDraft by rememberSaveable { mutableStateOf("") }
    var invoiceTaxRateDraft by rememberSaveable { mutableStateOf("") }
    var invoicePrefixDraft by rememberSaveable { mutableStateOf("") }
    var invoiceSupportEmailDraft by rememberSaveable { mutableStateOf("") }
    var shopifyStoreDomainDraft by rememberSaveable { mutableStateOf("") }
    var shopifyStorefrontAccessTokenDraft by rememberSaveable { mutableStateOf("") }
    var shopifyCollectionHandlesDraft by rememberSaveable { mutableStateOf("") }
    var shopifyCollectionSearchDraft by rememberSaveable { mutableStateOf("") }
    var homeHeaderImageUrlDraft by rememberSaveable { mutableStateOf("") }
    var homeHeaderEyebrowDraft by rememberSaveable { mutableStateOf("") }
    var homeHeaderTitleDraft by rememberSaveable { mutableStateOf("") }
    var homeHeaderSubtitleDraft by rememberSaveable { mutableStateOf("") }
    var homeHeaderDetailDraft by rememberSaveable { mutableStateOf("") }
    var musicHubHeaderImageUrlDraft by rememberSaveable { mutableStateOf("") }
    var musicHubHeaderEyebrowDraft by rememberSaveable { mutableStateOf("") }
    var musicHubHeaderTitleDraft by rememberSaveable { mutableStateOf("") }
    var musicHubHeaderSubtitleDraft by rememberSaveable { mutableStateOf("") }
    var musicHubHeaderDetailDraft by rememberSaveable { mutableStateOf("") }
    var shopHeaderImageUrlDraft by rememberSaveable { mutableStateOf("") }
    var shopHeaderEyebrowDraft by rememberSaveable { mutableStateOf("") }
    var shopHeaderTitleDraft by rememberSaveable { mutableStateOf("") }
    var shopHeaderSubtitleDraft by rememberSaveable { mutableStateOf("") }
    var shopHeaderDetailDraft by rememberSaveable { mutableStateOf("") }
    var videoHeaderImageUrlDraft by rememberSaveable { mutableStateOf("") }
    var videoHeaderHeroVideoUrlDraft by rememberSaveable { mutableStateOf("") }
    var videoHeaderEyebrowDraft by rememberSaveable { mutableStateOf("") }
    var videoHeaderTitleDraft by rememberSaveable { mutableStateOf("") }
    var videoHeaderSubtitleDraft by rememberSaveable { mutableStateOf("") }
    var videoHeaderDetailDraft by rememberSaveable { mutableStateOf("") }
    var automationProviderDraft by rememberSaveable { mutableStateOf("activepieces") }
    var automationEnabledDraft by rememberSaveable { mutableStateOf(false) }
    var automationSendsUserContextDraft by rememberSaveable { mutableStateOf(true) }
    var automationWorkflowNameDraft by rememberSaveable { mutableStateOf("") }
    var automationBaseUrlDraft by rememberSaveable { mutableStateOf("") }
    var automationWebhookPathDraft by rememberSaveable { mutableStateOf("") }
    var automationAuthHeaderNameDraft by rememberSaveable { mutableStateOf("") }
    var automationAuthHeaderValueDraft by rememberSaveable { mutableStateOf("") }
    var automationKnowledgeContextDraft by rememberSaveable { mutableStateOf("") }
    var manusByosEnabledDraft by rememberSaveable { mutableStateOf(false) }
    var manusByosApiKeyDraft by rememberSaveable { mutableStateOf("") }
    var agentProfileEnabledDraft by rememberSaveable { mutableStateOf(false) }
    var agentRoleLabelDraft by rememberSaveable { mutableStateOf("") }
    var agentSkillProfileDraft by rememberSaveable { mutableStateOf("") }
    var agentOutputFormatDraft by rememberSaveable { mutableStateOf("") }
    var agentGuardrailsDraft by rememberSaveable { mutableStateOf("") }
    var agentKnowledgeContextDraft by rememberSaveable { mutableStateOf("") }
    var aiTextInstructionDraft by rememberSaveable { mutableStateOf("") }
    var aiVisualInstructionDraft by rememberSaveable { mutableStateOf("") }
    var aiAgentSystemInstructionDraft by rememberSaveable { mutableStateOf("") }
    var aiFaqInstructionDraft by rememberSaveable { mutableStateOf("") }
    var aiFaqKnowledgeBaseDraft by rememberSaveable { mutableStateOf("") }
    var aiAssetLibraryLinkDraft by rememberSaveable { mutableStateOf("") }
    var aiAssetReferenceNotesDraft by rememberSaveable { mutableStateOf("") }
    var aiCostGuardEnabledDraft by rememberSaveable { mutableStateOf(true) }
    var aiBotPromptVersionDraft by rememberSaveable { mutableStateOf("") }
    var aiBotQualityModeDraft by rememberSaveable { mutableStateOf("balanced") }
    var aiBotFaqModeDraft by rememberSaveable { mutableStateOf("auto") }
    var aiBotOwnerModeDraft by rememberSaveable { mutableStateOf("standard") }
    var aiBotAnswerLengthDraft by rememberSaveable { mutableStateOf("adaptive") }
    var aiBotPersonalityStyleDraft by rememberSaveable { mutableStateOf("") }
    var aiBotLoggingLevelDraft by rememberSaveable { mutableStateOf("") }
    var aiBotDiagnosticsModeDraft by rememberSaveable { mutableStateOf("owner_only") }
    var aiBotKillSwitchDraft by rememberSaveable { mutableStateOf(false) }
    var aiBotTextPrimaryModelDraft by rememberSaveable { mutableStateOf("") }
    var aiBotTextFallbackModelDraft by rememberSaveable { mutableStateOf("") }
    var aiBotVisualPrimaryModelDraft by rememberSaveable { mutableStateOf("") }
    var aiBotVisualFallbackModelDraft by rememberSaveable { mutableStateOf("") }
    var aiBotCostGuardEnabledDraft by rememberSaveable { mutableStateOf(true) }
    var aiBotPreferBriefCriticalDraft by rememberSaveable { mutableStateOf(true) }
    var aiBotShortAnswerMaxTokensDraft by rememberSaveable { mutableStateOf("") }
    var aiBotStandardAnswerMaxTokensDraft by rememberSaveable { mutableStateOf("") }
    var aiBotPreferFaqRoutingDraft by rememberSaveable { mutableStateOf(true) }
    var aiBotPreferProductGuideDraft by rememberSaveable { mutableStateOf(true) }
    var aiBotAllowVisualGenerationDraft by rememberSaveable { mutableStateOf(true) }
    var aiBotAllowTextFallbackDraft by rememberSaveable { mutableStateOf(true) }
    var aiBotAllowVisualFallbackDraft by rememberSaveable { mutableStateOf(true) }
    var aiBotExposeFallbackReasonDraft by rememberSaveable { mutableStateOf(true) }
    var aiBotSafeModeEnabledDraft by rememberSaveable { mutableStateOf(true) }
    var aiBotStrictUnknownHandlingDraft by rememberSaveable { mutableStateOf(true) }
    var aiBotBlockSpeculativeFaqDraft by rememberSaveable { mutableStateOf(true) }
    var aiBotProactiveHintsEnabledDraft by rememberSaveable { mutableStateOf(true) }
    var aiBotTriggerAiLimitNearEnabledDraft by rememberSaveable { mutableStateOf(true) }
    var aiBotTriggerRestoreAvailableEnabledDraft by rememberSaveable { mutableStateOf(true) }
    var aiBotTriggerOrderShippedEnabledDraft by rememberSaveable { mutableStateOf(true) }
    var aiBotTriggerPaymentMethodsChangedEnabledDraft by rememberSaveable { mutableStateOf(true) }
    var aiBotTriggerUsageBasedUpgradeEnabledDraft by rememberSaveable { mutableStateOf(true) }
    var aiBotWarningThresholdPercentDraft by rememberSaveable { mutableStateOf("70") }
    var aiBotCriticalThresholdPercentDraft by rememberSaveable { mutableStateOf("90") }
    var aiBotUpgradeHintFreeToProTextDraft by rememberSaveable { mutableStateOf("") }
    var aiBotUpgradeHintProToCreatorTextDraft by rememberSaveable { mutableStateOf("") }
    var aiBotFaqPriorityModeDraft by rememberSaveable { mutableStateOf("live_owner_generic") }
    var aiBotPromptVersionAliasDraft by rememberSaveable { mutableStateOf("bot-max-v1") }
    var aiFaqReviewLoopLoading by rememberSaveable { mutableStateOf(false) }
    var aiFaqReviewLoopError by rememberSaveable { mutableStateOf("") }
    var aiFaqReviewLoop by remember { mutableStateOf(AiFaqOwnerReviewLoop()) }
    var aiFaqReviewActionMessage by rememberSaveable { mutableStateOf("") }
    var aiFaqReviewMetricsSnapshot by rememberSaveable { mutableStateOf("") }
    var aiAgentProviderDraft by rememberSaveable { mutableStateOf(AiRuntimeAgentProvider.Grok.rawValue) }
    var aiFallbackAgentProviderDraft by rememberSaveable { mutableStateOf(AiRuntimeAgentProvider.Gemini.rawValue) }
    var aiManusEnabledDraft by rememberSaveable { mutableStateOf(false) }
    var aiManusRequestTimeoutMsDraft by rememberSaveable { mutableStateOf("") }
    var aiManusPollIntervalMsDraft by rememberSaveable { mutableStateOf("") }
    var aiManusMaxPollAttemptsDraft by rememberSaveable { mutableStateOf("") }
    var aiManusListMessagesLimitDraft by rememberSaveable { mutableStateOf("") }
    var aiManusMaxPromptCharsDraft by rememberSaveable { mutableStateOf("") }
    var aiManusMaxHistoryTurnsDraft by rememberSaveable { mutableStateOf("") }
    var aiManusAutoStopOnWaitingDraft by rememberSaveable { mutableStateOf(true) }
    var aiManusBlockHighCreditEventsDraft by rememberSaveable { mutableStateOf(true) }
    var aiManusIncludeVerboseEventsDraft by rememberSaveable { mutableStateOf(false) }
    var aiHardTextLimitDraft by rememberSaveable { mutableStateOf("") }
    var aiHardVisualLimitDraft by rememberSaveable { mutableStateOf("") }
    var aiHardAgentLimitDraft by rememberSaveable { mutableStateOf("") }
    var aiGlobalTextLimitDraft by rememberSaveable { mutableStateOf("") }
    var aiGlobalVisualLimitDraft by rememberSaveable { mutableStateOf("") }
    var aiGlobalAgentLimitDraft by rememberSaveable { mutableStateOf("") }
    var legalBrandNameDraft by rememberSaveable { mutableStateOf("") }
    var legalOperatorNameDraft by rememberSaveable { mutableStateOf("") }
    var legalRightsHolderNameDraft by rememberSaveable { mutableStateOf("") }
    var legalSupportEmailDraft by rememberSaveable { mutableStateOf("") }
    var legalLastUpdatedLabelDraft by rememberSaveable { mutableStateOf("") }
    var legalImprintReferenceDraft by rememberSaveable { mutableStateOf("") }
    var legalMasterNumberMeaningDraft by rememberSaveable { mutableStateOf("") }
    var legalBrandManifestoDraft by rememberSaveable { mutableStateOf("") }
    var legalSymbolicNumericCodeDraft by rememberSaveable { mutableStateOf("") }
    var legalSymbolicLeetCodeDraft by rememberSaveable { mutableStateOf("") }
    var legalSymbolicCodeExplanationDraft by rememberSaveable { mutableStateOf("") }
    val managedShowcasePages = remember(artistPages) {
        val existingPages = ArtistPagesStore.pagesForBrand(com.nash.skyos.data.ArtistPageBrand.Zweizwei) +
            ArtistPagesStore.pagesForBrand(com.nash.skyos.data.ArtistPageBrand.Nicma)
        val requiredNicmaPages = listOf("NICMA MUSIC", "NICMA STUDIO")
            .map { profileName ->
                ArtistPagesStore.pageFor(
                    brand = com.nash.skyos.data.ArtistPageBrand.Nicma,
                    artistName = profileName,
                )
            }
        (existingPages + requiredNicmaPages)
            .distinctBy { it.slug }
            .sortedWith(compareBy<ArtistPageUi>({ it.brand.displayTitle }, { it.artistName.lowercase() }))
    }
    val assignedArtistPageCount = managedShowcasePages.count { it.editorUids.isNotEmpty() }
    val publishedArtistPageCount = managedShowcasePages.count { it.hasCustomPresentation }
    var profileUsernameDraft by rememberSaveable { mutableStateOf("") }
    var profileWhatsAppDraft by rememberSaveable { mutableStateOf("") }
    var profileTaglineDraft by rememberSaveable { mutableStateOf("") }
    var profileBioDraft by rememberSaveable { mutableStateOf("") }
    var profileInstagramHandleDraft by rememberSaveable { mutableStateOf("") }
    var profileAiAccessEnabledDraft by rememberSaveable { mutableStateOf(true) }
    val activeLegalDocument = rememberSaveable {
        mutableStateOf<SettingsLegalDocumentType?>(null)
    }
    val showDeleteAccountDialog = rememberSaveable {
        mutableStateOf(false)
    }
    var activeAdminWorkspaceKey by rememberSaveable(initialAdminWorkspaceKey) {
        mutableStateOf(initialAdminWorkspaceKey ?: AdminWorkspaceSection.Users.name)
    }
    val activeAdminWorkspace = remember(activeAdminWorkspaceKey) {
        AdminWorkspaceSection.fromSavedKey(activeAdminWorkspaceKey)
    }
    var showAdminWorkspaceSheet by rememberSaveable(initialAdminWorkspaceKey) {
        mutableStateOf(initialAdminWorkspaceKey != null)
    }
    var membershipOpsTab by rememberSaveable { mutableStateOf(MembershipOpsTab.Dashboard.name) }
    var membershipOpsLoading by rememberSaveable { mutableStateOf(false) }
    var membershipOpsError by rememberSaveable { mutableStateOf("") }
    var membershipDashboard by remember { mutableStateOf<Map<String, Any?>>(emptyMap()) }
    var membershipTimeseries by remember { mutableStateOf<Map<String, Any?>>(emptyMap()) }
    var membershipLearnings by remember { mutableStateOf<Map<String, Any?>>(emptyMap()) }
    var membershipRecommendations by remember { mutableStateOf<List<MembershipOpsRecommendation>>(emptyList()) }
    var membershipSimulations by remember { mutableStateOf<Map<String, Any?>>(emptyMap()) }
    var membershipTimeline by remember { mutableStateOf<Map<String, Any?>>(emptyMap()) }
    var membershipTimelineRange by rememberSaveable { mutableStateOf("30d") }
    var hygieneCooldownCompletedDraft by rememberSaveable { mutableStateOf("10") }
    var hygieneCooldownRejectedDraft by rememberSaveable { mutableStateOf("21") }
    var hygieneCooldownProposedDraft by rememberSaveable { mutableStateOf("7") }
    var hygieneSimilarityStrictnessDraft by rememberSaveable { mutableStateOf("balanced") }
    var hygieneRecurringPenaltyDraft by rememberSaveable { mutableStateOf("0.18") }
    var hygieneFreshnessFloorDraft by rememberSaveable { mutableStateOf("0.20") }
    var hygieneDuplicateMergeWindowDraft by rememberSaveable { mutableStateOf("14") }
    var hygieneProfileLabel by rememberSaveable { mutableStateOf("balanced") }
    var selectedRecommendationId by rememberSaveable { mutableStateOf("") }
    var experimentLifecycleIdDraft by rememberSaveable { mutableStateOf("") }
    var experimentNotesDraft by rememberSaveable { mutableStateOf("") }
    var experimentLearningsDraft by rememberSaveable { mutableStateOf("") }
    var experimentCvrDeltaDraft by rememberSaveable { mutableStateOf("0.00") }
    var experimentAnnualDeltaDraft by rememberSaveable { mutableStateOf("0.00") }
    var experimentCreatorDeltaDraft by rememberSaveable { mutableStateOf("0.00") }
    var experimentCancelDeltaDraft by rememberSaveable { mutableStateOf("0.00") }
    var experimentObservedDaysDraft by rememberSaveable { mutableStateOf("14") }
    var pendingHeaderImageTarget by remember { mutableStateOf<SettingsHeaderImageTarget?>(null) }
    var activeHeaderImageUploadTarget by remember { mutableStateOf<SettingsHeaderImageTarget?>(null) }
    val visiblePaymentMethodCount = listOf(
        uiState.paymentMethods.stripe.connected && uiState.paymentMethods.stripe.enabled,
        uiState.paymentMethods.paypal.connected && uiState.paymentMethods.paypal.enabled,
        uiState.paymentMethods.klarna.connected && uiState.paymentMethods.klarna.enabled,
        uiState.paymentMethods.bankTransfer.isConfigured && uiState.paymentMethods.bankTransfer.enabled,
    ).count { it }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.updateNotifications(granted && NotificationPermissionCoordinator.areNotificationsEnabled(context))
        feedbackMessage = if (granted) {
            resources.getString(R.string.settings_notifications_toast_enabled)
        } else {
            resources.getString(R.string.settings_notifications_toast_enable_in_settings)
        }
        feedbackType = if (granted) ToastType.Success else ToastType.Warning
    }

    LaunchedEffect(uiState.paymentMethods) {
        stripeAccountHintDraft = uiState.paymentMethods.stripe.accountHint
        paypalAccountHintDraft = uiState.paymentMethods.paypal.accountHint
        klarnaAccountHintDraft = uiState.paymentMethods.klarna.accountHint
        bankAccountHolderDraft = uiState.paymentMethods.bankTransfer.accountHolder
        bankIbanDraft = uiState.paymentMethods.bankTransfer.iban
        bankBicDraft = uiState.paymentMethods.bankTransfer.bic
        bankNameDraft = uiState.paymentMethods.bankTransfer.bankName
        bankInstructionsDraft = uiState.paymentMethods.bankTransfer.paymentInstructions
    }

    LaunchedEffect(uiState.commerceSettings) {
        domesticShippingDraft = formatDecimalDraft(uiState.commerceSettings.shipping.domesticCost)
        euShippingDraft = formatDecimalDraft(uiState.commerceSettings.shipping.euCost)
        internationalShippingDraft = formatDecimalDraft(uiState.commerceSettings.shipping.internationalCost)
        freeShippingThresholdDraft = formatDecimalDraft(uiState.commerceSettings.shipping.freeShippingThreshold)
        shippingNotesDraft = uiState.commerceSettings.shipping.shippingNotes
        invoiceCompanyNameDraft = uiState.commerceSettings.invoice.companyName
        invoiceCompanyAddressDraft = uiState.commerceSettings.invoice.companyAddress
        invoiceTaxNumberDraft = uiState.commerceSettings.invoice.taxNumber
        invoiceVatIdDraft = uiState.commerceSettings.invoice.vatId
        invoiceTaxRateDraft = formatDecimalDraft(uiState.commerceSettings.invoice.taxRate, decimals = 1)
        invoicePrefixDraft = uiState.commerceSettings.invoice.invoicePrefix
        invoiceSupportEmailDraft = uiState.commerceSettings.invoice.supportEmail
    }

    LaunchedEffect(uiState.shopifyAdminSettings) {
        shopifyStoreDomainDraft = uiState.shopifyAdminSettings.storeDomain
        shopifyStorefrontAccessTokenDraft = uiState.shopifyAdminSettings.storefrontAccessToken
        shopifyCollectionHandlesDraft = uiState.shopifyAdminSettings.collectionHandlesDraft
    }

    LaunchedEffect(screenHeaderSettings) {
        homeHeaderImageUrlDraft = screenHeaderSettings.homeImageUrl
        homeHeaderEyebrowDraft = screenHeaderSettings.homeEyebrow
        homeHeaderTitleDraft = screenHeaderSettings.homeTitle
        homeHeaderSubtitleDraft = screenHeaderSettings.homeSubtitle
        homeHeaderDetailDraft = screenHeaderSettings.homeDetail
        musicHubHeaderImageUrlDraft = screenHeaderSettings.musicHubImageUrl
        musicHubHeaderEyebrowDraft = screenHeaderSettings.musicHubEyebrow
        musicHubHeaderTitleDraft = screenHeaderSettings.musicHubTitle
        musicHubHeaderSubtitleDraft = screenHeaderSettings.musicHubSubtitle
        musicHubHeaderDetailDraft = screenHeaderSettings.musicHubDetail
        shopHeaderImageUrlDraft = screenHeaderSettings.shopImageUrl
        shopHeaderEyebrowDraft = screenHeaderSettings.shopEyebrow
        shopHeaderTitleDraft = screenHeaderSettings.shopTitle
        shopHeaderSubtitleDraft = screenHeaderSettings.shopSubtitle
        shopHeaderDetailDraft = screenHeaderSettings.shopDetail
        videoHeaderImageUrlDraft = screenHeaderSettings.videoHubImageUrl
        videoHeaderHeroVideoUrlDraft = screenHeaderSettings.videoHubHeroVideoUrl
        videoHeaderEyebrowDraft = screenHeaderSettings.videoHubEyebrow
        videoHeaderTitleDraft = screenHeaderSettings.videoHubTitle
        videoHeaderSubtitleDraft = screenHeaderSettings.videoHubSubtitle
        videoHeaderDetailDraft = screenHeaderSettings.videoHubDetail
    }

    LaunchedEffect(uiState.workflowAutomationSettings) {
        automationProviderDraft = uiState.workflowAutomationSettings.provider.ifBlank { "activepieces" }
        automationEnabledDraft = uiState.workflowAutomationSettings.isEnabled
        automationSendsUserContextDraft = uiState.workflowAutomationSettings.sendsUserContext
        automationWorkflowNameDraft = uiState.workflowAutomationSettings.workflowName
        automationBaseUrlDraft = uiState.workflowAutomationSettings.baseUrl
        automationWebhookPathDraft = uiState.workflowAutomationSettings.webhookPath
        automationAuthHeaderNameDraft = uiState.workflowAutomationSettings.authHeaderName
        automationAuthHeaderValueDraft = uiState.workflowAutomationSettings.authHeaderValue
        automationKnowledgeContextDraft = uiState.workflowAutomationSettings.knowledgeContext
    }

    LaunchedEffect(uiState.manusByosSettings) {
        manusByosEnabledDraft = uiState.manusByosSettings.isEnabled
    }

    LaunchedEffect(uiState.currentUserId) {
        manusByosApiKeyDraft = ""
    }

    LaunchedEffect(uiState.agentProfileSettings) {
        agentProfileEnabledDraft = uiState.agentProfileSettings.isEnabled
        agentRoleLabelDraft = uiState.agentProfileSettings.roleLabel
        agentSkillProfileDraft = uiState.agentProfileSettings.skillProfile
        agentOutputFormatDraft = uiState.agentProfileSettings.outputFormat
        agentGuardrailsDraft = uiState.agentProfileSettings.guardrails
        agentKnowledgeContextDraft = uiState.agentProfileSettings.knowledgeContext
    }

    LaunchedEffect(uiState.aiPromptSettings) {
        aiTextInstructionDraft = uiState.aiPromptSettings.textInstruction
        aiVisualInstructionDraft = uiState.aiPromptSettings.visualInstruction
        aiAgentSystemInstructionDraft = uiState.aiPromptSettings.agentSystemInstruction
        aiFaqInstructionDraft = uiState.aiPromptSettings.faqInstruction
        aiFaqKnowledgeBaseDraft = uiState.aiPromptSettings.faqKnowledgeBase
        aiAssetLibraryLinkDraft = uiState.aiPromptSettings.assetLibraryLink
        aiAssetReferenceNotesDraft = uiState.aiPromptSettings.assetReferenceNotes
    }

    LaunchedEffect(uiState.aiRuntimeSettings) {
        aiCostGuardEnabledDraft = uiState.aiRuntimeSettings.costGuardEnabled
        aiAgentProviderDraft = uiState.aiRuntimeSettings.agentProvider.rawValue
        aiFallbackAgentProviderDraft = uiState.aiRuntimeSettings.fallbackAgentProvider.rawValue
        aiManusEnabledDraft = uiState.aiRuntimeSettings.manus.isEnabled
        aiManusRequestTimeoutMsDraft = uiState.aiRuntimeSettings.manus.requestTimeoutMs.toString()
        aiManusPollIntervalMsDraft = uiState.aiRuntimeSettings.manus.pollIntervalMs.toString()
        aiManusMaxPollAttemptsDraft = uiState.aiRuntimeSettings.manus.maxPollAttempts.toString()
        aiManusListMessagesLimitDraft = uiState.aiRuntimeSettings.manus.listMessagesLimit.toString()
        aiManusMaxPromptCharsDraft = uiState.aiRuntimeSettings.manus.maxPromptChars.toString()
        aiManusMaxHistoryTurnsDraft = uiState.aiRuntimeSettings.manus.maxHistoryTurns.toString()
        aiManusAutoStopOnWaitingDraft = uiState.aiRuntimeSettings.manus.autoStopOnWaiting
        aiManusBlockHighCreditEventsDraft = uiState.aiRuntimeSettings.manus.blockHighCreditEvents
        aiManusIncludeVerboseEventsDraft = uiState.aiRuntimeSettings.manus.includeVerboseEvents
        aiHardTextLimitDraft = uiState.aiRuntimeSettings.hardDailyCaps.text.toString()
        aiHardVisualLimitDraft = uiState.aiRuntimeSettings.hardDailyCaps.visual.toString()
        aiHardAgentLimitDraft = uiState.aiRuntimeSettings.hardDailyCaps.agent.toString()
        aiGlobalTextLimitDraft = uiState.aiRuntimeSettings.globalDailyCaps.text.toString()
        aiGlobalVisualLimitDraft = uiState.aiRuntimeSettings.globalDailyCaps.visual.toString()
        aiGlobalAgentLimitDraft = uiState.aiRuntimeSettings.globalDailyCaps.agent.toString()
        aiBotPromptVersionDraft = uiState.aiRuntimeSettings.bot.promptVersion
        aiBotQualityModeDraft = uiState.aiRuntimeSettings.bot.qualityMode
        aiBotFaqModeDraft = uiState.aiRuntimeSettings.bot.faqMode
        aiBotOwnerModeDraft = uiState.aiRuntimeSettings.bot.ownerMode
        aiBotAnswerLengthDraft = uiState.aiRuntimeSettings.bot.answerLength
        aiBotPersonalityStyleDraft = uiState.aiRuntimeSettings.bot.personalityStyle
        aiBotLoggingLevelDraft = uiState.aiRuntimeSettings.bot.loggingLevel
        aiBotDiagnosticsModeDraft = uiState.aiRuntimeSettings.bot.diagnosticsMode
        aiBotKillSwitchDraft = uiState.aiRuntimeSettings.bot.killSwitchEnabled
        aiBotTextPrimaryModelDraft = uiState.aiRuntimeSettings.bot.modelPolicy.textPrimaryModel
        aiBotTextFallbackModelDraft = uiState.aiRuntimeSettings.bot.modelPolicy.textFallbackModel
        aiBotVisualPrimaryModelDraft = uiState.aiRuntimeSettings.bot.modelPolicy.visualPrimaryModel
        aiBotVisualFallbackModelDraft = uiState.aiRuntimeSettings.bot.modelPolicy.visualFallbackModel
        aiBotCostGuardEnabledDraft = uiState.aiRuntimeSettings.bot.costGuard.enabled
        aiBotPreferBriefCriticalDraft = uiState.aiRuntimeSettings.bot.costGuard.preferBriefAnswersWhenCritical
        aiBotShortAnswerMaxTokensDraft = uiState.aiRuntimeSettings.bot.costGuard.shortAnswerMaxOutputTokens.toString()
        aiBotStandardAnswerMaxTokensDraft = uiState.aiRuntimeSettings.bot.costGuard.standardAnswerMaxOutputTokens.toString()
        aiBotPreferFaqRoutingDraft = uiState.aiRuntimeSettings.bot.routingPolicy.preferFaqWhenTopicMatched
        aiBotPreferProductGuideDraft = uiState.aiRuntimeSettings.bot.routingPolicy.preferProductGuideForNewUsers
        aiBotAllowVisualGenerationDraft = uiState.aiRuntimeSettings.bot.routingPolicy.allowVisualGeneration
        aiBotAllowTextFallbackDraft = uiState.aiRuntimeSettings.bot.fallbackPolicy.allowTextFallback
        aiBotAllowVisualFallbackDraft = uiState.aiRuntimeSettings.bot.fallbackPolicy.allowVisualFallback
        aiBotExposeFallbackReasonDraft = uiState.aiRuntimeSettings.bot.fallbackPolicy.exposeFallbackReason
        aiBotSafeModeEnabledDraft = uiState.aiRuntimeSettings.bot.safetyPolicy.safeModeEnabled
        aiBotStrictUnknownHandlingDraft = uiState.aiRuntimeSettings.bot.safetyPolicy.strictUnknownHandling
        aiBotBlockSpeculativeFaqDraft = uiState.aiRuntimeSettings.bot.safetyPolicy.blockSpeculativeFaqAnswers
        aiBotProactiveHintsEnabledDraft = uiState.aiRuntimeSettings.bot.actionLayer.proactiveHintsEnabled
        aiBotTriggerAiLimitNearEnabledDraft = uiState.aiRuntimeSettings.bot.actionLayer.triggerAiLimitNearEnabled
        aiBotTriggerRestoreAvailableEnabledDraft = uiState.aiRuntimeSettings.bot.actionLayer.triggerRestoreAvailableEnabled
        aiBotTriggerOrderShippedEnabledDraft = uiState.aiRuntimeSettings.bot.actionLayer.triggerOrderShippedEnabled
        aiBotTriggerPaymentMethodsChangedEnabledDraft = uiState.aiRuntimeSettings.bot.actionLayer.triggerPaymentMethodsChangedEnabled
        aiBotTriggerUsageBasedUpgradeEnabledDraft = uiState.aiRuntimeSettings.bot.actionLayer.triggerUsageBasedUpgradeEnabled
        aiBotWarningThresholdPercentDraft = uiState.aiRuntimeSettings.bot.actionLayer.warningThresholdPercent.toString()
        aiBotCriticalThresholdPercentDraft = uiState.aiRuntimeSettings.bot.actionLayer.criticalThresholdPercent.toString()
        aiBotUpgradeHintFreeToProTextDraft = uiState.aiRuntimeSettings.bot.actionLayer.upgradeHintFreeToProText
        aiBotUpgradeHintProToCreatorTextDraft = uiState.aiRuntimeSettings.bot.actionLayer.upgradeHintProToCreatorText
        aiBotFaqPriorityModeDraft = uiState.aiRuntimeSettings.bot.actionLayer.faqPriorityMode
        aiBotPromptVersionAliasDraft = uiState.aiRuntimeSettings.bot.actionLayer.promptVersionAlias
    }

    LaunchedEffect(uiState.legalContentSettings) {
        legalBrandNameDraft = uiState.legalContentSettings.resolvedBrandName
        legalOperatorNameDraft = uiState.legalContentSettings.resolvedOperatorName
        legalRightsHolderNameDraft = uiState.legalContentSettings.resolvedRightsHolderName
        legalSupportEmailDraft = uiState.legalContentSettings.resolvedSupportEmail
        legalLastUpdatedLabelDraft = uiState.legalContentSettings.resolvedLastUpdatedLabel
        legalImprintReferenceDraft = uiState.legalContentSettings.resolvedImprintReference
        legalMasterNumberMeaningDraft = uiState.legalContentSettings.resolvedMasterNumberMeaning
        legalBrandManifestoDraft = uiState.legalContentSettings.resolvedBrandManifesto
        legalSymbolicNumericCodeDraft = uiState.legalContentSettings.resolvedSymbolicNumericCode
        legalSymbolicLeetCodeDraft = uiState.legalContentSettings.resolvedSymbolicLeetCode
        legalSymbolicCodeExplanationDraft = uiState.legalContentSettings.resolvedSymbolicCodeExplanation
    }

    LaunchedEffect(
        uiState.currentUserId,
        uiState.username,
        uiState.whatsApp,
        uiState.profileTagline,
        uiState.profileBio,
        uiState.instagramHandle,
        uiState.aiAccessEnabled,
    ) {
        profileUsernameDraft = uiState.username
        profileWhatsAppDraft = uiState.whatsApp
        profileTaglineDraft = uiState.profileTagline
        profileBioDraft = uiState.profileBio
        profileInstagramHandleDraft = uiState.instagramHandle
        profileAiAccessEnabledDraft = uiState.aiAccessEnabled
    }

    LaunchedEffect(Unit) {
        viewModel.refreshSystemLanguage()
        viewModel.updateNotifications(NotificationPermissionCoordinator.areNotificationsEnabled(context))
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshSystemLanguage()
                viewModel.updateNotifications(NotificationPermissionCoordinator.areNotificationsEnabled(context))
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(uiState.paymentFeedbackMessage) {
        val message = uiState.paymentFeedbackMessage ?: return@LaunchedEffect
        feedbackMessage = message
        feedbackType = if (uiState.isPaymentFeedbackError) ToastType.Error else ToastType.Success
        viewModel.clearPaymentFeedback()
    }

    LaunchedEffect(feedbackMessage) {
        if (!feedbackMessage.isNullOrBlank()) {
            delay(3000)
            feedbackMessage = null
        }
    }

    LaunchedEffect(activeAdminWorkspace, uiState.isOwner) {
        if (!uiState.isOwner || activeAdminWorkspace != AdminWorkspaceSection.MembershipOps) {
            return@LaunchedEffect
        }
        membershipOpsLoading = true
        membershipOpsError = ""
        runCatching {
            val dashboard = membershipOpsRepository.loadDashboard()
            val timeseries = membershipOpsRepository.loadTimeseries(windowDays = 30)
            val recommendations = membershipOpsRepository.loadRecommendations()
            val learnings = membershipOpsRepository.loadLearningInsights()
            val timeline = membershipOpsRepository.loadTimeline(range = membershipTimelineRange)
            val hygiene = membershipOpsRepository.loadHygieneControls()
            val simulations = if (recommendations.isNotEmpty()) {
                membershipOpsRepository.simulateImpact(
                    recommendationIds = recommendations.take(6).map { it.id },
                    horizonDays = 14,
                )
            } else {
                emptyMap()
            }
            MembershipOpsDataBundle(
                dashboard = dashboard,
                timeseries = timeseries,
                recommendations = recommendations,
                learnings = learnings,
                simulations = simulations,
                timeline = timeline,
                hygiene = hygiene,
            )
        }.onSuccess { bundle ->
            membershipDashboard = bundle.dashboard
            membershipTimeseries = bundle.timeseries
            membershipRecommendations = bundle.recommendations
            membershipLearnings = bundle.learnings
            membershipSimulations = bundle.simulations
            membershipTimeline = bundle.timeline
            val hygiene = bundle.hygiene["membershipHygiene"] as? Map<*, *> ?: emptyMap<Any, Any>()
            hygieneCooldownCompletedDraft = (hygiene["cooldownDaysCompleted"] as? Number)?.toInt()?.toString() ?: "10"
            hygieneCooldownRejectedDraft = (hygiene["cooldownDaysRejected"] as? Number)?.toInt()?.toString() ?: "21"
            hygieneCooldownProposedDraft = (hygiene["cooldownDaysProposed"] as? Number)?.toInt()?.toString() ?: "7"
            hygieneSimilarityStrictnessDraft = (hygiene["similarityStrictness"] as? String) ?: "balanced"
            hygieneRecurringPenaltyDraft = (hygiene["recurringPenalty"] as? Number)?.toDouble()?.toString() ?: "0.18"
            hygieneFreshnessFloorDraft = (hygiene["freshnessFloor"] as? Number)?.toDouble()?.toString() ?: "0.20"
            hygieneDuplicateMergeWindowDraft = (hygiene["duplicateMergeWindowDays"] as? Number)?.toInt()?.toString() ?: "14"
            hygieneProfileLabel = (bundle.hygiene["profile"] as? String ?: "balanced")
            selectedRecommendationId = bundle.recommendations.firstOrNull()?.id.orEmpty()
        }.onFailure { error ->
            membershipOpsError = error.localizedMessage
                ?: resources.getString(R.string.settings_membership_ops_load_failed)
        }
        membershipOpsLoading = false
    }

    LaunchedEffect(activeAdminWorkspace, uiState.isOwner) {
        if (!uiState.isOwner || activeAdminWorkspace != AdminWorkspaceSection.AiPrompts) {
            aiFaqReviewLoopLoading = false
            aiFaqReviewLoopError = ""
            aiFaqReviewLoop = AiFaqOwnerReviewLoop()
            aiFaqReviewActionMessage = ""
            aiFaqReviewMetricsSnapshot = ""
            return@LaunchedEffect
        }
        aiFaqReviewLoopLoading = true
        aiFaqReviewLoopError = ""
        runCatching {
            faqOwnerReviewRepository.loadReviewLoop(windowDays = 30)
        }.onSuccess { review ->
            aiFaqReviewLoop = review
        }.onFailure { error ->
            aiFaqReviewLoopError = error.localizedMessage
                ?: resources.getString(R.string.settings_admin_faq_review_load_failed)
        }
        aiFaqReviewLoopLoading = false
    }

    val currentHeaderImageUrl: (SettingsHeaderImageTarget) -> String = { target ->
        when (target) {
            SettingsHeaderImageTarget.Home -> homeHeaderImageUrlDraft
            SettingsHeaderImageTarget.MusicHub -> musicHubHeaderImageUrlDraft
            SettingsHeaderImageTarget.Shop -> shopHeaderImageUrlDraft
            SettingsHeaderImageTarget.VideoHub -> videoHeaderImageUrlDraft
        }
    }
    val applyHeaderImageUrl: (SettingsHeaderImageTarget, String) -> Unit = { target, imageUrl ->
        when (target) {
            SettingsHeaderImageTarget.Home -> homeHeaderImageUrlDraft = imageUrl
            SettingsHeaderImageTarget.MusicHub -> musicHubHeaderImageUrlDraft = imageUrl
            SettingsHeaderImageTarget.Shop -> shopHeaderImageUrlDraft = imageUrl
            SettingsHeaderImageTarget.VideoHub -> videoHeaderImageUrlDraft = imageUrl
        }
    }

    val headerImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        val target = pendingHeaderImageTarget ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            activeHeaderImageUploadTarget = target
            coroutineScope.launch {
                val previousImageUrl = currentHeaderImageUrl(target)
                val result = editableImageAssetRepository.uploadImageAsset(
                    uri = uri,
                    contentResolver = context.contentResolver,
                )
                if (result.isSuccess) {
                    val uploadedImage = result.getOrNull()
                    if (uploadedImage != null) {
                        applyHeaderImageUrl(target, uploadedImage.downloadUrl)
                        if (previousImageUrl.isNotBlank() && previousImageUrl != uploadedImage.downloadUrl) {
                            editableImageAssetRepository.deleteImageAsset(previousImageUrl)
                        }
                    }
                    feedbackMessage = resources.getString(R.string.settings_feedback_image_uploaded)
                    feedbackType = ToastType.Success
                } else {
                    feedbackMessage = result.exceptionOrNull()?.message
                        ?: resources.getString(R.string.settings_feedback_image_upload_failed)
                    feedbackType = ToastType.Error
                }
                activeHeaderImageUploadTarget = null
                pendingHeaderImageTarget = null
            }
        } else {
            activeHeaderImageUploadTarget = null
            pendingHeaderImageTarget = null
        }
    }

    val adminWorkspaceContent: @Composable (AdminWorkspaceSection) -> Unit = { section ->
        AdminWorkspaceSummaryCard(section = section)

        when (section) {
            AdminWorkspaceSection.Users -> {
                Text(
                    text = stringResource(R.string.settings_admin_users_intro),
                    modifier = Modifier.padding(top = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )

                LazyRow(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
                ) {
                    item {
                        SettingsBadge(
                            text = stringResource(R.string.settings_admin_users_roles_badge),
                            icon = Icons.Default.Person,
                            isActive = true,
                        )
                    }
                    item {
                        SettingsBadge(
                            text = stringResource(R.string.settings_admin_users_account_count, uiState.managedUsers.size),
                            icon = Icons.Default.Person,
                            isActive = uiState.managedUsers.isNotEmpty(),
                        )
                    }
                }

                AdminUserRoleGuideCard(
                    modifier = Modifier.padding(top = 14.dp),
                )

                uiState.managedUsersErrorMessage?.let { message ->
                    Text(
                        text = message,
                        modifier = Modifier.padding(top = 12.dp),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                if (uiState.managedUsers.isEmpty()) {
                    Text(
                        text = stringResource(R.string.settings_admin_users_empty_hint),
                        modifier = Modifier.padding(top = 12.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    )
                } else {
                    Column(
                        modifier = Modifier.padding(top = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingCompact),
                    ) {
                        uiState.managedUsers.forEach { managedUser ->
                            AdminManagedUserCard(
                                user = managedUser,
                                currentUserId = uiState.currentUserId,
                                onSave = viewModel::saveManagedUser,
                            )
                        }
                    }
                }
            }

            AdminWorkspaceSection.Artists -> {
                Text(
                    text = stringResource(R.string.settings_admin_artists_intro),
                    modifier = Modifier.padding(top = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )

                LazyRow(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
                ) {
                    item {
                        SettingsBadge(
                            text = stringResource(R.string.settings_admin_artists_pages_with_content, publishedArtistPageCount),
                            icon = Icons.Default.LibraryMusic,
                            isActive = publishedArtistPageCount > 0,
                        )
                    }
                    item {
                        SettingsBadge(
                            text = stringResource(R.string.settings_admin_artists_with_editors, assignedArtistPageCount),
                            icon = Icons.Default.Person,
                            isActive = assignedArtistPageCount > 0,
                        )
                    }
                }

                artistPagesError?.let { message ->
                    Text(
                        text = message,
                        modifier = Modifier.padding(top = 12.dp),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Column(
                    modifier = Modifier.padding(top = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingCompact),
                ) {
                    for (page in managedShowcasePages) {
                        ArtistPageAdminCard(
                            page = page,
                            users = uiState.managedUsers.filterNot { it.isPlatformOwner },
                            onSave = { updatedPage ->
                                coroutineScope.launch {
                                    val result = ArtistPagesStore.save(updatedPage)
                                    feedbackMessage = if (result.isSuccess) {
                                        resources.getString(
                                            R.string.settings_admin_artist_page_saved,
                                            updatedPage.artistName,
                                        )
                                    } else {
                                        result.exceptionOrNull()?.message
                                            ?: resources.getString(R.string.settings_admin_artist_page_save_failed)
                                    }
                                    feedbackType = if (result.isSuccess) ToastType.Success else ToastType.Error
                                }
                            },
                        )
                    }
                }
            }

            AdminWorkspaceSection.Headers -> {
                Text(
                    text = stringResource(R.string.settings_admin_headers_intro),
                    modifier = Modifier.padding(top = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )

                LazyRow(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
                ) {
                    item {
                        SettingsBadge(
                            text = stringResource(R.string.settings_admin_headers_configured_badge, screenHeaderSettings.configuredCount),
                            icon = Icons.Default.Palette,
                            isActive = screenHeaderSettings.configuredCount > 0,
                        )
                    }
                    item {
                        SettingsBadge(
                            text = stringResource(R.string.settings_admin_headers_overlay_active),
                            icon = Icons.Default.CheckCircle,
                            isActive = true,
                        )
                    }
                    item {
                        SettingsBadge(
                            text = stringResource(R.string.settings_admin_headers_crud_ready),
                            icon = Icons.Default.CheckCircle,
                            isActive = true,
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.settings_admin_headers_save_hint),
                    modifier = Modifier.padding(top = 10.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )

                EditableImageFieldCard(
                    title = stringResource(R.string.settings_card_header_home),
                    imageUrl = homeHeaderImageUrlDraft,
                    isUploading = activeHeaderImageUploadTarget == SettingsHeaderImageTarget.Home,
                    uploadStatusText = stringResource(R.string.settings_admin_headers_home_header_uploading),
                    onPickImage = {
                        pendingHeaderImageTarget = SettingsHeaderImageTarget.Home
                        headerImagePicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    onImageUrlChange = { homeHeaderImageUrlDraft = it },
                    onRemoveImage = {
                        val previousImageUrl = homeHeaderImageUrlDraft
                        homeHeaderImageUrlDraft = ""
                        coroutineScope.launch {
                            editableImageAssetRepository.deleteImageAsset(previousImageUrl)
                            feedbackMessage = resources.getString(R.string.settings_feedback_image_removed)
                            feedbackType = ToastType.Success
                        }
                    },
                    modifier = Modifier.padding(top = 16.dp),
                )

                OutlinedTextField(
                    value = homeHeaderEyebrowDraft,
                    onValueChange = { homeHeaderEyebrowDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    label = { Text(stringResource(R.string.settings_admin_headers_home_eyebrow)) },
                    placeholder = { Text(stringResource(R.string.settings_admin_headers_home_eyebrow_placeholder)) },
                    singleLine = true,
                )

                OutlinedTextField(
                    value = homeHeaderTitleDraft,
                    onValueChange = { homeHeaderTitleDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_admin_headers_home_title)) },
                    placeholder = { Text(stringResource(R.string.settings_admin_headers_home_title_placeholder)) },
                    singleLine = true,
                )

                OutlinedTextField(
                    value = homeHeaderSubtitleDraft,
                    onValueChange = { homeHeaderSubtitleDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_admin_headers_home_subtitle)) },
                    placeholder = { Text(stringResource(R.string.settings_admin_headers_home_subtitle_placeholder)) },
                    minLines = 2,
                )

                OutlinedTextField(
                    value = homeHeaderDetailDraft,
                    onValueChange = { homeHeaderDetailDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_admin_headers_home_detail)) },
                    placeholder = { Text(stringResource(R.string.settings_admin_headers_home_detail_placeholder)) },
                    minLines = 3,
                )
                EditableImageFieldCard(
                    title = stringResource(R.string.settings_card_header_music),
                    imageUrl = musicHubHeaderImageUrlDraft,
                    isUploading = activeHeaderImageUploadTarget == SettingsHeaderImageTarget.MusicHub,
                    uploadStatusText = stringResource(R.string.settings_admin_headers_music_header_uploading),
                    onPickImage = {
                        pendingHeaderImageTarget = SettingsHeaderImageTarget.MusicHub
                        headerImagePicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    onImageUrlChange = { musicHubHeaderImageUrlDraft = it },
                    onRemoveImage = {
                        val previousImageUrl = musicHubHeaderImageUrlDraft
                        musicHubHeaderImageUrlDraft = ""
                        coroutineScope.launch {
                            editableImageAssetRepository.deleteImageAsset(previousImageUrl)
                            feedbackMessage = resources.getString(R.string.settings_feedback_image_removed)
                            feedbackType = ToastType.Success
                        }
                    },
                    modifier = Modifier.padding(top = 10.dp),
                )
                OutlinedTextField(
                    value = musicHubHeaderEyebrowDraft,
                    onValueChange = { musicHubHeaderEyebrowDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_admin_headers_music_eyebrow)) },
                    placeholder = { Text(stringResource(R.string.settings_admin_headers_music_eyebrow_placeholder)) },
                )

                OutlinedTextField(
                    value = musicHubHeaderTitleDraft,
                    onValueChange = { musicHubHeaderTitleDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_admin_headers_music_title)) },
                    placeholder = { Text(stringResource(R.string.settings_admin_headers_music_title_placeholder)) },
                )

                OutlinedTextField(
                    value = musicHubHeaderSubtitleDraft,
                    onValueChange = { musicHubHeaderSubtitleDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_admin_headers_music_subtitle)) },
                    placeholder = { Text(stringResource(R.string.settings_admin_headers_music_subtitle_placeholder)) },
                    minLines = 2,
                )

                OutlinedTextField(
                    value = musicHubHeaderDetailDraft,
                    onValueChange = { musicHubHeaderDetailDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_admin_headers_music_detail)) },
                    placeholder = { Text(stringResource(R.string.settings_admin_headers_music_detail_placeholder)) },
                    minLines = 3,
                )
                EditableImageFieldCard(
                    title = stringResource(R.string.settings_card_header_shop),
                    imageUrl = shopHeaderImageUrlDraft,
                    isUploading = activeHeaderImageUploadTarget == SettingsHeaderImageTarget.Shop,
                    uploadStatusText = stringResource(R.string.settings_admin_headers_shop_header_uploading),
                    onPickImage = {
                        pendingHeaderImageTarget = SettingsHeaderImageTarget.Shop
                        headerImagePicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    onImageUrlChange = { shopHeaderImageUrlDraft = it },
                    onRemoveImage = {
                        val previousImageUrl = shopHeaderImageUrlDraft
                        shopHeaderImageUrlDraft = ""
                        coroutineScope.launch {
                            editableImageAssetRepository.deleteImageAsset(previousImageUrl)
                            feedbackMessage = resources.getString(R.string.settings_feedback_image_removed)
                            feedbackType = ToastType.Success
                        }
                    },
                    modifier = Modifier.padding(top = 10.dp),
                )
                OutlinedTextField(
                    value = shopHeaderEyebrowDraft,
                    onValueChange = { shopHeaderEyebrowDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_admin_headers_shop_eyebrow)) },
                    placeholder = { Text(stringResource(R.string.settings_admin_headers_shop_eyebrow_placeholder)) },
                )

                OutlinedTextField(
                    value = shopHeaderTitleDraft,
                    onValueChange = { shopHeaderTitleDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_admin_headers_shop_title)) },
                    placeholder = { Text(stringResource(R.string.settings_admin_headers_shop_title_placeholder)) },
                )

                OutlinedTextField(
                    value = shopHeaderSubtitleDraft,
                    onValueChange = { shopHeaderSubtitleDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_admin_headers_shop_subtitle)) },
                    placeholder = { Text(stringResource(R.string.settings_admin_headers_shop_subtitle_placeholder)) },
                    minLines = 2,
                )

                OutlinedTextField(
                    value = shopHeaderDetailDraft,
                    onValueChange = { shopHeaderDetailDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_admin_headers_shop_detail)) },
                    placeholder = { Text(stringResource(R.string.settings_admin_headers_shop_detail_placeholder)) },
                    minLines = 3,
                )
                EditableImageFieldCard(
                    title = stringResource(R.string.settings_card_header_video),
                    imageUrl = videoHeaderImageUrlDraft,
                    isUploading = activeHeaderImageUploadTarget == SettingsHeaderImageTarget.VideoHub,
                    uploadStatusText = stringResource(R.string.settings_admin_headers_video_header_uploading),
                    onPickImage = {
                        pendingHeaderImageTarget = SettingsHeaderImageTarget.VideoHub
                        headerImagePicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    onImageUrlChange = { videoHeaderImageUrlDraft = it },
                    onRemoveImage = {
                        val previousImageUrl = videoHeaderImageUrlDraft
                        videoHeaderImageUrlDraft = ""
                        coroutineScope.launch {
                            editableImageAssetRepository.deleteImageAsset(previousImageUrl)
                            feedbackMessage = resources.getString(R.string.settings_feedback_image_removed)
                            feedbackType = ToastType.Success
                        }
                    },
                    modifier = Modifier.padding(top = 10.dp),
                )
                OutlinedTextField(
                    value = videoHeaderHeroVideoUrlDraft,
                    onValueChange = { videoHeaderHeroVideoUrlDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_admin_headers_video_hero_video_url)) },
                    placeholder = { Text(stringResource(R.string.settings_admin_headers_video_hero_video_url_placeholder)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = videoHeaderEyebrowDraft,
                    onValueChange = { videoHeaderEyebrowDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_admin_headers_video_eyebrow)) },
                    placeholder = { Text(stringResource(R.string.settings_admin_headers_video_eyebrow_placeholder)) },
                )

                OutlinedTextField(
                    value = videoHeaderTitleDraft,
                    onValueChange = { videoHeaderTitleDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_admin_headers_video_title)) },
                    placeholder = { Text(stringResource(R.string.settings_admin_headers_video_title_placeholder)) },
                )

                OutlinedTextField(
                    value = videoHeaderSubtitleDraft,
                    onValueChange = { videoHeaderSubtitleDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_admin_headers_video_subtitle)) },
                    placeholder = { Text(stringResource(R.string.settings_admin_headers_video_subtitle_placeholder)) },
                    minLines = 2,
                )

                OutlinedTextField(
                    value = videoHeaderDetailDraft,
                    onValueChange = { videoHeaderDetailDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_admin_headers_video_detail)) },
                    placeholder = { Text(stringResource(R.string.settings_admin_headers_video_detail_placeholder)) },
                    minLines = 3,
                )

                Text(
                    text = stringResource(R.string.settings_admin_headers_empty_gradient),
                    modifier = Modifier.padding(top = 10.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )

                Button(
                    onClick = {
                        if (!uiState.isOwner) {
                            feedbackMessage = adminHeadersOwnerOnlyMessage
                            feedbackType = ToastType.Error
                        } else {
                            coroutineScope.launch {
                                val result = screenHeaderSettingsRepository.updateSettings(
                                    ScreenHeaderSettings(
                                        homeImageUrl = homeHeaderImageUrlDraft.trim(),
                                        homeEyebrow = homeHeaderEyebrowDraft.trim(),
                                        homeTitle = homeHeaderTitleDraft.trim(),
                                        homeSubtitle = homeHeaderSubtitleDraft.trim(),
                                        homeDetail = homeHeaderDetailDraft.trim(),
                                        musicHubImageUrl = musicHubHeaderImageUrlDraft.trim(),
                                        musicHubEyebrow = musicHubHeaderEyebrowDraft.trim(),
                                        musicHubTitle = musicHubHeaderTitleDraft.trim(),
                                        musicHubSubtitle = musicHubHeaderSubtitleDraft.trim(),
                                        musicHubDetail = musicHubHeaderDetailDraft.trim(),
                                        shopImageUrl = shopHeaderImageUrlDraft.trim(),
                                        shopEyebrow = shopHeaderEyebrowDraft.trim(),
                                        shopTitle = shopHeaderTitleDraft.trim(),
                                        shopSubtitle = shopHeaderSubtitleDraft.trim(),
                                        shopDetail = shopHeaderDetailDraft.trim(),
                                        videoHubImageUrl = videoHeaderImageUrlDraft.trim(),
                                        videoHubHeroVideoUrl = videoHeaderHeroVideoUrlDraft.trim(),
                                        videoHubEyebrow = videoHeaderEyebrowDraft.trim(),
                                        videoHubTitle = videoHeaderTitleDraft.trim(),
                                        videoHubSubtitle = videoHeaderSubtitleDraft.trim(),
                                        videoHubDetail = videoHeaderDetailDraft.trim(),
                                    ),
                                )
                                feedbackMessage = if (result.isSuccess) {
                                    resources.getString(R.string.settings_admin_headers_saved)
                                } else {
                                    result.exceptionOrNull()?.message
                                        ?: resources.getString(R.string.settings_admin_headers_save_failed)
                                }
                                feedbackType = if (result.isSuccess) ToastType.Success else ToastType.Error
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
                ) {
                    Text(stringResource(R.string.settings_save_header))
                }
            }

            AdminWorkspaceSection.Shopify -> {
                Text(
                    text = stringResource(R.string.settings_admin_shopify_intro),
                    modifier = Modifier.padding(top = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )

                Text(
                    text = stringResource(R.string.settings_admin_shopify_source_label),
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )

                OutlinedTextField(
                    value = shopifyStoreDomainDraft,
                    onValueChange = { shopifyStoreDomainDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    label = { Text(stringResource(R.string.settings_shopify_store_domain)) },
                    placeholder = { Text(stringResource(R.string.settings_shopify_store_domain_sample)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = shopifyStorefrontAccessTokenDraft,
                    onValueChange = { shopifyStorefrontAccessTokenDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_shopify_storefront_token)) },
                    placeholder = { Text(stringResource(R.string.settings_shopify_token_placeholder)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = shopifyCollectionHandlesDraft,
                    onValueChange = { shopifyCollectionHandlesDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_shopify_collection_handles)) },
                    placeholder = { Text(stringResource(R.string.settings_shopify_collection_handles_placeholder)) },
                    singleLine = false,
                )

                OutlinedButton(
                    onClick = { viewModel.refreshShopifyCollections(force = true) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
                    enabled = !uiState.isLoadingShopifyCollections,
                ) {
                    Text(
                        if (uiState.isLoadingShopifyCollections) {
                            stringResource(R.string.settings_shopify_collections_loading)
                        } else {
                            stringResource(R.string.settings_shopify_collections_load_from_shopify)
                        },
                    )
                }

                if (uiState.availableShopifyCollections.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.settings_admin_shopify_collections_available),
                        modifier = Modifier.padding(top = 12.dp),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )

                    OutlinedTextField(
                        value = shopifyCollectionSearchDraft,
                        onValueChange = { shopifyCollectionSearchDraft = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        label = { Text(stringResource(R.string.settings_shopify_search_collections)) },
                        placeholder = { Text(stringResource(R.string.settings_shopify_filter_placeholder)) },
                        singleLine = true,
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        val selectedHandles = shopifyCollectionHandlesDraft
                            .split('\n', ',')
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                            .distinct()
                        val collectionQuery = shopifyCollectionSearchDraft.trim().lowercase()
                        val filteredCollections = if (collectionQuery.isBlank()) {
                            uiState.availableShopifyCollections
                        } else {
                            uiState.availableShopifyCollections.filter { collection ->
                                collection.handle.lowercase().contains(collectionQuery) ||
                                    collection.displayTitle.lowercase().contains(collectionQuery)
                            }
                        }

                        if (selectedHandles.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.settings_admin_shopify_selected_count, selectedHandles.size),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                            )
                        }

                        filteredCollections.forEach { collection ->
                            val isSelected = selectedHandles.contains(collection.handle)
                            OutlinedButton(
                                onClick = {
                                    val updatedHandles = selectedHandles.toMutableList().apply {
                                        if (contains(collection.handle)) {
                                            remove(collection.handle)
                                        } else {
                                            add(collection.handle)
                                        }
                                    }
                                    shopifyCollectionHandlesDraft = updatedHandles.joinToString(", ")
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    },
                                ),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingHairline),
                                    ) {
                                        Text(
                                            text = collection.displayTitle,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Text(
                                            text = collection.handle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                                        )
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        collection.productCount?.let { count ->
                                            Text(
                                                text = stringResource(R.string.settings_admin_shopify_collection_count, count),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = if (isSelected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    uiState.shopifyCollectionsErrorMessage?.let { collectionsError ->
                        Text(
                            text = stringResource(
                                R.string.settings_admin_shopify_load_error,
                                collectionsError,
                            ),
                            modifier = Modifier.padding(top = 10.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.settings_admin_shopify_handles_hint),
                    modifier = Modifier.padding(top = 10.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )

                Text(
                    text = if (uiState.shopifyAdminSettings.hasCollectionFilter) {
                        stringResource(
                            R.string.settings_admin_shopify_active_collection,
                            uiState.shopifyAdminSettings.activeCollectionLabel,
                        )
                    } else {
                        stringResource(R.string.settings_admin_shopify_active_full_store)
                    },
                    modifier = Modifier.padding(top = 12.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )

                Button(
                    onClick = {
                        viewModel.saveShopifyAdminSettings(
                            uiState.shopifyAdminSettings.copy(
                                storeDomain = shopifyStoreDomainDraft.trim(),
                                storefrontAccessToken = shopifyStorefrontAccessTokenDraft.trim(),
                                collectionHandles = shopifyCollectionHandlesDraft
                                    .split('\n', ',')
                                    .map { it.trim() }
                                    .filter { it.isNotBlank() },
                            ),
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
                ) {
                    Text(stringResource(R.string.settings_save_shopify))
                }
            }

            AdminWorkspaceSection.Payments -> {
                Text(
                    text = stringResource(R.string.settings_admin_payments_intro),
                    modifier = Modifier.padding(top = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )

                PaymentProviderAdminCard(
                    title = stringResource(R.string.settings_payment_method_stripe),
                    providerKind = PaymentProviderKind.Stripe,
                    connected = uiState.paymentMethods.stripe.connected,
                    enabledInCheckout = uiState.paymentMethods.stripe.connected &&
                        uiState.paymentMethods.stripe.enabled,
                    accountHint = stripeAccountHintDraft,
                    accountHintLabel = stringResource(R.string.settings_payments_stripe_account_hint_label),
                    accountHintPlaceholder = stringResource(R.string.settings_payments_stripe_account_hint_placeholder),
                    onAccountHintChange = { stripeAccountHintDraft = it },
                    onSaveConnection = { viewModel.connectStripe(stripeAccountHintDraft) },
                    onDisconnect = if (uiState.paymentMethods.stripe.connected) {
                        { viewModel.disconnectStripe() }
                    } else {
                        null
                    },
                    onToggleEnabled = viewModel::setStripeEnabled,
                    modifier = Modifier.padding(top = 16.dp),
                )

                StripeBackendSecretsAdminCard(
                    status = uiState.stripeBackendSecretsStatus,
                    stripeSecretKey = stripeSecretKeyDraft,
                    stripeWebhookSecret = stripeWebhookSecretDraft,
                    onStripeSecretKeyChange = { stripeSecretKeyDraft = it },
                    onStripeWebhookSecretChange = { stripeWebhookSecretDraft = it },
                    onSave = {
                        viewModel.saveStripeBackendSecrets(
                            stripeSecretKey = stripeSecretKeyDraft,
                            stripeWebhookSecret = stripeWebhookSecretDraft,
                        )
                    },
                    modifier = Modifier.padding(top = 14.dp),
                )

                PaymentProviderAdminCard(
                    title = stringResource(R.string.settings_payment_method_paypal),
                    providerKind = PaymentProviderKind.PayPal,
                    connected = uiState.paymentMethods.paypal.connected,
                    enabledInCheckout = uiState.paymentMethods.paypal.connected &&
                        uiState.paymentMethods.paypal.enabled,
                    accountHint = paypalAccountHintDraft,
                    accountHintLabel = stringResource(R.string.settings_payments_paypal_account_hint_label),
                    accountHintPlaceholder = stringResource(R.string.settings_payments_paypal_account_hint_placeholder),
                    onAccountHintChange = { paypalAccountHintDraft = it },
                    onSaveConnection = { viewModel.connectPayPal(paypalAccountHintDraft) },
                    onDisconnect = if (uiState.paymentMethods.paypal.connected) {
                        { viewModel.disconnectPayPal() }
                    } else {
                        null
                    },
                    onToggleEnabled = viewModel::setPayPalEnabled,
                    modifier = Modifier.padding(top = 14.dp),
                )

                PaymentProviderAdminCard(
                    title = stringResource(R.string.settings_payment_method_klarna),
                    providerKind = PaymentProviderKind.Klarna,
                    connected = uiState.paymentMethods.klarna.connected,
                    enabledInCheckout = uiState.paymentMethods.klarna.connected &&
                        uiState.paymentMethods.klarna.enabled,
                    accountHint = klarnaAccountHintDraft,
                    accountHintLabel = stringResource(R.string.settings_payments_klarna_account_hint_label),
                    accountHintPlaceholder = stringResource(R.string.settings_payments_klarna_account_hint_placeholder),
                    onAccountHintChange = { klarnaAccountHintDraft = it },
                    onSaveConnection = { viewModel.connectKlarna(klarnaAccountHintDraft) },
                    onDisconnect = if (uiState.paymentMethods.klarna.connected) {
                        { viewModel.disconnectKlarna() }
                    } else {
                        null
                    },
                    onToggleEnabled = viewModel::setKlarnaEnabled,
                    modifier = Modifier.padding(top = 14.dp),
                )

                BankTransferAdminCard(
                    configured = uiState.paymentMethods.bankTransfer.isConfigured,
                    enabledInCheckout = uiState.paymentMethods.bankTransfer.enabled &&
                        uiState.paymentMethods.bankTransfer.isConfigured,
                    accountHolder = bankAccountHolderDraft,
                    iban = bankIbanDraft,
                    bic = bankBicDraft,
                    bankName = bankNameDraft,
                    paymentInstructions = bankInstructionsDraft,
                    onAccountHolderChange = { bankAccountHolderDraft = it },
                    onIbanChange = { bankIbanDraft = it },
                    onBicChange = { bankBicDraft = it },
                    onBankNameChange = { bankNameDraft = it },
                    onPaymentInstructionsChange = { bankInstructionsDraft = it },
                    onSave = {
                        viewModel.saveBankTransfer(
                            accountHolder = bankAccountHolderDraft,
                            iban = bankIbanDraft,
                            bic = bankBicDraft,
                            bankName = bankNameDraft,
                            paymentInstructions = bankInstructionsDraft,
                        )
                    },
                    onToggleEnabled = viewModel::setBankTransferEnabled,
                    modifier = Modifier.padding(top = 14.dp),
                )
            }

            AdminWorkspaceSection.Commerce -> {
                Text(
                    text = stringResource(R.string.settings_admin_commerce_intro),
                    modifier = Modifier.padding(top = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )

                Text(
                    text = stringResource(R.string.settings_admin_commerce_shipping_heading),
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )

                OutlinedTextField(
                    value = domesticShippingDraft,
                    onValueChange = { domesticShippingDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    label = { Text(stringResource(R.string.settings_shipping_de)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = euShippingDraft,
                    onValueChange = { euShippingDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_shipping_eu)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = internationalShippingDraft,
                    onValueChange = { internationalShippingDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_shipping_intl)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = freeShippingThresholdDraft,
                    onValueChange = { freeShippingThresholdDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_shipping_free_from)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = shippingNotesDraft,
                    onValueChange = { shippingNotesDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_shipping_note)) },
                    minLines = 2,
                    maxLines = 3,
                )

                Text(
                    text = stringResource(R.string.settings_admin_commerce_invoice_heading),
                    modifier = Modifier.padding(top = 18.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )

                OutlinedTextField(
                    value = invoiceCompanyNameDraft,
                    onValueChange = { invoiceCompanyNameDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    label = { Text(stringResource(R.string.settings_invoice_company_name)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = invoiceCompanyAddressDraft,
                    onValueChange = { invoiceCompanyAddressDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_invoice_company_address)) },
                    minLines = 2,
                    maxLines = 3,
                )
                OutlinedTextField(
                    value = invoiceTaxNumberDraft,
                    onValueChange = { invoiceTaxNumberDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_invoice_tax_number)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = invoiceVatIdDraft,
                    onValueChange = { invoiceVatIdDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_invoice_vat_id)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = invoiceTaxRateDraft,
                    onValueChange = { invoiceTaxRateDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_invoice_vat_rate)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = invoicePrefixDraft,
                    onValueChange = { invoicePrefixDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_invoice_prefix)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = invoiceSupportEmailDraft,
                    onValueChange = { invoiceSupportEmailDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_invoice_support_email)) },
                    singleLine = true,
                )

                Button(
                    onClick = {
                        val domesticCost = domesticShippingDraft.parseDecimalInput()
                            ?: uiState.commerceSettings.shipping.domesticCost
                        val euCost = euShippingDraft.parseDecimalInput()
                            ?: uiState.commerceSettings.shipping.euCost
                        val internationalCost = internationalShippingDraft.parseDecimalInput()
                            ?: uiState.commerceSettings.shipping.internationalCost
                        val freeShippingThreshold = freeShippingThresholdDraft.parseDecimalInput()
                            ?: uiState.commerceSettings.shipping.freeShippingThreshold
                        val taxRate = invoiceTaxRateDraft.parseDecimalInput()
                            ?: uiState.commerceSettings.invoice.taxRate

                        viewModel.saveCommerceSettings(
                            uiState.commerceSettings.copy(
                                shipping = uiState.commerceSettings.shipping.copy(
                                    domesticCost = domesticCost.coerceAtLeast(0.0),
                                    euCost = euCost.coerceAtLeast(0.0),
                                    internationalCost = internationalCost.coerceAtLeast(0.0),
                                    freeShippingThreshold = freeShippingThreshold.coerceAtLeast(0.0),
                                    shippingNotes = shippingNotesDraft.trim(),
                                ),
                                invoice = uiState.commerceSettings.invoice.copy(
                                    companyName = invoiceCompanyNameDraft.trim(),
                                    companyAddress = invoiceCompanyAddressDraft.trim(),
                                    taxNumber = invoiceTaxNumberDraft.trim(),
                                    vatId = invoiceVatIdDraft.trim(),
                                    taxRate = taxRate.coerceAtLeast(0.0),
                                    invoicePrefix = invoicePrefixDraft.trim(),
                                    supportEmail = invoiceSupportEmailDraft.trim(),
                                ),
                            ),
                            successMessage = resources.getString(R.string.settings_admin_commerce_save_success),
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
                ) {
                    Text(stringResource(R.string.settings_save_shipping_invoice))
                }
            }

            AdminWorkspaceSection.Visuals -> {
                Text(
                    text = stringResource(R.string.settings_admin_visuals_title),
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                SettingsToggleRow(
                    title = stringResource(R.string.settings_toggle_ref_library_title),
                    body = stringResource(R.string.settings_toggle_ref_library_body),
                    checked = uiState.aiVisualReferenceLibrary.isEnabled,
                    onCheckedChange = viewModel::updateAiVisualReferenceEnabled,
                    modifier = Modifier.padding(top = 10.dp),
                )
                OutlinedTextField(
                    value = uiState.aiVisualReferenceLibrary.storageLink,
                    onValueChange = viewModel::updateAiVisualStorageLink,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    label = { Text(stringResource(R.string.settings_ref_drive_or_asset)) },
                    placeholder = { Text(stringResource(R.string.settings_ref_drive_link_placeholder)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = uiState.aiVisualReferenceLibrary.namingPrefix,
                    onValueChange = viewModel::updateAiVisualNamingPrefix,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_ref_naming_prefix)) },
                    placeholder = { Text(stringResource(R.string.settings_ref_naming_prefix_placeholder)) },
                    singleLine = true,
                )
                uiState.aiVisualReferenceLibrary.referenceHints.forEachIndexed { index, referenceHint ->
                    OutlinedTextField(
                        value = referenceHint,
                        onValueChange = { value ->
                            viewModel.updateAiVisualReferenceHint(index, value)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        label = { Text(stringResource(R.string.settings_ref_label, index + 1)) },
                        placeholder = {
                            Text(stringResource(R.string.settings_ref_description_placeholder))
                        },
                        minLines = 2,
                        maxLines = 3,
                    )
                }
            }

            AdminWorkspaceSection.Automation -> {
                Text(
                    text = if (uiState.isOwner) {
                        stringResource(R.string.settings_admin_automation_title_owner)
                    } else {
                        stringResource(R.string.settings_admin_automation_title_user)
                    },
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                SettingsToggleRow(
                    title = if (uiState.isOwner) {
                        stringResource(R.string.settings_admin_automation_toggle_owner_title)
                    } else {
                        stringResource(R.string.settings_admin_automation_toggle_user_title)
                    },
                    body = if (uiState.isOwner) {
                        stringResource(R.string.settings_admin_automation_toggle_owner_body)
                    } else {
                        stringResource(R.string.settings_admin_automation_toggle_user_body)
                    },
                    checked = automationEnabledDraft,
                    onCheckedChange = { automationEnabledDraft = it },
                    modifier = Modifier.padding(top = 10.dp),
                )
                SettingsToggleRow(
                    title = stringResource(R.string.settings_toggle_app_user_context_title),
                    body = stringResource(R.string.settings_toggle_app_user_context_body),
                    checked = automationSendsUserContextDraft,
                    onCheckedChange = { automationSendsUserContextDraft = it },
                    modifier = Modifier.padding(top = 10.dp),
                )
                if (!uiState.isOwner) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
                    ) {
                        OutlinedButton(
                            onClick = { automationProviderDraft = "activepieces" },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (automationProviderDraft == "activepieces") {
                                    MaterialTheme.colorScheme.tertiary
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.32f)
                                },
                            ),
                        ) {
                            Text(stringResource(R.string.settings_automation_activepieces))
                        }
                        OutlinedButton(
                            onClick = { automationProviderDraft = "n8n" },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (automationProviderDraft == "n8n") {
                                    MaterialTheme.colorScheme.tertiary
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.32f)
                                },
                            ),
                        ) {
                            Text(stringResource(R.string.settings_automation_n8n))
                        }
                    }
                }
                OutlinedTextField(
                    value = automationWorkflowNameDraft,
                    onValueChange = { automationWorkflowNameDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    label = { Text(stringResource(R.string.settings_automation_workflow_name)) },
                    placeholder = { Text(stringResource(R.string.settings_automation_workflow_name_placeholder)) },
                    singleLine = true,
                    shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
                )
                OutlinedTextField(
                    value = automationBaseUrlDraft,
                    onValueChange = { automationBaseUrlDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_automation_activepieces_base_url)) },
                    placeholder = { Text(stringResource(R.string.settings_automation_activepieces_base_url_placeholder)) },
                    singleLine = true,
                    shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
                )
                OutlinedTextField(
                    value = automationWebhookPathDraft,
                    onValueChange = { automationWebhookPathDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_automation_webhook_path)) },
                    placeholder = { Text(stringResource(R.string.settings_automation_webhook_path_placeholder)) },
                    singleLine = true,
                    shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
                )
                OutlinedTextField(
                    value = automationAuthHeaderNameDraft,
                    onValueChange = { automationAuthHeaderNameDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_automation_auth_header_name)) },
                    placeholder = { Text(stringResource(R.string.settings_automation_auth_header_name_placeholder)) },
                    singleLine = true,
                    shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
                )
                OutlinedTextField(
                    value = automationAuthHeaderValueDraft,
                    onValueChange = { automationAuthHeaderValueDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_automation_auth_header_value)) },
                    placeholder = { Text(stringResource(R.string.settings_automation_optional)) },
                    singleLine = true,
                    shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
                )
                OutlinedTextField(
                    value = automationKnowledgeContextDraft,
                    onValueChange = { automationKnowledgeContextDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_automation_knowledge_context)) },
                    placeholder = {
                        Text(stringResource(R.string.settings_automation_knowledge_context_placeholder))
                    },
                    minLines = 3,
                    shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
                )

                val resolvedWebhookUrl = resolveAutomationDraftWebhookUrl(
                    baseUrl = automationBaseUrlDraft,
                    webhookPath = automationWebhookPathDraft,
                )

                Text(
                    text = if (resolvedWebhookUrl != null) {
                        stringResource(
                            R.string.settings_automation_owner_webhook_url,
                            resolvedWebhookUrl,
                        )
                    } else {
                        stringResource(R.string.settings_automation_owner_webhook_incomplete)
                    },
                    modifier = Modifier.padding(top = 12.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill),
                ) {
                    val updatedAutomationSettings = uiState.workflowAutomationSettings.copy(
                        provider = if (!uiState.isOwner && automationProviderDraft == "n8n") "n8n" else "activepieces",
                        scope = if (uiState.isOwner) "owner_global" else "user_personal",
                        isEnabled = automationEnabledDraft,
                        sendsUserContext = automationSendsUserContextDraft,
                        workflowName = automationWorkflowNameDraft.trim(),
                        baseUrl = automationBaseUrlDraft.trim(),
                        webhookPath = automationWebhookPathDraft.trim(),
                        authHeaderName = automationAuthHeaderNameDraft.trim(),
                        authHeaderValue = automationAuthHeaderValueDraft.trim(),
                        knowledgeContext = automationKnowledgeContextDraft.trim(),
                    )

                    Button(
                        onClick = {
                            viewModel.saveWorkflowAutomationSettings(updatedAutomationSettings)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
                    ) {
                        Text(stringResource(R.string.settings_automation_save_flow))
                    }

                    OutlinedButton(
                        onClick = { viewModel.testWorkflowAutomationSettings(updatedAutomationSettings) },
                        modifier = Modifier.weight(1f),
                        enabled = automationEnabledDraft && resolvedWebhookUrl != null,
                        shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
                    ) {
                        Text(stringResource(R.string.settings_automation_send_test))
                    }
                }

                Text(
                    text = stringResource(R.string.settings_admin_manus_optional_title),
                    modifier = Modifier.padding(top = 18.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )

                SettingsToggleRow(
                    title = stringResource(R.string.settings_toggle_manus_own_title),
                    body = stringResource(R.string.settings_toggle_manus_own_body),
                    checked = manusByosEnabledDraft,
                    onCheckedChange = { manusByosEnabledDraft = it },
                    modifier = Modifier.padding(top = 10.dp),
                )

                Row(
                    modifier = Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
                ) {
                    SettingsBadge(
                        text = if (uiState.manusByosSettings.hasApiKey) {
                            stringResource(R.string.settings_manus_key_stored_locally)
                        } else {
                            stringResource(R.string.settings_manus_key_missing)
                        },
                        icon = Icons.Default.CheckCircle,
                        isActive = uiState.manusByosSettings.hasApiKey,
                    )
                    SettingsBadge(
                        text = if (uiState.manusByosSettings.isEnabled && uiState.manusByosSettings.hasApiKey) {
                            stringResource(R.string.settings_manus_byos_active)
                        } else {
                            stringResource(R.string.settings_manus_byos_inactive)
                        },
                        icon = Icons.Default.Bolt,
                        isActive = uiState.manusByosSettings.isEnabled && uiState.manusByosSettings.hasApiKey,
                    )
                }

                OutlinedTextField(
                    value = manusByosApiKeyDraft,
                    onValueChange = { manusByosApiKeyDraft = it.take(1024) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_manus_api_key)) },
                    placeholder = { Text(stringResource(R.string.settings_manus_api_key_placeholder)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )

                Text(
                    text = stringResource(R.string.settings_admin_manus_key_hint),
                    modifier = Modifier.padding(top = 10.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )

                SettingsBadge(
                    text = stringResource(
                        R.string.settings_admin_manus_validate_status,
                        resolveManusValidationLabel(uiState.manusValidationStatus),
                    ),
                    icon = Icons.Default.Security,
                    isActive = uiState.manusValidationStatus == "key_valid",
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    text = uiState.manusValidationMessage,
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodySmall,
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill),
                ) {
                    Button(
                        onClick = {
                            viewModel.saveManusByosSettings(
                                enabled = manusByosEnabledDraft,
                                apiKeyDraft = manusByosApiKeyDraft,
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
                    ) {
                        Text(stringResource(R.string.settings_manus_save))
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.clearManusByosApiKey()
                            manusByosApiKeyDraft = ""
                        },
                        modifier = Modifier.weight(1f),
                        enabled = uiState.manusByosSettings.hasApiKey || manusByosApiKeyDraft.isNotBlank(),
                        shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
                    ) {
                        Text(stringResource(R.string.settings_manus_remove_key))
                    }
                }

                OutlinedButton(
                    onClick = { viewModel.validateManusByosKey(manusByosApiKeyDraft) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
                    enabled = manusByosApiKeyDraft.isNotBlank() || uiState.manusByosSettings.hasApiKey,
                ) {
                    Text(stringResource(R.string.settings_manus_validate))
                }

                Text(
                    text = stringResource(R.string.settings_admin_agent_profile_title),
                    modifier = Modifier.padding(top = 18.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )

                SettingsToggleRow(
                    title = stringResource(R.string.settings_toggle_agent_profile_title),
                    body = stringResource(R.string.settings_toggle_agent_profile_body),
                    checked = agentProfileEnabledDraft,
                    onCheckedChange = { agentProfileEnabledDraft = it },
                    modifier = Modifier.padding(top = 10.dp),
                )

                OutlinedTextField(
                    value = agentRoleLabelDraft,
                    onValueChange = { agentRoleLabelDraft = it.take(240) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_agent_role_focus)) },
                    placeholder = { Text(stringResource(R.string.settings_agent_role_focus_placeholder)) },
                    minLines = 2,
                    maxLines = 4,
                )
                OutlinedTextField(
                    value = agentSkillProfileDraft,
                    onValueChange = { agentSkillProfileDraft = it.take(12000) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_agent_skills)) },
                    placeholder = {
                        Text(stringResource(R.string.settings_agent_skills_placeholder))
                    },
                    minLines = 4,
                    maxLines = 12,
                )
                OutlinedTextField(
                    value = agentOutputFormatDraft,
                    onValueChange = { agentOutputFormatDraft = it.take(4000) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_agent_deliverable_format)) },
                    placeholder = { Text(stringResource(R.string.settings_agent_deliverable_format_placeholder)) },
                    minLines = 3,
                    maxLines = 8,
                )
                OutlinedTextField(
                    value = agentGuardrailsDraft,
                    onValueChange = { agentGuardrailsDraft = it.take(4000) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_agent_guardrails)) },
                    placeholder = { Text(stringResource(R.string.settings_agent_guardrails_placeholder)) },
                    minLines = 3,
                    maxLines = 8,
                )
                OutlinedTextField(
                    value = agentKnowledgeContextDraft,
                    onValueChange = { agentKnowledgeContextDraft = it.take(4000) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_agent_knowledge)) },
                    placeholder = { Text(stringResource(R.string.settings_agent_knowledge_placeholder)) },
                    minLines = 3,
                    maxLines = 8,
                )

                Button(
                    onClick = {
                        viewModel.saveAgentProfileSettings(
                            uiState.agentProfileSettings.copy(
                                isEnabled = agentProfileEnabledDraft,
                                roleLabel = agentRoleLabelDraft,
                                skillProfile = agentSkillProfileDraft,
                                outputFormat = agentOutputFormatDraft,
                                guardrails = agentGuardrailsDraft,
                                knowledgeContext = agentKnowledgeContextDraft,
                            ),
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
                ) {
                    Text(stringResource(R.string.settings_agent_save_profile))
                }
            }

            AdminWorkspaceSection.AiPrompts -> {
                Text(
                    text = stringResource(R.string.settings_admin_ai_prompts_intro),
                    modifier = Modifier.padding(top = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )

                LazyRow(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
                ) {
                    item {
                        SettingsBadge(
                            text = stringResource(R.string.settings_admin_ai_char_count_text, aiTextInstructionDraft.trim().length),
                            icon = Icons.Default.Settings,
                            isActive = aiTextInstructionDraft.isNotBlank(),
                        )
                    }
                    item {
                        SettingsBadge(
                            text = stringResource(R.string.settings_admin_ai_char_count_visual, aiVisualInstructionDraft.trim().length),
                            icon = Icons.Default.Palette,
                            isActive = aiVisualInstructionDraft.isNotBlank(),
                        )
                    }
                    item {
                        SettingsBadge(
                            text = stringResource(R.string.settings_admin_ai_char_count_agent, aiAgentSystemInstructionDraft.trim().length),
                            icon = Icons.Default.Bolt,
                            isActive = aiAgentSystemInstructionDraft.isNotBlank(),
                        )
                    }
                    item {
                        SettingsBadge(
                            text = stringResource(R.string.settings_admin_ai_char_count_faq, aiFaqInstructionDraft.trim().length),
                            icon = Icons.Default.Email,
                            isActive = aiFaqInstructionDraft.isNotBlank(),
                        )
                    }
                    item {
                        SettingsBadge(
                            text = if (aiAssetLibraryLinkDraft.isBlank()) {
                                stringResource(R.string.settings_ai_asset_library_badge_off)
                            } else {
                                stringResource(R.string.settings_ai_asset_library_badge_on)
                            },
                            icon = Icons.Default.Link,
                            isActive = aiAssetLibraryLinkDraft.isNotBlank(),
                        )
                    }
                }

                OutlinedTextField(
                    value = aiTextInstructionDraft,
                    onValueChange = { aiTextInstructionDraft = it.take(12000) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    label = { Text(stringResource(R.string.settings_ai_bot_text_instruction)) },
                    minLines = 6,
                    maxLines = 14,
                )

                OutlinedTextField(
                    value = aiVisualInstructionDraft,
                    onValueChange = { aiVisualInstructionDraft = it.take(12000) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_ai_visual_instruction)) },
                    minLines = 6,
                    maxLines = 14,
                )

                OutlinedTextField(
                    value = aiAgentSystemInstructionDraft,
                    onValueChange = { aiAgentSystemInstructionDraft = it.take(12000) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_ai_agent_system_instruction)) },
                    minLines = 6,
                    maxLines = 16,
                )

                OutlinedTextField(
                    value = aiFaqInstructionDraft,
                    onValueChange = { aiFaqInstructionDraft = it.take(12000) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_ai_faq_system_instruction)) },
                    minLines = 5,
                    maxLines = 12,
                )

                OutlinedTextField(
                    value = aiFaqKnowledgeBaseDraft,
                    onValueChange = { aiFaqKnowledgeBaseDraft = it.take(12000) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_ai_faq_owner_knowledge)) },
                    minLines = 6,
                    maxLines = 16,
                )

                OutlinedTextField(
                    value = aiAssetLibraryLinkDraft,
                    onValueChange = { aiAssetLibraryLinkDraft = it.take(2000) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_ai_asset_library)) },
                    placeholder = { Text(stringResource(R.string.settings_ai_asset_library_placeholder)) },
                    singleLine = true,
                )

                OutlinedTextField(
                    value = aiAssetReferenceNotesDraft,
                    onValueChange = { aiAssetReferenceNotesDraft = it.take(12000) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_ai_asset_notes)) },
                    minLines = 4,
                    maxLines = 10,
                )

                Text(
                    text = stringResource(R.string.settings_admin_ai_empty_restores_default),
                    modifier = Modifier.padding(top = 10.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodySmall,
                )

                Button(
                    onClick = {
                        viewModel.saveAiPromptSettings(
                            uiState.aiPromptSettings.copy(
                                textInstruction = aiTextInstructionDraft,
                                visualInstruction = aiVisualInstructionDraft,
                                agentSystemInstruction = aiAgentSystemInstructionDraft,
                                faqInstruction = aiFaqInstructionDraft,
                                faqKnowledgeBase = aiFaqKnowledgeBaseDraft,
                                assetLibraryLink = aiAssetLibraryLinkDraft,
                                assetReferenceNotes = aiAssetReferenceNotesDraft,
                            ),
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
                ) {
                    Text(stringResource(R.string.settings_ai_save_instructions))
                }

                Text(
                    text = stringResource(R.string.settings_admin_ai_runtime_heading),
                    modifier = Modifier.padding(top = 18.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )

                Text(
                    text = stringResource(R.string.settings_admin_ai_runtime_intro),
                    modifier = Modifier.padding(top = 4.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodySmall,
                )

                Text(
                    text = stringResource(R.string.settings_admin_ai_faq_review_title),
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                if (aiFaqReviewActionMessage.isNotBlank()) {
                    Text(
                        text = aiFaqReviewActionMessage,
                        modifier = Modifier.padding(top = 6.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (aiFaqReviewMetricsSnapshot.isNotBlank()) {
                    Text(
                        text = aiFaqReviewMetricsSnapshot,
                        modifier = Modifier.padding(top = 4.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }

                if (aiFaqReviewLoopLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .size(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else if (aiFaqReviewLoopError.isNotBlank()) {
                    Text(
                        text = aiFaqReviewLoopError,
                        modifier = Modifier.padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    LazyRow(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
                    ) {
                        item {
                            SettingsBadge(
                                text = stringResource(R.string.settings_faq_review_strong_count, aiFaqReviewLoop.strongestTriggers.size),
                                icon = Icons.Default.CheckCircle,
                                isActive = aiFaqReviewLoop.strongestTriggers.isNotEmpty(),
                            )
                        }
                        item {
                            SettingsBadge(
                                text = stringResource(R.string.settings_faq_review_weak_count, aiFaqReviewLoop.weakTriggers.size),
                                icon = Icons.Default.Settings,
                                isActive = aiFaqReviewLoop.weakTriggers.isNotEmpty(),
                            )
                        }
                        item {
                            SettingsBadge(
                                text = stringResource(R.string.settings_faq_review_useless_count, aiFaqReviewLoop.likelyUselessTriggers.size),
                                icon = Icons.Default.Lock,
                                isActive = aiFaqReviewLoop.likelyUselessTriggers.isNotEmpty(),
                            )
                        }
                        item {
                            SettingsBadge(
                                text = stringResource(R.string.settings_faq_review_repeat_count, aiFaqReviewLoop.repeatHeavyTopics.size),
                                icon = Icons.Default.Email,
                                isActive = aiFaqReviewLoop.repeatHeavyTopics.isNotEmpty(),
                            )
                        }
                    }

                    aiFaqReviewLoop.strongestTriggers.firstOrNull()?.let { entry ->
                        Text(
                            text = stringResource(
                                R.string.settings_faq_review_strongest_line,
                                entry.triggerKey,
                                (entry.conversionRate * 100).toInt(),
                                (entry.repeatRate * 100).toInt(),
                            ),
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    aiFaqReviewLoop.weakTriggers.firstOrNull()?.let { entry ->
                        Text(
                            text = stringResource(
                                R.string.settings_faq_review_weak_line,
                                entry.triggerKey,
                                (entry.conversionRate * 100).toInt(),
                                (entry.repeatRate * 100).toInt(),
                            ),
                            modifier = Modifier.padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    aiFaqReviewLoop.repeatHeavyTopics.firstOrNull()?.let { topic ->
                        Text(
                            text = stringResource(
                                R.string.settings_faq_review_repeat_topic,
                                topic.key,
                                topic.value,
                                (topic.share * 100).toInt(),
                            ),
                            modifier = Modifier.padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }

                    aiFaqReviewLoop.strategyInsights.take(3).forEach { insight ->
                        SkydownCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingNano)) {
                                Text(
                                    text = insight.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = insight.summary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                                )
                                Text(
                                    text = stringResource(R.string.settings_faq_review_expected_impact, insight.expectedImpact),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                                )
                            }
                        }
                    }

                    aiFaqReviewLoop.recommendations.take(3).forEach { recommendation ->
                        SkydownCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingNano)) {
                                Text(
                                    text = recommendation.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = recommendation.summary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                                )
                                Text(
                                    text = stringResource(
                                        R.string.settings_faq_review_action_line,
                                        recommendation.actionType,
                                        recommendation.targetField,
                                        recommendation.suggestedValueLabel,
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                val preview = faqOwnerReviewRepository.previewRecommendation(recommendation, 30)
                                                aiFaqReviewActionMessage = preview.message
                                                aiFaqReviewMetricsSnapshot = preview.metricsSnapshot
                                                feedbackMessage = preview.message
                                                feedbackType = if (preview.status == "blocked") ToastType.Warning else ToastType.Info
                                            }
                                        },
                                        shape = RoundedCornerShape(SkydownUiTokens.tightRadius),
                                    ) {
                                        Text(stringResource(R.string.settings_ai_preview))
                                    }
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                val applyResult = faqOwnerReviewRepository.applyRecommendation(recommendation.id, 30)
                                                aiFaqReviewActionMessage = applyResult.message
                                                aiFaqReviewMetricsSnapshot = applyResult.metricsSnapshot
                                                feedbackMessage = applyResult.message
                                                feedbackType = if (applyResult.status == "applied") ToastType.Success else ToastType.Warning
                                                if (applyResult.status == "applied") {
                                                    aiFaqReviewLoop = faqOwnerReviewRepository.loadReviewLoop(windowDays = 30)
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(SkydownUiTokens.tightRadius),
                                    ) {
                                        Text(stringResource(R.string.settings_ai_apply))
                                    }
                                }
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                val revert = faqOwnerReviewRepository.revertLastChange()
                                aiFaqReviewActionMessage = revert.message
                                feedbackMessage = revert.message
                                feedbackType = if (revert.status == "reverted") ToastType.Success else ToastType.Info
                                if (revert.status == "reverted") {
                                    aiFaqReviewLoop = faqOwnerReviewRepository.loadReviewLoop(windowDays = 30)
                                }
                            }
                        },
                        modifier = Modifier.padding(top = 8.dp),
                        shape = RoundedCornerShape(SkydownUiTokens.tightRadius),
                    ) {
                        Text(stringResource(R.string.settings_ai_revert_last))
                    }
                }

                LazyRow(
                    modifier = Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
                ) {
                    item {
                        SettingsBadge(
                            text = if (aiBotPromptVersionDraft.isBlank()) {
                                stringResource(R.string.settings_ai_prompt_version_unset)
                            } else {
                                aiBotPromptVersionDraft
                            },
                            icon = Icons.Default.Settings,
                            isActive = aiBotPromptVersionDraft.isNotBlank(),
                        )
                    }
                    item {
                        SettingsBadge(
                            text = stringResource(R.string.settings_ai_runtime_chip_faq, aiBotFaqModeDraft),
                            icon = Icons.Default.Email,
                            isActive = true,
                        )
                    }
                    item {
                        SettingsBadge(
                            text = stringResource(R.string.settings_ai_runtime_chip_quality, aiBotQualityModeDraft),
                            icon = Icons.Default.CheckCircle,
                            isActive = true,
                        )
                    }
                    item {
                        SettingsBadge(
                            text = stringResource(
                                R.string.settings_ai_runtime_chip_provider,
                                AiRuntimeAgentProvider.resolve(aiAgentProviderDraft).displayTitle,
                            ),
                            icon = Icons.Default.Bolt,
                            isActive = true,
                        )
                    }
                    item {
                        SettingsBadge(
                            text = if (aiCostGuardEnabledDraft) {
                                stringResource(R.string.settings_ai_badge_cost_guard_on)
                            } else {
                                stringResource(R.string.settings_ai_badge_cost_guard_off)
                            },
                            icon = Icons.Default.CheckCircle,
                            isActive = aiCostGuardEnabledDraft,
                        )
                    }
                    item {
                        SettingsBadge(
                            text = if (aiManusEnabledDraft) {
                                stringResource(R.string.settings_ai_badge_manus_backend_on)
                            } else {
                                stringResource(R.string.settings_ai_badge_manus_backend_off)
                            },
                            icon = Icons.Default.Settings,
                            isActive = aiManusEnabledDraft,
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.settings_admin_ai_bot_core_heading),
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )

                OutlinedTextField(
                    value = aiBotPromptVersionDraft,
                    onValueChange = { aiBotPromptVersionDraft = it.take(120) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_ai_prompt_version)) },
                    singleLine = true,
                )

                OutlinedTextField(
                    value = aiBotPersonalityStyleDraft,
                    onValueChange = { aiBotPersonalityStyleDraft = it.take(160) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_ai_personality_style)) },
                    singleLine = true,
                )

                OutlinedTextField(
                    value = aiBotLoggingLevelDraft,
                    onValueChange = { aiBotLoggingLevelDraft = it.take(80) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_ai_logging_level)) },
                    singleLine = true,
                )

                Text(
                    text = stringResource(R.string.settings_admin_ai_section_quality_mode),
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
                LazyRow(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
                ) {
                    listOf(
                        "balanced" to R.string.settings_ai_label_quality_balanced,
                        "high" to R.string.settings_ai_label_quality_high,
                    ).forEach { (value, labelRes) ->
                        item {
                            val label = stringResource(labelRes)
                            val isSelected = aiBotQualityModeDraft == value
                            OutlinedButton(
                                onClick = { aiBotQualityModeDraft = value },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                                ),
                                shape = RoundedCornerShape(SkydownUiTokens.compactRadius),
                            ) { Text(label) }
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.settings_admin_ai_section_faq_mode),
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
                LazyRow(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
                ) {
                    listOf(
                        "off" to R.string.settings_ai_label_faq_off,
                        "auto" to R.string.settings_ai_label_faq_auto,
                        "prefer_faq" to R.string.settings_ai_label_faq_prefer,
                    ).forEach { (value, labelRes) ->
                        item {
                            val label = stringResource(labelRes)
                            val isSelected = aiBotFaqModeDraft == value
                            OutlinedButton(
                                onClick = { aiBotFaqModeDraft = value },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                                ),
                                shape = RoundedCornerShape(SkydownUiTokens.compactRadius),
                            ) { Text(label) }
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.settings_admin_ai_section_owner_mode),
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
                LazyRow(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
                ) {
                    listOf(
                        "standard" to R.string.settings_ai_label_owner_standard,
                        "diagnostic" to R.string.settings_ai_label_owner_diagnostic,
                    ).forEach { (value, labelRes) ->
                        item {
                            val label = stringResource(labelRes)
                            val isSelected = aiBotOwnerModeDraft == value
                            OutlinedButton(
                                onClick = { aiBotOwnerModeDraft = value },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                                ),
                                shape = RoundedCornerShape(SkydownUiTokens.compactRadius),
                            ) { Text(label) }
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.settings_admin_ai_section_answer_length),
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
                LazyRow(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
                ) {
                    listOf(
                        "adaptive" to R.string.settings_ai_label_answer_adaptive,
                        "short" to R.string.settings_ai_label_answer_short,
                        "detailed" to R.string.settings_ai_label_answer_detailed,
                    ).forEach { (value, labelRes) ->
                        item {
                            val label = stringResource(labelRes)
                            val isSelected = aiBotAnswerLengthDraft == value
                            OutlinedButton(
                                onClick = { aiBotAnswerLengthDraft = value },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                                ),
                                shape = RoundedCornerShape(SkydownUiTokens.compactRadius),
                            ) { Text(label) }
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.settings_admin_ai_section_diagnostics),
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
                LazyRow(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
                ) {
                    listOf(
                        "off" to R.string.settings_ai_label_diag_off,
                        "owner_only" to R.string.settings_ai_label_diag_owner_only,
                        "verbose" to R.string.settings_ai_label_diag_verbose,
                    ).forEach { (value, labelRes) ->
                        item {
                            val label = stringResource(labelRes)
                            val isSelected = aiBotDiagnosticsModeDraft == value
                            OutlinedButton(
                                onClick = { aiBotDiagnosticsModeDraft = value },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                                ),
                                shape = RoundedCornerShape(SkydownUiTokens.compactRadius),
                            ) { Text(label) }
                        }
                    }
                }

                SettingsToggleRow(
                    title = stringResource(R.string.settings_toggle_kill_switch_title),
                    body = stringResource(R.string.settings_toggle_kill_switch_body),
                    checked = aiBotKillSwitchDraft,
                    onCheckedChange = { aiBotKillSwitchDraft = it },
                    modifier = Modifier.padding(top = 10.dp),
                )

                SettingsToggleRow(
                    title = stringResource(R.string.settings_toggle_bot_cost_guard_title),
                    body = stringResource(R.string.settings_toggle_bot_cost_guard_body),
                    checked = aiBotCostGuardEnabledDraft,
                    onCheckedChange = { aiBotCostGuardEnabledDraft = it },
                    modifier = Modifier.padding(top = 8.dp),
                )

                SettingsToggleRow(
                    title = stringResource(R.string.settings_toggle_brief_critical_title),
                    body = stringResource(R.string.settings_toggle_brief_critical_body),
                    checked = aiBotPreferBriefCriticalDraft,
                    onCheckedChange = { aiBotPreferBriefCriticalDraft = it },
                    modifier = Modifier.padding(top = 8.dp),
                )

                SettingsToggleRow(
                    title = stringResource(R.string.settings_toggle_faq_topic_title),
                    body = stringResource(R.string.settings_toggle_faq_topic_body),
                    checked = aiBotPreferFaqRoutingDraft,
                    onCheckedChange = { aiBotPreferFaqRoutingDraft = it },
                    modifier = Modifier.padding(top = 8.dp),
                )

                SettingsToggleRow(
                    title = stringResource(R.string.settings_toggle_product_guide_title),
                    body = stringResource(R.string.settings_toggle_product_guide_body),
                    checked = aiBotPreferProductGuideDraft,
                    onCheckedChange = { aiBotPreferProductGuideDraft = it },
                    modifier = Modifier.padding(top = 8.dp),
                )

                SettingsToggleRow(
                    title = stringResource(R.string.settings_toggle_visual_gen_title),
                    body = stringResource(R.string.settings_toggle_visual_gen_body),
                    checked = aiBotAllowVisualGenerationDraft,
                    onCheckedChange = { aiBotAllowVisualGenerationDraft = it },
                    modifier = Modifier.padding(top = 8.dp),
                )

                SettingsToggleRow(
                    title = stringResource(R.string.settings_toggle_text_fallback_title),
                    body = stringResource(R.string.settings_toggle_text_fallback_body),
                    checked = aiBotAllowTextFallbackDraft,
                    onCheckedChange = { aiBotAllowTextFallbackDraft = it },
                    modifier = Modifier.padding(top = 8.dp),
                )

                SettingsToggleRow(
                    title = stringResource(R.string.settings_toggle_visual_fallback_title),
                    body = stringResource(R.string.settings_toggle_visual_fallback_body),
                    checked = aiBotAllowVisualFallbackDraft,
                    onCheckedChange = { aiBotAllowVisualFallbackDraft = it },
                    modifier = Modifier.padding(top = 8.dp),
                )

                SettingsToggleRow(
                    title = stringResource(R.string.settings_toggle_show_fallback_reason_title),
                    body = stringResource(R.string.settings_toggle_show_fallback_reason_body),
                    checked = aiBotExposeFallbackReasonDraft,
                    onCheckedChange = { aiBotExposeFallbackReasonDraft = it },
                    modifier = Modifier.padding(top = 8.dp),
                )

                SettingsToggleRow(
                    title = stringResource(R.string.settings_toggle_safe_mode_title),
                    body = stringResource(R.string.settings_toggle_safe_mode_body),
                    checked = aiBotSafeModeEnabledDraft,
                    onCheckedChange = { aiBotSafeModeEnabledDraft = it },
                    modifier = Modifier.padding(top = 8.dp),
                )

                SettingsToggleRow(
                    title = stringResource(R.string.settings_toggle_strict_unknown_title),
                    body = stringResource(R.string.settings_toggle_strict_unknown_body),
                    checked = aiBotStrictUnknownHandlingDraft,
                    onCheckedChange = { aiBotStrictUnknownHandlingDraft = it },
                    modifier = Modifier.padding(top = 8.dp),
                )

                SettingsToggleRow(
                    title = stringResource(R.string.settings_toggle_block_speculative_faq_title),
                    body = stringResource(R.string.settings_toggle_block_speculative_faq_body),
                    checked = aiBotBlockSpeculativeFaqDraft,
                    onCheckedChange = { aiBotBlockSpeculativeFaqDraft = it },
                    modifier = Modifier.padding(top = 8.dp),
                )

                SettingsToggleRow(
                    title = stringResource(R.string.settings_toggle_proactive_hints_title),
                    body = stringResource(R.string.settings_toggle_proactive_hints_body),
                    checked = aiBotProactiveHintsEnabledDraft,
                    onCheckedChange = { aiBotProactiveHintsEnabledDraft = it },
                    modifier = Modifier.padding(top = 8.dp),
                )

                SettingsToggleRow(
                    title = stringResource(R.string.settings_toggle_trigger_ai_limit_title),
                    body = stringResource(R.string.settings_toggle_trigger_ai_limit_body),
                    checked = aiBotTriggerAiLimitNearEnabledDraft,
                    onCheckedChange = { aiBotTriggerAiLimitNearEnabledDraft = it },
                    modifier = Modifier.padding(top = 8.dp),
                )

                SettingsToggleRow(
                    title = stringResource(R.string.settings_toggle_trigger_restore_title),
                    body = stringResource(R.string.settings_toggle_trigger_restore_body),
                    checked = aiBotTriggerRestoreAvailableEnabledDraft,
                    onCheckedChange = { aiBotTriggerRestoreAvailableEnabledDraft = it },
                    modifier = Modifier.padding(top = 8.dp),
                )

                SettingsToggleRow(
                    title = stringResource(R.string.settings_toggle_trigger_order_shipped_title),
                    body = stringResource(R.string.settings_toggle_trigger_order_shipped_body),
                    checked = aiBotTriggerOrderShippedEnabledDraft,
                    onCheckedChange = { aiBotTriggerOrderShippedEnabledDraft = it },
                    modifier = Modifier.padding(top = 8.dp),
                )

                SettingsToggleRow(
                    title = stringResource(R.string.settings_toggle_trigger_payment_changed_title),
                    body = stringResource(R.string.settings_toggle_trigger_payment_changed_body),
                    checked = aiBotTriggerPaymentMethodsChangedEnabledDraft,
                    onCheckedChange = { aiBotTriggerPaymentMethodsChangedEnabledDraft = it },
                    modifier = Modifier.padding(top = 8.dp),
                )

                SettingsToggleRow(
                    title = stringResource(R.string.settings_toggle_trigger_upgrade_usage_title),
                    body = stringResource(R.string.settings_toggle_trigger_upgrade_usage_body),
                    checked = aiBotTriggerUsageBasedUpgradeEnabledDraft,
                    onCheckedChange = { aiBotTriggerUsageBasedUpgradeEnabledDraft = it },
                    modifier = Modifier.padding(top = 8.dp),
                )

                OutlinedTextField(
                    value = aiBotWarningThresholdPercentDraft,
                    onValueChange = { aiBotWarningThresholdPercentDraft = it.take(3) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_ai_warning_threshold)) },
                    singleLine = true,
                )

                OutlinedTextField(
                    value = aiBotCriticalThresholdPercentDraft,
                    onValueChange = { aiBotCriticalThresholdPercentDraft = it.take(3) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_ai_critical_threshold)) },
                    singleLine = true,
                )

                OutlinedTextField(
                    value = aiBotUpgradeHintFreeToProTextDraft,
                    onValueChange = { aiBotUpgradeHintFreeToProTextDraft = it.take(220) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_ai_upgrade_hint_free_pro)) },
                    singleLine = false,
                    minLines = 2,
                    maxLines = 3,
                )

                OutlinedTextField(
                    value = aiBotUpgradeHintProToCreatorTextDraft,
                    onValueChange = { aiBotUpgradeHintProToCreatorTextDraft = it.take(220) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_ai_upgrade_hint_pro_creator)) },
                    singleLine = false,
                    minLines = 2,
                    maxLines = 3,
                )

                OutlinedTextField(
                    value = aiBotPromptVersionAliasDraft,
                    onValueChange = { aiBotPromptVersionAliasDraft = it.take(120) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_ai_prompt_version_alias)) },
                    singleLine = true,
                )

                Text(
                    text = stringResource(R.string.settings_admin_ai_section_faq_priority),
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
                LazyRow(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
                ) {
                    listOf(
                        "live_owner_generic" to R.string.settings_ai_label_faq_priority_live_owner,
                        "owner_live_generic" to R.string.settings_ai_label_faq_priority_owner_live,
                        "balanced" to R.string.settings_ai_label_faq_priority_balanced,
                    ).forEach { (value, labelRes) ->
                        item {
                            val label = stringResource(labelRes)
                            val isSelected = aiBotFaqPriorityModeDraft == value
                            OutlinedButton(
                                onClick = { aiBotFaqPriorityModeDraft = value },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                                ),
                                shape = RoundedCornerShape(SkydownUiTokens.compactRadius),
                            ) { Text(label) }
                        }
                    }
                }

                OutlinedTextField(
                    value = aiBotTextPrimaryModelDraft,
                    onValueChange = { aiBotTextPrimaryModelDraft = it.take(120) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_ai_text_primary_model)) },
                    singleLine = true,
                )

                OutlinedTextField(
                    value = aiBotTextFallbackModelDraft,
                    onValueChange = { aiBotTextFallbackModelDraft = it.take(120) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_ai_text_fallback_model)) },
                    singleLine = true,
                )

                OutlinedTextField(
                    value = aiBotVisualPrimaryModelDraft,
                    onValueChange = { aiBotVisualPrimaryModelDraft = it.take(120) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_ai_visual_primary_model)) },
                    singleLine = true,
                )

                OutlinedTextField(
                    value = aiBotVisualFallbackModelDraft,
                    onValueChange = { aiBotVisualFallbackModelDraft = it.take(120) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_ai_visual_fallback_model)) },
                    singleLine = true,
                )

                OutlinedTextField(
                    value = aiBotShortAnswerMaxTokensDraft,
                    onValueChange = { aiBotShortAnswerMaxTokensDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_ai_short_answer_max_tokens)) },
                    singleLine = true,
                )

                OutlinedTextField(
                    value = aiBotStandardAnswerMaxTokensDraft,
                    onValueChange = { aiBotStandardAnswerMaxTokensDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_ai_standard_answer_max_tokens)) },
                    singleLine = true,
                )

                SettingsToggleRow(
                    title = stringResource(R.string.settings_toggle_cost_guard_title),
                    body = stringResource(R.string.settings_toggle_cost_guard_body),
                    checked = aiCostGuardEnabledDraft,
                    onCheckedChange = { aiCostGuardEnabledDraft = it },
                    modifier = Modifier.padding(top = 10.dp),
                )

                Text(
                    text = stringResource(R.string.settings_admin_ai_section_agent_provider),
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )

                LazyRow(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
                ) {
                    items(AiRuntimeAgentProvider.entries, key = { it.rawValue }) { provider ->
                        val isSelected = aiAgentProviderDraft == provider.rawValue
                        OutlinedButton(
                            onClick = { aiAgentProviderDraft = provider.rawValue },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                            ),
                            shape = RoundedCornerShape(SkydownUiTokens.compactRadius),
                        ) {
                            Text(provider.displayTitle)
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.settings_admin_ai_section_fallback_provider),
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )

                LazyRow(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
                ) {
                    items(AiRuntimeAgentProvider.entries, key = { it.rawValue }) { provider ->
                        val isSelected = aiFallbackAgentProviderDraft == provider.rawValue
                        OutlinedButton(
                            onClick = { aiFallbackAgentProviderDraft = provider.rawValue },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                            ),
                            shape = RoundedCornerShape(SkydownUiTokens.compactRadius),
                        ) {
                            Text(provider.displayTitle)
                        }
                    }
                }

                SettingsToggleRow(
                    title = stringResource(R.string.settings_toggle_manus_enable_title),
                    body = stringResource(R.string.settings_toggle_manus_enable_body),
                    checked = aiManusEnabledDraft,
                    onCheckedChange = { aiManusEnabledDraft = it },
                    modifier = Modifier.padding(top = 10.dp),
                )

                Text(
                    text = stringResource(R.string.settings_admin_ai_manus_runtime_heading),
                    modifier = Modifier.padding(top = 10.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )

                Text(
                    text = stringResource(R.string.settings_admin_ai_manus_secret_hint),
                    modifier = Modifier.padding(top = 4.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodySmall,
                )

                OutlinedTextField(
                    value = aiManusRequestTimeoutMsDraft,
                    onValueChange = { aiManusRequestTimeoutMsDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_ai_request_timeout_ms)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = aiManusPollIntervalMsDraft,
                    onValueChange = { aiManusPollIntervalMsDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_ai_poll_interval_ms)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = aiManusMaxPollAttemptsDraft,
                    onValueChange = { aiManusMaxPollAttemptsDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_ai_max_poll_attempts)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = aiManusListMessagesLimitDraft,
                    onValueChange = { aiManusListMessagesLimitDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_ai_list_messages_limit)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = aiManusMaxPromptCharsDraft,
                    onValueChange = { aiManusMaxPromptCharsDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_ai_max_prompt_chars)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = aiManusMaxHistoryTurnsDraft,
                    onValueChange = { aiManusMaxHistoryTurnsDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_ai_max_history_turns)) },
                    singleLine = true,
                )

                SettingsToggleRow(
                    title = stringResource(R.string.settings_toggle_manus_autostop_title),
                    body = stringResource(R.string.settings_toggle_manus_autostop_body),
                    checked = aiManusAutoStopOnWaitingDraft,
                    onCheckedChange = { aiManusAutoStopOnWaitingDraft = it },
                    modifier = Modifier.padding(top = 10.dp),
                )
                SettingsToggleRow(
                    title = stringResource(R.string.settings_toggle_manus_block_high_credit_title),
                    body = stringResource(R.string.settings_toggle_manus_block_high_credit_body),
                    checked = aiManusBlockHighCreditEventsDraft,
                    onCheckedChange = { aiManusBlockHighCreditEventsDraft = it },
                    modifier = Modifier.padding(top = 8.dp),
                )
                SettingsToggleRow(
                    title = stringResource(R.string.settings_toggle_manus_verbose_title),
                    body = stringResource(R.string.settings_toggle_manus_verbose_body),
                    checked = aiManusIncludeVerboseEventsDraft,
                    onCheckedChange = { aiManusIncludeVerboseEventsDraft = it },
                    modifier = Modifier.padding(top = 8.dp),
                )

                OutlinedTextField(
                    value = aiHardTextLimitDraft,
                    onValueChange = { aiHardTextLimitDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_ai_hard_cap_text)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = aiHardVisualLimitDraft,
                    onValueChange = { aiHardVisualLimitDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_ai_hard_cap_visual)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = aiHardAgentLimitDraft,
                    onValueChange = { aiHardAgentLimitDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_ai_hard_cap_agent)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = aiGlobalTextLimitDraft,
                    onValueChange = { aiGlobalTextLimitDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_ai_global_cap_text)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = aiGlobalVisualLimitDraft,
                    onValueChange = { aiGlobalVisualLimitDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_ai_global_cap_visual)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = aiGlobalAgentLimitDraft,
                    onValueChange = { aiGlobalAgentLimitDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text(stringResource(R.string.settings_ai_global_cap_agent)) },
                    singleLine = true,
                )

                Button(
                    onClick = {
                        val currentRuntime = uiState.aiRuntimeSettings
                        val updatedRuntime = currentRuntime.copy(
                            costGuardEnabled = aiCostGuardEnabledDraft,
                            agentProvider = AiRuntimeAgentProvider.resolve(aiAgentProviderDraft),
                            fallbackAgentProvider = AiRuntimeAgentProvider.resolve(aiFallbackAgentProviderDraft),
                            hardDailyCaps = currentRuntime.hardDailyCaps.copy(
                                text = aiHardTextLimitDraft.parsePositiveIntOrDefault(currentRuntime.hardDailyCaps.text),
                                visual = aiHardVisualLimitDraft.parsePositiveIntOrDefault(currentRuntime.hardDailyCaps.visual),
                                agent = aiHardAgentLimitDraft.parsePositiveIntOrDefault(currentRuntime.hardDailyCaps.agent),
                            ),
                            globalDailyCaps = currentRuntime.globalDailyCaps.copy(
                                text = aiGlobalTextLimitDraft.parsePositiveIntOrDefault(currentRuntime.globalDailyCaps.text),
                                visual = aiGlobalVisualLimitDraft.parsePositiveIntOrDefault(currentRuntime.globalDailyCaps.visual),
                                agent = aiGlobalAgentLimitDraft.parsePositiveIntOrDefault(currentRuntime.globalDailyCaps.agent),
                            ),
                            manus = currentRuntime.manus.copy(
                                isEnabled = aiManusEnabledDraft,
                                requestTimeoutMs = aiManusRequestTimeoutMsDraft.parseIntInRangeOrDefault(
                                    fallback = currentRuntime.manus.requestTimeoutMs,
                                    min = 3000,
                                    max = 30000,
                                ),
                                pollIntervalMs = aiManusPollIntervalMsDraft.parseIntInRangeOrDefault(
                                    fallback = currentRuntime.manus.pollIntervalMs,
                                    min = 500,
                                    max = 5000,
                                ),
                                maxPollAttempts = aiManusMaxPollAttemptsDraft.parseIntInRangeOrDefault(
                                    fallback = currentRuntime.manus.maxPollAttempts,
                                    min = 2,
                                    max = 60,
                                ),
                                listMessagesLimit = aiManusListMessagesLimitDraft.parseIntInRangeOrDefault(
                                    fallback = currentRuntime.manus.listMessagesLimit,
                                    min = 5,
                                    max = 100,
                                ),
                                maxPromptChars = aiManusMaxPromptCharsDraft.parseIntInRangeOrDefault(
                                    fallback = currentRuntime.manus.maxPromptChars,
                                    min = 300,
                                    max = 12000,
                                ),
                                maxHistoryTurns = aiManusMaxHistoryTurnsDraft.parseIntInRangeOrDefault(
                                    fallback = currentRuntime.manus.maxHistoryTurns,
                                    min = 0,
                                    max = 24,
                                ),
                                autoStopOnWaiting = aiManusAutoStopOnWaitingDraft,
                                blockHighCreditEvents = aiManusBlockHighCreditEventsDraft,
                                includeVerboseEvents = aiManusIncludeVerboseEventsDraft,
                            ),
                            bot = currentRuntime.bot.copy(
                                promptVersion = aiBotPromptVersionDraft,
                                qualityMode = aiBotQualityModeDraft,
                                faqMode = aiBotFaqModeDraft,
                                ownerMode = aiBotOwnerModeDraft,
                                answerLength = aiBotAnswerLengthDraft,
                                personalityStyle = aiBotPersonalityStyleDraft,
                                loggingLevel = aiBotLoggingLevelDraft,
                                diagnosticsMode = aiBotDiagnosticsModeDraft,
                                killSwitchEnabled = aiBotKillSwitchDraft,
                                modelPolicy = currentRuntime.bot.modelPolicy.copy(
                                    textPrimaryModel = aiBotTextPrimaryModelDraft,
                                    textFallbackModel = aiBotTextFallbackModelDraft,
                                    visualPrimaryModel = aiBotVisualPrimaryModelDraft,
                                    visualFallbackModel = aiBotVisualFallbackModelDraft,
                                ),
                                costGuard = currentRuntime.bot.costGuard.copy(
                                    enabled = aiBotCostGuardEnabledDraft,
                                    preferBriefAnswersWhenCritical = aiBotPreferBriefCriticalDraft,
                                    shortAnswerMaxOutputTokens = aiBotShortAnswerMaxTokensDraft.parseIntInRangeOrDefault(
                                        fallback = currentRuntime.bot.costGuard.shortAnswerMaxOutputTokens,
                                        min = 80,
                                        max = 1200,
                                    ),
                                    standardAnswerMaxOutputTokens = aiBotStandardAnswerMaxTokensDraft.parseIntInRangeOrDefault(
                                        fallback = currentRuntime.bot.costGuard.standardAnswerMaxOutputTokens,
                                        min = 120,
                                        max = 2400,
                                    ),
                                ),
                                routingPolicy = currentRuntime.bot.routingPolicy.copy(
                                    preferFaqWhenTopicMatched = aiBotPreferFaqRoutingDraft,
                                    preferProductGuideForNewUsers = aiBotPreferProductGuideDraft,
                                    allowVisualGeneration = aiBotAllowVisualGenerationDraft,
                                ),
                                fallbackPolicy = currentRuntime.bot.fallbackPolicy.copy(
                                    allowTextFallback = aiBotAllowTextFallbackDraft,
                                    allowVisualFallback = aiBotAllowVisualFallbackDraft,
                                    exposeFallbackReason = aiBotExposeFallbackReasonDraft,
                                ),
                                safetyPolicy = currentRuntime.bot.safetyPolicy.copy(
                                    safeModeEnabled = aiBotSafeModeEnabledDraft,
                                    strictUnknownHandling = aiBotStrictUnknownHandlingDraft,
                                    blockSpeculativeFaqAnswers = aiBotBlockSpeculativeFaqDraft,
                                ),
                                actionLayer = currentRuntime.bot.actionLayer.copy(
                                    proactiveHintsEnabled = aiBotProactiveHintsEnabledDraft,
                                    triggerAiLimitNearEnabled = aiBotTriggerAiLimitNearEnabledDraft,
                                    triggerRestoreAvailableEnabled = aiBotTriggerRestoreAvailableEnabledDraft,
                                    triggerOrderShippedEnabled = aiBotTriggerOrderShippedEnabledDraft,
                                    triggerPaymentMethodsChangedEnabled = aiBotTriggerPaymentMethodsChangedEnabledDraft,
                                    triggerUsageBasedUpgradeEnabled = aiBotTriggerUsageBasedUpgradeEnabledDraft,
                                    warningThresholdPercent = aiBotWarningThresholdPercentDraft.parseIntInRangeOrDefault(
                                        fallback = currentRuntime.bot.actionLayer.warningThresholdPercent,
                                        min = 50,
                                        max = 99,
                                    ),
                                    criticalThresholdPercent = aiBotCriticalThresholdPercentDraft.parseIntInRangeOrDefault(
                                        fallback = currentRuntime.bot.actionLayer.criticalThresholdPercent,
                                        min = 60,
                                        max = 100,
                                    ),
                                    upgradeHintFreeToProText = aiBotUpgradeHintFreeToProTextDraft,
                                    upgradeHintProToCreatorText = aiBotUpgradeHintProToCreatorTextDraft,
                                    faqPriorityMode = aiBotFaqPriorityModeDraft,
                                    promptVersionAlias = aiBotPromptVersionAliasDraft,
                                ),
                            ),
                        )
                        viewModel.saveAiRuntimeSettings(updatedRuntime)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
                ) {
                    Text(stringResource(R.string.settings_ai_runtime_save))
                }
            }

            AdminWorkspaceSection.MembershipOps -> {
                Text(
                    text = stringResource(R.string.settings_membership_ops_subtitle),
                    modifier = Modifier.padding(top = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )

                if (membershipOpsError.isNotBlank()) {
                    Text(
                        text = membershipOpsError,
                        modifier = Modifier.padding(top = 12.dp),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                LazyRow(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
                ) {
                    items(MembershipOpsTab.entries.toList()) { tab ->
                        val selected = membershipOpsTab == tab.name
                        OutlinedButton(
                            onClick = { membershipOpsTab = tab.name },
                            shape = RoundedCornerShape(SkydownUiTokens.compactRadius),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selected) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text(tab.label, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                if (membershipOpsLoading) {
                    Row(
                        modifier = Modifier.padding(top = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text(stringResource(R.string.settings_membership_ops_loading))
                    }
                } else {
                    when (MembershipOpsTab.fromKey(membershipOpsTab)) {
                        MembershipOpsTab.Dashboard -> {
                            val windows = membershipDashboard["windows"] as? Map<*, *> ?: emptyMap<Any, Any>()
                            val d7 = windows["d7"] as? Map<*, *> ?: emptyMap<Any, Any>()
                            val alerts = membershipDashboard["alerts"] as? List<*> ?: emptyList<Any>()
                            val costOverlay = membershipDashboard["costOverlay"] as? Map<*, *> ?: emptyMap<Any, Any>()
                            Text(
                                text = stringResource(
                                    R.string.settings_membership_dashboard_7d,
                                    (d7["membershipOpens"] as? Number)?.toInt() ?: 0,
                                    (d7["purchaseSuccess"] as? Number)?.toInt() ?: 0,
                                    (d7["cvr"] as? Number)?.toDouble() ?: 0.0,
                                ),
                                modifier = Modifier.padding(top = 14.dp),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = stringResource(
                                    R.string.settings_membership_dashboard_alerts,
                                    alerts.size,
                                    (costOverlay["freePlanLoadRatio"] as? Number)?.toDouble() ?: 0.0,
                                ),
                                modifier = Modifier.padding(top = 8.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }

                        MembershipOpsTab.Recommendations -> {
                            if (membershipRecommendations.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.settings_membership_ops_empty_recommendations),
                                    modifier = Modifier.padding(top = 14.dp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                                )
                            } else {
                                Column(
                                    modifier = Modifier.padding(top = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill),
                                ) {
                                    membershipRecommendations.forEach { recommendation ->
                                        SkydownCard {
                                            Text(recommendation.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                recommendation.summary,
                                                modifier = Modifier.padding(top = 6.dp),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                                            )
                                            Text(
                                                stringResource(
                                                    R.string.settings_membership_recommendation_meta,
                                                    recommendation.recommendationType,
                                                    (recommendation.confidenceScore * 100).toInt(),
                                                    recommendation.severity,
                                                ),
                                                modifier = Modifier.padding(top = 6.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                            )
                                            Row(
                                                modifier = Modifier.padding(top = 10.dp),
                                                horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
                                            ) {
                                                Button(
                                                    onClick = {
                                                        coroutineScope.launch {
                                                            val lifecycleId = "lifecycle_${recommendation.id}_${System.currentTimeMillis()}"
                                                            membershipOpsRepository.startExperiment(
                                                                MembershipOpsExperimentDraft(
                                                                    lifecycleId = lifecycleId,
                                                                    recommendationId = recommendation.id,
                                                                    recommendationType = recommendation.recommendationType,
                                                                    notes = membershipOpsExperimentStartNotes,
                                                                ),
                                                            )
                                                            experimentLifecycleIdDraft = lifecycleId
                                                            selectedRecommendationId = recommendation.id
                                                            feedbackMessage = membershipOpsExperimentStartedMessage
                                                            feedbackType = ToastType.Success
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(SkydownUiTokens.tightRadius),
                                                ) { Text(stringResource(R.string.settings_membership_ops_start_experiment)) }
                                                OutlinedButton(
                                                    onClick = {
                                                        coroutineScope.launch {
                                                            membershipOpsRepository.rejectRecommendation(
                                                                recommendationId = recommendation.id,
                                                                recommendationType = recommendation.recommendationType,
                                                                notes = membershipOpsRejectNotes,
                                                            )
                                                            feedbackMessage = membershipOpsRecommendationRejectedMessage
                                                            feedbackType = ToastType.Warning
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(SkydownUiTokens.tightRadius),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                ) { Text(stringResource(R.string.settings_membership_ops_reject)) }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        MembershipOpsTab.Experiments -> {
                            Text(
                                text = stringResource(R.string.settings_membership_ops_complete_title),
                                modifier = Modifier.padding(top = 14.dp),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            OutlinedTextField(
                                value = experimentLifecycleIdDraft,
                                onValueChange = { experimentLifecycleIdDraft = it },
                                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                                label = { Text(stringResource(R.string.settings_experiment_lifecycle_id)) },
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = experimentLearningsDraft,
                                onValueChange = { experimentLearningsDraft = it.take(1000) },
                                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                                label = { Text(stringResource(R.string.settings_membership_ops_learnings)) },
                                minLines = 2,
                                maxLines = 5,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro), modifier = Modifier.padding(top = 10.dp)) {
                                OutlinedTextField(value = experimentCvrDeltaDraft, onValueChange = { experimentCvrDeltaDraft = it }, modifier = Modifier.weight(1f), label = { Text(stringResource(R.string.settings_experiment_cvr_delta)) }, singleLine = true)
                                OutlinedTextField(value = experimentAnnualDeltaDraft, onValueChange = { experimentAnnualDeltaDraft = it }, modifier = Modifier.weight(1f), label = { Text(stringResource(R.string.settings_experiment_annual_delta)) }, singleLine = true)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro), modifier = Modifier.padding(top = 10.dp)) {
                                OutlinedTextField(value = experimentCreatorDeltaDraft, onValueChange = { experimentCreatorDeltaDraft = it }, modifier = Modifier.weight(1f), label = { Text(stringResource(R.string.settings_experiment_creator_delta)) }, singleLine = true)
                                OutlinedTextField(value = experimentCancelDeltaDraft, onValueChange = { experimentCancelDeltaDraft = it }, modifier = Modifier.weight(1f), label = { Text(stringResource(R.string.settings_experiment_cancel_delta)) }, singleLine = true)
                            }
                            OutlinedTextField(
                                value = experimentObservedDaysDraft,
                                onValueChange = { experimentObservedDaysDraft = it },
                                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                                label = { Text(stringResource(R.string.settings_experiment_observed_window_days)) },
                                singleLine = true,
                            )
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        runCatching {
                                            membershipOpsRepository.completeExperiment(
                                                lifecycleId = experimentLifecycleIdDraft.trim(),
                                                cvrDelta = experimentCvrDeltaDraft.toDoubleOrNull() ?: 0.0,
                                                annualDelta = experimentAnnualDeltaDraft.toDoubleOrNull() ?: 0.0,
                                                creatorDelta = experimentCreatorDeltaDraft.toDoubleOrNull() ?: 0.0,
                                                cancelDelta = experimentCancelDeltaDraft.toDoubleOrNull() ?: 0.0,
                                                observedWindowDays = experimentObservedDaysDraft.toIntOrNull() ?: 14,
                                                success = (experimentCvrDeltaDraft.toDoubleOrNull() ?: 0.0) > 0,
                                                learnings = experimentLearningsDraft,
                                            )
                                        }.onSuccess {
                                            feedbackMessage = membershipOpsExperimentCompletedMessage
                                            feedbackType = ToastType.Success
                                        }.onFailure { error ->
                                            feedbackMessage = error.localizedMessage
                                                ?: membershipOpsExperimentCompleteFailedMessage
                                            feedbackType = ToastType.Error
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                                shape = RoundedCornerShape(SkydownUiTokens.compactRadius),
                            ) {
                                Text(stringResource(R.string.settings_membership_ops_complete_experiment))
                            }
                        }

                        MembershipOpsTab.Learnings -> {
                            val insights = membershipLearnings["insights"] as? Map<*, *> ?: emptyMap<Any, Any>()
                            val dataStrength = membershipLearnings["dataStrength"] as? String ?: "unknown"
                            Text(
                                text = stringResource(R.string.settings_membership_data_strength, dataStrength),
                                modifier = Modifier.padding(top = 14.dp),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = stringResource(
                                    R.string.settings_membership_calibration_line,
                                    (insights["confidenceCalibrationScore"] as? Number)?.toDouble() ?: 0.0,
                                    (insights["simulationAccuracyTrend"] as? Number)?.toDouble() ?: 0.0,
                                ),
                                modifier = Modifier.padding(top = 8.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            )
                            val topSurfaces = insights["bestConvertingSurfaces"] as? List<*> ?: emptyList<Any>()
                            Text(
                                text = stringResource(
                                    R.string.settings_membership_top_surfaces,
                                    topSurfaces.take(3).joinToString { ((it as? Map<*, *>)?.get("surface") as? String).orEmpty() },
                                ),
                                modifier = Modifier.padding(top = 8.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            )

                            SkydownCard(modifier = Modifier.padding(top = 14.dp)) {
                                Text(stringResource(R.string.settings_membership_ops_hygiene_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = stringResource(
                                        R.string.settings_membership_hygiene_profile_value,
                                        stringResource(R.string.settings_membership_ops_hygiene_profile),
                                        hygieneProfileLabel,
                                    ),
                                    modifier = Modifier.padding(top = 6.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f),
                                )
                                OutlinedTextField(
                                    value = hygieneCooldownCompletedDraft,
                                    onValueChange = { hygieneCooldownCompletedDraft = it },
                                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                                    label = { Text(stringResource(R.string.settings_hygiene_cooldown_days_completed)) },
                                    supportingText = { Text(stringResource(R.string.settings_hygiene_cooldown_days_completed_help)) },
                                    singleLine = true,
                                )
                                OutlinedTextField(
                                    value = hygieneCooldownRejectedDraft,
                                    onValueChange = { hygieneCooldownRejectedDraft = it },
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    label = { Text(stringResource(R.string.settings_hygiene_cooldown_days_rejected)) },
                                    supportingText = { Text(stringResource(R.string.settings_hygiene_cooldown_days_rejected_help)) },
                                    singleLine = true,
                                )
                                OutlinedTextField(
                                    value = hygieneCooldownProposedDraft,
                                    onValueChange = { hygieneCooldownProposedDraft = it },
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    label = { Text(stringResource(R.string.settings_hygiene_cooldown_days_proposed)) },
                                    supportingText = { Text(stringResource(R.string.settings_hygiene_cooldown_days_proposed_help)) },
                                    singleLine = true,
                                )
                                OutlinedTextField(
                                    value = hygieneSimilarityStrictnessDraft,
                                    onValueChange = { hygieneSimilarityStrictnessDraft = it },
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    label = { Text(stringResource(R.string.settings_hygiene_similarity_strictness)) },
                                    supportingText = { Text(stringResource(R.string.settings_hygiene_similarity_strictness_help)) },
                                    singleLine = true,
                                )
                                OutlinedTextField(
                                    value = hygieneRecurringPenaltyDraft,
                                    onValueChange = { hygieneRecurringPenaltyDraft = it },
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    label = { Text(stringResource(R.string.settings_hygiene_recurring_penalty)) },
                                    supportingText = { Text(stringResource(R.string.settings_hygiene_recurring_penalty_help)) },
                                    singleLine = true,
                                )
                                OutlinedTextField(
                                    value = hygieneFreshnessFloorDraft,
                                    onValueChange = { hygieneFreshnessFloorDraft = it },
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    label = { Text(stringResource(R.string.settings_hygiene_freshness_floor)) },
                                    supportingText = { Text(stringResource(R.string.settings_hygiene_freshness_floor_help)) },
                                    singleLine = true,
                                )
                                OutlinedTextField(
                                    value = hygieneDuplicateMergeWindowDraft,
                                    onValueChange = { hygieneDuplicateMergeWindowDraft = it },
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    label = { Text(stringResource(R.string.settings_hygiene_duplicate_merge_window)) },
                                    supportingText = { Text(stringResource(R.string.settings_hygiene_duplicate_merge_window_help)) },
                                    singleLine = true,
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill),
                                ) {
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                runCatching {
                                                    membershipOpsRepository.saveHygieneControls(
                                                        values = mapOf(
                                                            "cooldownDaysCompleted" to (hygieneCooldownCompletedDraft.toIntOrNull() ?: 10),
                                                            "cooldownDaysRejected" to (hygieneCooldownRejectedDraft.toIntOrNull() ?: 21),
                                                            "cooldownDaysProposed" to (hygieneCooldownProposedDraft.toIntOrNull() ?: 7),
                                                            "similarityStrictness" to hygieneSimilarityStrictnessDraft.trim(),
                                                            "recurringPenalty" to (hygieneRecurringPenaltyDraft.toDoubleOrNull() ?: 0.18),
                                                            "freshnessFloor" to (hygieneFreshnessFloorDraft.toDoubleOrNull() ?: 0.20),
                                                            "duplicateMergeWindowDays" to (hygieneDuplicateMergeWindowDraft.toIntOrNull() ?: 14),
                                                        ),
                                                    )
                                                }.onSuccess { result ->
                                                    hygieneProfileLabel = result["profile"] as? String ?: hygieneProfileLabel
                                                    feedbackMessage = membershipOpsHygieneSavedMessage
                                                    feedbackType = ToastType.Success
                                                }.onFailure { error ->
                                                    feedbackMessage = error.localizedMessage ?: membershipOpsHygieneSaveFailedMessage
                                                    feedbackType = ToastType.Error
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(SkydownUiTokens.tightRadius),
                                    ) { Text(stringResource(R.string.common_save)) }
                                    OutlinedButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                runCatching {
                                                    membershipOpsRepository.saveHygieneControls(
                                                        values = emptyMap(),
                                                        resetToDefaults = true,
                                                    )
                                                }.onSuccess { result ->
                                                    val hygiene = result["membershipHygiene"] as? Map<*, *> ?: emptyMap<Any, Any>()
                                                    hygieneCooldownCompletedDraft = (hygiene["cooldownDaysCompleted"] as? Number)?.toInt()?.toString() ?: "10"
                                                    hygieneCooldownRejectedDraft = (hygiene["cooldownDaysRejected"] as? Number)?.toInt()?.toString() ?: "21"
                                                    hygieneCooldownProposedDraft = (hygiene["cooldownDaysProposed"] as? Number)?.toInt()?.toString() ?: "7"
                                                    hygieneSimilarityStrictnessDraft = (hygiene["similarityStrictness"] as? String) ?: "balanced"
                                                    hygieneRecurringPenaltyDraft = (hygiene["recurringPenalty"] as? Number)?.toDouble()?.toString() ?: "0.18"
                                                    hygieneFreshnessFloorDraft = (hygiene["freshnessFloor"] as? Number)?.toDouble()?.toString() ?: "0.20"
                                                    hygieneDuplicateMergeWindowDraft = (hygiene["duplicateMergeWindowDays"] as? Number)?.toInt()?.toString() ?: "14"
                                                    hygieneProfileLabel = result["profile"] as? String ?: "balanced"
                                                    feedbackMessage = membershipOpsHygieneDefaultsRestoredMessage
                                                    feedbackType = ToastType.Success
                                                }.onFailure { error ->
                                                    feedbackMessage = error.localizedMessage
                                                        ?: membershipOpsResetDefaultsFailedMessage
                                                    feedbackType = ToastType.Error
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(SkydownUiTokens.tightRadius),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    ) { Text(stringResource(R.string.settings_membership_ops_reset_defaults)) }
                                }
                            }
                        }

                        MembershipOpsTab.Timeline -> {
                            LazyRow(
                                modifier = Modifier.padding(top = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
                            ) {
                                items(listOf("7d", "30d", "90d", "all")) { range ->
                                    val selected = membershipTimelineRange == range
                                    OutlinedButton(
                                        onClick = {
                                            membershipTimelineRange = range
                                            coroutineScope.launch {
                                                runCatching {
                                                    membershipOpsRepository.loadTimeline(range = range)
                                                }.onSuccess { timeline ->
                                                    membershipTimeline = timeline
                                                }.onFailure { error ->
                                                    feedbackMessage = error.localizedMessage ?: membershipOpsTimelineLoadFailedMessage
                                                    feedbackType = ToastType.Error
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(SkydownUiTokens.compactRadius),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    ) { Text(range) }
                                }
                            }

                            val entries = membershipTimeline["entries"] as? List<*> ?: emptyList<Any>()
                            if (entries.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.settings_membership_ops_timeline_empty),
                                    modifier = Modifier.padding(top = 14.dp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                                )
                            } else {
                                Column(
                                    modifier = Modifier.padding(top = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill),
                                ) {
                                    entries.take(40).forEach { row ->
                                        val item = row as? Map<*, *> ?: emptyMap<Any, Any>()
                                        val dateKey = item["dateKey"] as? String ?: "-"
                                        val type = item["type"] as? String ?: "event"
                                        val title = item["title"] as? String ?: type
                                        val summary = item["summary"] as? String ?: ""
                                        val ownerAction = item["ownerAction"] as? String ?: ""
                                        val recommendationId = item["recommendationId"] as? String ?: ""
                                        val learnings = item["learnings"] as? String ?: ""
                                        SkydownCard {
                                            Text(stringResource(R.string.settings_membership_ops_timeline_row, dateKey, type), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f))
                                            Text(title, modifier = Modifier.padding(top = 4.dp), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                            if (summary.isNotBlank()) {
                                                Text(summary, modifier = Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f))
                                            }
                                            if (ownerAction.isNotBlank()) {
                                                Text(stringResource(R.string.settings_membership_ops_owner_action_line, stringResource(R.string.settings_membership_ops_owner_action), ownerAction), modifier = Modifier.padding(top = 6.dp), style = MaterialTheme.typography.labelSmall)
                                            }
                                            if (learnings.isNotBlank()) {
                                                Text(stringResource(R.string.settings_membership_ops_learnings_line, stringResource(R.string.settings_membership_ops_learnings), learnings), modifier = Modifier.padding(top = 4.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f))
                                            }
                                            if (recommendationId.isNotBlank()) {
                                                OutlinedButton(
                                                    onClick = {
                                                        selectedRecommendationId = recommendationId
                                                        experimentLifecycleIdDraft = "lifecycle_${recommendationId}_${System.currentTimeMillis()}"
                                                        experimentLearningsDraft = resources.getString(
                                                            R.string.settings_membership_timeline_rerun_learnings,
                                                            title,
                                                        )
                                                        membershipOpsTab = MembershipOpsTab.Experiments.name
                                                    },
                                                    modifier = Modifier.padding(top = 10.dp),
                                                    shape = RoundedCornerShape(SkydownUiTokens.tightRadius),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                ) {
                                                    Text(stringResource(R.string.settings_membership_ops_rerun))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    SkydownTopBarTitle(
                        title = stringResource(R.string.settings_screen_title),
                        subtitle = stringResource(R.string.settings_screen_subtitle),
                    )
                },
                navigationIcon = {
                    onClose?.let { close ->
                        IconButton(onClick = close) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.common_close),
                            )
                        }
                    }
                },
                colors = skydownTopBarColors(),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    skydownScreenBrush(
                        secondaryColor = MaterialTheme.colorScheme.tertiary,
                        primaryAlpha = 0.06f,
                        secondaryAlpha = 0.05f,
                    ),
                ),
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
            ) {
                val contentMaxWidth = if (maxWidth > 1040.dp) 1040.dp else Dp.Unspecified
                val contentWidthFraction = if (contentMaxWidth != Dp.Unspecified && maxWidth > 0.dp) {
                    contentMaxWidth.value / maxWidth.value
                } else {
                    1f
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(contentWidthFraction)
                        .align(Alignment.TopCenter)
                        .testTag("settings.root"),
                    contentPadding = skydownContentPadding(innerPadding),
                    verticalArrangement = Arrangement.spacedBy(sectionSpacing),
                ) {
                item {
                    SettingsUtilityRow(
                        isOwner = uiState.isOwner,
                        onOpenPayments = {
                            if (uiState.isOwner) {
                                activeAdminWorkspaceKey = AdminWorkspaceSection.Payments.name
                                showAdminWorkspaceSheet = true
                            } else {
                                openSupportEmail(
                                    context = context,
                                    userEmail = uiState.email,
                                    userName = uiState.username,
                                    supportEmail = uiState.legalContentSettings.resolvedSupportEmail,
                                )
                            }
                        },
                        onOpenPrivacy = { activeLegalDocument.value = SettingsLegalDocumentType.PrivacyPolicy },
                        onOpenOrders = onOpenOrders,
                    )
                }

                item {
                    SkydownCard {
                        SectionHeader(stringResource(R.string.settings_section_profile))
                        if (uiState.isLoggedIn) {
                            Text(
                                text = stringResource(R.string.settings_signed_in_as, uiState.username),
                                modifier = Modifier.padding(top = 8.dp),
                                fontWeight = FontWeight.SemiBold,
                            )
                            if (uiState.email.isNotBlank()) {
                                Text(
                                    text = uiState.email,
                                    modifier = Modifier.padding(top = 4.dp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                                )
                            }
                            Button(
                                onClick = onOpenProfile,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
                            ) {
                                Text(stringResource(R.string.settings_open_edit_profile))
                            }
                            SettingsToggleRow(
                                title = stringResource(R.string.settings_ai_my_account_toggle_title),
                                body = stringResource(R.string.settings_ai_my_account_toggle_body),
                                checked = profileAiAccessEnabledDraft,
                                onCheckedChange = { profileAiAccessEnabledDraft = it },
                                enabled = !uiState.isSavingProfile && !uiState.isSigningOut && !uiState.isDeletingAccount,
                                modifier = Modifier.padding(top = 12.dp),
                            )
                            OutlinedButton(
                                onClick = { viewModel.saveAiAccessConsent(profileAiAccessEnabledDraft) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                enabled = !uiState.isSavingProfile &&
                                    !uiState.isSigningOut &&
                                    !uiState.isDeletingAccount &&
                                    profileAiAccessEnabledDraft != uiState.aiAccessEnabled,
                                shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
                            ) {
                                Text(stringResource(R.string.settings_save_ai_consent))
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill),
                            ) {
                                Button(
                                    onClick = { viewModel.signOut() },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !uiState.isSigningOut && !uiState.isDeletingAccount,
                                    shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
                                ) {
                                    Text(
                                        if (uiState.isSigningOut) {
                                            stringResource(R.string.settings_sign_out_progress)
                                        } else {
                                            stringResource(R.string.settings_sign_out)
                                        },
                                    )
                                }
                                OutlinedButton(
                                    onClick = { viewModel.signOut(onOpenLogin) },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !uiState.isSigningOut && !uiState.isDeletingAccount,
                                    shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
                                ) {
                                    Text(stringResource(R.string.settings_use_other_account))
                                }
                                OutlinedButton(
                                    onClick = { showDeleteAccountDialog.value = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !uiState.isSigningOut && !uiState.isDeletingAccount,
                                    shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error,
                                    ),
                                ) {
                                    Text(
                                        if (uiState.isDeletingAccount) {
                                            stringResource(R.string.settings_account_deleting)
                                        } else {
                                            stringResource(R.string.settings_account_delete)
                                        },
                                    )
                                }
                            }
                            uiState.accountErrorMessage?.let { message ->
                                Text(
                                    text = message,
                                    modifier = Modifier.padding(top = 10.dp),
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill),
                            ) {
                                Button(
                                    onClick = onOpenLogin,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
                                ) {
                                    Text(stringResource(R.string.auth_sign_in))
                                }
                                OutlinedButton(
                                    onClick = onOpenRegistration,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
                                ) {
                                    Text(stringResource(R.string.settings_register))
                                }
                            }
                        }
                    }
                }

                item {
                    SkydownCard(
                        modifier = Modifier.testTag("settings.membership.section"),
                    ) {
                        SectionHeader(stringResource(R.string.settings_section_membership))
                        Text(
                            text = stringResource(R.string.settings_membership_section_summary),
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        )
                        if (uiState.isLoggedIn) {
                            val membershipState = stringResource(
                                if (uiState.aiAccessEnabled) {
                                    R.string.settings_membership_state_active
                                } else {
                                    R.string.settings_membership_state_limited
                                },
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill),
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        if (uiState.isOwner) {
                                            activeAdminWorkspaceKey = AdminWorkspaceSection.MembershipOps.name
                                            showAdminWorkspaceSheet = true
                                        } else {
                                            openSupportEmail(
                                                context = context,
                                                userEmail = uiState.email,
                                                userName = uiState.username,
                                                supportEmail = uiState.legalContentSettings.resolvedSupportEmail,
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
                                ) {
                                    Text(
                                        if (uiState.isOwner) {
                                            stringResource(R.string.settings_membership_plan_manage, membershipState)
                                        } else {
                                            stringResource(R.string.settings_membership_billing_help, membershipState)
                                        },
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        openSupportEmail(
                                            context = context,
                                            userEmail = uiState.email,
                                            userName = uiState.username,
                                            supportEmail = uiState.legalContentSettings.resolvedSupportEmail,
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
                                ) {
                                    Text(stringResource(R.string.settings_check_restore))
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = onOpenLogin,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
                            ) {
                                Text(stringResource(R.string.auth_sign_in))
                            }
                        }
                    }
                }

                if (uiState.isOwner) {
                    item {
                        SkydownCard {
                            SectionHeader(stringResource(R.string.settings_section_owner))
                            Text(
                                text = stringResource(R.string.settings_owner_section_summary),
                                modifier = Modifier.padding(top = 8.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            )
                            Text(
                                text = stringResource(R.string.settings_owner_hub_hint),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 6.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                            )
                            OutlinedButton(
                                onClick = onOpenOwnerHub,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                                    .testTag("settings.open_owner_hub"),
                                shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
                            ) {
                                Text(stringResource(R.string.settings_open_owner_hub))
                            }
                            OwnerCommandCenterCard(
                                isOwner = uiState.isOwner,
                                paymentStatus = stringResource(R.string.settings_owner_payment_routes, visiblePaymentMethodCount),
                                userStatus = stringResource(R.string.settings_owner_user_accounts, uiState.managedUsers.size),
                                headerStatus = stringResource(R.string.settings_owner_headers_live, screenHeaderSettings.configuredCount),
                                aiStatus = if (uiState.aiRuntimeSettings.costGuardEnabled) {
                                    stringResource(R.string.settings_owner_ai_guard_on)
                                } else {
                                    stringResource(R.string.settings_owner_ai_guard_check)
                                },
                                onOpenUsers = {
                                    activeAdminWorkspaceKey = AdminWorkspaceSection.Users.name
                                    showAdminWorkspaceSheet = true
                                },
                                onOpenPayments = {
                                    activeAdminWorkspaceKey = AdminWorkspaceSection.Payments.name
                                    showAdminWorkspaceSheet = true
                                },
                                onOpenHeaders = {
                                    activeAdminWorkspaceKey = AdminWorkspaceSection.Headers.name
                                    showAdminWorkspaceSheet = true
                                },
                                onOpenAi = {
                                    activeAdminWorkspaceKey = AdminWorkspaceSection.AiPrompts.name
                                    showAdminWorkspaceSheet = true
                                },
                                modifier = Modifier.padding(top = 14.dp),
                            )
                            OutlinedButton(
                                onClick = {
                                    activeAdminWorkspaceKey = AdminWorkspaceSection.Users.name
                                    showAdminWorkspaceSheet = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
                            ) {
                                Text(stringResource(R.string.settings_all_admin_sections))
                            }
                            OutlinedButton(
                                onClick = onOpenOrders,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
                            ) {
                                Text(stringResource(R.string.settings_open_orders))
                            }
                        }
                    }
                }

                if (uiState.isLoggedIn) {
                    item {
                        SkydownCard {
                            SectionHeader(stringResource(R.string.settings_section_ai_agent))
                            Row(
                                modifier = Modifier.padding(top = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
                            ) {
                                SettingsBadge(
                                    text = if (uiState.workflowAutomationSettings.isPrepared) {
                                        stringResource(R.string.settings_dashboard_automation_ready)
                                    } else {
                                        stringResource(R.string.settings_dashboard_automation_open)
                                    },
                                    icon = Icons.Default.Bolt,
                                    isActive = uiState.workflowAutomationSettings.isPrepared,
                                    onClick = {
                                        activeAdminWorkspaceKey = AdminWorkspaceSection.Automation.name
                                        showAdminWorkspaceSheet = true
                                    },
                                )
                                SettingsBadge(
                                    text = if (uiState.agentProfileSettings.isConfigured) {
                                        stringResource(R.string.settings_dashboard_skills_ready)
                                    } else {
                                        stringResource(R.string.settings_dashboard_skills_open)
                                    },
                                    icon = Icons.Default.Settings,
                                    isActive = uiState.agentProfileSettings.isConfigured,
                                    onClick = {
                                        activeAdminWorkspaceKey = AdminWorkspaceSection.Automation.name
                                        showAdminWorkspaceSheet = true
                                    },
                                )
                                SettingsBadge(
                                    text = if (uiState.manusByosSettings.isEnabled && uiState.manusByosSettings.hasApiKey) {
                                        stringResource(R.string.settings_dashboard_manus_byos_ready)
                                    } else {
                                        stringResource(R.string.settings_dashboard_manus_byos_open)
                                    },
                                    icon = Icons.Default.Bolt,
                                    isActive = uiState.manusByosSettings.isEnabled && uiState.manusByosSettings.hasApiKey,
                                    onClick = {
                                        activeAdminWorkspaceKey = AdminWorkspaceSection.Automation.name
                                        showAdminWorkspaceSheet = true
                                    },
                                )
                                uiState.workflowAutomationSettings.workflowName
                                    .takeIf { it.isNotBlank() }
                                    ?.let { workflowName ->
                                        SettingsBadge(
                                            text = workflowName.take(26),
                                            icon = Icons.Default.Settings,
                                            isActive = true,
                                            onClick = {
                                                activeAdminWorkspaceKey = AdminWorkspaceSection.Automation.name
                                                showAdminWorkspaceSheet = true
                                            },
                                        )
                                    }
                            }
                            Button(
                                onClick = {
                                    activeAdminWorkspaceKey = AdminWorkspaceSection.Automation.name
                                    showAdminWorkspaceSheet = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
                            ) {
                                Text(stringResource(R.string.settings_manage_agent_service))
                            }
                        }
                    }
                }

                item {
                    SkydownCard {
                        val notificationsEnabledToast = stringResource(R.string.settings_notifications_toast_enabled)
                        val notificationsEnableInSettingsToast = stringResource(R.string.settings_notifications_toast_enable_in_settings)
                        val notificationsManageInSettingsToast = stringResource(R.string.settings_notifications_toast_manage_in_settings)

                        SectionHeader(stringResource(R.string.settings_section_system))
                        SettingsToggleRow(
                            title = stringResource(R.string.settings_notifications_title),
                            body = stringResource(R.string.settings_notifications_subtitle),
                            checked = uiState.notificationsEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    if (NotificationPermissionCoordinator.areNotificationsEnabled(context)) {
                                        viewModel.updateNotifications(true)
                                        feedbackMessage = notificationsEnabledToast
                                        feedbackType = ToastType.Success
                                    } else if (NotificationPermissionCoordinator.requiresRuntimePermission()) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        NotificationPermissionCoordinator.openNotificationSettings(context)
                                        feedbackMessage = notificationsEnableInSettingsToast
                                        feedbackType = ToastType.Info
                                    }
                                } else {
                                    NotificationPermissionCoordinator.openNotificationSettings(context)
                                    feedbackMessage = notificationsManageInSettingsToast
                                    feedbackType = ToastType.Info
                                }
                            },
                            modifier = Modifier.padding(top = 12.dp),
                        )
                        Text(
                            text = stringResource(R.string.settings_workflow_status_title),
                            modifier = Modifier.padding(top = 14.dp),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(R.string.settings_workflow_status_reminder),
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                        )
                        Text(
                            text = stringResource(R.string.settings_workflow_status_tasks),
                            modifier = Modifier.padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        )
                        Text(
                            text = stringResource(R.string.settings_workflow_status_notes),
                            modifier = Modifier.padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        )
                    }
                }

                item {
                    SkydownCard {
                        SectionHeader(stringResource(R.string.settings_section_theme))
                        AppearanceMode.entries.forEach { scheme ->
                            val schemeTitle = when (scheme) {
                                AppearanceMode.Light -> stringResource(R.string.settings_theme_light)
                                AppearanceMode.Dark -> stringResource(R.string.settings_theme_dark)
                                AppearanceMode.System -> stringResource(R.string.settings_theme_system)
                            }
                            AppearanceChoiceRow(
                                title = schemeTitle,
                                selected = uiState.colorScheme == scheme,
                                onClick = { viewModel.updateColorScheme(scheme) },
                                modifier = Modifier.padding(top = 10.dp),
                            )
                        }
                    }
                }

                item {
                    SkydownCard {
                        SectionHeader(stringResource(R.string.settings_legal_section_title))
                        Text(
                            text = stringResource(R.string.settings_legal_section_subtitle),
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        )
                        Text(
                            text = stringResource(R.string.settings_legal_version_label, uiState.appVersion),
                            modifier = Modifier.padding(top = 10.dp),
                        )
                        OutlinedButton(
                            onClick = {
                                activeLegalDocument.value = SettingsLegalDocumentType.ReadmeGuide
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
                        ) {
                            Text(stringResource(R.string.settings_legal_faq_guide))
                        }
                        OutlinedButton(
                            onClick = {
                                activeLegalDocument.value = SettingsLegalDocumentType.TermsAndConditions
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
                        ) {
                            Text(stringResource(R.string.settings_legal_terms_agb))
                        }
                        OutlinedButton(
                            onClick = {
                                activeLegalDocument.value = SettingsLegalDocumentType.PrivacyPolicy
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
                        ) {
                            Text(stringResource(R.string.settings_legal_privacy))
                        }
                        OutlinedButton(
                            onClick = {
                                activeLegalDocument.value = SettingsLegalDocumentType.TermsOfService
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
                        ) {
                            Text(stringResource(R.string.settings_legal_terms_of_use))
                        }
                        OutlinedButton(
                            onClick = {
                                activeLegalDocument.value = SettingsLegalDocumentType.SubscriptionTerms
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
                        ) {
                            Text(stringResource(R.string.settings_legal_subscription))
                        }
                        OutlinedButton(
                            onClick = {
                                activeLegalDocument.value = SettingsLegalDocumentType.AiUsageNotice
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
                        ) {
                            Text(stringResource(R.string.settings_legal_ai_usage))
                        }
                        OutlinedButton(
                            onClick = {
                                activeLegalDocument.value = SettingsLegalDocumentType.ImprintInfo
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
                        ) {
                            Text(stringResource(R.string.settings_legal_imprint))
                        }
                        Text(
                            text = uiState.legalContentSettings.resolvedSupportEmail,
                            modifier = Modifier.padding(top = 12.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        )
                        Button(
                            onClick = {
                                openSupportEmail(
                                    context = context,
                                    userEmail = uiState.email,
                                    userName = uiState.username,
                                    supportEmail = uiState.legalContentSettings.resolvedSupportEmail,
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
                        ) {
                            Text(stringResource(R.string.settings_legal_support_cta))
                        }
                        Text(
                            text = stringResource(R.string.settings_legal_availability_note),
                            modifier = Modifier.padding(top = 10.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        )

                        if (uiState.isOwner) {
                            Text(
                                text = stringResource(R.string.settings_admin_legal_owner_heading),
                                modifier = Modifier.padding(top = 14.dp),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = stringResource(R.string.settings_admin_legal_owner_intro),
                                modifier = Modifier.padding(top = 6.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            )

                            OutlinedTextField(
                                value = legalBrandNameDraft,
                                onValueChange = { legalBrandNameDraft = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                label = { Text(stringResource(R.string.settings_legal_brand_name)) },
                                placeholder = { Text(stringResource(R.string.settings_legal_brand_name_placeholder)) },
                                singleLine = true,
                            )

                            OutlinedTextField(
                                value = legalOperatorNameDraft,
                                onValueChange = { legalOperatorNameDraft = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                label = { Text(stringResource(R.string.settings_legal_operator)) },
                                placeholder = { Text(stringResource(R.string.settings_legal_operator_placeholder)) },
                                singleLine = true,
                            )

                            OutlinedTextField(
                                value = legalRightsHolderNameDraft,
                                onValueChange = { legalRightsHolderNameDraft = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                label = { Text(stringResource(R.string.settings_legal_rights_holder)) },
                                placeholder = { Text(stringResource(R.string.settings_legal_operator_placeholder)) },
                                singleLine = true,
                            )

                            OutlinedTextField(
                                value = legalSupportEmailDraft,
                                onValueChange = { legalSupportEmailDraft = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                label = { Text(stringResource(R.string.settings_legal_support_email_field)) },
                                placeholder = { Text(stringResource(R.string.settings_legal_support_email_placeholder)) },
                                singleLine = true,
                            )

                            OutlinedTextField(
                                value = legalLastUpdatedLabelDraft,
                                onValueChange = { legalLastUpdatedLabelDraft = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                label = { Text(stringResource(R.string.settings_legal_last_updated)) },
                                placeholder = { Text(stringResource(R.string.settings_legal_last_updated_placeholder)) },
                                singleLine = true,
                            )

                            OutlinedTextField(
                                value = legalImprintReferenceDraft,
                                onValueChange = { legalImprintReferenceDraft = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                label = { Text(stringResource(R.string.settings_legal_imprint_note)) },
                                minLines = 3,
                            )

                            OutlinedTextField(
                                value = legalMasterNumberMeaningDraft,
                                onValueChange = { legalMasterNumberMeaningDraft = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                label = { Text(stringResource(R.string.settings_legal_master22_meaning)) },
                                minLines = 3,
                            )

                            OutlinedTextField(
                                value = legalBrandManifestoDraft,
                                onValueChange = { legalBrandManifestoDraft = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                label = { Text(stringResource(R.string.settings_legal_manifesto)) },
                                minLines = 6,
                            )

                            OutlinedTextField(
                                value = legalSymbolicNumericCodeDraft,
                                onValueChange = { legalSymbolicNumericCodeDraft = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                label = { Text(stringResource(R.string.settings_legal_symbol_code)) },
                                placeholder = { Text(stringResource(R.string.settings_legal_symbol_code_placeholder)) },
                                singleLine = true,
                            )

                            OutlinedTextField(
                                value = legalSymbolicLeetCodeDraft,
                                onValueChange = { legalSymbolicLeetCodeDraft = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                label = { Text(stringResource(R.string.settings_legal_leet_code)) },
                                placeholder = { Text(stringResource(R.string.settings_legal_leet_code_placeholder)) },
                                singleLine = true,
                            )

                            OutlinedTextField(
                                value = legalSymbolicCodeExplanationDraft,
                                onValueChange = { legalSymbolicCodeExplanationDraft = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                label = { Text(stringResource(R.string.settings_legal_code_explanation)) },
                                minLines = 4,
                            )

                            Button(
                                onClick = {
                                    viewModel.saveLegalContentSettings(
                                        LegalContentSettings(
                                            brandName = legalBrandNameDraft,
                                            operatorName = legalOperatorNameDraft,
                                            rightsHolderName = legalRightsHolderNameDraft,
                                            supportEmail = legalSupportEmailDraft,
                                            lastUpdatedLabel = legalLastUpdatedLabelDraft,
                                            imprintReference = legalImprintReferenceDraft,
                                            masterNumberMeaning = legalMasterNumberMeaningDraft,
                                            brandManifesto = legalBrandManifestoDraft,
                                            symbolicNumericCode = legalSymbolicNumericCodeDraft,
                                            symbolicLeetCode = legalSymbolicLeetCodeDraft,
                                            symbolicCodeExplanation = legalSymbolicCodeExplanationDraft,
                                        ),
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
                            ) {
                                Text(stringResource(R.string.settings_legal_save))
                            }
                        }
                    }
                }
                }
            }

            ToastHost(
                message = if (showAdminWorkspaceSheet) null else feedbackMessage,
                type = feedbackType,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
            )
        }
    }

    if (showAdminWorkspaceSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAdminWorkspaceSheet = false },
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .padding(bottom = if (!feedbackMessage.isNullOrBlank()) 84.dp else 0.dp),
                    verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingRelaxed),
                ) {
                    Text(
                        text = stringResource(activeAdminWorkspace.titleRes),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    adminWorkspaceContent(activeAdminWorkspace)
                }

                ToastHost(
                    message = feedbackMessage,
                    type = feedbackType,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp),
                )
            }
        }
    }

    activeLegalDocument.value?.let { documentType ->
        SettingsLegalDocumentSheet(
            documentType = documentType,
            legalContent = uiState.legalContentSettings,
            onDismiss = { activeLegalDocument.value = null },
        )
    }

    if (showDeleteAccountDialog.value) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog.value = false },
            title = {
                Text(stringResource(R.string.settings_account_delete))
            },
            text = {
                Text(stringResource(R.string.settings_account_delete_confirm))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteAccountDialog.value = false
                        viewModel.deleteAccount()
                    },
                    enabled = !uiState.isDeletingAccount,
                ) {
                    Text(stringResource(R.string.settings_account_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteAccountDialog.value = false },
                ) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

private enum class PaymentProviderKind {
    Stripe,
    PayPal,
    Klarna,
}

@Composable
private fun PaymentProviderAdminCard(
    title: String,
    providerKind: PaymentProviderKind,
    connected: Boolean,
    enabledInCheckout: Boolean,
    accountHint: String,
    accountHintLabel: String,
    accountHintPlaceholder: String,
    onAccountHintChange: (String) -> Unit,
    onSaveConnection: () -> Unit,
    onDisconnect: (() -> Unit)?,
    onToggleEnabled: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    SkydownCard(
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingNano),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = when (providerKind) {
                        PaymentProviderKind.PayPal -> if (connected) {
                            stringResource(R.string.settings_payments_status_paypal_saved)
                        } else {
                            stringResource(R.string.settings_payments_status_paypal_missing)
                        }
                        else -> if (connected) {
                            stringResource(R.string.settings_payments_status_connected)
                        } else {
                            stringResource(R.string.settings_payments_status_disconnected)
                        }
                    },
                    color = if (connected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    },
                )
            }
            SettingsBadge(
                text = if (enabledInCheckout) {
                    stringResource(R.string.settings_payments_checkout_visible_badge)
                } else {
                    stringResource(R.string.settings_payments_checkout_hidden_badge)
                },
                icon = Icons.Default.CheckCircle,
                isActive = enabledInCheckout,
            )
        }

        OutlinedTextField(
            value = accountHint,
            onValueChange = onAccountHintChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            label = { Text(accountHintLabel) },
            placeholder = { Text(accountHintPlaceholder) },
            singleLine = true,
        )

        SettingsToggleRow(
            title = stringResource(R.string.settings_payments_show_in_checkout),
            body = stringResource(R.string.settings_payments_show_in_checkout_stripe_body),
            checked = enabledInCheckout,
            onCheckedChange = onToggleEnabled,
            modifier = Modifier.padding(top = 12.dp),
            enabled = connected,
        )

        Row(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill),
        ) {
            Button(
                onClick = onSaveConnection,
                shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
            ) {
                Text(
                    when (providerKind) {
                        PaymentProviderKind.PayPal -> if (connected) {
                            stringResource(R.string.settings_payments_action_update_paypal)
                        } else {
                            stringResource(R.string.settings_payments_action_save_paypal)
                        }
                        else -> if (connected) {
                            stringResource(R.string.settings_payments_action_update_connection)
                        } else {
                            stringResource(R.string.settings_payments_action_connect)
                        }
                    },
                )
            }
            onDisconnect?.let { disconnect ->
                OutlinedButton(
                    onClick = disconnect,
                    shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
                ) {
                    Text(
                        if (providerKind == PaymentProviderKind.PayPal) {
                            stringResource(R.string.settings_payments_action_remove_paypal)
                        } else {
                            stringResource(R.string.settings_payments_action_disconnect)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun StripeBackendSecretsAdminCard(
    status: com.nash.skyos.data.StripeBackendSecretsStatus,
    stripeSecretKey: String,
    stripeWebhookSecret: String,
    onStripeSecretKeyChange: (String) -> Unit,
    onStripeWebhookSecretChange: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SkydownCard(
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingNano),
            ) {
                Text(
                    text = stringResource(R.string.settings_payments_stripe_heading),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (status.isReady) {
                        stringResource(R.string.settings_payments_stripe_backend_status_ready)
                    } else {
                        stringResource(R.string.settings_payments_stripe_backend_status_incomplete)
                    },
                    color = if (status.isReady) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    },
                )
            }
            SettingsBadge(
                text = if (status.isReady) {
                    stringResource(R.string.settings_payments_stripe_backend_badge_ready)
                } else {
                    stringResource(R.string.settings_payments_stripe_backend_badge_incomplete)
                },
                icon = Icons.Default.CheckCircle,
                isActive = status.isReady,
            )
        }

        Row(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
        ) {
            SettingsBadge(
                text = if (status.hasSecretKey) {
                    stringResource(R.string.settings_payments_stripe_secret_configured)
                } else {
                    stringResource(R.string.settings_payments_stripe_secret_missing)
                },
                icon = Icons.Default.CheckCircle,
                isActive = status.hasSecretKey,
            )
            SettingsBadge(
                text = if (status.hasWebhookSecret) {
                    stringResource(R.string.settings_payments_stripe_webhook_configured)
                } else {
                    stringResource(R.string.settings_payments_stripe_webhook_missing)
                },
                icon = Icons.Default.CheckCircle,
                isActive = status.hasWebhookSecret,
            )
        }

        Text(
            text = stringResource(R.string.settings_payments_stripe_secret_hint),
            modifier = Modifier.padding(top = 12.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
        )

        OutlinedTextField(
            value = stripeSecretKey,
            onValueChange = onStripeSecretKeyChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            label = { Text(stringResource(R.string.settings_payments_stripe_secret)) },
            placeholder = { Text(stringResource(R.string.settings_payments_stripe_secret_placeholder)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
        )

        OutlinedTextField(
            value = stripeWebhookSecret,
            onValueChange = onStripeWebhookSecretChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            label = { Text(stringResource(R.string.settings_payments_stripe_webhook)) },
            placeholder = { Text(stringResource(R.string.settings_payments_stripe_webhook_placeholder)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
        )

        Button(
            onClick = onSave,
            modifier = Modifier.padding(top = 12.dp),
            shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
        ) {
            Text(stringResource(R.string.settings_payments_save_securely))
        }
    }
}

@Composable
private fun BankTransferAdminCard(
    configured: Boolean,
    enabledInCheckout: Boolean,
    accountHolder: String,
    iban: String,
    bic: String,
    bankName: String,
    paymentInstructions: String,
    onAccountHolderChange: (String) -> Unit,
    onIbanChange: (String) -> Unit,
    onBicChange: (String) -> Unit,
    onBankNameChange: (String) -> Unit,
    onPaymentInstructionsChange: (String) -> Unit,
    onSave: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    SkydownCard(
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingNano),
            ) {
                Text(
                    text = stringResource(R.string.settings_payments_bank_transfer_heading),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (configured) {
                        stringResource(R.string.settings_payments_bank_details_saved)
                    } else {
                        stringResource(R.string.settings_payments_bank_details_missing)
                    },
                    color = if (configured) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    },
                )
            }
            SettingsBadge(
                text = if (enabledInCheckout) {
                    stringResource(R.string.settings_payments_checkout_visible_badge)
                } else {
                    stringResource(R.string.settings_payments_checkout_hidden_badge)
                },
                icon = Icons.Default.CheckCircle,
                isActive = enabledInCheckout,
            )
        }

        OutlinedTextField(
            value = accountHolder,
            onValueChange = onAccountHolderChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            label = { Text(stringResource(R.string.settings_payments_account_holder)) },
            singleLine = true,
        )
        OutlinedTextField(
            value = iban,
            onValueChange = onIbanChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            label = { Text(stringResource(R.string.settings_payments_iban)) },
            singleLine = true,
        )
        OutlinedTextField(
            value = bic,
            onValueChange = onBicChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            label = { Text(stringResource(R.string.settings_payments_bic)) },
            singleLine = true,
        )
        OutlinedTextField(
            value = bankName,
            onValueChange = onBankNameChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            label = { Text(stringResource(R.string.settings_payments_bank_name)) },
            singleLine = true,
        )
        OutlinedTextField(
            value = paymentInstructions,
            onValueChange = onPaymentInstructionsChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            label = { Text(stringResource(R.string.settings_payments_payment_note)) },
            minLines = 2,
            maxLines = 3,
        )

        SettingsToggleRow(
            title = stringResource(R.string.settings_payments_show_in_checkout),
            body = stringResource(R.string.settings_payments_show_in_checkout_bank_body),
            checked = enabledInCheckout,
            onCheckedChange = onToggleEnabled,
            modifier = Modifier.padding(top = 12.dp),
            enabled = configured,
        )

        Button(
            onClick = onSave,
            modifier = Modifier.padding(top = 12.dp),
            shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
        ) {
            Text(
                if (configured) {
                    stringResource(R.string.settings_payments_bank_action_update)
                } else {
                    stringResource(R.string.settings_payments_bank_action_save)
                },
            )
        }
    }
}

@Composable
private fun ProfileEditorCard(
    username: String,
    whatsApp: String,
    profileTagline: String,
    profileBio: String,
    instagramHandle: String,
    isSaving: Boolean,
    onUsernameChange: (String) -> Unit,
    onWhatsAppChange: (String) -> Unit,
    onProfileTaglineChange: (String) -> Unit,
    onProfileBioChange: (String) -> Unit,
    onInstagramHandleChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingCompact)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingCompact),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(SkydownUiTokens.compactRadius))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingNano)) {
                Text(
                    text = stringResource(R.string.settings_profile_inline_heading),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.settings_profile_inline_subtitle),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
            }
        }

        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.settings_profile_username)) },
            singleLine = true,
        )
        OutlinedTextField(
            value = profileTagline,
            onValueChange = onProfileTaglineChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.settings_profile_tagline)) },
            singleLine = true,
        )
        OutlinedTextField(
            value = profileBio,
            onValueChange = onProfileBioChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.settings_profile_bio)) },
            minLines = 3,
            maxLines = 5,
        )
        OutlinedTextField(
            value = instagramHandle,
            onValueChange = onInstagramHandleChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.settings_profile_instagram)) },
            singleLine = true,
        )
        OutlinedTextField(
            value = whatsApp,
            onValueChange = onWhatsAppChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.settings_profile_whatsapp)) },
            singleLine = true,
        )

        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving,
            shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
        ) {
            Text(if (isSaving) "Profil wird gespeichert..." else "Profil speichern")
        }
    }
}

@Composable
private fun SettingsUtilityRow(
    isOwner: Boolean,
    onOpenPayments: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenOrders: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
    ) {
        SettingsUtilityChip(
            label = if (isOwner) stringResource(R.string.settings_utility_payments) else stringResource(R.string.settings_utility_support),
            icon = if (isOwner) Icons.Default.CreditCard else Icons.Default.Email,
            accent = MaterialTheme.colorScheme.primary,
            onClick = onOpenPayments,
            modifier = Modifier.weight(1f),
        )
        SettingsUtilityChip(
            label = stringResource(R.string.settings_utility_privacy),
            icon = Icons.Default.Lock,
            accent = MaterialTheme.colorScheme.secondary,
            onClick = onOpenPrivacy,
            modifier = Modifier.weight(1f),
        )
        SettingsUtilityChip(
            label = "Orders",
            icon = Icons.Default.LocalShipping,
            accent = MaterialTheme.colorScheme.tertiary,
            onClick = onOpenOrders,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SettingsUtilityChip(
    label: String,
    icon: ImageVector,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(SkydownUiTokens.fullCapsuleRadius)
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.24f)),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = accent.copy(alpha = 0.12f),
            contentColor = accent,
        ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(13.dp),
        )
        Spacer(modifier = Modifier.size(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
        )
    }
}

@Composable
private fun OwnerCommandCenterCard(
    isOwner: Boolean,
    paymentStatus: String,
    userStatus: String,
    headerStatus: String,
    aiStatus: String,
    onOpenUsers: () -> Unit,
    onOpenPayments: () -> Unit,
    onOpenHeaders: () -> Unit,
    onOpenAi: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SkydownUiTokens.elevatedPanelRadius))
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
                    ),
                ),
            )
            .padding(SkydownUiTokens.cardPadding),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingRelaxed)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingNano),
                ) {
                    Text(
                        text = if (isOwner) {
                            stringResource(R.string.settings_owner_command_title)
                        } else {
                            stringResource(R.string.settings_owner_command_title_locked)
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = if (isOwner) {
                            stringResource(R.string.settings_owner_command_subtitle)
                        } else {
                            stringResource(R.string.settings_owner_command_subtitle_locked)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }

                SettingsBadge(
                    text = if (isOwner) {
                        stringResource(R.string.settings_owner_command_badge_active)
                    } else {
                        stringResource(R.string.settings_owner_command_badge_locked)
                    },
                    icon = if (isOwner) Icons.Default.CheckCircle else Icons.Default.Lock,
                    isActive = isOwner,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OwnerCommandSignalButton(
                    title = stringResource(R.string.settings_owner_signal_roles),
                    detail = userStatus,
                    icon = Icons.Default.Person,
                    enabled = isOwner,
                    onClick = onOpenUsers,
                    modifier = Modifier.weight(1f),
                )
                OwnerCommandSignalButton(
                    title = stringResource(R.string.settings_owner_signal_payments),
                    detail = paymentStatus,
                    icon = Icons.Default.CreditCard,
                    enabled = isOwner,
                    onClick = onOpenPayments,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OwnerCommandSignalButton(
                    title = stringResource(R.string.settings_owner_signal_headers),
                    detail = headerStatus,
                    icon = Icons.Default.Palette,
                    enabled = isOwner,
                    onClick = onOpenHeaders,
                    modifier = Modifier.weight(1f),
                )
                OwnerCommandSignalButton(
                    title = stringResource(R.string.settings_owner_signal_ai_safety),
                    detail = aiStatus,
                    icon = Icons.Default.Bolt,
                    enabled = isOwner,
                    onClick = onOpenAi,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun OwnerCommandSignalButton(
    title: String,
    detail: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(SkydownUiTokens.messageBubbleRadius))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = if (enabled) 0.9f else 0.46f))
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingSnug),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 0.62f else 0.24f),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsLegalDocumentSheet(
    documentType: SettingsLegalDocumentType,
    legalContent: LegalContentSettings,
    onDismiss: () -> Unit,
) {
    val document = documentType.resolve(legalContent)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        LazyColumn(
            contentPadding = PaddingValues(
                start = 20.dp,
                top = com.nash.skyos.ui.component.SkydownUiTokens.screenTopPadding,
                end = 20.dp,
                bottom = 36.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingRelaxed),
        ) {
            item {
                Text(
                    text = stringResource(R.string.legal_ui_transparency_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro)) {
                    Text(
                        text = document.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.legal_ui_last_updated, document.updatedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                    )
                    Text(
                        text = document.introduction,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
                    )
                }
            }

            items(document.sections) { section ->
                SkydownCard {
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = section.body,
                        modifier = Modifier.padding(top = 10.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
                    )
                }
            }

            item {
                SkydownCard {
                    Text(
                        text = stringResource(R.string.legal_ui_contact),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = document.contactEmail,
                        modifier = Modifier.padding(top = 10.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    body: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SkydownUiTokens.messageBubbleRadius))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingNano),
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}

@Composable
private fun AppearanceChoiceRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SkydownUiTokens.messageBubbleRadius))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
            )
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
        RadioButton(
            selected = selected,
            onClick = onClick,
        )
    }
}

@Composable
private fun SettingsBadge(
    text: String,
    icon: ImageVector,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val backgroundColor = if (isActive) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)
    }
    val contentColor = if (isActive) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }
    val interactionSource = remember { MutableInteractionSource() }
    val badgeModifier = modifier
        .skydownPressable(
            interactionSource = interactionSource,
            pressedScale = 0.992f,
        )
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            role = Role.Button,
            onClick = onClick,
        )

    Row(
        modifier = badgeModifier
            .clip(RoundedCornerShape(SkydownUiTokens.fullCapsuleRadius))
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = contentColor.copy(alpha = 0.82f),
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
private fun SettingsLockedHint(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SkydownUiTokens.compactRadius))
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.68f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

private enum class AdminWorkspaceSection(
    @param:StringRes val titleRes: Int,
    @param:StringRes val subtitleRes: Int,
    val icon: ImageVector,
) {
    Payments(
        titleRes = R.string.settings_admin_ws_payments_title,
        subtitleRes = R.string.settings_admin_ws_payments_subtitle,
        icon = Icons.Default.CreditCard,
    ),
    Users(
        titleRes = R.string.settings_admin_ws_users_title,
        subtitleRes = R.string.settings_admin_ws_users_subtitle,
        icon = Icons.Default.Person,
    ),
    Artists(
        titleRes = R.string.settings_admin_ws_artists_title,
        subtitleRes = R.string.settings_admin_ws_artists_subtitle,
        icon = Icons.Default.LibraryMusic,
    ),
    Headers(
        titleRes = R.string.settings_admin_ws_headers_title,
        subtitleRes = R.string.settings_admin_ws_headers_subtitle,
        icon = Icons.Default.Palette,
    ),
    Shopify(
        titleRes = R.string.settings_admin_ws_shopify_title,
        subtitleRes = R.string.settings_admin_ws_shopify_subtitle,
        icon = Icons.Default.ShoppingBag,
    ),
    Commerce(
        titleRes = R.string.settings_admin_ws_commerce_title,
        subtitleRes = R.string.settings_admin_ws_commerce_subtitle,
        icon = Icons.Default.LocalShipping,
    ),
    Visuals(
        titleRes = R.string.settings_admin_ws_visuals_title,
        subtitleRes = R.string.settings_admin_ws_visuals_subtitle,
        icon = Icons.Default.Palette,
    ),
    Automation(
        titleRes = R.string.settings_admin_ws_automation_title,
        subtitleRes = R.string.settings_admin_ws_automation_subtitle,
        icon = Icons.Default.Bolt,
    ),
    AiPrompts(
        titleRes = R.string.settings_admin_ws_ai_prompts_title,
        subtitleRes = R.string.settings_admin_ws_ai_prompts_subtitle,
        icon = Icons.Default.Settings,
    ),
    MembershipOps(
        titleRes = R.string.settings_admin_ws_membership_title,
        subtitleRes = R.string.settings_admin_ws_membership_subtitle,
        icon = Icons.Default.Bolt,
    ),

    ;

    companion object {
        fun fromSavedKey(key: String): AdminWorkspaceSection {
            return when (key) {
                "Overview" -> Users
                else -> entries.firstOrNull { it.name == key } ?: Users
            }
        }
    }
}

private fun adminWorkspaceStatusText(
    section: AdminWorkspaceSection,
    uiState: SettingsUiState,
    visiblePaymentMethodCount: Int,
    publishedArtistPageCount: Int,
    configuredScreenHeaderCount: Int,
): String {
    return when (section) {
        AdminWorkspaceSection.Payments -> "$visiblePaymentMethodCount live im Checkout"
        AdminWorkspaceSection.Users -> "${uiState.managedUsers.size} Konten"
        AdminWorkspaceSection.Artists -> "${publishedArtistPageCount} Artist-Seiten"
        AdminWorkspaceSection.Headers -> "$configuredScreenHeaderCount Header live"
        AdminWorkspaceSection.Shopify -> uiState.shopifyAdminSettings.activeCollectionLabel
        AdminWorkspaceSection.Commerce -> uiState.commerceSettings.invoice.supportEmail.ifBlank { "Versand & Rechnung" }
        AdminWorkspaceSection.Visuals -> if (uiState.aiVisualReferenceLibrary.isEnabled) "Visuals aktiv" else "Visuals aus"
        AdminWorkspaceSection.Automation -> when {
            uiState.workflowAutomationSettings.isPrepared &&
                uiState.agentProfileSettings.isConfigured &&
                uiState.manusByosSettings.isEnabled &&
                uiState.manusByosSettings.hasApiKey -> "Automation + Skills + Manus"
            uiState.workflowAutomationSettings.isPrepared && uiState.agentProfileSettings.isConfigured -> "Automation + Skills bereit"
            uiState.workflowAutomationSettings.isPrepared && uiState.manusByosSettings.hasApiKey -> "Automation + Manus bereit"
            uiState.workflowAutomationSettings.isPrepared -> "Automation bereit"
            uiState.agentProfileSettings.isConfigured && uiState.manusByosSettings.hasApiKey -> "Skills + Manus bereit"
            uiState.agentProfileSettings.isConfigured -> "Skills bereit"
            uiState.manusByosSettings.hasApiKey -> "Manus bereit"
            else -> "Noch offen"
        }
        AdminWorkspaceSection.AiPrompts -> if (uiState.aiPromptSettings.assetLibraryLink.isBlank()) {
            "Text ${uiState.aiPromptSettings.textInstruction.length}"
        } else {
            "Assets + Prompts"
        }
        AdminWorkspaceSection.MembershipOps -> "Revenue aktiv"
    }
}

private enum class SettingsHeaderImageTarget {
    Home,
    MusicHub,
    Shop,
    VideoHub,
}

private data class MembershipOpsDataBundle(
    val dashboard: Map<String, Any?>,
    val timeseries: Map<String, Any?>,
    val recommendations: List<MembershipOpsRecommendation>,
    val learnings: Map<String, Any?>,
    val simulations: Map<String, Any?>,
    val timeline: Map<String, Any?>,
    val hygiene: Map<String, Any?>,
)

private enum class MembershipOpsTab(val label: String) {
    Dashboard("Dashboard"),
    Recommendations("Recommendations"),
    Experiments("Experiments"),
    Learnings("Learnings"),
    Timeline("Timeline"),
    ;

    companion object {
        fun fromKey(raw: String): MembershipOpsTab {
            return entries.firstOrNull { it.name == raw } ?: Dashboard
        }
    }
}

@Composable
private fun AdminWorkspaceListRow(
    section: AdminWorkspaceSection,
    detailText: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(SkydownUiTokens.cardCornerRadius),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingCompact),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(SkydownUiTokens.denseRadius))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = section.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingNano),
            ) {
                Text(
                    text = stringResource(section.titleRes),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(section.subtitleRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingDense),
            ) {
                Text(
                    text = detailText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                Text(
                    text = stringResource(R.string.common_open),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
private fun AdminWorkspaceChip(
    section: AdminWorkspaceSection,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(SkydownUiTokens.fullCapsuleRadius),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surface
            },
            contentColor = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        ),
    ) {
        Icon(
            imageVector = section.icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = stringResource(section.titleRes),
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun AdminWorkspaceRailButton(
    section: AdminWorkspaceSection,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .skydownPressable(interactionSource, pressedScale = 0.982f),
        shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
        interactionSource = interactionSource,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surface
            },
            contentColor = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = section.icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(section.titleRes),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun AdminWorkspaceSummaryCard(
    section: AdminWorkspaceSection,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SkydownUiTokens.messageBubbleRadius))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingCompact),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = section.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingDense)) {
            Text(
                text = stringResource(section.titleRes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(section.subtitleRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
        }
    }
}

@Composable
private fun AdminUserRoleGuideCard(
    modifier: Modifier = Modifier,
) {
    SkydownCard(
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.settings_admin_roles_guide_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Column(
            modifier = Modifier.padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill),
        ) {
            UserRole.entries.forEach { role ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingNano),
                ) {
                    Text(
                        text = role.displayTitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = role.roleSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistPageAdminCard(
    page: ArtistPageUi,
    users: List<User>,
    onSave: (ArtistPageUi) -> Unit,
) {
    var selectedEditorUids by rememberSaveable(page.slug, page.editorUids) {
        mutableStateOf(
            if (page.brand == ArtistPageBrand.Nicma) {
                page.editorUids.take(1).toSet()
            } else {
                page.editorUids.toSet()
            },
        )
    }

    SkydownCard {
        Column(verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingRelaxed)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingCompact),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingNano),
                ) {
                    Text(
                        text = page.artistName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = if (page.hasCustomPresentation) {
                            "Seite hat schon Inhalt."
                        } else {
                            "Noch als Platzhalter. Nach dem ersten Speichern ist die Artist-Seite live."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingDense),
                ) {
                    SettingsBadge(
                        text = if (page.hasCustomPresentation) "Live" else "Platzhalter",
                        icon = Icons.Default.LibraryMusic,
                        isActive = page.hasCustomPresentation,
                    )
                    SettingsBadge(
                        text = if (page.brand == ArtistPageBrand.Nicma) {
                            if (selectedEditorUids.isEmpty()) "Kein Editor" else "1 Editor"
                        } else {
                            "${selectedEditorUids.size} Editoren"
                        },
                        icon = Icons.Default.Person,
                        isActive = selectedEditorUids.isNotEmpty(),
                    )
                }
            }

            if (users.isEmpty()) {
                Text(
                    text = stringResource(R.string.settings_admin_artists_editors_hint),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                Text(
                    text = if (page.brand == ArtistPageBrand.Nicma) "Editor" else "Editoren",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )

                Column(verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro)) {
                    users.forEach { user ->
                        val userId = user.id.orEmpty()
                        val isSelected = selectedEditorUids.contains(userId)

                        Button(
                            onClick = {
                                selectedEditorUids = if (page.brand == ArtistPageBrand.Nicma) {
                                    if (isSelected) emptySet() else setOf(userId)
                                } else {
                                    if (isSelected) selectedEditorUids - userId else selectedEditorUids + userId
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(SkydownUiTokens.denseRadius),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                contentColor = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            ),
                        ) {
                            Text(
                                text = user.username,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = user.resolvedRole.displayTitle,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    onSave(
                        page.copy(
                            editorUids = if (page.brand == ArtistPageBrand.Nicma) {
                                selectedEditorUids.take(1)
                            } else {
                                selectedEditorUids.toList().sorted()
                            },
                            updatedAtEpochMillis = System.currentTimeMillis(),
                            isPlaceholder = false,
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
            ) {
                Text(stringResource(R.string.settings_save_editors))
            }
        }
    }
}

@Composable
private fun AdminManagedUserCard(
    user: User,
    currentUserId: String?,
    onSave: suspend (User) -> Result<String>,
    modifier: Modifier = Modifier,
) {
    val isCurrentUser = user.id == currentUserId
    val coroutineScope = rememberCoroutineScope()
    var selectedRole by rememberSaveable(user.id, user.role) {
        mutableStateOf(user.resolvedRole.rawValue)
    }
    var selectedQuotaPlan by rememberSaveable(user.id, user.quotaPlan) {
        mutableStateOf(user.resolvedQuotaPlan.rawValue)
    }
    var aiAccessEnabled by rememberSaveable(user.id, user.aiAccessEnabled) {
        mutableStateOf(user.aiAccessEnabled)
    }
    var textLimitDraft by rememberSaveable(user.id, user.aiTextRequestsPerDay) {
        mutableStateOf(user.resolvedAiTextRequestsPerDay.toString())
    }
    var visualLimitDraft by rememberSaveable(user.id, user.aiVisualRequestsPerDay) {
        mutableStateOf(user.resolvedAiVisualRequestsPerDay.toString())
    }
    var agentLimitDraft by rememberSaveable(user.id, user.aiAgentRequestsPerDay) {
        mutableStateOf(user.resolvedAiAgentRequestsPerDay.toString())
    }
    var historyRetentionDays by rememberSaveable(user.id, user.aiHistoryRetentionDays) {
        mutableIntStateOf(user.resolvedAiHistoryRetentionDays)
    }
    var canManageMusicCatalog by rememberSaveable(user.id, user.canManageMusicCatalog) {
        mutableStateOf(user.canManageMusicCatalog)
    }
    var canManageVideoCatalog by rememberSaveable(user.id, user.canManageVideoCatalog) {
        mutableStateOf(user.canManageVideoCatalog)
    }
    var canModerateProfiles by rememberSaveable(user.id, user.canModerateProfiles) {
        mutableStateOf(user.canModerateProfiles)
    }
    var isSaving by rememberSaveable(user.id) {
        mutableStateOf(false)
    }
    var saveErrorMessage by remember(user.id) {
        mutableStateOf<String?>(null)
    }
    var successfulSaveCount by rememberSaveable(user.id) {
        mutableIntStateOf(0)
    }
    val resolvedRole = UserRole.resolve(selectedRole, user.isAdmin, user.email)
    val resolvedQuotaPlan = UserQuotaPlan.resolve(selectedQuotaPlan, resolvedRole)
    val canAssignOwnerRoleToUser = user.email.trim().lowercase() == UserRole.OWNER_EMAIL
    val draftUser = remember(
        user,
        resolvedRole,
        resolvedQuotaPlan,
        aiAccessEnabled,
        textLimitDraft,
        visualLimitDraft,
        agentLimitDraft,
        historyRetentionDays,
        canManageMusicCatalog,
        canManageVideoCatalog,
        canModerateProfiles,
    ) {
        val finalQuotaPlan = when (resolvedRole) {
            UserRole.Owner -> UserQuotaPlan.OwnerUnlimited
            UserRole.Admin -> UserQuotaPlan.InternalTeam
            UserRole.Subadmin -> if (resolvedQuotaPlan == UserQuotaPlan.Studio) {
                UserQuotaPlan.Studio
            } else {
                UserQuotaPlan.Creator
            }
            UserRole.User -> UserQuotaPlan.Free
        }

        user.copy(
            isAdmin = resolvedRole.hasStaffAccess,
            role = resolvedRole.rawValue,
            quotaPlan = finalQuotaPlan.rawValue,
            aiAccessEnabled = aiAccessEnabled,
            aiTextRequestsPerDay = textLimitDraft.parsePositiveIntOrDefault(
                finalQuotaPlan.aiTextRequestsPerDay,
            ),
            aiVisualRequestsPerDay = visualLimitDraft.parsePositiveIntOrDefault(
                finalQuotaPlan.aiVisualRequestsPerDay,
            ),
            aiAgentRequestsPerDay = agentLimitDraft.parsePositiveIntOrDefault(
                finalQuotaPlan.aiAgentRequestsPerDay,
            ),
            aiHistoryRetentionDays = historyRetentionDays,
            canManageMusicCatalog = when (resolvedRole) {
                UserRole.Owner -> true
                UserRole.Admin -> canManageMusicCatalog
                else -> false
            },
            canManageVideoCatalog = when (resolvedRole) {
                UserRole.Owner -> true
                UserRole.Admin -> canManageVideoCatalog
                else -> false
            },
            canModerateProfiles = when (resolvedRole) {
                UserRole.Owner -> true
                UserRole.Admin -> canModerateProfiles
                else -> false
            },
        )
    }
    val hasPendingChanges = draftUser != user

    SkydownCard(
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingNano),
            ) {
                Text(
                    text = user.username,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingDense),
            ) {
                SettingsBadge(
                    text = resolvedRole.displayTitle,
                    icon = Icons.Default.Person,
                    isActive = true,
                )
                if (isCurrentUser) {
                    SettingsBadge(
                        text = stringResource(R.string.settings_managed_user_you),
                        icon = Icons.Default.CheckCircle,
                        isActive = true,
                    )
                }
                if (isSaving) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                        Text(
                            text = stringResource(R.string.settings_managed_user_saving),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        )
                    }
                } else if (hasPendingChanges) {
                    SettingsBadge(
                        text = stringResource(R.string.settings_managed_user_draft),
                        icon = Icons.Default.Bolt,
                        isActive = false,
                    )
                } else if (successfulSaveCount > 0) {
                    SettingsBadge(
                        text = stringResource(R.string.settings_managed_user_saved),
                        icon = Icons.Default.CheckCircle,
                        isActive = true,
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.settings_managed_user_role_label),
            modifier = Modifier.padding(top = 14.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )

        LazyRow(
            modifier = Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
        ) {
            items(UserRole.entries.toList()) { role ->
                val selected = resolvedRole == role
                val roleSelectionEnabled = !isCurrentUser &&
                    !user.isPlatformOwner &&
                    (role != UserRole.Owner || canAssignOwnerRoleToUser)
                if (selected) {
                    Button(
                        onClick = { selectedRole = role.rawValue },
                        shape = RoundedCornerShape(SkydownUiTokens.fullCapsuleRadius),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Text(role.displayTitle)
                    }
                } else {
                    OutlinedButton(
                        onClick = { selectedRole = role.rawValue },
                        enabled = roleSelectionEnabled,
                        shape = RoundedCornerShape(SkydownUiTokens.fullCapsuleRadius),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingDense),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (!roleSelectionEnabled) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                            Text(role.displayTitle)
                        }
                    }
                }
            }
        }

        if (user.isPlatformOwner) {
            Text(
                text = stringResource(R.string.settings_managed_user_owner_bound, "nash.lioncorna@gmail.com"),
                modifier = Modifier.padding(top = 10.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
        } else if (isCurrentUser) {
            Text(
                text = stringResource(R.string.settings_managed_user_self_protected),
                modifier = Modifier.padding(top = 10.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
        } else if (!canAssignOwnerRoleToUser) {
            Text(
                text = stringResource(R.string.settings_managed_user_owner_only),
                modifier = Modifier.padding(top = 10.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
        }

        when (resolvedRole) {
            UserRole.Owner -> {
                Text(
                    text = stringResource(R.string.settings_managed_user_owner_control_blurb),
                    modifier = Modifier.padding(top = 10.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
            }

            UserRole.Admin -> {
                Text(
                    text = stringResource(R.string.settings_managed_user_assigned_features),
                    modifier = Modifier.padding(top = 14.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )

                SettingsToggleRow(
                    title = stringResource(R.string.settings_subadmin_music_title),
                    body = stringResource(R.string.settings_subadmin_music_body),
                    checked = canManageMusicCatalog,
                    onCheckedChange = { canManageMusicCatalog = it },
                    modifier = Modifier.padding(top = 10.dp),
                )
                SettingsToggleRow(
                    title = stringResource(R.string.settings_subadmin_video_title),
                    body = stringResource(R.string.settings_subadmin_video_body),
                    checked = canManageVideoCatalog,
                    onCheckedChange = { canManageVideoCatalog = it },
                    modifier = Modifier.padding(top = 10.dp),
                )
                SettingsToggleRow(
                    title = stringResource(R.string.settings_subadmin_profile_title),
                    body = stringResource(R.string.settings_subadmin_profile_body),
                    checked = canModerateProfiles,
                    onCheckedChange = { canModerateProfiles = it },
                    modifier = Modifier.padding(top = 10.dp),
                )
            }

            UserRole.Subadmin, UserRole.User -> {
                Text(
                    text = if (resolvedRole == UserRole.Subadmin) {
                        stringResource(R.string.settings_managed_user_quota_heading_subadmin)
                    } else {
                        stringResource(R.string.settings_managed_user_quota_heading_user)
                    },
                    modifier = Modifier.padding(top = 14.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )

                if (resolvedRole == UserRole.User) {
                    Text(
                        text = UserQuotaPlan.Free.planSummary,
                        modifier = Modifier.padding(top = 10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    )
                } else {
                    LazyRow(
                        modifier = Modifier.padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
                    ) {
                        items(listOf(UserQuotaPlan.Creator, UserQuotaPlan.Studio)) { plan ->
                            val isSelected = resolvedQuotaPlan == plan
                            if (isSelected) {
                                Button(
                                    onClick = { selectedQuotaPlan = plan.rawValue },
                                    shape = RoundedCornerShape(SkydownUiTokens.fullCapsuleRadius),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                ) {
                                    Text(plan.displayTitle)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { selectedQuotaPlan = plan.rawValue },
                                    shape = RoundedCornerShape(SkydownUiTokens.fullCapsuleRadius),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                ) {
                                    Text(plan.displayTitle)
                                }
                            }
                        }
                    }

                    Text(
                        text = resolvedQuotaPlan.planSummary,
                        modifier = Modifier.padding(top = 10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    )
                }
            }
        }

        SettingsToggleRow(
            title = stringResource(R.string.settings_ai_account_toggle_title),
            body = stringResource(R.string.settings_ai_account_toggle_body),
            checked = aiAccessEnabled,
            onCheckedChange = { aiAccessEnabled = it },
            modifier = Modifier.padding(top = 14.dp),
        )

        Text(
            text = stringResource(R.string.settings_managed_user_daily_limits),
            modifier = Modifier.padding(top = 14.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )

        OutlinedTextField(
            value = textLimitDraft,
            onValueChange = { textLimitDraft = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            label = { Text(stringResource(R.string.settings_admin_limit_bot)) },
            singleLine = true,
        )
        OutlinedTextField(
            value = visualLimitDraft,
            onValueChange = { visualLimitDraft = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            label = { Text(stringResource(R.string.settings_admin_limit_visuals)) },
            singleLine = true,
        )
        OutlinedTextField(
            value = agentLimitDraft,
            onValueChange = { agentLimitDraft = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            label = { Text(stringResource(R.string.settings_admin_limit_agent)) },
            singleLine = true,
        )

        Text(
            text = stringResource(R.string.settings_managed_user_history_retention),
            modifier = Modifier.padding(top = 14.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )

        LazyRow(
            modifier = Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
        ) {
            items(listOf(1, 3, 7, 30)) { option ->
                val isSelected = historyRetentionDays == option
                if (isSelected) {
                    Button(
                        onClick = { historyRetentionDays = option },
                        shape = RoundedCornerShape(SkydownUiTokens.fullCapsuleRadius),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Text(historyOptionLabel(option))
                    }
                } else {
                    OutlinedButton(
                        onClick = { historyRetentionDays = option },
                        shape = RoundedCornerShape(SkydownUiTokens.fullCapsuleRadius),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Text(historyOptionLabel(option))
                    }
                }
            }
        }

        Button(
            onClick = {
                if (isSaving || !hasPendingChanges) return@Button

                isSaving = true
                saveErrorMessage = null
                coroutineScope.launch {
                    val result = onSave(draftUser)
                    isSaving = false
                    if (result.isSuccess) {
                        successfulSaveCount += 1
                        saveErrorMessage = null
                    } else {
                        saveErrorMessage = result.exceptionOrNull()?.message
                            ?: "Konto konnte nicht gespeichert werden."
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
            enabled = !isSaving && hasPendingChanges,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isSaving) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                Text(if (isSaving) "Speichert..." else "Konto speichern")
            }
        }

        when {
            saveErrorMessage != null -> {
                Text(
                    text = saveErrorMessage.orEmpty(),
                    modifier = Modifier.padding(top = 10.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            isSaving -> {
                Text(
                    text = stringResource(R.string.settings_managed_user_sync_pending),
                    modifier = Modifier.padding(top = 10.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
            }

            hasPendingChanges -> {
                Text(
                    text = stringResource(R.string.settings_managed_user_unsaved),
                    modifier = Modifier.padding(top = 10.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
            }

            successfulSaveCount > 0 -> {
                Text(
                    text = stringResource(R.string.settings_managed_user_saved_confirmed),
                    modifier = Modifier.padding(top = 10.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }

    LaunchedEffect(draftUser) {
        if (draftUser != user) {
            saveErrorMessage = null
        }
    }

    LaunchedEffect(resolvedRole) {
        when (resolvedRole) {
            UserRole.Owner -> {
                selectedQuotaPlan = UserQuotaPlan.OwnerUnlimited.rawValue
                canManageMusicCatalog = true
                canManageVideoCatalog = true
                canModerateProfiles = true
                textLimitDraft = UserQuotaPlan.OwnerUnlimited.aiTextRequestsPerDay.toString()
                visualLimitDraft = UserQuotaPlan.OwnerUnlimited.aiVisualRequestsPerDay.toString()
                agentLimitDraft = UserQuotaPlan.OwnerUnlimited.aiAgentRequestsPerDay.toString()
                historyRetentionDays = UserQuotaPlan.OwnerUnlimited.aiHistoryRetentionDays
            }
            UserRole.Admin -> {
                selectedQuotaPlan = UserQuotaPlan.InternalTeam.rawValue
                canManageMusicCatalog = user.canManageMusicCatalog
                canManageVideoCatalog = user.canManageVideoCatalog
                canModerateProfiles = user.canModerateProfiles
                textLimitDraft = UserQuotaPlan.InternalTeam.aiTextRequestsPerDay.toString()
                visualLimitDraft = UserQuotaPlan.InternalTeam.aiVisualRequestsPerDay.toString()
                agentLimitDraft = UserQuotaPlan.InternalTeam.aiAgentRequestsPerDay.toString()
                historyRetentionDays = UserQuotaPlan.InternalTeam.aiHistoryRetentionDays
            }
            UserRole.Subadmin -> {
                if (resolvedQuotaPlan !in listOf(UserQuotaPlan.Creator, UserQuotaPlan.Studio)) {
                    selectedQuotaPlan = UserQuotaPlan.Creator.rawValue
                }
                canManageMusicCatalog = false
                canManageVideoCatalog = false
                canModerateProfiles = false
            }
            UserRole.User -> {
                selectedQuotaPlan = UserQuotaPlan.Free.rawValue
                canManageMusicCatalog = false
                canManageVideoCatalog = false
                canModerateProfiles = false
                textLimitDraft = UserQuotaPlan.Free.aiTextRequestsPerDay.toString()
                visualLimitDraft = UserQuotaPlan.Free.aiVisualRequestsPerDay.toString()
                agentLimitDraft = UserQuotaPlan.Free.aiAgentRequestsPerDay.toString()
                historyRetentionDays = UserQuotaPlan.Free.aiHistoryRetentionDays
            }
        }
    }

    LaunchedEffect(selectedQuotaPlan, resolvedRole) {
        if (resolvedRole == UserRole.Subadmin) {
            val plan = UserQuotaPlan.resolve(selectedQuotaPlan, resolvedRole)
            textLimitDraft = plan.aiTextRequestsPerDay.toString()
            visualLimitDraft = plan.aiVisualRequestsPerDay.toString()
            agentLimitDraft = plan.aiAgentRequestsPerDay.toString()
            historyRetentionDays = plan.aiHistoryRetentionDays
        }
    }
}

private val UserRole.displayTitle: String
    get() = when (this) {
        UserRole.Owner -> "Owner"
        UserRole.Admin -> "Admin"
        UserRole.Subadmin -> "Premium"
        UserRole.User -> "User"
    }

private val UserRole.roleSummary: String
    get() = when (this) {
        UserRole.Owner -> "Festes Hauptkonto der App. Fuer diese App ist nash.lioncorna@gmail.com immer der Owner. Root-Zugriff auf Shopify, Zahlungen, Rollen, KI-Defaults und Recovery."
        UserRole.Admin -> "Teaminterne Leute. Der Owner weist ihnen gezielt Funktionen wie Music, Video oder Profil-Moderation zu. Kein Zugriff auf Owner-Systembereiche."
        UserRole.Subadmin -> "Externe Premium-Konten mit buchbarem Kontingentmodell. Kein Admin-Workspace, keine Owner-Rechte."
        UserRole.User -> "Normales Nutzerkonto mit Free-Kontingent. Nicht eingeloggte Leute sind zusaetzlich Gast-Nutzer ohne gespeichertes Konto."
    }

private val UserQuotaPlan.displayTitle: String
    get() = when (this) {
        UserQuotaPlan.OwnerUnlimited -> "Owner Unlimited"
        UserQuotaPlan.InternalTeam -> "Internal Team"
        UserQuotaPlan.Free -> "Free"
        UserQuotaPlan.Creator -> "Creator"
        UserQuotaPlan.Studio -> "Studio"
    }

private val UserQuotaPlan.planSummary: String
    get() = when (this) {
        UserQuotaPlan.OwnerUnlimited -> "Praktisch unbegrenztes Owner-Kontingent fuer Systemsteuerung, Tests und Recovery."
        UserQuotaPlan.InternalTeam -> "Internes Team-Kontingent fuer feste Mitarbeiter."
        UserQuotaPlan.Free -> "Basiszugang mit kleinem Free-Kontingent."
        UserQuotaPlan.Creator -> "Erweitertes Creator-Kontingent fuer regelmaessige Nutzung."
        UserQuotaPlan.Studio -> "Grosses Studio-Kontingent fuer intensivere Nutzung und laengere History."
    }

private fun String.parsePositiveIntOrDefault(fallback: Int): Int {
    val value = trim().toIntOrNull() ?: return fallback
    return if (value > 0) value else fallback
}

private fun String.parseIntInRangeOrDefault(
    fallback: Int,
    min: Int,
    max: Int,
): Int {
    val value = trim().toIntOrNull() ?: return fallback
    return value.coerceIn(min, max)
}

private fun historyOptionLabel(days: Int): String {
    return if (days == 1) "1 Tag" else "$days Tage"
}

private fun resolveAutomationDraftWebhookUrl(
    baseUrl: String,
    webhookPath: String,
): String? {
    val trimmedBaseUrl = baseUrl.trim()
    if (trimmedBaseUrl.isBlank()) {
        return null
    }

    val normalizedBaseUrl = if (trimmedBaseUrl.startsWith("https://") || trimmedBaseUrl.startsWith("http://")) {
        trimmedBaseUrl
    } else {
        "https://$trimmedBaseUrl"
    }.trimEnd('/')

    val trimmedPath = webhookPath.trim().trim('/')
    return if (trimmedPath.isBlank()) normalizedBaseUrl else "$normalizedBaseUrl/$trimmedPath"
}

private fun resolveManusValidationLabel(status: String): String {
    return when (status) {
        "key_valid" -> "key valid"
        "key_invalid" -> "key invalid"
        "awaiting_external_auth" -> "awaiting external auth"
        "fallback_internal" -> "fallback internal"
        "external_failed" -> "external failed"
        else -> "fallback internal"
    }
}

private fun openSupportEmail(
    context: Context,
    userEmail: String,
    userName: String,
    supportEmail: String,
) {
    val subject = if (userEmail.isNotBlank()) {
        "Support-Anfrage - $userEmail"
    } else {
        "Support-Anfrage"
    }
    val body = """
        Hallo SkyOS-Team,

        ich habe folgende Anfrage:

        Eingeloggter Account: ${userName.ifBlank { "Nicht verfuegbar" }}
        Account-E-Mail: ${userEmail.ifBlank { "Nicht verfuegbar" }}

        Nachricht:
    """.trimIndent()
    openEmailDraft(
        context = context,
        recipients = listOf(supportEmail.ifBlank { "skydownent@gmail.com" }),
        subject = subject,
        body = body,
    )
}

private fun String.parseDecimalInput(): Double? {
    return trim().replace(",", ".").toDoubleOrNull()
}

private fun formatDecimalDraft(value: Double, decimals: Int = 2): String {
    return String.format(Locale.US, "%.${decimals}f", value)
}
