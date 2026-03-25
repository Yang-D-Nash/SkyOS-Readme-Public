package com.skydown.android.data

import android.content.Context
import android.net.Uri
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
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

object SpotifyAuthManager {
    private const val clientId = "e22e102e5cd847bb8f59a140fda76bcc"
    private const val redirectUri = "com.skydown.android://spotify-auth"
    private const val authorizeUrl = "https://accounts.spotify.com/authorize"
    private const val tokenUrl = "https://accounts.spotify.com/api/token"

    private const val prefsName = "spotify_auth"
    private const val keyAccessToken = "access_token"
    private const val keyRefreshToken = "refresh_token"
    private const val keyExpiry = "expiry_epoch_millis"
    private const val keyState = "pkce_state"
    private const val keyVerifier = "pkce_verifier"

    private lateinit var appContext: Context
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

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
            .putString(keyVerifier, verifier)
            .apply()

        return Uri.parse(authorizeUrl).buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", redirectUri)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("state", state)
            .appendQueryParameter("show_dialog", "false")
            .build()
    }

    suspend fun handleRedirect(uri: Uri): Result<Unit> = runCatching {
        val expectedState = preferences().getString(keyState, null)
        val receivedState = uri.getQueryParameter("state")
        require(expectedState != null && expectedState == receivedState) { "Spotify-Callback ist ungueltig." }

        val code = uri.getQueryParameter("code")
            ?: error("Spotify hat keinen Code geliefert.")

        val verifier = preferences().getString(keyVerifier, null)
            ?: error("Spotify-PKCE-Verifier fehlt.")

        val tokenResponse = exchangeCodeForToken(code, verifier)
        saveTokenResponse(tokenResponse)
    }

    suspend fun validAccessToken(): String? {
        if (hasValidAccessToken()) {
            return preferences().getString(keyAccessToken, null)
        }

        val refreshToken = preferences().getString(keyRefreshToken, null) ?: return null
        return runCatching {
            val tokenResponse = refreshAccessToken(refreshToken)
            saveTokenResponse(tokenResponse)
            tokenResponse.accessToken
        }.getOrNull()
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
            .putString(keyAccessToken, tokenResponse.accessToken)
            .putString(keyRefreshToken, tokenResponse.refreshToken ?: preferences().getString(keyRefreshToken, null))
            .putLong(keyExpiry, System.currentTimeMillis() + (tokenResponse.expiresIn * 1000L))
            .apply()
        _isConnected.value = true
    }

    private fun hasValidAccessToken(): Boolean {
        return preferences().getString(keyAccessToken, null) != null &&
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

    private data class TokenResponse(
        val accessToken: String,
        val refreshToken: String?,
        val expiresIn: Int,
    )
}
