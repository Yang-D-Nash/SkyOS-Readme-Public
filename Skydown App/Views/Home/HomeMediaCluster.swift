import SwiftUI
import AVKit

struct HomeMediaCluster: View {
    let colorScheme: ColorScheme
    @ObservedObject var viewModel: HomeViewModel
    @ObservedObject var playbackManager: AudioPlayerManager
    @ObservedObject var videoPlaybackManager: HomeInlineVideoPlaybackManager
    let onOpenVideoHub: (FeaturedHomeVideo) -> Void
    let onOpenOriginal: (FeaturedHomeVideo) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HomeLatestReleaseCard(viewModel: viewModel, playbackManager: playbackManager, colorScheme: colorScheme) { track in
                videoPlaybackManager.stop()
                playbackManager.playPreview(for: track)
            }
            .id("release")

            HomeLatestVideoCard(viewModel: viewModel, playbackManager: videoPlaybackManager, colorScheme: colorScheme) { video in
                playbackManager.stop()
                videoPlaybackManager.togglePlayback(for: video)
            } onOpenVideoHub: { video in
                playbackManager.stop()
                videoPlaybackManager.stop()
                onOpenVideoHub(video)
            } onOpenOriginal: { video in
                playbackManager.stop()
                videoPlaybackManager.stop()
                onOpenOriginal(video)
            }
            .id("video")
        }
    }
}

private struct HomeLatestReleaseCard: View {
    @ObservedObject var viewModel: HomeViewModel
    @ObservedObject var playbackManager: AudioPlayerManager
    let colorScheme: ColorScheme
    let onPreviewToggle: (Track) -> Void
    @Environment(\.openURL) private var openURL

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HomeSectionBanner(title: "Music Update", subtitle: "Neuester Release aus dem Katalog.", icon: "music.note", colorScheme: colorScheme, accent: AppColors.accent(for: colorScheme))
            if let track = viewModel.featuredTrack {
                let hasPreview = !(track.previewUrl?.isEmpty ?? true)
                let hasSpotifyTarget = homeSpotifyTargetURL(for: track) != nil
                HStack(spacing: 14) {
                    AsyncImage(url: URL(string: track.artworkUrl100 ?? "")) { image in image.resizable().scaledToFill() } placeholder: {
                        RoundedRectangle(cornerRadius: 22).fill(AppColors.secondaryBackground(for: colorScheme))
                    }
                    .frame(width: 82, height: 82).clipShape(RoundedRectangle(cornerRadius: 22))
                    VStack(alignment: .leading, spacing: 6) {
                        Text(track.trackName).font(.headline).foregroundColor(AppColors.text(for: colorScheme))
                        Text(track.artistName ?? "22").font(.subheadline).foregroundColor(AppColors.secondaryText(for: colorScheme))
                        Text(homeReleaseLine(for: track)).font(.caption).foregroundColor(AppColors.secondaryText(for: colorScheme))
                    }
                    Spacer()
                }
                Text(hasPreview ? "Vorschau direkt hier in der App." : (hasSpotifyTarget ? "Direkt bei Spotify weiterhoeren." : "Der neueste Track ist hier fuer dich hinterlegt."))
                    .font(.caption).foregroundColor(AppColors.secondaryText(for: colorScheme))
                VStack(spacing: 10) {
                    if hasPreview {
                        HomeActionButton(
                            title: playbackManager.currentlyPlayingId == track.trackId ? "Stop" : "Play",
                            icon: playbackManager.currentlyPlayingId == track.trackId ? "stop.fill" : "play.fill",
                            colorScheme: colorScheme,
                            isPrimary: playbackManager.currentlyPlayingId == track.trackId
                        ) { onPreviewToggle(track) }
                    }
                    if let spotifyURL = homeSpotifyTargetURL(for: track) {
                        HomeActionButton(title: homeSpotifyActionTitle(for: track), icon: "music.note", colorScheme: colorScheme, brand: .spotify, isPrimary: false) { openURL(spotifyURL) }
                    }
                }
            } else {
                Text(viewModel.homeTrackMessage ?? "Neuer Song erscheint hier.").font(.body).foregroundColor(AppColors.secondaryText(for: colorScheme))
            }
        }
        .padding(SkydownLayout.cardPadding)
        .skydownPanelSurface(colorScheme: colorScheme, accent: AppColors.accent(for: colorScheme), cornerRadius: SkydownLayout.cardCornerRadius, shadowRadius: 9, shadowYOffset: 4)
    }
}

