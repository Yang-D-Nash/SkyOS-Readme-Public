import Foundation
import FirebaseFirestore

struct CommerceShippingSettings: Equatable {
    var domesticCost: Double = 4.90
    var euCost: Double = 6.90
    var internationalCost: Double = 11.90
    var freeShippingThreshold: Double = 89.0
    var shippingNotes: String = ""
}

struct CommerceInvoiceSettings: Equatable {
    var companyName: String = "Skydown Entertainment"
    var companyAddress: String = ""
    var taxNumber: String = ""
    var vatId: String = ""
    var taxRate: Double = 19.0
    var invoicePrefix: String = "SD"
    var supportEmail: String = "skydownent@gmail.com"
}

struct CommerceSettings: Equatable {
    var shipping: CommerceShippingSettings = .init()
    var invoice: CommerceInvoiceSettings = .init()

    static let `default` = CommerceSettings()
}

protocol CommerceSettingsServicing {
    func observeSettings(_ onChange: @escaping @MainActor (Result<CommerceSettings, Error>) -> Void) -> () -> Void
    func updateSettings(_ settings: CommerceSettings) async throws
}

final class FirestoreCommerceSettingsService: CommerceSettingsServicing {
    private let firestore: Firestore
    private let collectionName = "appConfig"
    private let documentName = "commerceSettings"

    init(firestore: Firestore = Firestore.firestore()) {
        self.firestore = firestore
    }

    func observeSettings(_ onChange: @escaping @MainActor (Result<CommerceSettings, Error>) -> Void) -> () -> Void {
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

    func updateSettings(_ settings: CommerceSettings) async throws {
        try await firestore.collection(collectionName).document(documentName).setData(Self.encode(settings), merge: true)
    }

    private static func decode(_ data: [String: Any]) -> CommerceSettings {
        let shipping = data["shipping"] as? [String: Any] ?? [:]
        let invoice = data["invoice"] as? [String: Any] ?? [:]

        return CommerceSettings(
            shipping: CommerceShippingSettings(
                domesticCost: decodeDouble(shipping["domesticCost"], fallback: 4.90),
                euCost: decodeDouble(
                    shipping["euCost"],
                    fallback: decodeDouble(shipping["internationalCost"], fallback: 6.90)
                ),
                internationalCost: decodeDouble(shipping["internationalCost"], fallback: 11.90),
                freeShippingThreshold: decodeDouble(shipping["freeShippingThreshold"], fallback: 89.0),
                shippingNotes: shipping["shippingNotes"] as? String ?? ""
            ),
            invoice: CommerceInvoiceSettings(
                companyName: invoice["companyName"] as? String ?? "Skydown Entertainment",
                companyAddress: invoice["companyAddress"] as? String ?? "",
                taxNumber: invoice["taxNumber"] as? String ?? "",
                vatId: invoice["vatId"] as? String ?? "",
                taxRate: decodeDouble(invoice["taxRate"], fallback: 19.0),
                invoicePrefix: invoice["invoicePrefix"] as? String ?? "SD",
                supportEmail: invoice["supportEmail"] as? String ?? "skydownent@gmail.com"
            )
        )
    }

    private static func encode(_ settings: CommerceSettings) -> [String: Any] {
        [
            "shipping": [
                "domesticCost": settings.shipping.domesticCost,
                "euCost": settings.shipping.euCost,
                "internationalCost": settings.shipping.internationalCost,
                "freeShippingThreshold": settings.shipping.freeShippingThreshold,
                "shippingNotes": settings.shipping.shippingNotes.trimmed
            ],
            "invoice": [
                "companyName": settings.invoice.companyName.trimmed,
                "companyAddress": settings.invoice.companyAddress.trimmed,
                "taxNumber": settings.invoice.taxNumber.trimmed,
                "vatId": settings.invoice.vatId.trimmed,
                "taxRate": settings.invoice.taxRate,
                "invoicePrefix": settings.invoice.invoicePrefix.trimmed,
                "supportEmail": settings.invoice.supportEmail.trimmed
            ],
            "updatedAt": FieldValue.serverTimestamp()
        ]
    }

    private static func decodeDouble(_ value: Any?, fallback: Double) -> Double {
        switch value {
        case let number as NSNumber:
            return number.doubleValue
        case let text as String:
            return Double(text) ?? fallback
        default:
            return fallback
        }
    }
}

@MainActor
final class CommerceSettingsStore: ObservableObject {
    static let shared = CommerceSettingsStore()

    @Published private(set) var settings: CommerceSettings = .default
    @Published private(set) var lastErrorMessage: String?

    private let service: CommerceSettingsServicing
    private var stopObserving: (() -> Void)?

    init(service: CommerceSettingsServicing = FirestoreCommerceSettingsService()) {
        self.service = service
        startObserving()
    }

    func save(_ settings: CommerceSettings) async throws {
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
    var trimmed: String {
        trimmingCharacters(in: .whitespacesAndNewlines)
    }
}
