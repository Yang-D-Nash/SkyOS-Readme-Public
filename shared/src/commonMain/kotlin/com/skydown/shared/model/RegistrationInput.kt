package com.skydown.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class RegistrationInput(
    val username: String,
    val email: String,
    val whatsApp: String,
    val password: String,
    val confirmPassword: String,
    val consent: RegistrationConsentInput = RegistrationConsentInput(),
)

@Serializable
data class RegistrationConsentInput(
    val acceptedTerms: Boolean = false,
    val acceptedPrivacyPolicy: Boolean = false,
    val aiConsentEnabled: Boolean = false,
    val legalVersionLabel: String = "",
    val consentSource: String = "",
)
