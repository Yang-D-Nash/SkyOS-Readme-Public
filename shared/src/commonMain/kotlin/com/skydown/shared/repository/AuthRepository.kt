package com.skydown.shared.repository

import com.skydown.shared.model.LoginInput
import com.skydown.shared.model.ProfileUpdateInput
import com.skydown.shared.model.RegistrationConsentInput
import com.skydown.shared.model.RegistrationInput
import com.skydown.shared.model.User

interface AuthRepository {
    suspend fun currentUser(): User?
    suspend fun signIn(input: LoginInput): Result<User>
    suspend fun signInWithGoogle(
        idToken: String,
        preferredUsername: String? = null,
        registrationConsent: RegistrationConsentInput? = null,
    ): Result<User>
    suspend fun register(input: RegistrationInput): Result<User>
    suspend fun updateCurrentProfile(input: ProfileUpdateInput): Result<User>
    suspend fun updateCurrentAiAccessEnabled(enabled: Boolean): Result<User>
    suspend fun signOut(): Result<Unit>
    suspend fun deleteCurrentAccount(): Result<Unit>
}
