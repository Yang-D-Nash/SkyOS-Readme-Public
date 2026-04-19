package com.skydown.android

import android.app.Application
import android.content.pm.ApplicationInfo
import android.os.Build
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.skydown.android.data.AppNetworkMonitor
import com.skydown.android.data.AppTextResolver
import com.skydown.android.data.AgentPendingQueueStore
import com.skydown.android.data.ManusByosPreferences

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
            val providerFactory = if (isDebuggable && isProbablyEmulator()) {
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

    private fun isProbablyEmulator(): Boolean {
        val fingerprint = Build.FINGERPRINT.lowercase()
        val model = Build.MODEL.lowercase()
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        val device = Build.DEVICE.lowercase()
        val product = Build.PRODUCT.lowercase()

        return fingerprint.startsWith("generic") ||
            fingerprint.contains("emulator") ||
            fingerprint.contains("vbox") ||
            model.contains("emulator") ||
            model.contains("android sdk built for") ||
            manufacturer.contains("genymotion") ||
            (brand.startsWith("generic") && device.startsWith("generic")) ||
            product.contains("sdk") ||
            product.contains("emulator") ||
            product.contains("simulator")
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
