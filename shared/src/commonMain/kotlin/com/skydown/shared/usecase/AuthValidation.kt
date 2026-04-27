package com.skydown.shared.usecase

import com.skydown.shared.model.LoginInput
import com.skydown.shared.model.RegistrationConsentInput
import com.skydown.shared.model.RegistrationInput
import com.skydown.shared.text.SharedText

object AuthValidation {
    fun validateLogin(input: LoginInput): String? {
        if (input.email.isBlankOrAny(input.password)) {
            return SharedText.AUTH_LOGIN_EMAIL_PASSWORD_REQUIRED
        }
        return null
    }

    fun validateRegistration(input: RegistrationInput): String? {
        if (input.email.isBlankOrAny(input.password, input.confirmPassword)) {
            return SharedText.AUTH_REGISTER_REQUIRED_FIELDS
        }
        if (input.password != input.confirmPassword) {
            return SharedText.AUTH_REGISTER_PASSWORD_MISMATCH
        }
        return validateRegistrationConsent(input.consent)
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

private fun String.isBlankOrAny(vararg others: String): Boolean {
    return isBlank() || others.any(String::isBlank)
}
