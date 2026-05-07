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

struct SkydownPremiumIconAction: View {
    let systemImage: String
    let tint: Color
    let colorScheme: ColorScheme
    var isSelected: Bool = false
    var isEnabled: Bool = true
    var size: CGFloat = SkydownLayout.iconActionSurfaceSize
    var iconSize: CGFloat = SkydownLayout.iconActionContentSize
    var accessibilityLabel: String
    let action: () -> Void

    var body: some View {
        Button {
            guard isEnabled else { return }
            SkydownHaptics.selection()
            action()
        } label: {
            SkydownPremiumIconSurface(
                systemImage: systemImage,
                tint: tint,
                colorScheme: colorScheme,
                isSelected: isSelected,
                size: size,
                iconSize: iconSize
            )
        }
        .buttonStyle(.plain)
        .disabled(!isEnabled)
        .opacity(isEnabled ? 1 : 0.52)
        .skydownTactileAction()
        .accessibilityLabel(accessibilityLabel)
        .accessibilityAddTraits(isSelected ? [.isButton, .isSelected] : .isButton)
    }
}

struct SkydownPremiumIconSurface: View {
    let systemImage: String
    let tint: Color
    let colorScheme: ColorScheme
    var isSelected: Bool = false
    var size: CGFloat = SkydownLayout.iconActionSurfaceSize
    var iconSize: CGFloat = SkydownLayout.iconActionContentSize

