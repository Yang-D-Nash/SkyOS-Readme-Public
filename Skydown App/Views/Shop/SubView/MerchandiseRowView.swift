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
                        Label("Löschen", systemImage: "trash")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                }
                .padding(.horizontal, 10)
                .padding(.bottom, 10)
            }
        }
        .listRowBackground(AppColors.secondaryBackground(for: environmentColorScheme))
        .swipeActions(edge: .leading, allowsFullSwipe: false) {
            if isAdmin {
                Button {
                    onEdit(item)
                } label: {
                    Label("Bearbeiten", systemImage: "pencil")
                }
                .tint(.blue)

                Button(role: .destructive) {
                    onDelete(item)
                } label: {
                    Label("Löschen", systemImage: "trash")
                }
            }
        }
    }
}
