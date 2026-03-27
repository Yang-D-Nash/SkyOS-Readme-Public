import SwiftUI
import WebKit

struct SpotifyEmbedPlayerView: View {
    let track: Track

    @Environment(\.dismiss) private var dismiss
    @Environment(\.openURL) private var openURL

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
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Fertig") {
                        dismiss()
                    }
                }

                if let appURL = spotifyAppURL(track: track) {
                    ToolbarItem(placement: .topBarTrailing) {
                        Button {
                            openURL(appURL) { accepted in
                                if !accepted, let webURL = spotifyWebURL(track: track) {
                                    openURL(webURL)
                                }
                            }
                        } label: {
                            Image(systemName: "arrow.up.forward.circle")
                        }
                    }
                } else if let webURL = spotifyWebURL(track: track) {
                    ToolbarItem(placement: .topBarTrailing) {
                        Button {
                            openURL(webURL)
                        } label: {
                            Image(systemName: "arrow.up.forward.circle")
                        }
                    }
                }
            }
        }
    }
}

private struct SpotifyEmbedWebView: UIViewRepresentable {
    let url: URL

    func makeUIView(context: Context) -> WKWebView {
        let configuration = WKWebViewConfiguration()
        configuration.allowsInlineMediaPlayback = true
        configuration.mediaTypesRequiringUserActionForPlayback = []

        let webView = WKWebView(frame: .zero, configuration: configuration)
        webView.scrollView.contentInsetAdjustmentBehavior = .never
        webView.isOpaque = false
        webView.backgroundColor = .black
        webView.scrollView.backgroundColor = .black
        return webView
    }

    func updateUIView(_ webView: WKWebView, context: Context) {
        if webView.url != url {
            webView.load(URLRequest(url: url))
        }
    }
}

private func spotifyWebURL(track: Track) -> URL? {
    if let externalURL = track.externalURL, !externalURL.isEmpty {
        return URL(string: externalURL)
    }
    guard let trackID = resolvedSpotifyTrackID(track: track) else { return nil }
    return URL(string: "https://open.spotify.com/track/\(trackID)")
}

private func spotifyAppURL(track: Track) -> URL? {
    guard let trackID = resolvedSpotifyTrackID(track: track) else { return nil }
    return URL(string: "spotify:track:\(trackID)")
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
