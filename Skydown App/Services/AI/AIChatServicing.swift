import Foundation
import FirebaseAI

struct AIGeneratedVisual {
    let text: String
    let imageData: Data
    let mimeType: String
}

protocol AIChatServicing {
    func makeChat() -> Chat
    func generateVisual(prompt: String) async throws -> AIGeneratedVisual
}

struct FirebaseAIChatService: AIChatServicing {
    func makeChat() -> Chat {
        let config = GenerationConfig(
            candidateCount: 1,
            maxOutputTokens: 768,
            thinkingConfig: ThinkingConfig(thinkingBudget: 0)
        )

        return FirebaseAI
            .firebaseAI(backend: .googleAI())
            .generativeModel(
                modelName: "gemini-2.5-flash-lite",
                generationConfig: config
            )
            .startChat()
    }

    func generateVisual(prompt: String) async throws -> AIGeneratedVisual {
        let response = try await FirebaseAI
            .firebaseAI(backend: .googleAI())
            .generativeModel(
                modelName: "gemini-2.5-flash-image",
                generationConfig: GenerationConfig(
                    responseModalities: [.text, .image]
                )
            )
            .generateContent(prompt)

        guard let inlinePart = response.inlineDataParts.first else {
            throw GenerateVisualError.missingImage
        }

        return AIGeneratedVisual(
            text: response.text?.trimmingCharacters(in: .whitespacesAndNewlines).nilIfEmpty ?? "Visual generiert.",
            imageData: inlinePart.data,
            mimeType: inlinePart.mimeType,
            )
    }
}

private enum GenerateVisualError: LocalizedError {
    case missingImage
}

private extension String {
    var nilIfEmpty: String? {
        isEmpty ? nil : self
    }
}
