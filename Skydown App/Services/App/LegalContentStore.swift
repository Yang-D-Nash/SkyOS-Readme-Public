import Foundation
import FirebaseFirestore

struct LegalContentSettings: Codable, Equatable {
    var brandName: String = "Skydown x 22"
    var operatorName: String = "Yang D. Nash - Skydown"
    var rightsHolderName: String = "Yang D. Nash - Skydown"
    var supportEmail: String = "skydownent@gmail.com"
    var lastUpdatedLabel: String = "12. April 2026"
    var imprintReference: String = "Die vollstaendige Anbieterkennzeichnung einschliesslich ladungsfaehiger Anschrift ist im Impressum, Store-Eintrag oder Anbieterprofil bereitzuhalten."

    static let `default` = LegalContentSettings()

    var resolvedBrandName: String {
        brandName.legalTrimmed.legalNilIfEmpty ?? Self.default.brandName
    }

    var resolvedOperatorName: String {
        operatorName.legalTrimmed.legalNilIfEmpty ?? Self.default.operatorName
    }

    var resolvedRightsHolderName: String {
        rightsHolderName.legalTrimmed.legalNilIfEmpty ?? Self.default.rightsHolderName
    }

    var resolvedSupportEmail: String {
        supportEmail.legalTrimmed.legalNilIfEmpty ?? Self.default.supportEmail
    }

    var resolvedLastUpdatedLabel: String {
        lastUpdatedLabel.legalTrimmed.legalNilIfEmpty ?? Self.default.lastUpdatedLabel
    }

    var resolvedImprintReference: String {
        imprintReference.legalTrimmed.legalNilIfEmpty ?? Self.default.imprintReference
    }

    var privacyPolicyText: String {
        LegalTextTemplateFactory.privacyPolicy(using: self)
    }

    var termsAndConditionsText: String {
        LegalTextTemplateFactory.termsAndConditions(using: self)
    }

    var termsOfServiceText: String {
        LegalTextTemplateFactory.termsOfService(using: self)
    }
}

protocol LegalContentServicing {
    func observeSettings(_ onChange: @escaping @MainActor (Result<LegalContentSettings, Error>) -> Void) -> () -> Void
    func updateSettings(_ settings: LegalContentSettings) async throws
}

final class FirestoreLegalContentService: LegalContentServicing {
    private let firestore: Firestore
    private let collectionName = "appConfig"
    private let documentName = "legalContent"

    init(firestore: Firestore = Firestore.firestore()) {
        self.firestore = firestore
    }

    func observeSettings(_ onChange: @escaping @MainActor (Result<LegalContentSettings, Error>) -> Void) -> () -> Void {
        let listener = firestore.collection(collectionName).document(documentName).addSnapshotListener { snapshot, error in
            Task { @MainActor in
                if let error {
                    onChange(.failure(error))
                    return
                }

                onChange(.success(Self.decode(snapshot?.data() ?? [:])))
            }
        }

        return {
            listener.remove()
        }
    }

    func updateSettings(_ settings: LegalContentSettings) async throws {
        try await firestore.collection(collectionName).document(documentName).setData(Self.encode(settings), merge: true)
    }

    private static func decode(_ data: [String: Any]) -> LegalContentSettings {
        LegalContentSettings(
            brandName: data["brandName"] as? String ?? LegalContentSettings.default.brandName,
            operatorName: data["operatorName"] as? String ?? LegalContentSettings.default.operatorName,
            rightsHolderName: data["rightsHolderName"] as? String ?? LegalContentSettings.default.rightsHolderName,
            supportEmail: data["supportEmail"] as? String ?? LegalContentSettings.default.supportEmail,
            lastUpdatedLabel: data["lastUpdatedLabel"] as? String ?? LegalContentSettings.default.lastUpdatedLabel,
            imprintReference: data["imprintReference"] as? String ?? LegalContentSettings.default.imprintReference
        )
    }

    private static func encode(_ settings: LegalContentSettings) -> [String: Any] {
        [
            "brandName": settings.resolvedBrandName,
            "operatorName": settings.resolvedOperatorName,
            "rightsHolderName": settings.resolvedRightsHolderName,
            "supportEmail": settings.resolvedSupportEmail,
            "lastUpdatedLabel": settings.resolvedLastUpdatedLabel,
            "imprintReference": settings.resolvedImprintReference,
            "updatedAt": FieldValue.serverTimestamp()
        ]
    }
}

@MainActor
final class LegalContentStore: ObservableObject {
    static let shared = LegalContentStore()

    @Published private(set) var settings: LegalContentSettings = .default
    @Published private(set) var lastErrorMessage: String?

    private let service: LegalContentServicing
    private var stopObserving: (() -> Void)?

    init(service: LegalContentServicing = FirestoreLegalContentService()) {
        self.service = service
        startObserving()
    }

    func save(_ settings: LegalContentSettings) async throws {
        try await service.updateSettings(settings)
    }

    private func startObserving() {
        stopObserving?()
        stopObserving = service.observeSettings { [weak self] result in
            guard let self else { return }

            switch result {
            case .success(let settings):
                self.settings = settings
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

private extension String {
    var legalTrimmed: String {
        trimmingCharacters(in: .whitespacesAndNewlines)
    }

    var legalNilIfEmpty: String? {
        isEmpty ? nil : self
    }
}
