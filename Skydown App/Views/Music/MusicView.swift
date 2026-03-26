//
//  MusicView.swift
//  Skydown App
//
//  Created by Yang D. Nash on 23.07.25.
//

import AVFoundation
import AVKit
import SwiftUI
import UniformTypeIdentifiers

struct MusicView: View {
    @StateObject private var viewModel = MusicViewModel()
    @StateObject private var audioManager = AudioPlayerManager()
    @State private var selectedArtist = "Yang D. Nash"
    @Environment(\.colorScheme) private var colorScheme
    let onOpenCart: () -> Void = {}
    let onOpenSettings: () -> Void = {}

    let artists = ["Yang D. Nash", "ThaDude", "MAVE", "JANNO", "TANGAJOE007", "Toprack941"]

    private var instagramDestinations: [MusicInstagramDestination] {
        [
            artistInstagramDestinations[selectedArtist],
            skydownMusicInstagramDestination,
            zweizweiInstagramDestination,
        ].compactMap { $0 }
    }

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

                        Text("Waehle einen Artist, hoere eine Preview in der App und oeffne die komplette Wiedergabe mit Spotify Premium.")
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
                            ? "Spotify ist verbunden. Du kannst Previews testen, die komplette Wiedergabe in Spotify braucht aber Premium."
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
                            Text("Verbinde Spotify, um Tracks fuer den ausgewaehlten Artist zu laden. Fuer die komplette Wiedergabe in Spotify wird Premium benoetigt.")
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

                    MusicInstagramHubCard(
                        selectedArtist: selectedArtist,
                        destinations: instagramDestinations,
                        colorScheme: colorScheme
                    )

                    NavigationLink {
                        VideoHubView()
                    } label: {
                        VideoHubSpotlightCard(colorScheme: colorScheme)
                    }
                    .buttonStyle(.plain)

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

private struct NicmaProducerSpotlightCard: View {
    let colorScheme: ColorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .top, spacing: 14) {
                VStack(alignment: .leading, spacing: 8) {
                    Text("NICMA MUSIC")
                        .font(.title2.weight(.bold))
                        .foregroundColor(AppColors.text(for: colorScheme))

                    Text("Producer-Seite mit Preisliste fuer Mixing, Mastering und Recording.")
                        .font(.subheadline)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }

                Spacer()

                ZStack {
                    Circle()
                        .fill(AppColors.accentMystic(for: colorScheme).opacity(0.16))
                        .frame(width: 54, height: 54)

                    Image(systemName: "waveform.path.ecg")
                        .font(.title3)
                        .foregroundColor(AppColors.accentMystic(for: colorScheme))
                }
            }

            HStack(spacing: 8) {
                MusicBadge(text: "Mixing", isAccent: true)
                MusicBadge(text: "Mastering", isAccent: false)
                MusicBadge(text: "Studio Services", isAccent: false)
            }

            HStack {
                Text("Preise & Services oeffnen")
                    .font(.headline)
                    .foregroundColor(AppColors.accent(for: colorScheme))

                Spacer()

                Image(systemName: "arrow.right.circle.fill")
                    .font(.title3)
                    .foregroundColor(AppColors.accent(for: colorScheme))
            }
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            LinearGradient(
                colors: [
                    AppColors.cardBackground(for: colorScheme),
                    AppColors.secondaryBackground(for: colorScheme).opacity(0.92)
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .overlay(
            RoundedRectangle(cornerRadius: 24)
                .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.18), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 24))
    }
}

private struct BeatHubSpotlightCard: View {
    let colorScheme: ColorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .top, spacing: 14) {
                VStack(alignment: .leading, spacing: 8) {
                    Text("Beat Hub")
                        .font(.title2.weight(.bold))
                        .foregroundColor(AppColors.text(for: colorScheme))

                    Text("Eigener Bereich fuer Beat-Uploads, oeffentliche Hubsounds und Admin-Freigaben.")
                        .font(.subheadline)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }

                Spacer()

                ZStack {
                    Circle()
                        .fill(AppColors.accent(for: colorScheme).opacity(0.16))
                        .frame(width: 54, height: 54)

                    Image(systemName: "speaker.wave.3.fill")
                        .font(.title3)
                        .foregroundColor(AppColors.accent(for: colorScheme))
                }
            }

            HStack(spacing: 8) {
                MusicBadge(text: "Upload", isAccent: true)
                MusicBadge(text: "Listen", isAccent: false)
                MusicBadge(text: "Admin Review", isAccent: false)
            }

            HStack {
                Text("Beat Hub oeffnen")
                    .font(.headline)
                    .foregroundColor(AppColors.accent(for: colorScheme))

                Spacer()

                Image(systemName: "arrow.right.circle.fill")
                    .font(.title3)
                    .foregroundColor(AppColors.accent(for: colorScheme))
            }
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            LinearGradient(
                colors: [
                    AppColors.cardBackground(for: colorScheme),
                    AppColors.secondaryBackground(for: colorScheme).opacity(0.92)
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .overlay(
            RoundedRectangle(cornerRadius: 24)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.18), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 24))
    }
}

