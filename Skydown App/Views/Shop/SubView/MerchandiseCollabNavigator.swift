import SwiftUI

// MARK: - Merch copy (Android parity, presentation only)

private func shopMerchLocalizedLaneTitle(_ lane: MerchandiseCollabLane) -> String {
    if lane.id == MerchandiseCollabLane.allID {
        return AppLocalized.text("shop.lane.all_drops_title", fallback: "All pieces")
    }
    return lane.title
}

private func shopMerchLocalizedLaneBannerSubtitle(_ lane: MerchandiseCollabLane) -> String {
    if lane.id == MerchandiseCollabLane.allID {
        return AppLocalized.text("shop.lane.all_drops_subtitle", fallback: "Everything in the current catalog at a glance")
    }
    return lane.subtitle
}

private func shopMerchLocalizedPieces(_ count: Int) -> String {
    if count == 1 {
        return AppLocalized.text("shop.pieces.in_lane_one", fallback: "1 product")
    }
    return String(
        format: AppLocalized.text("shop.pieces.in_lane_other", fallback: "%d products"),
        count
    )
}

private func shopMerchLocalizedKind(_ lane: MerchandiseCollabLane) -> String {
    if lane.id == MerchandiseCollabLane.allID {
        return AppLocalized.text("shop.lane.kind.all", fallback: "All")
    }
    if lane.isCoreLane {
        return AppLocalized.text("shop.lane.kind.core", fallback: "House")
    }
    return AppLocalized.text("shop.lane.kind.collection", fallback: "Collection")
}

struct MerchandiseCollabLane: Identifiable, Equatable {
    static let allID = "all-drops"

    let id: String
    let title: String
    let subtitle: String
    let itemCount: Int
    let isCoreLane: Bool

    private struct LaneAggregate {
        var title: String
        var subtitle: String
        var isCoreLane: Bool
        var itemIDs: Set<String>
    }

    static func build(from items: [MerchandiseItem]) -> [MerchandiseCollabLane] {
        guard !items.isEmpty else {
            return [
                MerchandiseCollabLane(
                    id: allID,
                    title: "All Drops",
                    subtitle: "Alle Collabs und Core Pieces.",
                    itemCount: 0,
                    isCoreLane: false
                )
            ]
        }

        var aggregates: [String: LaneAggregate] = [:]

        for item in items {
            let laneIDs = item.laneMemberships.map(\.id)
            let uniqueLaneIDs = Array(NSOrderedSet(array: laneIDs)) as? [String] ?? laneIDs
            for membership in uniqueLaneIDs {
                var aggregate = aggregates[membership] ?? LaneAggregate(
                    title: item.titleForLane(id: membership),
                    subtitle: item.subtitleForLane(id: membership),
                    isCoreLane: item.isCoreLane(id: membership),
                    itemIDs: []
                )
                aggregate.itemIDs.insert(item.id ?? item.shopifyProductId ?? item.name)
                aggregates[membership] = aggregate
            }
        }

        let lanes = aggregates.map { laneID, aggregate in
            MerchandiseCollabLane(
                id: laneID,
                title: aggregate.title,
                subtitle: aggregate.subtitle,
                itemCount: aggregate.itemIDs.count,
                isCoreLane: aggregate.isCoreLane
            )
        }
        .sorted { lhs, rhs in
            if lhs.isCoreLane != rhs.isCoreLane {
                return !lhs.isCoreLane && rhs.isCoreLane
            }
            if lhs.itemCount != rhs.itemCount {
                return lhs.itemCount > rhs.itemCount
            }
            return lhs.title.localizedCaseInsensitiveCompare(rhs.title) == .orderedAscending
        }

        return [
            MerchandiseCollabLane(
                id: allID,
                title: "All Drops",
                subtitle: "Alle Collabs und Core Pieces.",
                itemCount: items.count,
                isCoreLane: false
            )
        ] + lanes
    }
}

