//
//  Tracks.swift
//  Skydown App
//
//  Created by Yang D. Nash on 26.07.25.
//
import Foundation

struct Track: Identifiable, Decodable {
    let trackId: Int
    let artistId: Int
    let spotifyArtistID: String?
    let spotifyTrackID: String?
    let artistName: String?
    let trackName: String
    let collectionName: String?
    let artworkUrl100: String?
    let previewUrl: String?
    let externalURL: String?
    let wrapperType: String?
    let releaseDate: String?

    var id: Int { trackId }
}

struct SearchResult: Decodable {
    let results: [Track]
}
