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
                Text("Collection Sidebar")
                    .font(.headline)
                    .foregroundColor(AppColors.text(for: colorScheme))

                Text("Collections")
                    .font(.footnote)
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
        .frame(minHeight: 170)
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

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .top, spacing: 12) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(selectedLane.title)
                        .font(.title3.weight(.bold))
                        .foregroundColor(AppColors.text(for: colorScheme))

                    Text(selectedLane.subtitle)
                        .font(.footnote)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }

                Spacer(minLength: 0)

                VStack(alignment: .trailing, spacing: 4) {
                    Text(laneLabel)
                        .font(.caption.weight(.bold))
                        .foregroundColor(AppColors.accentHighlight(for: colorScheme))

                    Text(totalItemCount == selectedLane.itemCount ? "Gesamt" : "Auswahl")
                        .font(.caption2.weight(.semibold))
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }
            }

            HStack(spacing: 8) {
                MerchandiseCollabMetaPill(
                    text: laneLabel,
                    colorScheme: colorScheme,
                    accent: AppColors.accent(for: colorScheme)
                )
                MerchandiseCollabMetaPill(
                    text: selectedLane.id == MerchandiseCollabLane.allID
                        ? "Alle"
                        : (selectedLane.isCoreLane ? "Core" : "Collection"),
                    colorScheme: colorScheme,
                    accent: AppColors.accentMystic(for: colorScheme)
                )
            }
        }
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
                            .font(.subheadline.weight(.bold))
                            .foregroundColor(isSelected ? .white : AppColors.text(for: colorScheme))
                            .multilineTextAlignment(.leading)

                        Text(lane.subtitle)
                            .font(.caption)
                            .foregroundColor(
                                isSelected
                                    ? Color.white.opacity(0.84)
                                    : AppColors.secondaryText(for: colorScheme)
                            )
                            .multilineTextAlignment(.leading)
                            .lineLimit(2)
                    }

                    Spacer(minLength: 0)
                }

                HStack(spacing: 8) {
                    Text(lane.itemCount == 1 ? "1 Piece" : "\(lane.itemCount) Pieces")
                        .font(.caption.weight(.semibold))
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
                }
            }
            .padding(.horizontal, compact ? 14 : 16)
            .padding(.vertical, compact ? 14 : 15)
            .frame(maxWidth: .infinity, alignment: .leading)
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

private struct MerchandiseCollabMetaPill: View {
    let text: String
    let colorScheme: ColorScheme
    let accent: Color

    var body: some View {
        Text(text)
            .font(.caption.weight(.semibold))
            .foregroundColor(accent)
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(
                Capsule()
                    .fill(AppColors.secondaryBackground(for: colorScheme))
            )
    }
}
