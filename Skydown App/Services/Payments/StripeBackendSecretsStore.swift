import Foundation
import FirebaseFirestore
import FirebaseFunctions

struct StripeBackendSecretsStatus: Equatable {
    var hasSecretKey: Bool = false
    var hasWebhookSecret: Bool = false
    var updatedAt: Date?

    var isReady: Bool {
        hasSecretKey && hasWebhookSecret
    }
}

protocol StripeBackendSecretsServicing {
    func observeStatus(_ onChange: @escaping @MainActor (Result<StripeBackendSecretsStatus, Error>) -> Void) -> () -> Void
    func saveSecrets(stripeSecretKey: String, stripeWebhookSecret: String) async throws -> StripeBackendSecretsStatus
}

final class FirebaseStripeBackendSecretsService: StripeBackendSecretsServicing {
    private let firestore: Firestore
    private let functions: Functions

    init(
        firestore: Firestore = Firestore.firestore(),
        functions: Functions = Functions.functions(region: "us-central1")
    ) {
        self.firestore = firestore
        self.functions = functions
    }

    func observeStatus(_ onChange: @escaping @MainActor (Result<StripeBackendSecretsStatus, Error>) -> Void) -> () -> Void {
        let listener = firestore.collection("adminConfig").document("stripeCheckoutSecrets").addSnapshotListener { snapshot, error in
            Task { @MainActor in
                if let error {
                    onChange(.failure(error))
                    return
                }

                let data = snapshot?.data() ?? [:]
                onChange(.success(Self.decode(data)))
            }
        }

        return {
            listener.remove()
        }
    }

    func saveSecrets(stripeSecretKey: String, stripeWebhookSecret: String) async throws -> StripeBackendSecretsStatus {
        let payload: [String: Any] = [
            "stripeSecretKey": stripeSecretKey.trimmingCharacters(in: .whitespacesAndNewlines),
            "stripeWebhookSecret": stripeWebhookSecret.trimmingCharacters(in: .whitespacesAndNewlines)
        ]

        let result = try await functions.httpsCallable("configureStripeBackendSecrets").call(payload)
        guard let data = result.data as? [String: Any] else {
            return .init()
        }

        if let statusData = data["status"] as? [String: Any] {
            return Self.decode(statusData)
        }

        return .init()
    }

    private static func decode(_ data: [String: Any]) -> StripeBackendSecretsStatus {
        let updatedAt: Date?
        if let timestamp = data["updatedAt"] as? Timestamp {
            updatedAt = timestamp.dateValue()
        } else if let date = data["updatedAt"] as? Date {
            updatedAt = date
        } else {
            updatedAt = nil
        }

        return StripeBackendSecretsStatus(
            hasSecretKey: data["hasSecretKey"] as? Bool ?? false,
            hasWebhookSecret: data["hasWebhookSecret"] as? Bool ?? false,
            updatedAt: updatedAt
        )
    }
}

@MainActor
final class StripeBackendSecretsStore: ObservableObject {
    static let shared = StripeBackendSecretsStore()

    @Published private(set) var status: StripeBackendSecretsStatus = .init()
    @Published private(set) var lastErrorMessage: String?

    private let service: StripeBackendSecretsServicing
    private var stopObserving: (() -> Void)?
    private var isObservationEnabled = false

    init(service: StripeBackendSecretsServicing = FirebaseStripeBackendSecretsService()) {
        self.service = service
    }

    func saveSecrets(stripeSecretKey: String, stripeWebhookSecret: String) async throws {
        let updatedStatus = try await service.saveSecrets(
            stripeSecretKey: stripeSecretKey,
            stripeWebhookSecret: stripeWebhookSecret
        )
        status = updatedStatus
    }

    func setObservationEnabled(_ isEnabled: Bool) {
        guard isEnabled != isObservationEnabled else { return }
        isObservationEnabled = isEnabled

        if isEnabled {
            startObserving()
        } else {
            stopObserving?()
            stopObserving = nil
            lastErrorMessage = nil
            status = .init()
        }
    }

    private func startObserving() {
        stopObserving?()
        stopObserving = service.observeStatus { [weak self] result in
            guard let self else { return }

            switch result {
            case .success(let status):
                self.status = status
                self.lastErrorMessage = nil
            case .failure(let error):
                self.lastErrorMessage = error.localizedDescription
            }
        }
    }

    deinit {
        stopObserving?()
    }
}
