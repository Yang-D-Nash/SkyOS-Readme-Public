//
//  MerchandiseViewModel.swift
//  Skydown App
//
//  Created by Yang D. Nash on 23.07.25.
//

import Foundation
import UserNotifications
import FirebaseFirestore
import FirebaseAuth
import UserNotifications

@MainActor
final class MerchandiseViewModel: ObservableObject {
    @Published var merchandiseItems: [MerchandiseItem] = []
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var isStoreOpen = true
    @Published var isUpdatingStoreState = false
    @Published var isSyncingCatalog = false

    // Toast
    @Published var toastMessage = ""
    @Published var showToast = false
    @Published var toastStyle: ToastStyle = .info
    private let merchandiseService: MerchandiseServicing
    private let merchStoreStatusService: MerchStoreStatusServicing
    private let shopifySyncService: ShopifyMerchSyncServicing
    private let publicShopifyCatalogService: PublicShopifyCatalogServicing
    private let authManager: AuthManager
    private var stopObservingItems: (() -> Void)?
    private var stopObservingStoreStatus: (() -> Void)?
    private var allItems: [MerchandiseItem] = []
    private var hasAttemptedAutomaticShopifySync = false

    private var canManageMerchandise: Bool {
        authManager.userSession?.isPlatformOwner == true
    }

    init(
        merchandiseService: MerchandiseServicing = FirebaseMerchandiseService(),
        merchStoreStatusService: MerchStoreStatusServicing = FirestoreMerchStoreStatusService(),
        shopifySyncService: ShopifyMerchSyncServicing = FirebaseFunctionsShopifyMerchSyncService(),
        publicShopifyCatalogService: PublicShopifyCatalogServicing = PublicShopifyCatalogService(),
        authManager: AuthManager
    ) {
        self.merchandiseService = merchandiseService
        self.merchStoreStatusService = merchStoreStatusService
        self.shopifySyncService = shopifySyncService
        self.publicShopifyCatalogService = publicShopifyCatalogService
        self.authManager = authManager

        Task { [weak self] in
            for await _ in authManager.$userSession.values {
                guard let self else { return }
                self.merchandiseItems = self.filterVisibleItems(self.allItems)
            }
        }
    }

    func fetchData() {
        isLoading = true
        errorMessage = nil
        if canManageMerchandise && allItems.isEmpty {
            hasAttemptedAutomaticShopifySync = false
        }

        stopObservingItems?()
        observeStoreStatusIfNeeded()

        stopObservingItems = merchandiseService.observeItems { [weak self] result in
            guard let self else { return }
            self.isLoading = false

            switch result {
            case .success(let items):
                self.errorMessage = nil
                self.allItems = items
                self.merchandiseItems = self.filterVisibleItems(items)
                if self.shouldLoadShopifyFallback(for: items) {
                    self.handleEmptyCatalogSnapshot()
                } else {
                    self.hasAttemptedAutomaticShopifySync = false
                }
            case .failure(let error):
                Task { @MainActor in
                    let loadedFallback = await self.loadPublicCatalogFallback(showToast: false)
                    if loadedFallback {
                        self.errorMessage = nil
                        return
                    }

                    skydownDebugLog("Dev Fehler fetchData:", error.localizedDescription)
                    self.errorMessage = Self.userFacingMerchandiseLoadError(error)
                    self.merchandiseItems = []
                    self.allItems = []
                }
            }
        }
    }

    func toggleStoreOpen() async {
        guard canManageMerchandise else {
            showUserToast("Nur der Owner darf den Merch Store schalten.", style: .error)
            return
        }

        isUpdatingStoreState = true
        defer { isUpdatingStoreState = false }

        do {
            let nextState = !isStoreOpen
            try await merchStoreStatusService.updateStoreOpen(nextState)
            showUserToast(nextState ? "Merch Store geoeffnet." : "Merch Store geschlossen.", style: .success)
        } catch {
            showUserToast(
                "Store-Status konnte nicht aktualisiert werden. Bitte später erneut.",
                style: .error
            )
        }
    }

    func syncShopifyCatalog(automatic: Bool = false) async {
        guard canManageMerchandise else {
            showUserToast("Nur der Owner darf den Shopify-Sync starten.", style: .error)
            return
        }

        isSyncingCatalog = true
        defer { isSyncingCatalog = false }

        do {
            let message = try await shopifySyncService.triggerSync()
            showUserToast(
                automatic ? "Shopify-Katalog wurde neu geladen." : message,
                style: .success
            )
            fetchData()
        } catch {
            let loadedFallback = await loadPublicCatalogFallback(showToast: automatic == false)
            if !loadedFallback {
                showUserToast(
                    "Shopify-Sync konnte nicht abgeschlossen werden. Bitte später erneut.",
                    style: .error
                )
            }
        }
    }

