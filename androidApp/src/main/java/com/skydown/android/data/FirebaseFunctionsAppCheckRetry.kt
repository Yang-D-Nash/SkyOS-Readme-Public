package com.skydown.android.data

import android.content.pm.ApplicationInfo
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.FirebaseApp
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.HttpsCallableResult
import kotlinx.coroutines.tasks.await

private const val APP_CHECK_DEBUG_PACKAGE_NAME = "com.skydown.android"

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

        FirebaseAppCheck.getInstance().getToken(true).await()
        getHttpsCallable(functionName).call(payload).await()
    }
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
    if (code != FirebaseFunctionsException.Code.FAILED_PRECONDITION) {
        return false
    }

    val text = localizedMessage.orEmpty().lowercase()
    return text.contains("app check") ||
        text.contains("app-check") ||
        text.contains("app verification") ||
        text.contains("missing app check token")
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