private struct VideoHubSpotlightCard: View {
    let colorScheme: ColorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .top, spacing: 14) {
                VStack(alignment: .leading, spacing: 8) {
                    Text("Videography")
                        .font(.title2.weight(.bold))
                        .foregroundColor(AppColors.text(for: colorScheme))

                    Text("Eigener Bereich fuer Skydown x 22 Videos mit Playback, Admin-Uploads und klaren Format-Hinweisen.")
                        .font(.subheadline)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }

                Spacer()

                ZStack {
                    Circle()
                        .fill(AppColors.accentMystic(for: colorScheme).opacity(0.16))
                        .frame(width: 54, height: 54)

                    Image(systemName: "video.fill")
                        .font(.title3)
                        .foregroundColor(AppColors.accentMystic(for: colorScheme))
                }
            }

            HStack(spacing: 8) {
                MusicBadge(text: "Playback", isAccent: true)
                MusicBadge(text: "Admin Upload", isAccent: false)
                MusicBadge(text: "MP4 / MOV / M4V", isAccent: false)
            }

            HStack {
                Text("Videography oeffnen")
                    .font(.headline)
                    .foregroundColor(AppColors.accent(for: colorScheme))

                Spacer()

                Image(systemName: "arrow.right.circle.fill")
                    .font(.title3)
                    .foregroundColor(AppColors.accent(for: colorScheme))
            }
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            LinearGradient(
                colors: [
                    AppColors.cardBackground(for: colorScheme),
                    AppColors.secondaryBackground(for: colorScheme).opacity(0.92)
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .overlay(
            RoundedRectangle(cornerRadius: 24)
                .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.18), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 24))
    }
}

private struct MusicInstagramHubCard: View {
    let selectedArtist: String
    let destinations: [MusicInstagramDestination]
    let colorScheme: ColorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Instagram")
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            Text("Hol dir mehr Vibe direkt ueber den aktuellen Artist, 22 und Skydown.")
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            ForEach(destinations) { destination in
                if let url = destination.url {
                    Link(destination: url) {
                        HStack(spacing: 12) {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(destination.title)
                                    .font(.headline)
                                    .foregroundColor(AppColors.text(for: colorScheme))
                                    .frame(maxWidth: .infinity, alignment: .leading)

                                Text(destination.subtitle(selectedArtist: selectedArtist))
                                    .font(.subheadline)
                                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                                    .frame(maxWidth: .infinity, alignment: .leading)
                            }

                            Image(systemName: "arrow.up.forward.circle.fill")
                                .font(.title3)
                                .foregroundColor(AppColors.accent(for: colorScheme))
                        }
                        .padding(.horizontal, 14)
                        .padding(.vertical, 14)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(AppColors.secondaryBackground(for: colorScheme))
                        .overlay(
                            RoundedRectangle(cornerRadius: 18)
                                .stroke(AppColors.accent(for: colorScheme).opacity(0.16), lineWidth: 1)
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 18))
                    }
                    .buttonStyle(.plain)
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
}

private struct NicmaProducerView: View {
    @Environment(\.colorScheme) private var colorScheme
    private var inquiryMailURL: URL? {
        URL(string: "mailto:nicoheine@me.com?subject=NICMA%20MUSIC%20Anfrage")
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                VStack(alignment: .leading, spacing: 12) {
                    Text("NICMA MUSIC")
                        .font(.largeTitle.bold())
                        .foregroundColor(AppColors.text(for: colorScheme))

                    Text("Mixing, Mastering und Recording mit klarer Preisliste, direktem Kontakt und sauberem Producer-Fokus.")
                        .font(.body)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))

                    HStack(spacing: 8) {
                        MusicBadge(text: "Studio Services", isAccent: true)
                        MusicBadge(text: "Mix & Master", isAccent: false)
                        MusicBadge(text: "Recording", isAccent: false)
                    }
                }
                .padding(20)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(
                    RoundedRectangle(cornerRadius: 26)
                        .fill(AppColors.cardBackground(for: colorScheme))
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 26)
                        .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.18), lineWidth: 1)
                )

                VStack(alignment: .leading, spacing: 14) {
                    Text("Preisliste")
                        .font(.headline)
                        .foregroundColor(AppColors.text(for: colorScheme))

                    ForEach(nicmaProducerPackages) { package in
                        NicmaProducerPriceCard(
                            package: package,
                            colorScheme: colorScheme
                        )
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

                VStack(alignment: .leading, spacing: 12) {
                    Text("Kontakt")
                        .font(.headline)
                        .foregroundColor(AppColors.text(for: colorScheme))

                    Text("Anfragen fuer Mixing, Mastering und Recording gehen direkt an nicoheine@me.com.")
                        .font(.subheadline)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))

                    if let inquiryMailURL {
                        Link(destination: inquiryMailURL) {
                            Label("NICMA MUSIC anfragen", systemImage: "envelope.fill")
                                .font(.headline)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 14)
                        }
                        .foregroundColor(.white)
                        .background(AppColors.accentMystic(for: colorScheme))
                        .clipShape(RoundedRectangle(cornerRadius: 18))
                    }

                    if let instagramURL = nicmaInstagramDestination.url {
                        Link(destination: instagramURL) {
                            Label("NICMA MUSIC auf Instagram", systemImage: "camera.fill")
                                .font(.headline)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 14)
                        }
                        .foregroundColor(AppColors.text(for: colorScheme))
                        .background(AppColors.secondaryBackground(for: colorScheme))
                        .overlay(
                            RoundedRectangle(cornerRadius: 18)
                                .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.18), lineWidth: 1)
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 18))
                    }

                    Text("Mail: nicoheine@me.com")
                        .font(.footnote)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }
                .padding(18)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(
                    RoundedRectangle(cornerRadius: 24)
                        .fill(AppColors.cardBackground(for: colorScheme))
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 24)
                        .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.16), lineWidth: 1)
                )
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
        }
        .background(AppColors.primaryBackground(for: colorScheme).ignoresSafeArea())
        .navigationTitle("NICMA MUSIC")
    }
}

