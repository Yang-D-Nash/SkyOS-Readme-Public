//
//  MerchandiseItemView.swift
//  Skydown App
//
//  Created by Yang D. Nash on 24.07.25.
//

import SwiftUI

struct MerchandiseItemView: View {
    let item: MerchandiseItem
    @Environment(\.colorScheme) private var colorScheme
    @State private var selectedImageIndex = 0

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            ZStack(alignment: .bottomLeading) {
                TabView(selection: $selectedImageIndex) {
                    ForEach(Array(item.imageURLs.enumerated()), id: \.offset) { index, urlString in
                        AsyncImage(url: URL(string: urlString)) { image in
                            image
                                .resizable()
                                .aspectRatio(contentMode: .fill)
                                .frame(maxWidth: .infinity)
                                .frame(height: 320)
                                .clipped()
                        } placeholder: {
                            RoundedRectangle(cornerRadius: 24)
                                .fill(AppColors.secondaryBackground(for: colorScheme))
                                .overlay {
                                    ProgressView()
                                }
                        }
                        .tag(index)
                    }
                }
                .tabViewStyle(.page(indexDisplayMode: .automatic))
                .frame(height: 320)
                .clipShape(RoundedRectangle(cornerRadius: 24))
                .overlay(
                    RoundedRectangle(cornerRadius: 24)
                        .stroke(AppColors.accent(for: colorScheme).opacity(0.14), lineWidth: 1)
                )

                LinearGradient(
                    colors: [
                        .clear,
                        AppColors.primaryBackground(for: colorScheme).opacity(0.18),
                        AppColors.primaryBackground(for: colorScheme).opacity(0.92)
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .clipShape(RoundedRectangle(cornerRadius: 24))

                VStack(alignment: .leading, spacing: 8) {
                    HStack(spacing: 8) {
                        MerchInfoBadge(
                            text: item.available ? "Drop live" : "Sold out",
                            colorScheme: colorScheme,
                            isAccent: item.available
                        )
                        if item.imageURLs.count > 1 {
                            MerchInfoBadge(
                                text: "\(item.imageURLs.count) Bilder",
                                colorScheme: colorScheme,
                                isAccent: false
                            )
                        }
                    }

                    Text(item.name)
                        .font(.title2.weight(.bold))
                        .foregroundColor(AppColors.text(for: colorScheme))

                    Text("EUR \(item.price, specifier: "%.2f")")
                        .font(.headline.weight(.semibold))
                        .foregroundColor(AppColors.accent(for: colorScheme))
                }
                .padding(18)
            }

            HStack(alignment: .top, spacing: 10) {
                Text(item.description)
                    .font(.subheadline)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    .lineLimit(3)
            }
        }
    }
}

private struct MerchInfoBadge: View {
    let text: String
    let colorScheme: ColorScheme
    let isAccent: Bool

    var body: some View {
        Text(text)
            .font(.caption.weight(.semibold))
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(
                isAccent
                    ? AppColors.accent(for: colorScheme).opacity(0.14)
                    : AppColors.secondaryBackground(for: colorScheme).opacity(0.9)
            )
            .foregroundColor(
                isAccent
                    ? AppColors.accent(for: colorScheme)
                    : AppColors.secondaryText(for: colorScheme)
            )
            .clipShape(Capsule())
    }
}

#Preview {
    let mockItem = MerchandiseItem(
        id: "1",
        name: "Exklusives T-Shirt",
        price: 24.99,
        description: "Ein super cooles T-Shirt fuer jeden Anlass.",
        imageURLs: [
            "https://i.imgur.com/8QG3tQJ.png",
            "https://i.imgur.com/G20y5iQ.png",
            "https://i.imgur.com/R38w6kS.png"
        ],
        available: true
    )

    MerchandiseItemView(item: mockItem)
        .padding()
}
