import SwiftUI

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
        if (shopifyProductId?.isEmpty == false), normalizedHandles.isEmpty == false {
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
        VStack(alignment: .leading, spacing: 14) {
            VStack(alignment: .leading, spacing: 6) {
                Text("Drop Map")
                    .font(AppTypography.sectionHeadline)
                    .foregroundColor(AppColors.text(for: colorScheme))

                Text("Alle Collections, Collabs und Core Pieces auf einen Blick.")
                    .font(AppTypography.bodyCaption)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }

            VStack(alignment: .leading, spacing: 10) {
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

struct MerchandiseCollabRail: View {
    let lanes: [MerchandiseCollabLane]
    let selectedLaneID: String
    let colorScheme: ColorScheme
    let onSelect: (MerchandiseCollabLane) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 10) {
                ForEach(lanes) { lane in
                    MerchandiseCollabSidebarButton(
                        lane: lane,
                        isSelected: lane.id == selectedLaneID,
                        colorScheme: colorScheme,
                        compact: true
                    ) {
                        onSelect(lane)
                    }
                    .frame(width: 214)
                }
            }
            .padding(.horizontal, 1)
        }
    }
}

struct MerchandiseCollabQuickGrid: View {
    let lanes: [MerchandiseCollabLane]
    let selectedLaneID: String
    let colorScheme: ColorScheme
    let onSelect: (MerchandiseCollabLane) -> Void

    private let columns = [
        GridItem(.flexible(), spacing: 12),
        GridItem(.flexible(), spacing: 12)
    ]

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            VStack(alignment: .leading, spacing: 5) {
                Text("Direktwahl")
                    .font(AppTypography.sectionHeadline)
                    .foregroundColor(AppColors.text(for: colorScheme))

                Text("Neben dem Swipe kannst du jede Collection direkt antippen.")
                    .font(AppTypography.bodyCaption)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }

            LazyVGrid(columns: columns, alignment: .leading, spacing: 12) {
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

    private var laneLabel: String {
        selectedLane.itemCount == 1 ? "1 Piece" : "\(selectedLane.itemCount) Pieces"
    }

    private var laneKindLabel: String {
        if selectedLane.id == MerchandiseCollabLane.allID {
            return "Alle"
        }
        return selectedLane.isCoreLane ? "Core" : "Collection"
    }

    private var coverageLabel: String {
        guard totalItemCount > 0 else { return "0%" }
        let ratio = Double(selectedLane.itemCount) / Double(totalItemCount)
        return "\(Int((ratio * 100).rounded()))%"
    }

    private var coverageDetail: String {
        totalItemCount == selectedLane.itemCount ? "Gesamter Katalog" : "des sichtbaren Katalogs"
    }

    private var focusTitle: String {
        selectedLane.id == MerchandiseCollabLane.allID ? "Alle Drops im Fokus" : "Fokus auf \(selectedLane.title)"
    }

    private var focusDetail: String {
        if selectedLane.id == MerchandiseCollabLane.allID {
            return "Direkt durch alle sichtbaren Pieces browsen."
        }
        return "Filtert direkt auf diese Lane und haelt den Rest ruhig im Hintergrund."
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(alignment: .top, spacing: 12) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Drop Map")
                        .font(AppTypography.sectionEyebrow)
                        .foregroundColor(AppColors.accentHighlight(for: colorScheme))

                    Text(selectedLane.title)
                        .font(AppTypography.cardTitle)
                        .foregroundColor(AppColors.text(for: colorScheme))
                        .lineLimit(2)

                    Text(focusTitle)
                        .font(AppTypography.bodyCaption)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        .lineLimit(2)
                }

                Spacer(minLength: 0)

                VStack(alignment: .trailing, spacing: 4) {
                    Text(laneLabel)
                        .font(AppTypography.sectionEyebrow)
                        .foregroundColor(AppColors.accentHighlight(for: colorScheme))

                    Text(coverageLabel)
                        .font(AppTypography.bodyCaption)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }
            }

            Text(focusDetail)
                .font(AppTypography.body)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    MerchandiseCollabFocusMetric(
                        title: "Route",
                        value: laneKindLabel,
                        detail: selectedLane.subtitle,
                        colorScheme: colorScheme,
                        accent: AppColors.accentMystic(for: colorScheme)
                    )
                    MerchandiseCollabFocusMetric(
                        title: "Share",
                        value: coverageLabel,
                        detail: coverageDetail,
                        colorScheme: colorScheme,
                        accent: AppColors.accentHighlight(for: colorScheme)
                    )
                    MerchandiseCollabFocusMetric(
                        title: "Pieces",
                        value: laneLabel,
                        detail: totalItemCount == selectedLane.itemCount ? "Gesamtauswahl" : "Aktiver Filter",
                        colorScheme: colorScheme,
                        accent: AppColors.accent(for: colorScheme)
                    )
                }
            }
        }
        .frame(height: 186, alignment: .topLeading)
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
                HStack(alignment: .top, spacing: 10) {
                    Circle()
                        .fill(
                            isSelected
                                ? Color.white.opacity(0.92)
                                : AppColors.accentHighlight(for: colorScheme).opacity(0.16)
                        )
                        .frame(width: 12, height: 12)

                    VStack(alignment: .leading, spacing: 4) {
                        Text(lane.title)
                            .font(AppTypography.buttonLabel)
                            .foregroundColor(isSelected ? .white : AppColors.text(for: colorScheme))
                            .multilineTextAlignment(.leading)
                            .lineLimit(compact ? 1 : 2)
                            .minimumScaleFactor(0.86)

                        Text(lane.subtitle)
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

                HStack(spacing: 8) {
                    Text(lane.itemCount == 1 ? "1 Piece" : "\(lane.itemCount) Pieces")
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

                    Text(
                        lane.id == MerchandiseCollabLane.allID
                            ? "Alle"
                            : (lane.isCoreLane ? "Core" : "Collection")
                    )
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
            .padding(.horizontal, compact ? 14 : 16)
            .padding(.vertical, compact ? 14 : 15)
            .frame(maxWidth: .infinity, alignment: .leading)
            .frame(height: compact ? 114 : 126, alignment: .topLeading)
            .background(
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .fill(
                        isSelected
                            ? AppColors.accent(for: colorScheme)
                            : AppColors.secondaryBackground(for: colorScheme)
                    )
            )
            .overlay(
                RoundedRectangle(cornerRadius: 20, style: .continuous)
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
        VStack(alignment: .leading, spacing: 5) {
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
        .padding(.horizontal, 12)
        .padding(.vertical, 11)
        .background(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .fill(AppColors.secondaryBackground(for: colorScheme))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
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
