//
//  MerchandiseItemView.swift
//  Skydown App
//
//  Created by Yang D. Nash on 24.07.25.
//

import SwiftUI

struct MerchandiseItemView: View {
    let item: MerchandiseItem
    var shelfSpotlight: Bool = false
    var shelfSettled: Bool = false
    @Environment(\.colorScheme) private var colorScheme
    @State private var selectedImageIndex = 0
    @State private var showingFullscreenGallery = false
    private var imageWidth: CGFloat {
        if shelfSpotlight { return 132 }
        if shelfSettled { return 108 }
        return 112
    }
    private var imageHeight: CGFloat {
        if shelfSpotlight { return 168 }
        if shelfSettled { return 128 }
        return 132
    }
    private var rowHSpacing: CGFloat { shelfSettled ? 8 : 12 }
    private var imageCorner: CGFloat { shelfSpotlight ? 22 : (shelfSettled ? 16 : 18) }

    private var displayImageURLs: [String] {
        if let customOverride = item.customImageOverride.takeIfNotBlank() {
            return [customOverride] + item.imageURLs.filter { $0 != customOverride }
        }
        return item.imageURLs
    }

    private var safeImageURLs: [String] {
        displayImageURLs.isEmpty ? [""] : displayImageURLs
    }

    /// Featured drops use aurora highlight; core catalog uses primary slate accent.
    private var priceAccentColor: Color {
        item.featured
            ? AppColors.accentHighlight(for: colorScheme)
            : AppColors.accent(for: colorScheme)
    }

    var body: some View {
        HStack(alignment: .top, spacing: rowHSpacing) {
            imagePager

            VStack(alignment: .leading, spacing: 8) {
                badgeStrip

                Text(item.name)
                    .font(AppTypography.listTitle)
                    .foregroundColor(AppColors.text(for: colorScheme))
                    .lineLimit(2)
                    .fixedSize(horizontal: false, vertical: true)

                Text("\(item.currency) \(item.price, specifier: "%.2f")")
                    .font(AppTypography.metricLabel)
                    .foregroundColor(priceAccentColor)

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
                        .foregroundColor(priceAccentColor)
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
                    MerchCatalogThumbImage(
                        urlString: urlString,
                        colorScheme: colorScheme,
                        width: imageWidth,
                        height: imageHeight
                    )
                    .id("\(String(describing: item.id))-\(index)-\(urlString)")
                    .tag(index)
                }
            }
            .tabViewStyle(.page(indexDisplayMode: .never))
            .frame(width: imageWidth, height: imageHeight)
            .clipShape(RoundedRectangle(cornerRadius: imageCorner, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: imageCorner, style: .continuous)
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
            .clipShape(RoundedRectangle(cornerRadius: imageCorner, style: .continuous))

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
                    isAccent: item.available,
                    featured: item.featured
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

private func merchImageURL(_ raw: String) -> URL? {
    let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
    guard !trimmed.isEmpty, let url = URL(string: trimmed) else { return nil }
    return url
}

/// List / card thumbnail: no implicit crossfade, static placeholder (no spinner) for calmer scroll.
private struct MerchCatalogThumbImage: View {
    let urlString: String
    let colorScheme: ColorScheme
    let width: CGFloat
    let height: CGFloat

    var body: some View {
        Group {
            if let url = merchImageURL(urlString) {
                AsyncImage(url: url, transaction: Transaction(animation: nil)) { phase in
                    switch phase {
                    case .empty:
                        thumbPlaceholder
                    case .success(let image):
                        image
                            .resizable()
                            .aspectRatio(contentMode: .fill)
                            .frame(width: width, height: height)
                            .clipped()
                    case .failure:
                        thumbPlaceholder
                    @unknown default:
                        thumbPlaceholder
                    }
                }
            } else {
                thumbPlaceholder
            }
        }
        .transaction { $0.animation = nil }
    }

    private var thumbPlaceholder: some View {
        RoundedRectangle(cornerRadius: 18, style: .continuous)
            .fill(AppColors.secondaryBackground(for: colorScheme))
            .overlay {
                Image(systemName: "photo")
                    .font(.title3)
                    .foregroundStyle(AppColors.secondaryText(for: colorScheme).opacity(0.35))
            }
            .frame(width: width, height: height)
    }
}

/// Fullscreen gallery: same no-fade policy; light spinner only while loading large asset.
private struct MerchGalleryImage: View {
    let urlString: String

    var body: some View {
        Group {
            if let url = merchImageURL(urlString) {
                AsyncImage(url: url, transaction: Transaction(animation: nil)) { phase in
                    switch phase {
                    case .empty:
                        ProgressView()
                            .tint(.white)
                            .scaleEffect(0.9)
                    case .success(let image):
                        image
                            .resizable()
                            .scaledToFit()
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 40)
                    case .failure:
                        galleryPlaceholder
                    @unknown default:
                        galleryPlaceholder
                    }
                }
            } else {
                galleryPlaceholder
            }
        }
        .transaction { $0.animation = nil }
    }

    private var galleryPlaceholder: some View {
        Image(systemName: "photo")
            .font(.largeTitle)
            .foregroundStyle(.white.opacity(0.35))
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

                        MerchGalleryImage(urlString: urlString)
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
    var featured: Bool = false

    var body: some View {
        SkydownMetaLabel(
            text: text,
            tint: isAccent
                ? (featured
                    ? AppColors.accentHighlight(for: colorScheme)
                    : AppColors.accent(for: colorScheme))
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
