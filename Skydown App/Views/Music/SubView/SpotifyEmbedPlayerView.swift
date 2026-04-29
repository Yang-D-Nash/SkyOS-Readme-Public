import SwiftUI
import WebKit

struct SpotifyEmbedPlayerView: View {
    let track: Track

    @Environment(\.dismiss) private var dismiss
    @Environment(\.openURL) private var openURL
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        NavigationStack {
            Group {
                if let embedURL = spotifyEmbedURL(track: track) {
                    SpotifyEmbedWebView(url: embedURL)
                        .background(Color.black)
                } else {
                    ContentUnavailableView(
                        "Spotify Player nicht verfuegbar",
                        systemImage: "music.note"
                    )
                }
            }
            .navigationTitle(track.trackName)
            .navigationBarTitleDisplayMode(.inline)
            .skydownNavigationChrome(colorScheme: colorScheme)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    SkydownBrandActionButton(
                        title: AppLocalized.text("common.done", fallback: "Fertig"),
                        accent: AppColors.accent(for: colorScheme),
                        colorScheme: colorScheme,
                        role: .muted,
                        font: .subheadline.weight(.semibold),
                        cornerRadius: SkydownLayout.denseRadius,
                        verticalPadding: 8,
                        expandToFullWidth: false,
                        action: { dismiss() }
                    )
                    .skydownInteractiveFeedback()
                }

                if let appURL = spotifyAppURL(track: track) {
                    ToolbarItem(placement: .topBarTrailing) {
                        SkydownBrandActionButton(
                            title: AppLocalized.text("common.open_link", fallback: "Open"),
                            systemImage: "arrow.up.forward.circle",
                            accent: AppColors.spotify(for: colorScheme),
                            colorScheme: colorScheme,
                            role: .muted,
                            font: .caption.weight(.semibold),
                            cornerRadius: SkydownLayout.denseRadius,
                            verticalPadding: 8,
                            expandToFullWidth: false,
                            action: {
                                openURL(appURL) { accepted in
                                    if !accepted, let webURL = spotifyWebURL(track: track) {
                                        openURL(webURL)
                                    }
                                }
                            }
                        )
                        .skydownInteractiveFeedback()
                    }
                } else if let webURL = spotifyWebURL(track: track) {
                    ToolbarItem(placement: .topBarTrailing) {
                        SkydownBrandActionButton(
                            title: AppLocalized.text("common.open_link", fallback: "Open"),
                            systemImage: "arrow.up.forward.circle",
                            accent: AppColors.spotify(for: colorScheme),
                            colorScheme: colorScheme,
                            role: .muted,
                            font: .caption.weight(.semibold),
                            cornerRadius: SkydownLayout.denseRadius,
                            verticalPadding: 8,
                            expandToFullWidth: false,
                            action: { openURL(webURL) }
                        )
                        .skydownInteractiveFeedback()
                    }
                }
            }
        }
    }
}

private struct SpotifyEmbedWebView: UIViewRepresentable {
    let url: URL

    func makeCoordinator() -> Coordinator {
        Coordinator()
    }

    func makeUIView(context: Context) -> WKWebView {
        let configuration = WKWebViewConfiguration()
        configuration.allowsInlineMediaPlayback = true
        configuration.mediaTypesRequiringUserActionForPlayback = []
        configuration.preferences.javaScriptCanOpenWindowsAutomatically = false

        let webView = WKWebView(frame: .zero, configuration: configuration)
        webView.navigationDelegate = context.coordinator
        webView.scrollView.contentInsetAdjustmentBehavior = .never
        webView.isOpaque = false
        webView.backgroundColor = .black
        webView.scrollView.backgroundColor = .black
        return webView
    }

    func updateUIView(_ webView: WKWebView, context: Context) {
        guard spotifyAllowsEmbeddedWebNavigation(url) else { return }
        if webView.url != url {
            webView.load(URLRequest(url: url))
        }
    }

    final class Coordinator: NSObject, WKNavigationDelegate {
        func webView(
            _ webView: WKWebView,
            decidePolicyFor navigationAction: WKNavigationAction,
            decisionHandler: @escaping (WKNavigationActionPolicy) -> Void
        ) {
            decisionHandler(spotifyAllowsEmbeddedWebNavigation(navigationAction.request.url) ? .allow : .cancel)
        }
    }
}

private func spotifyAllowsEmbeddedWebNavigation(_ url: URL?) -> Bool {
    guard let scheme = url?.scheme?.lowercased() else { return false }
    return scheme == "https" || scheme == "about"
}

private func spotifyWebURL(track: Track) -> URL? {
    if let trackID = resolvedSpotifyTrackID(track: track) {
        return URL(string: "https://open.spotify.com/track/\(trackID)")
    }
    if let artistID = resolvedSpotifyArtistID(track: track) {
        return URL(string: "https://open.spotify.com/artist/\(artistID)")
    }
    if let externalURL = track.externalURL, !externalURL.isEmpty {
        return URL(string: externalURL)
    }
    return nil
}

private func spotifyAppURL(track: Track) -> URL? {
    if let trackID = resolvedSpotifyTrackID(track: track) {
        return URL(string: "spotify:track:\(trackID)")
    }
    guard let artistID = resolvedSpotifyArtistID(track: track) else { return nil }
    return URL(string: "spotify:artist:\(artistID)")
}

private func spotifyEmbedURL(track: Track) -> URL? {
    guard let trackID = resolvedSpotifyTrackID(track: track) else { return nil }
    return URL(string: "https://open.spotify.com/embed/track/\(trackID)?utm_source=generator")
}

private func resolvedSpotifyTrackID(track: Track) -> String? {
    if let spotifyTrackID = track.spotifyTrackID, !spotifyTrackID.isEmpty {
        return spotifyTrackID
    }

    guard let externalURL = track.externalURL,
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

private func resolvedSpotifyArtistID(track: Track) -> String? {
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