struct VideoHubView: View {
    @EnvironmentObject private var authManager: AuthManager
    @Environment(\.colorScheme) private var colorScheme
    @StateObject private var viewModel = SkydownVideoHubViewModel()
    @StateObject private var playbackManager = VideoPlaybackManager()
    @State private var showingFileImporter = false

    private var selectedVideo: SkydownVideoHubItem? {
        viewModel.videos.first(where: { $0.id == playbackManager.selectedVideoID }) ?? viewModel.videos.first
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                VStack(alignment: .leading, spacing: 12) {
                    Text("Videography")
                        .font(.largeTitle.bold())
                        .foregroundColor(AppColors.text(for: colorScheme))

                    Text(viewModel.isAdmin
                         ? "Hier landen Reels, Clips, Sessions und Visuals. Als Admin kannst du Videos hochladen und eins direkt fuer Home auswaehlen."
                         : "Hier laufen die oeffentlichen Videos von Skydown x 22. Uploads und Pflege bleiben im Admin-Bereich.")
                        .font(.body)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))

                    HStack(spacing: 8) {
                        MusicBadge(text: "Videos", isAccent: true)
                        MusicBadge(text: viewModel.isAdmin ? "Admin Upload" : "Playback", isAccent: false)
                        MusicBadge(text: "Storage", isAccent: false)
                    }
                }
                .padding(20)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(
                    RoundedRectangle(cornerRadius: 26)
                        .fill(AppColors.cardBackground(for: colorScheme))
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 26)
                        .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.18), lineWidth: 1)
                )

                VStack(alignment: .leading, spacing: 12) {
                    Text("Format-Hinweis")
                        .font(.headline)
                        .foregroundColor(AppColors.text(for: colorScheme))

                    Text("Empfohlen sind MP4, MOV oder M4V. Am stabilsten laufen H.264 oder H.265 mit sauberem Export fuer mobile Wiedergabe.")
                        .font(.subheadline)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))

                    Text("Querformat und Hochformat funktionieren beide. Fuer schnellere Uploads sind komprimierte Social-Cuts besser als rohe Master-Dateien.")
                        .font(.footnote)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
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

                VideoEquipmentCard(colorScheme: colorScheme)

                if viewModel.isAdmin {
                    VStack(alignment: .leading, spacing: 14) {
                        Text("Video Upload")
                            .font(.headline)
                            .foregroundColor(AppColors.text(for: colorScheme))

                        Text("Nur Admins sehen diesen Bereich. Die Videos landen direkt in Firebase Storage und erscheinen danach in der Library.")
                            .font(.subheadline)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))

                        NicmaUploadField(
                            title: "Titel",
                            text: $viewModel.videoTitle,
                            colorScheme: colorScheme
                        )

                        NicmaUploadField(
                            title: "Projekt / Artist",
                            text: $viewModel.projectName,
                            colorScheme: colorScheme
                        )

                        NicmaUploadField(
                            title: "E-Mail",
                            text: $viewModel.email,
                            colorScheme: colorScheme,
                            keyboard: .emailAddress,
                            autocapitalization: .never
                        )

                        VStack(alignment: .leading, spacing: 8) {
                            Text("Notiz (optional)")
                                .font(.subheadline.weight(.semibold))
                                .foregroundColor(AppColors.text(for: colorScheme))

                            TextEditor(text: $viewModel.notes)
                                .frame(minHeight: 110)
                                .padding(12)
                                .background(AppColors.secondaryBackground(for: colorScheme))
                                .clipShape(RoundedRectangle(cornerRadius: 18))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 18)
                                        .stroke(AppColors.accent(for: colorScheme).opacity(0.12), lineWidth: 1)
                                )
                        }

                        Button {
                            showingFileImporter = true
                        } label: {
                            Label("Videos auswaehlen", systemImage: "video.badge.plus")
                                .font(.headline)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 14)
                        }
                        .buttonStyle(.bordered)
                        .tint(AppColors.accentMystic(for: colorScheme))

                        if !viewModel.selectedFiles.isEmpty {
                            VStack(spacing: 10) {
                                ForEach(viewModel.selectedFiles) { file in
                                    SkydownSelectedVideoRow(
                                        file: file,
                                        colorScheme: colorScheme
                                    ) {
                                        viewModel.removeFile(file.id)
                                    }
                                }
                            }
                        }

                        if let validationMessage = viewModel.validationMessage {
                            Text(validationMessage)
                                .font(.footnote)
                                .foregroundColor(.red)
                        }

                        Button {
                            Task {
                                await viewModel.uploadSelectedVideos()
                            }
                        } label: {
                            if viewModel.isUploading {
                                ProgressView()
                                    .tint(.white)
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 14)
                            } else {
                                Label("Videos hochladen", systemImage: "arrow.up.circle.fill")
                                    .font(.headline)
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 14)
                            }
                        }
                        .background(AppColors.accent(for: colorScheme))
                        .foregroundColor(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 18))
                        .disabled(!viewModel.canUpload)
                        .opacity(viewModel.canUpload ? 1 : 0.6)
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

                VStack(alignment: .leading, spacing: 14) {
                    Text("Player")
                        .font(.headline)
                        .foregroundColor(AppColors.text(for: colorScheme))

                    if let selectedVideo {
                        Text(selectedVideo.title)
                            .font(.title3.weight(.bold))
                            .foregroundColor(AppColors.text(for: colorScheme))

                        Text(selectedVideo.projectName)
                            .font(.subheadline)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))

                        VideoPlayer(player: playbackManager.player)
                            .frame(height: 240)
                            .clipShape(RoundedRectangle(cornerRadius: 20))

                        if !selectedVideo.notes.isEmpty {
                            Text(selectedVideo.notes)
                                .font(.subheadline)
                                .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        }
                    } else {
                        Text("Noch kein Video ausgewaehlt. Sobald Uploads live sind, kannst du sie hier direkt abspielen.")
                            .font(.subheadline)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
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
                    Text("Video Library")
                        .font(.headline)
                        .foregroundColor(AppColors.text(for: colorScheme))

                    if viewModel.isLoadingVideos {
                        ProgressView("Videography wird geladen ...")
                    } else if viewModel.videos.isEmpty {
                        Text(viewModel.isAdmin
                             ? "Noch keine Videos im Hub. Neue Uploads tauchen hier sofort auf."
                             : "Noch keine freigegebenen Videos. Sobald ein Clip live ist, kannst du ihn hier abspielen.")
                            .font(.subheadline)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    } else {
                        ForEach(viewModel.videos) { video in
                            VideoHubLibraryRow(
                                video: video,
                                isAdmin: viewModel.isAdmin,
                                isSelected: playbackManager.selectedVideoID == video.id,
                                isPlaying: playbackManager.playingVideoID == video.id,
                                colorScheme: colorScheme,
                                onSelect: { playbackManager.load(video: video) },
                                onPlayToggle: { playbackManager.togglePlayback(for: video) },
                                onToggleHomeFeatured: {
                                    Task {
                                        await viewModel.toggleHomeFeatured(video)
                                    }
                                },
                                onDelete: {
                                    Task {
                                        await viewModel.deleteVideo(video)
                                    }
                                }
                            )
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
        .navigationTitle("Videography")
        .task {
            viewModel.configure(currentUser: authManager.userSession)
        }
        .onReceive(authManager.$userSession) { user in
            viewModel.configure(currentUser: user)
        }
        .onReceive(viewModel.$videos) { videos in
            guard !videos.isEmpty else {
                playbackManager.stop()
                return
            }

            guard videos.contains(where: { $0.id == playbackManager.selectedVideoID }) else {
                playbackManager.load(video: videos.first)
                return
            }
        }
        .onDisappear {
            playbackManager.stop()
        }
        .fileImporter(
            isPresented: $showingFileImporter,
            allowedContentTypes: supportedVideoContentTypes,
            allowsMultipleSelection: true
        ) { result in
            viewModel.handleFileImport(result)
        }
        .fancyToast(
            isPresented: $viewModel.showToast,
            message: viewModel.toastMessage,
            style: viewModel.toastStyle
        )
    }
}

private struct BeatHubView: View {
    @EnvironmentObject private var authManager: AuthManager
    @Environment(\.colorScheme) private var colorScheme
    @StateObject private var viewModel = NicmaProducerViewModel()
    @StateObject private var playbackManager = BeatPlaybackManager()
    @State private var showingFileImporter = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                VStack(alignment: .leading, spacing: 12) {
                    Text("Beat Hub")
                        .font(.largeTitle.bold())
                        .foregroundColor(AppColors.text(for: colorScheme))

                    Text(viewModel.isAdmin
                         ? "Als Admin siehst du alle Beats, kannst Uploads steuern und Hoerproben direkt pruefen."
                         : "Hier hoerst du die freigegebenen Beats aus dem Hub. Uploads sind nur fuer Admins sichtbar.")
                        .font(.body)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))

                    HStack(spacing: 8) {
                        MusicBadge(text: viewModel.isAdmin ? "Upload" : "Curated", isAccent: true)
                        MusicBadge(text: "Listen", isAccent: false)
                        MusicBadge(text: viewModel.isAdmin ? "Admin aktiv" : "Public Beats", isAccent: false)
                    }
                }
                .padding(20)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(
                    RoundedRectangle(cornerRadius: 26)
                        .fill(AppColors.cardBackground(for: colorScheme))
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 26)
                        .stroke(AppColors.accent(for: colorScheme).opacity(0.18), lineWidth: 1)
                )

                if viewModel.isAdmin {
                    VStack(alignment: .leading, spacing: 14) {
                        Text("Beat Upload")
                            .font(.headline)
                            .foregroundColor(AppColors.text(for: colorScheme))

                        Text("Nur Admins laden neue Beats oder ZIP-Sessions hoch. Audio-Dateien koennen danach direkt im Hub getestet und bei Bedarf verborgen werden.")
                            .font(.subheadline)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))

                        NicmaUploadField(
                            title: "Beat Titel (optional)",
                            text: $viewModel.beatTitle,
                            colorScheme: colorScheme
                        )

                        NicmaUploadField(
                            title: "Artist / Projekt",
                            text: $viewModel.artistName,
                            colorScheme: colorScheme
                        )

                        NicmaUploadField(
                            title: "E-Mail",
                            text: $viewModel.email,
                            colorScheme: colorScheme,
                            keyboard: .emailAddress,
                            autocapitalization: .never
                        )

                        VStack(alignment: .leading, spacing: 8) {
                            Text("Notiz (optional)")
                                .font(.subheadline.weight(.semibold))
                                .foregroundColor(AppColors.text(for: colorScheme))

                            TextEditor(text: $viewModel.notes)
                                .frame(minHeight: 110)
                                .padding(12)
                                .background(AppColors.secondaryBackground(for: colorScheme))
                                .clipShape(RoundedRectangle(cornerRadius: 18))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 18)
                                        .stroke(AppColors.accent(for: colorScheme).opacity(0.12), lineWidth: 1)
                                )
                        }

                        Button {
                            showingFileImporter = true
                        } label: {
                            Label("Audio-Dateien oder ZIP waehlen", systemImage: "waveform.badge.plus")
                                .font(.headline)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 14)
                        }
                        .buttonStyle(.bordered)
                        .tint(AppColors.accentMystic(for: colorScheme))

                        if !viewModel.selectedFiles.isEmpty {
                            VStack(spacing: 10) {
                                ForEach(viewModel.selectedFiles) { file in
                                    NicmaSelectedFileRow(
                                        file: file,
                                        colorScheme: colorScheme
                                    ) {
                                        viewModel.removeFile(file.id)
                                    }
                                }
                            }
                        }

                        if let validationMessage = viewModel.validationMessage {
                            Text(validationMessage)
                                .font(.footnote)
                                .foregroundColor(.red)
                        }

                        Button {
                            Task {
                                await viewModel.uploadSelectedBeats()
                            }
                        } label: {
                            if viewModel.isUploading {
                                ProgressView()
                                    .tint(.white)
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 14)
                            } else {
                                Label("In den Beat Hub hochladen", systemImage: "arrow.up.circle.fill")
                                    .font(.headline)
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 14)
                            }
                        }
                        .background(AppColors.accent(for: colorScheme))
                        .foregroundColor(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 18))
                        .disabled(!viewModel.canUpload)
                        .opacity(viewModel.canUpload ? 1 : 0.6)
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
                } else {
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Listening Only")
                            .font(.headline)
                            .foregroundColor(AppColors.text(for: colorScheme))

                        Text("Der Beat Hub ist fuer Horer gedacht. Neue Uploads und die Pflege der Library bleiben im Admin-Bereich.")
                            .font(.subheadline)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))

                        HStack(spacing: 8) {
                            MusicBadge(text: "Public Beats", isAccent: true)
                            MusicBadge(text: "Preview", isAccent: false)
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
                            .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.16), lineWidth: 1)
                    )
                }

                VStack(alignment: .leading, spacing: 14) {
                    Text("Beats")
                        .font(.headline)
                        .foregroundColor(AppColors.text(for: colorScheme))

                    if viewModel.isLoadingBeats {
                        ProgressView("Beat Hub wird geladen ...")
                    } else if viewModel.beats.isEmpty {
                        Text(viewModel.isAdmin
                             ? "Noch keine Beats im Hub. Neue Uploads tauchen hier sofort auf."
                             : "Noch keine freigegebenen Beats. Sobald ein Beat live geschaltet wird, kannst du ihn hier abspielen.")
                            .font(.subheadline)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    } else {
                        ForEach(viewModel.beats) { beat in
                            BeatHubLibraryRow(
                                beat: beat,
                                isAdmin: viewModel.isAdmin,
                                isPlaying: playbackManager.currentBeatID == beat.id,
                                colorScheme: colorScheme,
                                onPlayToggle: { playbackManager.togglePlayback(for: beat) },
                                onVisibilityToggle: {
                                    Task {
                                        await viewModel.toggleBeatVisibility(beat)
                                    }
                                }
                            )
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
        .navigationTitle("Beat Hub")
        .task {
            viewModel.configure(currentUser: authManager.userSession)
        }
        .onReceive(authManager.$userSession) { user in
            viewModel.configure(currentUser: user)
        }
        .onDisappear {
            playbackManager.stop()
        }
        .fileImporter(
            isPresented: $showingFileImporter,
            allowedContentTypes: [UTType.audio, UTType.zip],
            allowsMultipleSelection: true
        ) { result in
            viewModel.handleFileImport(result)
        }
        .fancyToast(
            isPresented: $viewModel.showToast,
            message: viewModel.toastMessage,
            style: viewModel.toastStyle
        )
    }
}

private struct VideoHubLibraryRow: View {
    let video: SkydownVideoHubItem
    let isAdmin: Bool
    let isSelected: Bool
    let isPlaying: Bool
    let colorScheme: ColorScheme
    let onSelect: () -> Void
    let onPlayToggle: () -> Void
    let onToggleHomeFeatured: () -> Void
    let onDelete: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top, spacing: 12) {
                ZStack {
                    Circle()
                        .fill(AppColors.accentMystic(for: colorScheme).opacity(0.16))
                        .frame(width: 44, height: 44)

                    Image(systemName: isSelected ? "play.rectangle.fill" : "video.fill")
                        .foregroundColor(AppColors.accentMystic(for: colorScheme))
                }

                VStack(alignment: .leading, spacing: 4) {
                    Text(video.title)
                        .font(.headline)
                        .foregroundColor(AppColors.text(for: colorScheme))

                    Text("\(video.projectName) • \(skydownVideoDateFormatter.string(from: video.createdAt))")
                        .font(.subheadline)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))

                    if !video.notes.isEmpty {
                        Text(video.notes)
                            .font(.footnote)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    }
                }

                Spacer()
            }

            HStack(spacing: 8) {
                MusicBadge(text: video.isPublic ? "Live" : "Hidden", isAccent: video.isPublic)
                if video.isHomeFeatured {
                    MusicBadge(text: "Home", isAccent: true)
                }
                MusicBadge(text: video.fileName, isAccent: false)
            }

            HStack(spacing: 10) {
                Button(action: onSelect) {
                    Label(isSelected ? "Im Player" : "Auswaehlen", systemImage: "rectangle.on.rectangle")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)

                Button(action: onPlayToggle) {
                    Label(isPlaying ? "Stoppen" : "Abspielen", systemImage: isPlaying ? "stop.fill" : "play.fill")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .tint(AppColors.accent(for: colorScheme))
                .disabled(!video.isPlayable)
            }

            if isAdmin {
                HStack(spacing: 10) {
                    Button(action: onToggleHomeFeatured) {
                        Label(video.isHomeFeatured ? "Home aktiv" : "Im Home zeigen", systemImage: "house.fill")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)

                    Button(role: .destructive, action: onDelete) {
                        Label("Loeschen", systemImage: "trash")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)
                }
            }
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: 18))
    }
}

