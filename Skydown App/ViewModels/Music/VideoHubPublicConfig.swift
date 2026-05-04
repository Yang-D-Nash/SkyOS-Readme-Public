import Foundation

struct SkydownVideoEquipmentItem: Identifiable, Equatable {
    var id: String
    var title: String
    var detail: String
    var imageURLString: String?
}

struct SkydownYouTubeVideoItem: Identifiable, Equatable {
    var id: String
    var title: String
    var subtitle: String
    var urlString: String
}

struct SkydownProducedWithArtist: Identifiable, Equatable {
    var id: String
    var name: String
    var role: String
    var highlight: String
    var vibe: String
    var imageURLString: String?
    var spotifyArtistID: String?
    var instagramURLString: String?
    var youtubeURLString: String?
}

struct SkydownVideoHubPublicConfig: Equatable {
    var equipmentItems: [SkydownVideoEquipmentItem]
    var youtubeItems: [SkydownYouTubeVideoItem]
    var collaborationItems: [SkydownProducedWithArtist]

    static let `default` = SkydownVideoHubPublicConfig(
        equipmentItems: [
            SkydownVideoEquipmentItem(
                id: "drones",
                title: "Drohnen",
                detail: "DJI Neo und DJI Avata 2 fuer bewegte Luftshots und FPV-Looks.",
                imageURLString: nil
            ),
            SkydownVideoEquipmentItem(
                id: "camera",
                title: "Kamera",
                detail: "Sony FX30 mit Sigma 18-50 mm f/2.8 plus Gimbals fuer saubere Motion-Shots.",
                imageURLString: nil
            ),
            SkydownVideoEquipmentItem(
                id: "mobile-capture",
                title: "Mobile Capture",
                detail: "iPhone 16 Pro mit Apple Log fuer flexible schnelle Shoots.",
                imageURLString: nil
            ),
            SkydownVideoEquipmentItem(
                id: "post-production",
                title: "Postproduktion",
                detail: "Adobe Premiere Pro, DaVinci Resolve Studio und Adobe After Effects.",
                imageURLString: nil
            )
        ],
        youtubeItems: [],
        collaborationItems: defaultProducedWithArtists
    )
}

let defaultProducedWithArtists: [SkydownProducedWithArtist] = []
