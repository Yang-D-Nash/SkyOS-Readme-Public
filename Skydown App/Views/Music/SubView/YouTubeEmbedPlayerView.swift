import SwiftUI
import WebKit

struct YouTubeEmbedPlayerView: View {
    let item: SkydownYouTubeVideoItem

    @Environment(\.dismiss) private var dismiss
    @Environment(\.openURL) private var openURL

    var body: some View {
        let playerSource = youtubePlayerSource(from: item.urlString)

        NavigationStack {
            Group {
                if let playerSource {
                    YouTubeEmbedWebView(source: playerSource)
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

                if let webURL = playerSource?.externalURL {
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

private struct YouTubePlayerSource {
    let embedURL: URL
    let externalURL: URL
    let embedKey: String
}

private struct YouTubeEmbedWebView: UIViewRepresentable {
    let source: YouTubePlayerSource

    final class Coordinator {
        var lastEmbedKey: String?
    }

    func makeCoordinator() -> Coordinator {
        Coordinator()
    }

    func makeUIView(context: Context) -> WKWebView {
        let configuration = WKWebViewConfiguration()
        configuration.allowsInlineMediaPlayback = true
        configuration.mediaTypesRequiringUserActionForPlayback = []
        configuration.defaultWebpagePreferences.allowsContentJavaScript = true

        let webView = WKWebView(frame: .zero, configuration: configuration)
        webView.scrollView.contentInsetAdjustmentBehavior = .never
        webView.scrollView.isScrollEnabled = false
        webView.isOpaque = false
        webView.backgroundColor = .black
        webView.scrollView.backgroundColor = .black
        webView.allowsBackForwardNavigationGestures = false
        return webView
    }

    func updateUIView(_ webView: WKWebView, context: Context) {
        if context.coordinator.lastEmbedKey != source.embedKey {
            context.coordinator.lastEmbedKey = source.embedKey
            webView.load(URLRequest(url: source.embedURL))
        }
    }
}

private func youtubePlayerSource(from rawURL: String) -> YouTubePlayerSource? {
    guard let normalizedURL = normalizedYouTubeURL(from: rawURL) else {
        return nil
    }

    guard let videoID = resolvedYouTubeVideoID(from: rawURL, normalizedURL: normalizedURL) else {
        return nil
    }

    let externalURL = URL(string: "https://www.youtube.com/watch?v=\(videoID)") ?? normalizedURL
    guard let embedURL = URL(
        string: "https://www.youtube.com/embed/\(videoID)?playsinline=1&rel=0&modestbranding=1&controls=1"
    ) else {
        return nil
    }

    return YouTubePlayerSource(
        embedURL: embedURL,
        externalURL: externalURL,
        embedKey: videoID
    )
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
