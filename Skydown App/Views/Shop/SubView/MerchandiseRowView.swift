//
//  MerchandiseRowView.swift
//  Skydown App
//
//  Created by Yang D. Nash on 22.08.25.
//

import SwiftUI

struct MerchandiseRowView: View {
    let item: MerchandiseItem
    let environmentColorScheme: ColorScheme
    let onTap: (MerchandiseItem) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            MerchandiseItemView(item: item)
                .contentShape(Rectangle())
                .onTapGesture {
                    onTap(item)
                }

            HStack {
                if item.hasCuratedMerchCategory {
                    Text(item.merchCategoryTitle)
                        .font(AppTypography.buttonLabel)
                        .foregroundColor(AppColors.accentHighlight(for: environmentColorScheme))
                } else {
                    Text("Details")
                        .font(AppTypography.buttonLabel)
                        .foregroundColor(AppColors.accent(for: environmentColorScheme))
                }

                Spacer()

                Image(systemName: "arrow.right.circle.fill")
                    .foregroundColor(AppColors.accent(for: environmentColorScheme))
            }
        }
        .padding(16)
        .skydownPanelSurface(
            colorScheme: environmentColorScheme,
            accent: AppColors.accentHighlight(for: environmentColorScheme),
            cornerRadius: 24,
            shadowRadius: 14,
            shadowYOffset: 8
        )
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.isButton)
        .accessibilityIdentifier("shop.merch.row")
    }
}
