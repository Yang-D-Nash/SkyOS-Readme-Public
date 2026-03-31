//
//  VideoHubSupport.swift
//  Skydown App
//
//  Created by Codex on 31.03.26.
//

import Foundation
import UniformTypeIdentifiers

let supportedVideoContentTypes: [UTType] = [
    .movie,
    .mpeg4Movie,
    .quickTimeMovie,
    UTType(filenameExtension: "m4v") ?? .movie
]

let skydownVideoDateFormatter: DateFormatter = {
    let formatter = DateFormatter()
    formatter.locale = Locale(identifier: "de_DE")
    formatter.dateFormat = "dd.MM.yyyy"
    return formatter
}()