extension MerchandiseItem {
    var laneMemberships: [(id: String, type: String)] {
        let normalizedHandles = shopifyCollectionHandles
            .compactMap { $0.trimmingCharacters(in: .whitespacesAndNewlines).lowercased().nilIfEmpty }
        if normalizedHandles.isEmpty == false {
            return normalizedHandles.map { ("collection:\($0)", "collection") }
        }
        return [(merchCategoryKey, "category")]
    }

    func belongsToLane(id laneID: String) -> Bool {
        laneMemberships.contains { $0.id == laneID }
    }

    func titleForLane(id laneID: String) -> String {
        if laneID.hasPrefix("collection:") {
            return laneID.replacingOccurrences(of: "collection:", with: "").prettifiedCollectionHandle
        }
        return merchCategoryTitle
    }

    func subtitleForLane(id laneID: String) -> String {
        if laneID.hasPrefix("collection:") {
            return "Shopify Collection"
        }
        return merchCategorySubtitle
    }

    func isCoreLane(id laneID: String) -> Bool {
        if laneID.hasPrefix("collection:") {
            return false
        }
        return !hasCuratedMerchCategory
    }
}

private extension String {
    var nilIfEmpty: String? {
        isEmpty ? nil : self
    }

    var prettifiedCollectionHandle: String {
        split(separator: "-")
            .filter { !$0.isEmpty }
            .map {
                let value = String($0)
                return value.prefix(1).uppercased() + value.dropFirst()
            }
            .joined(separator: " ")
    }
}

struct MerchandiseCollabSidebar: View {
    let lanes: [MerchandiseCollabLane]
    let selectedLaneID: String
    let colorScheme: ColorScheme
    let onSelect: (MerchandiseCollabLane) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingRelaxed) {
            HStack(alignment: .top, spacing: SkydownLayout.stackSpacingMicro) {
                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingNano) {
                    Text(AppLocalized.text("shop.map.title", fallback: "Collections map"))
                        .font(AppTypography.sectionHeadline)
                        .foregroundColor(AppColors.text(for: colorScheme))
                    Text(AppLocalized.text("shop.map.subtitle", fallback: "Core lines, collabs, and imported collections in one calm list."))
                        .font(AppTypography.bodyCaption)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }
                Spacer(minLength: 0)
                Text(AppLocalized.text("shop.map.tag", fallback: "MAP"))
                    .font(.caption2.weight(.bold))
                    .foregroundColor(AppColors.accentHighlight(for: colorScheme))
                    .padding(.horizontal, 7)
                    .padding(.vertical, 4)
                    .background(
                        Capsule(style: .continuous)
                            .fill(AppColors.accentHighlight(for: colorScheme).opacity(0.12))
                    )
            }

            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
                ForEach(lanes) { lane in
                    MerchandiseCollabSidebarButton(
                        lane: lane,
                        isSelected: lane.id == selectedLaneID,
                        colorScheme: colorScheme
                    ) {
                        onSelect(lane)
                    }
                }
            }
        }
        .padding(SkydownLayout.cardPadding)
        .skydownPanelSurface(colorScheme: colorScheme, accent: AppColors.accentHighlight(for: colorScheme))
    }
}

