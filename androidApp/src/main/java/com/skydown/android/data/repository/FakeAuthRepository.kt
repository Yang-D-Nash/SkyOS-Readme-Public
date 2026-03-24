package com.skydown.android.data.repository

import com.skydown.android.data.AppSessionStore
import com.skydown.shared.model.LoginInput
import com.skydown.shared.model.RegistrationInput
import com.skydown.shared.model.User
import com.skydown.shared.model.sampleUser
import com.skydown.shared.repository.AuthRepository

class FakeAuthRepository : AuthRepository {
    private var currentUser: User? = sampleUser().also(AppSessionStore::update)

    override suspend fun currentUser(): User? = currentUser

    override suspend fun signIn(input: LoginInput): Result<User> {
        currentUser = sampleUser().copy(email = input.email)
        AppSessionStore.update(currentUser)
        return Result.success(currentUser!!)
    }

    override suspend fun register(input: RegistrationInput): Result<User> {
        currentUser = sampleUser().copy(
            email = input.email,
            username = input.username,
            whatsApp = input.whatsApp,
        )
        AppSessionStore.update(currentUser)
        return Result.success(currentUser!!)
    }

    override suspend fun signOut(): Result<Unit> {
        currentUser = null
        AppSessionStore.update(null)
        return Result.success(Unit)
    }
}
