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
    private let imageWidth: CGFloat = 112
    private let imageHeight: CGFloat = 132

    private var displayImageURLs: [String] {
        if let customOverride = item.customImageOverride.takeIfNotBlank() {
            return [customOverride] + item.imageURLs.filter { $0 != customOverride }
        }
        return item.imageURLs
    }

    private var safeImageURLs: [String] {
        displayImageURLs.isEmpty ? [""] : displayImageURLs
    }

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            imagePager

            VStack(alignment: .leading, spacing: 8) {
                badgeStrip

                Text(item.name)
                    .font(AppTypography.listTitle)
                    .foregroundColor(AppColors.text(for: colorScheme))
                    .lineLimit(2)
                    .fixedSize(horizontal: false, vertical: true)

                Text("EUR \(item.price, specifier: "%.2f")")
                    .font(AppTypography.metricLabel)
                    .foregroundColor(AppColors.accent(for: colorScheme))

                if !item.description.isEmpty {
                    Text(item.description)
                        .font(AppTypography.bodyCaption)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        .lineLimit(2)
                }

                HStack(spacing: 8) {
                    Text(item.hasCuratedMerchCategory ? item.merchCategorySubtitle : "House line")
                        .font(AppTypography.listMeta)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        .lineLimit(1)

                    Spacer(minLength: 6)

                    Label("Details", systemImage: "chevron.right")
                        .font(AppTypography.buttonLabel)
                        .labelStyle(.titleAndIcon)
                        .foregroundColor(AppColors.accent(for: colorScheme))
                        .lineLimit(1)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .fullScreenCover(isPresented: $showingFullscreenGallery) {
            NavigationStack {
                MerchandiseFullscreenGalleryView(
                    itemName: item.name,
                    imageURLs: safeImageURLs,
                    selectedImageIndex: $selectedImageIndex
                )
                .toolbar {
                    ToolbarItem(placement: .topBarTrailing) {
                        Button("Schliessen") {
                            showingFullscreenGallery = false
                        }
                        .accessibilityIdentifier("shop.merch.fullscreen.close")
                    }
                }
                .toolbarBackground(.hidden, for: .navigationBar)
                .toolbarColorScheme(.dark, for: .navigationBar)
                .navigationBarTitleDisplayMode(.inline)
            }
        }
    }

    private var imagePager: some View {
        ZStack(alignment: .bottom) {
            TabView(selection: $selectedImageIndex) {
                ForEach(Array(safeImageURLs.enumerated()), id: \.offset) { index, urlString in
                    AsyncImage(url: URL(string: urlString)) { image in
                        image
                            .resizable()
                            .aspectRatio(contentMode: .fill)
                            .frame(width: imageWidth, height: imageHeight)
                            .clipped()
                    } placeholder: {
                        RoundedRectangle(cornerRadius: 18, style: .continuous)
                            .fill(AppColors.secondaryBackground(for: colorScheme))
                            .overlay {
                                ProgressView()
                            }
                    }
                    .tag(index)
                }
            }
            .tabViewStyle(.page(indexDisplayMode: .never))
            .frame(width: imageWidth, height: imageHeight)
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .stroke(AppColors.accent(for: colorScheme).opacity(0.12), lineWidth: 1)
            )

            LinearGradient(
                colors: [
                    .clear,
                    Color.black.opacity(0.24)
                ],
                startPoint: .center,
                endPoint: .bottom
            )
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))

            if safeImageURLs.count > 1 {
                HStack(spacing: 4) {
                    ForEach(safeImageURLs.indices, id: \.self) { index in
                        Capsule(style: .continuous)
                            .fill(index == selectedImageIndex ? Color.white : Color.white.opacity(0.42))
                            .frame(width: index == selectedImageIndex ? 12 : 5, height: 5)
                    }
                }
                .padding(.bottom, 8)
            }

            Button {
                showingFullscreenGallery = true
            } label: {
                Image(systemName: "arrow.up.left.and.arrow.down.right")
                    .font(.caption2.weight(.bold))
                    .foregroundColor(.white)
                    .padding(7)
                    .background(Color.black.opacity(0.32))
                    .clipShape(Circle())
            }
            .buttonStyle(.plain)
            .skydownTactileAction()
            .accessibilityIdentifier("shop.merch.fullscreen.open")
            .frame(width: imageWidth, height: imageHeight, alignment: .topTrailing)
            .padding(7)
        }
        .frame(width: imageWidth, height: imageHeight)
    }

    private var badgeStrip: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                MerchInfoBadge(
                    text: item.available ? "Drop live" : "Sold out",
                    colorScheme: colorScheme,
                    isAccent: item.available
                )
                if item.hasCuratedMerchCategory {
                    MerchInfoBadge(
                        text: item.merchCategoryTitle,
                        colorScheme: colorScheme,
                        isAccent: false
                    )
                }
                if safeImageURLs.count > 1 {
                    MerchInfoBadge(
                        text: "\(safeImageURLs.count) Bilder",
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
        }
    }
}

private struct MerchandiseFullscreenGalleryView: View {
    let itemName: String
    let imageURLs: [String]
    @Binding var selectedImageIndex: Int
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ZStack {
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
        }
        .tabViewStyle(.page(indexDisplayMode: .automatic))
        .safeAreaInset(edge: .bottom, spacing: 0) {
            HStack {
                Text(itemName)
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(.white)
                    .lineLimit(1)

                Spacer()

                Text("\(selectedImageIndex + 1) von \(max(imageURLs.count, 1))")
                    .font(.subheadline)
                    .foregroundColor(.white.opacity(0.76))
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
            .background(Color.black.opacity(0.72))
        }
        .accessibilityIdentifier("shop.merch.fullscreen.root")
        .toolbar(.hidden, for: .tabBar)
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
        SkydownMetaLabel(
            text: text,
            tint: isAccent
                ? AppColors.accent(for: colorScheme)
                : AppColors.secondaryText(for: colorScheme)
        )
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
