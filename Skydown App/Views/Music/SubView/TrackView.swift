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
    let isSelected: Bool
    let onSelect: () -> Void
    @State private var showSpotifyPlayer = false
    @Environment(\.openURL) private var openURL
    @Environment(\.colorScheme) private var colorScheme

    private var isPlaying: Bool {
        audioManager.currentlyPlayingId == track.trackId
    }

    private var hasDirectSpotifyTrack: Bool {
        resolvedTrackSpotifyID(track) != nil
    }

    private var hasSpotifySearch: Bool {
        track.externalURL != nil && !hasDirectSpotifyTrack
    }

    private var hasSpotifyArtistLink: Bool {
        resolvedTrackSpotifyArtistID(track) != nil && !hasDirectSpotifyTrack
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top, spacing: 12) {
                AsyncImage(url: URL(string: track.artworkUrl100 ?? "")) { image in
                    image
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                        .frame(width: 64, height: 64)
                        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                } placeholder: {
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .fill(Color.gray.opacity(0.2))
                        .frame(width: 64, height: 64)
                }
                .overlay(
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .stroke(AppColors.accent(for: colorScheme).opacity(0.16), lineWidth: 1)
                )

                VStack(alignment: .leading, spacing: 8) {
                    HStack(alignment: .center, spacing: 8) {
                        if let artist = track.artistName, !artist.isEmpty {
                            Text(artist.uppercased())
                                .font(AppTypography.listMeta)
                                .foregroundColor(AppColors.accent(for: colorScheme))
                                .lineLimit(1)
                        }

                        Spacer(minLength: 0)

                        if isPlaying {
                            TrackTag(text: "Laeuft", isAccent: true)
                        } else if isSelected {
                            TrackTag(text: "Im Player", isAccent: false)
                        } else if track.previewUrl != nil {
                            TrackTag(text: "Preview", isAccent: false)
                        }
                    }

                    Text(track.trackName)
                        .font(AppTypography.listTitle)
                        .foregroundColor(AppColors.text(for: colorScheme))
                        .lineLimit(2)

                    if let artist = track.artistName, !artist.isEmpty {
                        Text("Von \(artist)")
                            .font(AppTypography.bodyCaption)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                            .lineLimit(1)
                    }

                    if let album = track.collectionName, !album.isEmpty {
                        Text(album)
                            .font(AppTypography.bodyCaption)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                            .lineLimit(1)
                    }

                    HStack(spacing: 8) {
                        if track.previewUrl != nil {
                            TrackTag(text: "In-App Preview", isAccent: false)
                        }
                        if hasDirectSpotifyTrack {
                            TrackTag(text: "Spotify Player", isAccent: false, tint: AppColors.spotify(for: colorScheme))
                        } else if hasSpotifyArtistLink {
                            TrackTag(text: "Spotify Artist", isAccent: false, tint: AppColors.spotify(for: colorScheme))
                        } else if hasSpotifySearch {
                            TrackTag(text: "Spotify Suche", isAccent: false, tint: AppColors.spotify(for: colorScheme))
                        }
                    }
                }
            }

            HStack(spacing: 10) {
                if track.previewUrl != nil {
                    Button {
                        onSelect()
                        audioManager.playPreview(for: track)
                    } label: {
                        Label(
                            isPlaying ? "Preview stoppen" : "Preview abspielen",
                            systemImage: isPlaying ? "pause.fill" : "play.fill"
                        )
                        .font(AppTypography.buttonLabel)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .background(AppColors.accent(for: colorScheme))
                        .foregroundColor(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                    }
                }

                if hasDirectSpotifyTrack {
                    Button {
                        onSelect()
                        showSpotifyPlayer = true
                    } label: {
                            Label("In App", systemImage: "music.note.tv")
                                .font(AppTypography.buttonLabel)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 12)
                                .background(AppColors.spotifySurface(for: colorScheme))
                                .foregroundColor(AppColors.spotify(for: colorScheme))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 14)
                                        .stroke(AppColors.spotify(for: colorScheme).opacity(0.28), lineWidth: 1)
                                )
                                .clipShape(RoundedRectangle(cornerRadius: 14))
                    }
                } else if hasSpotifyArtistLink || hasSpotifySearch {
                    Button {
                        onSelect()
                        if let url = URL(string: track.externalURL ?? "") {
                            openURL(url)
                        }
                    } label: {
                        Label(hasSpotifyArtistLink ? "Artist" : "Spotify", systemImage: "arrow.up.forward.square")
                            .font(AppTypography.buttonLabel)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                            .background(AppColors.spotifySurface(for: colorScheme))
                            .foregroundColor(AppColors.spotify(for: colorScheme))
                            .overlay(
                                RoundedRectangle(cornerRadius: 14)
                                    .stroke(AppColors.spotify(for: colorScheme).opacity(0.28), lineWidth: 1)
                            )
                            .clipShape(RoundedRectangle(cornerRadius: 14))
                    }
                }
            }
        }
        .padding(13)
        .frame(maxWidth: .infinity, alignment: .leading)
        .skydownPanelSurface(
            colorScheme: colorScheme,
            accent: isSelected ? AppColors.accent(for: colorScheme) : AppColors.accentMystic(for: colorScheme),
            cornerRadius: 22,
            shadowRadius: 9,
            shadowYOffset: 5
        )
        .onTapGesture(perform: onSelect)
        .sheet(isPresented: $showSpotifyPlayer) {
            SpotifyEmbedPlayerView(track: track)
        }
    }
}

