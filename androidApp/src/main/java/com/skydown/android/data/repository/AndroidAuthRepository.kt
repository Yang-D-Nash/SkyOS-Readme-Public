package com.skydown.android.data.repository

import com.skydown.android.data.AppSessionStore
import com.skydown.android.data.GoogleSignInManager
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.FirebaseFunctions
import com.skydown.shared.model.LoginInput
import com.skydown.shared.model.ProfileUpdateInput
import com.skydown.shared.model.RegistrationInput
import com.skydown.shared.model.User
import com.skydown.shared.model.UserQuotaPlan
import com.skydown.shared.model.UserRole
import com.skydown.shared.repository.AuthRepository
import kotlinx.coroutines.tasks.await

class AndroidAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("us-central1"),
) : AuthRepository {
    override suspend fun currentUser(): User? {
        val authUser = auth.currentUser ?: return null
        return resolveCurrentUserAfterAuth(authUser)
            .also(AppSessionStore::update)
    }

    override suspend fun signIn(input: LoginInput): Result<User> {
        return runCatching {
            val authResult = auth.signInWithEmailAndPassword(input.email, input.password).await()
            authResult.user?.let { syncSessionClaimsIfPossible(it) }
            (authResult.user?.let { resolveCurrentUserAfterAuth(it) } ?: error("Benutzer konnte nicht geladen werden."))
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
            resolveCurrentUserAfterAuth(firebaseUser, preferredUsername)
                .also(AppSessionStore::update)
        }.recoverCatching { error ->
            throw error.toReadableAuthError()
        }
    }

    override suspend fun register(input: RegistrationInput): Result<User> {
        return runCatching {
            ensureRegistrationsOpen()
            val authResult = auth.createUserWithEmailAndPassword(input.email, input.password).await()
            val firebaseUser = authResult.user ?: error("Benutzer konnte nicht erstellt werden.")
            val registeredEmail = firebaseUser.email?.takeIf { it.isNotBlank() }?.lowercase() ?: input.email.trim().lowercase()
            val resolvedRole = UserRole.User
            val quotaPlan = UserQuotaPlan.defaultPlanFor(resolvedRole)
            val user = User(
                id = firebaseUser.uid,
                email = registeredEmail,
                username = input.username,
                whatsApp = input.whatsApp.ifBlank { null },
                profileTagline = null,
                profileBio = null,
                instagramHandle = null,
                registrationDateEpochMillis = System.currentTimeMillis(),
                isAdmin = false,
                role = resolvedRole.rawValue,
                quotaPlan = quotaPlan.rawValue,
                aiAccessEnabled = true,
                aiTextRequestsPerDay = quotaPlan.aiTextRequestsPerDay,
                aiVisualRequestsPerDay = quotaPlan.aiVisualRequestsPerDay,
                aiAgentRequestsPerDay = quotaPlan.aiAgentRequestsPerDay,
                aiHistoryRetentionDays = quotaPlan.aiHistoryRetentionDays,
                canManageMusicCatalog = false,
                canManageVideoCatalog = false,
                canModerateProfiles = false,
            )
            firestore.collection("users").document(firebaseUser.uid).set(user).await()
            syncPublicProfileDocument(
                uid = firebaseUser.uid,
                username = user.username,
                profileImageUrl = null,
                profileImagePath = null,
                profileTagline = null,
                profileBio = null,
                instagramHandle = null,
                whatsApp = user.whatsApp,
            )
            syncSessionClaimsIfPossible(firebaseUser)
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

    override suspend fun deleteCurrentAccount(): Result<Unit> {
        return runCatching {
            val authUser = auth.currentUser ?: error("Kein Benutzer angemeldet.")
            val uid = authUser.uid

            authUser.delete().await()
            runCatching {
                firestore.collection("users").document(uid).delete().await()
            }
            GoogleSignInManager.client(auth.app.applicationContext).signOut().await()
            AppSessionStore.update(null)
        }.recoverCatching { error ->
            throw error.toReadableAuthError()
        }
    }

    override suspend fun updateCurrentProfile(input: ProfileUpdateInput): Result<User> {
        return runCatching {
            val authUser = auth.currentUser ?: error("Kein Benutzer angemeldet.")
            val documentReference = firestore.collection("users").document(authUser.uid)

            authUser.updateProfile(
                userProfileChangeRequest {
                    displayName = input.username
                },
            ).await()

            val updates = mutableMapOf<String, Any>(
                "username" to input.username,
                "whatsApp" to (input.whatsApp ?: FieldValue.delete()),
                "profileTagline" to (input.profileTagline ?: FieldValue.delete()),
                "profileBio" to (input.profileBio ?: FieldValue.delete()),
                "instagramHandle" to (input.instagramHandle ?: FieldValue.delete()),
            )

            documentReference.update(updates).await()
            val currentSnapshot = documentReference.get().await()
            val currentData = currentSnapshot.data.orEmpty()
            syncPublicProfileDocument(
                uid = authUser.uid,
                username = input.username,
                profileImageUrl = currentData["profileImageURL"] as? String,
                profileImagePath = currentData["profileImagePath"] as? String,
                profileTagline = input.profileTagline,
                profileBio = input.profileBio,
                instagramHandle = input.instagramHandle,
                whatsApp = input.whatsApp,
            )
            val updatedUser = syncUserDocument(authUser)
            AppSessionStore.update(updatedUser)
            updatedUser
        }.recoverCatching { error ->
            throw error.toReadableAuthError()
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
            val fallbackEmail = authUser.email.orEmpty().lowercase()
            val resolvedRole = UserRole.resolve(
                rawValue = null,
                isAdmin = false,
                email = fallbackEmail,
            )
            val quotaPlan = UserQuotaPlan.defaultPlanFor(resolvedRole)
            ensureRegistrationAllowedForBootstrap(authUser, resolvedRole)
            val user = User(
                id = authUser.uid,
                email = fallbackEmail,
                username = resolvedUsername,
                whatsApp = null,
                profileTagline = null,
                profileBio = null,
                instagramHandle = null,
                registrationDateEpochMillis = authUser.metadata?.creationTimestamp ?: System.currentTimeMillis(),
                isAdmin = resolvedRole.hasStaffAccess,
                role = resolvedRole.rawValue,
                quotaPlan = quotaPlan.rawValue,
                aiAccessEnabled = true,
                aiTextRequestsPerDay = quotaPlan.aiTextRequestsPerDay,
                aiVisualRequestsPerDay = quotaPlan.aiVisualRequestsPerDay,
                aiAgentRequestsPerDay = quotaPlan.aiAgentRequestsPerDay,
                aiHistoryRetentionDays = quotaPlan.aiHistoryRetentionDays,
                canManageMusicCatalog = resolvedRole == UserRole.Owner,
                canManageVideoCatalog = resolvedRole == UserRole.Owner,
                canModerateProfiles = resolvedRole == UserRole.Owner,
            )

            documentReference.set(user).await()
            syncPublicProfileDocument(
                uid = authUser.uid,
                username = user.username,
                profileImageUrl = null,
                profileImagePath = null,
                profileTagline = null,
                profileBio = null,
                instagramHandle = null,
                whatsApp = null,
            )
            syncSessionClaimsIfPossible(authUser)
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

        val storedIsAdmin = data["isAdmin"] as? Boolean ?: false
        val resolvedRole = UserRole.resolve(
            rawValue = data["role"] as? String,
            isAdmin = storedIsAdmin,
            email = data["email"] as? String ?: authUser.email,
        )

        if ((data["role"] as? String).isNullOrBlank() || resolvedRole == UserRole.Owner) {
            updates["role"] = resolvedRole.rawValue
        }

        val normalizedEmail = authUser.email.orEmpty().trim().lowercase()
        if ((data["email"] as? String)?.trim()?.lowercase() != normalizedEmail && normalizedEmail.isNotBlank()) {
            updates["email"] = normalizedEmail
        }

        if ((data["isAdmin"] as? Boolean) != resolvedRole.hasStaffAccess) {
            updates["isAdmin"] = resolvedRole.hasStaffAccess
        }

        val quotaPlan = UserQuotaPlan.resolve(
            rawValue = data["quotaPlan"] as? String,
            role = resolvedRole,
        )

        if ((data["quotaPlan"] as? String)?.trim()?.lowercase() != quotaPlan.rawValue || resolvedRole == UserRole.Owner) {
            updates["quotaPlan"] = quotaPlan.rawValue
        }

        if (data["aiAccessEnabled"] !is Boolean) {
            updates["aiAccessEnabled"] = true
        }

        if (data["aiTextRequestsPerDay"] !is Number || resolvedRole == UserRole.Owner) {
            updates["aiTextRequestsPerDay"] = quotaPlan.aiTextRequestsPerDay
        }

        if (data["aiVisualRequestsPerDay"] !is Number || resolvedRole == UserRole.Owner) {
            updates["aiVisualRequestsPerDay"] = quotaPlan.aiVisualRequestsPerDay
        }

        if (data["aiAgentRequestsPerDay"] !is Number || resolvedRole == UserRole.Owner) {
            updates["aiAgentRequestsPerDay"] = quotaPlan.aiAgentRequestsPerDay
        }

        if (data["aiHistoryRetentionDays"] !is Number || resolvedRole == UserRole.Owner) {
            updates["aiHistoryRetentionDays"] = quotaPlan.aiHistoryRetentionDays
        }

        if (data["canManageMusicCatalog"] !is Boolean || resolvedRole == UserRole.Owner) {
            updates["canManageMusicCatalog"] = resolvedRole == UserRole.Owner
        }

        if (data["canManageVideoCatalog"] !is Boolean || resolvedRole == UserRole.Owner) {
            updates["canManageVideoCatalog"] = resolvedRole == UserRole.Owner
        }

        if (data["canModerateProfiles"] !is Boolean || resolvedRole == UserRole.Owner) {
            updates["canModerateProfiles"] = resolvedRole == UserRole.Owner
        }

        if (updates.isNotEmpty()) {
            documentReference.update(updates).await()
            syncSessionClaimsIfPossible(authUser)
            syncPublicProfileDocument(
                uid = authUser.uid,
                username = (updates["username"] as? String) ?: (data["username"] as? String ?: resolvedUsername),
                profileImageUrl = (updates["profileImageURL"] as? String) ?: (data["profileImageURL"] as? String),
                profileImagePath = (updates["profileImagePath"] as? String) ?: (data["profileImagePath"] as? String),
                profileTagline = (updates["profileTagline"] as? String) ?: (data["profileTagline"] as? String),
                profileBio = (updates["profileBio"] as? String) ?: (data["profileBio"] as? String),
                instagramHandle = (updates["instagramHandle"] as? String) ?: (data["instagramHandle"] as? String),
                whatsApp = (updates["whatsApp"] as? String) ?: (data["whatsApp"] as? String),
            )
            return documentReference.get().await().toSharedUser(authUser)
                ?: fallbackUser.copy(username = resolvedUsername)
        }

        syncSessionClaimsIfPossible(authUser)
        syncPublicProfileDocument(
            uid = authUser.uid,
            username = data["username"] as? String ?: resolvedUsername,
            profileImageUrl = data["profileImageURL"] as? String,
            profileImagePath = data["profileImagePath"] as? String,
            profileTagline = data["profileTagline"] as? String,
            profileBio = data["profileBio"] as? String,
            instagramHandle = data["instagramHandle"] as? String,
            whatsApp = data["whatsApp"] as? String,
        )
        return snapshot.toSharedUser(authUser) ?: fallbackUser.copy(username = resolvedUsername)
    }

    private suspend fun refreshAuthToken(authUser: FirebaseUser) {
        authUser.getIdToken(true).await()
    }

    private suspend fun syncSessionClaims(authUser: FirebaseUser) {
        try {
            functions.getHttpsCallable("syncCurrentUserClaims").call(emptyMap<String, Any>()).await()
            refreshAuthToken(authUser)
        } catch (error: FirebaseFunctionsException) {
            if (error.code == FirebaseFunctionsException.Code.NOT_FOUND ||
                error.code == FirebaseFunctionsException.Code.UNIMPLEMENTED
            ) {
                return
            }
            throw error
        }
    }

    private suspend fun syncSessionClaimsIfPossible(authUser: FirebaseUser) {
        runCatching { syncSessionClaims(authUser) }
    }

    private suspend fun resolveCurrentUserAfterAuth(
        authUser: FirebaseUser,
        preferredUsername: String? = null,
    ): User {
        return runCatching {
            syncUserDocument(authUser, preferredUsername)
        }.getOrElse {
            authUser.toSharedUser().copy(username = resolvedUsername(authUser, preferredUsername))
        }
    }

    private suspend fun ensureRegistrationsOpen() {
        val snapshot = firestore.collection("system").document("runtimeConfig").get().await()
        val data = snapshot.data.orEmpty()
        val registrationsEnabled = data["registrationsEnabled"] as? Boolean ?: true
        val lockdown = data["lockdown"] as? Boolean ?: false

        if (!registrationsEnabled || lockdown) {
            error("Registrierungen sind derzeit pausiert.")
        }
    }

    private suspend fun ensureRegistrationAllowedForBootstrap(
        authUser: FirebaseUser,
        bootstrapRole: UserRole,
    ) {
        if (bootstrapRole == UserRole.Owner) {
            return
        }

        runCatching {
            ensureRegistrationsOpen()
        }.getOrElse { error ->
            runCatching { authUser.delete().await() }
            throw error
        }
    }

    private suspend fun syncPublicProfileDocument(
        uid: String,
        username: String,
        profileImageUrl: String?,
        profileImagePath: String?,
        profileTagline: String?,
        profileBio: String?,
        instagramHandle: String?,
        whatsApp: String?,
    ) {
        try {
            val documentReference = firestore.collection("userProfiles").document(uid)
            val snapshot = documentReference.get().await()
            val createdAt = snapshot.data?.get("createdAt") ?: com.google.firebase.Timestamp.now()
            documentReference.set(
                mapOf(
                    "ownerUid" to uid,
                    "username" to username,
                    "profileImageURL" to (profileImageUrl ?: FieldValue.delete()),
                    "profileImagePath" to (profileImagePath ?: FieldValue.delete()),
                    "profileTagline" to (profileTagline ?: FieldValue.delete()),
                    "profileBio" to (profileBio ?: FieldValue.delete()),
                    "instagramHandle" to (instagramHandle ?: FieldValue.delete()),
                    "whatsApp" to (whatsApp ?: FieldValue.delete()),
                    "createdAt" to createdAt,
                    "updatedAt" to com.google.firebase.Timestamp.now(),
                ),
                com.google.firebase.firestore.SetOptions.merge(),
            ).await()
        } catch (error: FirebaseFirestoreException) {
            if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                return
            }
            throw error
        }
    }
}

private fun resolvedUsername(
    authUser: FirebaseUser,
    preferredUsername: String?,
): String {
    val fallbackEmail = authUser.email.orEmpty()
    val candidate = preferredUsername?.trim()?.takeIf { it.isNotEmpty() }
        ?: authUser.displayName?.takeIf { it.isNotBlank() }
        ?: fallbackEmail.substringBefore("@").ifBlank { "Skydown User" }
    return candidate.trim().take(32).trim().ifBlank { "Skydown User" }
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
            "ERROR_REQUIRES_RECENT_LOGIN" -> "Bitte melde dich erneut an, bevor du dein Konto loeschst."
            else -> null
        }
    }

    return IllegalStateException(firebaseMessage ?: message ?: "Authentifizierung fehlgeschlagen.")
}
