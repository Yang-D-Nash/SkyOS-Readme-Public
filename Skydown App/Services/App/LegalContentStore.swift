import Foundation
import FirebaseFirestore

struct LegalContentSettings: Codable, Equatable {
    var brandName: String = "22xSky"
    var operatorName: String = "Ngoc Anh Nguyen (Yang D. Nash - Skydown)"
    var rightsHolderName: String = "Ngoc Anh Nguyen (Yang D. Nash - Skydown)"
    var supportEmail: String = "skydownent@gmail.com"
    var lastUpdatedLabel: String = "15. April 2026"
    var imprintReference: String = "Anbieterkennzeichnung: Ngoc Anh Nguyen, Yang D. Nash - Skydown, Erich-Plate-Weg 44, 22419 Hamburg, Deutschland. Kontakt: skydownent@gmail.com."
    var masterNumberMeaning: String = """
    Die Meisterzahl 22 gilt als Master Builder: visionaer, praktisch und umsetzungsstark. Sie verbindet Inspiration mit Disziplin und macht aus Ideen reale, belastbare Strukturen.
    """
    var brandManifesto: String = """
    Dort, wo der Himmel faellt, beginnt unser Denken.
    Was zerbricht, offenbart Tiefe - nicht Verlust.
    Wir hoeren auf das, was nicht laut ist: Wandel, Stille, Sinn.
    Unser Handeln wurzelt im Inneren, wo Klarheit entsteht.
    Nicht im Machen liegt unsere Kraft, sondern im Verstehen.
    Denn wir glauben: Der Himmel faellt nicht auf uns - er oeffnet sich in uns.
    """
    var symbolicNumericCode: String = "1337-514-731"
    var symbolicLeetCode: String = "7H3_F4LL_0F_H34/3N"
    var symbolicCodeExplanation: String = """
    7H3 steht fuer THE, F4LL fuer FALL, 0F fuer OF und H34/3N fuer HEAVEN.
    Der Code symbolisiert den Fall des Himmels als innere Oeffnung - wie ein Schluessel zu verborgener Erkenntnis.
    Alternative Codes: 731-4177-0V3R-H34/3N oder 1337-514-731.
    """

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

    var resolvedMasterNumberMeaning: String {
        masterNumberMeaning.legalTrimmed.legalNilIfEmpty ?? Self.default.masterNumberMeaning
    }

    var resolvedBrandManifesto: String {
        brandManifesto.legalTrimmed.legalNilIfEmpty ?? Self.default.brandManifesto
    }

    var resolvedSymbolicNumericCode: String {
        symbolicNumericCode.legalTrimmed.legalNilIfEmpty ?? Self.default.symbolicNumericCode
    }

    var resolvedSymbolicLeetCode: String {
        symbolicLeetCode.legalTrimmed.legalNilIfEmpty ?? Self.default.symbolicLeetCode
    }

    var resolvedSymbolicCodeExplanation: String {
        symbolicCodeExplanation.legalTrimmed.legalNilIfEmpty ?? Self.default.symbolicCodeExplanation
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

    var appGuideText: String {
        LegalTextTemplateFactory.appGuide(using: self)
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
            imprintReference: data["imprintReference"] as? String ?? LegalContentSettings.default.imprintReference,
            masterNumberMeaning: data["masterNumberMeaning"] as? String ?? LegalContentSettings.default.masterNumberMeaning,
            brandManifesto: data["brandManifesto"] as? String ?? LegalContentSettings.default.brandManifesto,
            symbolicNumericCode: data["symbolicNumericCode"] as? String ?? LegalContentSettings.default.symbolicNumericCode,
            symbolicLeetCode: data["symbolicLeetCode"] as? String ?? LegalContentSettings.default.symbolicLeetCode,
            symbolicCodeExplanation: data["symbolicCodeExplanation"] as? String ?? LegalContentSettings.default.symbolicCodeExplanation
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
            "masterNumberMeaning": settings.resolvedMasterNumberMeaning,
            "brandManifesto": settings.resolvedBrandManifesto,
            "symbolicNumericCode": settings.resolvedSymbolicNumericCode,
            "symbolicLeetCode": settings.resolvedSymbolicLeetCode,
            "symbolicCodeExplanation": settings.resolvedSymbolicCodeExplanation,
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
