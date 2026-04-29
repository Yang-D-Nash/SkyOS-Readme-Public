import SwiftUI

/// Compact portal chip (icon + title) for horizontal shortcut rows (e.g. Home “Portals”).
/// Matches Android `SkydownPortalChip` intent; tweak both when evolving the design system.
struct SkydownPortalChip: View {
    let title: String
    let systemImage: String
    let tint: Color
    let colorScheme: ColorScheme
    let action: () -> Void

    var body: some View {
        Button {
            SkydownHaptics.selection()
            action()
        } label: {
            HStack(spacing: SkydownLayout.stackSpacingDense) {
                Image(systemName: systemImage)
                    .font(.caption.weight(.medium))
                Text(title)
                    .font(.caption.weight(.semibold))
            }
            .padding(.horizontal, 13)
            .padding(.vertical, 10)
            .background(
                RoundedRectangle(cornerRadius: SkydownLayout.tightRadius, style: .continuous)
                    .fill(AppColors.secondaryBackground(for: colorScheme).opacity(0.58))
            )
            .overlay(
                RoundedRectangle(cornerRadius: SkydownLayout.tightRadius, style: .continuous)
                    .stroke(tint.opacity(0.16), lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
        .foregroundColor(tint)
        .contentShape(RoundedRectangle(cornerRadius: SkydownLayout.tightRadius, style: .continuous))
        .skydownTactileAction()
        .accessibilityLabel(title)
    }
}