private struct VideoEquipmentCard: View {
    let colorScheme: ColorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Equipment & Software")
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            Text("Damit direkt klar ist, womit Skydown x 22 die Videography umsetzt.")
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            VStack(spacing: 10) {
                VideoEquipmentRow(
                    title: "Drohnen",
                    detail: "DJI Neo und DJI Avata 2 fuer bewegte Luftshots und FPV-Looks.",
                    colorScheme: colorScheme
                )
                VideoEquipmentRow(
                    title: "Kamera",
                    detail: "Sony FX30 mit Sigma 18-50 mm f/2.8 plus Gimbals fuer saubere Motion-Shots.",
                    colorScheme: colorScheme
                )
                VideoEquipmentRow(
                    title: "Mobile Capture",
                    detail: "iPhone 16 Pro mit Apple Log fuer flexible schnelle Shoots.",
                    colorScheme: colorScheme
                )
                VideoEquipmentRow(
                    title: "Postproduktion",
                    detail: "Adobe Premiere Pro, DaVinci Resolve Studio und Adobe After Effects.",
                    colorScheme: colorScheme
                )
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
}

private struct VideoEquipmentRow: View {
    let title: String
    let detail: String
    let colorScheme: ColorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title)
                .font(.subheadline.weight(.bold))
                .foregroundColor(AppColors.text(for: colorScheme))

