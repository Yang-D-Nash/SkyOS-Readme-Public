import SwiftUI

struct HomeUtilityRow: View {
    let colorScheme: ColorScheme
    let onOpenAI: () -> Void
    let onOpenMusic: () -> Void
    let onOpenCreate: () -> Void
    let onOpenOrders: () -> Void
    let onOpenSearch: () -> Void
    let onOpenSettings: () -> Void

    private enum UtilityStyle {
        case primary
        case iconOnly
    }

    private struct UtilityItem {
        let title: String
        let icon: String
        let style: UtilityStyle
        let action: () -> Void
    }

    private var utilities: [UtilityItem] {
        [
            UtilityItem(title: "AI", icon: "sparkles", style: .primary, action: onOpenAI),
            UtilityItem(title: "Music", icon: "music.note", style: .primary, action: onOpenMusic),
            UtilityItem(title: "Create", icon: "plus.circle", style: .primary, action: onOpenCreate),
            UtilityItem(title: "Orders", icon: "bag", style: .primary, action: onOpenOrders),
            UtilityItem(title: "Search", icon: "magnifyingglass", style: .iconOnly, action: onOpenSearch),
            UtilityItem(title: "Settings", icon: "gearshape", style: .iconOnly, action: onOpenSettings)
        ]
    }

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(Array(utilities.enumerated()), id: \.offset) { _, utility in
                    Button {
                        SkydownHaptics.selection()
                        utility.action()
                    } label: {
                        switch utility.style {
                        case .primary:
                            Label(utility.title, systemImage: utility.icon)
                        case .iconOnly:
                            Image(systemName: utility.icon)
                                .frame(width: 18, height: 18)
                                .accessibilityLabel(utility.title)
                        }
                    }
                    .buttonStyle(.bordered)
                    .controlSize(.small)
                    .tint(AppColors.accent(for: colorScheme))
                }
            }
            .padding(.horizontal, 8)
            .padding(.vertical, 8)
        }
        .background(AppColors.cardBackground(for: colorScheme).opacity(0.35))
        .overlay(RoundedRectangle(cornerRadius: 12, style: .continuous).stroke(AppColors.accent(for: colorScheme).opacity(0.10), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

private struct HomeRevealModifier: ViewModifier {
    let order: Int
    @State private var isVisible = false

    func body(content: Content) -> some View {
        content
            .opacity(isVisible ? 1 : 0)
            .offset(y: isVisible ? 0 : 18)
            .scaleEffect(isVisible ? 1 : 0.985)
            .animation(.spring(response: 0.52, dampingFraction: 0.88).delay(Double(order) * 0.05), value: isVisible)
            .onAppear { isVisible = true }
    }
}

extension View {
    func homeReveal(_ order: Int) -> some View {
        modifier(HomeRevealModifier(order: order))
    }
}
