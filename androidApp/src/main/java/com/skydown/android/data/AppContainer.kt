package com.skydown.android.data

import com.skydown.android.data.repository.AndroidAuthRepository
import com.skydown.android.data.repository.AndroidMerchandiseRepository
import com.skydown.android.data.repository.AndroidMusicRepository
import com.skydown.android.data.repository.AndroidOrderRepository
import com.skydown.android.data.repository.FakeAuthRepository
import com.skydown.android.data.repository.FakeMerchandiseRepository
import com.skydown.android.data.repository.FakeMusicRepository
import com.skydown.android.data.repository.FakeOrderRepository
import com.skydown.shared.repository.AuthRepository
import com.skydown.shared.repository.MerchandiseRepository
import com.skydown.shared.repository.MusicRepository
import com.skydown.shared.repository.OrderRepository
import com.skydown.shared.service.AuthService
import com.skydown.shared.service.MerchandiseService
import com.skydown.shared.service.MusicService
import com.skydown.shared.service.OrderService
import kotlinx.coroutines.flow.StateFlow

object AppContainer {
    private const val useFirebaseRepositories = true

    val aiChatClient: AiChatClient by lazy { AiChatClient() }
    val aiImageClient: AiImageClient by lazy { AiImageClient() }
    val aiUsageAuthorizationClient: AiUsageAuthorizationClient by lazy { AiUsageAuthorizationClient() }
    val agentClient: AgentClient by lazy { AgentClient() }
    val shopifyMerchSyncClient: ShopifyMerchSyncClient by lazy { ShopifyMerchSyncClient() }
    val shopifyPublicCatalogClient: ShopifyPublicCatalogClient by lazy { ShopifyPublicCatalogClient() }
    val merchOrderPaymentClient: MerchOrderPaymentClient by lazy { MerchOrderPaymentClient() }
    val hostedMerchCheckoutClient: HostedMerchCheckoutClient by lazy { HostedMerchCheckoutClient() }
    val userProfileRepository: UserProfileRepository by lazy { UserProfileRepository() }
    val paymentMethodsRepository: PaymentMethodsRepository by lazy { PaymentMethodsRepository() }
    val stripeBackendSecretsRepository: StripeBackendSecretsRepository by lazy { StripeBackendSecretsRepository() }
    val commerceSettingsRepository: CommerceSettingsRepository by lazy { CommerceSettingsRepository() }
    val screenHeaderSettingsRepository: ScreenHeaderSettingsRepository by lazy { ScreenHeaderSettingsRepository() }
    val shopifyAdminSettingsRepository: ShopifyAdminSettingsRepository by lazy { ShopifyAdminSettingsRepository() }
    val adminUserManagementRepository: AdminUserManagementRepository by lazy { AdminUserManagementRepository() }
    val merchStoreStatusRepository: MerchStoreStatusRepository by lazy { MerchStoreStatusRepository() }

    val authRepository: AuthRepository by lazy {
        if (useFirebaseRepositories) AndroidAuthRepository() else FakeAuthRepository()
    }

    val merchandiseRepository: MerchandiseRepository by lazy {
        if (useFirebaseRepositories) AndroidMerchandiseRepository() else FakeMerchandiseRepository()
    }

    val orderRepository: OrderRepository by lazy {
        if (useFirebaseRepositories) AndroidOrderRepository() else FakeOrderRepository()
    }

    val musicRepository: MusicRepository by lazy {
        if (useFirebaseRepositories) AndroidMusicRepository() else FakeMusicRepository()
    }

    val authService: AuthService by lazy { AuthService(authRepository) }
    val merchandiseService: MerchandiseService by lazy { MerchandiseService(merchandiseRepository) }
    val orderService: OrderService by lazy { OrderService(orderRepository) }
    val musicService: MusicService by lazy { MusicService(musicRepository) }
    val currentUser: StateFlow<com.skydown.shared.model.User?> = AppSessionStore.currentUser
    val aiEnabled: StateFlow<Boolean> = AppFeatureFlagsStore.isAiEnabled

    suspend fun refreshCurrentUser() {
        runCatching { authService.currentUser() }
            .onSuccess(AppSessionStore::update)
    }
}
