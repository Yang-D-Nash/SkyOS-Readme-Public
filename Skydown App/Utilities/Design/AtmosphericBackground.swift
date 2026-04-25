import SwiftUI

struct SkydownAtmosphereBackdrop: View {
    let colorScheme: ColorScheme
    /// Extra luminance scrim on top of the image (other screens). Landing uses `false` — base + image only.
    var showsLuminanceScrim: Bool = true

    var body: some View {
        Image("SkylineAtmosphereBackground")
            .resizable()
            .scaledToFill()
            .saturation(colorScheme == .dark ? 0.96 : 0.78)
            .contrast(colorScheme == .dark ? 1.04 : 0.90)
            .opacity(colorScheme == .dark ? 0.18 : 0.08)
            .blendMode(colorScheme == .dark ? .screen : .multiply)
            .overlay {
                if showsLuminanceScrim {
                    LinearGradient(
                        colors: [
                            Color.black.opacity(colorScheme == .dark ? 0.24 : 0.03),
                            Color.black.opacity(colorScheme == .dark ? 0.12 : 0.00),
                            Color.black.opacity(colorScheme == .dark ? 0.18 : 0.05)
                        ],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                }
            }
            .ignoresSafeArea()
            .allowsHitTesting(false)
            .accessibilityHidden(true)
    }
}