    var body: some View {
        Image(systemName: systemImage)
            .font(.system(size: iconSize, weight: .bold))
            .foregroundColor(isSelected ? .white : tint)
            .frame(width: size, height: size)
            .background(background)
            .overlay(border)
            .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.compactRadius, style: .continuous))
            .shadow(
                color: tint.opacity(isSelected ? (colorScheme == .dark ? 0.30 : 0.20) : 0.10),
                radius: isSelected ? 12 : 7,
                y: isSelected ? 5 : 3
            )
            .accessibilityHidden(true)
    }

    @ViewBuilder
    private var background: some View {
        if isSelected {
            LinearGradient(
                colors: [tint, tint.opacity(0.84)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        } else {
            LinearGradient(
                colors: [
                    AppColors.cardBackground(for: colorScheme).opacity(colorScheme == .dark ? 0.94 : 0.98),
                    AppColors.secondaryBackground(for: colorScheme).opacity(colorScheme == .dark ? 0.74 : 0.58),
                    tint.opacity(colorScheme == .dark ? 0.10 : 0.07)
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        }
    }

    private var border: some View {
        RoundedRectangle(cornerRadius: SkydownLayout.compactRadius, style: .continuous)
            .stroke(
                LinearGradient(
                    colors: [
                        Color.white.opacity(colorScheme == .dark ? 0.14 : 0.28),
                        tint.opacity(isSelected ? 0.36 : 0.18)
                    ],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                ),
                lineWidth: 0.9
            )
    }
}

struct SkydownPremiumLinkSurface: View {
    let title: String
    let systemImage: String
    let tint: Color
    let colorScheme: ColorScheme
    var isExpanded: Bool = true
    let action: () -> Void

    var body: some View {
        Button {
            SkydownHaptics.selection()
            action()
        } label: {
            HStack(spacing: SkydownLayout.stackSpacingTick) {
                Image(systemName: systemImage)
                    .font(.caption2.weight(.semibold))
                Text(title)
                    .font(.caption2.weight(.bold))
                    .lineLimit(1)
                    .minimumScaleFactor(0.86)
            }
            .frame(maxWidth: isExpanded ? .infinity : nil)
            .padding(.vertical, SkydownLayout.linkSurfaceVerticalPadding)
            .padding(.horizontal, SkydownLayout.linkSurfaceHorizontalPadding)
            .foregroundColor(tint)
            .background(
                Capsule(style: .continuous)
                    .fill(
                        LinearGradient(
                            colors: [
                                AppColors.secondaryBackground(for: colorScheme).opacity(colorScheme == .dark ? 0.78 : 0.66),
                                tint.opacity(colorScheme == .dark ? 0.22 : 0.14),
                                tint.opacity(colorScheme == .dark ? 0.12 : 0.08)
                            ],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
            )
            .overlay(
                Capsule(style: .continuous)
                    .stroke(tint.opacity(0.30), lineWidth: 1)
            )
            .overlay(
                Capsule(style: .continuous)
                    .stroke(
                        LinearGradient(
                            colors: [
                                .white.opacity(colorScheme == .dark ? 0.16 : 0.28),
                                tint.opacity(0.18)
                            ],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        ),
                        lineWidth: 0.8
                    )
            )
        }
        .buttonStyle(.plain)
        .skydownTactileAction()
        .accessibilityLabel(title)
    }
}

struct SkydownPremiumInlineSurface<Content: View>: View {
    let colorScheme: ColorScheme
    var accent: Color?
    var cornerRadius: CGFloat = SkydownLayout.messageBubbleRadius
    var action: (() -> Void)?
    @ViewBuilder let content: Content

    var body: some View {
        Group {
            if let action {
                Button {
                    SkydownHaptics.selection()
                    action()
                } label: {
                    surfaceContent
                }
                .buttonStyle(.plain)
                .skydownTactileAction()
            } else {
                surfaceContent
            }
        }
    }

    private var surfaceContent: some View {
        content
            .padding(SkydownLayout.inlineSurfacePadding)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                LinearGradient(
                    colors: [
                        AppColors.secondaryBackground(for: colorScheme).opacity(colorScheme == .dark ? 0.68 : 0.52),
                        (accent ?? AppColors.accent(for: colorScheme)).opacity(colorScheme == .dark ? 0.12 : 0.08)
                    ],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
            )
            .overlay(
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                    .stroke((accent ?? AppColors.accent(for: colorScheme)).opacity(0.18), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
    }
}

struct SkydownPremiumCircularProgress: View {
    let tint: Color
    let colorScheme: ColorScheme
    var scale: CGFloat = 0.92

    var body: some View {
        ProgressView()
            .tint(tint)
            .scaleEffect(scale)
            .padding(SkydownLayout.stackSpacingDense)
            .background(
                RoundedRectangle(cornerRadius: SkydownLayout.compactRadius, style: .continuous)
                    .fill(AppColors.secondaryBackground(for: colorScheme).opacity(0.78))
            )
            .overlay(
                RoundedRectangle(cornerRadius: SkydownLayout.compactRadius, style: .continuous)
                    .stroke(tint.opacity(0.16), lineWidth: 1)
            )
            .accessibilityLabel(AppLocalized.text("common.loading", fallback: "Loading"))
    }
}

struct SkydownPremiumLinearProgress: View {
    let progress: Double
    let tint: Color
    let colorScheme: ColorScheme

    var body: some View {
        ProgressView(value: min(max(progress, 0), 1))
            .tint(tint)
            .padding(.vertical, SkydownLayout.stackSpacingTick)
            .background(
                Capsule(style: .continuous)
                    .fill(AppColors.secondaryBackground(for: colorScheme).opacity(0.54))
            )
            .overlay(
                Capsule(style: .continuous)
                    .stroke(tint.opacity(0.12), lineWidth: 1)
            )
    }
}

struct SkydownPremiumToggleStyle: ToggleStyle {
    let colorScheme: ColorScheme
    var accent: Color? = nil

    func makeBody(configuration: Configuration) -> some View {
        Button {
            SkydownHaptics.selection()
            withAnimation(.spring(response: 0.28, dampingFraction: 0.82)) {
                configuration.isOn.toggle()
            }
        } label: {
            HStack(spacing: SkydownLayout.stackSpacingCompact) {
                configuration.label
                    .foregroundColor(AppColors.text(for: colorScheme))

                Spacer(minLength: SkydownLayout.stackSpacingPill)

                ZStack(alignment: configuration.isOn ? .trailing : .leading) {
                    Capsule(style: .continuous)
                        .fill(trackColor(isOn: configuration.isOn))
                        .overlay(
                            Capsule(style: .continuous)
                                .stroke(activeAccent.opacity(configuration.isOn ? 0.32 : 0.16), lineWidth: 1)
                        )
                        .shadow(
                            color: configuration.isOn ? activeAccent.opacity(0.18) : .clear,
                            radius: 10,
                            y: 4
                        )

                    Circle()
                        .fill(configuration.isOn ? AppColors.primaryBackground(for: colorScheme) : AppColors.text(for: colorScheme).opacity(0.88))
                        .frame(width: 24, height: 24)
                        .padding(3)
                }
                .frame(width: 50, height: 30)
                .accessibilityHidden(true)
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityValue(configuration.isOn ? Text("On") : Text("Off"))
    }

    private var activeAccent: Color {
        accent ?? AppColors.accent(for: colorScheme)
    }

    private func trackColor(isOn: Bool) -> Color {
        if isOn {
            return activeAccent.opacity(colorScheme == .dark ? 0.86 : 0.78)
        }
        return AppColors.secondaryBackground(for: colorScheme).opacity(colorScheme == .dark ? 0.74 : 0.58)
    }
}

struct SkydownPremiumPromptTile: View {
    let title: String
    let tint: Color
    let colorScheme: ColorScheme
    var width: CGFloat = 230
    let action: () -> Void

    var body: some View {
        Button {
            SkydownHaptics.selection()
            action()
        } label: {
            Text(title)
                .font(.subheadline.weight(.semibold))
                .multilineTextAlignment(.leading)
                .foregroundColor(AppColors.text(for: colorScheme))
                .lineLimit(3)
                .minimumScaleFactor(0.88)
                .padding(.horizontal, 14)
                .padding(.vertical, 12)
                .frame(width: width, alignment: .leading)
                .background(
                    LinearGradient(
                        colors: [
                            AppColors.cardBackground(for: colorScheme).opacity(0.96),
                            tint.opacity(colorScheme == .dark ? 0.13 : 0.09)
                        ],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                    .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.elevatedPanelRadius, style: .continuous))
                )
                .overlay(
                    RoundedRectangle(cornerRadius: SkydownLayout.elevatedPanelRadius, style: .continuous)
                        .stroke(tint.opacity(colorScheme == .dark ? 0.18 : 0.13), lineWidth: 1)
                )
        }
        .buttonStyle(.plain)
        .skydownTactileAction()
        .accessibilityLabel(title)
    }
}
