//
//  MusicView.swift
//  Skydown App
//
//  Created by Yang D. Nash on 23.07.25.
//

import SwiftUI

struct MusicView: View {
    @StateObject private var viewModel = MusicViewModel()
    @StateObject private var audioManager = AudioPlayerManager()
    @State private var selectedArtist = "Yang D. Nash"
    @State private var selectedTrackID: Int?
    @State private var showFeaturedSpotifyPlayer = false
    @Environment(\.colorScheme) private var colorScheme

    let onOpenCart: () -> Void
    let onOpenSettings: () -> Void

    init(
        onOpenCart: @escaping () -> Void = {},
        onOpenSettings: @escaping () -> Void = {}
    ) {
        self.onOpenCart = onOpenCart
        self.onOpenSettings = onOpenSettings
    }

    private var artists: [String] {
        ["Yang D. Nash", "ThaDude", "MAVE", "JANNO", "TANGAJOE007", "Toprack941"]
    }

    private var instagramDestinations: [MusicInstagramDestination] {
        [
            artistInstagramDestinations[selectedArtist],
            skydownMusicInstagramDestination,
            zweizweiInstagramDestination,
        ].compactMap { $0 }
    }

    private var spotifyStatusText: String {
        if viewModel.isSpotifyConnected {
            return "Spotify ist verbunden. Du kannst Previews testen oder den Spotify Player direkt in der App oeffnen."
        }
        return "Previews laufen direkt in der App. Spotify ist optional, falls du kompatible Tracks zusaetzlich im In-App-Player oeffnen willst."
    }

    private var tracksStatusText: String {
        "\(viewModel.tracks.count) Titel fuer \(selectedArtist)"
    }

    private var selectedTrack: Track? {
        viewModel.tracks.first(where: { $0.trackId == selectedTrackID }) ?? viewModel.tracks.first
    }

