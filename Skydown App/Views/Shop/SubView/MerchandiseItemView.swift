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
    @State private var showingFullscreenGallery = false

    private var displayImageURLs: [String] {
        if let customOverride = item.customImageOverride.takeIfNotBlank() {
            return [customOverride] + item.imageURLs.filter { $0 != customOverride }
        }
        return item.imageURLs
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            ZStack(alignment: .bottomLeading) {
                TabView(selection: $selectedImageIndex) {
                    ForEach(Array(displayImageURLs.enumerated()), id: \.offset) { index, urlString in
                        AsyncImage(url: URL(string: urlString)) { image in
                            image
                                .resizable()
                                .aspectRatio(contentMode: .fill)
                                .frame(maxWidth: .infinity)
                                .frame(height: 360)
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
                .frame(height: 360)
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
                        if displayImageURLs.count > 1 {
                            MerchInfoBadge(
                                text: "\(displayImageURLs.count) Bilder",
                                colorScheme: colorScheme,
                                isAccent: false
                            )
                        }
                        if let customBadge = item.customBadge.takeIfNotBlank() {
                            MerchInfoBadge(
                                text: customBadge,
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

                Button {
                    showingFullscreenGallery = true
                } label: {
                    Label("Vollbild", systemImage: "arrow.up.left.and.arrow.down.right")
                        .font(.caption.weight(.semibold))
                        .foregroundColor(AppColors.text(for: colorScheme))
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(AppColors.cardBackground(for: colorScheme).opacity(0.92))
                        .clipShape(Capsule())
                }
                .buttonStyle(.plain)
                .padding(16)
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topTrailing)
            }

            HStack(alignment: .top, spacing: 10) {
                Text(item.description)
                    .font(.subheadline)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    .lineLimit(3)
            }
        }
        .fullScreenCover(isPresented: $showingFullscreenGallery) {
            MerchandiseFullscreenGalleryView(
                itemName: item.name,
                imageURLs: displayImageURLs,
                selectedImageIndex: $selectedImageIndex
            )
        }
    }
}

private struct MerchandiseFullscreenGalleryView: View {
    let itemName: String
    let imageURLs: [String]
    @Binding var selectedImageIndex: Int
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ZStack(alignment: .top) {
            Color.black
                .ignoresSafeArea()

            TabView(selection: $selectedImageIndex) {
                ForEach(Array(imageURLs.enumerated()), id: \.offset) { index, urlString in
                    ZStack {
                        Color.black
                            .ignoresSafeArea()

                        AsyncImage(url: URL(string: urlString)) { image in
                            image
                                .resizable()
                                .scaledToFit()
                                .frame(maxWidth: .infinity, maxHeight: .infinity)
                                .padding(.horizontal, 16)
                                .padding(.vertical, 40)
                        } placeholder: {
                            ProgressView()
                                .tint(.white)
                        }
                    }
                    .tag(index)
                }
            }
            .tabViewStyle(.page(indexDisplayMode: .automatic))

            VStack(spacing: 12) {
                HStack(alignment: .top) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(itemName)
                            .font(.headline.weight(.bold))
                            .foregroundColor(.white)
                            .lineLimit(2)

                        Text("\(selectedImageIndex + 1) von \(max(imageURLs.count, 1))")
                            .font(.subheadline)
                            .foregroundColor(.white.opacity(0.72))
                    }

                    Spacer()

                    Button(action: { dismiss() }, label: {
                        Image(systemName: "xmark")
                            .font(.headline.weight(.bold))
                            .foregroundColor(.white)
                            .frame(width: 42, height: 42)
                            .background(Color.white.opacity(0.14))
                            .clipShape(Circle())
                    })
                    .buttonStyle(.plain)
                }
                .padding(.horizontal, 20)
                .padding(.top, 18)

                Spacer()
            }
        }
    }
}

private extension String {
    func takeIfNotBlank() -> String? {
        let trimmed = trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : trimmed
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
