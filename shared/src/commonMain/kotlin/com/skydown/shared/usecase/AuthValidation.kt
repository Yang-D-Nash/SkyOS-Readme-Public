package com.skydown.shared.usecase

import com.skydown.shared.model.LoginInput
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
        return null
    }
}