struct MerchandiseCollabQuickGrid: View {
    let lanes: [MerchandiseCollabLane]
    let selectedLaneID: String
    let colorScheme: ColorScheme
    let onSelect: (MerchandiseCollabLane) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
            HStack(alignment: .top, spacing: SkydownLayout.stackSpacingMicro) {
                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingNano) {
                    Text(AppLocalized.text("shop.quick.title", fallback: "Quick access"))
                        .font(AppTypography.sectionHeadline)
                        .foregroundColor(AppColors.text(for: colorScheme))
                    Text(AppLocalized.text("shop.quick.subtitle", fallback: "Jump to a collection when you already know the lane."))
                        .font(AppTypography.bodyCaption)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }
                Spacer(minLength: 0)
                Text(AppLocalized.text("shop.quick.tag", fallback: "LIST"))
                    .font(.caption2.weight(.bold))
                    .foregroundColor(AppColors.accentHighlight(for: colorScheme))
                    .padding(.horizontal, 7)
                    .padding(.vertical, 4)
                    .background(
                        Capsule(style: .continuous)
                            .fill(AppColors.accentHighlight(for: colorScheme).opacity(0.12))
                    )
            }

            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingMicro) {
                ForEach(lanes) { lane in
                    MerchandiseCollabSidebarButton(
                        lane: lane,
                        isSelected: lane.id == selectedLaneID,
                        colorScheme: colorScheme,
                        compact: true
                    ) {
                        onSelect(lane)
                    }
                }
            }
        }
        .padding(SkydownLayout.cardPadding)
        .skydownPanelSurface(colorScheme: colorScheme, accent: AppColors.accentHighlight(for: colorScheme))
    }
}

struct MerchandiseCollabCarousel: View {
    let lanes: [MerchandiseCollabLane]
    @Binding var selectedLaneID: String
    let totalItemCount: Int
    let colorScheme: ColorScheme
    @State private var selectedPage = 0

    private var safeLanes: [MerchandiseCollabLane] {
        if lanes.isEmpty {
            return [
                MerchandiseCollabLane(
                    id: MerchandiseCollabLane.allID,
                    title: "All Drops",
                    subtitle: "Alle Collabs und Core Pieces.",
                    itemCount: totalItemCount,
                    isCoreLane: false
                )
            ]
        }

        return lanes
    }

    private var resolvedPage: Int {
        safeLanes.firstIndex { $0.id == selectedLaneID } ?? 0
    }

    var body: some View {
        let indexStyle = PageTabViewStyle(
            indexDisplayMode: safeLanes.count > 1 ? .automatic : .never
        )

        TabView(selection: $selectedPage) {
            ForEach(Array(safeLanes.enumerated()), id: \.element.id) { index, lane in
                MerchandiseCollabSelectionCard(
                    selectedLane: lane,
                    totalItemCount: totalItemCount,
                    colorScheme: colorScheme
                )
                .tag(index)
                .padding(.horizontal, 2)
            }
        }
        .frame(maxWidth: .infinity)
        .frame(height: 224)
        .tabViewStyle(indexStyle)
        .indexViewStyle(
            PageIndexViewStyle(backgroundDisplayMode: .always)
        )
        .onAppear {
            selectedPage = resolvedPage
        }
        .onChange(of: selectedLaneID) { _, _ in
            let targetPage = resolvedPage
            if selectedPage != targetPage {
                selectedPage = targetPage
            }
        }
        .onChange(of: safeLanes.map(\.id).joined(separator: "|")) { _, _ in
            let targetPage = resolvedPage
            selectedPage = targetPage
            selectedLaneID = safeLanes[targetPage].id
        }
        .onChange(of: selectedPage) { _, page in
            guard safeLanes.indices.contains(page) else { return }
            let laneID = safeLanes[page].id
            if selectedLaneID != laneID {
                selectedLaneID = laneID
            }
        }
    }
}

struct MerchandiseCollabSelectionCard: View {
    let selectedLane: MerchandiseCollabLane
    let totalItemCount: Int
    let colorScheme: ColorScheme

    private var laneLabel: String { shopMerchLocalizedPieces(selectedLane.itemCount) }

    private var laneKindLabel: String { shopMerchLocalizedKind(selectedLane) }

    private var coverageLabel: String {
        guard totalItemCount > 0 else { return "0%" }
        let ratio = Double(selectedLane.itemCount) / Double(totalItemCount)
        return "\(Int((ratio * 100).rounded()))%"
    }

