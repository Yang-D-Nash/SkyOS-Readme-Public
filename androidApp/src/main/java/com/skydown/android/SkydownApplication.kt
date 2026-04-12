package com.skydown.android

import android.app.Application
import android.content.pm.ApplicationInfo
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.skydown.android.data.AppNetworkMonitor

class SkydownApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppNetworkMonitor.initialize(this)
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
        }.getOrElse {
            PlayIntegrityAppCheckProviderFactory.getInstance()
        }
    }

    private fun configureFirestoreCache() {
        runCatching {
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build()

            FirebaseFirestore.getInstance().firestoreSettings = settings
        }.onFailure { error ->
            Log.w("SkydownApplication", "Firestore-Cache konnte nicht konfiguriert werden.", error)
        }
    }
}
