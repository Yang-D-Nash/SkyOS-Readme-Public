import Foundation
import FirebaseFirestore

protocol MerchandiseServicing {
    func observeItems(_ onChange: @escaping @MainActor (Result<[MerchandiseItem], Error>) -> Void) -> () -> Void
}

final class UITestMerchandiseService: MerchandiseServicing {
    func observeItems(_ onChange: @escaping @MainActor (Result<[MerchandiseItem], Error>) -> Void) -> () -> Void {
        Task { @MainActor in
            onChange(.success([
                MerchandiseItem(
                    id: "ui-test-merch-item",
                    name: "Skydown Atelier Hoodie",
                    price: 79.0,
                    description: "Schwerer Signature-Drop fuer den Merch-Vollbild-Flow.",
                    imageURLs: [
                        "https://example.com/ui-test-merch-1.jpg",
                        "https://example.com/ui-test-merch-2.jpg",
                    ],
                    available: true,
                    shopifyProductId: "gid://shopify/Product/ui-test-merch",
                    shopifyHandle: "skydown-atelier-hoodie",
                    variants: [
                        MerchandiseVariant(
                            id: "ui-test-variant-black-m",
                            title: "Black / M",
                            size: "M",
                            color: "Black",
                            shopifyVariantId: "gid://shopify/ProductVariant/ui-test-merch-black-m",
                            sku: "SKY-ATELIER-HOODIE-BLK-M",
                            price: 79.0
                        ),
                    ],
                    source: "shopify",
                    featured: true,
                    customBadge: "Atelier",
                    category: "Signature Drop",
                    collabPartner: "Skydown",
                    shopifyCollectionHandles: ["signature"]
                ),
            ]))
        }

        return {}
    }
}

final class FirebaseMerchandiseService: MerchandiseServicing {
    private let firestore: Firestore

    init(
        firestore: Firestore = Firestore.firestore()
    ) {
        self.firestore = firestore
    }

    func observeItems(_ onChange: @escaping @MainActor (Result<[MerchandiseItem], Error>) -> Void) -> () -> Void {
        let listener = firestore.collection("merchandise").addSnapshotListener { snapshot, error in
            Task { @MainActor in
                if let error {
                    onChange(.failure(error))
                    return
                }

                let items = snapshot?.documents.compactMap { document in
                    document.toMerchandiseItem()
                }.sorted {
                    if $0.featured != $1.featured {
                        return $0.featured && !$1.featured
                    }
                    if $0.sortOrder != $1.sortOrder {
                        return $0.sortOrder < $1.sortOrder
                    }
                    return $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending
                } ?? []

                onChange(.success(items))
            }
        }

        return {
            listener.remove()
        }
    }
}

private extension DocumentSnapshot {
    func toMerchandiseItem() -> MerchandiseItem? {
        guard let data = data() else { return nil }
        let imageURLs = (data["imageURLs"] as? [String])
            ?? (data["imageUrls"] as? [String])
            ?? []

        guard let name = data["name"] as? String,
              let priceNumber = data["price"] as? NSNumber else {
            return nil
        }

        return MerchandiseItem(
            id: documentID,
            name: name,
            price: priceNumber.doubleValue,
            description: data["description"] as? String ?? "",
            imageURLs: imageURLs,
            available: data["available"] as? Bool ?? true,
            currency: data["currency"] as? String ?? "EUR",
            sku: data["sku"] as? String,
            shopifyProductId: data["shopifyProductId"] as? String,
            shopifyHandle: data["shopifyHandle"] as? String,
            availableForSale: data["availableForSale"] as? Bool ?? (data["available"] as? Bool ?? true),
            shopifySyncActive: data["shopifySyncActive"] as? Bool ?? true,
            variants: (data["variants"] as? [[String: Any]])?.map { rawVariant in
                MerchandiseVariant(
                    id: rawVariant["id"] as? String ?? UUID().uuidString,
                    title: rawVariant["title"] as? String ?? "",
                    size: rawVariant["size"] as? String,
                    color: rawVariant["color"] as? String,
                    shopifyVariantId: rawVariant["shopifyVariantId"] as? String,
                    sku: rawVariant["sku"] as? String,
                    price: (rawVariant["price"] as? NSNumber)?.doubleValue ?? 0,
                    currency: rawVariant["currency"] as? String ?? "EUR",
                    availableForSale: rawVariant["availableForSale"] as? Bool ?? true
                )
            } ?? [],
            source: data["source"] as? String ?? "manual",
            isVisibleInApp: data["isVisibleInApp"] as? Bool ?? true,
            featured: data["featured"] as? Bool ?? false,
            sortOrder: (data["sortOrder"] as? NSNumber)?.intValue ?? 0,
            customBadge: data["customBadge"] as? String ?? "",
            customImageOverride: data["customImageOverride"] as? String ?? "",
            category: data["category"] as? String
                ?? data["collabCategory"] as? String
                ?? data["collection"] as? String
                ?? "",
            collabPartner: data["collabPartner"] as? String
                ?? data["collab"] as? String
                ?? "",
            shopifyCollectionHandles: (data["shopifyCollectionHandles"] as? [String]) ?? []
        )
    }
}
