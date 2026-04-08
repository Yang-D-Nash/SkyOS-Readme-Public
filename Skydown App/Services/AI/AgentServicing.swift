import Foundation
import FirebaseFunctions

struct AgentHistoryTurn {
    let role: String
    let text: String
}

struct AgentChatResponse {
    let reply: String
    let historyRetentionDays: Int
    let automationTriggered: Bool
    let automationAttempted: Bool
    let automationMessage: String
    let workflowName: String
}

protocol AgentChatServicing {
    func sendMessage(
        prompt: String,
        history: [AgentHistoryTurn],
        mode: String,
        executeAutomation: Bool
    ) async throws -> AgentChatResponse
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
        history: [AgentHistoryTurn],
        mode: String,
        executeAutomation: Bool
    ) async throws -> AgentChatResponse {
        let payload: [String: Any] = [
            "prompt": prompt,
            "history": history.map { turn in
                [
                    "role": turn.role,
                    "text": turn.text
                ]
            },
            "mode": mode,
            "executeAutomation": executeAutomation
        ]

        let result = try await functions
            .httpsCallable("skydownAgent")
            .call(payload)

        if let reply = result.data as? String, !reply.isEmpty {
            return AgentChatResponse(
                reply: reply,
                historyRetentionDays: UserRole.user.defaultAIHistoryRetentionDays,
                automationTriggered: false,
                automationAttempted: false,
                automationMessage: "",
                workflowName: ""
            )
        }

        if let payload = result.data as? [String: Any],
           let reply = payload["reply"] as? String,
           !reply.isEmpty {
            return AgentChatResponse(
                reply: reply,
                historyRetentionDays: (payload["historyRetentionDays"] as? NSNumber)?.intValue
                    ?? UserRole.user.defaultAIHistoryRetentionDays,
                automationTriggered: payload["automationTriggered"] as? Bool ?? false,
                automationAttempted: payload["automationAttempted"] as? Bool ?? false,
                automationMessage: (payload["automationMessage"] as? String) ?? "",
                workflowName: (payload["workflowName"] as? String) ?? ""
            )
        }

        throw AgentServiceError.invalidResponse
    }
}
