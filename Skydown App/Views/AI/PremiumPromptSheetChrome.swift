//
//  PremiumPromptSheetChrome.swift
//  Skydown — shared layout for AI & Agent prompt sheets (calm hierarchy, card sections).
//

import SwiftUI

// MARK: - Header

struct PremiumPromptSheetHeader: View {
    let iconSystemName: String
    let title: String
    let subtitle: String
    let accent: Color
    let colorScheme: ColorScheme
    let onDismiss: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingSnug) {
            HStack(alignment: .top) {
                Spacer(minLength: 0)
                Button(action: onDismiss) {
                    Image(systemName: "xmark")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        .frame(width: 32, height: 32)
                        .background(
                            Circle()
                                .fill(AppColors.secondaryBackground(for: colorScheme).opacity(0.85))
                        )
                }
                .buttonStyle(.plain)
                .skydownTactileAction()
                .accessibilityLabel(AppLocalized.text("common.close", fallback: "Close"))
            }

            HStack(alignment: .center, spacing: SkydownLayout.stackSpacingLoft) {
                ZStack {
                    RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous)
                        .fill(
                            LinearGradient(
                                colors: [
                                    accent.opacity(0.2),
                                    accent.opacity(0.08),
                                ],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                    Image(systemName: iconSystemName)
                        .font(.system(size: 22, weight: .semibold))
                        .foregroundStyle(accent)
                }
                .frame(width: 56, height: 56)
                .overlay(
                    RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous)
                        .stroke(accent.opacity(0.2), lineWidth: 1)
                )

                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingTick) {
                    Text(title)
                        .font(.title2.weight(.bold))
                        .foregroundColor(AppColors.text(for: colorScheme))
                        .fixedSize(horizontal: false, vertical: true)
                    Text(subtitle)
                        .font(.subheadline)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        .fixedSize(horizontal: false, vertical: true)
                        .lineSpacing(2)
                }
            }
        }
    }
}

// MARK: - Section

struct PremiumPromptSectionHeader: View {
    let title: String
    var footnote: String? = nil
    let accent: Color
    let colorScheme: ColorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingNano) {
            Text(title.uppercased())
                .font(.caption2.weight(.bold))
                .tracking(0.6)
                .foregroundColor(accent.opacity(0.95))
            if let footnote, !footnote.isEmpty {
                Text(footnote)
                    .font(.caption)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }
        }
    }
}

struct PremiumPromptCard<Content: View>: View {
    let colorScheme: ColorScheme
    var emphasisAccent: Color? = nil
    @ViewBuilder var content: () -> Content

    var body: some View {
        content()
            .padding(SkydownLayout.cardPadding)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                RoundedRectangle(cornerRadius: SkydownLayout.elevatedPanelRadius, style: .continuous)
                    .fill(AppColors.cardBackground(for: colorScheme).opacity(0.92))
            )
            .overlay(
                RoundedRectangle(cornerRadius: SkydownLayout.elevatedPanelRadius, style: .continuous)
                    .stroke(
                        (emphasisAccent ?? AppColors.secondaryText(for: colorScheme)).opacity(0.1),
                        lineWidth: 1
                    )
            )
            .shadow(
                color: AppColors.text(for: colorScheme).opacity(colorScheme == .dark ? 0.12 : 0.04),
                radius: 12,
                y: 4
            )
    }
}

// MARK: - Primary CTA

struct PremiumPromptPrimaryButton: View {
    let title: String
    let systemImage: String
    let accent: Color
    let colorScheme: ColorScheme
    let isEnabled: Bool
    let action: () -> Void
    @Environment(\.accessibilityReduceMotion) private var accessibilityReduceMotion

    var body: some View {
        SkydownBrandActionButton(
            title: title,
            systemImage: systemImage,
            accent: accent,
            colorScheme: colorScheme,
            isEnabled: isEnabled,
            font: .subheadline.weight(.semibold),
            cornerRadius: SkydownLayout.buttonCornerRadius + 2,
            verticalPadding: 12,
            action: action
        )
        .animation(
            SkydownMotion.preferredContentReveal(accessibilityReduceMotion: accessibilityReduceMotion),
            value: isEnabled
        )
    }
}

// MARK: - Dropdown settings (menu pickers, kompakt, eine Karte)

struct PremiumPromptSettingsDropdownCard<Content: View>: View {
    let colorScheme: ColorScheme
    var emphasisAccent: Color? = nil
    @ViewBuilder var content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            content()
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.vertical, 4)
        .padding(.horizontal, SkydownLayout.tightRadius)
        .background(
            RoundedRectangle(cornerRadius: SkydownLayout.elevatedPanelRadius, style: .continuous)
                .fill(AppColors.cardBackground(for: colorScheme).opacity(0.92))
        )
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.elevatedPanelRadius, style: .continuous)
                .stroke(
                    (emphasisAccent ?? AppColors.secondaryText(for: colorScheme)).opacity(0.1),
                    lineWidth: 1
                )
        )
    }
}

