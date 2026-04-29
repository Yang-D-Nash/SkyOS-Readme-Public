//
//  PolicyView.swift
//  Skydown App
//
//  Created by Yang D. Nash on 29.07.25.
//

import SwiftUI

struct PolicyView: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    let title: String
    let text: String
    /// Optional line shown under the title, e.g. "Stand: …"
    var lastUpdated: String? = nil
    /// Shown in the footer; optional.
    var supportEmail: String? = nil
    /// Short operational disclaimer — not the legal text itself. Keep distinct styling.
    var showTransparencyNote: Bool = true

    private var parsedParagraphs: [String] {
        text
            .components(separatedBy: "\n\n")
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingSection) {
                    Text(title)
                        .font(.title2.bold())
                        .foregroundStyle(AppColors.text(for: colorScheme))
                        .frame(maxWidth: .infinity, alignment: .leading)

                    if let lastUpdated, !lastUpdated.isEmpty {
                        Text(
                            String(
                                format: AppLocalized.text("legal.ui.last_updated_format", fallback: "Last updated: %@"),
                                lastUpdated
                            )
                        )
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(AppColors.secondaryText(for: colorScheme))
                        .frame(maxWidth: .infinity, alignment: .leading)
                    }

                    if showTransparencyNote {
                        Text(
                            AppLocalized.text(
                                "legal.ui.transparency_note",
                                fallback: "This information is provided for transparency and is updated on an ongoing basis."
                            )
                        )
                        .font(.caption)
                        .foregroundStyle(AppColors.secondaryText(for: colorScheme))
                        .padding(12)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(
                            RoundedRectangle(cornerRadius: SkydownLayout.tightRadius, style: .continuous)
                                .fill(AppColors.secondaryBackground(for: colorScheme).opacity(0.65))
                        )
                        .accessibilityLabel(
                            AppLocalized.text("legal.ui.transparency_a11y", fallback: "Transparency notice before policy text")
                        )
                    }

                    if parsedParagraphs.count > 1 {
                        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingRelaxed) {
                            ForEach(Array(parsedParagraphs.enumerated()), id: \.offset) { _, paragraph in
                                Text(paragraph)
                                    .font(.body)
                                    .foregroundStyle(AppColors.text(for: colorScheme))
                                    .lineSpacing(3)
                                    .textSelection(.enabled)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                            }
                        }
                    } else {
                        Text(text)
                            .font(.body)
                            .foregroundStyle(AppColors.text(for: colorScheme))
                            .lineSpacing(3)
                            .textSelection(.enabled)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }

                    if let supportEmail, !supportEmail.isEmpty {
                        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingDense) {
                            Text(AppLocalized.text("legal.ui.contact_header", fallback: "Contact"))
                                .font(.subheadline.weight(.semibold))
                                .foregroundStyle(AppColors.text(for: colorScheme))
                            Text(supportEmail)
                                .font(.subheadline)
                                .textSelection(.enabled)
                                .foregroundStyle(AppColors.accent(for: colorScheme))
                        }
                        .padding(.top, 4)
                    }
                }
                .padding(20)
            }
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .skydownNavigationChrome(colorScheme: colorScheme)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    SkydownBrandActionButton(
                        title: AppLocalized.text("common.done", fallback: "Done"),
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
            .background(AppColors.primaryBackground(for: colorScheme).ignoresSafeArea())
        }
    }
}
