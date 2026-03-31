import Foundation

@MainActor
final class MerchStoreStatusStore: ObservableObject {
    static let shared = MerchStoreStatusStore()

    @Published private(set) var status: MerchStoreStatus = .default
    @Published private(set) var lastErrorMessage: String?

    private let service: MerchStoreStatusServicing
    private var stopObserving: (() -> Void)?

    init(service: MerchStoreStatusServicing = FirestoreMerchStoreStatusService()) {
        self.service = service
        startObserving()
    }

    private func startObserving() {
        stopObserving?()
        stopObserving = service.observeStatus { [weak self] result in
            guard let self else { return }

            switch result {
            case .success(let status):
                self.status = status
                self.lastErrorMessage = nil
            case .failure(let error):
                self.lastErrorMessage = error.localizedDescription
            }
        }
    }

    deinit {
        stopObserving?()
    }
}
