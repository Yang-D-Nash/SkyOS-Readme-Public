import Foundation

struct SkydownQueuedPresentation<Item: Equatable> {
    private enum Phase: Equatable {
        case idle
        case presented(Item)
        case dismissing(Item)
    }

    private var phase: Phase = .idle
    private var pendingItems: [Item] = []

    var activeItem: Item? {
        switch phase {
        case .presented(let item):
            return item
        case .idle, .dismissing:
            return nil
        }
    }

    mutating func request(_ item: Item) {
        guard !contains(item) else { return }

        switch phase {
        case .idle:
            phase = .presented(item)
        case .presented(let current):
            pendingItems.append(item)
            phase = .dismissing(current)
        case .dismissing:
            pendingItems.append(item)
        }
    }

    mutating func updatePresentedItem(_ item: Item?) {
        switch (phase, item) {
        case (.idle, .none):
            break
        case (.idle, .some(let item)):
            phase = .presented(item)
        case (.presented(let current), .some(let item)):
            if item != current {
                phase = .presented(item)
            }
        case (.presented, .none), (.dismissing, .none):
            promoteNextIfNeeded()
        case (.dismissing, .some(let item)):
            phase = .presented(item)
        }
    }

    private mutating func promoteNextIfNeeded() {
        guard !pendingItems.isEmpty else {
            phase = .idle
            return
        }

        let nextItem = pendingItems.removeFirst()
        phase = .presented(nextItem)
    }

    private func contains(_ item: Item) -> Bool {
        switch phase {
        case .idle:
            return pendingItems.contains(item)
        case .presented(let current), .dismissing(let current):
            return current == item || pendingItems.contains(item)
        }
    }
}
