import SwiftUI

/// Primary / muted CTA aligned with Android `BrandActionButton` (gradient vs bordered surface).
struct SkydownBrandActionButton: View {
    enum Role {
        case primary
        case muted
    }

    let title: String
    var systemImage: String?
    let accent: Color
    let colorScheme: ColorScheme
    var role: Role = .primary
    var isEnabled: Bool = true
    var isLoading: Bool = false
    var font: Font = .headline
    var cornerRadius: CGFloat = SkydownLayout.cardCornerRadius
    var verticalPadding: CGFloat = 14
    /// When `false`, the control sizes to its content (e.g. trailing strip actions).
    var expandToFullWidth: Bool = true
    let action: () -> Void

    private var trimmedTitle: String {
        title.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    /// Symbol-only toolbar CTAs (empty title + `systemImage`) size like a compact chip.
    private var isIconOnly: Bool {
        !isLoading && trimmedTitle.isEmpty && systemImage != nil
    }

    var body: some View {
        Button {
            guard isEnabled && !isLoading else { return }
            SkydownHaptics.selection()
            action()
        } label: {
            let row = HStack(spacing: SkydownLayout.stackSpacingMicro) {
                if isLoading {
                    ProgressView()
                        .tint(foregroundColor)
                        .scaleEffect(0.9)
                } else if let systemImage {
                    Image(systemName: systemImage)
                        .font(font)
                }
                if !trimmedTitle.isEmpty {
                    Text(title)
                        .font(font)
                        .multilineTextAlignment(.center)
                        .lineLimit(2)
                        .minimumScaleFactor(0.88)
                }
            }
            Group {
                if expandToFullWidth {
                    row.frame(maxWidth: .infinity)
                } else {
                    row
                }
            }
            .padding(.horizontal, isIconOnly ? 10 : 0)
            .padding(.vertical, verticalPadding)
            .foregroundColor(foregroundColor)
            .background(background)
            .overlay(
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                    .stroke(borderColor, lineWidth: borderWidth)
            )
            .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
            .shadow(color: shadowColor, radius: shadowRadius, y: shadowY)
        }
        .buttonStyle(.plain)
        .disabled(!isEnabled || isLoading)
        .opacity(role == .muted && !isEnabled ? 0.6 : (isEnabled ? 1 : 0.78))
        .skydownTactileAction()
    }

    private var foregroundColor: Color {
        switch role {
        case .primary:
            if isEnabled {
                return .white.opacity(0.96)
            }
            return AppColors.secondaryText(for: colorScheme)
        case .muted:
            if isEnabled {
                return AppColors.text(for: colorScheme)
            }
            return AppColors.secondaryText(for: colorScheme)
        }
    }

    @ViewBuilder
    private var background: some View {
        switch role {
        case .primary:
            if isEnabled {
                LinearGradient(
                    colors: [accent, accent.opacity(0.88)],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
            } else {
                AppColors.secondaryBackground(for: colorScheme)
            }
        case .muted:
            AppColors.secondaryBackground(for: colorScheme)
        }
    }

    private var borderColor: Color {
        switch role {
        case .primary:
            return isEnabled ? Color.clear : accent.opacity(0.12)
        case .muted:
            return accent.opacity(isEnabled ? 0.14 : 0.10)
        }
    }

    private var borderWidth: CGFloat {
        switch role {
        case .primary:
            return isEnabled ? 0 : 1
        case .muted:
            return 1
        }
    }

    private var shadowColor: Color {
        guard role == .primary, isEnabled else { return .clear }
        return accent.opacity(colorScheme == .dark ? 0.28 : 0.18)
    }

    private var shadowRadius: CGFloat {
        role == .primary && isEnabled ? 10 : 0
    }

    private var shadowY: CGFloat {
        role == .primary && isEnabled ? 4 : 0
    }
}
