import Foundation

struct SkydownVideoEquipmentItem: Identifiable, Equatable {
    var id: String
    var title: String
    var detail: String
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
                detail: "DJI Neo und DJI Avata 2 fuer bewegte Luftshots und FPV-Looks."
            ),
            SkydownVideoEquipmentItem(
                id: "camera",
                title: "Kamera",
                detail: "Sony FX30 mit Sigma 18-50 mm f/2.8 plus Gimbals fuer saubere Motion-Shots."
            ),
            SkydownVideoEquipmentItem(
                id: "mobile-capture",
                title: "Mobile Capture",
                detail: "iPhone 16 Pro mit Apple Log fuer flexible schnelle Shoots."
            ),
            SkydownVideoEquipmentItem(
                id: "post-production",
                title: "Postproduktion",
                detail: "Adobe Premiere Pro, DaVinci Resolve Studio und Adobe After Effects."
            )
        ],
        youtubeItems: [],
        collaborationItems: defaultProducedWithArtists
    )
}

let defaultProducedWithArtists: [SkydownProducedWithArtist] = [
    SkydownProducedWithArtist(
        id: "produced-with-toprack941",
        name: "Toprack941",
        role: "Performance & Release Visuals",
        highlight: "Straighte Performance-Shots mit rauer Energie.",
        vibe: "Street Energy",
        imageURLString: nil,
        spotifyArtistID: "4CoozMQ3B3I20day60N7QA",
        instagramURLString: "https://www.instagram.com/toprack_941/"
    ),
    SkydownProducedWithArtist(
        id: "produced-with-directa",
        name: "Directa",
        role: "Artist Visuals",
        highlight: "Direkte Bildsprache fuer dunkle, klare Releases.",
        vibe: "Dark Cut",
        imageURLString: nil,
        spotifyArtistID: "2v4YJINKwkBY3DajPcmWm3",
        instagramURLString: "https://www.instagram.com/directascut/"
    ),
    SkydownProducedWithArtist(
        id: "produced-with-michael-klotz",
        name: "Michael Klotz",
        role: "Cinematic Sessions",
        highlight: "Mehr Ruhe, mehr Fokus, mehr cineastische Naehe.",
        vibe: "Cinematic",
        imageURLString: nil,
        spotifyArtistID: "4i8fdH54Ielws2ghRMPKGh",
        instagramURLString: "https://www.instagram.com/michael.ktz/"
    ),
    SkydownProducedWithArtist(
        id: "produced-with-jojee",
        name: "Jojee",
        role: "Performance Clips",
        highlight: "Klarer Front-Fokus mit Hook-Momenten im Bild.",
        vibe: "Performance",
        imageURLString: nil,
        spotifyArtistID: "2azLzTxCm662tbpPwPwQwI",
        instagramURLString: "https://www.instagram.com/iamjojee/"
    ),
    SkydownProducedWithArtist(
        id: "produced-with-sowjet020",
        name: "Sowjet020",
        role: "Release Visuals",
        highlight: "Roh, direkt und auf Attitude geschnitten.",
        vibe: "Raw",
        imageURLString: nil,
        spotifyArtistID: "2LLwIW8Nk30IYyC9sHr0Cm",
        instagramURLString: "https://www.instagram.com/sowjet020/"
    ),
    SkydownProducedWithArtist(
        id: "produced-with-cemo",
        name: "C€MO",
        role: "Artist Portraits",
        highlight: "Mehr Charakter im Close-up, weniger Distanz.",
        vibe: "Portrait",
        imageURLString: nil,
        spotifyArtistID: "0KD8un5on2oXfJ3dwxpw40",
        instagramURLString: "https://www.instagram.com/cemo_sd/"
    ),
    SkydownProducedWithArtist(
        id: "produced-with-daco27",
        name: "Daco27",
        role: "Motion Visuals",
        highlight: "Vorwaertsdrang, Bewegung und Tempo im Frame.",
        vibe: "Motion",
        imageURLString: nil,
        spotifyArtistID: "3oxSFNrPcoqo7qkAIsR74D",
        instagramURLString: "https://www.instagram.com/daco.27/"
    ),
    SkydownProducedWithArtist(
        id: "produced-with-neo",
        name: "Neo",
        role: "Creative Sessions",
        highlight: "Lockerere Sessions mit persoenlicherem Touch.",
        vibe: "Session",
        imageURLString: nil,
        spotifyArtistID: nil,
        instagramURLString: "https://www.instagram.com/sincity.neo/"
    ),
    SkydownProducedWithArtist(
        id: "produced-with-phil",
        name: "Phil",
        role: "Portrait Moments",
        highlight: "Ruhige Frames, menschlicher Look, klare Praesenz.",
        vibe: "Portrait",
        imageURLString: nil,
        spotifyArtistID: nil,
        instagramURLString: "https://www.instagram.com/philip_beau/"
    )
]
