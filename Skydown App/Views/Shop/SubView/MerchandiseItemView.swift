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

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 12) {
                    ForEach(item.imageURLs, id: \.self) { urlString in
                        AsyncImage(url: URL(string: urlString)) { image in
                            image
                                .resizable()
                                .aspectRatio(contentMode: .fill)
                                .frame(width: 260, height: 260)
                                .clipShape(RoundedRectangle(cornerRadius: 18))
                        } placeholder: {
                            RoundedRectangle(cornerRadius: 18)
                                .fill(AppColors.secondaryBackground(for: colorScheme))
                                .frame(width: 260, height: 260)
                                .overlay {
                                    ProgressView()
                                }
                        }
                    }
                }
            }

            HStack(alignment: .top, spacing: 10) {
                VStack(alignment: .leading, spacing: 6) {
                    Text(item.name)
                        .font(.title3.weight(.semibold))
                        .foregroundColor(AppColors.text(for: colorScheme))

                    Text(item.description)
                        .font(.subheadline)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        .lineLimit(3)
                }

                Spacer(minLength: 0)

                Text("EUR \(item.price, specifier: "%.2f")")
                    .font(.subheadline.weight(.semibold))
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(AppColors.accent(for: colorScheme).opacity(0.12))
                    .foregroundColor(AppColors.accent(for: colorScheme))
                    .clipShape(Capsule())
            }
        }
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
