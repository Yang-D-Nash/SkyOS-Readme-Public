import Foundation

/// Context for sign-in entry points (mirrors Android `AuthEntryContext`) so the login surface can echo
/// a calm, benefit-led reason instead of a generic gate.
enum AuthEntryContext: String, Equatable, CaseIterable, Sendable {
    case standard
    case ai
    case merchShop
    case cart
    case settings
    case music
}

extension AuthEntryContext {
    /// Localization key + English fallback for the subtitle under the welcome line on `LoginView`.
    var loginSubtitleLocalization: (key: String, fallback: String) {
        switch self {
        case .standard:
            return ("auth.login.subtitle", "Sign in to access your account and creator tools.")
        case .ai:
            return ("auth.entry.subtitle.ai", "Save workflows and pick up exactly where you left off.")
        case .merchShop:
            return ("auth.entry.subtitle.merch", "Track orders and delivery in one place when you are ready.")
        case .cart:
            return ("auth.entry.subtitle.cart", "Secure your bag and finish checkout on your terms.")
        case .settings:
            return ("auth.entry.subtitle.settings", "Connect an account for personalized settings and support.")
        case .music:
            return ("auth.entry.subtitle.music", "Keep saves and listening history with your library.")
        }
    }
}
