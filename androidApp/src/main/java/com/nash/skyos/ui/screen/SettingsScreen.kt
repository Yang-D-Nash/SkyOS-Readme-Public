package com.nash.skyos.ui.screen

import android.Manifest
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
    viewModel: SettingsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val screenHeaderSettingsRepository = remember { AppContainer.screenHeaderSettingsRepository }
    val screenHeaderSettings by screenHeaderSettingsRepository.settings.collectAsStateWithLifecycle()
    val artistPages by ArtistPagesStore.pages.collectAsStateWithLifecycle()
    val artistPagesError by ArtistPagesStore.lastErrorMessage.collectAsStateWithLifecycle()
    val context = LocalContext.current
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
        (
            ArtistPagesStore.pagesForBrand(com.nash.skyos.data.ArtistPageBrand.Zweizwei) +
                ArtistPagesStore.pagesForBrand(com.nash.skyos.data.ArtistPageBrand.Nicma)
            ).sortedWith(
                compareBy<ArtistPageUi>({ it.brand.displayTitle }, { it.artistName.lowercase() }),
            )
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
            "Benachrichtigungen sind aktiv."
        } else {
            "Benachrichtigungen sind aktuell aus. Du kannst sie in den Systemeinstellungen aktivieren."
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
            membershipOpsError = error.localizedMessage ?: "Membership Command Center konnte nicht geladen werden."
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
            aiFaqReviewLoopError = error.localizedMessage ?: "FAQ Review Loop konnte nicht geladen werden."
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
                    feedbackMessage = "Bild hochgeladen und uebernommen."
                    feedbackType = ToastType.Success
                } else {
                    feedbackMessage = result.exceptionOrNull()?.message ?: "Bild konnte nicht hochgeladen werden."
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
                    text = "Hier steuerst du, welche Konten normaler User, Subadmin, Admin oder Owner sind. Gleichzeitig legst du fest, ob KI fuer ein Konto aktiv ist und wie hoch die Tageslimits fuer Bot, Visuals und Agent liegen.",
                    modifier = Modifier.padding(top = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )

                LazyRow(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        SettingsBadge(
                            text = "4 Rollen",
                            icon = Icons.Default.Person,
                            isActive = true,
                        )
                    }
                    item {
                        SettingsBadge(
                            text = "${uiState.managedUsers.size} Konten",
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
                        text = "Sobald weitere Konten in der App registriert sind, erscheinen sie hier direkt zur Rollen- und KI-Verwaltung.",
                        modifier = Modifier.padding(top = 12.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    )
                } else {
                    Column(
                        modifier = Modifier.padding(top = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
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
                    text = "Hier bekommen 22-Artists und NICMA ihre eigene repraesentative Seite. Du als Owner verteilst Editor-Rechte; nur diese Konten oder du selbst duerfen den Inhalt spaeter anpassen.",
                    modifier = Modifier.padding(top = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )

                LazyRow(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        SettingsBadge(
                            text = "$publishedArtistPageCount Seiten mit Inhalt",
                            icon = Icons.Default.LibraryMusic,
                            isActive = publishedArtistPageCount > 0,
                        )
                    }
                    item {
                        SettingsBadge(
                            text = "$assignedArtistPageCount mit Editoren",
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
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    for (page in managedShowcasePages) {
                        ArtistPageAdminCard(
                            page = page,
                            users = uiState.managedUsers.filterNot { it.isPlatformOwner },
                            onSave = { updatedPage ->
                                coroutineScope.launch {
                                    val result = ArtistPagesStore.save(updatedPage)
                                    feedbackMessage = if (result.isSuccess) {
                                        "${updatedPage.artistName} gespeichert."
                                    } else {
                                        result.exceptionOrNull()?.message ?: "Artist-Seite konnte nicht gespeichert werden."
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
                    text = "Diese Hero-Bereiche laufen direkt unter den Header-Karten von Home, Music, Shop und Video. Die App dunkelt Bilder automatisch ab, damit Schrift und Badges lesbar bleiben. Fuer alle vier Bereiche kannst du Bild, Titel und kurze Positionierung pflegen.",
                    modifier = Modifier.padding(top = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )

                LazyRow(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        SettingsBadge(
                            text = "${screenHeaderSettings.configuredCount} angepasst",
                            icon = Icons.Default.Palette,
                            isActive = screenHeaderSettings.configuredCount > 0,
                        )
                    }
                    item {
                        SettingsBadge(
                            text = "Overlay aktiv",
                            icon = Icons.Default.CheckCircle,
                            isActive = true,
                        )
                    }
                    item {
                        SettingsBadge(
                            text = "CRUD bereit",
                            icon = Icons.Default.CheckCircle,
                            isActive = true,
                        )
                    }
                }

                Text(
                    text = "Bilder und Texte kannst du neu setzen, ersetzen oder entfernen. Live gehen die Aenderungen erst nach `Header speichern`.",
                    modifier = Modifier.padding(top = 10.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )

                EditableImageFieldCard(
                    title = "Home Header",
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
                            feedbackMessage = "Bild entfernt."
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
                    title = "Music Hub Header",
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
                            feedbackMessage = "Bild entfernt."
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
                    title = "Shop Header",
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
                            feedbackMessage = "Bild entfernt."
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
                    title = "Video Header",
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
                            feedbackMessage = "Bild entfernt."
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
                                    "Header gespeichert."
                                } else {
                                    result.exceptionOrNull()?.message ?: "Header konnten nicht gespeichert werden."
                                }
                                feedbackType = if (result.isSuccess) ToastType.Success else ToastType.Error
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text("Header speichern")
                }
            }

            AdminWorkspaceSection.Shopify -> {
                Text(
                    text = "Fuer den Merch-Katalog pflegt der Owner hier die Store-Domain, optional den Storefront Access Token und die aktivierten Collections. Danach laedt der Shop direkt aus Shopify.",
                    modifier = Modifier.padding(top = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )

                Text(
                    text = "Shopify Quelle",
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
                    label = { Text("Store-Domain") },
                    placeholder = { Text("k5t1sc-ps.myshopify.com") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = shopifyStorefrontAccessTokenDraft,
                    onValueChange = { shopifyStorefrontAccessTokenDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("Storefront Access Token") },
                    placeholder = { Text("shpat_... oder storefront token") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = shopifyCollectionHandlesDraft,
                    onValueChange = { shopifyCollectionHandlesDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("Collection-Handles") },
                    placeholder = { Text("z. B. spring-drop-2026, hoodies, accessories") },
                    singleLine = false,
                )

                OutlinedButton(
                    onClick = { viewModel.refreshShopifyCollections(force = true) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    shape = RoundedCornerShape(18.dp),
                    enabled = !uiState.isLoadingShopifyCollections,
                ) {
                    Text(
                        if (uiState.isLoadingShopifyCollections) {
                            "Collections werden geladen..."
                        } else {
                            "Collections aus Shopify laden"
                        },
                    )
                }

                if (uiState.availableShopifyCollections.isNotEmpty()) {
                    Text(
                        text = "Verfuegbare Collections",
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
                        label = { Text("Collections suchen") },
                        placeholder = { Text("Nach Titel oder Handle filtern") },
                        singleLine = true,
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
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
                                text = "${selectedHandles.size} ausgewaehlt",
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
                                shape = RoundedCornerShape(18.dp),
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
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
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
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        collection.productCount?.let { count ->
                                            Text(
                                                text = "$count",
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
                } else if (uiState.shopifyCollectionsErrorMessage != null) {
                    Text(
                        text = "Collections konnten nicht geladen werden: ${uiState.shopifyCollectionsErrorMessage}",
                        modifier = Modifier.padding(top = 10.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    )
                }

                Text(
                    text = "Mehrere Collections kannst du oben antippen oder hier manuell per Komma oder Zeilenumbruch pflegen. Leer bedeutet: gesamter veroeffentlichter Store.",
                    modifier = Modifier.padding(top = 10.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )

                Text(
                    text = if (uiState.shopifyAdminSettings.hasCollectionFilter) {
                        "Aktuell aktiv: ${uiState.shopifyAdminSettings.activeCollectionLabel}"
                    } else {
                        "Aktuell aktiv: gesamter Shopify-Store"
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
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text("Shopify speichern")
                }
            }

            AdminWorkspaceSection.Payments -> {
                Text(
                    text = "PayPal und Bankueberweisung laufen als manueller Owner-Handoff. Stripe ist als sicherer Live-Checkout aktiv, und Klarna laeuft live ueber Stripe, sobald es im Stripe-Dashboard freigeschaltet und serverseitig konfiguriert ist.",
                    modifier = Modifier.padding(top = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )

                PaymentProviderAdminCard(
                    title = "Stripe",
                    connected = uiState.paymentMethods.stripe.connected,
                    enabledInCheckout = uiState.paymentMethods.stripe.connected &&
                        uiState.paymentMethods.stripe.enabled,
                    accountHint = stripeAccountHintDraft,
                    accountHintLabel = "Stripe Konto / Workspace",
                    accountHintPlaceholder = "z. B. SkyOS Merch Workspace",
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
                    title = "PayPal",
                    connected = uiState.paymentMethods.paypal.connected,
                    enabledInCheckout = uiState.paymentMethods.paypal.connected &&
                        uiState.paymentMethods.paypal.enabled,
                    accountHint = paypalAccountHintDraft,
                    accountHintLabel = "PayPal.Me Link oder Business-Mail",
                    accountHintPlaceholder = "z. B. https://paypal.me/deinname",
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
                    title = "Klarna",
                    connected = uiState.paymentMethods.klarna.connected,
                    enabledInCheckout = uiState.paymentMethods.klarna.connected &&
                        uiState.paymentMethods.klarna.enabled,
                    accountHint = klarnaAccountHintDraft,
                    accountHintLabel = "Klarna Merchant / Store ID",
                    accountHintPlaceholder = "z. B. Klarna Merchant EU",
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
                    text = "Der Checkout nutzt diese Werte direkt fuer Versand, MwSt.-Ausweisung und vorbereitete Bestellsummen. Der Store-Schalter aus Merchandise bleibt dabei die harte Freigabe fuer Kunden.",
                    modifier = Modifier.padding(top = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )

                Text(
                    text = "Versand",
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
                    label = { Text("Versand Deutschland (EUR)") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = euShippingDraft,
                    onValueChange = { euShippingDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("Versand EU (EUR)") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = internationalShippingDraft,
                    onValueChange = { internationalShippingDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("Versand International (EUR)") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = freeShippingThresholdDraft,
                    onValueChange = { freeShippingThresholdDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("Versand frei ab (EUR)") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = shippingNotesDraft,
                    onValueChange = { shippingNotesDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("Versandhinweis") },
                    minLines = 2,
                    maxLines = 3,
                )

                Text(
                    text = "Rechnung",
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
                    label = { Text("Firmenname") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = invoiceCompanyAddressDraft,
                    onValueChange = { invoiceCompanyAddressDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("Firmenadresse") },
                    minLines = 2,
                    maxLines = 3,
                )
                OutlinedTextField(
                    value = invoiceTaxNumberDraft,
                    onValueChange = { invoiceTaxNumberDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("Steuernummer") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = invoiceVatIdDraft,
                    onValueChange = { invoiceVatIdDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("USt-IdNr.") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = invoiceTaxRateDraft,
                    onValueChange = { invoiceTaxRateDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("MwSt. Satz (%)") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = invoicePrefixDraft,
                    onValueChange = { invoicePrefixDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("Rechnungs-Praefix") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = invoiceSupportEmailDraft,
                    onValueChange = { invoiceSupportEmailDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("Support / Rechnungs-Mail") },
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
                            successMessage = "Versand- und Rechnungsdaten gespeichert.",
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text("Versand & Rechnung speichern")
                }
            }

            AdminWorkspaceSection.Visuals -> {
                Text(
                    text = "Visual Reference Pack",
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                SettingsToggleRow(
                    title = "Referenzbibliothek aktiv",
                    body = "Drive-Link, Benennungs-Praefix und bis zu 5 Referenzhinweise fuer Visual-Prompts auf diesem Admin-Geraet.",
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
                    label = { Text("Drive- oder Asset-Link") },
                    placeholder = { Text("https://drive.google.com/...") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = uiState.aiVisualReferenceLibrary.namingPrefix,
                    onValueChange = viewModel::updateAiVisualNamingPrefix,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("Benennungs-Praefix") },
                    placeholder = { Text("z. B. skydown_drop_") },
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
                        label = { Text("Referenz ${index + 1}") },
                        placeholder = {
                            Text("z. B. Charakter, Outfit, Shot, Element oder Mood")
                        },
                        minLines = 2,
                        maxLines = 3,
                    )
                }
            }

            AdminWorkspaceSection.Automation -> {
                Text(
                    text = "Mein Agent-Service (Activepieces + Manus, n8n optional)",
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                SettingsToggleRow(
                    title = "External Workflow aktiv",
                    body = "Die App bleibt normal ueber Firebase eingeloggt. Dieser Workflow gilt nur fuer dein aktuelles Konto.",
                    checked = automationEnabledDraft,
                    onCheckedChange = { automationEnabledDraft = it },
                    modifier = Modifier.padding(top = 10.dp),
                )
                SettingsToggleRow(
                    title = "App-User-Kontext mitsenden",
                    body = "UID, E-Mail und Username werden serverseitig geprueft und an den externen Workflow uebergeben.",
                    checked = automationSendsUserContextDraft,
                    onCheckedChange = { automationSendsUserContextDraft = it },
                    modifier = Modifier.padding(top = 10.dp),
                )
                OutlinedTextField(
                    value = automationWorkflowNameDraft,
                    onValueChange = { automationWorkflowNameDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    label = { Text("Workflow Name") },
                    placeholder = { Text("z. B. AI Script Pipeline") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = automationBaseUrlDraft,
                    onValueChange = { automationBaseUrlDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("Activepieces Base URL") },
                    placeholder = { Text("https://cloud.activepieces.com") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = automationWebhookPathDraft,
                    onValueChange = { automationWebhookPathDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("Webhook Path") },
                    placeholder = { Text("webhook/skydown-app") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = automationAuthHeaderNameDraft,
                    onValueChange = { automationAuthHeaderNameDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("Auth Header Name") },
                    placeholder = { Text("z. B. X-SkyOS-Automation-Key") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = automationAuthHeaderValueDraft,
                    onValueChange = { automationAuthHeaderValueDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("Auth Header Value") },
                    placeholder = { Text("optional") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = automationKnowledgeContextDraft,
                    onValueChange = { automationKnowledgeContextDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("Knowledge-Kontext (optional)") },
                    placeholder = {
                        Text("z. B. Drive-Ordner, Brand-Guidelines, SOPs oder Projektregeln")
                    },
                    minLines = 3,
                )

                val resolvedWebhookUrl = resolveAutomationDraftWebhookUrl(
                    baseUrl = automationBaseUrlDraft,
                    webhookPath = automationWebhookPathDraft,
                )

                Text(
                    text = resolvedWebhookUrl?.let { "Webhook: $it" } ?: "Webhook noch nicht vollstaendig",
                    modifier = Modifier.padding(top = 12.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    val updatedAutomationSettings = uiState.workflowAutomationSettings.copy(
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
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Text("Service speichern")
                    }

                    OutlinedButton(
                        onClick = { viewModel.testWorkflowAutomationSettings(updatedAutomationSettings) },
                        modifier = Modifier.weight(1f),
                        enabled = automationEnabledDraft && resolvedWebhookUrl != null,
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Text("Test senden")
                    }
                }

                Text(
                    text = "Mein Manus-Account (optional)",
                    modifier = Modifier.padding(top = 18.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )

                SettingsToggleRow(
                    title = "Eigenen Manus-Account verwenden",
                    body = "Wenn aktiv, sendet der Agent deinen lokalen Manus API Key pro Anfrage. Der Key wird nur auf diesem Geraet gespeichert.",
                    checked = manusByosEnabledDraft,
                    onCheckedChange = { manusByosEnabledDraft = it },
                    modifier = Modifier.padding(top = 10.dp),
                )

                Row(
                    modifier = Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SettingsBadge(
                        text = if (uiState.manusByosSettings.hasApiKey) "Key lokal gespeichert" else "Key fehlt",
                        icon = Icons.Default.CheckCircle,
                        isActive = uiState.manusByosSettings.hasApiKey,
                    )
                    SettingsBadge(
                        text = if (uiState.manusByosSettings.isEnabled && uiState.manusByosSettings.hasApiKey) {
                            "BYOS aktiv"
                        } else {
                            "BYOS aus"
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
                    label = { Text("Manus API Key") },
                    placeholder = { Text("sk-...") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )

                Text(
                    text = "Du kannst den Key jederzeit ersetzen oder entfernen. Ohne lokalen Key nutzt der Agent wieder das Backend-Setup.",
                    modifier = Modifier.padding(top = 10.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )

                SettingsBadge(
                    text = "Validate: ${resolveManusValidationLabel(uiState.manusValidationStatus)}",
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
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = {
                            viewModel.saveManusByosSettings(
                                enabled = manusByosEnabledDraft,
                                apiKeyDraft = manusByosApiKeyDraft,
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Text("Manus speichern")
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.clearManusByosApiKey()
                            manusByosApiKeyDraft = ""
                        },
                        modifier = Modifier.weight(1f),
                        enabled = uiState.manusByosSettings.hasApiKey || manusByosApiKeyDraft.isNotBlank(),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Text("Key entfernen")
                    }
                }

                OutlinedButton(
                    onClick = { viewModel.validateManusByosKey(manusByosApiKeyDraft) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    shape = RoundedCornerShape(18.dp),
                    enabled = manusByosApiKeyDraft.isNotBlank() || uiState.manusByosSettings.hasApiKey,
                ) {
                    Text("Validate Manus Key")
                }

                Text(
                    text = "Mein Agent-Profil (Skills & Arbeitsweise)",
                    modifier = Modifier.padding(top = 18.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )

                SettingsToggleRow(
                    title = "Persoenliches Agent-Profil aktiv",
                    body = "Wenn aktiv, nutzt der Agent deine Skills, Output-Vorgaben und Guardrails fuer dieses Konto.",
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
                    label = { Text("Rolle / Fokus") },
                    placeholder = { Text("z. B. Artist Brand Strategist, Rap Release Lead") },
                    minLines = 2,
                    maxLines = 4,
                )
                OutlinedTextField(
                    value = agentSkillProfileDraft,
                    onValueChange = { agentSkillProfileDraft = it.take(12000) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("Skills & Spezialisierungen") },
                    placeholder = {
                        Text("z. B. TikTok Hooks, Storyboard, Shotlist, Reels, Scriptwriting, Content-Repurposing")
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
                    label = { Text("Antwort- / Deliverable-Format") },
                    placeholder = { Text("z. B. Erst Plan, dann Shotlist, dann CTA-Varianten in Tabellenform") },
                    minLines = 3,
                    maxLines = 8,
                )
                OutlinedTextField(
                    value = agentGuardrailsDraft,
                    onValueChange = { agentGuardrailsDraft = it.take(4000) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("Guardrails / No-Gos") },
                    placeholder = { Text("z. B. keine generischen Phrasen, keine Jugend-Slang-Simulation, kein Clickbait") },
                    minLines = 3,
                    maxLines = 8,
                )
                OutlinedTextField(
                    value = agentKnowledgeContextDraft,
                    onValueChange = { agentKnowledgeContextDraft = it.take(4000) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("Persoenlicher Knowledge-Kontext") },
                    placeholder = { Text("z. B. Zielgruppe, Projekt-DNA, Sound-Referenzen, Release-KPIs") },
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
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text("Agent-Profil speichern")
                }
            }

            AdminWorkspaceSection.AiPrompts -> {
                Text(
                    text = "Hier definierst du zentrale KI-Anweisungen fuer Bot, Visuals und Agent. Die Werte liegen in Firestore (`adminConfig/aiPromptSettings`) und gelten serverseitig sofort. Ueber die Asset-Bibliothek kannst du z. B. einen MEGA-, Drive- oder Moodboard-Link global mitgeben.",
                    modifier = Modifier.padding(top = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )

                LazyRow(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        SettingsBadge(
                            text = "Text ${aiTextInstructionDraft.trim().length}",
                            icon = Icons.Default.Settings,
                            isActive = aiTextInstructionDraft.isNotBlank(),
                        )
                    }
                    item {
                        SettingsBadge(
                            text = "Visual ${aiVisualInstructionDraft.trim().length}",
                            icon = Icons.Default.Palette,
                            isActive = aiVisualInstructionDraft.isNotBlank(),
                        )
                    }
                    item {
                        SettingsBadge(
                            text = "Agent ${aiAgentSystemInstructionDraft.trim().length}",
                            icon = Icons.Default.Bolt,
                            isActive = aiAgentSystemInstructionDraft.isNotBlank(),
                        )
                    }
                    item {
                        SettingsBadge(
                            text = "FAQ ${aiFaqInstructionDraft.trim().length}",
                            icon = Icons.Default.Email,
                            isActive = aiFaqInstructionDraft.isNotBlank(),
                        )
                    }
                    item {
                        SettingsBadge(
                            text = if (aiAssetLibraryLinkDraft.isBlank()) "Assets aus" else "Assets aktiv",
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
                    label = { Text("Bot Text-Anweisung") },
                    minLines = 6,
                    maxLines = 14,
                )

                OutlinedTextField(
                    value = aiVisualInstructionDraft,
                    onValueChange = { aiVisualInstructionDraft = it.take(12000) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("Visual-Anweisung") },
                    minLines = 6,
                    maxLines = 14,
                )

                OutlinedTextField(
                    value = aiAgentSystemInstructionDraft,
                    onValueChange = { aiAgentSystemInstructionDraft = it.take(12000) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("Agent System-Anweisung") },
                    minLines = 6,
                    maxLines = 16,
                )

                OutlinedTextField(
                    value = aiFaqInstructionDraft,
                    onValueChange = { aiFaqInstructionDraft = it.take(12000) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("FAQ System-Anweisung") },
                    minLines = 5,
                    maxLines = 12,
                )

                OutlinedTextField(
                    value = aiFaqKnowledgeBaseDraft,
                    onValueChange = { aiFaqKnowledgeBaseDraft = it.take(12000) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("FAQ / Owner Knowledge") },
                    minLines = 6,
                    maxLines = 16,
                )

                OutlinedTextField(
                    value = aiAssetLibraryLinkDraft,
                    onValueChange = { aiAssetLibraryLinkDraft = it.take(2000) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("Asset- / Referenzbibliothek") },
                    placeholder = { Text("z. B. https://mega.nz/folder/...") },
                    singleLine = true,
                )

                OutlinedTextField(
                    value = aiAssetReferenceNotesDraft,
                    onValueChange = { aiAssetReferenceNotesDraft = it.take(12000) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("Asset-Hinweise fuer Bot, Visuals und Agent") },
                    minLines = 4,
                    maxLines = 10,
                )

                Text(
                    text = "Leer lassen stellt den jeweiligen Standard wieder her.",
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
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text("KI-Anweisungen speichern")
                }

                Text(
                    text = "Runtime & Provider (`adminConfig/aiRuntime`)",
                    modifier = Modifier.padding(top = 18.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )

                Text(
                    text = "Hier steuerst du Bot-Core, FAQ-Prioritaet, Diagnostics, Fallbacks sowie Agent-Provider und Limits serverseitig fuer iOS und Android gemeinsam.",
                    modifier = Modifier.padding(top = 4.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodySmall,
                )

                Text(
                    text = "FAQ Review Loop (30d)",
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item {
                            SettingsBadge(
                                text = "Strong ${aiFaqReviewLoop.strongestTriggers.size}",
                                icon = Icons.Default.CheckCircle,
                                isActive = aiFaqReviewLoop.strongestTriggers.isNotEmpty(),
                            )
                        }
                        item {
                            SettingsBadge(
                                text = "Weak ${aiFaqReviewLoop.weakTriggers.size}",
                                icon = Icons.Default.Settings,
                                isActive = aiFaqReviewLoop.weakTriggers.isNotEmpty(),
                            )
                        }
                        item {
                            SettingsBadge(
                                text = "Useless ${aiFaqReviewLoop.likelyUselessTriggers.size}",
                                icon = Icons.Default.Lock,
                                isActive = aiFaqReviewLoop.likelyUselessTriggers.isNotEmpty(),
                            )
                        }
                        item {
                            SettingsBadge(
                                text = "Repeat ${aiFaqReviewLoop.repeatHeavyTopics.size}",
                                icon = Icons.Default.Email,
                                isActive = aiFaqReviewLoop.repeatHeavyTopics.isNotEmpty(),
                            )
                        }
                    }

                    aiFaqReviewLoop.strongestTriggers.firstOrNull()?.let { entry ->
                        Text(
                            text = "Strongest: ${entry.triggerKey} · Conv ${(entry.conversionRate * 100).toInt()}% · Repeat ${(entry.repeatRate * 100).toInt()}%",
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    aiFaqReviewLoop.weakTriggers.firstOrNull()?.let { entry ->
                        Text(
                            text = "Weak: ${entry.triggerKey} · Conv ${(entry.conversionRate * 100).toInt()}% · Repeat ${(entry.repeatRate * 100).toInt()}%",
                            modifier = Modifier.padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    aiFaqReviewLoop.repeatHeavyTopics.firstOrNull()?.let { topic ->
                        Text(
                            text = "Repeat-Heavy Topic: ${topic.key} (${topic.value}x, ${(topic.share * 100).toInt()}%)",
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
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                                    text = "Erwartete Wirkung: ${insight.expectedImpact}",
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
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                                    text = "Action: ${recommendation.actionType} · Target: ${recommendation.targetField} · Suggest: ${recommendation.suggestedValueLabel}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                                        shape = RoundedCornerShape(12.dp),
                                    ) {
                                        Text("Preview")
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
                                        shape = RoundedCornerShape(12.dp),
                                    ) {
                                        Text("Apply")
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
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Revert Last Change")
                    }
                }

                LazyRow(
                    modifier = Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        SettingsBadge(
                            text = if (aiBotPromptVersionDraft.isBlank()) "Prompt v?" else aiBotPromptVersionDraft,
                            icon = Icons.Default.Settings,
                            isActive = aiBotPromptVersionDraft.isNotBlank(),
                        )
                    }
                    item {
                        SettingsBadge(
                            text = "FAQ $aiBotFaqModeDraft",
                            icon = Icons.Default.Email,
                            isActive = true,
                        )
                    }
                    item {
                        SettingsBadge(
                            text = "Q $aiBotQualityModeDraft",
                            icon = Icons.Default.CheckCircle,
                            isActive = true,
                        )
                    }
                    item {
                        SettingsBadge(
                            text = "Provider ${AiRuntimeAgentProvider.resolve(aiAgentProviderDraft).displayTitle}",
                            icon = Icons.Default.Bolt,
                            isActive = true,
                        )
                    }
                    item {
                        SettingsBadge(
                            text = if (aiCostGuardEnabledDraft) "Kosten-Guard an" else "Kosten-Guard aus",
                            icon = Icons.Default.CheckCircle,
                            isActive = aiCostGuardEnabledDraft,
                        )
                    }
                    item {
                        SettingsBadge(
                            text = if (aiManusEnabledDraft) "Manus aktiv" else "Manus aus",
                            icon = Icons.Default.Settings,
                            isActive = aiManusEnabledDraft,
                        )
                    }
                }

                Text(
                    text = "Bot Core (`adminConfig/aiRuntime.bot`)",
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
                    label = { Text("Prompt Version") },
                    singleLine = true,
                )

                OutlinedTextField(
                    value = aiBotPersonalityStyleDraft,
                    onValueChange = { aiBotPersonalityStyleDraft = it.take(160) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("AI Personality Stil") },
                    singleLine = true,
                )

                OutlinedTextField(
                    value = aiBotLoggingLevelDraft,
                    onValueChange = { aiBotLoggingLevelDraft = it.take(80) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("Logging Level") },
                    singleLine = true,
                )

                Text(
                    text = "Quality Mode",
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
                LazyRow(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf("balanced" to "Balanced", "high" to "High").forEach { (value, label) ->
                        item {
                            val isSelected = aiBotQualityModeDraft == value
                            OutlinedButton(
                                onClick = { aiBotQualityModeDraft = value },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                                ),
                                shape = RoundedCornerShape(14.dp),
                            ) { Text(label) }
                        }
                    }
                }

                Text(
                    text = "FAQ Mode",
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
                LazyRow(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf("off" to "Off", "auto" to "Auto", "prefer_faq" to "Prefer").forEach { (value, label) ->
                        item {
                            val isSelected = aiBotFaqModeDraft == value
                            OutlinedButton(
                                onClick = { aiBotFaqModeDraft = value },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                                ),
                                shape = RoundedCornerShape(14.dp),
                            ) { Text(label) }
                        }
                    }
                }

                Text(
                    text = "Owner Mode",
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
                LazyRow(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf("standard" to "Standard", "diagnostic" to "Diagnostic").forEach { (value, label) ->
                        item {
                            val isSelected = aiBotOwnerModeDraft == value
                            OutlinedButton(
                                onClick = { aiBotOwnerModeDraft = value },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                                ),
                                shape = RoundedCornerShape(14.dp),
                            ) { Text(label) }
                        }
                    }
                }

                Text(
                    text = "Antwortlaenge",
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
                LazyRow(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf("adaptive" to "Adaptive", "short" to "Kurz", "detailed" to "Tief").forEach { (value, label) ->
                        item {
                            val isSelected = aiBotAnswerLengthDraft == value
                            OutlinedButton(
                                onClick = { aiBotAnswerLengthDraft = value },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                                ),
                                shape = RoundedCornerShape(14.dp),
                            ) { Text(label) }
                        }
                    }
                }

                Text(
                    text = "Diagnostics",
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
                LazyRow(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf("off" to "Off", "owner_only" to "Owner", "verbose" to "Verbose").forEach { (value, label) ->
                        item {
                            val isSelected = aiBotDiagnosticsModeDraft == value
                            OutlinedButton(
                                onClick = { aiBotDiagnosticsModeDraft = value },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                                ),
                                shape = RoundedCornerShape(14.dp),
                            ) { Text(label) }
                        }
                    }
                }

                SettingsToggleRow(
                    title = "Kill Switch aktiv",
                    body = "Blockiert Bot-Anfragen owner-seitig sofort.",
                    checked = aiBotKillSwitchDraft,
                    onCheckedChange = { aiBotKillSwitchDraft = it },
                    modifier = Modifier.padding(top = 10.dp),
                )

                SettingsToggleRow(
                    title = "Bot Cost Guard aktiv",
                    body = "Kurzere Antworten und ruhigere Limits im Bot-Core.",
                    checked = aiBotCostGuardEnabledDraft,
                    onCheckedChange = { aiBotCostGuardEnabledDraft = it },
                    modifier = Modifier.padding(top = 8.dp),
                )

                SettingsToggleRow(
                    title = "Kurz bei Critical Guard",
                    body = "Verkuerzt Antworten, wenn Cost Guard kritisch wird.",
                    checked = aiBotPreferBriefCriticalDraft,
                    onCheckedChange = { aiBotPreferBriefCriticalDraft = it },
                    modifier = Modifier.padding(top = 8.dp),
                )

                SettingsToggleRow(
                    title = "FAQ priorisieren bei Topic-Match",
                    body = "Zieht FAQ-Antworten bei passenden Help-/Guide-Fragen nach vorne.",
                    checked = aiBotPreferFaqRoutingDraft,
                    onCheckedChange = { aiBotPreferFaqRoutingDraft = it },
                    modifier = Modifier.padding(top = 8.dp),
                )

                SettingsToggleRow(
                    title = "Produkt-Guide fuer neue Nutzer bevorzugen",
                    body = "Erklaert Einsteigerfragen eher als Produktfuehrung.",
                    checked = aiBotPreferProductGuideDraft,
                    onCheckedChange = { aiBotPreferProductGuideDraft = it },
                    modifier = Modifier.padding(top = 8.dp),
                )

                SettingsToggleRow(
                    title = "Visual-Generierung erlauben",
                    body = "Schaltet die Visual-Pipeline im Bot-Core frei.",
                    checked = aiBotAllowVisualGenerationDraft,
                    onCheckedChange = { aiBotAllowVisualGenerationDraft = it },
                    modifier = Modifier.padding(top = 8.dp),
                )

                SettingsToggleRow(
                    title = "Text-Fallback erlauben",
                    body = "Darf bei Modellproblemen einen Text-Fallback nutzen.",
                    checked = aiBotAllowTextFallbackDraft,
                    onCheckedChange = { aiBotAllowTextFallbackDraft = it },
                    modifier = Modifier.padding(top = 8.dp),
                )

                SettingsToggleRow(
                    title = "Visual-Fallback erlauben",
                    body = "Darf bei Bildproblemen auf das Fallback-Modell gehen.",
                    checked = aiBotAllowVisualFallbackDraft,
                    onCheckedChange = { aiBotAllowVisualFallbackDraft = it },
                    modifier = Modifier.padding(top = 8.dp),
                )

                SettingsToggleRow(
                    title = "Fallback-Grund anzeigen",
                    body = "Legt offen, warum ein Fallback genutzt wurde.",
                    checked = aiBotExposeFallbackReasonDraft,
                    onCheckedChange = { aiBotExposeFallbackReasonDraft = it },
                    modifier = Modifier.padding(top = 8.dp),
                )

                SettingsToggleRow(
                    title = "Safe Mode aktiv",
                    body = "Reduziert riskante Behauptungen im FAQ-/Help-Modus.",
                    checked = aiBotSafeModeEnabledDraft,
                    onCheckedChange = { aiBotSafeModeEnabledDraft = it },
                    modifier = Modifier.padding(top = 8.dp),
                )

                SettingsToggleRow(
                    title = "Strict Unknown Handling",
                    body = "Sagt offen 'weiss ich nicht sicher', statt zu erfinden.",
                    checked = aiBotStrictUnknownHandlingDraft,
                    onCheckedChange = { aiBotStrictUnknownHandlingDraft = it },
                    modifier = Modifier.padding(top = 8.dp),
                )

                SettingsToggleRow(
                    title = "Spekulative FAQ blocken",
                    body = "Verhindert erfundene Membership-, Versand- oder Account-Regeln.",
                    checked = aiBotBlockSpeculativeFaqDraft,
                    onCheckedChange = { aiBotBlockSpeculativeFaqDraft = it },
                    modifier = Modifier.padding(top = 8.dp),
                )

                SettingsToggleRow(
                    title = "Proaktive Hinweise aktiv",
                    body = "Aktiviert den Action Layer fuer hohe-Nutzen-Hinweise.",
                    checked = aiBotProactiveHintsEnabledDraft,
                    onCheckedChange = { aiBotProactiveHintsEnabledDraft = it },
                    modifier = Modifier.padding(top = 8.dp),
                )

                SettingsToggleRow(
                    title = "Trigger: AI-Limit fast erreicht",
                    body = "Hinweis kurz vor dem Tageslimit.",
                    checked = aiBotTriggerAiLimitNearEnabledDraft,
                    onCheckedChange = { aiBotTriggerAiLimitNearEnabledDraft = it },
                    modifier = Modifier.padding(top = 8.dp),
                )

                SettingsToggleRow(
                    title = "Trigger: Restore verfuegbar",
                    body = "Hinweis wenn Entitlement inaktiv, aber Restore moeglich.",
                    checked = aiBotTriggerRestoreAvailableEnabledDraft,
                    onCheckedChange = { aiBotTriggerRestoreAvailableEnabledDraft = it },
                    modifier = Modifier.padding(top = 8.dp),
                )

                SettingsToggleRow(
                    title = "Trigger: Bestellung versendet",
                    body = "Hinweis bei shipped/in_transit Fulfillment.",
                    checked = aiBotTriggerOrderShippedEnabledDraft,
                    onCheckedChange = { aiBotTriggerOrderShippedEnabledDraft = it },
                    modifier = Modifier.padding(top = 8.dp),
                )

                SettingsToggleRow(
                    title = "Trigger: Payment-Methode geaendert",
                    body = "Hinweis bei kuerzlich aktualisierten Payment-Settings.",
                    checked = aiBotTriggerPaymentMethodsChangedEnabledDraft,
                    onCheckedChange = { aiBotTriggerPaymentMethodsChangedEnabledDraft = it },
                    modifier = Modifier.padding(top = 8.dp),
                )

                SettingsToggleRow(
                    title = "Trigger: Upgrade nach Nutzung",
                    body = "Hint nur bei hoher realer Nutzung.",
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
                    label = { Text("Warning Threshold (%)") },
                    singleLine = true,
                )

                OutlinedTextField(
                    value = aiBotCriticalThresholdPercentDraft,
                    onValueChange = { aiBotCriticalThresholdPercentDraft = it.take(3) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("Critical Threshold (%)") },
                    singleLine = true,
                )

                OutlinedTextField(
                    value = aiBotUpgradeHintFreeToProTextDraft,
                    onValueChange = { aiBotUpgradeHintFreeToProTextDraft = it.take(220) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("Upgrade Hint Free -> Pro") },
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
                    label = { Text("Upgrade Hint Pro -> Creator") },
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
                    label = { Text("Prompt Version Alias") },
                    singleLine = true,
                )

                Text(
                    text = "FAQ Prioritaet",
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
                LazyRow(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        "live_owner_generic" to "Live->Owner",
                        "owner_live_generic" to "Owner->Live",
                        "balanced" to "Balanced",
                    ).forEach { (value, label) ->
                        item {
                            val isSelected = aiBotFaqPriorityModeDraft == value
                            OutlinedButton(
                                onClick = { aiBotFaqPriorityModeDraft = value },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                                ),
                                shape = RoundedCornerShape(14.dp),
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
                    label = { Text("Text Primary Model") },
                    singleLine = true,
                )

                OutlinedTextField(
                    value = aiBotTextFallbackModelDraft,
                    onValueChange = { aiBotTextFallbackModelDraft = it.take(120) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("Text Fallback Model") },
                    singleLine = true,
                )

                OutlinedTextField(
                    value = aiBotVisualPrimaryModelDraft,
                    onValueChange = { aiBotVisualPrimaryModelDraft = it.take(120) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("Visual Primary Model") },
                    singleLine = true,
                )

                OutlinedTextField(
                    value = aiBotVisualFallbackModelDraft,
                    onValueChange = { aiBotVisualFallbackModelDraft = it.take(120) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("Visual Fallback Model") },
                    singleLine = true,
                )

                OutlinedTextField(
                    value = aiBotShortAnswerMaxTokensDraft,
                    onValueChange = { aiBotShortAnswerMaxTokensDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("Short Answer Max Tokens") },
                    singleLine = true,
                )

                OutlinedTextField(
                    value = aiBotStandardAnswerMaxTokensDraft,
                    onValueChange = { aiBotStandardAnswerMaxTokensDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("Standard Answer Max Tokens") },
                    singleLine = true,
                )

                SettingsToggleRow(
                    title = "Kosten-Guard aktiv",
                    body = "Harte User- und Global-Limits greifen serverseitig als Kostenbremse.",
                    checked = aiCostGuardEnabledDraft,
                    onCheckedChange = { aiCostGuardEnabledDraft = it },
                    modifier = Modifier.padding(top = 10.dp),
                )

                Text(
                    text = "Agent Provider",
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )

                LazyRow(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Text(provider.displayTitle)
                        }
                    }
                }

                Text(
                    text = "Fallback Provider",
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )

                LazyRow(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Text(provider.displayTitle)
                        }
                    }
                }

                SettingsToggleRow(
                    title = "Manus freigeben",
                    body = "Schaltet Manus im Backend frei. Ohne aktiviertes Secret oder bei Fehlern greift der Fallback.",
                    checked = aiManusEnabledDraft,
                    onCheckedChange = { aiManusEnabledDraft = it },
                    modifier = Modifier.padding(top = 10.dp),
                )

                Text(
                    text = "Manus Runtime (`adminConfig/aiRuntime.manus`)",
                    modifier = Modifier.padding(top = 10.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )

                Text(
                    text = "Der API-Key bleibt aus Sicherheitsgruenden im Firebase Functions Secret `MANUS_API_KEY` und wird nicht in der App gespeichert.",
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
                    label = { Text("Request Timeout (ms)") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = aiManusPollIntervalMsDraft,
                    onValueChange = { aiManusPollIntervalMsDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("Poll Interval (ms)") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = aiManusMaxPollAttemptsDraft,
                    onValueChange = { aiManusMaxPollAttemptsDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("Max Poll Attempts") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = aiManusListMessagesLimitDraft,
                    onValueChange = { aiManusListMessagesLimitDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("List Messages Limit") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = aiManusMaxPromptCharsDraft,
                    onValueChange = { aiManusMaxPromptCharsDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("Max Prompt Chars") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = aiManusMaxHistoryTurnsDraft,
                    onValueChange = { aiManusMaxHistoryTurnsDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("Max History Turns") },
                    singleLine = true,
                )

                SettingsToggleRow(
                    title = "Auto Stop bei Waiting-Event",
                    body = "Stoppt den Manus-Run automatisch bei Waiting-Status.",
                    checked = aiManusAutoStopOnWaitingDraft,
                    onCheckedChange = { aiManusAutoStopOnWaitingDraft = it },
                    modifier = Modifier.padding(top = 10.dp),
                )
                SettingsToggleRow(
                    title = "High-Credit Events blocken",
                    body = "Bricht Runs bei kostenintensiven Events sofort ab.",
                    checked = aiManusBlockHighCreditEventsDraft,
                    onCheckedChange = { aiManusBlockHighCreditEventsDraft = it },
                    modifier = Modifier.padding(top = 8.dp),
                )
                SettingsToggleRow(
                    title = "Verbose Events einblenden",
                    body = "Aktiviert detailliertere Event-Ausgaben fuer Debugging.",
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
                    label = { Text("Hard Cap Text / Tag") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = aiHardVisualLimitDraft,
                    onValueChange = { aiHardVisualLimitDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("Hard Cap Visual / Tag") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = aiHardAgentLimitDraft,
                    onValueChange = { aiHardAgentLimitDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("Hard Cap Agent / Tag") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = aiGlobalTextLimitDraft,
                    onValueChange = { aiGlobalTextLimitDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("Global Cap Text / Tag") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = aiGlobalVisualLimitDraft,
                    onValueChange = { aiGlobalVisualLimitDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("Global Cap Visual / Tag") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = aiGlobalAgentLimitDraft,
                    onValueChange = { aiGlobalAgentLimitDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("Global Cap Agent / Tag") },
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
                    shape = RoundedCornerShape(18.dp),
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(MembershipOpsTab.entries.toList()) { tab ->
                        val selected = membershipOpsTab == tab.name
                        OutlinedButton(
                            onClick = { membershipOpsTab = tab.name },
                            shape = RoundedCornerShape(14.dp),
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                                text = "7d Opens: ${(d7["membershipOpens"] as? Number)?.toInt() ?: 0} · Purchases: ${(d7["purchaseSuccess"] as? Number)?.toInt() ?: 0} · CVR: ${(d7["cvr"] as? Number)?.toDouble() ?: 0.0}",
                                modifier = Modifier.padding(top = 14.dp),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = "Alerts: ${alerts.size} · Free Load Ratio: ${(costOverlay["freePlanLoadRatio"] as? Number)?.toDouble() ?: 0.0}",
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
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
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
                                                "Type: ${recommendation.recommendationType} · Confidence ${(recommendation.confidenceScore * 100).toInt()}% · ${recommendation.severity}",
                                                modifier = Modifier.padding(top = 6.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                            )
                                            Row(
                                                modifier = Modifier.padding(top = 10.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                                                                    notes = "Started from Android Membership Command Center",
                                                                ),
                                                            )
                                                            experimentLifecycleIdDraft = lifecycleId
                                                            selectedRecommendationId = recommendation.id
                                                            feedbackMessage = membershipOpsExperimentStartedMessage
                                                            feedbackType = ToastType.Success
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(12.dp),
                                                ) { Text(stringResource(R.string.settings_membership_ops_start_experiment)) }
                                                OutlinedButton(
                                                    onClick = {
                                                        coroutineScope.launch {
                                                            membershipOpsRepository.rejectRecommendation(
                                                                recommendationId = recommendation.id,
                                                                recommendationType = recommendation.recommendationType,
                                                                notes = "Rejected from Android Membership Command Center",
                                                            )
                                                            feedbackMessage = membershipOpsRecommendationRejectedMessage
                                                            feedbackType = ToastType.Warning
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(12.dp),
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
                                label = { Text("Lifecycle ID") },
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
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
                                OutlinedTextField(value = experimentCvrDeltaDraft, onValueChange = { experimentCvrDeltaDraft = it }, modifier = Modifier.weight(1f), label = { Text("CVR Δ") }, singleLine = true)
                                OutlinedTextField(value = experimentAnnualDeltaDraft, onValueChange = { experimentAnnualDeltaDraft = it }, modifier = Modifier.weight(1f), label = { Text("Annual Δ") }, singleLine = true)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
                                OutlinedTextField(value = experimentCreatorDeltaDraft, onValueChange = { experimentCreatorDeltaDraft = it }, modifier = Modifier.weight(1f), label = { Text("Creator Δ") }, singleLine = true)
                                OutlinedTextField(value = experimentCancelDeltaDraft, onValueChange = { experimentCancelDeltaDraft = it }, modifier = Modifier.weight(1f), label = { Text("Cancel Δ") }, singleLine = true)
                            }
                            OutlinedTextField(
                                value = experimentObservedDaysDraft,
                                onValueChange = { experimentObservedDaysDraft = it },
                                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                                label = { Text("Observed Window Days") },
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
                                            feedbackMessage = error.localizedMessage ?: "Experiment konnte nicht abgeschlossen werden."
                                            feedbackType = ToastType.Error
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Text(stringResource(R.string.settings_membership_ops_complete_experiment))
                            }
                        }

                        MembershipOpsTab.Learnings -> {
                            val insights = membershipLearnings["insights"] as? Map<*, *> ?: emptyMap<Any, Any>()
                            val dataStrength = membershipLearnings["dataStrength"] as? String ?: "unknown"
                            Text(
                                text = "Data strength: $dataStrength",
                                modifier = Modifier.padding(top = 14.dp),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = "Calibration: ${(insights["confidenceCalibrationScore"] as? Number)?.toDouble() ?: 0.0} · Simulation Accuracy: ${(insights["simulationAccuracyTrend"] as? Number)?.toDouble() ?: 0.0}",
                                modifier = Modifier.padding(top = 8.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            )
                            val topSurfaces = insights["bestConvertingSurfaces"] as? List<*> ?: emptyList<Any>()
                            Text(
                                text = "Top Surfaces: ${topSurfaces.take(3).joinToString { ((it as? Map<*, *>)?.get("surface") as? String).orEmpty() }}",
                                modifier = Modifier.padding(top = 8.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            )

                            SkydownCard(modifier = Modifier.padding(top = 14.dp)) {
                                Text(stringResource(R.string.settings_membership_ops_hygiene_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "${stringResource(R.string.settings_membership_ops_hygiene_profile)}: $hygieneProfileLabel",
                                    modifier = Modifier.padding(top = 6.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f),
                                )
                                OutlinedTextField(
                                    value = hygieneCooldownCompletedDraft,
                                    onValueChange = { hygieneCooldownCompletedDraft = it },
                                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                                    label = { Text("cooldownDaysCompleted") },
                                    supportingText = { Text("Laenger = weniger Recommendation-Rotation nach completed.") },
                                    singleLine = true,
                                )
                                OutlinedTextField(
                                    value = hygieneCooldownRejectedDraft,
                                    onValueChange = { hygieneCooldownRejectedDraft = it },
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    label = { Text("cooldownDaysRejected") },
                                    supportingText = { Text("Erhoehen bei wiederholten No-Fit-Recommendations.") },
                                    singleLine = true,
                                )
                                OutlinedTextField(
                                    value = hygieneCooldownProposedDraft,
                                    onValueChange = { hygieneCooldownProposedDraft = it },
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    label = { Text("cooldownDaysProposed") },
                                    supportingText = { Text("Verhindert Spam bei frisch vorgeschlagenen Ideen.") },
                                    singleLine = true,
                                )
                                OutlinedTextField(
                                    value = hygieneSimilarityStrictnessDraft,
                                    onValueChange = { hygieneSimilarityStrictnessDraft = it },
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    label = { Text("similarityStrictness") },
                                    supportingText = { Text("strict | balanced | loose.") },
                                    singleLine = true,
                                )
                                OutlinedTextField(
                                    value = hygieneRecurringPenaltyDraft,
                                    onValueChange = { hygieneRecurringPenaltyDraft = it },
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    label = { Text("recurringPenalty") },
                                    supportingText = { Text("Hoeher = staerkerer Prioritaetsabzug fuer Wiederholungen.") },
                                    singleLine = true,
                                )
                                OutlinedTextField(
                                    value = hygieneFreshnessFloorDraft,
                                    onValueChange = { hygieneFreshnessFloorDraft = it },
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    label = { Text("freshnessFloor") },
                                    supportingText = { Text("Mindest-Freshness, darunter wird suppress/downgrade wahrscheinlicher.") },
                                    singleLine = true,
                                )
                                OutlinedTextField(
                                    value = hygieneDuplicateMergeWindowDraft,
                                    onValueChange = { hygieneDuplicateMergeWindowDraft = it },
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    label = { Text("duplicateMergeWindowDays") },
                                    supportingText = { Text("Fenster fuer Duplicate-Kompression.") },
                                    singleLine = true,
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                                        shape = RoundedCornerShape(12.dp),
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
                                                    feedbackMessage = error.localizedMessage ?: "Reset auf Defaults fehlgeschlagen."
                                                    feedbackType = ToastType.Error
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    ) { Text(stringResource(R.string.settings_membership_ops_reset_defaults)) }
                                }
                            }
                        }

                        MembershipOpsTab.Timeline -> {
                            LazyRow(
                                modifier = Modifier.padding(top = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                                        shape = RoundedCornerShape(14.dp),
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
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
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
                                            Text("$dateKey · $type", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f))
                                            Text(title, modifier = Modifier.padding(top = 4.dp), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                            if (summary.isNotBlank()) {
                                                Text(summary, modifier = Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f))
                                            }
                                            if (ownerAction.isNotBlank()) {
                                                Text("${stringResource(R.string.settings_membership_ops_owner_action)}: $ownerAction", modifier = Modifier.padding(top = 6.dp), style = MaterialTheme.typography.labelSmall)
                                            }
                                            if (learnings.isNotBlank()) {
                                                Text("${stringResource(R.string.settings_membership_ops_learnings)}: $learnings", modifier = Modifier.padding(top = 4.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f))
                                            }
                                            if (recommendationId.isNotBlank()) {
                                                OutlinedButton(
                                                    onClick = {
                                                        selectedRecommendationId = recommendationId
                                                        experimentLifecycleIdDraft = "lifecycle_${recommendationId}_${System.currentTimeMillis()}"
                                                        experimentLearningsDraft = "Re-run based on timeline learnings: $title"
                                                        membershipOpsTab = MembershipOpsTab.Experiments.name
                                                    },
                                                    modifier = Modifier.padding(top = 10.dp),
                                                    shape = RoundedCornerShape(12.dp),
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
                        title = "Einstellungen",
                        subtitle = "Konto, Rechtliches, Anzeige und Support sauber sortiert.",
                    )
                },
                navigationIcon = {
                    onClose?.let { close ->
                        IconButton(onClick = close) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Schliessen",
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
                    SettingsOverviewCard(uiState = uiState)
                }

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
                        SectionHeader("Profile / Account")
                        Text(
                            text = "Persoenliche Identitaet, Login-Status und Kontosicherheit.",
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        )
                        if (uiState.isLoggedIn) {
                            Text(
                                text = "Angemeldet als ${uiState.username}",
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
                                shape = RoundedCornerShape(18.dp),
                            ) {
                                Text("Profil bearbeiten")
                            }
                            SettingsToggleRow(
                                title = "KI fuer mein Konto aktiv",
                                body = "Wenn deaktiviert, sind Bot, Visuals und Agent fuer dein Konto serverseitig gesperrt.",
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
                                shape = RoundedCornerShape(18.dp),
                            ) {
                                Text("KI-Einwilligung speichern")
                            }
                            Text(
                                text = "Kontoaktionen",
                                modifier = Modifier.padding(top = 8.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Button(
                                    onClick = { viewModel.signOut() },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !uiState.isSigningOut && !uiState.isDeletingAccount,
                                    shape = RoundedCornerShape(18.dp),
                                ) {
                                    Text(if (uiState.isSigningOut) "Abmelden..." else "Abmelden")
                                }
                                OutlinedButton(
                                    onClick = { viewModel.signOut(onOpenLogin) },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !uiState.isSigningOut && !uiState.isDeletingAccount,
                                    shape = RoundedCornerShape(18.dp),
                                ) {
                                    Text("Anderes Konto")
                                }
                                OutlinedButton(
                                    onClick = { showDeleteAccountDialog.value = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !uiState.isSigningOut && !uiState.isDeletingAccount,
                                    shape = RoundedCornerShape(18.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error,
                                    ),
                                ) {
                                    Text(
                                        if (uiState.isDeletingAccount) {
                                            "Konto wird geloescht..."
                                        } else {
                                            "Konto loeschen"
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
                            Text(
                                text = stringResource(R.string.auth_settings_guest_hint),
                                modifier = Modifier.padding(top = 8.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Button(
                                    onClick = onOpenLogin,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                ) {
                                    Text(stringResource(R.string.auth_sign_in))
                                }
                                OutlinedButton(
                                    onClick = onOpenRegistration,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                ) {
                                    Text("Registrieren")
                                }
                            }
                        }
                    }
                }

                item {
                    SkydownCard(
                        modifier = Modifier.testTag("settings.membership.section"),
                    ) {
                        SectionHeader("Membership")
                        Text(
                            text = "Aktueller Plan, Billing-Klarheit und Restore an einem Ort.",
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        )
                        if (uiState.isLoggedIn) {
                            val membershipState = if (uiState.aiAccessEnabled) "Aktiv" else "Eingeschraenkt"
                            Text(
                                text = "Status: $membershipState",
                                modifier = Modifier.padding(top = 10.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
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
                                    shape = RoundedCornerShape(18.dp),
                                ) {
                                    Text(if (uiState.isOwner) "Plan verwalten" else "Billing-Hilfe")
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
                                    shape = RoundedCornerShape(18.dp),
                                ) {
                                    Text("Restore pruefen")
                                }
                            }
                        } else {
                            Text(
                                text = "Melde dich an, um Membership-Status und Billing zu sehen.",
                                modifier = Modifier.padding(top = 10.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            )
                        }
                    }
                }

                item {
                    SkydownCard {
                        SectionHeader("Owner-Bereich")
                        Text(
                            text = if (uiState.isOwner) {
                                "Geschuetzte Steuerung fuer Revenue, Nutzer und Runtime."
                            } else {
                                "Geschuetzter Owner-Bereich."
                            },
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        )
                        if (uiState.isOwner) {
                            OwnerCommandCenterCard(
                                isOwner = uiState.isOwner,
                                paymentStatus = "$visiblePaymentMethodCount Zahlungsrouten",
                                userStatus = "${uiState.managedUsers.size} Konten",
                                headerStatus = "${screenHeaderSettings.configuredCount} Header",
                                aiStatus = if (uiState.aiRuntimeSettings.costGuardEnabled) "AI Guard aktiv" else "AI Guard pruefen",
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
                                onClick = onOpenOrders,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                shape = RoundedCornerShape(18.dp),
                            ) {
                                Text("Bestellungen oeffnen")
                            }
                        } else {
                            SettingsLockedHint(
                                text = "Nicht verfuegbar fuer dieses Konto.",
                                modifier = Modifier.padding(top = 10.dp),
                            )
                        }

                        if (uiState.isOwner) {
                            Column(
                                modifier = Modifier.padding(top = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                AdminWorkspaceSection.entries.forEach { section ->
                                    AdminWorkspaceListRow(
                                        section = section,
                                        detailText = adminWorkspaceStatusText(
                                            section = section,
                                            uiState = uiState,
                                            visiblePaymentMethodCount = visiblePaymentMethodCount,
                                            publishedArtistPageCount = publishedArtistPageCount,
                                            configuredScreenHeaderCount = screenHeaderSettings.configuredCount,
                                        ),
                                        onClick = {
                                            activeAdminWorkspaceKey = section.name
                                            showAdminWorkspaceSheet = true
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                if (uiState.isLoggedIn) {
                    item {
                        SkydownCard {
                            SectionHeader("AI & Agent")
                            Text(
                                text = "Bot, Agent und Automation zentral steuern.",
                                modifier = Modifier.padding(top = 8.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            )
                            Row(
                                modifier = Modifier.padding(top = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                SettingsBadge(
                                    text = if (uiState.workflowAutomationSettings.isPrepared) "Automation bereit" else "Automation offen",
                                    icon = Icons.Default.Bolt,
                                    isActive = uiState.workflowAutomationSettings.isPrepared,
                                    onClick = {
                                        activeAdminWorkspaceKey = AdminWorkspaceSection.Automation.name
                                        showAdminWorkspaceSheet = true
                                    },
                                )
                                SettingsBadge(
                                    text = if (uiState.agentProfileSettings.isConfigured) "Skills aktiv" else "Skills offen",
                                    icon = Icons.Default.Settings,
                                    isActive = uiState.agentProfileSettings.isConfigured,
                                    onClick = {
                                        activeAdminWorkspaceKey = AdminWorkspaceSection.Automation.name
                                        showAdminWorkspaceSheet = true
                                    },
                                )
                                SettingsBadge(
                                    text = if (uiState.manusByosSettings.isEnabled && uiState.manusByosSettings.hasApiKey) {
                                        "Manus BYOS aktiv"
                                    } else {
                                        "Manus BYOS aus"
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
                                            text = workflowName,
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
                                shape = RoundedCornerShape(18.dp),
                            ) {
                                Text("Agent-Service verwalten")
                            }
                        }
                    }
                }

                item {
                    SkydownCard {
                        val notificationsEnabledToast = stringResource(R.string.settings_notifications_toast_enabled)
                        val notificationsEnableInSettingsToast = stringResource(R.string.settings_notifications_toast_enable_in_settings)
                        val notificationsManageInSettingsToast = stringResource(R.string.settings_notifications_toast_manage_in_settings)

                        SectionHeader("System")
                        Text(
                            text = stringResource(R.string.settings_system_language_value, uiState.language),
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        )
                        Text(
                            text = stringResource(R.string.settings_supported_languages_summary),
                            modifier = Modifier.padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        )
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
                    }
                }

                item {
                    SkydownCard {
                        SectionHeader("Theme")
                        Text(
                            text = "Aktuell: ${uiState.colorScheme.label}",
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        )
                        AppearanceMode.entries.forEach { scheme ->
                            AppearanceChoiceRow(
                                title = scheme.label,
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
                            shape = RoundedCornerShape(18.dp),
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
                            shape = RoundedCornerShape(18.dp),
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
                            shape = RoundedCornerShape(18.dp),
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
                            shape = RoundedCornerShape(18.dp),
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
                            shape = RoundedCornerShape(18.dp),
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
                            shape = RoundedCornerShape(18.dp),
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
                            shape = RoundedCornerShape(18.dp),
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
                            shape = RoundedCornerShape(18.dp),
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
                                text = "Rechtliches (Owner)",
                                modifier = Modifier.padding(top = 14.dp),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "Diese Module steuern AGB, Datenschutz und Nutzungsbedingungen appweit und koennen ohne App-Release gepflegt werden.",
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
                                label = { Text("Brandname") },
                                placeholder = { Text("z. B. SkyOS") },
                                singleLine = true,
                            )

                            OutlinedTextField(
                                value = legalOperatorNameDraft,
                                onValueChange = { legalOperatorNameDraft = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                label = { Text("Betreiber / Vertragspartner") },
                                placeholder = { Text("z. B. Yang D. Nash - Skydown") },
                                singleLine = true,
                            )

                            OutlinedTextField(
                                value = legalRightsHolderNameDraft,
                                onValueChange = { legalRightsHolderNameDraft = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                label = { Text("Rechteinhaber") },
                                placeholder = { Text("z. B. Yang D. Nash - Skydown") },
                                singleLine = true,
                            )

                            OutlinedTextField(
                                value = legalSupportEmailDraft,
                                onValueChange = { legalSupportEmailDraft = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                label = { Text("Support E-Mail") },
                                placeholder = { Text(stringResource(R.string.settings_legal_support_email_placeholder)) },
                                singleLine = true,
                            )

                            OutlinedTextField(
                                value = legalLastUpdatedLabelDraft,
                                onValueChange = { legalLastUpdatedLabelDraft = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                label = { Text("Zuletzt aktualisiert") },
                                placeholder = { Text("z. B. 12. April 2026") },
                                singleLine = true,
                            )

                            OutlinedTextField(
                                value = legalImprintReferenceDraft,
                                onValueChange = { legalImprintReferenceDraft = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                label = { Text("Impressum-Hinweis") },
                                minLines = 3,
                            )

                            OutlinedTextField(
                                value = legalMasterNumberMeaningDraft,
                                onValueChange = { legalMasterNumberMeaningDraft = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                label = { Text("Meisterzahl 22 - Bedeutung") },
                                minLines = 3,
                            )

                            OutlinedTextField(
                                value = legalBrandManifestoDraft,
                                onValueChange = { legalBrandManifestoDraft = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                label = { Text("Wer wir sind (Manifest)") },
                                minLines = 6,
                            )

                            OutlinedTextField(
                                value = legalSymbolicNumericCodeDraft,
                                onValueChange = { legalSymbolicNumericCodeDraft = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                label = { Text("Symbolcode (numerisch)") },
                                placeholder = { Text("z. B. 1337-514-731") },
                                singleLine = true,
                            )

                            OutlinedTextField(
                                value = legalSymbolicLeetCodeDraft,
                                onValueChange = { legalSymbolicLeetCodeDraft = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                label = { Text("Leet-Code") },
                                placeholder = { Text("z. B. 7H3_F4LL_0F_H34/3N") },
                                singleLine = true,
                            )

                            OutlinedTextField(
                                value = legalSymbolicCodeExplanationDraft,
                                onValueChange = { legalSymbolicCodeExplanationDraft = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                label = { Text("Code-Erklaerung") },
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
                                shape = RoundedCornerShape(18.dp),
                            ) {
                                Text("Rechtliches speichern")
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
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        text = activeAdminWorkspace.label,
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
                Text("Konto loeschen")
            },
            text = {
                Text("Moechtest du dein Konto unwiderruflich loeschen?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteAccountDialog.value = false
                        viewModel.deleteAccount()
                    },
                    enabled = !uiState.isDeletingAccount,
                ) {
                    Text("Konto loeschen", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteAccountDialog.value = false },
                ) {
                    Text("Abbrechen")
                }
            },
        )
    }
}

@Composable
private fun PaymentProviderAdminCard(
    title: String,
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
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = when (title) {
                        "PayPal" -> if (connected) "Hinterlegt" else "Noch nicht hinterlegt"
                        else -> if (connected) "Verbunden" else "Nicht verbunden"
                    },
                    color = if (connected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    },
                )
            }
            SettingsBadge(
                text = if (enabledInCheckout) "Im Checkout sichtbar" else "Ausgeblendet",
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
            title = "Fuer Kunden im Checkout anzeigen",
            body = "Erst nach der Verbindung sichtbar schalten.",
            checked = enabledInCheckout,
            onCheckedChange = onToggleEnabled,
            modifier = Modifier.padding(top = 12.dp),
            enabled = connected,
        )

        Row(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = onSaveConnection,
                shape = RoundedCornerShape(18.dp),
            ) {
                Text(
                    when (title) {
                        "PayPal" -> if (connected) "PayPal aktualisieren" else "PayPal hinterlegen"
                        else -> if (connected) "Verbindung aktualisieren" else "Verbinden"
                    },
                )
            }
            onDisconnect?.let { disconnect ->
                OutlinedButton(
                    onClick = disconnect,
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text(if (title == "PayPal") "Entfernen" else "Trennen")
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
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Sicheres Stripe-Backend",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (status.isReady) "Backend bereit" else "Backend noch unvollstaendig",
                    color = if (status.isReady) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    },
                )
            }
            SettingsBadge(
                text = if (status.isReady) "Backend bereit" else "Setup fehlt",
                icon = Icons.Default.CheckCircle,
                isActive = status.isReady,
            )
        }

        Row(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SettingsBadge(
                text = if (status.hasSecretKey) "Secret Key gesetzt" else "Secret Key fehlt",
                icon = Icons.Default.CheckCircle,
                isActive = status.hasSecretKey,
            )
            SettingsBadge(
                text = if (status.hasWebhookSecret) "Webhook Secret gesetzt" else "Webhook Secret fehlt",
                icon = Icons.Default.CheckCircle,
                isActive = status.hasWebhookSecret,
            )
        }

        Text(
            text = "Die Werte werden nur serverseitig gespeichert. Live- oder Test-Keys sind moeglich, leere Felder lassen bestehende Secrets unveraendert.",
            modifier = Modifier.padding(top = 12.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
        )

        OutlinedTextField(
            value = stripeSecretKey,
            onValueChange = onStripeSecretKeyChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            label = { Text("Stripe Secret Key") },
            placeholder = { Text("sk_live_..., rk_live_..., sk_test_... oder rk_test_...") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
        )

        OutlinedTextField(
            value = stripeWebhookSecret,
            onValueChange = onStripeWebhookSecretChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            label = { Text("Stripe Webhook Secret") },
            placeholder = { Text("whsec_...") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
        )

        Button(
            onClick = onSave,
            modifier = Modifier.padding(top = 12.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text("Sicher speichern")
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
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Bankueberweisung",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (configured) "Bankdaten hinterlegt" else "Noch nicht hinterlegt",
                    color = if (configured) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    },
                )
            }
            SettingsBadge(
                text = if (enabledInCheckout) "Im Checkout sichtbar" else "Ausgeblendet",
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
            label = { Text("Kontoinhaber") },
            singleLine = true,
        )
        OutlinedTextField(
            value = iban,
            onValueChange = onIbanChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            label = { Text("IBAN") },
            singleLine = true,
        )
        OutlinedTextField(
            value = bic,
            onValueChange = onBicChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            label = { Text("BIC") },
            singleLine = true,
        )
        OutlinedTextField(
            value = bankName,
            onValueChange = onBankNameChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            label = { Text("Bankname") },
            singleLine = true,
        )
        OutlinedTextField(
            value = paymentInstructions,
            onValueChange = onPaymentInstructionsChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            label = { Text("Zahlungsanweisung") },
            minLines = 2,
            maxLines = 3,
        )

        SettingsToggleRow(
            title = "Fuer Kunden im Checkout anzeigen",
            body = "Erst aktivieren, wenn die Bankdaten vollstaendig sind.",
            checked = enabledInCheckout,
            onCheckedChange = onToggleEnabled,
            modifier = Modifier.padding(top = 12.dp),
            enabled = configured,
        )

        Button(
            onClick = onSave,
            modifier = Modifier.padding(top = 12.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text(if (configured) "Bankdaten aktualisieren" else "Bankdaten hinterlegen")
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
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Profil",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Username, Kurzinfo und Links.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
            }
        }

        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Benutzername") },
            singleLine = true,
        )
        OutlinedTextField(
            value = profileTagline,
            onValueChange = onProfileTaglineChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Kurzinfo") },
            singleLine = true,
        )
        OutlinedTextField(
            value = profileBio,
            onValueChange = onProfileBioChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Bio") },
            minLines = 3,
            maxLines = 5,
        )
        OutlinedTextField(
            value = instagramHandle,
            onValueChange = onInstagramHandleChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Instagram") },
            singleLine = true,
        )
        OutlinedTextField(
            value = whatsApp,
            onValueChange = onWhatsAppChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("WhatsApp") },
            singleLine = true,
        )

        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving,
            shape = RoundedCornerShape(18.dp),
        ) {
            Text(if (isSaving) "Profil wird gespeichert..." else "Profil speichern")
        }
    }
}

@Composable
private fun SettingsOverviewCard(
    uiState: SettingsUiState,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                shape = shape,
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = if (uiState.isLoggedIn) "${uiState.username} · Einstellungen aktiv" else "SkyOS Einstellungen",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.weight(1f))
        }

        Text(
            text = "Konto, Rechtliches, Anzeige und Support bleiben in einem klaren Flow gebuendelt.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsBadge(
                text = if (uiState.isLoggedIn) "Konto aktiv" else "Gast",
                icon = Icons.Default.Person,
                isActive = uiState.isLoggedIn,
            )
            SettingsBadge(
                text = if (uiState.notificationsEnabled) "Hinweise an" else "Hinweise aus",
                icon = Icons.Default.Notifications,
                isActive = uiState.notificationsEnabled,
            )
            SettingsBadge(
                text = uiState.colorScheme.label,
                icon = Icons.Default.Palette,
                isActive = true,
            )
        }

        if (uiState.isOwner) {
            SettingsBadge(
                text = "Owner aktiv",
                icon = Icons.Default.CheckCircle,
                isActive = true,
                modifier = Modifier.padding(top = 10.dp),
            )
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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SettingsUtilityChip(
            label = if (isOwner) "Zahlungen" else "Support",
            icon = if (isOwner) Icons.Default.CreditCard else Icons.Default.Email,
            accent = MaterialTheme.colorScheme.primary,
            onClick = onOpenPayments,
            modifier = Modifier.weight(1f),
        )
        SettingsUtilityChip(
            label = "Datenschutz",
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
    val shape = RoundedCornerShape(999.dp)
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
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
                    ),
                ),
            )
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = if (isOwner) "Owner-Steuerung" else "Owner-Steuerung gesperrt",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = if (isOwner) {
                            "Direkte Kontrollpunkte fuer Release, Commerce und KI-Kosten."
                        } else {
                            "Nur das feste Owner-Konto kann diese Live-Systeme veraendern."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }

                SettingsBadge(
                    text = if (isOwner) "Aktiv" else "Gesperrt",
                    icon = if (isOwner) Icons.Default.CheckCircle else Icons.Default.Lock,
                    isActive = isOwner,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OwnerCommandSignalButton(
                    title = "Rollen",
                    detail = userStatus,
                    icon = Icons.Default.Person,
                    enabled = isOwner,
                    onClick = onOpenUsers,
                    modifier = Modifier.weight(1f),
                )
                OwnerCommandSignalButton(
                    title = "Zahlungen",
                    detail = paymentStatus,
                    icon = Icons.Default.CreditCard,
                    enabled = isOwner,
                    onClick = onOpenPayments,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OwnerCommandSignalButton(
                    title = "Header",
                    detail = headerStatus,
                    icon = Icons.Default.Palette,
                    enabled = isOwner,
                    onClick = onOpenHeaders,
                    modifier = Modifier.weight(1f),
                )
                OwnerCommandSignalButton(
                    title = "KI Schutz",
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
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = if (enabled) 0.9f else 0.46f))
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
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
            .clip(RoundedCornerShape(18.dp))
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
            .clip(RoundedCornerShape(999.dp))
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.68f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
    val label: String,
    val subtitle: String,
    val icon: ImageVector,
) {
    Payments(
        label = "Zahlungen",
        subtitle = "Provider verbinden und fuer den Checkout sichtbar schalten.",
        icon = Icons.Default.CreditCard,
    ),
    Users(
        label = "User",
        subtitle = "Rollen, KI-Zugriff, Tageslimits und History pro Konto steuern.",
        icon = Icons.Default.Person,
    ),
    Artists(
        label = "Artists",
        subtitle = "Artist-Seiten pflegen und Editor-Rechte pro Artist zuteilen.",
        icon = Icons.Default.LibraryMusic,
    ),
    Headers(
        label = "Header",
        subtitle = "Hero-Bilder und Texte fuer Home, Music, Shop und Video pflegen.",
        icon = Icons.Default.Palette,
    ),
    Shopify(
        label = "Shopify",
        subtitle = "Owner-Quelle fuer Store-Domain, Token und Merch-Sync pflegen.",
        icon = Icons.Default.ShoppingBag,
    ),
    Commerce(
        label = "Versand",
        subtitle = "Versandkosten, MwSt. und Rechnungsdaten gesammelt pflegen.",
        icon = Icons.Default.LocalShipping,
    ),
    Visuals(
        label = "Visuals",
        subtitle = "Drive-Link, Referenzhinweise und Namensschema pflegen.",
        icon = Icons.Default.Palette,
    ),
    Automation(
        label = "Agent-Service",
        subtitle = "Persoenliche Automation und Agent-Skills pro Konto pflegen.",
        icon = Icons.Default.Bolt,
    ),
    AiPrompts(
        label = "KI Prompts",
        subtitle = "Serverseitige Anweisungen fuer Bot, Visuals und Agent zentral pflegen.",
        icon = Icons.Default.Settings,
    ),
    MembershipOps(
        label = "Membership-Steuerung",
        subtitle = "KPIs, Trends, Experimente und Learnings fuer Revenue.",
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
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(16.dp))
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
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = section.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = section.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = detailText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                Text(
                    text = "Oeffnen",
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
        shape = RoundedCornerShape(999.dp),
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
            text = section.label,
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
        shape = RoundedCornerShape(18.dp),
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
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = section.icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = section.label,
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
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = section.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = section.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = section.subtitle,
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
            text = "Rollen im System",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Column(
            modifier = Modifier.padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            UserRole.entries.forEach { role ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
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
        mutableStateOf(page.editorUids.toSet())
    }

    SkydownCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
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
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SettingsBadge(
                        text = if (page.hasCustomPresentation) "Live" else "Platzhalter",
                        icon = Icons.Default.LibraryMusic,
                        isActive = page.hasCustomPresentation,
                    )
                    SettingsBadge(
                        text = "${selectedEditorUids.size} Editoren",
                        icon = Icons.Default.Person,
                        isActive = selectedEditorUids.isNotEmpty(),
                    )
                }
            }

            if (users.isEmpty()) {
                Text(
                    text = "Sobald weitere Konten registriert sind, kannst du hier Editoren fuer diese Artist-Seite zuweisen.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                Text(
                    text = "Editoren",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    users.forEach { user ->
                        val userId = user.id.orEmpty()
                        val isSelected = selectedEditorUids.contains(userId)

                        Button(
                            onClick = {
                                selectedEditorUids = if (isSelected) {
                                    selectedEditorUids - userId
                                } else {
                                    selectedEditorUids + userId
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
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
                            editorUids = selectedEditorUids.toList().sorted(),
                            updatedAtEpochMillis = System.currentTimeMillis(),
                            isPlaceholder = false,
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
            ) {
                Text("Editoren speichern")
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
                verticalArrangement = Arrangement.spacedBy(4.dp),
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
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SettingsBadge(
                    text = resolvedRole.displayTitle,
                    icon = Icons.Default.Person,
                    isActive = true,
                )
                if (isCurrentUser) {
                    SettingsBadge(
                        text = "Du",
                        icon = Icons.Default.CheckCircle,
                        isActive = true,
                    )
                }
                if (isSaving) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                        Text(
                            text = "Speichert...",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        )
                    }
                } else if (hasPendingChanges) {
                    SettingsBadge(
                        text = "Entwurf",
                        icon = Icons.Default.Bolt,
                        isActive = false,
                    )
                } else if (successfulSaveCount > 0) {
                    SettingsBadge(
                        text = "Gespeichert",
                        icon = Icons.Default.CheckCircle,
                        isActive = true,
                    )
                }
            }
        }

        Text(
            text = "Rolle",
            modifier = Modifier.padding(top = 14.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )

        LazyRow(
            modifier = Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(UserRole.entries.toList()) { role ->
                val selected = resolvedRole == role
                val roleSelectionEnabled = !isCurrentUser &&
                    !user.isPlatformOwner &&
                    (role != UserRole.Owner || canAssignOwnerRoleToUser)
                if (selected) {
                    Button(
                        onClick = { selectedRole = role.rawValue },
                        shape = RoundedCornerShape(999.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Text(role.displayTitle)
                    }
                } else {
                    OutlinedButton(
                        onClick = { selectedRole = role.rawValue },
                        enabled = roleSelectionEnabled,
                        shape = RoundedCornerShape(999.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                text = "Das Owner-Konto ist fest an nash.lioncorna@gmail.com gebunden und bleibt immer Owner.",
                modifier = Modifier.padding(top = 10.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
        } else if (isCurrentUser) {
            Text(
                text = "Dein eigenes Konto bleibt vor versehentlichen Rollenwechseln geschuetzt. Limits kannst du hier trotzdem anpassen.",
                modifier = Modifier.padding(top = 10.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
        } else if (!canAssignOwnerRoleToUser) {
            Text(
                text = "Owner ist nur fuer das feste Hauptkonto moeglich. Fuer KI-Zugang nutze bitte Admin und aktiviere die KI-Freigabe.",
                modifier = Modifier.padding(top = 10.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
        }

        when (resolvedRole) {
            UserRole.Owner -> {
                Text(
                    text = "Owner-Kontrolle: Shopify, Zahlungen, Rollen, KI-Defaults und Recovery laufen nur ueber dieses Konto.",
                    modifier = Modifier.padding(top = 10.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
            }

            UserRole.Admin -> {
                Text(
                    text = "Zugewiesene Funktionen",
                    modifier = Modifier.padding(top = 14.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )

                SettingsToggleRow(
                    title = "Music verwalten",
                    body = "Beats, Releases und Upload-Freigaben pflegen.",
                    checked = canManageMusicCatalog,
                    onCheckedChange = { canManageMusicCatalog = it },
                    modifier = Modifier.padding(top = 10.dp),
                )
                SettingsToggleRow(
                    title = "Video verwalten",
                    body = "Video Hub, Uploads und Home-Highlights steuern.",
                    checked = canManageVideoCatalog,
                    onCheckedChange = { canManageVideoCatalog = it },
                    modifier = Modifier.padding(top = 10.dp),
                )
                SettingsToggleRow(
                    title = "Profile moderieren",
                    body = "Profile und Galerie-Inhalte fuer Support und Moderation einsehen.",
                    checked = canModerateProfiles,
                    onCheckedChange = { canModerateProfiles = it },
                    modifier = Modifier.padding(top = 10.dp),
                )
            }

            UserRole.Subadmin, UserRole.User -> {
                Text(
                    text = if (resolvedRole == UserRole.Subadmin) "Kontingentmodell" else "Kontingent",
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(listOf(UserQuotaPlan.Creator, UserQuotaPlan.Studio)) { plan ->
                            val isSelected = resolvedQuotaPlan == plan
                            if (isSelected) {
                                Button(
                                    onClick = { selectedQuotaPlan = plan.rawValue },
                                    shape = RoundedCornerShape(999.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                ) {
                                    Text(plan.displayTitle)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { selectedQuotaPlan = plan.rawValue },
                                    shape = RoundedCornerShape(999.dp),
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
            title = "KI fuer dieses Konto aktiv",
            body = "Wenn aus, sind Bot, Visuals und Agent fuer dieses Konto gesperrt.",
            checked = aiAccessEnabled,
            onCheckedChange = { aiAccessEnabled = it },
            modifier = Modifier.padding(top = 14.dp),
        )

        Text(
            text = "Tageslimits",
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
            label = { Text("Bot pro Tag") },
            singleLine = true,
        )
        OutlinedTextField(
            value = visualLimitDraft,
            onValueChange = { visualLimitDraft = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            label = { Text("Visuals pro Tag") },
            singleLine = true,
        )
        OutlinedTextField(
            value = agentLimitDraft,
            onValueChange = { agentLimitDraft = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            label = { Text("Agent pro Tag") },
            singleLine = true,
        )

        Text(
            text = "History-Aufbewahrung",
            modifier = Modifier.padding(top = 14.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )

        LazyRow(
            modifier = Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(listOf(1, 3, 7, 30)) { option ->
                val isSelected = historyRetentionDays == option
                if (isSelected) {
                    Button(
                        onClick = { historyRetentionDays = option },
                        shape = RoundedCornerShape(999.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Text(historyOptionLabel(option))
                    }
                } else {
                    OutlinedButton(
                        onClick = { historyRetentionDays = option },
                        shape = RoundedCornerShape(999.dp),
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
            shape = RoundedCornerShape(18.dp),
            enabled = !isSaving && hasPendingChanges,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                    text = "Rolle, Rechte und KI-Limits werden gerade serverseitig synchronisiert.",
                    modifier = Modifier.padding(top = 10.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
            }

            hasPendingChanges -> {
                Text(
                    text = "Ungespeicherte Aenderungen. Erst nach dem Speichern werden Claims und Limits live uebernommen.",
                    modifier = Modifier.padding(top = 10.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
            }

            successfulSaveCount > 0 -> {
                Text(
                    text = "Gespeichert. Die letzte Aenderung wurde serverseitig bestaetigt.",
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
