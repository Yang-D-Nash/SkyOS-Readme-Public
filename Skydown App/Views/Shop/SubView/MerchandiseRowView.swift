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
    var shelfSpotlight: Bool = false
    var shelfSettled: Bool = false
    let onTap: (MerchandiseItem) -> Void

    private var edgePadding: CGFloat {
        if shelfSpotlight { return 16 }
        if shelfSettled { return 8 }
        return 12
    }

    var body: some View {
        MerchandiseItemView(item: item, shelfSpotlight: shelfSpotlight, shelfSettled: shelfSettled)
            .contentShape(Rectangle())
            .onTapGesture {
                onTap(item)
            }
            .padding(edgePadding)
            .opacity(shelfSettled ? 0.95 : 1)
        .overlay(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .stroke(
                    AppColors.accentHighlight(for: environmentColorScheme)
                        .opacity(shelfSpotlight ? 0.3 : 0),
                    lineWidth: shelfSpotlight ? 1.5 : 0
                )
        )
        .skydownPanelSurface(
            colorScheme: environmentColorScheme,
            accent: AppColors.accentHighlight(for: environmentColorScheme),
            cornerRadius: 22,
            shadowRadius: shelfSpotlight ? 12 : (shelfSettled ? 7 : 9),
            shadowYOffset: shelfSpotlight ? 6 : (shelfSettled ? 3 : 5)
        )
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.isButton)
        .accessibilityIdentifier("shop.merch.row")
    }
}
