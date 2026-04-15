package com.skydown.shared.usecase

import com.skydown.shared.model.LoginInput
import com.skydown.shared.model.RegistrationConsentInput
import com.skydown.shared.model.RegistrationInput

object AuthValidation {
    fun validateLogin(input: LoginInput): String? {
        if (input.email.isBlank() || input.password.isBlank()) {
            return "Bitte E-Mail und Passwort ausfuellen."
        }
        return null
    }

    fun validateRegistration(input: RegistrationInput): String? {
        if (input.email.isBlank() || input.password.isBlank() || input.confirmPassword.isBlank()) {
            return "Bitte alle Pflichtfelder ausfuellen."
        }
        if (input.password != input.confirmPassword) {
            return "Passwoerter stimmen nicht ueberein."
        }
        val consentError = validateRegistrationConsent(input.consent)
        if (consentError != null) {
            return consentError
        }
        return null
    }

    fun validateRegistrationConsent(consent: RegistrationConsentInput): String? {
        if (!consent.acceptedTerms || !consent.acceptedPrivacyPolicy) {
            return "Bitte akzeptiere AGB und Datenschutz, um fortzufahren."
        }

        if (consent.legalVersionLabel.isBlank()) {
            return "Die Rechtsversion konnte nicht geladen werden. Bitte versuche es erneut."
        }

        if (consent.consentSource.isBlank()) {
            return "Die Zustimmung konnte nicht korrekt zugeordnet werden."
        }

        return null
    }
}
