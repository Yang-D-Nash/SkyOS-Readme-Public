package com.nash.skyos

import android.Manifest
import android.content.pm.ApplicationInfo
import android.media.AudioManager
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.media3.common.util.UnstableApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nash.skyos.data.AppearancePreferences
import com.nash.skyos.data.AppContainer
import com.nash.skyos.data.AppFeatureFlagsStore
import com.nash.skyos.data.AiAccessMode
import com.nash.skyos.data.AiConversationHistoryStore
import com.nash.skyos.data.AiVisualReferenceLibraryPreferences
import com.nash.skyos.data.CheckoutRedirectStore
import com.nash.skyos.data.NotificationPermissionCoordinator
import com.nash.skyos.data.AppSessionStore
import com.nash.skyos.data.MembershipAnalyticsTracker
import com.nash.skyos.data.SpotifyAuthManager
import com.nash.skyos.ui.SkydownApp
import com.nash.skyos.ui.theme.AppearanceMode
import com.nash.skyos.ui.theme.SkydownTheme
import com.skydown.shared.model.User
import com.skydown.shared.model.UserQuotaPlan
import com.skydown.shared.model.UserRole
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    companion object {
        const val EXTRA_UI_TEST_SKIP_INTRO = "ui_test_skip_intro"
        const val EXTRA_UI_TEST_START_ROUTE = "ui_test_start_route"
        const val EXTRA_UI_TEST_USE_MOCK_MERCH = "ui_test_use_mock_merch"
        const val EXTRA_UI_TEST_USE_MOCK_MUSIC = "ui_test_use_mock_music"
        const val EXTRA_UI_TEST_USE_MOCK_VIDEO_HUB = "ui_test_use_mock_video_hub"
        const val EXTRA_UI_TEST_USE_MOCK_AI_VISUAL = "ui_test_use_mock_ai_visual"
        const val EXTRA_UI_TEST_SIGNED_IN_USER = "ui_test_signed_in_user"
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        NotificationPermissionCoordinator.markPrompted(this)
    }

    @OptIn(ExperimentalLayoutApi::class)
    @UnstableApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uiTestLaunchOptionsEnabled =
            (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        val skipIntroForUiTest = uiTestLaunchOptionsEnabled &&
            intent?.getBooleanExtra(EXTRA_UI_TEST_SKIP_INTRO, false) == true
        val startRouteForUiTest = if (uiTestLaunchOptionsEnabled) {
            intent?.getStringExtra(EXTRA_UI_TEST_START_ROUTE)
        } else {
            null
        }
        val useMockMerchForUiTest = uiTestLaunchOptionsEnabled &&
            intent?.getBooleanExtra(EXTRA_UI_TEST_USE_MOCK_MERCH, false) == true
        val useMockMusicForUiTest = uiTestLaunchOptionsEnabled &&
            intent?.getBooleanExtra(EXTRA_UI_TEST_USE_MOCK_MUSIC, false) == true
        val useMockVideoHubForUiTest = uiTestLaunchOptionsEnabled &&
            intent?.getBooleanExtra(EXTRA_UI_TEST_USE_MOCK_VIDEO_HUB, false) == true
        val useMockAiVisualForUiTest = uiTestLaunchOptionsEnabled &&
            intent?.getBooleanExtra(EXTRA_UI_TEST_USE_MOCK_AI_VISUAL, false) == true
        val useSignedInUserForUiTest = uiTestLaunchOptionsEnabled &&
            intent?.getBooleanExtra(EXTRA_UI_TEST_SIGNED_IN_USER, false) == true
        val uiTestUser = if (useSignedInUserForUiTest) {
            createUiTestSignedInUser()
        } else {
            null
        }
        val isUiTestLaunch =
            skipIntroForUiTest ||
                startRouteForUiTest != null ||
                useMockMerchForUiTest ||
                useMockMusicForUiTest ||
                useMockVideoHubForUiTest ||
                useMockAiVisualForUiTest ||
                uiTestUser != null
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(
                AndroidColor.TRANSPARENT,
                AndroidColor.TRANSPARENT,
            ),
        )
        AppearancePreferences.initialize(applicationContext)
        AppFeatureFlagsStore.configureUiTestOverrides(
            aiEnabled = if (useMockAiVisualForUiTest || uiTestUser != null) true else null,
            aiAccessMode = if (useMockAiVisualForUiTest || uiTestUser != null) AiAccessMode.SignedIn else null,
        )
        AppFeatureFlagsStore.initialize()
        MembershipAnalyticsTracker(this).track("app_open", surface = "app_start")
        AiConversationHistoryStore.initialize(applicationContext)
        AiVisualReferenceLibraryPreferences.initialize(applicationContext)
        SpotifyAuthManager.initialize(applicationContext)
        if (isUiTestLaunch) {
            AppSessionStore.update(uiTestUser)
        }
        AppContainer.configureUiTestMode(
            useMockMerchandise = useMockMerchForUiTest,
            useMockMusic = useMockMusicForUiTest,
            useMockVideoHub = useMockVideoHubForUiTest,
            useMockAiVisual = useMockAiVisualForUiTest,
            currentUserOverride = uiTestUser,
        )
        handleIncomingLink(intent?.data)
        volumeControlStream = AudioManager.STREAM_MUSIC
        if (!isUiTestLaunch) {
            maybeRequestNotificationPermissionOnLaunch()
        }
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
                    color = Color.Transparent,
                ) {
                    SkydownApp(
                        startRouteOverride = startRouteForUiTest,
                        skipIntro = skipIntroForUiTest,
                    )
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

        if (uri?.scheme == "com.nash.skyos" && uri.host == "spotify-auth") {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun createUiTestSignedInUser(): User {
        return User(
            id = "ui-test-user",
            email = "creator@skydown.app",
            username = "SkyOS Creator",
            registrationDateEpochMillis = 1_710_000_000_000,
            isAdmin = false,
            role = UserRole.User.rawValue,
            quotaPlan = UserQuotaPlan.Free.rawValue,
            aiAccessEnabled = true,
            aiTextRequestsPerDay = 30,
            aiVisualRequestsPerDay = 4,
            aiAgentRequestsPerDay = 18,
            aiHistoryRetentionDays = 3,
        )
    }
}
