package com.nash.skyos.data

import com.nash.skyos.data.repository.AndroidAuthRepository
import com.nash.skyos.data.repository.AndroidMerchandiseRepository
import com.nash.skyos.data.repository.AndroidMusicRepository
import com.nash.skyos.data.repository.AndroidOrderRepository
import com.nash.skyos.data.repository.UiTestAiImageClient
import com.nash.skyos.data.repository.UiTestMerchandiseRepository
import com.nash.skyos.data.repository.UiTestMusicRepository
import com.nash.skyos.data.repository.UiTestVideoHubService
import com.skydown.shared.repository.AuthRepository
import com.skydown.shared.repository.MerchandiseRepository
import com.skydown.shared.repository.MusicRepository
import com.skydown.shared.repository.OrderRepository
import com.skydown.shared.model.User
import com.skydown.shared.service.AuthService
import com.skydown.shared.service.MerchandiseService
import com.skydown.shared.service.MusicService
import com.skydown.shared.service.OrderService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object AppContainer {
    @Volatile
    private var merchandiseRepositoryOverride: MerchandiseRepository? = null
    @Volatile
    private var musicRepositoryOverride: MusicRepository? = null
    @Volatile
    private var aiImageClientOverride: AiImageClient? = null
    @Volatile
    private var videoHubServiceOverride: VideoHubService? = null
    @Volatile
    private var currentUserOverride: User? = null

    private val defaultAiChatClient: AiChatClient by lazy { AiChatClient() }
    private val defaultAiImageClient: AiImageClient by lazy { AiImageClient() }
    private val defaultVideoHubService: VideoHubService by lazy { VideoHubService() }

    val aiChatClient: AiChatClient
        get() = defaultAiChatClient
    val aiImageClient: AiImageClient
        get() = aiImageClientOverride ?: defaultAiImageClient
    val aiUsageAuthorizationClient: AiUsageAuthorizationClient by lazy { AiUsageAuthorizationClient() }
    private val pushTokenSyncClient: PushTokenSyncClient by lazy { PushTokenSyncClient() }
    val agentClient: AgentClient by lazy { AgentClient() }
    val shopifyMerchSyncClient: ShopifyMerchSyncClient by lazy { ShopifyMerchSyncClient() }
    val shopifyPublicCatalogClient: ShopifyPublicCatalogClient by lazy { ShopifyPublicCatalogClient() }
    val merchOrderPaymentClient: MerchOrderPaymentClient by lazy { MerchOrderPaymentClient() }
    val hostedMerchCheckoutClient: HostedMerchCheckoutClient by lazy { HostedMerchCheckoutClient() }
    val userProfileRepository: UserProfileRepository by lazy { UserProfileRepository() }
    val aiPromptSettingsRepository: AiPromptSettingsRepository by lazy { AiPromptSettingsRepository() }
    val aiOwnerInspirationRepository: AiOwnerInspirationRepository by lazy { AiOwnerInspirationRepository() }
    val aiRuntimeSettingsRepository: AiRuntimeSettingsRepository by lazy { AiRuntimeSettingsRepository() }
    val paymentMethodsRepository: PaymentMethodsRepository by lazy { PaymentMethodsRepository() }
    val stripeBackendSecretsRepository: StripeBackendSecretsRepository by lazy { StripeBackendSecretsRepository() }
    val commerceSettingsRepository: CommerceSettingsRepository by lazy { CommerceSettingsRepository() }
    val legalContentRepository: LegalContentRepository by lazy { LegalContentRepository() }
    val editableImageAssetRepository: EditableImageAssetRepository by lazy { EditableImageAssetRepository() }
    val screenHeaderSettingsRepository: ScreenHeaderSettingsRepository by lazy { ScreenHeaderSettingsRepository() }
    val shopifyAdminSettingsRepository: ShopifyAdminSettingsRepository by lazy { ShopifyAdminSettingsRepository() }
    val adminUserManagementRepository: AdminUserManagementRepository by lazy { AdminUserManagementRepository() }
    val merchStoreStatusRepository: MerchStoreStatusRepository by lazy { MerchStoreStatusRepository() }
    val membershipOpsAdminRepository: MembershipOpsAdminRepository by lazy { MembershipOpsAdminRepository() }
    val aiFaqOwnerReviewRepository: AiFaqOwnerReviewRepository by lazy { AiFaqOwnerReviewRepository() }
    val taskRepository: TaskRepository by lazy { TaskRepository() }
    val noteRepository: NoteRepository by lazy { NoteRepository() }

    private val defaultAuthRepository: AuthRepository by lazy { AndroidAuthRepository() }
    private val defaultMerchandiseRepository: MerchandiseRepository by lazy { AndroidMerchandiseRepository() }
    private val defaultOrderRepository: OrderRepository by lazy { AndroidOrderRepository() }
    private val defaultMusicRepository: MusicRepository by lazy { AndroidMusicRepository() }

    val authRepository: AuthRepository
        get() = defaultAuthRepository
    val merchandiseRepository: MerchandiseRepository
        get() = merchandiseRepositoryOverride ?: defaultMerchandiseRepository
    val videoHubService: VideoHubService
        get() = videoHubServiceOverride ?: defaultVideoHubService
    val orderRepository: OrderRepository
        get() = defaultOrderRepository
    val musicRepository: MusicRepository
        get() = musicRepositoryOverride ?: defaultMusicRepository

    val authService: AuthService
        get() = AuthService(authRepository)
    val merchandiseService: MerchandiseService
        get() = MerchandiseService(merchandiseRepository)
    val orderService: OrderService
        get() = OrderService(orderRepository)
    val musicService: MusicService
        get() = MusicService(musicRepository)
    val currentUser: StateFlow<com.skydown.shared.model.User?> = AppSessionStore.currentUser
    val aiEnabled: StateFlow<Boolean> = AppFeatureFlagsStore.isAiEnabled
    private val _homeProductivitySheetRequest = MutableStateFlow<String?>(null)
    val homeProductivitySheetRequest: StateFlow<String?> = _homeProductivitySheetRequest
    val isUiTestCurrentUserOverrideActive: Boolean
        get() = currentUserOverride != null

    fun configureUiTestMode(
        useMockMerchandise: Boolean,
        useMockMusic: Boolean = false,
        useMockVideoHub: Boolean = false,
        useMockAiVisual: Boolean = false,
        currentUserOverride: User? = null,
    ) {
        merchandiseRepositoryOverride = if (useMockMerchandise) {
            UiTestMerchandiseRepository
        } else {
            null
        }
        musicRepositoryOverride = if (useMockMusic) {
            UiTestMusicRepository
        } else {
            null
        }
        videoHubServiceOverride = if (useMockVideoHub) {
            UiTestVideoHubService()
        } else {
            null
        }
        aiImageClientOverride = if (useMockAiVisual) {
            UiTestAiImageClient()
        } else {
            null
        }
        this.currentUserOverride = currentUserOverride
    }

    suspend fun refreshCurrentUser() {
        currentUserOverride?.let {
            AppSessionStore.update(it)
            runCatchingSyncPushToken(it.id)
            return
        }

        val user = try {
            authService.currentUser()
        } catch (_: Throwable) {
            null
        }
        AppSessionStore.update(user)
        runCatchingSyncPushToken(user?.id)
    }

    private suspend fun runCatchingSyncPushToken(uid: String?) {
        try {
            pushTokenSyncClient.syncIfPossible(uid)
        } catch (_: Throwable) {
            // Silent by design; token sync should not block session bootstrap.
        }
    }

    fun openHomeProductivitySheet(sheet: String) {
        _homeProductivitySheetRequest.value = sheet
    }

    fun clearHomeProductivitySheetRequest() {
        _homeProductivitySheetRequest.value = null
    }
}
