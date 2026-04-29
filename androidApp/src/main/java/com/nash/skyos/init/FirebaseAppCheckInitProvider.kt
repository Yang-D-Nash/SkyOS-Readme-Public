package com.nash.skyos.init

import android.content.ContentProvider
import android.content.ContentValues
import android.content.pm.ApplicationInfo
import android.database.Cursor
import android.net.Uri
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

/**
 * Runs before [com.google.firebase.provider.FirebaseInitProvider] (higher [android:initOrder]) so
 * App Check uses the debug provider on debuggable builds instead of briefly defaulting to
 * attestation paths that fail on emulators ("App attestation failed", placeholder tokens).
 */
class FirebaseAppCheckInitProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        val appContext = context ?: return true
        runCatching {
            if (FirebaseApp.getApps(appContext).isEmpty()) {
                FirebaseApp.initializeApp(appContext)
            }

            val isDebuggable = (appContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
            val providerFactory =
                if (isDebuggable) {
                    debugAppCheckProviderFactory()
                } else {
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                }

            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(providerFactory)
            val modeLog =
                if (isDebuggable) {
                    "Firebase App Check (early): Debug-Provider-Modus."
                } else {
                    "Firebase App Check (early): Play Integrity."
                }
            Log.i(TAG, modeLog)
        }.onFailure { error ->
            Log.e(TAG, "Firebase App Check Early-Init fehlgeschlagen.", error)
        }
        return true
    }

    private fun debugAppCheckProviderFactory(): AppCheckProviderFactory {
        return runCatching {
            val factoryClass = Class.forName("com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory")
            val getInstance = factoryClass.getMethod("getInstance")
            getInstance.invoke(null) as AppCheckProviderFactory
        }.getOrElse { error ->
            Log.w(
                TAG,
                "Firebase App Check Debug-Provider fehlt; Debug-Build faellt auf Play Integrity zurueck.",
                error,
            )
            PlayIntegrityAppCheckProviderFactory.getInstance()
        }
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    private companion object {
        private const val TAG = "FirebaseAppCheckInit"
    }
}
