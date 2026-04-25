package com.nash.skyos.data

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class ManusByosSettings(
    val isEnabled: Boolean = false,
    val hasApiKey: Boolean = false,
)

object ManusByosPreferences {
    private const val preferencesName = "manus_byos_preferences"
    private const val enabledPrefix = "enabled_"
    private const val cipherPrefix = "cipher_"
    private const val ivPrefix = "iv_"
    private const val keyAlias = "com.skydown.manus.byos"
    private const val keyStoreProvider = "AndroidKeyStore"
    private const val cipherTransformation = "AES/GCM/NoPadding"
    private const val gcmTagLengthBits = 128

    private lateinit var sharedPreferences: SharedPreferences
    private var currentUserId: String? = null
    private val _settings = MutableStateFlow(ManusByosSettings())
    val settings: StateFlow<ManusByosSettings> = _settings.asStateFlow()

    fun initialize(context: Context) {
        if (::sharedPreferences.isInitialized) {
            return
        }
        sharedPreferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    fun setUserMode(userId: String?) {
        ensureInitialized()
        val normalizedUserId = normalizeUserId(userId)
        if (normalizedUserId == currentUserId) {
            return
        }
        currentUserId = normalizedUserId
        _settings.value = normalizedUserId
            ?.let(::readSettingsForUser)
            ?: ManusByosSettings()
    }

    fun updateEnabled(isEnabled: Boolean): Result<ManusByosSettings> {
        return runCatching {
            ensureInitialized()
            val userId = requireActiveUserId()
            val existing = readSettingsForUser(userId)
            require(!isEnabled || existing.hasApiKey) {
                "Bitte hinterlege zuerst einen Manus API Key."
            }

            sharedPreferences.edit()
                .putBoolean(enabledKey(userId), isEnabled)
                .apply()

            val updated = readSettingsForUser(userId).copy(isEnabled = isEnabled && existing.hasApiKey)
            _settings.value = updated
            updated
        }
    }

    fun saveApiKey(rawApiKey: String): Result<ManusByosSettings> {
        return runCatching {
            ensureInitialized()
            val userId = requireActiveUserId()
            val apiKey = rawApiKey.trim()
            require(apiKey.isNotEmpty()) {
                "Bitte gib einen Manus API Key ein."
            }
            require(apiKey.length <= 1024) {
                "Der Manus API Key ist zu lang."
            }

            val encrypted = encryptApiKey(apiKey)
            sharedPreferences.edit()
                .putString(cipherKey(userId), encrypted.cipherTextBase64)
                .putString(ivKey(userId), encrypted.ivBase64)
                .putBoolean(enabledKey(userId), true)
                .apply()

            val updated = ManusByosSettings(
                isEnabled = true,
                hasApiKey = true,
            )
            _settings.value = updated
            updated
        }
    }

    fun clearApiKey(): Result<ManusByosSettings> {
        return runCatching {
            ensureInitialized()
            val userId = requireActiveUserId()
            sharedPreferences.edit()
                .remove(cipherKey(userId))
                .remove(ivKey(userId))
                .putBoolean(enabledKey(userId), false)
                .apply()

            val updated = ManusByosSettings(
                isEnabled = false,
                hasApiKey = false,
            )
            _settings.value = updated
            updated
        }
    }

    fun currentManusApiKeyOrNull(): String? {
        return runCatching {
            ensureInitialized()
            val userId = currentUserId ?: return null
            val currentSettings = readSettingsForUser(userId)
            if (!currentSettings.isEnabled || !currentSettings.hasApiKey) {
                return null
            }
            decryptApiKey(userId)?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private fun requireActiveUserId(): String {
        return requireNotNull(currentUserId) {
            "Kein aktiver User fuer Manus BYOS gefunden."
        }
    }

    private fun readSettingsForUser(userId: String): ManusByosSettings {
        val hasApiKey = hasStoredApiKey(userId)
        val isEnabled = sharedPreferences.getBoolean(enabledKey(userId), false) && hasApiKey
        return ManusByosSettings(
            isEnabled = isEnabled,
            hasApiKey = hasApiKey,
        )
    }

    private fun hasStoredApiKey(userId: String): Boolean {
        val cipherText = sharedPreferences.getString(cipherKey(userId), null).orEmpty()
        val iv = sharedPreferences.getString(ivKey(userId), null).orEmpty()
        return cipherText.isNotBlank() && iv.isNotBlank()
    }

    private fun encryptApiKey(apiKey: String): EncryptedApiKey {
        val cipher = Cipher.getInstance(cipherTransformation)
        cipher.init(Cipher.ENCRYPT_MODE, resolveSecretKey())
        val encryptedBytes = cipher.doFinal(apiKey.toByteArray(StandardCharsets.UTF_8))
        return EncryptedApiKey(
            cipherTextBase64 = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP),
            ivBase64 = Base64.encodeToString(cipher.iv, Base64.NO_WRAP),
        )
    }

    private fun decryptApiKey(userId: String): String? {
        val cipherTextBase64 = sharedPreferences.getString(cipherKey(userId), null).orEmpty()
        val ivBase64 = sharedPreferences.getString(ivKey(userId), null).orEmpty()
        if (cipherTextBase64.isBlank() || ivBase64.isBlank()) {
            return null
        }

        val cipherTextBytes = Base64.decode(cipherTextBase64, Base64.NO_WRAP)
        val ivBytes = Base64.decode(ivBase64, Base64.NO_WRAP)

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

    private fun enabledKey(userId: String): String = "$enabledPrefix$userId"
    private fun cipherKey(userId: String): String = "$cipherPrefix$userId"
    private fun ivKey(userId: String): String = "$ivPrefix$userId"

    private fun normalizeUserId(userId: String?): String? {
        val trimmed = userId?.trim().orEmpty()
        if (trimmed.isBlank()) {
            return null
        }
        return trimmed
            .lowercase()
            .replace(Regex("[^a-z0-9_.-]"), "_")
            .ifBlank { null }
    }

    private fun ensureInitialized() {
        check(::sharedPreferences.isInitialized) {
            "ManusByosPreferences.initialize(context) must be called before usage."
        }
    }
}

private data class EncryptedApiKey(
    val cipherTextBase64: String,
    val ivBase64: String,
)