    private var focusDetail: String {
        if selectedLane.id == MerchandiseCollabLane.allID {
            return AppLocalized.text("shop.lane.focus_all", fallback: "Scroll the picks above, then browse everything here.")
        }
        return String(
            format: AppLocalized.text(
                "shop.lane.focus_named",
                fallback: "Narrowing the shelf to “%@” and keeping the rest out of the way."
            ),
            shopMerchLocalizedLaneTitle(selectedLane)
        )
    }

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingCompact) {
            HStack(alignment: .top, spacing: SkydownLayout.stackSpacingPill) {
                Image(systemName: selectedLane.id == MerchandiseCollabLane.allID ? "bag" : "person.2")
                    .font(.caption.weight(.semibold))
                    .foregroundColor(AppColors.accent(for: colorScheme))
                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingTick) {
                    Text(shopMerchLocalizedLaneTitle(selectedLane))
                        .font(AppTypography.cardTitle)
                        .foregroundColor(AppColors.text(for: colorScheme))
                        .lineLimit(2)
                    Text(
                        selectedLane.id == MerchandiseCollabLane.allID
                            ? AppLocalized.text("shop.lane.all_drops_subtitle", fallback: "Everything in the current catalog at a glance")
                            : shopMerchLocalizedLaneBannerSubtitle(selectedLane)
                    )
                    .font(AppTypography.bodyCaption)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    .lineLimit(2)
                }
                Spacer(minLength: 0)
                Text(laneLabel)
                    .font(.caption2.weight(.bold))
                    .foregroundColor(AppColors.accentHighlight(for: colorScheme))
                    .padding(.horizontal, 7)
                    .padding(.vertical, 4)
                    .background(
                        Capsule(style: .continuous)
                            .fill(AppColors.accentHighlight(for: colorScheme).opacity(0.12))
                    )
            }

            Text(focusDetail)
                .font(AppTypography.body)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: SkydownLayout.stackSpacingPill) {
                    MerchandiseCollabFocusMetric(
                        title: AppLocalized.text("shop.lane.metric.type", fallback: "Type"),
                        value: laneKindLabel,
                        detail: shopMerchLocalizedLaneBannerSubtitle(selectedLane),
                        colorScheme: colorScheme,
                        accent: AppColors.accentMystic(for: colorScheme)
                    )
                    MerchandiseCollabFocusMetric(
                        title: AppLocalized.text("shop.lane.metric.coverage", fallback: "Coverage"),
                        value: coverageLabel,
                        detail: totalItemCount == selectedLane.itemCount
                            ? AppLocalized.text("shop.lane.coverage.detail_full", fallback: "of the full shelf")
                            : AppLocalized.text("shop.lane.coverage.detail_partial", fallback: "of the visible shelf"),
                        colorScheme: colorScheme,
                        accent: AppColors.accentHighlight(for: colorScheme)
                    )
                    MerchandiseCollabFocusMetric(
                        title: AppLocalized.text("shop.lane.metric.count", fallback: "Products"),
                        value: laneLabel,
                        detail: totalItemCount == selectedLane.itemCount
                            ? AppLocalized.text("shop.lane.count.all", fallback: "Full run")
                            : AppLocalized.text("shop.lane.count.filter", fallback: "In this view"),
                        colorScheme: colorScheme,
                        accent: AppColors.accent(for: colorScheme)
                    )
                }
            }
        }
        .frame(minHeight: 160, alignment: .topLeading)
        .padding(SkydownLayout.cardPadding)
        .skydownPanelSurface(colorScheme: colorScheme, accent: AppColors.accent(for: colorScheme))
    }
}