    private func showUserToast(_ message: String, style: ToastStyle) {
        toastMessage = message
        toastStyle = style
        showToast = true
    }

    private func observeStoreStatusIfNeeded() {
        guard stopObservingStoreStatus == nil else { return }

        stopObservingStoreStatus = merchStoreStatusService.observeStatus { [weak self] result in
            guard let self else { return }

            switch result {
            case .success(let status):
                self.isStoreOpen = status.isOpen
            case .failure(let error):
                skydownDebugLog("Merch store status observe:", error.localizedDescription)
                self.showUserToast(
                    "Store-Status konnte nicht geladen werden. Shop bleibt sichtbar.",
                    style: .error
                )
            }
        }
    }

    private func handleEmptyCatalogSnapshot() {
        Task {
            let loadedFallback = await loadPublicCatalogFallback(showToast: false)
            guard canManageMerchandise else { return }
            guard !isSyncingCatalog else { return }
            guard !hasAttemptedAutomaticShopifySync else { return }
            if loadedFallback {
                hasAttemptedAutomaticShopifySync = true
                return
            }

            hasAttemptedAutomaticShopifySync = true
            await syncShopifyCatalog(automatic: true)
        }
    }

    private func shouldLoadShopifyFallback(for items: [MerchandiseItem]) -> Bool {
        if items.isEmpty {
            return true
        }

        if canManageMerchandise {
            return merchandiseItems.isEmpty
        }

        return !hasVisibleShopifyItems(items)
    }

    private func hasVisibleShopifyItems(_ items: [MerchandiseItem]) -> Bool {
        items.contains { item in
            item.isVisibleInApp &&
                item.source == "shopify" &&
                item.shopifySyncActive &&
                item.shopifyProductId?.isEmpty == false
        }
    }

    private func loadPublicCatalogFallback(showToast: Bool) async -> Bool {
        do {
            let items = try await publicShopifyCatalogService.fetchCatalog()
            guard !items.isEmpty else { return false }

            allItems = items
            merchandiseItems = filterVisibleItems(items)
            if showToast {
                showUserToast("Shopify-Katalog direkt aus dem Store geladen.", style: .success)
            }
            return true
        } catch {
            return false
        }
    }

    private func filterVisibleItems(_ items: [MerchandiseItem]) -> [MerchandiseItem] {
        let activeItems = items.filter { item in
            item.source != "shopify" || item.shopifySyncActive
        }
        let visibleItems = activeItems.filter { canManageMerchandise || $0.isVisibleInApp }
        let hasVisibleShopifyItems = visibleItems.contains { item in
            item.source == "shopify" && item.shopifySyncActive && item.shopifyProductId?.isEmpty == false
        }

        let prioritizedItems: [MerchandiseItem]
        if canManageMerchandise {
            prioritizedItems = visibleItems
        } else if hasVisibleShopifyItems {
            prioritizedItems = visibleItems.filter { item in
                item.source == "shopify" && item.shopifySyncActive && item.shopifyProductId?.isEmpty == false
            }
        } else {
            prioritizedItems = visibleItems
        }

        return prioritizedItems.sorted {
            if $0.featured != $1.featured {
                return $0.featured && !$1.featured
            }
            if $0.source != $1.source {
                return $0.source == "shopify"
            }
            if $0.sortOrder != $1.sortOrder {
                return $0.sortOrder < $1.sortOrder
            }
            return $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending
        }
    }

    /// No raw `NSError` strings in UI — release-safe copy for shoppers.
    private static func userFacingMerchandiseLoadError(_ error: Error) -> String {
        let ns = error as NSError
        if ns.domain == NSURLErrorDomain {
            switch ns.code {
            case NSURLErrorNotConnectedToInternet, NSURLErrorNetworkConnectionLost, NSURLErrorTimedOut, -1009:
                return "Keine Verbindung. Bitte WLAN oder Mobilfunk prüfen und erneut versuchen."
            default:
                break
            }
        }
        let desc = ns.localizedDescription
        if desc.localizedCaseInsensitiveContains("permission")
            || desc.localizedCaseInsensitiveContains("insufficient") {
            return "Zugriff auf den Shop verweigert. Bitte neu anmelden oder es später erneut versuchen."
        }
        return "Der Shop konnte gerade nicht geladen werden. Bitte in Kürze erneut versuchen."
    }

