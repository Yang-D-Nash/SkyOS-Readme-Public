//
//  MerchandiseRowView.swift
//  Skydown App
//
//  Created by Yang D. Nash on 22.08.25.
//

import SwiftUI

struct MerchandiseRowView: View {
    let item: MerchandiseItem
    let isAdmin: Bool
    let environmentColorScheme: ColorScheme
    let onTap: (MerchandiseItem) -> Void
    let onEdit: (MerchandiseItem) -> Void
    let onDelete: (MerchandiseItem) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            MerchandiseItemView(item: item)
                .contentShape(Rectangle())
                .onTapGesture {
                    onTap(item)
                }

            if isAdmin {
                HStack(spacing: 12) {
                    Button {
                        onEdit(item)
                    } label: {
                        Label("Bearbeiten", systemImage: "pencil")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)
                    .tint(AppColors.accent(for: environmentColorScheme))

                    Button(role: .destructive) {
                        onDelete(item)
                    } label: {
                        Label("Loeschen", systemImage: "trash")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                }
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
