import Foundation

/// In-repo defaults for platform owner RBAC and the public support inbox.
/// Keep `ownerEmail` in sync with `PlatformContactEmails.OWNER_EMAIL` in shared KMP
/// and with `isOwnerEmail()` in `firestore.rules` / `storage.rules`.
enum PlatformContactEmails {
    static let ownerEmail = "nash.lioncorna@gmail.com"
    static let defaultSupportEmail = "skydownent@gmail.com"
}