    deinit {
        stopObservingItems?()
        stopObservingStoreStatus?()
    }
}

struct FeaturedHomeVideo: Identifiable {
    let id: String
    let title: String
    let projectName: String
    let notes: String
    let downloadURL: String
    let externalURL: String
    let embedURL: String
    let sourceProvider: String

    var provider: ExternalMediaProvider {
        ExternalMediaProvider(rawValueOrDefault: sourceProvider)
    }

    var nativePlaybackURLString: String {
        let trimmedDownloadURL = downloadURL.trimmingCharacters(in: .whitespacesAndNewlines)
        if !trimmedDownloadURL.isEmpty {
            return trimmedDownloadURL
        }

        let trimmedExternalURL = externalURL.trimmingCharacters(in: .whitespacesAndNewlines)
        return isDirectFeaturedHomeVideoURL(trimmedExternalURL) ? trimmedExternalURL : ""
    }

    var usesEmbeddedPreview: Bool {
        provider != .firebaseStorage && !embedURL.isEmpty && nativePlaybackURLString.isEmpty
    }

    var supportsInlinePlayback: Bool {
        usesEmbeddedPreview || !nativePlaybackURLString.isEmpty
    }

    var opensOriginalInApp: Bool {
        !openURLString.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    var originalDestinationDescription: String {
        if openURLString.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return AppLocalized.text("videohub.destination.no_original", fallback: "No original link available.")
        }

        if !nativePlaybackURLString.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return AppLocalized.text("videohub.destination.inline_player", fallback: "This clip starts directly in the in-app viewer.")
        }

        return AppLocalized.text("videohub.destination.web_view", fallback: "This link opens in an in-app web view with back and close.")
    }

    var openURLString: String {
        externalURL.isEmpty ? downloadURL : externalURL
    }
}

private func isDirectFeaturedHomeVideoURL(_ value: String) -> Bool {
    let trimmed = value.trimmingCharacters(in: .whitespacesAndNewlines)
    guard let url = URL(string: trimmed) else { return false }
    let path = url.path.lowercased()
    return path.hasSuffix(".mp4")
        || path.hasSuffix(".mov")
        || path.hasSuffix(".m4v")
        || path.hasSuffix(".webm")
        || path.hasSuffix(".m3u8")
}

@MainActor
final class HomeViewModel: ObservableObject {
    struct ProductivityReminder: Identifiable {
        let id: String
        let title: String
        let dueAt: Date?
        let isUpcoming: Bool
    }

    struct ProductivityTask: Identifiable {
        let id: String
        let title: String
        let dueAt: Date?
    }

    struct ProductivityNote: Identifiable {
        let id: String
        let title: String
        let updatedAt: Date?
    }

    @Published var featuredTrack: Track?
    @Published var featuredVideo: FeaturedHomeVideo?
    @Published var homeTrackMessage: String?
    @Published var homeVideoMessage: String?
    @Published var aiUsageWarning: String?
    @Published var creatorLimitZone = false
    @Published var agentRunning = false
    @Published var workflowWaiting = false
    @Published var commerceSignal: String?
    @Published var syncPaused = false
    @Published var recoverableError: String?
    @Published var newDataAvailable = false
    @Published var contentSignal: String?
    @Published var dueTodayReminders: [ProductivityReminder] = []
    @Published var upcomingReminders: [ProductivityReminder] = []
    @Published var openTasks: [ProductivityTask] = []
    @Published var recentNotes: [ProductivityNote] = []

    private let musicService: MusicServicing
    private let firestore = Firestore.firestore()
    private let featuredArtists = ["JANNO", "Yang D. Nash", "ThaDude", "MAVE", "TANGAJOE007"]
    private var refreshGeneration: UInt = 0
    private let noteRetentionTimeInterval: TimeInterval = 7 * 24 * 60 * 60

    init(musicService: MusicServicing = SpotifyMusicService()) {
        self.musicService = musicService
    }

