import Foundation
import FirebaseFunctions

struct AgentHistoryTurn {
    let role: String
    let text: String
}

protocol AgentChatServicing {
    func sendMessage(
        prompt: String,
        history: [AgentHistoryTurn]
    ) async throws -> String
}

enum AgentServiceError: LocalizedError {
    case invalidResponse

    var errorDescription: String? {
        switch self {
        case .invalidResponse:
            return "Der Skydown x 22 Agent hat keine Antwort geliefert."
        }
    }
}

struct FirebaseFunctionsAgentService: AgentChatServicing {
    private let functions: Functions

    init(functions: Functions = Functions.functions(region: "us-central1")) {
        self.functions = functions
    }

    func sendMessage(
        prompt: String,
        history: [AgentHistoryTurn]
    ) async throws -> String {
        let payload: [String: Any] = [
            "prompt": prompt,
            "history": history.map { turn in
                [
                    "role": turn.role,
                    "text": turn.text
                ]
            }
        ]

        let result = try await functions
            .httpsCallable("skydownAgent")
            .call(payload)

        if let reply = result.data as? String, !reply.isEmpty {
            return reply
        }

        if let payload = result.data as? [String: Any],
           let reply = payload["reply"] as? String,
           !reply.isEmpty {
            return reply
        }

        throw AgentServiceError.invalidResponse
    }
}
