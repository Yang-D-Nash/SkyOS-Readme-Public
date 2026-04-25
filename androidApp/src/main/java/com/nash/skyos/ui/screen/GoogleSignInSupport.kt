@file:Suppress("DEPRECATION")

package com.nash.skyos.ui.screen

import android.app.Activity
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes

@Suppress("DEPRECATION")
internal fun ApiException.toReadableGoogleMessage(): String {
    return when (statusCode) {
        GoogleSignInStatusCodes.SIGN_IN_CANCELLED,
        CommonStatusCodes.CANCELED -> "Google-Anmeldung wurde abgebrochen."
        CommonStatusCodes.NETWORK_ERROR -> "Netzwerkfehler bei Google-Anmeldung. Bitte erneut versuchen."
        CommonStatusCodes.DEVELOPER_ERROR -> "Google-Anmeldung ist nicht korrekt konfiguriert (Android SHA). Bitte App-Update nutzen oder Support kontaktieren."
        GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS -> "Google-Anmeldung laeuft bereits."
        GoogleSignInStatusCodes.SIGN_IN_FAILED -> "Google-Anmeldung ist fehlgeschlagen."
        CommonStatusCodes.INTERNAL_ERROR -> "Interner Google-Fehler. Bitte App neu starten."
        else -> "Google-Anmeldung fehlgeschlagen: ${localizedMessage ?: statusCode}"
    }
}

internal fun readableGoogleFallbackForActivityResult(resultCode: Int): String {
    return when (resultCode) {
        Activity.RESULT_CANCELED -> "Google-Anmeldung wurde abgebrochen."
        Activity.RESULT_OK -> "Google-Anmeldung konnte nicht abgeschlossen werden."
        else -> "Google-Anmeldung ist fehlgeschlagen (Code $resultCode)."
    }
}
