package com.skydown.android.ui.auth

/**
 * Where sign-in was requested from, so the login sheet can echo a calm, benefit-led reason
 * instead of a generic gate.
 */
enum class AuthEntryContext {
    DEFAULT,
    AI,
    MERCH_SHOP,
    CART,
    SETTINGS,
    MUSIC,
}
