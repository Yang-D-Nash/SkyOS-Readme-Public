package com.nash.skyos

import android.app.Application
import android.content.pm.ApplicationInfo
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.nash.skyos.data.AppNetworkMonitor
import com.nash.skyos.data.AppTextResolver
import com.nash.skyos.data.AgentPendingQueueStore
import com.nash.skyos.data.ManusByosPreferences

class SkydownApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppNetworkMonitor.initialize(this)
        AppTextResolver.initialize(this)
        AgentPendingQueueStore.initialize(this)
        ManusByosPreferences.initialize(this)
        runCatching {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }

            val isDebuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
            val providerFactory = if (isDebuggable) {
                debugAppCheckProviderFactory()
            } else {
                PlayIntegrityAppCheckProviderFactory.getInstance()
            }

            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(providerFactory)
            Log.i(
                "SkydownApplication",
                if (isDebuggable) {
                    "Firebase App Check laeuft im Debug-Provider-Modus."
                } else {
                    "Firebase App Check laeuft mit Play Integrity."
                },
            )
        }.onFailure { error ->
            Log.e("SkydownApplication", "Firebase App Check konnte nicht initialisiert werden.", error)
        }

        configureFirestoreCache()
    }

    private fun debugAppCheckProviderFactory(): AppCheckProviderFactory {
        return runCatching {
            val factoryClass = Class.forName("com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory")
            val getInstance = factoryClass.getMethod("getInstance")
            getInstance.invoke(null) as AppCheckProviderFactory
        }.getOrElse { error ->
            Log.w(
                "SkydownApplication",
                "Firebase App Check Debug-Provider fehlt; Debug-Build faellt auf Play Integrity zurueck.",
                error,
            )
            PlayIntegrityAppCheckProviderFactory.getInstance()
        }
    }

    private fun configureFirestoreCache() {
        runCatching {
            val cacheSettings = PersistentCacheSettings.newBuilder()
                .setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build()

            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(cacheSettings)
                .build()

            FirebaseFirestore.getInstance().firestoreSettings = settings
        }.onFailure { error ->
            Log.w("SkydownApplication", "Firestore-Cache konnte nicht konfiguriert werden.", error)
        }
    }
}
