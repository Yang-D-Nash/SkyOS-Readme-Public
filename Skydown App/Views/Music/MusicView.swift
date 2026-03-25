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

    let artists = ["Yang D. Nash", "ThaDude", "MAVE", "JANNO", "TANGAJOE007"]

    var body: some View {
        NavigationStack {
            VStack {
                Picker("Artist", selection: $selectedArtist) {
                    ForEach(artists, id: \.self) { artist in
                        Text(artist)
                    }
                }
                .pickerStyle(.segmented)
                .padding()

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
                        } else {
                            Label("Spotify verbinden", systemImage: "music.note")
                                .font(.headline)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(AppColors.accent(for: colorScheme))
                    .cornerRadius(12)
                    .foregroundColor(.white)
                    .padding(.horizontal)
                } else {
                    Button(role: .destructive) {
                        audioManager.stop()
                        viewModel.disconnectSpotify()
                    } label: {
                        Label("Spotify trennen", systemImage: "rectangle.portrait.and.arrow.right")
                            .font(.headline)
                            .frame(maxWidth: .infinity)
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(AppColors.cardBackground(for: colorScheme))
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(AppColors.accent(for: colorScheme).opacity(0.22), lineWidth: 1)
                    )
                    .cornerRadius(12)
                    .foregroundColor(AppColors.text(for: colorScheme))
                    .padding(.horizontal)
                }

                Group {
                    if !viewModel.isSpotifyConnected {
                        Spacer()
                        Text("Verbinde Spotify, um Tracks fuer den ausgewaehlten Artist zu laden.")
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                            .multilineTextAlignment(.center)
                            .padding(.horizontal)
                        Spacer()
                    } else if viewModel.tracks.isEmpty {
                        if viewModel.showToast {
                            Spacer()
                        } else {
                            Spacer()
                            ProgressView("Lade Songs...")
                            Spacer()
                        }
                    } else {
                        List(viewModel.tracks) { track in
                            TrackView(track: track, audioManager: audioManager)
                        }
                        .listStyle(.plain)
                    }
                }
            }
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

#Preview {
    MusicView()
}
