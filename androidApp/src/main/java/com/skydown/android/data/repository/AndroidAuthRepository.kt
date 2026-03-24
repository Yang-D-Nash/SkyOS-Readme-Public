package com.skydown.android.data.repository

import com.skydown.android.data.AppSessionStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.skydown.shared.model.LoginInput
import com.skydown.shared.model.RegistrationInput
import com.skydown.shared.model.User
import com.skydown.shared.repository.AuthRepository
import kotlinx.coroutines.tasks.await

class AndroidAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : AuthRepository {
    override suspend fun currentUser(): User? {
        val uid = auth.currentUser?.uid ?: return null
        return firestore.collection("users").document(uid).get().await().toSharedUser()
            ?.also(AppSessionStore::update)
    }

    override suspend fun signIn(input: LoginInput): Result<User> {
        return runCatching {
            auth.signInWithEmailAndPassword(input.email, input.password).await()
            (currentUser() ?: error("Benutzer konnte nicht geladen werden."))
                .also(AppSessionStore::update)
        }
    }

    override suspend fun register(input: RegistrationInput): Result<User> {
        return runCatching {
            val authResult = auth.createUserWithEmailAndPassword(input.email, input.password).await()
            val user = User(
                id = authResult.user?.uid,
                email = input.email,
                username = input.username,
                whatsApp = input.whatsApp.ifBlank { null },
                registrationDateEpochMillis = System.currentTimeMillis(),
                isAdmin = false,
            )
            firestore.collection("users").document(authResult.user?.uid.orEmpty()).set(user).await()
            AppSessionStore.update(user)
            user
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return runCatching {
            auth.signOut()
            AppSessionStore.update(null)
        }
    }
}

private fun com.google.firebase.firestore.DocumentSnapshot.toSharedUser(): User? {
    val data = data ?: return null
    return User(
        id = id,
        email = data["email"] as? String ?: return null,
        username = data["username"] as? String ?: return null,
        whatsApp = data["whatsApp"] as? String,
        registrationDateEpochMillis = (data["registrationDateEpochMillis"] as? Number)?.toLong()
            ?: (data["registrationDate"] as? com.google.firebase.Timestamp)?.toDate()?.time
            ?: System.currentTimeMillis(),
        isAdmin = data["isAdmin"] as? Boolean ?: false,
    )
}
