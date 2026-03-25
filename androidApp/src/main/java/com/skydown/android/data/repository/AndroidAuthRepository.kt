package com.skydown.android.data.repository

import com.skydown.android.data.AppSessionStore
import com.skydown.android.data.GoogleSignInManager
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
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
        val authUser = auth.currentUser ?: return null
        return syncUserDocument(authUser)
            .also(AppSessionStore::update)
    }

    override suspend fun signIn(input: LoginInput): Result<User> {
        return runCatching {
            auth.signInWithEmailAndPassword(input.email, input.password).await()
            (currentUser() ?: error("Benutzer konnte nicht geladen werden."))
                .also(AppSessionStore::update)
        }.recoverCatching { error ->
            throw error.toReadableAuthError()
        }
    }

    override suspend fun signInWithGoogle(idToken: String, preferredUsername: String?): Result<User> {
        return runCatching {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user ?: error("Google-Benutzer konnte nicht geladen werden.")
            syncUserDocument(firebaseUser, preferredUsername)
                .also(AppSessionStore::update)
        }.recoverCatching { error ->
            throw error.toReadableAuthError()
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
        }.recoverCatching { error ->
            throw error.toReadableAuthError()
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return runCatching {
            GoogleSignInManager.client(auth.app.applicationContext).signOut().await()
            auth.signOut()
            AppSessionStore.update(null)
        }
    }

    private suspend fun syncUserDocument(
        authUser: FirebaseUser,
        preferredUsername: String? = null,
    ): User {
        val documentReference = firestore.collection("users").document(authUser.uid)
        val snapshot = documentReference.get().await()
        val fallbackUser = authUser.toSharedUser()
        val resolvedUsername = resolvedUsername(authUser, preferredUsername)

        if (!snapshot.exists()) {
            val fallbackEmail = authUser.email.orEmpty()
            val user = User(
                id = authUser.uid,
                email = fallbackEmail,
                username = resolvedUsername,
                whatsApp = null,
                registrationDateEpochMillis = authUser.metadata?.creationTimestamp ?: System.currentTimeMillis(),
                isAdmin = false,
            )

            documentReference.set(user).await()
            return user
        }

        val data = snapshot.data.orEmpty()
        val updates = mutableMapOf<String, Any>()

        if ((data["username"] as? String).isNullOrBlank()) {
            updates["username"] = resolvedUsername
        }

        val hasRegistrationDate = data["registrationDateEpochMillis"] is Number || data["registrationDate"] != null
        if (!hasRegistrationDate) {
            updates["registrationDateEpochMillis"] = authUser.metadata?.creationTimestamp ?: System.currentTimeMillis()
        }

        if (updates.isNotEmpty()) {
            documentReference.update(updates).await()
            return documentReference.get().await().toSharedUser(authUser)
                ?: fallbackUser.copy(username = resolvedUsername)
        }

        return snapshot.toSharedUser(authUser) ?: fallbackUser.copy(username = resolvedUsername)
    }
}

private fun resolvedUsername(
    authUser: FirebaseUser,
    preferredUsername: String?,
): String {
    val fallbackEmail = authUser.email.orEmpty()
    return preferredUsername?.trim()?.takeIf { it.isNotEmpty() }
        ?: authUser.displayName?.takeIf { it.isNotBlank() }
        ?: fallbackEmail.substringBefore("@").ifBlank { "Skydown User" }
}

private fun Throwable.toReadableAuthError(): Throwable {
    val firebaseMessage = (this as? FirebaseAuthException)?.errorCode?.let { errorCode ->
        when (errorCode) {
            "ERROR_INVALID_EMAIL" -> "Die E-Mail-Adresse ist ungueltig."
            "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" -> "E-Mail oder Passwort stimmen nicht."
            "ERROR_USER_NOT_FOUND" -> "Kein Benutzer mit dieser E-Mail gefunden."
            "ERROR_USER_DISABLED" -> "Dieses Konto wurde deaktiviert."
            "ERROR_EMAIL_ALREADY_IN_USE" -> "Diese E-Mail wird bereits verwendet."
            "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" -> "Diese E-Mail ist bereits mit einer anderen Anmeldemethode verknuepft."
            "ERROR_CREDENTIAL_ALREADY_IN_USE" -> "Das Google-Konto wird bereits von einem anderen Benutzer verwendet."
            "ERROR_WEAK_PASSWORD" -> "Das Passwort ist zu schwach."
            else -> null
        }
    }

    return IllegalStateException(firebaseMessage ?: message ?: "Authentifizierung fehlgeschlagen.")
}
