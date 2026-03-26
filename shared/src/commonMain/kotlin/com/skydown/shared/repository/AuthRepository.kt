package com.skydown.shared.repository

import com.skydown.shared.model.LoginInput
import com.skydown.shared.model.RegistrationInput
import com.skydown.shared.model.User

interface AuthRepository {
    suspend fun currentUser(): User?
    suspend fun signIn(input: LoginInput): Result<User>
    suspend fun signInWithGoogle(idToken: String, preferredUsername: String? = null): Result<User>
    suspend fun register(input: RegistrationInput): Result<User>
    suspend fun signOut(): Result<Unit>
    suspend fun deleteCurrentAccount(): Result<Unit>
}
