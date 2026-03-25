import Foundation

@MainActor
final class AppServices: ObservableObject {
    let authService: AuthServicing
    let aiChatService: AIChatServicing
    let agentChatService: AgentChatServicing
    let featureFlags: FeatureFlagsService
    let merchandiseService: MerchandiseServicing
    let orderService: OrderServicing
    let musicService: MusicServicing

    let authManager: AuthManager
    let cartViewModel: CartViewModel

    init(
        authService: AuthServicing = FirebaseAuthService(),
        aiChatService: AIChatServicing = FirebaseAIChatService(),
        agentChatService: AgentChatServicing = FirebaseFunctionsAgentService(),
        merchandiseService: MerchandiseServicing = FirebaseMerchandiseService(),
        orderService: OrderServicing = FirebaseOrderService(),
        musicService: MusicServicing = SpotifyMusicService()
    ) {
        self.authService = authService
        self.aiChatService = aiChatService
        self.agentChatService = agentChatService
        self.featureFlags = FeatureFlagsService()
        self.merchandiseService = merchandiseService
        self.orderService = orderService
        self.musicService = musicService

        let authManager = AuthManager(authService: authService)
        self.authManager = authManager
        self.cartViewModel = CartViewModel(
            authManager: authManager,
            orderService: orderService
        )
    }
}