private struct HomeLatestVideoCard: View {
    @ObservedObject var viewModel: HomeViewModel
    @ObservedObject var playbackManager: HomeInlineVideoPlaybackManager
    let colorScheme: ColorScheme
    let onPlayToggle: (FeaturedHomeVideo) -> Void
    let onOpenVideoHub: (FeaturedHomeVideo) -> Void
    let onOpenOriginal: (FeaturedHomeVideo) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HomeSectionBanner(title: "Visual Update", subtitle: "Naechster Clip direkt im Fokus.", icon: "video.fill", colorScheme: colorScheme, accent: AppColors.accentHighlight(for: colorScheme))
            if let video = viewModel.featuredVideo {
                Text(video.title).font(.headline).foregroundColor(AppColors.text(for: colorScheme))
                if video.usesEmbeddedPreview {
                    ExternalVideoEmbedSurface(urlString: video.embedURL).frame(height: 220).clipShape(RoundedRectangle(cornerRadius: 20))
                } else if !video.downloadURL.isEmpty {
                    VideoPlayer(player: playbackManager.player).frame(height: 220).allowsHitTesting(false).clipShape(RoundedRectangle(cornerRadius: 20))
                        .onAppear { playbackManager.prepare(video: video) }
                        .onChange(of: video.id) { _, _ in playbackManager.prepare(video: video) }
                }
                if !video.downloadURL.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    HomeActionButton(
                        title: playbackManager.isPlaying && playbackManager.currentVideoID == video.id ? "Stoppen" : "Abspielen",
                        icon: playbackManager.isPlaying && playbackManager.currentVideoID == video.id ? "stop.fill" : "play.rectangle.fill",
                        colorScheme: colorScheme,
                        isPrimary: playbackManager.isPlaying && playbackManager.currentVideoID == video.id
                    ) { onPlayToggle(video) }
                } else if !video.openURLString.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || !video.embedURL.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    HomeActionButton(title: video.supportsInlinePlayback ? "Video direkt oeffnen" : video.provider.originalVideoActionTitle, icon: video.supportsInlinePlayback ? "play.rectangle.fill" : "arrow.up.forward.square", colorScheme: colorScheme, isPrimary: true) { onOpenOriginal(video) }
                } else if video.supportsInlinePlayback {
                    HomeActionButton(title: "Im Video ansehen", icon: "rectangle.portrait.and.arrow.right", colorScheme: colorScheme, isPrimary: true) { onOpenVideoHub(video) }
                }
            } else {
                Text(viewModel.homeVideoMessage ?? "Neues Video erscheint hier.").font(.body).foregroundColor(AppColors.secondaryText(for: colorScheme))
            }
        }
        .padding(SkydownLayout.cardPadding)
        .skydownPanelSurface(colorScheme: colorScheme, accent: AppColors.accent(for: colorScheme), cornerRadius: SkydownLayout.cardCornerRadius, shadowRadius: 9, shadowYOffset: 4)
    }
}

private func homeReleaseLine(for track: Track) -> String {
    let collection = track.collectionName ?? "Spotify Release"
    if let releaseDate = track.releaseDate, !releaseDate.isEmpty { return "\(collection) • \(releaseDate)" }
    return collection
}

private func homeSpotifyTargetURL(for track: Track) -> URL? {
    if let spotifyTrackID = track.spotifyTrackID, !spotifyTrackID.isEmpty { return URL(string: "https://open.spotify.com/track/\(spotifyTrackID)") }
    if let spotifyArtistID = track.spotifyArtistID, !spotifyArtistID.isEmpty { return URL(string: "https://open.spotify.com/artist/\(spotifyArtistID)") }
    if let externalURL = track.externalURL, let url = URL(string: externalURL) { return url }
    return nil
}

