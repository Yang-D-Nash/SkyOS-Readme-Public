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
    @Environment(\.colorScheme) private var colorScheme

    private var isPlaying: Bool {
        audioManager.currentlyPlayingId == track.trackId
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .top, spacing: 14) {
                AsyncImage(url: URL(string: track.artworkUrl100 ?? "")) { image in
                    image
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                        .frame(width: 78, height: 78)
                        .clipShape(RoundedRectangle(cornerRadius: 18))
                } placeholder: {
                    RoundedRectangle(cornerRadius: 18)
                        .fill(Color.gray.opacity(0.2))
                        .frame(width: 78, height: 78)
                }
                .overlay(
                    RoundedRectangle(cornerRadius: 18)
                        .stroke(AppColors.accent(for: colorScheme).opacity(0.16), lineWidth: 1)
                )

                VStack(alignment: .leading, spacing: 8) {
                    HStack(alignment: .center, spacing: 8) {
                        if let artist = track.artistName, !artist.isEmpty {
                            Text(artist.uppercased())
                                .font(.caption.weight(.bold))
                                .foregroundColor(AppColors.accent(for: colorScheme))
                                .lineLimit(1)
                        }

                        Spacer(minLength: 0)

                        if isPlaying {
                            TrackTag(text: "Laeuft", isAccent: true)
                        } else if track.previewUrl != nil {
                            TrackTag(text: "Preview", isAccent: false)
                        }
                    }

                    Text(track.trackName)
                        .font(.headline.weight(.semibold))
                        .foregroundColor(AppColors.text(for: colorScheme))
                        .lineLimit(2)

                    if let artist = track.artistName, !artist.isEmpty {
                        Text("Von \(artist)")
                            .font(.subheadline)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                            .lineLimit(1)
                    }

                    if let album = track.collectionName, !album.isEmpty {
                        Text(album)
                            .font(.subheadline)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                            .lineLimit(1)
                    }

                    HStack(spacing: 8) {
                        if track.previewUrl != nil {
                            TrackTag(text: "In-App Preview", isAccent: false)
                        }
                        if track.externalURL != nil {
                            TrackTag(text: "Spotify Premium", isAccent: false)
                        }
                    }
                }
            }

            HStack(spacing: 10) {
                if track.previewUrl != nil {
                    Button {
                        audioManager.playPreview(for: track)
                    } label: {
                        Label(
                            isPlaying ? "Preview stoppen" : "Preview abspielen",
                            systemImage: isPlaying ? "pause.fill" : "play.fill"
                        )
                        .font(.subheadline.weight(.semibold))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .background(AppColors.accent(for: colorScheme))
                        .foregroundColor(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                    }
                }

                if track.externalURL != nil {
                    Button {
                        openSpotifyTrack()
                    } label: {
                        Label("Spotify Premium", systemImage: "arrow.up.forward.circle.fill")
                            .font(.subheadline.weight(.semibold))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                            .background(AppColors.secondaryBackground(for: colorScheme))
                            .foregroundColor(AppColors.text(for: colorScheme))
                            .overlay(
                                RoundedRectangle(cornerRadius: 14)
                                    .stroke(AppColors.accent(for: colorScheme).opacity(0.22), lineWidth: 1)
                            )
                            .clipShape(RoundedRectangle(cornerRadius: 14))
                    }
                }
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 22)
                .fill(AppColors.cardBackground(for: colorScheme))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 22)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.12), lineWidth: 1)
        )
        .shadow(
            color: Color.black.opacity(colorScheme == .dark ? 0.18 : 0.06),
            radius: 12,
            x: 0,
            y: 8
        )
    }

    private func openSpotifyTrack() {
        if let appURL = spotifyAppURL {
            openURL(appURL) { accepted in
                if !accepted, let webURL = spotifyWebURL {
                    openURL(webURL)
                }
            }
        } else if let webURL = spotifyWebURL {
            openURL(webURL)
        }
    }

    private var spotifyWebURL: URL? {
        guard let externalURL = track.externalURL else { return nil }
        return URL(string: externalURL)
    }

    private var spotifyAppURL: URL? {
        guard let webURL = spotifyWebURL else { return nil }
        let components = webURL.pathComponents
        guard let trackIndex = components.firstIndex(of: "track"),
              trackIndex + 1 < components.count else {
            return nil
        }
        return URL(string: "spotify:track:\(components[trackIndex + 1])")
    }
}

private struct TrackTag: View {
    let text: String
    let isAccent: Bool
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        Text(text)
            .font(.caption.weight(.semibold))
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(
                isAccent
                ? AppColors.accent(for: colorScheme)
                : AppColors.accent(for: colorScheme).opacity(0.12)
            )
            .foregroundColor(isAccent ? .white : AppColors.accent(for: colorScheme))
            .clipShape(Capsule())
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
        wrapperType: "track",
        releaseDate: nil
    )
    
    let audioManager = AudioPlayerManager()
    
    TrackView(track: sampleTrack, audioManager: audioManager)
        .padding()
}
