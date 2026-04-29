package com.skydown.shared.model

/**
 * In-repo defaults for platform owner identity and the public support inbox.
 *
 * **Owner e-mail** must match the literal checked in `firestore.rules` and `storage.rules`
 * inside `isOwnerEmail()` whenever you change the address (Firebase Rules cannot read env vars).
 *
 * **Functions** mirror these defaults in `functions/src/security/constants.js`
 * (`OWNER_EMAIL`, `DEFAULT_SUPPORT_EMAIL`); override with `SKYOS_OWNER_EMAIL` / `SKYOS_SUPPORT_EMAIL` in production.
 */
object PlatformContactEmails {
    const val OWNER_EMAIL: String = "nash.lioncorna@gmail.com"
    const val DEFAULT_SUPPORT_EMAIL: String = "skydownent@gmail.com"
}
