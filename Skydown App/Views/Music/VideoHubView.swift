//
//  VideoHubView.swift
//  Skydown App
//
//  Created by Codex on 27.03.26.
//

import AVFoundation
import AVKit
import SwiftUI
import UniformTypeIdentifiers

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
                heroCard
                formatCard
                VideoEquipmentCard(colorScheme: colorScheme)

                if viewModel.isAdmin {
                    uploadCard
                }

                playerCard
                libraryCard
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

    private var heroCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Videography")
                .font(.largeTitle.bold())
                .foregroundColor(AppColors.text(for: colorScheme))

            Text(
                viewModel.isAdmin
                ? "Hier landen Reels, Clips, Sessions und Visuals. Als Admin kannst du Videos hochladen und eins direkt fuer Home auswaehlen."
                : "Hier laufen die oeffentlichen Videos von Skydown x 22. Uploads und Pflege bleiben im Admin-Bereich."
            )
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
    }

    private var formatCard: some View {
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
    }

    private var uploadCard: some View {
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

    private var playerCard: some View {
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
    }

    private var libraryCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Video Library")
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            if viewModel.isLoadingVideos {
                ProgressView("Videography wird geladen ...")
            } else if viewModel.videos.isEmpty {
                Text(
                    viewModel.isAdmin
                    ? "Noch keine Videos im Hub. Neue Uploads tauchen hier sofort auf."
                    : "Noch keine freigegebenen Videos. Sobald ein Clip live ist, kannst du ihn hier abspielen."
                )
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
}

struct VideoHubLibraryRow: View {
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

struct VideoEquipmentCard: View {
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

struct VideoEquipmentRow: View {
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

struct SkydownSelectedVideoRow: View {
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

final class VideoPlaybackManager: ObservableObject {
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

let supportedVideoContentTypes: [UTType] = [
    .movie,
    .mpeg4Movie,
    .quickTimeMovie,
    UTType(filenameExtension: "m4v") ?? .movie
]

let skydownVideoDateFormatter: DateFormatter = {
    let formatter = DateFormatter()
    formatter.locale = Locale(identifier: "de_DE")
    formatter.dateFormat = "dd.MM.yyyy"
    return formatter
}()
