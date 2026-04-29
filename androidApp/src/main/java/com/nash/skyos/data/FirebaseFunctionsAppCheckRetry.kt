package com.nash.skyos.data

import android.content.pm.ApplicationInfo
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.HttpsCallableResult
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.delay

private const val APP_CHECK_DEBUG_PACKAGE_NAME = "com.nash.skyos"

suspend fun FirebaseFunctions.callWithAppCheckRetry(
    functionName: String,
    payload: Any,
): HttpsCallableResult {
    return try {
        getHttpsCallable(functionName).call(payload).await()
    } catch (error: FirebaseFunctionsException) {
        if (!error.shouldRetryWithFreshAppCheckToken()) {
            throw error
        }

        refreshAppCheckTokenForCallableRetry()
        getHttpsCallable(functionName).call(payload).await()
    }
}

private suspend fun refreshAppCheckTokenForCallableRetry() {
    // IMPORTANT:
    // Forcing a fresh token on every retry can trigger Play Integrity / provider rate limits ("Too many attempts").
    // Prefer cached token first, then force refresh with backoff.
    try {
        FirebaseAppCheck.getInstance().getToken(false).await()
        return
    } catch (_: Throwable) {
        // ignore and try forced refresh
    }

    val delaysMs = intArrayOf(200, 600, 1200)
    var lastError: Throwable? = null
    for (delayMs in delaysMs) {
        try {
            delay(delayMs.toLong())
            FirebaseAppCheck.getInstance().getToken(true).await()
            return
        } catch (error: Throwable) {
            lastError = error
            if (!error.isTooManyAttempts()) {
                throw error
            }
        }
    }

    throw lastError ?: IllegalStateException("App Check token refresh failed.")
}

private fun Throwable.isTooManyAttempts(): Boolean {
    val text = generateSequence<Throwable>(this) { it.cause }
        .flatMap { error ->
            sequenceOf(
                error.message,
                error.localizedMessage,
            )
        }
        .filterNotNull()
        .joinToString(" ")
        .lowercase()
    return text.contains("too many attempts")
}

fun Throwable.toAppCheckVerificationMessage(retryActionInstruction: String): String? {
    if (!isAppCheckVerificationFailure()) {
        return null
    }

    return if (isDebuggableApp()) {
        "App-Validierung fuer dieses Testgeraet fehlt. Bitte den Firebase App Check Debug-Token fuer $APP_CHECK_DEBUG_PACKAGE_NAME in der Firebase Console registrieren und dann $retryActionInstruction."
    } else {
        "Die App-Validierung konnte nicht bestaetigt werden. Bitte spaeter erneut versuchen."
    }
}

private fun FirebaseFunctionsException.shouldRetryWithFreshAppCheckToken(): Boolean {
    val text = localizedMessage.orEmpty().lowercase()
    val looksLikeAppCheck =
        text.contains("app check") ||
            text.contains("app-check") ||
            text.contains("app verification") ||
            text.contains("missing app check token")

    // Some backends surface App Check failures as INTERNAL with a localized DE/EN message.
    return looksLikeAppCheck && (
        code == FirebaseFunctionsException.Code.FAILED_PRECONDITION ||
            code == FirebaseFunctionsException.Code.INTERNAL
        )
}

private fun Throwable.isAppCheckVerificationFailure(): Boolean {
    val text = appCheckDiagnosticText()
    return text.contains("app check") ||
        text.contains("app-check") ||
        text.contains("app verification") ||
        text.contains("verification missing") ||
        text.contains("missing app check token") ||
        text.contains("placeholder token") ||
        text.contains("app attestation failed") ||
        text.contains("too many attempts")
}

private fun Throwable.appCheckDiagnosticText(): String {
    return generateSequence(this) { current -> current.cause }
        .flatMap { error -> sequenceOf(error.message, error.localizedMessage) }
        .filterNotNull()
        .joinToString(" ")
        .lowercase()
}

private fun isDebuggableApp(): Boolean {
    return runCatching {
        val flags = FirebaseApp.getInstance().applicationContext.applicationInfo.flags
        (flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }.getOrDefault(false)
}
