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

    let artists = ["Yang D. Nash", "ThaDude"]

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

                Group {
                    if viewModel.tracks.isEmpty {
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
                await viewModel.fetchTracks(for: selectedArtist)
            }
        }
        .fancyToast(isPresented: $viewModel.showToast, message: viewModel.toastMessage, style: viewModel.toastStyle)
    }
}

#Preview {
    MusicView()
}