    func refresh() {
        refreshGeneration &+= 1
        let generation = refreshGeneration
        Task {
            await withTaskGroup(of: HomeRefreshResult.self) { group in
                group.addTask { .track(await self.loadLatestTrack()) }
                group.addTask { .video(await self.loadLatestVideo()) }
                group.addTask { .signals(await self.loadRuntimeSignals()) }
                group.addTask { .productivity(await self.loadProductivitySnapshot()) }

                for await result in group {
                    guard generation == refreshGeneration else { continue }
                    switch result {
                    case .track(let track):
                        featuredTrack = track
                        homeTrackMessage = track == nil
                            ? AppLocalized.text(
                                "home.track.placeholder",
                                fallback: "New song soon."
                            )
                            : nil
                        contentSignal = buildContentSignal()
                    case .video(let video):
                        featuredVideo = video
                        homeVideoMessage = video == nil
                            ? AppLocalized.text(
                                "home.video.placeholder",
                                fallback: "New video soon."
                            )
                            : nil
                        contentSignal = buildContentSignal()
                    case .signals(let signals):
                        aiUsageWarning = signals.aiUsageWarning
                        creatorLimitZone = signals.creatorLimitZone
                        agentRunning = signals.agentRunning
                        workflowWaiting = signals.workflowWaiting
                        commerceSignal = signals.commerceSignal
                        syncPaused = signals.syncPaused
                        recoverableError = signals.recoverableError
                        newDataAvailable = signals.newDataAvailable
                        contentSignal = buildContentSignal()
                    case .productivity(let productivity):
                        dueTodayReminders = productivity.dueTodayReminders
                        upcomingReminders = productivity.upcomingReminders
                        openTasks = productivity.openTasks
                        recentNotes = productivity.recentNotes
                    }
                }
            }
        }
    }

    func createReminder(title: String, dueAt: Date) async throws {
        guard let uid = Auth.auth().currentUser?.uid else { return }
        let normalizedTitle = title.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !normalizedTitle.isEmpty else { return }
        let ref = firestore
            .collection("users")
            .document(uid)
            .collection("reminders")
            .document()
        try await ref.setData([
            "title": normalizedTitle,
            "scheduledAt": Timestamp(date: dueAt),
            "timezone": TimeZone.current.identifier,
            "status": "scheduled",
            "source": "manual",
            "createdAt": FieldValue.serverTimestamp(),
            "updatedAt": FieldValue.serverTimestamp()
        ])
        scheduleReminderLocalNotification(id: ref.documentID, title: normalizedTitle, at: dueAt)
        refresh()
    }

    func createTask(title: String, details: String, dueAt: Date? = nil) async throws {
        guard let uid = Auth.auth().currentUser?.uid else { return }
        let normalizedTitle = title.trimmingCharacters(in: .whitespacesAndNewlines)
        let normalizedDetails = details.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !normalizedTitle.isEmpty else { return }
        var payload: [String: Any] = [
            "title": normalizedTitle,
            "description": normalizedDetails,
            "status": "open",
            "priority": "normal",
            "source": "manual",
            "createdAt": FieldValue.serverTimestamp(),
            "updatedAt": FieldValue.serverTimestamp()
        ]
        if let dueAt {
            payload["dueAt"] = Timestamp(date: dueAt)
        }
        try await firestore
            .collection("users")
            .document(uid)
            .collection("tasks")
            .addDocument(data: payload)
        refresh()
    }

    func createNote(title: String, content: String) async throws {
        guard let uid = Auth.auth().currentUser?.uid else { return }
        let normalizedTitle = title.trimmingCharacters(in: .whitespacesAndNewlines)
        let normalizedContent = content.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !normalizedTitle.isEmpty || !normalizedContent.isEmpty else { return }
        let safeTitle = normalizedTitle.isEmpty ? "Untitled" : normalizedTitle
        let expiresAt = Date().addingTimeInterval(noteRetentionTimeInterval)
        try await firestore
            .collection("users")
            .document(uid)
            .collection("notes")
            .addDocument(data: [
                "title": safeTitle,
                "content": normalizedContent,
                "source": "manual",
                "expiresAt": Timestamp(date: expiresAt),
                "createdAt": FieldValue.serverTimestamp(),
                "updatedAt": FieldValue.serverTimestamp()
            ])
        refresh()
    }

    func updateReminderTitle(reminderID: String, title: String) async throws {
        guard let uid = Auth.auth().currentUser?.uid else { return }
        let normalizedTitle = title.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !normalizedTitle.isEmpty else { return }
        try await firestore
            .collection("users")
            .document(uid)
            .collection("reminders")
            .document(reminderID)
            .setData([
                "title": normalizedTitle,
                "updatedAt": FieldValue.serverTimestamp()
            ], merge: true)
        refresh()
    }

    func deleteReminder(reminderID: String) async throws {
        guard let uid = Auth.auth().currentUser?.uid else { return }
        cancelReminderLocalNotification(id: reminderID)
        try await firestore
            .collection("users")
            .document(uid)
            .collection("reminders")
            .document(reminderID)
            .delete()
        refresh()
    }

