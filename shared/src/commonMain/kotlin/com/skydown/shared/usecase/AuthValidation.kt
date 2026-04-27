package com.skydown.shared.usecase

import com.skydown.shared.model.LoginInput
import com.skydown.shared.model.RegistrationConsentInput
import com.skydown.shared.model.RegistrationInput
import com.skydown.shared.text.SharedText

object AuthValidation {
    fun validateLogin(input: LoginInput): String? {
        if (input.email.isBlank() || input.password.isBlank()) {
            return SharedText.AUTH_LOGIN_EMAIL_PASSWORD_REQUIRED
        }
        return null
    }

    fun validateRegistration(input: RegistrationInput): String? {
        if (input.email.isBlank() || input.password.isBlank() || input.confirmPassword.isBlank()) {
            return SharedText.AUTH_REGISTER_REQUIRED_FIELDS
        }
        if (input.password != input.confirmPassword) {
            return SharedText.AUTH_REGISTER_PASSWORD_MISMATCH
        }
        val consentError = validateRegistrationConsent(input.consent)
        if (consentError != null) {
            return consentError
        }
        return null
    }

    fun validateRegistrationConsent(consent: RegistrationConsentInput): String? {
        if (!consent.acceptedTerms || !consent.acceptedPrivacyPolicy) {
            return SharedText.AUTH_REGISTER_CONSENT_REQUIRED
        }

        if (consent.legalVersionLabel.isBlank()) {
            return SharedText.AUTH_REGISTER_LEGAL_VERSION_MISSING
        }

        if (consent.consentSource.isBlank()) {
            return SharedText.AUTH_REGISTER_CONSENT_SOURCE_MISSING
        }

        return null
    }
}
