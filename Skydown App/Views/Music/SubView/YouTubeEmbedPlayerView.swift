import SwiftUI

struct YouTubeEmbedPlayerView: View {
    let item: SkydownYouTubeVideoItem

    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var colorScheme

    private var playbackURL: URL? {
        resolvedYouTubePlaybackURL(from: item.urlString)
    }

    var body: some View {
        Group {
            if let playbackURL {
                SkydownManagedBrowserView(
                    url: playbackURL,
                    title: item.title.isEmpty ? "YouTube" : item.title
                )
            } else {
                NavigationStack {
                    ContentUnavailableView(
                        "YouTube Player nicht verfuegbar",
                        systemImage: "play.rectangle",
                        description: Text("Der Link enthaelt keine gueltige Video-Adresse.")
                    )
                    .navigationTitle(item.title)
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
                    }
                }
            }
        }
    }
}

private func resolvedYouTubePlaybackURL(from rawURL: String) -> URL? {
    guard let normalizedURL = normalizedYouTubeURL(from: rawURL) else {
        return nil
    }

    if let videoID = resolvedYouTubeVideoID(from: rawURL, normalizedURL: normalizedURL),
       let canonicalURL = URL(string: "https://www.youtube.com/watch?v=\(videoID)") {
        return canonicalURL
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
