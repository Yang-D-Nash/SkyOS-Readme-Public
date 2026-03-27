import SwiftUI
import WebKit

struct SpotifyEmbedPlayerView: View {
    let track: Track

    @Environment(\.dismiss) private var dismiss
    @Environment(\.openURL) private var openURL

    var body: some View {
        NavigationStack {
            Group {
                if let embedURL = spotifyEmbedURL(externalURL: track.externalURL) {
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

                if let appURL = spotifyAppURL(externalURL: track.externalURL) {
                    ToolbarItem(placement: .topBarTrailing) {
                        Button {
                            openURL(appURL) { accepted in
                                if !accepted, let webURL = spotifyWebURL(externalURL: track.externalURL) {
                                    openURL(webURL)
                                }
                            }
                        } label: {
                            Image(systemName: "arrow.up.forward.circle")
                        }
                    }
                } else if let webURL = spotifyWebURL(externalURL: track.externalURL) {
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

private func spotifyWebURL(externalURL: String?) -> URL? {
    guard let externalURL, !externalURL.isEmpty else { return nil }
    return URL(string: externalURL)
}

private func spotifyAppURL(externalURL: String?) -> URL? {
    guard let trackID = spotifyTrackID(externalURL: externalURL) else { return nil }
    return URL(string: "spotify:track:\(trackID)")
}

private func spotifyEmbedURL(externalURL: String?) -> URL? {
    guard let trackID = spotifyTrackID(externalURL: externalURL) else { return nil }
    return URL(string: "https://open.spotify.com/embed/track/\(trackID)?utm_source=generator")
}

private func spotifyTrackID(externalURL: String?) -> String? {
    guard let webURL = spotifyWebURL(externalURL: externalURL),
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