    func updateTaskTitle(taskID: String, title: String) async throws {
        guard let uid = Auth.auth().currentUser?.uid else { return }
        let normalizedTitle = title.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !normalizedTitle.isEmpty else { return }
        try await firestore
            .collection("users")
            .document(uid)
            .collection("tasks")
            .document(taskID)
            .setData([
                "title": normalizedTitle,
                "updatedAt": FieldValue.serverTimestamp()
            ], merge: true)
        refresh()
    }

    func deleteTask(taskID: String) async throws {
        guard let uid = Auth.auth().currentUser?.uid else { return }
        try await firestore
            .collection("users")
            .document(uid)
            .collection("tasks")
            .document(taskID)
            .delete()
        refresh()
    }

    func updateNoteTitle(noteID: String, title: String) async throws {
        guard let uid = Auth.auth().currentUser?.uid else { return }
        let normalizedTitle = title.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !normalizedTitle.isEmpty else { return }
        try await firestore
            .collection("users")
            .document(uid)
            .collection("notes")
            .document(noteID)
            .setData([
                "title": normalizedTitle,
                "updatedAt": FieldValue.serverTimestamp()
            ], merge: true)
        refresh()
    }

    func deleteNote(noteID: String) async throws {
        guard let uid = Auth.auth().currentUser?.uid else { return }
        try await firestore
            .collection("users")
            .document(uid)
            .collection("notes")
            .document(noteID)
            .delete()
        refresh()
    }

    private func scheduleReminderLocalNotification(id: String, title: String, at date: Date) {
        guard date.timeIntervalSinceNow > 2 else { return }
        let content = UNMutableNotificationContent()
        content.title = AppLocalized.text("notification.productivity_reminder.title", fallback: "Reminder")
        content.body = title
        content.sound = .default
        var cal = Calendar.current
        cal.timeZone = .current
        let parts = cal.dateComponents([.year, .month, .day, .hour, .minute], from: date)
        let trigger = UNCalendarNotificationTrigger(dateMatching: parts, repeats: false)
        let request = UNNotificationRequest(
            identifier: "skyos_reminder_\(id)",
            content: content,
            trigger: trigger
        )
        UNUserNotificationCenter.current().add(request) { _ in }
    }

    private func cancelReminderLocalNotification(id: String) {
        UNUserNotificationCenter.current()
            .removePendingNotificationRequests(withIdentifiers: ["skyos_reminder_\(id)"])
    }

