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
    let trackName: String
    let collectionName: String?
    let artworkUrl100: String?
    let previewUrl: String?
    let wrapperType: String?

    var id: Int { trackId }
}

struct SearchResult: Decodable {
    let results: [Track]
}
