import Foundation
import FirebaseAI

protocol AIChatServicing {
    func makeChat() -> Chat
}

struct FirebaseAIChatService: AIChatServicing {
    func makeChat() -> Chat {
        let config = GenerationConfig(
            candidateCount: 1,
            maxOutputTokens: 220,
            thinkingConfig: ThinkingConfig(thinkingBudget: 256)
        )

        return FirebaseAI
            .firebaseAI(backend: .googleAI())
            .generativeModel(
                modelName: "gemini-2.5-flash-lite",
                generationConfig: config
            )
            .startChat()
    }
}
