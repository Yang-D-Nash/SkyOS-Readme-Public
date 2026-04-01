import Foundation
import FirebaseFirestore
import FirebaseStorage

protocol MerchandiseServicing {
    func observeItems(_ onChange: @escaping @MainActor (Result<[MerchandiseItem], Error>) -> Void) -> () -> Void
    func addItem(_ item: MerchandiseItem) async throws
    func uploadImages(_ imageDataList: [Data]) async throws -> [String]
    func updateItem(_ item: MerchandiseItem, imageDataList: [Data]) async throws
    func updatePrice(itemID: String, newPrice: Double) async throws
    func deleteItem(itemID: String) async throws
}

final class FirebaseMerchandiseService: MerchandiseServicing {
    private let firestore: Firestore
    private let storage: Storage

    init(
        firestore: Firestore = Firestore.firestore(),
        storage: Storage = Storage.storage()
    ) {
        self.firestore = firestore
        self.storage = storage
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

    func addItem(_ item: MerchandiseItem) async throws {
        _ = try await firestore.collection("merchandise").addDocument(data: merchandisePayload(for: item, imageURLs: item.imageURLs))
    }

    func uploadImages(_ imageDataList: [Data]) async throws -> [String] {
        try await withThrowingTaskGroup(of: (Int, String).self) { taskGroup in
            for (index, imageData) in imageDataList.enumerated() {
                taskGroup.addTask { [storage] in
                    let uploadID = UUID().uuidString
                    let fileName = "image-\(index + 1).jpg"
                    let reference = storage.reference().child("merchandise/items/\(uploadID)/\(fileName)")
                    let metadata = StorageMetadata()
                    metadata.contentType = "image/jpeg"

                    _ = try await reference.putDataAsync(imageData, metadata: metadata)
                    let downloadURL = try await reference.downloadURL()
                    return (index, downloadURL.absoluteString)
                }
            }

            var indexedURLs: [(Int, String)] = []
            for try await result in taskGroup {
                indexedURLs.append(result)
            }

            return indexedURLs
                .sorted { $0.0 < $1.0 }
                .map(\.1)
        }
    }

    func updatePrice(itemID: String, newPrice: Double) async throws {
        try await firestore.collection("merchandise").document(itemID).updateData([
            "price": newPrice
        ])
    }

    func updateItem(_ item: MerchandiseItem, imageDataList: [Data]) async throws {
        guard let itemID = item.id else {
            throw NSError(
                domain: "FirebaseMerchandiseService",
                code: -1,
                userInfo: [NSLocalizedDescriptionKey: "Artikel hat keine gueltige ID."]
            )
        }

        let updatedImageURLs: [String]
        if imageDataList.isEmpty {
            updatedImageURLs = item.imageURLs
        } else {
            updatedImageURLs = try await uploadImages(imageDataList)
        }

        try await firestore.collection("merchandise").document(itemID).setData(
            merchandisePayload(for: item, imageURLs: updatedImageURLs),
            merge: true
        )

        if !imageDataList.isEmpty {
            await deleteImages(at: item.imageURLs)
        }
    }

    func deleteItem(itemID: String) async throws {
        let document = try await firestore.collection("merchandise").document(itemID).getDocument()
        let imageURLs = document.toMerchandiseItem()?.imageURLs ?? []
        try await firestore.collection("merchandise").document(itemID).delete()
        await deleteImages(at: imageURLs)
    }

    private func merchandisePayload(for item: MerchandiseItem, imageURLs: [String]) -> [String: Any] {
        [
            "name": item.name,
            "price": item.price,
            "description": item.description,
            "imageURLs": imageURLs,
            "imageUrls": imageURLs,
            "available": item.available,
            "currency": item.currency,
            "sku": item.sku as Any,
            "shopifyProductId": item.shopifyProductId as Any,
            "shopifyHandle": item.shopifyHandle as Any,
            "availableForSale": item.availableForSale,
            "variants": item.variants.map { variant in
                [
                    "id": variant.id,
                    "title": variant.title,
                    "size": variant.size as Any,
                    "color": variant.color as Any,
                    "shopifyVariantId": variant.shopifyVariantId as Any,
                    "sku": variant.sku as Any,
                    "price": variant.price,
                    "currency": variant.currency,
                    "availableForSale": variant.availableForSale
                ]
            },
            "source": item.source,
            "isVisibleInApp": item.isVisibleInApp,
            "featured": item.featured,
            "sortOrder": item.sortOrder,
            "customBadge": item.customBadge,
            "customImageOverride": item.customImageOverride
        ]
    }

    private func deleteImages(at imageURLs: [String]) async {
        guard !imageURLs.isEmpty else { return }

        await withTaskGroup(of: Void.self) { taskGroup in
            for imageURL in imageURLs {
                taskGroup.addTask { [storage] in
                    let reference = storage.reference(forURL: imageURL)
                    try? await reference.delete()
                }
            }
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
            customImageOverride: data["customImageOverride"] as? String ?? ""
        )
    }
}