            Text(detail)
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: 18))
    }
}

private struct SkydownSelectedVideoRow: View {
    let file: SkydownSelectedVideoFile
    let colorScheme: ColorScheme
    let onRemove: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            ZStack {
                Circle()
                    .fill(AppColors.accentMystic(for: colorScheme).opacity(0.16))
                    .frame(width: 42, height: 42)

                Image(systemName: "video.fill")
                    .foregroundColor(AppColors.accentMystic(for: colorScheme))
            }

            VStack(alignment: .leading, spacing: 4) {
                Text(file.fileName)
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(AppColors.text(for: colorScheme))
                    .lineLimit(1)

                Text(ByteCountFormatter.string(fromByteCount: file.fileSizeInBytes, countStyle: .file))
                    .font(.caption)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }

            Spacer()

            Button(role: .destructive, action: onRemove) {
                Image(systemName: "xmark.circle.fill")
                    .font(.title3)
            }
        }
        .padding(12)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: 18))
    }
}

private struct BeatHubLibraryRow: View {
    let beat: NicmaBeatHubItem
    let isAdmin: Bool
    let isPlaying: Bool
    let colorScheme: ColorScheme
    let onPlayToggle: () -> Void
    let onVisibilityToggle: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top, spacing: 12) {
                ZStack {
                    Circle()
                        .fill(AppColors.accent(for: colorScheme).opacity(0.14))
                        .frame(width: 44, height: 44)

                    Image(systemName: beat.isPlayable ? "waveform.circle.fill" : "doc.zipper")
                        .foregroundColor(AppColors.accent(for: colorScheme))
                }

                VStack(alignment: .leading, spacing: 4) {
                    Text(beat.title)
                        .font(.headline)
                        .foregroundColor(AppColors.text(for: colorScheme))

                    Text("\(beat.artistName) • \(beat.uploaderName)")
                        .font(.subheadline)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))

                    if !beat.notes.isEmpty {
                        Text(beat.notes)
                            .font(.footnote)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    }
                }

                Spacer()
            }

            HStack(spacing: 8) {
                MusicBadge(text: beat.isPublic ? "Live" : "Review", isAccent: beat.isPublic)
                MusicBadge(text: beat.fileName, isAccent: false)
            }

            HStack(spacing: 10) {
                if beat.isPlayable {
                    Button(action: onPlayToggle) {
                        Label(isPlaying ? "Stoppen" : "Abspielen", systemImage: isPlaying ? "stop.fill" : "play.fill")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(AppColors.accent(for: colorScheme))
                } else {
                    Text("Nicht direkt abspielbar")
                        .font(.footnote)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }

                if isAdmin {
                    Button(beat.isPublic ? "Verbergen" : "Freigeben", action: onVisibilityToggle)
                        .buttonStyle(.bordered)
                }
            }
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: 18))
    }
}