    private var trackIDs: [Int] {
        viewModel.tracks.map(\.trackId)
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: SkydownLayout.sectionSpacing) {
                    heroCard
                    artistsCard
                    spotifyCard
                    musicPlayerCard
                    tracksCard
                    instagramCard
                    spotlightLinks
                }
                .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
                .padding(.top, SkydownLayout.screenTopPadding)
                .padding(.bottom, SkydownLayout.screenBottomPadding)
            }
            .scrollIndicators(.hidden)
            .background(AppColors.screenGradient(for: colorScheme).ignoresSafeArea())
            .navigationTitle("Skydown Music")
            .skydownNavigationChrome(colorScheme: colorScheme)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    AppSessionToolbarActions(
                        onOpenCart: onOpenCart,
                        onOpenSettings: onOpenSettings
                    )
                }
            }
            .task(id: selectedArtist) {
                await reloadTracksIfNeeded()
            }
            .onChange(of: trackIDs) { _ in
                guard !viewModel.tracks.isEmpty else {
                    selectedTrackID = nil
                    return
                }

                if selectedTrackID == nil || !viewModel.tracks.contains(where: { $0.trackId == selectedTrackID }) {
                    selectedTrackID = viewModel.tracks.first?.trackId
                }
            }
            .onChange(of: audioManager.currentlyPlayingId) { playingID in
                if let playingID {
                    selectedTrackID = playingID
                }
            }
        }
        .fancyToast(
            isPresented: $viewModel.showToast,
            message: viewModel.toastMessage,
            style: viewModel.toastStyle
        )
        .sheet(isPresented: $showFeaturedSpotifyPlayer) {
            if let selectedTrack {
                SpotifyEmbedPlayerView(track: selectedTrack)
            }
        }
    }

    private var heroCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Skydown Music")
                .font(.largeTitle.bold())

            Text(selectedArtist)
                .font(.headline.weight(.semibold))
                .foregroundColor(AppColors.text(for: colorScheme))

            Text("Artist waehlen, Preview starten oder Spotify direkt oeffnen.")
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
        }
        .padding(SkydownLayout.heroPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: SkydownLayout.heroCornerRadius)
                .fill(AppColors.cardBackground(for: colorScheme))
        )
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.heroCornerRadius)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.18), lineWidth: 1)
        )
    }

    private var artistsCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Artists")
                .font(.headline)

            ForEach(artists, id: \.self) { artist in
                artistButton(for: artist)
            }
        }
        .padding(SkydownLayout.cardPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius)
                .fill(AppColors.cardBackground(for: colorScheme))
        )
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.14), lineWidth: 1)
        )
    }

    private func artistButton(for artist: String) -> some View {
        let isSelected = selectedArtist == artist

        return Button {
            selectedArtist = artist
        } label: {
            HStack(spacing: 12) {
                Image(systemName: isSelected ? "checkmark.circle.fill" : "music.mic")
                    .font(.title3)

                VStack(alignment: .leading, spacing: 2) {
                    Text(artist)
                        .font(.headline)
                        .lineLimit(1)

                    Text(isSelected ? "Jetzt aktiv" : "Tippen zum Laden")
                        .font(.caption)
                        .foregroundColor(
                            isSelected
                            ? Color.white.opacity(0.82)
                            : AppColors.secondaryText(for: colorScheme)
                        )
                }

                Spacer()
            }
            .frame(maxWidth: .infinity)
            .padding(.horizontal, 14)
            .padding(.vertical, 13)
            .background(
                RoundedRectangle(cornerRadius: SkydownLayout.buttonCornerRadius)
                    .fill(
                        isSelected
                        ? AppColors.accent(for: colorScheme)
                        : AppColors.secondaryBackground(for: colorScheme)
                    )
            )
            .overlay(
                RoundedRectangle(cornerRadius: SkydownLayout.buttonCornerRadius)
                    .stroke(
                        isSelected
                        ? AppColors.accent(for: colorScheme)
                        : AppColors.accent(for: colorScheme).opacity(0.18),
                        lineWidth: 1
                    )
            )
            .foregroundColor(isSelected ? .white : AppColors.text(for: colorScheme))
        }
        .buttonStyle(.plain)
    }

    private var spotifyCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Spotify")
                .font(.headline)

            Text(spotifyStatusText)
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            if viewModel.isSpotifyConnected {
                Button(role: .destructive) {
                    audioManager.stop()
                    viewModel.disconnectSpotify()
                } label: {
                    Label("Spotify trennen", systemImage: "rectangle.portrait.and.arrow.right")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                }
                .background(AppColors.spotifySurface(for: colorScheme))
                .foregroundColor(AppColors.spotify(for: colorScheme))
                .overlay(
                    RoundedRectangle(cornerRadius: 16)
                        .stroke(AppColors.spotify(for: colorScheme).opacity(0.28), lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: 16))
            } else {
                Button {
                    Task {
                        await connectSpotifyAndLoadTracks()
                    }
                } label: {
                    if viewModel.isConnectingSpotify {
                        ProgressView()
                            .tint(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                    } else {
                        Label("Spotify verbinden", systemImage: "music.note")
                            .font(.headline)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                    }
                }
                .background(AppColors.spotify(for: colorScheme))
                .foregroundColor(.black)
                .clipShape(RoundedRectangle(cornerRadius: 16))
            }
        }
        .padding(SkydownLayout.cardPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius)
                .fill(AppColors.cardBackground(for: colorScheme))
        )
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.14), lineWidth: 1)
        )
    }

    private var tracksCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Tracks")
                .font(.headline)

            tracksContent
        }
        .padding(SkydownLayout.cardPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius)
                .fill(AppColors.cardBackground(for: colorScheme))
        )
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.14), lineWidth: 1)
        )
    }

    @ViewBuilder
    private var musicPlayerCard: some View {
        if let selectedTrack {
            VStack(alignment: .leading, spacing: 14) {
                Text("Song Player")
                    .font(.headline)

                HStack(alignment: .top, spacing: 14) {
                    AsyncImage(url: URL(string: selectedTrack.artworkUrl100 ?? "")) { image in
                        image
                            .resizable()
                            .aspectRatio(contentMode: .fill)
                    } placeholder: {
                        RoundedRectangle(cornerRadius: 20)
                            .fill(AppColors.secondaryBackground(for: colorScheme))
                    }
                    .frame(width: 86, height: 86)
                    .clipShape(RoundedRectangle(cornerRadius: 20))

                    VStack(alignment: .leading, spacing: 6) {
                        Text(selectedTrack.trackName)
                            .font(.headline)
                            .foregroundColor(AppColors.text(for: colorScheme))

                        Text(selectedTrack.artistName ?? "Skydown x 22")
                            .font(.subheadline)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))

                        if let album = selectedTrack.collectionName, !album.isEmpty {
                            Text(album)
                                .font(.caption)
                                .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        }
                    }
                }

                HStack(spacing: 10) {
                    if selectedTrack.previewUrl != nil {
                        Button {
                            audioManager.playPreview(for: selectedTrack)
                        } label: {
                            Label(
                                audioManager.currentlyPlayingId == selectedTrack.trackId ? "Preview stoppen" : "Preview starten",
                                systemImage: audioManager.currentlyPlayingId == selectedTrack.trackId ? "pause.fill" : "play.fill"
                            )
                            .font(.subheadline.weight(.semibold))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                            .background(AppColors.accent(for: colorScheme))
                            .foregroundColor(.white)
                            .clipShape(RoundedRectangle(cornerRadius: 14))
                        }
                    }

                    if resolvedMusicViewSpotifyTrackID(selectedTrack) != nil {
                        Button {
                            showFeaturedSpotifyPlayer = true
                        } label: {
                            Label("Spotify Player", systemImage: "music.note.tv")
                                .font(.subheadline.weight(.semibold))
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
                    } else if let externalURL = selectedTrack.externalURL, let url = URL(string: externalURL) {
                        Link(destination: url) {
                            Label(
                                resolvedMusicViewSpotifyArtistID(selectedTrack) != nil ? "Spotify Artist" : "Spotify Suche",
                                systemImage: "arrow.up.forward.square"
                            )
                                .font(.subheadline.weight(.semibold))
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
            .padding(SkydownLayout.cardPadding)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius)
                    .fill(AppColors.cardBackground(for: colorScheme))
            )
            .overlay(
                RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius)
                    .stroke(AppColors.accent(for: colorScheme).opacity(0.14), lineWidth: 1)
            )
        }
    }

    @ViewBuilder
    private var tracksContent: some View {
        if viewModel.isLoadingTracks {
            ProgressView("Lade Songs...")
                .frame(maxWidth: .infinity, alignment: .leading)
        } else if viewModel.tracks.isEmpty {
            Text("Noch keine Songs fuer \(selectedArtist). Sobald ein Release verfuegbar ist, taucht er hier direkt auf.")
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
        } else {
            Text(tracksStatusText)
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            LazyVStack(spacing: 12) {
                ForEach(viewModel.tracks) { track in
                    TrackView(
                        track: track,
                        audioManager: audioManager,
                        isSelected: selectedTrackID == track.trackId,
                        onSelect: {
                            selectedTrackID = track.trackId
                        }
                    )
                }
            }
        }
    }

    private var instagramCard: some View {
        MusicInstagramHubCard(
            selectedArtist: selectedArtist,
            destinations: instagramDestinations,
            colorScheme: colorScheme
        )
    }

    private var spotlightLinks: some View {
        Group {
            NavigationLink {
                NicmaProducerView()
            } label: {
                NicmaProducerSpotlightCard(colorScheme: colorScheme)
            }
            .buttonStyle(.plain)

            NavigationLink {
                BeatHubView()
            } label: {
                BeatHubSpotlightCard(colorScheme: colorScheme)
            }
            .buttonStyle(.plain)
        }
    }

    private func connectSpotifyAndLoadTracks() async {
        await viewModel.connectSpotify()
        if viewModel.isSpotifyConnected {
            await viewModel.fetchTracks(for: selectedArtist)
        }
    }

    private func reloadTracksIfNeeded() async {
        audioManager.stop()
        await viewModel.fetchTracks(for: selectedArtist)
    }
}

private func musicViewTrackSpotifyID(externalURL: String?) -> String? {
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

private func resolvedMusicViewSpotifyTrackID(_ track: Track) -> String? {
    if let spotifyTrackID = track.spotifyTrackID, !spotifyTrackID.isEmpty {
        return spotifyTrackID
    }
    return musicViewTrackSpotifyID(externalURL: track.externalURL)
}

private func resolvedMusicViewSpotifyArtistID(_ track: Track) -> String? {
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

#Preview {
    MusicView()
}
