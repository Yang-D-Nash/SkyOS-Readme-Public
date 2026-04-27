package com.skydown.shared.service

import com.skydown.shared.model.LoginInput
import com.skydown.shared.model.ProfileUpdateInput
import com.skydown.shared.model.RegistrationConsentInput
import com.skydown.shared.model.RegistrationInput
import com.skydown.shared.model.User
import com.skydown.shared.repository.AuthRepository
import com.skydown.shared.text.SharedText
import com.skydown.shared.usecase.AuthValidation

class AuthService(
    private val repository: AuthRepository,
) {
    suspend fun currentUser(): User? = repository.currentUser()

    suspend fun signIn(input: LoginInput): Result<User> {
        val validationError = AuthValidation.validateLogin(input)
        if (validationError != null) {
            return Result.failure(IllegalArgumentException(validationError))
        }

        return repository.signIn(input)
    }

    suspend fun signInWithGoogle(
        idToken: String,
        preferredUsername: String? = null,
        registrationConsent: RegistrationConsentInput? = null,
    ): Result<User> {
        if (idToken.isBlank()) {
            return Result.failure(IllegalArgumentException(SharedText.AUTH_GOOGLE_SIGNIN_START_FAILED))
        }

        if (registrationConsent != null) {
            val consentError = AuthValidation.validateRegistrationConsent(registrationConsent)
            if (consentError != null) {
                return Result.failure(IllegalArgumentException(consentError))
            }
        }

        return repository.signInWithGoogle(
            idToken = idToken,
            preferredUsername = preferredUsername?.trim()?.ifBlank { null },
            registrationConsent = registrationConsent,
        )
    }

    suspend fun register(input: RegistrationInput): Result<User> {
        val validationError = AuthValidation.validateRegistration(input)
        if (validationError != null) {
            return Result.failure(IllegalArgumentException(validationError))
        }

        return repository.register(input)
    }

    suspend fun updateCurrentProfile(input: ProfileUpdateInput): Result<User> {
        val normalizedUsername = input.username.trim()
        if (normalizedUsername.isEmpty()) {
            return Result.failure(IllegalArgumentException(SharedText.AUTH_PROFILE_USERNAME_REQUIRED))
        }
        if (normalizedUsername.length > 32) {
            return Result.failure(IllegalArgumentException(SharedText.AUTH_PROFILE_USERNAME_MAX))
        }
        if ((input.profileTagline?.trim()?.length ?: 0) > 60) {
            return Result.failure(IllegalArgumentException(SharedText.AUTH_PROFILE_TAGLINE_MAX))
        }
        if ((input.profileBio?.trim()?.length ?: 0) > 240) {
            return Result.failure(IllegalArgumentException(SharedText.AUTH_PROFILE_BIO_MAX))
        }
        if ((input.instagramHandle?.trim()?.removePrefix("@")?.length ?: 0) > 40) {
            return Result.failure(IllegalArgumentException(SharedText.AUTH_PROFILE_INSTAGRAM_HANDLE_MAX))
        }

        return repository.updateCurrentProfile(
            input.copy(
                username = normalizedUsername,
                whatsApp = input.whatsApp?.trim()?.ifBlank { null },
                profileTagline = input.profileTagline?.trim()?.ifBlank { null },
                profileBio = input.profileBio?.trim()?.ifBlank { null },
                instagramHandle = input.instagramHandle
                    ?.trim()
                    ?.removePrefix("@")
                    ?.ifBlank { null },
            ),
        )
    }

    suspend fun updateCurrentAiAccessEnabled(enabled: Boolean): Result<User> {
        return repository.updateCurrentAiAccessEnabled(enabled)
    }

    suspend fun signOut(): Result<Unit> = repository.signOut()

    suspend fun deleteCurrentAccount(): Result<Unit> = repository.deleteCurrentAccount()
}
