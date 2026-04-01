import SwiftUI
import WebKit

struct YouTubeEmbedPlayerView: View {
    let item: SkydownYouTubeVideoItem

    @Environment(\.dismiss) private var dismiss
    @Environment(\.openURL) private var openURL

    var body: some View {
        NavigationStack {
            Group {
                if let playbackURL = youtubePlaybackURL(from: item.urlString) {
                    YouTubeEmbedWebView(url: playbackURL)
                        .background(Color.black)
                } else {
                    ContentUnavailableView(
                        "YouTube Player nicht verfuegbar",
                        systemImage: "play.rectangle"
                    )
                }
            }
            .navigationTitle(item.title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Fertig") {
                        dismiss()
                    }
                }

                if let webURL = URL(string: item.urlString), !item.urlString.isEmpty {
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

private struct YouTubeEmbedWebView: UIViewRepresentable {
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

private func youtubePlaybackURL(from rawURL: String) -> URL? {
    guard let normalizedURL = normalizedYouTubeURL(from: rawURL) else {
        return nil
    }

    if let videoID = resolvedYouTubeVideoID(from: rawURL, normalizedURL: normalizedURL) {
        return URL(string: "https://www.youtube.com/embed/\(videoID)?playsinline=1&rel=0&modestbranding=1")
    }

    return normalizedURL
}

private func normalizedYouTubeURL(from rawURL: String) -> URL? {
    let trimmed = rawURL.trimmingCharacters(in: .whitespacesAndNewlines)
    guard !trimmed.isEmpty else { return nil }

    if let directURL = URL(string: trimmed), directURL.scheme != nil {
        return directURL
    }

    return URL(string: "https://\(trimmed)")
}

private func resolvedYouTubeVideoID(from rawURL: String, normalizedURL: URL) -> String? {
    guard let components = URLComponents(url: normalizedURL, resolvingAgainstBaseURL: false) else {
        return nil
    }

    if let host = components.host?.lowercased(), host.contains("youtu.be") {
        return components.path
            .split(separator: "/")
            .first
            .map(String.init)?
            .takeIfYouTubeID()
    }

    let pathComponents = components.path.split(separator: "/").map(String.init)
    if let markerIndex = pathComponents.firstIndex(where: { ["embed", "shorts", "live", "watch"].contains($0.lowercased()) }),
       markerIndex + 1 < pathComponents.count {
        return pathComponents[markerIndex + 1].takeIfYouTubeID()
    }

    if let queryID = components.queryItems?.first(where: { $0.name == "v" || $0.name == "vi" })?.value?.takeIfYouTubeID() {
        return queryID
    }

    let rawString = rawURL.trimmingCharacters(in: .whitespacesAndNewlines)
    let pattern = #"(?:(?<=v=)|(?<=vi=)|(?<=\/embed\/)|(?<=\/shorts\/)|(?<=youtu\.be\/)|(?<=\/live\/))([A-Za-z0-9_-]{11})"#
    guard let regex = try? NSRegularExpression(pattern: pattern) else {
        return nil
    }

    let range = NSRange(rawString.startIndex..<rawString.endIndex, in: rawString)
    guard let match = regex.firstMatch(in: rawString, range: range),
          let idRange = Range(match.range(at: 1), in: rawString) else {
        return nil
    }

    return String(rawString[idRange]).takeIfYouTubeID()
}

private extension String {
    func takeIfYouTubeID() -> String? {
        let trimmed = trimmingCharacters(in: .whitespacesAndNewlines)
        guard trimmed.count == 11 else { return nil }
        return trimmed
    }
}
