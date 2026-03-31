package com.skydown.android

import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skydown.android.data.AppearancePreferences
import com.skydown.android.data.AppFeatureFlagsStore
import com.skydown.android.data.AiVisualReferenceLibraryPreferences
import com.skydown.android.data.SpotifyAuthManager
import com.skydown.android.data.WorkflowAutomationPreferences
import com.skydown.android.ui.SkydownApp
import com.skydown.android.ui.theme.AppearanceMode
import com.skydown.android.ui.theme.SkydownTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppearancePreferences.initialize(applicationContext)
        AppFeatureFlagsStore.initialize()
        AiVisualReferenceLibraryPreferences.initialize(applicationContext)
        WorkflowAutomationPreferences.initialize(applicationContext)
        SpotifyAuthManager.initialize(applicationContext)
        handleSpotifyRedirect(intent?.data)
        volumeControlStream = AudioManager.STREAM_MUSIC
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
        handleSpotifyRedirect(intent.data)
    }

    private fun handleSpotifyRedirect(uri: android.net.Uri?) {
        if (uri?.scheme == "com.skydown.android" && uri.host == "spotify-auth") {
            lifecycleScope.launch {
                SpotifyAuthManager.handleRedirect(uri)
            }
        }
    }
}
