package com.skydown.android

import android.Manifest
import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.media3.common.util.UnstableApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skydown.android.data.AppearancePreferences
import com.skydown.android.data.AppFeatureFlagsStore
import com.skydown.android.data.AiConversationHistoryStore
import com.skydown.android.data.AiVisualReferenceLibraryPreferences
import com.skydown.android.data.CheckoutRedirectStore
import com.skydown.android.data.NotificationPermissionCoordinator
import com.skydown.android.data.SpotifyAuthManager
import com.skydown.android.data.WorkflowAutomationPreferences
import com.skydown.android.ui.SkydownApp
import com.skydown.android.ui.theme.AppearanceMode
import com.skydown.android.ui.theme.SkydownTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        NotificationPermissionCoordinator.markPrompted(this)
    }

    @UnstableApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppearancePreferences.initialize(applicationContext)
        AppFeatureFlagsStore.initialize()
        AiConversationHistoryStore.initialize(applicationContext)
        AiVisualReferenceLibraryPreferences.initialize(applicationContext)
        SpotifyAuthManager.initialize(applicationContext)
        handleIncomingLink(intent?.data)
        volumeControlStream = AudioManager.STREAM_MUSIC
        maybeRequestNotificationPermissionOnLaunch()
        lifecycleScope.launch {
            AppFeatureFlagsStore.refresh()
        }
        setContent {
            val appearanceMode by AppearancePreferences.appearanceMode.collectAsStateWithLifecycle()
            val darkTheme = when (appearanceMode) {
                AppearanceMode.Light -> false
                AppearanceMode.Dark -> true
                AppearanceMode.System -> isSystemInDarkTheme()
            }

            SkydownTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    SkydownApp()
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleIncomingLink(intent.data)
    }

    private fun handleIncomingLink(uri: android.net.Uri?) {
        if (CheckoutRedirectStore.handle(uri)) {
            return
        }

        if (uri?.scheme == "com.skydown.android" && uri.host == "spotify-auth") {
            lifecycleScope.launch {
                SpotifyAuthManager.handleRedirect(uri)
            }
        }
    }

    private fun maybeRequestNotificationPermissionOnLaunch() {
        if (!NotificationPermissionCoordinator.shouldRequestOnLaunch(this)) {
            return
        }
        NotificationPermissionCoordinator.markPrompted(this)
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
