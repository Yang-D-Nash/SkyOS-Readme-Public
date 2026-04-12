import Foundation
import FirebaseFunctions

protocol ShopifyMerchSyncServicing {
    func triggerSync() async throws -> String
}

struct FirebaseFunctionsShopifyMerchSyncService: ShopifyMerchSyncServicing {
    private let functions: Functions

    init(functions: Functions = Functions.functions(region: "us-central1")) {
        self.functions = functions
    }

    func triggerSync() async throws -> String {
        let response = try await functions.invokeCallable("syncShopifyMerch", payload: [:])

        let data = response.data as? [String: Any]
        let synced = (data?["syncedCount"] as? NSNumber)?.intValue ?? 0
        let created = (data?["createdCount"] as? NSNumber)?.intValue ?? 0
        let updated = (data?["updatedCount"] as? NSNumber)?.intValue ?? 0
        let deactivated = (data?["deactivatedCount"] as? NSNumber)?.intValue ?? 0
        let collectionHandles = (data?["collectionHandles"] as? [String])?
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
            ?? {
                guard let legacyHandle = (data?["collectionHandle"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines),
                      legacyHandle.isEmpty == false else {
                    return []
                }
                return [legacyHandle]
            }()

        if collectionHandles.isEmpty == false {
            return "Shopify-Sync abgeschlossen: \(collectionHandles.count) Collections, \(synced) Produkte, \(created) neu, \(updated) aktualisiert, \(deactivated) ausgeblendet."
        }

        return "Shopify-Sync abgeschlossen: \(synced) Produkte, \(created) neu, \(updated) aktualisiert, \(deactivated) ausgeblendet."
    }
}
