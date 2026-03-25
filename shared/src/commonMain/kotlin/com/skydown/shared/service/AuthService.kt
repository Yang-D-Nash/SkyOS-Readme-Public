package com.skydown.shared.service

import com.skydown.shared.model.LoginInput
import com.skydown.shared.model.RegistrationInput
import com.skydown.shared.model.User
import com.skydown.shared.repository.AuthRepository
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

    suspend fun signInWithGoogle(idToken: String): Result<User> {
        if (idToken.isBlank()) {
            return Result.failure(IllegalArgumentException("Google-Anmeldung konnte nicht gestartet werden."))
        }

        return repository.signInWithGoogle(idToken)
    }

    suspend fun register(input: RegistrationInput): Result<User> {
        val validationError = AuthValidation.validateRegistration(input)
        if (validationError != null) {
            return Result.failure(IllegalArgumentException(validationError))
        }

        return repository.register(input)
    }

    suspend fun signOut(): Result<Unit> = repository.signOut()
}
