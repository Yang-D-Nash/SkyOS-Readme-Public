import SwiftUI

struct HomeUtilityRow: View {
    let colorScheme: ColorScheme
    let onOpenMusic: () -> Void
    let onOpenVideo: () -> Void
    let onOpenMerch: () -> Void
    let onOpenSettings: () -> Void

    private enum UtilityKind: Hashable { case music, video, merch, settings }

    private struct UtilityItem {
        let kind: UtilityKind
        let title: String
        let icon: String
        let action: () -> Void
    }

    private var utilities: [UtilityItem] {
        [
            UtilityItem(
                kind: .music,
                title: AppLocalized.text("home.utility.music", fallback: "Music"),
                icon: "music.note",
                action: onOpenMusic
            ),
            UtilityItem(
                kind: .video,
                title: AppLocalized.text("home.utility.videos", fallback: "Videos"),
                icon: "play.rectangle",
                action: onOpenVideo
            ),
            UtilityItem(
                kind: .merch,
                title: AppLocalized.text("home.utility.merch", fallback: "Merch"),
                icon: "bag",
                action: onOpenMerch
            ),
            UtilityItem(
                kind: .settings,
                title: AppLocalized.text("home.utility.settings", fallback: "Settings"),
                icon: "gearshape",
                action: onOpenSettings
            )
        ]
    }

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingMicro) {
            Text(
                AppLocalized.text("home.explore.title", fallback: "Open")
            )
            .font(.caption2.weight(.medium))
            .foregroundColor(AppColors.text(for: colorScheme).opacity(0.5))

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: SkydownLayout.stackSpacingPill) {
                    ForEach(Array(utilities.enumerated()), id: \.offset) { _, utility in
                        utilityButton(utility)
                    }
                }
            }
        }
        .padding(.top, 4)
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    @ViewBuilder
    private func utilityButton(_ utility: UtilityItem) -> some View {
        let tint = utilityTint(for: utility.kind)
        Button {
            SkydownHaptics.selection()
            utility.action()
        } label: {
            HStack(spacing: SkydownLayout.stackSpacingDense) {
                Image(systemName: utility.icon)
                    .font(.caption.weight(.semibold))
                    .padding(5)
                    .background(Circle().fill(tint.opacity(0.14)))
                Text(utility.title)
                    .font(.caption.weight(.semibold))
            }
            .padding(.horizontal, 11)
            .padding(.vertical, 9)
            .background(
                RoundedRectangle(cornerRadius: SkydownLayout.tightRadius, style: .continuous)
                    .fill(AppColors.secondaryBackground(for: colorScheme).opacity(0.72))
            )
            .overlay(
                RoundedRectangle(cornerRadius: SkydownLayout.tightRadius, style: .continuous)
                    .stroke(tint.opacity(0.22), lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
        .foregroundColor(tint)
    }

    private func utilityTint(for kind: UtilityKind) -> Color {
        switch kind {
        case .music:
            return AppColors.accent(for: colorScheme).opacity(0.94)
        case .video:
            return AppColors.accentHighlight(for: colorScheme).opacity(0.94)
        case .merch:
            return AppColors.accentMystic(for: colorScheme).opacity(0.94)
        case .settings:
            return AppColors.text(for: colorScheme).opacity(0.76)
        }
    }
}

private struct HomeRevealModifier: ViewModifier {
    let order: Int
    @State private var isVisible = false

    func body(content: Content) -> some View {
        content
            .opacity(isVisible ? 1 : 0)
            .offset(y: isVisible ? 0 : 10)
            .animation(
                SkydownMotion.contentReveal.delay(Double(order) * SkydownMotion.listStaggerDelay),
                value: isVisible
            )
            .onAppear { isVisible = true }
    }
}

extension View {
    func homeReveal(_ order: Int) -> some View {
        modifier(HomeRevealModifier(order: order))
    }
}
