//
//  TrackView.swift
//  Skydown App
//
//  Created by Yang D. Nash on 19.08.25.
//

import SwiftUI

enum TrackListPresentation {
    /// Erster Titel – volle Hierarchie.
    case featured
    /// Zweiter Titel – noch im Set, aber leichter.
    case secondary
    /// Katalog-Rest.
    case catalog
}

struct TrackView: View {
    let track: Track
    @ObservedObject var audioManager: AudioPlayerManager
    /// Music-Katalog: `false` — In-App-Previews laufen auf der Artist Page.
    var allowsInAppPreview: Bool = true
    let isSelected: Bool
    let onSelect: () -> Void
    var presentation: TrackListPresentation = .featured
    @State private var showSpotifyPlayer = false
    @Environment(\.openURL) private var openURL
    @Environment(\.colorScheme) private var colorScheme

    private var isPlaying: Bool {
        allowsInAppPreview && (audioManager.currentlyPlayingId == track.trackId)
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

    private var catalog: Bool { presentation == .catalog }
    private var isSecondary: Bool { presentation == .secondary }
    private var artSize: CGFloat {
        switch presentation {
        case .catalog: 52
        case .secondary: 58
        case .featured: 64
        }
    }
    private var artCorner: CGFloat {
        switch presentation {
        case .catalog: 13
        case .secondary: 15
        case .featured: 16
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: catalog ? 9 : 12) {
            HStack(alignment: .top, spacing: catalog ? 10 : 12) {
                AsyncImage(url: URL(string: track.artworkUrl100 ?? "")) { image in
                    image
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                        .frame(width: artSize, height: artSize)
                        .clipShape(RoundedRectangle(cornerRadius: artCorner, style: .continuous))
                } placeholder: {
                    RoundedRectangle(cornerRadius: artCorner, style: .continuous)
                        .fill(Color.gray.opacity(0.2))
                        .frame(width: artSize, height: artSize)
                }
                .overlay(
                    RoundedRectangle(cornerRadius: artCorner, style: .continuous)
                        .stroke(
                            AppColors.accent(for: colorScheme).opacity(catalog ? 0.08 : (isSecondary ? 0.12 : 0.16)),
                            lineWidth: catalog ? 0.5 : (isSecondary ? 0.75 : 1)
                        )
                )

                VStack(alignment: .leading, spacing: catalog ? 3 : (isSecondary ? 5 : 8)) {
                    if catalog {
                        HStack(alignment: .center, spacing: 6) {
                            if let artist = track.artistName, !artist.isEmpty {
                                Text(artist.uppercased())
                                    .font(.caption2.weight(.semibold))
                                    .foregroundColor(AppColors.accent(for: colorScheme).opacity(0.9))
                                    .lineLimit(1)
                            }
                            Spacer(minLength: 0)
                            if isPlaying {
                                TrackTag(text: "Laeuft", isAccent: true)
                            } else if isSelected {
                                TrackTag(text: "Im Set", isAccent: false)
                            }
                        }
                    } else {
                    HStack(alignment: .center, spacing: 8) {
                        if let artist = track.artistName, !artist.isEmpty {
                            Text(artist.uppercased())
                                .font(isSecondary ? .caption2.weight(.semibold) : AppTypography.listMeta)
                                .foregroundColor(AppColors.accent(for: colorScheme).opacity(isSecondary ? 0.9 : 1.0))
                                .lineLimit(1)
                        }

                        Spacer(minLength: 0)

                        if isPlaying {
                            TrackTag(text: "Laeuft", isAccent: true)
                        } else if isSelected {
                            TrackTag(text: isSecondary ? "Im Set" : "Im Player", isAccent: false)
                        } else if allowsInAppPreview, track.previewUrl != nil, presentation == .secondary {
                            TrackTag(text: "Preview", isAccent: false)
                        }
                    }
                    }

                    Text(track.trackName)
                        .font(
                            catalog
                            ? .subheadline.weight(.semibold)
                            : (isSecondary ? .headline.weight(.semibold) : AppTypography.listTitle)
                        )
                        .foregroundColor(catalog ? AppColors.text(for: colorScheme).opacity(0.9) : AppColors.text(for: colorScheme))
                        .lineLimit(2)

                    if !catalog, !isSecondary, let artist = track.artistName, !artist.isEmpty {
                        Text("Von \(artist)")
                            .font(AppTypography.bodyCaption)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                            .lineLimit(1)
                    }

                    if !catalog, !isSecondary, let album = track.collectionName, !album.isEmpty {
                        Text(album)
                            .font(AppTypography.bodyCaption)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                            .lineLimit(1)
                    }

                    if isSecondary {
                    HStack(spacing: 8) {
                        if allowsInAppPreview, track.previewUrl != nil {
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
            }

            HStack(spacing: 10) {
                if allowsInAppPreview, track.previewUrl != nil {
                    Button {
                        onSelect()
                        withAnimation(.easeInOut(duration: 0.20)) {
                            audioManager.playPreview(for: track)
                        }
                    } label: {
                        Label(
                            isPlaying ? "Preview pausieren" : "Preview anhoeren",
                            systemImage: isPlaying ? "pause.fill" : "play.fill"
                        )
                        .font(AppTypography.buttonLabel)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .background(AppColors.accent(for: colorScheme))
                        .foregroundColor(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                    }
                    .skydownTactileAction()
                }

                if hasDirectSpotifyTrack {
                    Button {
                        onSelect()
                        showSpotifyPlayer = true
                    } label: {
                        Text("In Spotify weiter")
                            .font(AppTypography.buttonLabel)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 10)
                            .background(AppColors.spotifySurface(for: colorScheme))
                            .foregroundColor(AppColors.spotify(for: colorScheme))
                            .overlay(
                                RoundedRectangle(cornerRadius: 14)
                                    .stroke(AppColors.spotify(for: colorScheme).opacity(0.24), lineWidth: 1)
                            )
                            .clipShape(RoundedRectangle(cornerRadius: 14))
                    }
                    .skydownTactileAction()
                } else if hasSpotifyArtistLink || hasSpotifySearch {
                    Button {
                        onSelect()
                        if let url = URL(string: track.externalURL ?? "") {
                            openURL(url)
                        }
                    } label: {
                        Text(hasSpotifyArtistLink ? "Zum Artist" : "In Spotify suchen")
                            .font(AppTypography.buttonLabel)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 10)
                            .background(AppColors.spotifySurface(for: colorScheme))
                            .foregroundColor(AppColors.spotify(for: colorScheme))
                            .overlay(
                                RoundedRectangle(cornerRadius: 14)
                                    .stroke(AppColors.spotify(for: colorScheme).opacity(0.24), lineWidth: 1)
                            )
                            .clipShape(RoundedRectangle(cornerRadius: 14))
                    }
                    .skydownTactileAction()
                }
            }
        }
        .padding(
            presentation == .catalog
            ? 9
            : (presentation == .secondary ? 11 : 13)
        )
        .frame(maxWidth: .infinity, alignment: .leading)
        .skydownPanelSurface(
            colorScheme: colorScheme,
            accent: isSelected ? AppColors.accent(for: colorScheme) : AppColors.accentMystic(for: colorScheme),
            cornerRadius: catalog ? 17 : (isSecondary ? 20 : 22),
            shadowRadius: catalog ? 3 : (isSecondary ? 5 : 9),
            shadowYOffset: catalog ? 1 : (isSecondary ? 3 : 5)
        )
        .onTapGesture(perform: onSelect)
        .animation(.easeInOut(duration: 0.20), value: isPlaying)
        .animation(.easeInOut(duration: 0.20), value: isSelected)
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