private struct MerchandiseCollabSidebarButton: View {
    let lane: MerchandiseCollabLane
    let isSelected: Bool
    let colorScheme: ColorScheme
    var compact: Bool = false
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: compact ? 8 : 10) {
                HStack(alignment: .top, spacing: SkydownLayout.stackSpacingPill) {
                    Circle()
                        .fill(
                            isSelected
                                ? Color.white.opacity(0.92)
                                : AppColors.accentHighlight(for: colorScheme).opacity(0.16)
                        )
                        .frame(width: 12, height: 12)

                    VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingNano) {
                        Text(shopMerchLocalizedLaneTitle(lane))
                            .font(AppTypography.buttonLabel)
                            .foregroundColor(isSelected ? .white : AppColors.text(for: colorScheme))
                            .multilineTextAlignment(.leading)
                            .lineLimit(compact ? 1 : 2)
                            .minimumScaleFactor(0.86)

                        Text(shopMerchLocalizedLaneBannerSubtitle(lane))
                            .font(AppTypography.bodyCaption)
                            .foregroundColor(
                                isSelected
                                    ? Color.white.opacity(0.84)
                                    : AppColors.secondaryText(for: colorScheme)
                            )
                            .multilineTextAlignment(.leading)
                            .lineLimit(compact ? 1 : 2)
                    }

                    Spacer(minLength: 0)
                }

                HStack(spacing: SkydownLayout.stackSpacingMicro) {
                    Text(shopMerchLocalizedPieces(lane.itemCount))
                        .font(AppTypography.bodyCaption)
                        .foregroundColor(isSelected ? .white : AppColors.accentHighlight(for: colorScheme))
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(
                            Capsule()
                                .fill(
                                    isSelected
                                        ? Color.white.opacity(0.14)
                                        : AppColors.secondaryBackground(for: colorScheme)
                                )
                        )

                    Text(shopMerchLocalizedKind(lane))
                    .font(AppTypography.bodyCaption)
                    .foregroundColor(isSelected ? .white.opacity(0.92) : AppColors.text(for: colorScheme))
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(
                        Capsule()
                            .fill(
                                isSelected
                                    ? Color.white.opacity(0.10)
                                    : AppColors.secondaryBackground(for: colorScheme)
                            )
                    )
                }
            }
            .padding(.horizontal, compact ? SkydownLayout.compactRadius : SkydownLayout.denseRadius)
            .padding(.vertical, compact ? 14 : 15)
            .frame(maxWidth: .infinity, alignment: .leading)
            .frame(height: compact ? 114 : 126, alignment: .topLeading)
            .background(
                RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous)
                    .fill(
                        isSelected
                            ? AppColors.accent(for: colorScheme)
                            : AppColors.secondaryBackground(for: colorScheme)
                    )
            )
            .overlay(
                RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous)
                    .stroke(
                        isSelected
                            ? AppColors.accentHighlight(for: colorScheme)
                            : AppColors.accent(for: colorScheme).opacity(0.12),
                        lineWidth: 1
                    )
            )
        }
        .buttonStyle(.plain)
        .skydownTactileAction()
    }
}

private struct MerchandiseCollabFocusMetric: View {
    let title: String
    let value: String
    let detail: String
    let colorScheme: ColorScheme
    let accent: Color

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingSubtle) {
            Text(title.uppercased())
                .font(AppTypography.sectionEyebrow)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            Text(value)
                .font(AppTypography.metricLabel)
                .foregroundColor(AppColors.text(for: colorScheme))
                .lineLimit(1)

            Text(detail)
                .font(AppTypography.bodyCaption)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
                .lineLimit(1)
        }
        .frame(width: 136, alignment: .leading)
        .padding(.horizontal, SkydownLayout.tightRadius)
        .padding(.vertical, 11)
        .background(
            RoundedRectangle(cornerRadius: SkydownLayout.messageBubbleRadius, style: .continuous)
                .fill(AppColors.secondaryBackground(for: colorScheme))
        )
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.messageBubbleRadius, style: .continuous)
                .stroke(accent.opacity(0.16), lineWidth: 1)
        )
    }
}

private struct MerchandiseCollabMetaPill: View {
    let text: String
    let colorScheme: ColorScheme
    let accent: Color

    var body: some View {
        Text(text)
            .font(AppTypography.bodyCaption)
            .foregroundColor(accent)
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(
                Capsule()
                    .fill(AppColors.secondaryBackground(for: colorScheme))
            )
    }
}
