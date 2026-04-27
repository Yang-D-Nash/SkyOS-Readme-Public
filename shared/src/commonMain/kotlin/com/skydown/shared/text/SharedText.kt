package com.skydown.shared.text

object SharedText {
    const val AUTH_LOGIN_EMAIL_PASSWORD_REQUIRED = "Please enter email and password."
    const val AUTH_REGISTER_REQUIRED_FIELDS = "Please fill in all required fields."
    const val AUTH_REGISTER_PASSWORD_MISMATCH = "Passwords do not match."
    const val AUTH_REGISTER_CONSENT_REQUIRED = "Please accept terms and privacy policy to continue."
    const val AUTH_REGISTER_LEGAL_VERSION_MISSING = "Legal version could not be loaded. Please try again."
    const val AUTH_REGISTER_CONSENT_SOURCE_MISSING = "Consent source could not be resolved."
    const val AUTH_GOOGLE_SIGNIN_START_FAILED = "Google sign-in could not be started."
    const val AUTH_PROFILE_USERNAME_REQUIRED = "Please enter a username."
    const val AUTH_PROFILE_USERNAME_MAX = "Username must be at most 32 characters."
    const val AUTH_PROFILE_TAGLINE_MAX = "Tagline must be at most 60 characters."
    const val AUTH_PROFILE_BIO_MAX = "Bio must be at most 240 characters."
    const val AUTH_PROFILE_INSTAGRAM_HANDLE_MAX = "Instagram handle is too long."

    const val CART_CONTACT_NAME_EMAIL_REQUIRED = "Please fill in name and email."
    const val CART_CONTACT_MESSAGE_TEMPLATE = "Hello, I am interested in '%1\$s' in size %2\$s%3\$s x%4\$d."

    const val MERCH_VARIANT_NONE_AVAILABLE = "No Shopify variants available for %1\$s."
    const val MERCH_VARIANT_SIZE_REQUIRED = "Size is required."
    const val MERCH_VARIANT_NOT_FOUND = "No matching variant found for size %1\$s%2\$s."
    const val MERCH_VARIANT_MULTIPLE_FOUND = "Multiple variants found for size %1\$s%2\$s."

    const val ORDER_SUBMIT_CART_EMPTY = "Cart is empty."
    const val ORDER_SUBMIT_USER_NOT_SIGNED_IN = "User is not signed in."
    const val ORDER_SUBMIT_NAME_EMAIL_REQUIRED = "Name and email are required."
    const val ORDER_SUBMIT_ADDRESS_REQUIRED = "Address is required."
    const val ORDER_SUBMIT_SHIPPING_REQUIRED = "Shipping zone and country must be set."
    const val ORDER_INVALID_ID = "Order has no valid ID."
}