private final class VideoPlaybackManager: ObservableObject {
    @Published var selectedVideoID: String?
    @Published var playingVideoID: String?
    let player = AVPlayer()
    private var playbackObserver: NSObjectProtocol?

    deinit {
        stop()
    }

    func load(video: SkydownVideoHubItem?) {
        clearPlaybackObserver()
        player.pause()

        guard let video,
              let url = URL(string: video.downloadURL) else {
            player.replaceCurrentItem(with: nil)
            selectedVideoID = nil
            playingVideoID = nil
            return
        }

        if selectedVideoID != video.id || player.currentItem == nil {
            player.replaceCurrentItem(with: AVPlayerItem(url: url))
        }

        selectedVideoID = video.id
        playingVideoID = nil
        player.seek(to: .zero)
    }

    func togglePlayback(for video: SkydownVideoHubItem) {
        guard video.isPlayable else { return }

        if selectedVideoID != video.id || player.currentItem == nil {
            load(video: video)
            observePlaybackFinished()
            player.play()
            playingVideoID = video.id
            return
        }

        if playingVideoID == video.id {
            player.pause()
            playingVideoID = nil
        } else {
            observePlaybackFinished()
            player.play()
            playingVideoID = video.id
        }
    }

    func stop() {
        player.pause()
        player.replaceCurrentItem(with: nil)
        clearPlaybackObserver()
        selectedVideoID = nil
        playingVideoID = nil
    }

