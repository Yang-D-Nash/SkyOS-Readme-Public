package com.skydown.android.ui.screen

import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes

@Suppress("DEPRECATION")
internal fun ApiException.toReadableGoogleMessage(): String {
    return when (statusCode) {
        GoogleSignInStatusCodes.SIGN_IN_CANCELLED,
        CommonStatusCodes.CANCELED -> "Google-Anmeldung wurde abgebrochen."
        CommonStatusCodes.NETWORK_ERROR -> "Netzwerkfehler bei Google-Anmeldung. Bitte erneut versuchen."
        CommonStatusCodes.DEVELOPER_ERROR -> {
            "Google-Anmeldung ist fuer Android noch nicht korrekt konfiguriert. " +
                "In Firebase fehlt sehr wahrscheinlich die Android-SHA-1/SHA-256 fuer com.skydown.android."
        }
        GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS -> "Google-Anmeldung laeuft bereits."
        GoogleSignInStatusCodes.SIGN_IN_FAILED -> "Google-Anmeldung ist fehlgeschlagen."
        CommonStatusCodes.INTERNAL_ERROR -> "Interner Google-Fehler. Bitte App neu starten."
        else -> "Google-Anmeldung fehlgeschlagen: ${localizedMessage ?: statusCode}"
    }
}