private func homeSpotifyActionTitle(for track: Track) -> String {
    if let spotifyTrackID = track.spotifyTrackID, !spotifyTrackID.isEmpty { return "Song auf Spotify" }
    if let spotifyArtistID = track.spotifyArtistID, !spotifyArtistID.isEmpty { return "Artist auf Spotify" }
    if let externalURL = track.externalURL, externalURL.contains("/artist/") { return "Artist auf Spotify" }
    return "Auf Spotify ansehen"
}

enum HomeActionBrand { case neutral, spotify, instagram, youtube }

struct HomeActionButton: View {
    let title: String
    let subtitle: String?
    let icon: String?
    let colorScheme: ColorScheme
    let brand: HomeActionBrand
    let isPrimary: Bool
    let action: () -> Void

    init(title: String, subtitle: String? = nil, icon: String? = nil, colorScheme: ColorScheme, brand: HomeActionBrand = .neutral, isPrimary: Bool, action: @escaping () -> Void) {
        self.title = title
        self.subtitle = subtitle
        self.icon = icon
        self.colorScheme = colorScheme
        self.brand = brand
        self.isPrimary = isPrimary
        self.action = action
    }

    var body: some View {
        Button(action: action) {
            HStack(alignment: .top, spacing: 10) {
                if let icon {
                    Image(systemName: icon).font(.footnote.weight(.bold)).foregroundColor(isPrimary ? .white : AppColors.accent(for: colorScheme)).frame(width: 18, height: 18).padding(8).background(RoundedRectangle(cornerRadius: 12, style: .continuous).fill(Color.white.opacity(isPrimary ? 0.16 : 0.08)))
                }
                VStack(alignment: .leading, spacing: 4) {
                    Text(title).font(AppTypography.buttonLabel)
                    if let subtitle, !subtitle.isEmpty {
                        Text(subtitle).font(AppTypography.bodyCaption).foregroundColor(isPrimary ? Color.white.opacity(0.82) : AppColors.secondaryText(for: colorScheme))
                    }
                }
                Spacer()
            }
            .foregroundColor(isPrimary ? .white : AppColors.text(for: colorScheme))
            .padding(.horizontal, 13).padding(.vertical, 12).frame(maxWidth: .infinity)
            .background(RoundedRectangle(cornerRadius: 16).fill(isPrimary ? AppColors.accent(for: colorScheme) : AppColors.cardBackground(for: colorScheme)))
        }
        .buttonStyle(.plain)
        .skydownTactileAction()
    }
}

final class HomeInlineVideoPlaybackManager: ObservableObject {
    @Published var currentVideoID: String?
    @Published var isPlaying = false
    let player = AVPlayer()
    private var playbackObserver: NSObjectProtocol?

    deinit {
        clearPlaybackObserver()
        player.pause()
        player.replaceCurrentItem(with: nil)
    }

    func prepare(video: FeaturedHomeVideo?) {
        guard let video, let url = URL(string: video.downloadURL), !video.downloadURL.isEmpty else {
            stop()
            player.replaceCurrentItem(with: nil)
            currentVideoID = nil
            return
        }
        guard currentVideoID != video.id || player.currentItem == nil else { return }
        clearPlaybackObserver()
        player.pause()
        player.replaceCurrentItem(with: AVPlayerItem(url: url))
        currentVideoID = video.id
        isPlaying = false
        observePlaybackFinished()
    }

    func togglePlayback(for video: FeaturedHomeVideo) {
        prepare(video: video)
        guard currentVideoID == video.id else { return }
        if isPlaying {
            stop()
        } else {
            player.play()
            isPlaying = true
        }
    }

    func stop() {
        player.pause()
        player.seek(to: .zero)
        isPlaying = false
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
            self?.isPlaying = false
        }
    }

    private func clearPlaybackObserver() {
        if let playbackObserver {
            NotificationCenter.default.removeObserver(playbackObserver)
            self.playbackObserver = nil
        }
    }
}
