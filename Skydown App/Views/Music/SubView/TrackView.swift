//
//  TrackView.swift
//  Skydown App
//
//  Created by Yang D. Nash on 19.08.25.
//

import SwiftUI

struct TrackView: View {
    let track: Track
    @ObservedObject var audioManager: AudioPlayerManager
    @Environment(\.openURL) private var openURL

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            AsyncImage(url: URL(string: track.artworkUrl100 ?? "")) { image in
                image
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .frame(width: 60, height: 60)
                    .cornerRadius(8)
            } placeholder: {
                Rectangle()
                    .fill(Color.gray.opacity(0.3))
                    .frame(width: 60, height: 60)
                    .cornerRadius(8)
            }

            VStack(alignment: .leading, spacing: 4) {
                Text(track.trackName)
                    .font(.headline)
                    .lineLimit(2)
                if let album = track.collectionName {
                    Text(album)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                        .lineLimit(1)
                }

                if track.previewUrl != nil {
                    Button {
                        audioManager.playPreview(for: track)
                    } label: {
                        Label(
                            audioManager.currentlyPlayingId == track.trackId ? "Pause" : "Preview",
                            systemImage: audioManager.currentlyPlayingId == track.trackId ? "pause.circle.fill" : "play.circle.fill"
                        )
                        .labelStyle(.titleAndIcon)
                        .foregroundColor(.blue)
                    }
                    .padding(.top, 4)
                } else if let externalURL = track.externalURL,
                          let url = URL(string: externalURL) {
                    Button {
                        openURL(url)
                    } label: {
                        Label("In Spotify", systemImage: "arrow.up.forward.circle.fill")
                            .labelStyle(.titleAndIcon)
                            .foregroundColor(.green)
                    }
                    .padding(.top, 4)
                }
            }
        }
        .padding(.vertical, 6)
    }
}


#Preview {
    let sampleTrack = Track(
        trackId: 1,
        artistId: 123,
        spotifyArtistID: "sample-artist-id",
        artistName: "Skydown",
        trackName: "Beispiel Song",
        collectionName: "Beispiel Album",
        artworkUrl100: "https://via.placeholder.com/100",
        previewUrl: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
        externalURL: "https://open.spotify.com",
        wrapperType: "track"
    )
    
    let audioManager = AudioPlayerManager()
    
    TrackView(track: sampleTrack, audioManager: audioManager)
        .padding()
}
