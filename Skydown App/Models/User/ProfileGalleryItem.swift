import Foundation

enum ProfileMediaType: String, CaseIterable, Identifiable, Codable {
    case image

    var id: String { rawValue }

    var title: String {
        "Bilder"
    }

    var systemImage: String {
        "photo.fill"
    }

    var storageFolder: String {
        "gallery"
    }
}

struct ProfileGalleryItem: Codable, Identifiable {
    var id: String?
    var ownerId: String
    var type: String
    var title: String
    var caption: String?
    var mediaURL: String
    var thumbnailURL: String?
    var createdAt: Date

    var mediaType: ProfileMediaType {
        ProfileMediaType(rawValue: type) ?? .image
    }
}
