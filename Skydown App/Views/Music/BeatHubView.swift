//
//  BeatHubView.swift
//  Skydown App
//
//  Created by Codex on 27.03.26.
//

import AVFoundation
import SwiftUI
import UniformTypeIdentifiers

struct BeatHubView: View {
    @EnvironmentObject private var authManager: AuthManager
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.openURL) private var openURL
    @StateObject private var viewModel = NicmaProducerViewModel()
    @StateObject private var playbackManager = BeatPlaybackManager()
    @State private var showingFileImporter = false
    @State private var showingUploadSheet = false
    let onBack: (() -> Void)?

    init(onBack: (() -> Void)? = nil) {
        self.onBack = onBack
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                heroCard
                beatsCard
            }
            .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
            .padding(.top, SkydownLayout.screenTopPadding)
            .padding(.bottom, SkydownLayout.screenBottomPadding)
        }
        .scrollIndicators(.hidden)
        .scrollDismissesKeyboard(.interactively)
        .skydownDismissKeyboardOnTap()
        .background(AppColors.primaryBackground(for: colorScheme).ignoresSafeArea())
        .navigationTitle("Beat Hub")
        .navigationBarTitleDisplayMode(.inline)
        .skydownNavigationChrome(colorScheme: colorScheme)
        .toolbar {
            if let onBack {
                ToolbarItem(placement: .topBarLeading) {
                    Button(action: onBack) {
                        Image(systemName: "chevron.left")
                            .font(.headline.weight(.bold))
                    }
                }
            }

            ToolbarItemGroup(placement: .topBarTrailing) {
                if viewModel.isUploading {
                    ProgressView()
                        .controlSize(.small)
                }

                if viewModel.isAdmin {
                    Button {
                        showingUploadSheet = true
                    } label: {
                        Image(systemName: "arrow.up.circle")
                            .font(.headline.weight(.semibold))
                    }
                }
            }
        }
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
        .sheet(isPresented: $showingUploadSheet) {
            NavigationStack {
                ScrollView {
                    uploadCard
                        .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
                        .padding(.top, SkydownLayout.screenTopPadding)
                        .padding(.bottom, SkydownLayout.screenBottomPadding)
                }
                .scrollDismissesKeyboard(.interactively)
                .skydownDismissKeyboardOnTap()
                .background(AppColors.primaryBackground(for: colorScheme).ignoresSafeArea())
                .navigationTitle("Beat Upload")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .topBarLeading) {
                        Button("Schliessen") {
                            showingUploadSheet = false
                        }
                    }

                    if viewModel.isUploading {
                        ToolbarItem(placement: .topBarTrailing) {
                            ProgressView()
                                .controlSize(.small)
                        }
                    }
                }
            }
            .presentationDetents([.large])
        }
        .skydownKeyboardDismissToolbar()
    }

    private var heroCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Beat Hub")
                .font(.largeTitle.bold())
                .foregroundColor(AppColors.text(for: colorScheme))

            Text(viewModel.isAdmin ? "Beats, Uploads, Freigaben." : "Freigegebene Beats.")
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
    }

    private var uploadCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Beat Upload")
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            Text("Nur fuer Admins.")
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

            Text("Oder als externer Beat-Link freigeben.")
                .font(.footnote)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            NicmaUploadField(
                title: "Drive / MEGA / anderer Audio-Link",
                text: $viewModel.externalBeatURL,
                colorScheme: colorScheme,
                keyboard: .URL,
                autocapitalization: .never
            )
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

            Button {
                Task {
                    await viewModel.addExternalBeat()
                }
            } label: {
                Label("Externen Beat freigeben", systemImage: "link.badge.plus")
                    .font(.headline)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
            }
            .buttonStyle(.bordered)
            .disabled(!viewModel.canAddExternalBeat)
            .opacity(viewModel.canAddExternalBeat ? 1 : 0.6)
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

    private var beatsCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Beats")
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            if viewModel.isLoadingBeats {
                ProgressView("Beat Hub laedt...")
            } else if viewModel.beats.isEmpty {
                Text(
                    viewModel.isAdmin
                    ? "Noch keine Beats."
                    : "Noch nichts live."
                )
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
                        onOpenOriginal: {
                            if let url = URL(string: beat.openURLString), !beat.openURLString.isEmpty {
                                openURL(url)
                            }
                        },
                        onVisibilityToggle: {
                            Task {
                                await viewModel.toggleBeatVisibility(beat)
                            }
                        },
                        onDelete: {
                            Task {
                                await viewModel.deleteBeat(beat)
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

struct BeatHubLibraryRow: View {
    let beat: NicmaBeatHubItem
    let isAdmin: Bool
    let isPlaying: Bool
    let colorScheme: ColorScheme
    let onPlayToggle: () -> Void
    let onOpenOriginal: () -> Void
    let onVisibilityToggle: () -> Void
    let onDelete: () -> Void

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

                    Text(isAdmin ? "\(beat.artistName) • \(beat.uploaderName)" : beat.artistName)
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
                MusicBadge(text: beat.provider.badgeLabel, isAccent: false)
                if isAdmin {
                    MusicBadge(text: beat.fileName, isAccent: false)
                } else if beat.isPlayable {
                    MusicBadge(text: "Preview", isAccent: false)
                }
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
                    Button(action: onOpenOriginal) {
                        Label("Original oeffnen", systemImage: "arrow.up.forward.square")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)
                }

                if isAdmin {
                    HStack(spacing: 10) {
                        Button(beat.isPublic ? "Verbergen" : "Freigeben", action: onVisibilityToggle)
                            .buttonStyle(.bordered)

                        Button(role: .destructive, action: onDelete) {
                            Label("Loeschen", systemImage: "trash")
                        }
                        .buttonStyle(.bordered)
                    }
                }
            }
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: 18))
    }
}

final class BeatPlaybackManager: ObservableObject {
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
