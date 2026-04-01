//
//  CartItemSectionView.swift
//  Skydown App
//
//  Created by Yang D. Nash on 23.08.25.
//

import SwiftUI

struct CartItemSectionView: View {
    @ObservedObject var cartVM: CartViewModel

    var body: some View {
        Section("Dein Warenkorb") {
            if cartVM.items.isEmpty {
                Text("Dein Warenkorb ist leer.").foregroundColor(.gray)
            } else {
                ForEach(cartVM.items) { cartItem in
                    HStack {
                        VStack(alignment: .leading) {
                            Text(cartItem.item.name)
                            Text(
                                "Größe: \(cartItem.size)\(cartItem.color.map { ", Farbe: \($0)" } ?? ""), Anzahl: \(cartItem.quantity)"
                            )
                                .font(.subheadline)
                                .foregroundColor(.gray)
                        }
                        Spacer()
                        Text(String(format: "€ %.2f", cartItem.effectiveUnitPrice * Double(cartItem.quantity)))
                    }
                }
                .onDelete { indexSet in
                    indexSet.forEach { cartVM.removeItem(cartVM.items[$0]) }
                }
            }
        }
    }
}
