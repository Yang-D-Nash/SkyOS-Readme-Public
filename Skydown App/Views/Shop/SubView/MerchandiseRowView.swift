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
        HStack {
            MerchandiseItemView(item: item)
                .listRowBackground(AppColors.secondaryBackground(for: environmentColorScheme))
        }
        .contentShape(Rectangle())
        .onTapGesture {
            onTap(item)
        }
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
