import Foundation

enum HostedCheckoutRedirectStatus: Equatable {
    case success
    case cancel
}

struct HostedCheckoutRedirectEvent: Equatable {
    let status: HostedCheckoutRedirectStatus
    let orderID: String?
    let sessionID: String?
}

@MainActor
final class HostedCheckoutRedirectStore: ObservableObject {
    @Published private(set) var latestEvent: HostedCheckoutRedirectEvent?

    func handle(_ url: URL) -> Bool {
        guard url.scheme?.lowercased() == "skydown" else {
            return false
        }

        let status: HostedCheckoutRedirectStatus
        switch url.host?.lowercased() {
        case "checkout-success":
            status = .success
        case "checkout-cancel":
            status = .cancel
        default:
            return false
        }

        let components = URLComponents(url: url, resolvingAgainstBaseURL: false)
        let orderID = components?.queryItems?.first { $0.name == "orderId" }?.value?.trimmedNonEmpty
        let sessionID = components?.queryItems?.first { $0.name == "sessionId" }?.value?.trimmedNonEmpty

        latestEvent = HostedCheckoutRedirectEvent(
            status: status,
            orderID: orderID,
            sessionID: sessionID
        )
        return true
    }

    func clear() {
        latestEvent = nil
    }
}

private extension String {
    var trimmedNonEmpty: String? {
        let trimmed = trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : trimmed
    }
}
