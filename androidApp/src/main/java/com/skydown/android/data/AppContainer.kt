package com.skydown.android.data

import com.skydown.android.data.repository.AndroidAuthRepository
import com.skydown.android.data.repository.AndroidMerchandiseRepository
import com.skydown.android.data.repository.AndroidMusicRepository
import com.skydown.android.data.repository.AndroidOrderRepository
import com.skydown.android.data.repository.UiTestAiImageClient
import com.skydown.android.data.repository.UiTestMerchandiseRepository
import com.skydown.android.data.repository.UiTestMusicRepository
import com.skydown.android.data.repository.UiTestVideoHubService
import com.skydown.shared.repository.AuthRepository
import com.skydown.shared.repository.MerchandiseRepository
import com.skydown.shared.repository.MusicRepository
import com.skydown.shared.repository.OrderRepository
import com.skydown.shared.model.User
import com.skydown.shared.service.AuthService
import com.skydown.shared.service.MerchandiseService
import com.skydown.shared.service.MusicService
import com.skydown.shared.service.OrderService
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
    val agentClient: AgentClient by lazy { AgentClient() }
    val shopifyMerchSyncClient: ShopifyMerchSyncClient by lazy { ShopifyMerchSyncClient() }
    val shopifyPublicCatalogClient: ShopifyPublicCatalogClient by lazy { ShopifyPublicCatalogClient() }
    val merchOrderPaymentClient: MerchOrderPaymentClient by lazy { MerchOrderPaymentClient() }
    val hostedMerchCheckoutClient: HostedMerchCheckoutClient by lazy { HostedMerchCheckoutClient() }
    val userProfileRepository: UserProfileRepository by lazy { UserProfileRepository() }
    val aiPromptSettingsRepository: AiPromptSettingsRepository by lazy { AiPromptSettingsRepository() }
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
            return
        }

        runCatching { authService.currentUser() }
            .onSuccess(AppSessionStore::update)
    }
}
