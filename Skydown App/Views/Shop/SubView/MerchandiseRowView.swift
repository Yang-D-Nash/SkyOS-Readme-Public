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
        MerchandiseItemView(item: item)
            .contentShape(Rectangle())
            .onTapGesture {
                onTap(item)
            }
            .padding(12)
        .skydownPanelSurface(
            colorScheme: environmentColorScheme,
            accent: AppColors.accentHighlight(for: environmentColorScheme),
            cornerRadius: 22,
            shadowRadius: 9,
            shadowYOffset: 5
        )
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.isButton)
        .accessibilityIdentifier("shop.merch.row")
    }
}