    private func loadProductivitySnapshot() async -> HomeProductivitySnapshot {
        guard let uid = Auth.auth().currentUser?.uid else {
            return HomeProductivitySnapshot()
        }

        async let remindersSnapshot = firestore.collection("users")
            .document(uid)
            .collection("reminders")
            .limit(to: 60)
            .getDocuments()

        async let tasksSnapshot = firestore.collection("users")
            .document(uid)
            .collection("tasks")
            .limit(to: 40)
            .getDocuments()

        async let notesSnapshot = firestore.collection("users")
            .document(uid)
            .collection("notes")
            .limit(to: 40)
            .getDocuments()

        var dueToday: [ProductivityReminder] = []
        var upcoming: [ProductivityReminder] = []
        var tasks: [ProductivityTask] = []
        var notes: [ProductivityNote] = []
        let now = Date()
        let endOfDay = Calendar.current.startOfDay(for: now).addingTimeInterval(24 * 60 * 60)

        if let reminderDocs = try? await remindersSnapshot {
            for document in reminderDocs.documents {
                let data = document.data()
                let title = (data["title"] as? String)?
                    .trimmingCharacters(in: .whitespacesAndNewlines)
                    .isEmpty == false
                    ? (data["title"] as? String ?? "")
                    : ((data["text"] as? String ?? data["message"] as? String ?? "").trimmingCharacters(in: .whitespacesAndNewlines))
                guard !title.isEmpty else { continue }
                let dueAt = Self.resolveDate(
                    candidates: [
                        data["dueAt"],
                        data["scheduledAt"],
                        data["scheduledFor"],
                        data["remindAt"],
                        data["triggerAt"],
                        data["date"]
                    ]
                )
                let item = ProductivityReminder(
                    id: document.documentID,
                    title: title,
                    dueAt: dueAt,
                    isUpcoming: (dueAt ?? .distantPast) >= now
                )
                if let dueAt, dueAt >= now, dueAt < endOfDay {
                    dueToday.append(item)
                } else if let dueAt, dueAt >= now {
                    upcoming.append(item)
                }
            }
        }

        if let taskDocs = try? await tasksSnapshot {
            for document in taskDocs.documents {
                let data = document.data()
                let status = (data["status"] as? String ?? "open").lowercased()
                guard status != "completed" else { continue }
                let title = (data["title"] as? String ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
                guard !title.isEmpty else { continue }
                let dueAt = Self.resolveDate(candidates: [data["dueAt"], data["scheduledAt"]])
                if let dueAt, dueAt < now {
                    try? await firestore
                        .collection("users")
                        .document(uid)
                        .collection("tasks")
                        .document(document.documentID)
                        .delete()
                    continue
                }
                tasks.append(
                    ProductivityTask(
                        id: document.documentID,
                        title: title,
                        dueAt: dueAt
                    )
                )
            }
        }

        if let noteDocs = try? await notesSnapshot {
            for document in noteDocs.documents {
                let data = document.data()
                let createdAt = Self.resolveDate(candidates: [data["createdAt"]])
                let expiresAt = Self.resolveDate(candidates: [data["expiresAt"]])
                let effectiveExpiry: Date? = expiresAt
                    ?? createdAt.map { $0.addingTimeInterval(noteRetentionTimeInterval) }
                if let cut = effectiveExpiry, now >= cut {
                    try? await firestore
                        .collection("users")
                        .document(uid)
                        .collection("notes")
                        .document(document.documentID)
                        .delete()
                    continue
                }
                let rawTitle = (data["title"] as? String ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
                let content = (data["content"] as? String ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
                let title = rawTitle.isEmpty ? (content.isEmpty ? "" : String(content.prefix(48))) : rawTitle
                guard !title.isEmpty else { continue }
                notes.append(
                    ProductivityNote(
                        id: document.documentID,
                        title: title,
                        updatedAt: Self.resolveDate(candidates: [data["updatedAt"], data["createdAt"]])
                    )
                )
            }
        }

        dueToday.sort { ($0.dueAt ?? .distantFuture) < ($1.dueAt ?? .distantFuture) }
        upcoming.sort { ($0.dueAt ?? .distantFuture) < ($1.dueAt ?? .distantFuture) }
        tasks.sort {
            let lhsDue = $0.dueAt ?? .distantFuture
            let rhsDue = $1.dueAt ?? .distantFuture
            if lhsDue != rhsDue { return lhsDue < rhsDue }
            return $0.title.localizedCaseInsensitiveCompare($1.title) == .orderedAscending
        }
        notes.sort { ($0.updatedAt ?? .distantPast) > ($1.updatedAt ?? .distantPast) }

        return HomeProductivitySnapshot(
            dueTodayReminders: Array(dueToday.prefix(5)),
            upcomingReminders: Array(upcoming.prefix(5)),
            openTasks: Array(tasks.prefix(6)),
            recentNotes: Array(notes.prefix(6))
        )
    }

    private static func resolveDate(candidates: [Any?]) -> Date? {
        for candidate in candidates {
            switch candidate {
            case let timestamp as Timestamp:
                return timestamp.dateValue()
            case let date as Date:
                return date
            case let seconds as TimeInterval:
                return Date(timeIntervalSince1970: seconds > 10_000_000_000 ? seconds / 1000 : seconds)
            case let number as NSNumber:
                let value = number.doubleValue
                return Date(timeIntervalSince1970: value > 10_000_000_000 ? value / 1000 : value)
            default:
                continue
            }
        }
        return nil
    }

    private func loadRuntimeSignals() async -> HomeRuntimeSignals {
        guard let uid = Auth.auth().currentUser?.uid else {
            return HomeRuntimeSignals(contentSignal: buildContentSignal())
        }
        var signals = HomeRuntimeSignals(contentSignal: buildContentSignal())

        if let usageData = try? await firestore.collection("users")
            .document(uid)
            .collection("aiUsage")
            .order(by: "createdAt", descending: true)
            .limit(to: 1)
            .getDocuments()
            .documents
            .first?
            .data() {
            let warningLevel = (usageData["warningLevel"] as? String)?.lowercased() ?? ""
            if warningLevel == "warning" || warningLevel == "critical" {
                signals.aiUsageWarning = "Usage level: \(warningLevel.uppercased())."
            }
            let remaining = usageData["remainingForKind"] as? Int ?? -1
            let limit = usageData["limitForKind"] as? Int ?? -1
            if limit > 0 && remaining >= 0 {
                let usedRatio = Double(limit - remaining) / Double(limit)
                signals.creatorLimitZone = usedRatio >= 0.8
            }
        }

        if let runData = try? await firestore.collection("users")
            .document(uid)
            .collection("agentRuns")
            .order(by: "createdAt", descending: true)
            .limit(to: 1)
            .getDocuments()
            .documents
            .first?
            .data() {
            let status = (runData["status"] as? String ?? "").lowercased()
            signals.agentRunning = status == "running" || status == "processing"
            signals.workflowWaiting = status == "queued" || status == "waiting"
        }

        if let userData = try? await firestore.collection("users").document(uid).getDocument().data(),
           (userData["role"] as? String)?.lowercased() == "owner",
           let orderData = try? await firestore.collection("orders")
            .order(by: "timestamp", descending: true)
            .limit(to: 1)
            .getDocuments()
            .documents
            .first?
            .data() {
            let paymentStatus = orderData["paymentStatus"] as? String ?? ""
            let fulfillmentStatus = orderData["fulfillmentStatus"] as? String ?? ""
            if paymentStatus.lowercased() == "pending" {
                signals.commerceSignal = "Open payment requires review."
            } else if !fulfillmentStatus.isEmpty {
                signals.commerceSignal = "Shipping update: \(fulfillmentStatus)"
            } else {
                signals.commerceSignal = "New order activity available."
            }
        }

        if let runtimeData = try? await firestore.collection("system").document("runtimeConfig").getDocument().data() {
            let lockdown = runtimeData["lockdown"] as? Bool ?? false
            let uploadsEnabled = runtimeData["uploadsEnabled"] as? Bool ?? true
            let userWritesEnabled = runtimeData["userWritesEnabled"] as? Bool ?? true
            let registrationsEnabled = runtimeData["registrationsEnabled"] as? Bool ?? true
            let pausedCount = [uploadsEnabled, userWritesEnabled, registrationsEnabled].filter { !$0 }.count
            signals.newDataAvailable = !lockdown && pausedCount == 0
            signals.syncPaused = pausedCount > 0
            if pausedCount > 0 {
                signals.recoverableError = "System in reduced mode."
            }
        }
        signals.contentSignal = buildContentSignal()
        return signals
    }

    private func buildContentSignal() -> String? {
        if let track = featuredTrack {
            return String(
                format: AppLocalized.text("home.content_signal.new_drop", fallback: "New drop active: %@"),
                track.trackName
            )
        }
        if let video = featuredVideo {
            return String(
                format: AppLocalized.text("home.content_signal.video_activity", fallback: "Video activity: %@"),
                video.title
            )
        }
        return nil
    }

    private func loadLatestTrack() async -> Track? {
        let trackGroups = await withTaskGroup(of: [Track].self) { group in
            for artist in featuredArtists {
                group.addTask {
                    (try? await self.musicService.fetchTracks(for: artist)) ?? []
                }
            }

            var collectedTracks: [[Track]] = []
            for await tracks in group {
                collectedTracks.append(tracks)
            }
            return collectedTracks
        }

        let tracks = trackGroups.flatMap { $0 }
        guard !tracks.isEmpty else { return nil }

        return tracks.sorted(by: compareTracksForHomePriority).first
    }

    private func compareTracksForHomePriority(_ lhs: Track, _ rhs: Track) -> Bool {
        let lhsHasPreview = !(lhs.previewUrl ?? "").isEmpty
        let rhsHasPreview = !(rhs.previewUrl ?? "").isEmpty
        if lhsHasPreview != rhsHasPreview {
            return lhsHasPreview && !rhsHasPreview
        }

        let lhsHasFallbackTarget = trackHasHomeFallbackTarget(lhs)
        let rhsHasFallbackTarget = trackHasHomeFallbackTarget(rhs)
        if lhsHasFallbackTarget != rhsHasFallbackTarget {
            return lhsHasFallbackTarget && !rhsHasFallbackTarget
        }

        let lhsDate = parsedTrackReleaseDate(lhs.releaseDate)
        let rhsDate = parsedTrackReleaseDate(rhs.releaseDate)

        if lhsDate != rhsDate {
            return (lhsDate ?? .distantPast) > (rhsDate ?? .distantPast)
        }

        return lhs.trackName.localizedCaseInsensitiveCompare(rhs.trackName) == .orderedAscending
    }

    private func trackHasHomeFallbackTarget(_ track: Track) -> Bool {
        !(track.spotifyTrackID ?? "").isEmpty ||
        !(track.spotifyArtistID ?? "").isEmpty ||
        !(track.externalURL ?? "").isEmpty
    }

    private func parsedTrackReleaseDate(_ value: String?) -> Date? {
        guard let rawValue = value?.trimmingCharacters(in: .whitespacesAndNewlines), !rawValue.isEmpty else {
            return nil
        }

        let isoFormatter = ISO8601DateFormatter()
        isoFormatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        if let date = isoFormatter.date(from: rawValue) {
            return date
        }

        let fallbackISOFormatter = ISO8601DateFormatter()
        if let date = fallbackISOFormatter.date(from: rawValue) {
            return date
        }

        let formatters: [DateFormatter] = [
            {
                let formatter = DateFormatter()
                formatter.locale = Locale(identifier: "en_US_POSIX")
                formatter.timeZone = TimeZone(secondsFromGMT: 0)
                formatter.dateFormat = "yyyy-MM-dd"
                return formatter
            }(),
            {
                let formatter = DateFormatter()
                formatter.locale = Locale(identifier: "en_US_POSIX")
                formatter.timeZone = TimeZone(secondsFromGMT: 0)
                formatter.dateFormat = "yyyy-MM"
                return formatter
            }(),
            {
                let formatter = DateFormatter()
                formatter.locale = Locale(identifier: "en_US_POSIX")
                formatter.timeZone = TimeZone(secondsFromGMT: 0)
                formatter.dateFormat = "yyyy"
                return formatter
            }()
        ]

        for formatter in formatters {
            if let date = formatter.date(from: rawValue) {
                return date
            }
        }

        return nil
    }

    private func loadLatestVideo() async -> FeaturedHomeVideo? {
        do {
            let featuredSnapshot = try await firestore.collection("videographyHub")
                .whereField("isPublic", isEqualTo: true)
                .whereField("isHomeFeatured", isEqualTo: true)
                .limit(to: 1)
                .getDocuments()

            if let featuredDocument = featuredSnapshot.documents.first,
               let video = mapFeaturedVideo(from: featuredDocument) {
                return video
            }

            let snapshot = try await firestore.collection("videographyHub")
                .whereField("isPublic", isEqualTo: true)
                .limit(to: 12)
                .getDocuments()

            let latestDocument = snapshot.documents.max { lhs, rhs in
                documentDate(lhs) < documentDate(rhs)
            }

            guard let latestDocument else { return nil }
            return mapFeaturedVideo(from: latestDocument)
        } catch {
            return nil
        }
    }

    private func mapFeaturedVideo(from document: QueryDocumentSnapshot) -> FeaturedHomeVideo? {
        guard let title = document.data()["title"] as? String,
              !title.isEmpty else {
            return nil
        }

        return FeaturedHomeVideo(
            id: document.documentID,
            title: title,
            projectName: document.data()["projectName"] as? String ?? "Skydown Visual",
            notes: document.data()["notes"] as? String ?? "",
            downloadURL: document.data()["downloadURL"] as? String ?? "",
            externalURL: document.data()["externalURL"] as? String ?? "",
            embedURL: document.data()["embedURL"] as? String ?? "",
            sourceProvider: document.data()["sourceProvider"] as? String ?? ExternalMediaProvider.firebaseStorage.rawValue
        )
    }

    private func documentDate(_ document: QueryDocumentSnapshot) -> Date {
        let data = document.data()
        if let timestamp = data["createdAt"] as? Timestamp {
            return timestamp.dateValue()
        }
        if let date = data["createdAt"] as? Date {
            return date
        }
        return .distantPast
    }
}

private enum HomeRefreshResult {
    case track(Track?)
    case video(FeaturedHomeVideo?)
    case signals(HomeRuntimeSignals)
    case productivity(HomeProductivitySnapshot)
}

private struct HomeRuntimeSignals {
    var aiUsageWarning: String? = nil
    var creatorLimitZone: Bool = false
    var agentRunning: Bool = false
    var workflowWaiting: Bool = false
    var commerceSignal: String? = nil
    var syncPaused: Bool = false
    var recoverableError: String? = nil
    var newDataAvailable: Bool = false
    var contentSignal: String? = nil
}

private struct HomeProductivitySnapshot {
    var dueTodayReminders: [HomeViewModel.ProductivityReminder] = []
    var upcomingReminders: [HomeViewModel.ProductivityReminder] = []
    var openTasks: [HomeViewModel.ProductivityTask] = []
    var recentNotes: [HomeViewModel.ProductivityNote] = []
}
