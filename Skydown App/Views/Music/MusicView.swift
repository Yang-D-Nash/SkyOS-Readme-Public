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

    let artists = ["Yang D. Nash", "ThaDude", "MAVE", "JANNO", "TANGAJOE007", "Toprack941"]

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    VStack(alignment: .leading, spacing: 10) {
                        Text("Skydown Music")
                            .font(.largeTitle.bold())

                        Text(selectedArtist)
                            .font(.title3.weight(.semibold))
                            .foregroundColor(AppColors.text(for: colorScheme))

                        Text("Waehle einen Artist, hoere eine Preview in der App und oeffne den kompletten Song direkt in Spotify.")
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

                    VStack(alignment: .leading, spacing: 12) {
                        Text("Artists")
                            .font(.headline)

                        Text("Alle Artists stehen jetzt untereinander, damit die Auswahl ruhiger und klarer bleibt.")
                            .font(.subheadline)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))

                        ForEach(artists, id: \.self) { artist in
                            Button {
                                selectedArtist = artist
                            } label: {
                                HStack(spacing: 12) {
                                    Image(systemName: selectedArtist == artist ? "checkmark.circle.fill" : "music.mic")
                                        .font(.title3)

                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(artist)
                                            .font(.headline)
                                            .lineLimit(1)

                                        Text(selectedArtist == artist ? "Jetzt aktiv" : "Tippen zum Laden")
                                            .font(.caption)
                                            .foregroundColor(
                                                selectedArtist == artist
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
                                        .fill(selectedArtist == artist ? AppColors.accent(for: colorScheme) : AppColors.secondaryBackground(for: colorScheme))
                                )
                                .overlay(
                                    RoundedRectangle(cornerRadius: 18)
                                        .stroke(
                                            selectedArtist == artist
                                            ? AppColors.accent(for: colorScheme)
                                            : AppColors.accent(for: colorScheme).opacity(0.18),
                                            lineWidth: 1
                                        )
                                )
                                .foregroundColor(selectedArtist == artist ? .white : AppColors.text(for: colorScheme))
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
                            .stroke(AppColors.accent(for: colorScheme).opacity(0.14), lineWidth: 1)
                    )

                    VStack(alignment: .leading, spacing: 14) {
                        Text("Spotify")
                            .font(.headline)

                        Text(
                            viewModel.isSpotifyConnected
                            ? "Spotify ist verbunden. Du kannst Previews testen und volle Songs direkt in Spotify oeffnen."
                            : "Verbinde Spotify, damit wir Songs fuer den aktuellen Artist laden koennen."
                        )
                        .font(.subheadline)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))

                        if !viewModel.isSpotifyConnected {
                            Button {
                                Task {
                                    await viewModel.connectSpotify()
                                    if viewModel.isSpotifyConnected {
                                        await viewModel.fetchTracks(for: selectedArtist)
                                    }
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
                        } else {
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

                    VStack(alignment: .leading, spacing: 14) {
                        Text("Tracks")
                            .font(.headline)

                        if !viewModel.isSpotifyConnected {
                            Text("Verbinde Spotify, um Tracks fuer den ausgewaehlten Artist zu laden.")
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
                            Text("\(viewModel.tracks.count) Titel fuer \(selectedArtist)")
                                .font(.subheadline)
                                .foregroundColor(AppColors.secondaryText(for: colorScheme))

                            LazyVStack(spacing: 12) {
                                ForEach(viewModel.tracks) { track in
                                    TrackView(track: track, audioManager: audioManager)
                                }
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
                            .stroke(AppColors.accent(for: colorScheme).opacity(0.14), lineWidth: 1)
                    )
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 14)
            }
            .background(AppColors.primaryBackground(for: colorScheme).ignoresSafeArea())
            .navigationTitle("Skydown Music")
            .task(id: selectedArtist) {
                audioManager.stop()
                if viewModel.isSpotifyConnected {
                    await viewModel.fetchTracks(for: selectedArtist)
                }
            }
        }
        .fancyToast(isPresented: $viewModel.showToast, message: viewModel.toastMessage, style: viewModel.toastStyle)
    }
}

private struct MusicBadge: View {
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
                ? AppColors.accent(for: colorScheme).opacity(0.15)
                : AppColors.secondaryBackground(for: colorScheme)
            )
            .foregroundColor(
                isAccent
                ? AppColors.accent(for: colorScheme)
                : AppColors.secondaryText(for: colorScheme)
            )
            .clipShape(Capsule())
    }
}

#Preview {
    MusicView()
}