private func trackSpotifyID(externalURL: String?) -> String? {
    guard let externalURL,
          let webURL = URL(string: externalURL),
          let components = URLComponents(url: webURL, resolvingAgainstBaseURL: false) else {
        return nil
    }

    let pathComponents = components.path.split(separator: "/")
    guard let trackIndex = pathComponents.firstIndex(of: "track"),
          trackIndex + 1 < pathComponents.count else {
        return nil
    }

    return String(pathComponents[trackIndex + 1])
}

private func resolvedTrackSpotifyID(_ track: Track) -> String? {
    if let spotifyTrackID = track.spotifyTrackID, !spotifyTrackID.isEmpty {
        return spotifyTrackID
    }
    return trackSpotifyID(externalURL: track.externalURL)
}

private func resolvedTrackSpotifyArtistID(_ track: Track) -> String? {
    if let spotifyArtistID = track.spotifyArtistID, !spotifyArtistID.isEmpty {
        return spotifyArtistID
    }

    guard let externalURL = track.externalURL,
          let webURL = URL(string: externalURL),
          let components = URLComponents(url: webURL, resolvingAgainstBaseURL: false) else {
        return nil
    }

    let pathComponents = components.path.split(separator: "/")
    guard let artistIndex = pathComponents.firstIndex(of: "artist"),
          artistIndex + 1 < pathComponents.count else {
        return nil
    }

    return String(pathComponents[artistIndex + 1])
}

private struct TrackTag: View {
    let text: String
    let isAccent: Bool
    var tint: Color?
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        let resolvedTint = tint ?? AppColors.accent(for: colorScheme)
        Text(text)
            .font(.caption.weight(.semibold))
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(
                isAccent
                ? resolvedTint
                : resolvedTint.opacity(0.12)
            )
            .foregroundColor(isAccent ? .white : resolvedTint)
            .clipShape(Capsule())
    }
}


#Preview {
    let sampleTrack = Track(
        trackId: 1,
        artistId: 123,
        spotifyArtistID: "sample-artist-id",
        spotifyTrackID: "sample-track-id",
        artistName: "Skydown",
        trackName: "Beispiel Song",
        collectionName: "Beispiel Album",
        artworkUrl100: "",
        previewUrl: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
        externalURL: "https://open.spotify.com/track/sample-track-id",
        wrapperType: "track",
        releaseDate: nil
    )
    
    let audioManager = AudioPlayerManager()
    
    TrackView(
        track: sampleTrack,
        audioManager: audioManager,
        isSelected: true
    ) {}
        .padding()
}
