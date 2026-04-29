package com.nash.skyos

import android.app.Application
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.nash.skyos.data.AppNetworkMonitor
import com.nash.skyos.data.AppTextResolver
import com.nash.skyos.data.AgentPendingQueueStore
import com.nash.skyos.data.ManusByosPreferences
import com.nash.skyos.data.PushTokenRegistry

class SkydownApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppNetworkMonitor.initialize(this)
        AppTextResolver.initialize(this)
        AgentPendingQueueStore.initialize(this)
        ManusByosPreferences.initialize(this)
        PushTokenRegistry.initialize(this)
        // App Check installs in [FirebaseAppCheckInitProvider] before FirebaseInitProvider.

        configureFirestoreCache()
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