    private func observePlaybackFinished() {
        clearPlaybackObserver()
        guard let currentItem = player.currentItem else { return }
        playbackObserver = NotificationCenter.default.addObserver(
            forName: .AVPlayerItemDidPlayToEndTime,
            object: currentItem,
            queue: .main
        ) { [weak self] _ in
            self?.player.seek(to: .zero)
            self?.player.pause()
            self?.playingVideoID = nil
        }
    }

    private func clearPlaybackObserver() {
        if let playbackObserver {
            NotificationCenter.default.removeObserver(playbackObserver)
            self.playbackObserver = nil
        }
    }
}

private struct NicmaProducerPriceCard: View {
    let package: NicmaProducerPackage
    let colorScheme: ColorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(alignment: .top) {
                Text(package.title)
                    .font(.headline)
                    .foregroundColor(AppColors.text(for: colorScheme))

                Spacer()

                Text(package.price)
                    .font(.headline.weight(.bold))
                    .foregroundColor(AppColors.accent(for: colorScheme))
            }

            Text(package.detail)
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: 18))
    }
}

private struct NicmaUploadField: View {
    let title: String
    @Binding var text: String
    let colorScheme: ColorScheme
    var keyboard: UIKeyboardType = .default
    var autocapitalization: TextInputAutocapitalization = .sentences

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.subheadline.weight(.semibold))
                .foregroundColor(AppColors.text(for: colorScheme))

            TextField(title, text: $text)
                .keyboardType(keyboard)
                .textInputAutocapitalization(autocapitalization)
                .padding(.horizontal, 14)
                .padding(.vertical, 14)
                .background(AppColors.secondaryBackground(for: colorScheme))
                .clipShape(RoundedRectangle(cornerRadius: 18))
                .overlay(
                    RoundedRectangle(cornerRadius: 18)
                        .stroke(AppColors.accent(for: colorScheme).opacity(0.12), lineWidth: 1)
                )
        }
    }
}

private struct NicmaSelectedFileRow: View {
    let file: NicmaSelectedFile
    let colorScheme: ColorScheme
    let onRemove: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            ZStack {
                Circle()
                    .fill(AppColors.accentMystic(for: colorScheme).opacity(0.16))
                    .frame(width: 42, height: 42)

                Image(systemName: "waveform")
                    .foregroundColor(AppColors.accentMystic(for: colorScheme))
            }

            VStack(alignment: .leading, spacing: 4) {
                Text(file.fileName)
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(AppColors.text(for: colorScheme))
                    .lineLimit(1)

                Text(ByteCountFormatter.string(fromByteCount: file.fileSizeInBytes, countStyle: .file))
                    .font(.caption)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }

            Spacer()

            Button(role: .destructive, action: onRemove) {
                Image(systemName: "xmark.circle.fill")
                    .font(.title3)
            }
        }
        .padding(12)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: 18))
    }
}

private final class BeatPlaybackManager: ObservableObject {
    @Published var currentBeatID: String?
    private var player: AVPlayer?
    private var playbackObserver: NSObjectProtocol?

    func togglePlayback(for beat: NicmaBeatHubItem) {
        guard beat.isPlayable,
              let url = URL(string: beat.downloadURL) else { return }

        if currentBeatID == beat.id {
            stop()
            return
        }

        stop()
        configureAudioSession()
        player = AVPlayer(url: url)
        observePlaybackFinished()
        player?.play()
        currentBeatID = beat.id
    }

    func stop() {
        player?.pause()
        if let playbackObserver {
            NotificationCenter.default.removeObserver(playbackObserver)
            self.playbackObserver = nil
        }
        player = nil
        currentBeatID = nil
    }

