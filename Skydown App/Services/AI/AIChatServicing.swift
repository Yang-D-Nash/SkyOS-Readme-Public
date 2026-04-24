import Foundation
import FirebaseFunctions

struct AITextResponse {
    let text: String
    let historyRetentionDays: Int
    let usage: AIUsageSnapshot?
    let decision: AIBotDecision?
}

struct AIGeneratedVisual {
    let text: String
    let imageData: Data
    let mimeType: String
    let historyRetentionDays: Int
    let usage: AIUsageSnapshot?
    let decision: AIBotDecision?
}

struct AIUsageSnapshot {
    let remainingForKind: Int
    let limitForKind: Int
    let warningLevel: String
    let userFacingReason: String
    let suggestedUpgrade: String
    let resetHint: String
    let retryAfterSeconds: Int
    let lowerCostOption: String
}

struct AIBotDecision: Equatable {
    let state: String
    let route: String
    let topic: String
    let summary: String
    let promptVersion: String
    let qualityMode: String
    let faqMode: String
    let ownerMode: String
    let answerLength: String
    let personalityStyle: String
    let loggingLevel: String
    let diagnosticsMode: String
    let ownerDiagnosticActive: Bool
    let selectedModel: String
    let selectedProvider: String
    let fallbackActivated: Bool
    let fallbackReason: String
    let responseLimited: Bool
    let responseLimitReason: String
    let blocked: Bool
    let blockReason: String
    let retryable: Bool
    let retryReason: String
    let trace: [String]
}

protocol AIChatServicing {
    func generateText(prompt: String, mode: String, aiLevel: String) async throws -> AITextResponse
    func generateVisual(prompt: String, aiLevel: String) async throws -> AIGeneratedVisual
}

enum AIChatServiceError: LocalizedError {
    case invalidResponse
    case invalidImageData

    var errorDescription: String? {
        switch self {
        case .invalidResponse:
            return "Der SkyOS Bot hat keine gueltige Antwort geliefert."
        case .invalidImageData:
            return "Das generierte Visual konnte nicht gelesen werden."
        }
    }
}

struct FirebaseFunctionsAIChatService: AIChatServicing {
    private let functions: Functions

    init(functions: Functions = Functions.functions(region: "us-central1")) {
        self.functions = functions
    }

    func generateText(prompt: String, mode: String, aiLevel: String) async throws -> AITextResponse {
        try await ensureConnectivity()
        let result = try await functions.invokeCallable("generateAiText", payload: [
                "prompt": prompt,
                "mode": mode,
                "aiLevel": aiLevel
            ])

        guard
            let payload = result.data as? [String: Any],
            let reply = (payload["reply"] as? String)?.trimmedNilIfEmpty
        else {
            throw AIChatServiceError.invalidResponse
        }

        return AITextResponse(
            text: reply,
            historyRetentionDays: (payload["historyRetentionDays"] as? NSNumber)?.intValue
                ?? UserRole.user.defaultAIHistoryRetentionDays,
            usage: Self.parseUsage(payload["usage"] as? [String: Any]),
            decision: Self.parseDecision(payload["botDecision"] as? [String: Any])
        )
    }

    func generateVisual(prompt: String, aiLevel: String) async throws -> AIGeneratedVisual {
        var lastError: Error?
        for attempt in 1...2 {
            do {
                return try await generateVisualOnce(prompt: prompt, aiLevel: aiLevel)
            } catch {
                lastError = error
                guard attempt < 2, shouldRetryVisualGeneration(after: error) else {
                    throw error
                }

                try? await Task.sleep(nanoseconds: 800_000_000)
            }
        }

        throw lastError ?? AIChatServiceError.invalidResponse
    }

    private func ensureConnectivity() async throws {
        let isOnline = await MainActor.run { NetworkStatusMonitor.shared.isOnline }
        guard isOnline else {
            throw NSError(
                domain: "AIChatServicing",
                code: -1009,
                userInfo: [NSLocalizedDescriptionKey: "Du bist offline. Bot und Visuals werden wieder verfuegbar, sobald Internet da ist."]
            )
        }
    }

