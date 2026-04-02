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
                Text("Details")
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(AppColors.accent(for: environmentColorScheme))

                Spacer()

                Image(systemName: "arrow.right.circle.fill")
                    .foregroundColor(AppColors.accent(for: environmentColorScheme))
            }
        }
        .padding(16)
        .background(AppColors.cardBackground(for: environmentColorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: 24)
                .stroke(AppColors.accent(for: environmentColorScheme).opacity(0.12), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 24))
        .shadow(color: .black.opacity(environmentColorScheme == .dark ? 0.18 : 0.06), radius: 12, y: 8)
    }
}
