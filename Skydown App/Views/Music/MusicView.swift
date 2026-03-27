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
        return "Verbinde Spotify, damit wir Songs fuer den aktuellen Artist laden koennen."
    }

    private var tracksStatusText: String {
        "\(viewModel.tracks.count) Titel fuer \(selectedArtist)"
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    heroCard
                    artistsCard
                    spotifyCard
                    tracksCard
                    instagramCard
                    spotlightLinks
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 14)
            }
            .background(AppColors.primaryBackground(for: colorScheme).ignoresSafeArea())
            .navigationTitle("Skydown Music")
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
        }
        .fancyToast(
            isPresented: $viewModel.showToast,
            message: viewModel.toastMessage,
            style: viewModel.toastStyle
        )
    }

    private var heroCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Skydown Music")
                .font(.largeTitle.bold())

            Text(selectedArtist)
                .font(.title3.weight(.semibold))
                .foregroundColor(AppColors.text(for: colorScheme))

            Text("Waehle einen Artist, hoere eine Preview in der App oder oeffne den Spotify Player direkt hier.")
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            HStack(spacing: 8) {
                MusicBadge(
                    text: viewModel.isSpotifyConnected ? "Spotify verbunden" : "Spotify noch nicht verbunden",
                    isAccent: viewModel.isSpotifyConnected
                )

                if !viewModel.tracks.isEmpty {
                    MusicBadge(text: "\(viewModel.tracks.count) Songs", isAccent: false)
                }
            }
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 24)
                .fill(AppColors.cardBackground(for: colorScheme))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 24)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.18), lineWidth: 1)
        )
    }

    private var artistsCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Artists")
                .font(.headline)

            Text("Alle Artists stehen jetzt untereinander, damit die Auswahl ruhiger und klarer bleibt.")
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            ForEach(artists, id: \.self) { artist in
                artistButton(for: artist)
            }
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 24)
                .fill(AppColors.cardBackground(for: colorScheme))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 24)
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
                RoundedRectangle(cornerRadius: 18)
                    .fill(
                        isSelected
                        ? AppColors.accent(for: colorScheme)
                        : AppColors.secondaryBackground(for: colorScheme)
                    )
            )
            .overlay(
                RoundedRectangle(cornerRadius: 18)
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
                .background(AppColors.secondaryBackground(for: colorScheme))
                .foregroundColor(AppColors.text(for: colorScheme))
                .overlay(
                    RoundedRectangle(cornerRadius: 16)
                        .stroke(AppColors.accent(for: colorScheme).opacity(0.22), lineWidth: 1)
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
                .background(AppColors.accent(for: colorScheme))
                .foregroundColor(.white)
                .clipShape(RoundedRectangle(cornerRadius: 16))
            }
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 24)
                .fill(AppColors.cardBackground(for: colorScheme))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 24)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.14), lineWidth: 1)
        )
    }

    private var tracksCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Tracks")
                .font(.headline)

            tracksContent
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 24)
                .fill(AppColors.cardBackground(for: colorScheme))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 24)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.14), lineWidth: 1)
        )
    }

    @ViewBuilder
    private var tracksContent: some View {
        if !viewModel.isSpotifyConnected {
            Text("Verbinde Spotify, um Tracks fuer den ausgewaehlten Artist zu laden und den Spotify Player direkt hier zu starten.")
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
        } else if viewModel.tracks.isEmpty {
            if viewModel.showToast {
                EmptyView()
            } else {
                ProgressView("Lade Songs...")
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
        } else {
            Text(tracksStatusText)
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            LazyVStack(spacing: 12) {
                ForEach(viewModel.tracks) { track in
                    TrackView(track: track, audioManager: audioManager)
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
        if viewModel.isSpotifyConnected {
            await viewModel.fetchTracks(for: selectedArtist)
        }
    }
}

#Preview {
    MusicView()
}
