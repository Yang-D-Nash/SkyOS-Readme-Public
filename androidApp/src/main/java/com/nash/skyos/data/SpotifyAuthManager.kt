package com.nash.skyos.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.nash.skyos.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object SpotifyAuthManager {
    private const val clientId = "e22e102e5cd847bb8f59a140fda76bcc"
    private const val redirectUri = "com.nash.skyos://spotify-auth"
    private const val authorizeUrl = "https://accounts.spotify.com/authorize"
    private const val tokenUrl = "https://accounts.spotify.com/api/token"

    private const val prefsName = "spotify_auth"
    private const val keyAccessToken = "access_token"
    private const val keyRefreshToken = "refresh_token"
    private const val keyExpiry = "expiry_epoch_millis"
    private const val keyState = "pkce_state"
    private const val keyVerifier = "pkce_verifier"
    private const val encryptedValuePrefix = "enc:v1:"
    private const val encryptedValueSeparator = ":"
    private const val keyAlias = "com.skydown.spotify.oauth"
    private const val keyStoreProvider = "AndroidKeyStore"
    private const val cipherTransformation = "AES/GCM/NoPadding"
    private const val gcmTagLengthBits = 128

    private lateinit var appContext: Context
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    private val _lastErrorMessage = MutableStateFlow<String?>(null)
    val lastErrorMessage: StateFlow<String?> = _lastErrorMessage.asStateFlow()

    fun initialize(context: Context) {
        appContext = context.applicationContext
        _isConnected.value = hasValidAccessToken()
    }

    fun buildAuthorizationUri(): Uri {
        val state = java.util.UUID.randomUUID().toString()
        val verifier = randomVerifier()
        val challenge = codeChallenge(verifier)
        preferences()
            .edit()
            .putString(keyState, state)
            .putProtectedString(keyVerifier, verifier)
            .apply()
        _lastErrorMessage.value = null

        return Uri.parse(authorizeUrl).buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", redirectUri)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("state", state)
            .appendQueryParameter("show_dialog", "true")
            .build()
    }

    suspend fun handleRedirect(uri: Uri): Result<Unit> = runCatching {
        val expectedState = preferences().getString(keyState, null)
        val receivedState = uri.getQueryParameter("state")
        require(expectedState != null && expectedState == receivedState) { "Spotify-Callback ist ungueltig." }

        val code = uri.getQueryParameter("code")
            ?: error(AppTextResolver.string(R.string.spotify_error_code_missing))

        val verifier = protectedStringOrNull(keyVerifier)
            ?: error(AppTextResolver.string(R.string.spotify_error_pkce_verifier_missing))

        val tokenResponse = exchangeCodeForToken(code, verifier)
        saveTokenResponse(tokenResponse)
        _lastErrorMessage.value = null
    }.onFailure { error ->
        _lastErrorMessage.value = error.message ?: "Spotify konnte nicht verbunden werden."
    }

    suspend fun validAccessToken(): String? {
        if (hasValidAccessToken()) {
            return protectedStringOrNull(keyAccessToken)
        }

        val refreshToken = protectedStringOrNull(keyRefreshToken) ?: return null
        return runCatching {
            val tokenResponse = refreshAccessToken(refreshToken)
            saveTokenResponse(tokenResponse)
            tokenResponse.accessToken
        }.getOrNull()
    }

    fun disconnect() {
        preferences()
            .edit()
            .remove(keyAccessToken)
            .remove(keyRefreshToken)
            .remove(keyExpiry)
            .remove(keyState)
            .remove(keyVerifier)
            .apply()
        _isConnected.value = false
        _lastErrorMessage.value = null
    }

    fun clearError() {
        _lastErrorMessage.value = null
    }

    private suspend fun exchangeCodeForToken(code: String, verifier: String): TokenResponse {
        return performTokenRequest(
            mapOf(
                "grant_type" to "authorization_code",
                "code" to code,
                "redirect_uri" to redirectUri,
                "client_id" to clientId,
                "code_verifier" to verifier,
            ),
        )
    }

    private suspend fun refreshAccessToken(refreshToken: String): TokenResponse {
        return performTokenRequest(
            mapOf(
                "grant_type" to "refresh_token",
                "refresh_token" to refreshToken,
                "client_id" to clientId,
            ),
        )
    }

    private suspend fun performTokenRequest(parameters: Map<String, String>): TokenResponse {
        return withContext(Dispatchers.IO) {
            val connection = (URL(tokenUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            }

            val body = parameters.entries.joinToString("&") { (key, value) ->
                "${Uri.encode(key)}=${Uri.encode(value)}"
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(body)
            }

            val payload = (if (connection.responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }).bufferedReader().use(BufferedReader::readText)

            val json = JSONObject(payload)
            if (connection.responseCode !in 200..299) {
                error(json.optString("error_description", "Spotify-Token konnte nicht geladen werden."))
            }

            TokenResponse(
                accessToken = json.getString("access_token"),
                refreshToken = json.optString("refresh_token").takeIf { it.isNotBlank() },
                expiresIn = json.getInt("expires_in"),
            )
        }
    }

    private fun saveTokenResponse(tokenResponse: TokenResponse) {
        preferences()
            .edit()
            .putProtectedString(keyAccessToken, tokenResponse.accessToken)
            .putProtectedString(keyRefreshToken, tokenResponse.refreshToken ?: protectedStringOrNull(keyRefreshToken))
            .putLong(keyExpiry, System.currentTimeMillis() + (tokenResponse.expiresIn * 1000L))
            .apply()
        _isConnected.value = true
    }

    private fun hasValidAccessToken(): Boolean {
        return protectedStringOrNull(keyAccessToken) != null &&
            preferences().getLong(keyExpiry, 0L) > System.currentTimeMillis() + 60_000L
    }

    private fun randomVerifier(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun codeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray())
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    private fun preferences() = appContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    private fun SharedPreferences.Editor.putProtectedString(key: String, value: String?): SharedPreferences.Editor {
        return if (value.isNullOrBlank()) {
            remove(key)
        } else {
            putString(key, encryptProtectedString(value))
        }
    }

    private fun protectedStringOrNull(key: String): String? {
        return runCatching {
            val storedValue = preferences().getString(key, null) ?: return null
            if (storedValue.startsWith(encryptedValuePrefix)) {
                return decryptProtectedString(storedValue)
            }

            preferences()
                .edit()
                .putProtectedString(key, storedValue)
                .apply()
            storedValue
        }.getOrNull()
    }

    private fun encryptProtectedString(value: String): String {
        val cipher = Cipher.getInstance(cipherTransformation)
        cipher.init(Cipher.ENCRYPT_MODE, resolveSecretKey())
        val encryptedBytes = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
        val ivBase64 = Base64.getEncoder().encodeToString(cipher.iv)
        val cipherTextBase64 = Base64.getEncoder().encodeToString(encryptedBytes)
        return encryptedValuePrefix + ivBase64 + encryptedValueSeparator + cipherTextBase64
    }

    private fun decryptProtectedString(value: String): String? {
        val encryptedPayload = value.removePrefix(encryptedValuePrefix)
        val separatorIndex = encryptedPayload.indexOf(encryptedValueSeparator)
        if (separatorIndex <= 0 || separatorIndex >= encryptedPayload.lastIndex) {
            return null
        }

        val ivBytes = Base64.getDecoder().decode(encryptedPayload.substring(0, separatorIndex))
        val cipherTextBytes = Base64.getDecoder().decode(encryptedPayload.substring(separatorIndex + 1))
        val cipher = Cipher.getInstance(cipherTransformation)
        val parameterSpec = GCMParameterSpec(gcmTagLengthBits, ivBytes)
        cipher.init(Cipher.DECRYPT_MODE, resolveSecretKey(), parameterSpec)
        val decoded = cipher.doFinal(cipherTextBytes)
        return String(decoded, StandardCharsets.UTF_8)
    }

    private fun resolveSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(keyStoreProvider).apply { load(null) }
        val existing = keyStore.getKey(keyAlias, null) as? SecretKey
        if (existing != null) {
            return existing
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, keyStoreProvider)
        val spec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(false)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private data class TokenResponse(
        val accessToken: String,
        val refreshToken: String?,
        val expiresIn: Int,
    )
}
