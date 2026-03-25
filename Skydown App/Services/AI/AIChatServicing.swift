import Foundation
import FirebaseAI

protocol AIChatServicing {
    func makeChat() -> Chat
}

struct FirebaseAIChatService: AIChatServicing {
    func makeChat() -> Chat {
        FirebaseAI
            .firebaseAI(backend: .googleAI())
            .generativeModel(modelName: "gemini-2.5-flash")
            .startChat()
    }
}
