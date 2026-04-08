import Foundation
import FirebaseFunctions

struct AITextResponse {
    let text: String
    let historyRetentionDays: Int
}

struct AIGeneratedVisual {
    let text: String
    let imageData: Data
    let mimeType: String
    let historyRetentionDays: Int
}

protocol AIChatServicing {
    func generateText(prompt: String, mode: String) async throws -> AITextResponse
    func generateVisual(prompt: String) async throws -> AIGeneratedVisual
}

enum AIChatServiceError: LocalizedError {
    case invalidResponse
    case invalidImageData

    var errorDescription: String? {
        switch self {
        case .invalidResponse:
            return "Der Skydown x 22 Bot hat keine gueltige Antwort geliefert."
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

    func generateText(prompt: String, mode: String) async throws -> AITextResponse {
        let result = try await functions
            .httpsCallable("generateAiText")
            .call([
                "prompt": prompt,
                "mode": mode
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
                ?? UserRole.user.defaultAIHistoryRetentionDays
        )
    }

    func generateVisual(prompt: String) async throws -> AIGeneratedVisual {
        let result = try await functions
            .httpsCallable("generateAiVisual")
            .call(["prompt": prompt])

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
                ?? UserRole.user.defaultAIHistoryRetentionDays
        )
    }
}

private extension String {
    var trimmedNilIfEmpty: String? {
        let value = trimmingCharacters(in: .whitespacesAndNewlines)
        return value.isEmpty ? nil : value
    }
}
