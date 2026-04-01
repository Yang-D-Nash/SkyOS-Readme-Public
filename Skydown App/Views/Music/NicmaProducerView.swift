//
//  NicmaProducerView.swift
//  Skydown App
//
//  Created by Codex on 27.03.26.
//

import SwiftUI

struct NicmaProducerView: View {
    @Environment(\.colorScheme) private var colorScheme
    let onBack: (() -> Void)?

    init(onBack: (() -> Void)? = nil) {
        self.onBack = onBack
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                heroCard
                pricingCard
                contactCard
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
        }
        .background(AppColors.primaryBackground(for: colorScheme).ignoresSafeArea())
        .navigationTitle("NICMA MUSIC")
        .navigationBarTitleDisplayMode(.inline)
        .skydownNavigationChrome(colorScheme: colorScheme)
        .toolbar {
            if let onBack {
                ToolbarItem(placement: .topBarLeading) {
                    Button(action: onBack) {
                        Image(systemName: "chevron.left")
                            .font(.headline.weight(.bold))
                    }
                }
            }
        }
    }

    private var heroCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("NICMA MUSIC")
                .font(.largeTitle.bold())
                .foregroundColor(AppColors.text(for: colorScheme))

            Text("Mixing, Mastering und Recording mit klarer Preisliste, direktem Kontakt und sauberem Producer-Fokus.")
                .font(.body)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            HStack(spacing: 8) {
                MusicBadge(text: "Studio Services", isAccent: true)
                MusicBadge(text: "Mix & Master", isAccent: false)
                MusicBadge(text: "Recording", isAccent: false)
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 26)
                .fill(AppColors.cardBackground(for: colorScheme))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 26)
                .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.18), lineWidth: 1)
        )
    }

    private var pricingCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Preisliste")
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            ForEach(nicmaProducerPackages) { package in
                NicmaProducerPriceCard(
                    package: package,
                    colorScheme: colorScheme
                )
            }
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 24)
                .fill(AppColors.cardBackground(for: colorScheme))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 24)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.14), lineWidth: 1)
        )
    }

    private var contactCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Kontakt")
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            Text("Anfragen fuer Mixing, Mastering und Recording laufen oeffentlich nur noch ueber Instagram.")
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            if let instagramURL = nicmaInstagramDestination.url {
                Link(destination: instagramURL) {
                    Label("NICMA MUSIC auf Instagram", systemImage: "camera.fill")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                }
                .foregroundColor(AppColors.text(for: colorScheme))
                .background(AppColors.secondaryBackground(for: colorScheme))
                .overlay(
                    RoundedRectangle(cornerRadius: 18)
                        .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.18), lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: 18))
            }
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 24)
                .fill(AppColors.cardBackground(for: colorScheme))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 24)
                .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.16), lineWidth: 1)
        )
    }
}
