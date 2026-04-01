import SwiftUI
import WebKit

struct YouTubeEmbedPlayerView: View {
    let item: SkydownYouTubeVideoItem

    @Environment(\.dismiss) private var dismiss
    @Environment(\.openURL) private var openURL

    var body: some View {
        NavigationStack {
            Group {
                if let embedURL = youtubeEmbedURL(from: item.urlString) {
                    YouTubeEmbedWebView(url: embedURL)
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

private func youtubeEmbedURL(from rawURL: String) -> URL? {
    guard let videoID = resolvedYouTubeVideoID(from: rawURL) else {
        return nil
    }

    return URL(string: "https://www.youtube.com/embed/\(videoID)?playsinline=1&rel=0")
}

private func resolvedYouTubeVideoID(from rawURL: String) -> String? {
    guard let url = URL(string: rawURL),
          let components = URLComponents(url: url, resolvingAgainstBaseURL: false) else {
        return nil
    }

    if let host = components.host?.lowercased(), host.contains("youtu.be") {
        return components.path.split(separator: "/").first.map(String.init)
    }

    if components.path.contains("/embed/") || components.path.contains("/shorts/") {
        return components.path.split(separator: "/").last.map(String.init)
    }

    return components.queryItems?.first { $0.name == "v" }?.value
}