    private func generateVisualOnce(prompt: String, aiLevel: String) async throws -> AIGeneratedVisual {
        try await ensureConnectivity()
        let result = try await functions.invokeCallable("generateAiVisual", payload: [
            "prompt": prompt,
            "aiLevel": aiLevel
        ])

        guard let payload = result.data as? [String: Any] else {
            throw AIChatServiceError.invalidResponse
        }

        guard
            let imageBase64 = (payload["imageBase64"] as? String)?.trimmedNilIfEmpty,
            let imageData = Data(base64Encoded: imageBase64, options: [.ignoreUnknownCharacters])
        else {
            throw AIChatServiceError.invalidImageData
        }

        return AIGeneratedVisual(
            text: (payload["text"] as? String)?.trimmedNilIfEmpty ?? "Visual generiert.",
            imageData: imageData,
            mimeType: (payload["mimeType"] as? String)?.trimmedNilIfEmpty ?? "image/png",
            historyRetentionDays: (payload["historyRetentionDays"] as? NSNumber)?.intValue
                ?? UserRole.user.defaultAIHistoryRetentionDays,
            usage: Self.parseUsage(payload["usage"] as? [String: Any]),
            decision: Self.parseDecision(payload["botDecision"] as? [String: Any])
        )
    }

    private static func parseUsage(_ payload: [String: Any]?) -> AIUsageSnapshot? {
        guard let payload else { return nil }
        let hints = payload["guardrailHints"] as? [String: Any]
        return AIUsageSnapshot(
            remainingForKind: (payload["remainingForKind"] as? NSNumber)?.intValue ?? 0,
            limitForKind: (payload["limitForKind"] as? NSNumber)?.intValue ?? 0,
            warningLevel: (payload["warningLevel"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() ?? "ok",
            userFacingReason: (hints?["userFacingReason"] as? String) ?? "",
            suggestedUpgrade: (hints?["suggestedUpgrade"] as? String) ?? "",
            resetHint: (hints?["resetHint"] as? String) ?? "",
            retryAfterSeconds: (hints?["retryAfterSeconds"] as? NSNumber)?.intValue ?? 0,
            lowerCostOption: (hints?["lowerCostOption"] as? String) ?? ""
        )
    }

    private static func parseDecision(_ payload: [String: Any]?) -> AIBotDecision? {
        guard let payload else { return nil }
        return AIBotDecision(
            state: (payload["state"] as? String) ?? "complete",
            route: (payload["route"] as? String) ?? "assistant",
            topic: (payload["topic"] as? String) ?? "",
            summary: (payload["summary"] as? String) ?? "",
            promptVersion: (payload["promptVersion"] as? String) ?? "",
            qualityMode: (payload["qualityMode"] as? String) ?? "",
            faqMode: (payload["faqMode"] as? String) ?? "",
            ownerMode: (payload["ownerMode"] as? String) ?? "",
            answerLength: (payload["answerLength"] as? String) ?? "",
            personalityStyle: (payload["personalityStyle"] as? String) ?? "",
            loggingLevel: (payload["loggingLevel"] as? String) ?? "",
            diagnosticsMode: (payload["diagnosticsMode"] as? String) ?? "",
            ownerDiagnosticActive: payload["ownerDiagnosticActive"] as? Bool ?? false,
            selectedModel: (payload["selectedModel"] as? String) ?? "",
            selectedProvider: (payload["selectedProvider"] as? String) ?? "",
            fallbackActivated: payload["fallbackActivated"] as? Bool ?? false,
            fallbackReason: (payload["fallbackReason"] as? String) ?? "",
            responseLimited: payload["responseLimited"] as? Bool ?? false,
            responseLimitReason: (payload["responseLimitReason"] as? String) ?? "",
            blocked: payload["blocked"] as? Bool ?? false,
            blockReason: (payload["blockReason"] as? String) ?? "",
            retryable: payload["retryable"] as? Bool ?? false,
            retryReason: (payload["retryReason"] as? String) ?? "",
            trace: payload["trace"] as? [String] ?? []
        )
    }

    private func shouldRetryVisualGeneration(after error: Error) -> Bool {
        if let serviceError = error as? AIChatServiceError {
            switch serviceError {
            case .invalidResponse, .invalidImageData:
                return true
            }
        }

        let nsError = error as NSError
        if nsError.domain == FunctionsErrorDomain,
           let code = FunctionsErrorCode(rawValue: nsError.code) {
            switch code {
            case .internal, .unavailable, .deadlineExceeded:
                return true
            default:
                break
            }
        }

        let message = nsError.localizedDescription.lowercased()
        return message.contains("server responded with an error") ||
            message.contains("nicht sauber geantwortet")
    }
}

private extension String {
    var trimmedNilIfEmpty: String? {
        let value = trimmingCharacters(in: .whitespacesAndNewlines)
        return value.isEmpty ? nil : value
    }
}
