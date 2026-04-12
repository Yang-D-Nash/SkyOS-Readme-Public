import Foundation
import FirebaseFunctions

enum AIUsageAuthorizationKind: String {
    case text
    case visual
    case agent
}

struct AIUsageAuthorizationResult {
    let role: String
    let remainingForKind: Int
    let limitForKind: Int
    let historyRetentionDays: Int
}

protocol AIUsageAuthorizing {
    func authorize(kind: AIUsageAuthorizationKind) async throws -> AIUsageAuthorizationResult
}

enum AIUsageAuthorizationError: LocalizedError {
    case invalidResponse

    var errorDescription: String? {
        switch self {
        case .invalidResponse:
            return "Die KI-Kostenkontrolle konnte nicht gelesen werden."
        }
    }
}

struct FirebaseFunctionsAIUsageService: AIUsageAuthorizing {
    private let functions: Functions

    init(functions: Functions = Functions.functions(region: "us-central1")) {
        self.functions = functions
    }

    func authorize(kind: AIUsageAuthorizationKind) async throws -> AIUsageAuthorizationResult {
        let result = try await functions.invokeCallable("authorizeAiUsage", payload: ["kind": kind.rawValue])

        guard let payload = result.data as? [String: Any] else {
            throw AIUsageAuthorizationError.invalidResponse
        }

        guard
            let role = payload["role"] as? String,
            let remainingForKind = payload["remainingForKind"] as? NSNumber,
            let limitForKind = payload["limitForKind"] as? NSNumber,
            let historyRetentionDays = payload["historyRetentionDays"] as? NSNumber
        else {
            throw AIUsageAuthorizationError.invalidResponse
        }

        return AIUsageAuthorizationResult(
            role: role,
            remainingForKind: remainingForKind.intValue,
            limitForKind: limitForKind.intValue,
            historyRetentionDays: historyRetentionDays.intValue
        )
    }
}
