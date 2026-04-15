package com.skydown.android.data

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.HttpsCallableResult
import kotlinx.coroutines.tasks.await

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

private fun FirebaseFunctionsException.shouldRetryWithFreshAppCheckToken(): Boolean {
    if (code != FirebaseFunctionsException.Code.FAILED_PRECONDITION) {
        return false
    }

    val text = localizedMessage.orEmpty().lowercase()
    return text.contains("app check") || text.contains("validierung")
}