    private func configureAudioSession() {
        do {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.playback, mode: .default, options: [.mixWithOthers])
            try session.setActive(true)
        } catch {
            print("Dev Fehler Beat AudioSession:", error.localizedDescription)
        }
    }

    private func observePlaybackFinished() {
        guard let currentItem = player?.currentItem else { return }
        playbackObserver = NotificationCenter.default.addObserver(
            forName: .AVPlayerItemDidPlayToEndTime,
            object: currentItem,
            queue: .main
        ) { [weak self] _ in
            self?.stop()
        }
    }
}

private struct NicmaProducerPackage: Identifiable {
    let id = UUID()
    let title: String
    let detail: String
    let price: String
}

private struct MusicInstagramDestination: Identifiable {
    let id: String
    let title: String
    let handle: String
    let urlString: String
    let helper: String?

    var url: URL? {
        URL(string: urlString)
    }

    func subtitle(selectedArtist: String) -> String {
        if let helper {
            return "\(handle) • \(helper)"
        }

        if title == selectedArtist {
            return "\(handle) • Artist aktuell auswaehlt"
        }

        return handle
    }
}

private let artistInstagramDestinations: [String: MusicInstagramDestination] = [
    "Yang D. Nash": MusicInstagramDestination(
        id: "artist_yang_d_nash",
        title: "Yang D. Nash",
        handle: "@y.d.nash",
        urlString: "https://www.instagram.com/y.d.nash/",
        helper: "Artist aktuell ausgewaehlt"
    ),
    "MAVE": MusicInstagramDestination(
        id: "artist_mave",
        title: "MAVE",
        handle: "@mave__official",
        urlString: "https://www.instagram.com/mave__official/",
        helper: "Artist aktuell ausgewaehlt"
    ),
    "ThaDude": MusicInstagramDestination(
        id: "artist_thadude",
        title: "ThaDude",
        handle: "@thadude_offizielle",
        urlString: "https://www.instagram.com/thadude_offizielle/",
        helper: "Artist aktuell ausgewaehlt"
    ),
    "Toprack941": MusicInstagramDestination(
        id: "artist_toprack941",
        title: "Toprack941",
        handle: "@toprack_941",
        urlString: "https://www.instagram.com/toprack_941/",
        helper: "Artist aktuell ausgewaehlt"
    ),
    "TANGAJOE007": MusicInstagramDestination(
        id: "artist_tangajoe007",
        title: "TANGAJOE007",
        handle: "@tangajoe007",
        urlString: "https://www.instagram.com/tangajoe007/",
        helper: "Artist aktuell ausgewaehlt"
    ),
    "JANNO": MusicInstagramDestination(
        id: "artist_janno",
        title: "JANNO",
        handle: "@janno_official_",
        urlString: "https://www.instagram.com/janno_official_/",
        helper: "Artist aktuell ausgewaehlt"
    ),
]

private let zweizweiInstagramDestination = MusicInstagramDestination(
    id: "brand_22_music",
    title: "22 Music",
    handle: "@zweizwei_music",
    urlString: "https://www.instagram.com/zweizwei_music/",
    helper: "Skydown x 22 Universe"
)

private let skydownMusicInstagramDestination = MusicInstagramDestination(
    id: "brand_skydown",
    title: "Skydown Entertainment",
    handle: "@skydown_entertainment",
    urlString: "https://www.instagram.com/skydown_entertainment/",
    helper: "Label und Releases"
)

private let nicmaInstagramDestination = MusicInstagramDestination(
    id: "brand_nicma_music",
    title: "NICMA MUSIC",
    handle: "@nicma.music",
    urlString: "https://www.instagram.com/nicma.music/",
    helper: "Producer und Studio"
)

private let nicmaProducerPackages: [NicmaProducerPackage] = [
    NicmaProducerPackage(
        title: "Mixing",
        detail: "max. 24 Audio Files",
        price: "150 €"
    ),
    NicmaProducerPackage(
        title: "Mastering",
        detail: "2 stems",
        price: "70 €"
    ),
    NicmaProducerPackage(
        title: "Mastering",
        detail: "max. 5 stems",
        price: "90 €"
    ),
    NicmaProducerPackage(
        title: "Mixing + Mastering",
        detail: "max. 24 Audio Files",
        price: "200 €"
    ),
    NicmaProducerPackage(
        title: "Track Recording ohne Mix / Master",
        detail: "Recording Session",
        price: "120 €"
    ),
    NicmaProducerPackage(
        title: "Track Recording inkl. Mix / Master",
        detail: "Kompletter Recording-Flow",
        price: "250 €"
    ),
    NicmaProducerPackage(
        title: "8h Studio Zeit + Engineer",
        detail: "zzgl. Nachbearbeitung",
        price: "400 € + Nachbearbeitung"
    )
]

private let supportedVideoContentTypes: [UTType] = [
    .movie,
    .mpeg4Movie,
    .quickTimeMovie,
    UTType(filenameExtension: "m4v") ?? .movie
]

private let skydownVideoDateFormatter: DateFormatter = {
    let formatter = DateFormatter()
    formatter.locale = Locale(identifier: "de_DE")
    formatter.dateFormat = "dd.MM.yyyy"
    return formatter
}()

#Preview {
    MusicView()
}
