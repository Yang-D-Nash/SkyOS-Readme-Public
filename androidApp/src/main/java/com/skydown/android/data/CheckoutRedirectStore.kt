package com.skydown.android.data

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class CheckoutRedirectStatus {
    Success,
    Cancel,
}

data class CheckoutRedirectEvent(
    val status: CheckoutRedirectStatus,
    val orderId: String? = null,
    val sessionId: String? = null,
)

object CheckoutRedirectStore {
    private val _latestEvent = MutableStateFlow<CheckoutRedirectEvent?>(null)
    val latestEvent: StateFlow<CheckoutRedirectEvent?> = _latestEvent

    fun handle(uri: Uri?): Boolean {
        val normalizedUri = uri ?: return false
        if (normalizedUri.scheme != "com.skydown.android") {
            return false
        }

        val status = when (normalizedUri.host) {
            "checkout-success" -> CheckoutRedirectStatus.Success
            "checkout-cancel" -> CheckoutRedirectStatus.Cancel
            else -> return false
        }

        _latestEvent.value = CheckoutRedirectEvent(
            status = status,
            orderId = normalizedUri.getQueryParameter("orderId")?.takeIf { it.isNotBlank() },
            sessionId = normalizedUri.getQueryParameter("sessionId")?.takeIf { it.isNotBlank() },
        )
        return true
    }

    fun clear() {
        _latestEvent.value = null
    }
}
