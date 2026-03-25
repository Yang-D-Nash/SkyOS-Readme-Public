package com.skydown.android

import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.skydown.android.data.SpotifyAuthManager
import com.skydown.android.ui.SkydownApp
import com.skydown.android.ui.theme.SkydownTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SpotifyAuthManager.initialize(applicationContext)
        handleSpotifyRedirect(intent?.data)
        volumeControlStream = AudioManager.STREAM_MUSIC
        setContent {
            SkydownTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
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
