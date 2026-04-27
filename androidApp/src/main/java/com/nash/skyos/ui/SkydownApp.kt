package com.nash.skyos.ui

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.media3.common.util.UnstableApi
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import coil3.compose.AsyncImage
import com.nash.skyos.R
import com.google.firebase.firestore.FirebaseFirestore
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nash.skyos.data.AppContainer
import com.nash.skyos.data.AppCartStore
import com.nash.skyos.data.AppFeatureFlagsStore
import com.nash.skyos.data.AppNetworkMonitor
import com.nash.skyos.data.AppSessionStore
import com.nash.skyos.data.ArtistPageBrand
import com.nash.skyos.data.MembershipAnalyticsTracker
import com.nash.skyos.ui.component.BrandArtwork
import com.nash.skyos.ui.component.BrandHeroCard
import com.nash.skyos.ui.component.BrandPill
import com.nash.skyos.ui.component.ConnectivityStatusBanner
import com.nash.skyos.ui.component.AppTopBarSessionActions
import com.nash.skyos.ui.component.LocalSessionUser
import com.nash.skyos.ui.component.SkydownExitEasing
import com.nash.skyos.ui.component.SkydownMotionTokens
import com.nash.skyos.ui.component.SkydownStandardEasing
import com.nash.skyos.ui.component.SkydownTopBarTitle
import com.nash.skyos.ui.component.rememberIsCompactAppLayout
import com.nash.skyos.ui.component.rememberUsesCompactVisualDensity
import com.nash.skyos.ui.component.skydownLuminousSweep
import com.nash.skyos.ui.component.skydownPanelSurface
import com.nash.skyos.ui.component.skydownPressable
import com.nash.skyos.ui.component.skydownSelectionFeedback
import com.nash.skyos.ui.component.skydownCapsuleSurface
import com.nash.skyos.ui.component.SkydownUiTokens
import com.nash.skyos.ui.component.skydownAtmosphereBackground
import com.nash.skyos.ui.component.skydownTopBarColors
import com.nash.skyos.ui.screen.AiHubScreen
import com.nash.skyos.ui.screen.ArtistPageScreen
import com.nash.skyos.ui.screen.CartScreen
import com.nash.skyos.ui.screen.HomeScreen
import com.nash.skyos.ui.screen.IntroScreen
import com.nash.skyos.ui.screen.LoginScreen
import com.nash.skyos.ui.screen.MusicScreen
import com.nash.skyos.ui.screen.NicmaProducerScreen
import com.nash.skyos.ui.screen.OwnerHubScreen
import com.nash.skyos.ui.screen.OrderScreen
import com.nash.skyos.ui.screen.ProfileScreen
import com.nash.skyos.ui.screen.RegistrationScreen
import com.nash.skyos.ui.screen.SettingsScreen
import com.nash.skyos.ui.screen.ShopScreen
import com.nash.skyos.ui.screen.VideoHubScreen
import com.nash.skyos.ui.screen.openExternalLink
import com.nash.skyos.ui.auth.AuthEntryContext
import com.nash.skyos.ui.theme.BackgroundDark
import com.nash.skyos.ui.theme.SkydownBodyCaptionTextStyle
import com.nash.skyos.ui.theme.SkydownCardTitleTextStyle
import com.nash.skyos.ui.theme.SkydownEditorialCaptionTextStyle
import com.nash.skyos.ui.theme.SkydownHeroEyebrowTextStyle
import com.nash.skyos.ui.theme.SpotifyGreen
import com.nash.skyos.ui.theme.skydownAccent
import com.nash.skyos.ui.theme.skydownAccentHighlight
import com.nash.skyos.ui.theme.skydownAccentMystic
import com.nash.skyos.ui.theme.skydownCardBackground
import com.nash.skyos.ui.theme.skydownCinematicShadow
import com.nash.skyos.ui.theme.skydownIsDarkPalette
import com.nash.skyos.ui.theme.skydownLuminanceLift
import com.nash.skyos.ui.theme.skydownSecondaryBackground
import com.nash.skyos.ui.theme.skydownSecondaryText
import com.nash.skyos.ui.theme.skydownText
import com.nash.skyos.ui.theme.skydownYoutube
import com.google.firebase.auth.FirebaseAuth
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@UnstableApi
@Composable
fun SkydownApp() {
    SkydownApp(
        startRouteOverride = null,
        skipIntro = false,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@UnstableApi
@Composable
fun SkydownApp(
    startRouteOverride: String?,
    skipIntro: Boolean,
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val isCompactLayout = rememberIsCompactAppLayout()
    val currentUser by AppContainer.currentUser.collectAsStateWithLifecycle()
    val aiAccessMode by AppFeatureFlagsStore.aiAccessMode.collectAsStateWithLifecycle()
    val isOnline by AppNetworkMonitor.isOnline.collectAsStateWithLifecycle()
    var showIntro by rememberSaveable(skipIntro) { mutableStateOf(!skipIntro) }
    var selectedEntryRoute by rememberSaveable(startRouteOverride) { mutableStateOf(startRouteOverride) }
    var showsWorkflowWorkspace by rememberSaveable { mutableStateOf(false) }
    var pendingAgentPrefillPrompt by rememberSaveable { mutableStateOf<String?>(null) }
    var authSheet by rememberSaveable { mutableStateOf<AuthSheet?>(null) }
    var authSheetLocked by rememberSaveable { mutableStateOf(false) }
    var showOrders by rememberSaveable { mutableStateOf(false) }
    var authEntryContext by remember { mutableStateOf(AuthEntryContext.DEFAULT) }
    var hasTrackedOnboardingStarted by rememberSaveable { mutableStateOf(false) }
    var hasTrackedOnboardingCompleted by rememberSaveable { mutableStateOf(false) }
    var hasTrackedFirstValueMoment by rememberSaveable { mutableStateOf(false) }
    var observedAuthUid by rememberSaveable { mutableStateOf<String?>(null) }
    var initialSettingsWorkspaceKey by rememberSaveable { mutableStateOf<String?>(null) }
    val auth = remember { FirebaseAuth.getInstance() }
    val growthTracker = remember(context) { MembershipAnalyticsTracker(context) }
    val coroutineScope = rememberCoroutineScope()
    val currentAuthSheetLocked by rememberUpdatedState(authSheetLocked)
    val currentSessionUserId = rememberUpdatedState(currentUser?.id)
    val authSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { targetValue ->
            !currentAuthSheetLocked || targetValue != SheetValue.Hidden
        },
    )
    val ordersSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val openSettings = remember(navController) {
        {
            initialSettingsWorkspaceKey = null
            navController.navigate("settings") {
                launchSingleTop = true
            }
        }
    }
    val openAutomationSettings = remember(navController) {
        {
            initialSettingsWorkspaceKey = "Automation"
            navController.navigate("settings") {
                launchSingleTop = true
            }
        }
    }
    val openProfile = remember(navController) {
        {
            navController.navigate("profile") {
                launchSingleTop = true
            }
        }
    }
    val openCart = remember(navController) {
        {
            navController.navigate("cart") {
                launchSingleTop = true
            }
        }
    }

    fun dismissAuthSheet() {
        if (!authSheetLocked) {
            authEntryContext = AuthEntryContext.DEFAULT
            authSheet = null
        }
    }

    fun openAuthLogin(context: AuthEntryContext) {
        authEntryContext = context
        authSheet = AuthSheet.Login
    }
    val hasAiAccess = AppFeatureFlagsStore.allowsAiAccess(
        user = currentUser,
        accessMode = aiAccessMode,
    )

    LaunchedEffect(currentUser?.id) {
        if (currentUser != null && authSheet != null) {
            authSheetLocked = false
            authEntryContext = AuthEntryContext.DEFAULT
            authSheet = null
        }
    }

    LaunchedEffect(authSheet) {
        if (authSheet == null && authSheetLocked) {
            authSheetLocked = false
        }
    }

    LaunchedEffect(showIntro) {
        if (showIntro && !hasTrackedOnboardingStarted) {
            hasTrackedOnboardingStarted = true
            growthTracker.track("onboarding_started", surface = "launch_intro")
        } else if (!showIntro && !hasTrackedOnboardingCompleted) {
            hasTrackedOnboardingCompleted = true
            growthTracker.track("onboarding_completed", surface = "launch_landing")
        }
    }

    // After leaving the main shell for the launch landing, drain the NavController so the next entry is clean.
    LaunchedEffect(selectedEntryRoute) {
        if (selectedEntryRoute == null) {
            while (navController.popBackStack()) {
                // Drain stack while the main shell is not composed.
            }
        }
    }

    fun trackFirstValueMoment(surface: String) {
        if (hasTrackedFirstValueMoment) return
        hasTrackedFirstValueMoment = true
        growthTracker.track("first_value_moment", surface = surface)
    }

    if (!AppContainer.isUiTestCurrentUserOverrideActive) {
        DisposableAuthSync(
            auth = auth,
            observedAuthUid = observedAuthUid,
            currentSessionUserId = currentSessionUserId.value,
            onObservedAuthUidChanged = { observedAuthUid = it },
            onSignedOut = {
                AppSessionStore.update(null)
                authSheetLocked = false
            },
            onRefreshCurrentUser = {
                coroutineScope.launch {
                    AppContainer.refreshCurrentUser()
                }
            },
        )
        DisposableSessionPrivilegeSync(
            auth = auth,
            currentSessionUser = currentUser,
            onRefreshCurrentUser = {
                coroutineScope.launch {
                    val currentAuthUser = auth.currentUser ?: return@launch
                    runCatching { currentAuthUser.getIdToken(true).await() }
                    AppContainer.refreshCurrentUser()
                }
            },
        )
    }

    CompositionLocalProvider(LocalSessionUser provides currentUser) {
        val cartItems by AppCartStore.items.collectAsState()
        val cartCount = cartItems.size
        val destinations = buildList {
            add(BottomDestination("shop", stringResource(R.string.tabs_merch), MaterialTheme.colorScheme.skydownAccentHighlight()) { _ ->
                BadgedBox(
                    badge = {
                        if (cartCount > 0) {
                            Badge { Text(cartCount.toString()) }
                        }
                    },
                ) {
                    Icon(Icons.Default.ShoppingBag, contentDescription = null)
                }
            })
            add(BottomDestination("music", stringResource(R.string.tabs_music), SpotifyGreen) { _ ->
                Icon(Icons.Default.GraphicEq, contentDescription = null)
            })
            add(BottomDestination("home", stringResource(R.string.tabs_home), MaterialTheme.colorScheme.skydownAccent()) { _ ->
                Icon(Icons.Default.Home, contentDescription = null)
            })
            add(BottomDestination("video", stringResource(R.string.tabs_videos), MaterialTheme.colorScheme.skydownYoutube()) { _ ->
                Icon(Icons.Default.PlayCircleFilled, contentDescription = null)
            })
            add(BottomDestination("ai", stringResource(R.string.tabs_tools), MaterialTheme.colorScheme.skydownAccentMystic()) { _ ->
                Icon(Icons.Default.AutoAwesome, contentDescription = null)
            })
        }

        if (showIntro) {
            IntroScreen(
                onFinished = { showIntro = false },
            )
        } else if (selectedEntryRoute == null) {
            Box(Modifier.fillMaxSize()) {
                val landingContext = LocalContext.current
                val pressBackAgainToExit = stringResource(R.string.launch_press_back_again_to_exit)
                var lastBackPressForExitMs by rememberSaveable { mutableStateOf(0L) }
                BackHandler {
                    val now = System.currentTimeMillis()
                    if (lastBackPressForExitMs != 0L && now - lastBackPressForExitMs <= 2_000L) {
                        landingContext.findComponentActivity()?.finish()
                    } else {
                        lastBackPressForExitMs = now
                        Toast.makeText(
                            landingContext,
                            pressBackAgainToExit,
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
                LaunchLandingScreen(
                    onOpenHome = {
                        trackFirstValueMoment("launch_entry_home")
                        selectedEntryRoute = "home"
                    },
                    onOpenMusic = {
                        trackFirstValueMoment("launch_entry_music")
                        selectedEntryRoute = "music"
                    },
                    onOpenVideography = {
                        trackFirstValueMoment("launch_entry_video")
                        selectedEntryRoute = "video"
                    },
                    onOpenShop = {
                        trackFirstValueMoment("launch_entry_shop")
                        selectedEntryRoute = "shop"
                    },
                )
            }
        } else {
            val startRoute = selectedEntryRoute ?: "home"
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val topLevelRoutes = remember(destinations) { destinations.map { it.route }.toSet() }
            var topLevelRouteHistory by rememberSaveable(startRoute) { mutableStateOf(listOf(startRoute)) }
            val showFloatingDock = skydownShouldShowDock(currentDestination?.route)
            val floatingDockContentPadding = if (showFloatingDock) {
                skydownDockOverlayPadding(isCompactLayout)
            } else {
                0.dp
            }

            fun navigateToTopLevel(route: String, recordHistory: Boolean = true) {
                if (recordHistory && topLevelRouteHistory.lastOrNull() != route) {
                    topLevelRouteHistory = (topLevelRouteHistory + route).takeLast(12)
                }
                if (navController.currentDestination?.route != route) {
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }

            fun navigateBackWithinTopLevel(): Boolean {
                if (topLevelRouteHistory.size <= 1) {
                    return false
                }
                val previousRoute = topLevelRouteHistory[topLevelRouteHistory.lastIndex - 1]
                topLevelRouteHistory = topLevelRouteHistory.dropLast(1)
                navigateToTopLevel(previousRoute, recordHistory = false)
                return true
            }

            val hasBlockingAuthSheet = authSheet != null
            val hasOrdersSheet = showOrders
            val canCloseWorkflowWorkspace = currentDestination?.route == "ai" && showsWorkflowWorkspace
            val canPopNestedRoute = navController.previousBackStackEntry != null &&
                !topLevelRoutes.contains(currentDestination?.route)
            val canPopTopLevelRoute = topLevelRoutes.contains(currentDestination?.route) &&
                topLevelRouteHistory.size > 1
            val onMainShellAtRoot = topLevelRoutes.contains(currentDestination?.route) &&
                topLevelRouteHistory.size == 1

            BackHandler(
                enabled = hasBlockingAuthSheet ||
                    hasOrdersSheet ||
                    canCloseWorkflowWorkspace ||
                    canPopNestedRoute ||
                    canPopTopLevelRoute ||
                    onMainShellAtRoot,
            ) {
                when {
                    authSheet != null -> {
                        dismissAuthSheet()
                    }
                    showOrders -> showOrders = false
                    currentDestination?.route == "ai" && showsWorkflowWorkspace -> showsWorkflowWorkspace = false
                    navController.previousBackStackEntry != null && !topLevelRoutes.contains(currentDestination?.route) -> {
                        navController.popBackStack()
                    }
                    canPopTopLevelRoute -> {
                        navigateBackWithinTopLevel()
                    }
                    onMainShellAtRoot -> {
                        selectedEntryRoute = null
                    }
                    else -> { }
                }
            }

            LaunchedEffect(hasAiAccess, currentDestination?.route) {
                if (currentDestination?.route == "ai") {
                    AppFeatureFlagsStore.refresh()
                    AppContainer.refreshCurrentUser()
                }
                if (!hasAiAccess) {
                    showsWorkflowWorkspace = false
                }
                if (currentDestination?.route != "ai") {
                    showsWorkflowWorkspace = false
                }
            }

            Scaffold(
                modifier = Modifier.skydownSelectionFeedback(
                    trigger = currentDestination?.route ?: startRoute,
                ),
                contentWindowInsets = WindowInsets(0.dp),
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .skydownAtmosphereBackground(),
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = startRoute,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = floatingDockContentPadding),
                        enterTransition = { skydownEnterTransition() },
                        exitTransition = { skydownExitTransition() },
                        popEnterTransition = { skydownEnterTransition() },
                        popExitTransition = { skydownExitTransition() },
                    ) {
                        composable("home") {
                            HomeScreen(
                                onOpenCart = openCart,
                                onOpenProfile = openProfile,
                                onOpenSettings = openSettings,
                                onGuestSignIn = { openAuthLogin(AuthEntryContext.DEFAULT) },
                                onOpenWorkflow = if (hasAiAccess) {
                                    {
                                        showsWorkflowWorkspace = true
                                        navigateToTopLevel("ai")
                                    }
                                } else {
                                    null
                                },
                                onOpenWorkflowWithPrompt = if (hasAiAccess) {
                                    { prompt ->
                                        pendingAgentPrefillPrompt = prompt
                                        showsWorkflowWorkspace = false
                                        navigateToTopLevel("ai")
                                    }
                                } else {
                                    null
                                },
                            )
                        }
                        composable("shop") {
                            ShopScreen(
                                onOpenLogin = { openAuthLogin(AuthEntryContext.MERCH_SHOP) },
                                onGuestSignIn = { openAuthLogin(AuthEntryContext.DEFAULT) },
                                onOpenCart = openCart,
                                onOpenProfile = openProfile,
                                onOpenSettings = openSettings,
                            )
                        }
                        composable("music") {
                            ZweizweiMusicLaneScreen(
                                onOpenCart = openCart,
                                onOpenProfile = openProfile,
                                onOpenSettings = openSettings,
                                onGuestSignIn = { openAuthLogin(AuthEntryContext.MUSIC) },
                            )
                        }
                        composable("video") {
                            VideoHubScreen(
                                onOpenCart = openCart,
                                onOpenProfile = openProfile,
                                onOpenSettings = openSettings,
                                onGuestSignIn = { openAuthLogin(AuthEntryContext.DEFAULT) },
                            )
                        }
                        composable("cart") {
                            CartScreen(
                                onBack = { navController.popBackStack() },
                                onOpenLogin = { openAuthLogin(AuthEntryContext.CART) },
                                onGuestSignIn = { openAuthLogin(AuthEntryContext.DEFAULT) },
                                onOpenProfile = openProfile,
                                onOpenSettings = openSettings,
                            )
                        }
                        composable("ai") {
                            AiHubScreen(
                                immersiveMode = true,
                                showsWorkflowWorkspace = showsWorkflowWorkspace,
                                pendingAgentPrefillPrompt = pendingAgentPrefillPrompt,
                                onConsumePendingAgentPrefillPrompt = { pendingAgentPrefillPrompt = null },
                                onToggleWorkflow = { showsWorkflowWorkspace = !showsWorkflowWorkspace },
                                onHideWorkflow = { showsWorkflowWorkspace = false },
                                onExitImmersive = {
                                    if (!navigateBackWithinTopLevel()) {
                                        navigateToTopLevel(
                                            route = if (startRoute == "ai") "home" else startRoute,
                                            recordHistory = false,
                                        )
                                    }
                                },
                                onOpenLogin = { openAuthLogin(AuthEntryContext.AI) },
                                onGuestSignIn = { openAuthLogin(AuthEntryContext.DEFAULT) },
                                onOpenCart = openCart,
                                onOpenProfile = openProfile,
                                onOpenSettings = openSettings,
                                onOpenAutomationSettings = openAutomationSettings,
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                initialAdminWorkspaceKey = initialSettingsWorkspaceKey,
                                onClose = { navController.popBackStack() },
                                onOpenLogin = { openAuthLogin(AuthEntryContext.SETTINGS) },
                                onOpenRegistration = { authSheet = AuthSheet.Registration },
                                onOpenProfile = openProfile,
                                onOpenOrders = { showOrders = true },
                                onOpenOwnerHub = {
                                    navController.navigate("ownerHub") {
                                        launchSingleTop = true
                                    }
                                },
                            )
                        }
                        composable("ownerHub") {
                            OwnerHubScreen(
                                onBack = { navController.popBackStack() },
                                hasAiAccess = hasAiAccess,
                                onOpenAgentWithPrompt = { prompt ->
                                    if (hasAiAccess) {
                                        pendingAgentPrefillPrompt = prompt
                                        showsWorkflowWorkspace = false
                                        navigateToTopLevel("ai")
                                    }
                                },
                            )
                        }
                        composable("profile") {
                            ProfileScreen(
                                onBack = { navController.popBackStack() },
                                onOpenSettings = openSettings,
                                onOpenOrders = { showOrders = true },
                            )
                        }
                    }

                    ConnectivityStatusBanner(
                        visible = !isOnline,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(top = 10.dp),
                    )

                    if (showFloatingDock) {
                        SkydownFloatingBottomDock(
                            destinations = destinations,
                            isCompactLayout = isCompactLayout,
                            isSelected = { destination ->
                                currentDestination?.hierarchy?.any { it.route == destination.route } == true
                            },
                            onDestinationClick = { destination ->
                                if (currentDestination?.route == "settings" ||
                                    currentDestination?.route == "ownerHub"
                                ) {
                                    navController.popBackStack()
                                }

                                navigateToTopLevel(destination.route)
                            },
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                    }
                }
            }
        }

        authSheet?.let { sheet ->
            ModalBottomSheet(
                onDismissRequest = {
                    dismissAuthSheet()
                },
                sheetState = authSheetState,
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                when (sheet) {
                    AuthSheet.Login -> LoginScreen(
                        onClose = { dismissAuthSheet() },
                        onOpenRegistration = { authSheet = AuthSheet.Registration },
                        onBusyStateChanged = { authSheetLocked = it },
                        entryContext = authEntryContext,
                    )
                    AuthSheet.Registration -> RegistrationScreen(
                        growthTracker = growthTracker,
                        onClose = { dismissAuthSheet() },
                        onBusyStateChanged = { authSheetLocked = it },
                    )
                }
            }
        }

        if (showOrders) {
            ModalBottomSheet(
                onDismissRequest = { showOrders = false },
                sheetState = ordersSheetState,
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                OrderScreen(
                    onClose = { showOrders = false },
                )
            }
        }
    }
}

@Composable
private fun SkydownFloatingBottomDock(
    destinations: List<BottomDestination>,
    isCompactLayout: Boolean,
    isSelected: (BottomDestination) -> Boolean,
    onDestinationClick: (BottomDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDarkPalette = colorScheme.skydownIsDarkPalette()
    val selectedAccentTarget = destinations.firstOrNull { isSelected(it) }?.accent
        ?: colorScheme.skydownAccent()
    val selectedAccent by animateColorAsState(
        targetValue = selectedAccentTarget,
        animationSpec = tween(
            durationMillis = SkydownMotionTokens.premiumAccentTransitionMillis,
            easing = SkydownStandardEasing,
        ),
        label = "bottomDockAccent",
    )
    val dockBorder by animateColorAsState(
        targetValue = lerp(
            Color.White.copy(alpha = if (isDarkPalette) 0.09f else 0.18f),
            selectedAccent.copy(alpha = if (isDarkPalette) 0.14f else 0.11f),
            0.28f,
        ),
        animationSpec = tween(
            durationMillis = SkydownMotionTokens.premiumAccentTransitionMillis,
            easing = SkydownStandardEasing,
        ),
        label = "bottomDockBorder",
    )
    val dockGlow by animateColorAsState(
        targetValue = selectedAccent.copy(alpha = if (isDarkPalette) 0.075f else 0.055f),
        animationSpec = tween(
            durationMillis = SkydownMotionTokens.premiumAccentTransitionMillis,
            easing = SkydownStandardEasing,
        ),
        label = "bottomDockGlow",
    )
    val dockShape = RoundedCornerShape(
        if (isCompactLayout) SkydownUiTokens.spotlightRadius else SkydownUiTokens.heroCornerRadius,
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(
                horizontal = if (isCompactLayout) SkydownUiTokens.cardPadding else SkydownUiTokens.cardCornerRadius,
            )
            .padding(top = SkydownUiTokens.nanoCorner, bottom = skydownDockBottomSpacing()),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = if (isCompactLayout) 420.dp else 540.dp)
                .fillMaxWidth(if (isCompactLayout) 0.88f else 0.74f),
            shape = dockShape,
            color = colorScheme.skydownCardBackground().copy(alpha = if (isDarkPalette) 0.40f else 0.68f),
            tonalElevation = 0.dp,
            shadowElevation = if (isCompactLayout) SkydownUiTokens.stackSpacingMicro else SkydownUiTokens.stackSpacingPill,
            border = BorderStroke(
                width = 0.6.dp,
                color = dockBorder,
            ),
        ) {
            Box(
                modifier = Modifier
                    .clip(dockShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isDarkPalette) 0.12f else 0.20f),
                                Color.Transparent,
                            ),
                            center = Offset.Zero,
                            radius = 900f,
                        ),
                    )
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isDarkPalette) 0.16f else 0.24f),
                                Color.Transparent,
                                Color.Transparent,
                            ),
                        ),
                    )
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                colorScheme.skydownLuminanceLift().copy(alpha = if (isDarkPalette) 0.11f else 0.24f),
                                colorScheme.skydownCardBackground().copy(alpha = if (isDarkPalette) 0.76f else 0.78f),
                                colorScheme.skydownSecondaryBackground().copy(alpha = if (isDarkPalette) 0.22f else 0.24f),
                                selectedAccent.copy(alpha = if (isDarkPalette) 0.06f else 0.045f),
                                dockGlow.copy(alpha = if (isDarkPalette) 0.16f else 0.11f),
                            ),
                        ),
                    )
                    .border(
                        width = 0.8.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isDarkPalette) 0.22f else 0.30f),
                                selectedAccent.copy(alpha = if (isDarkPalette) 0.16f else 0.12f),
                                Color.White.copy(alpha = if (isDarkPalette) 0.10f else 0.16f),
                            ),
                        ),
                        shape = dockShape,
                    )
                    .skydownLuminousSweep(
                        shape = dockShape,
                        accent = selectedAccent,
                        alpha = if (isDarkPalette) 0.09f else 0.07f,
                    ),
            ) {
                Row(
                    modifier = Modifier
                        .height(if (isCompactLayout) 62.dp else 66.dp)
                        .fillMaxWidth()
                        .padding(
                            horizontal = if (isCompactLayout) 5.dp else 8.dp,
                            vertical = 4.dp,
                        ),
                    horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingNano),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val dockIconMotion = remember {
                        tween<Float>(
                            durationMillis = SkydownMotionTokens.dockSelectionDurationMillis,
                            easing = SkydownStandardEasing,
                        )
                    }
                    destinations.forEach { destination ->
                        val selected = isSelected(destination)
                        val itemShape = RoundedCornerShape(SkydownUiTokens.cardCornerRadius)
                        val itemInteractionSource = remember(destination.route) { MutableInteractionSource() }
                        val iconScale by animateFloatAsState(
                            targetValue = if (selected) 1f else 0.94f,
                            animationSpec = dockIconMotion,
                            label = "bottomNavIconScale_${destination.route}",
                        )
                        val iconLift by animateFloatAsState(
                            targetValue = if (selected) -1f else 0f,
                            animationSpec = dockIconMotion,
                            label = "bottomNavIconLift_${destination.route}",
                        )
                        val labelAlpha by animateFloatAsState(
                            targetValue = if (selected) 0.98f else 0.72f,
                            animationSpec = tween(
                                durationMillis = SkydownMotionTokens.premiumLabelTransitionMillis,
                                easing = SkydownStandardEasing,
                            ),
                            label = "bottomNavLabelAlpha_${destination.route}",
                        )
                        val selectedBorderAlpha by animateFloatAsState(
                            targetValue = if (selected) {
                                if (isDarkPalette) 0.22f else 0.20f
                            } else {
                                0f
                            },
                            animationSpec = tween(
                                durationMillis = SkydownMotionTokens.premiumLabelTransitionMillis,
                                easing = SkydownStandardEasing,
                            ),
                            label = "bottomNavBorderAlpha_${destination.route}",
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(horizontal = 1.dp, vertical = 4.dp)
                                .testTag("bottomDock.${destination.route}")
                                .clip(itemShape)
                                .background(
                                    if (selected) {
                                        Brush.linearGradient(
                                            colors = listOf(
                                                colorScheme.skydownLuminanceLift().copy(alpha = if (isDarkPalette) 0.11f else 0.18f),
                                                selectedAccent.copy(alpha = if (isDarkPalette) 0.18f else 0.12f),
                                                colorScheme.skydownAccentHighlight().copy(alpha = if (isDarkPalette) 0.08f else 0.06f),
                                            ),
                                        )
                                    } else {
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Transparent,
                                            ),
                                        )
                                    },
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (selected) {
                                        Color.White.copy(alpha = selectedBorderAlpha)
                                    } else {
                                        Color.Transparent
                                    },
                                    shape = itemShape,
                                )
                                .skydownPressable(
                                    interactionSource = itemInteractionSource,
                                    pressedScale = 0.988f,
                                )
                                .clickable(
                                    interactionSource = itemInteractionSource,
                                    indication = null,
                                ) {
                                    onDestinationClick(destination)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingHairline),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                        .graphicsLayer {
                                            scaleX = iconScale
                                            scaleY = iconScale
                                            translationY = iconLift
                                        },
                                ) {
                                    destination.icon(selected)
                                }
                                Text(
                                    modifier = Modifier.alpha(labelAlpha),
                                    text = destination.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (selected) {
                                        selectedAccent.copy(alpha = if (isDarkPalette) 0.98f else 0.90f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DisposableAuthSync(
    auth: FirebaseAuth,
    observedAuthUid: String?,
    currentSessionUserId: String?,
    onObservedAuthUidChanged: (String?) -> Unit,
    onSignedOut: () -> Unit,
    onRefreshCurrentUser: () -> Unit,
) {
    DisposableEffect(auth, observedAuthUid, currentSessionUserId) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val authUid = firebaseAuth.currentUser?.uid
            when {
                authUid == null -> {
                    if (observedAuthUid != null || currentSessionUserId != null) {
                        onObservedAuthUidChanged(null)
                        onSignedOut()
                    }
                }

                authUid != observedAuthUid || currentSessionUserId != authUid -> {
                    onObservedAuthUidChanged(authUid)
                    onRefreshCurrentUser()
                }
            }
        }

        auth.addAuthStateListener(listener)

        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }
}

@Composable
private fun DisposableSessionPrivilegeSync(
    auth: FirebaseAuth,
    currentSessionUser: com.skydown.shared.model.User?,
    onRefreshCurrentUser: () -> Unit,
) {
    val currentSessionSnapshot by rememberUpdatedState(currentSessionUser)
    val firestore = remember { FirebaseFirestore.getInstance() }

    DisposableEffect(auth, firestore, currentSessionUser?.id) {
        val authUid = auth.currentUser?.uid ?: currentSessionUser?.id
        if (authUid.isNullOrBlank()) {
            return@DisposableEffect onDispose {}
        }

        var lastObservedFingerprint = currentSessionSnapshot.sessionPrivilegeFingerprint()
        val listener = firestore.collection("users").document(authUid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                return@addSnapshotListener
            }

            val nextFingerprint = snapshot.sessionPrivilegeFingerprint()
                ?: return@addSnapshotListener
            if (nextFingerprint == lastObservedFingerprint) {
                return@addSnapshotListener
            }

            lastObservedFingerprint = nextFingerprint
            onRefreshCurrentUser()
        }

        onDispose {
            listener.remove()
        }
    }
}

private fun com.skydown.shared.model.User?.sessionPrivilegeFingerprint(): String? {
    val user = this ?: return null
    val userId = user.id?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return listOf(
        userId,
        user.role.trim().lowercase(),
        user.isAdmin.toString(),
        user.canManageMusicCatalog.toString(),
        user.canManageVideoCatalog.toString(),
        user.canModerateProfiles.toString(),
    ).joinToString("|")
}

private fun com.google.firebase.firestore.DocumentSnapshot?.sessionPrivilegeFingerprint(): String? {
    val snapshot = this ?: return null
    if (!snapshot.exists()) {
        return null
    }

    val data = snapshot.data.orEmpty()
    return listOf(
        snapshot.id,
        (data["role"] as? String)?.trim()?.lowercase().orEmpty(),
        ((data["isAdmin"] as? Boolean) == true).toString(),
        ((data["canManageMusicCatalog"] as? Boolean) == true).toString(),
        ((data["canManageVideoCatalog"] as? Boolean) == true).toString(),
        ((data["canModerateProfiles"] as? Boolean) == true).toString(),
    ).joinToString("|")
}

private data class BottomDestination(
    val route: String,
    val label: String,
    val accent: Color,
    val icon: @Composable (Boolean) -> Unit,
)

private val skydownPrimaryRoutes = listOf("shop", "music", "home", "video", "ai")
private val skydownOverlayRoutes = setOf("cart", "settings", "profile", "ownerHub")

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun skydownShouldShowDock(currentRoute: String?): Boolean {
    return !WindowInsets.isImeVisible && currentRoute != "ai"
}

private fun skydownDockOverlayPadding(isCompactLayout: Boolean) =
    if (isCompactLayout) SkydownUiTokens.spotlightRadius else SkydownUiTokens.heroCornerRadius

private fun skydownDockBottomSpacing() = SkydownUiTokens.stackSpacingMicro

private fun skydownPrimaryRouteIndex(route: String?): Int = skydownPrimaryRoutes.indexOf(route)

private fun AnimatedContentTransitionScope<NavBackStackEntry>.skydownEnterTransition(): EnterTransition {
    val initialRoute = initialState.destination.route
    val targetRoute = targetState.destination.route
    val initialIndex = skydownPrimaryRouteIndex(initialRoute)
    val targetIndex = skydownPrimaryRouteIndex(targetRoute)

    return when {
        initialIndex >= 0 && targetIndex >= 0 -> {
            val direction = if (targetIndex > initialIndex) {
                AnimatedContentTransitionScope.SlideDirection.Left
            } else {
                AnimatedContentTransitionScope.SlideDirection.Right
            }
            fadeIn(
                animationSpec = tween(
                    durationMillis = 220,
                    delayMillis = SkydownMotionTokens.navFadeLeadMillis,
                    easing = LinearOutSlowInEasing,
                ),
            ) + slideIntoContainer(
                towards = direction,
                animationSpec = tween(
                    durationMillis = SkydownMotionTokens.primaryEnterDurationMillis,
                    easing = SkydownStandardEasing,
                ),
                initialOffset = { fullSize -> (fullSize * 0.038f).toInt() },
            )
        }

        targetRoute in skydownOverlayRoutes -> {
            fadeIn(
                animationSpec = tween(
                    durationMillis = 210,
                    delayMillis = 16,
                    easing = LinearOutSlowInEasing,
                ),
            ) + slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Up,
                animationSpec = tween(
                    durationMillis = SkydownMotionTokens.overlayEnterDurationMillis,
                    easing = SkydownStandardEasing,
                ),
                initialOffset = { fullSize -> (fullSize * 0.055f).toInt() },
            )
        }

        else -> {
            fadeIn(
                animationSpec = tween(
                    durationMillis = 200,
                    delayMillis = 12,
                    easing = LinearOutSlowInEasing,
                ),
            )
        }
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.skydownExitTransition(): ExitTransition {
    val initialRoute = initialState.destination.route
    val targetRoute = targetState.destination.route
    val initialIndex = skydownPrimaryRouteIndex(initialRoute)
    val targetIndex = skydownPrimaryRouteIndex(targetRoute)

    return when {
        initialIndex >= 0 && targetIndex >= 0 -> {
            val direction = if (targetIndex > initialIndex) {
                AnimatedContentTransitionScope.SlideDirection.Left
            } else {
                AnimatedContentTransitionScope.SlideDirection.Right
            }
            fadeOut(
                animationSpec = tween(
                    durationMillis = 170,
                    easing = SkydownExitEasing,
                ),
            ) + slideOutOfContainer(
                towards = direction,
                animationSpec = tween(
                    durationMillis = SkydownMotionTokens.primaryExitDurationMillis,
                    easing = SkydownExitEasing,
                ),
                targetOffset = { fullSize -> (fullSize * 0.026f).toInt() },
            )
        }

        targetRoute in skydownOverlayRoutes -> {
            fadeOut(
                animationSpec = tween(
                    durationMillis = 150,
                    easing = SkydownExitEasing,
                ),
            )
        }

        initialRoute in skydownOverlayRoutes -> {
            fadeOut(
                animationSpec = tween(
                    durationMillis = 165,
                    easing = SkydownExitEasing,
                ),
            ) + slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Down,
                animationSpec = tween(
                    durationMillis = SkydownMotionTokens.overlayExitDurationMillis,
                    easing = SkydownExitEasing,
                ),
                targetOffset = { fullSize -> (fullSize * 0.05f).toInt() },
            )
        }

        else -> {
            fadeOut(
                animationSpec = tween(
                    durationMillis = 140,
                    easing = SkydownExitEasing,
                ),
            )
        }
    }
}

private enum class AuthSheet {
    Login,
    Registration,
}

private enum class LaunchLandingPathTier {
    /** Hauptpfad — groesseres Panel, volle Informationsdichte. */
    Primary,

    /** Alternativen — klar kleiner, weniger visuelles Gewicht. */
    Secondary,
}

@Composable
private fun LaunchLandingScreen(
    onOpenHome: () -> Unit,
    onOpenMusic: () -> Unit,
    onOpenVideography: () -> Unit,
    onOpenShop: () -> Unit,
) {
    val screenHeaderSettings by AppContainer.screenHeaderSettingsRepository.settings.collectAsStateWithLifecycle()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .skydownAtmosphereBackground(
                primaryAlpha = 0.058f,
                secondaryAlpha = 0.044f,
                includeScreenGradient = false,
            ),
    ) {
        val isWideLayout = maxWidth >= 900.dp
        val isThreeColumnLayout = maxWidth >= 1180.dp
        // Landing: alles sichtbar ohne Scroll — auf Phones aggressivere Abstaende/Insets.
        val isPhonePortrait = !isWideLayout
        val isShortHeightLayout = isPhonePortrait && maxHeight < 800.dp
        val isTightPhoneLanding = isPhonePortrait && maxHeight < 880.dp
        val contentMaxWidth = when {
            isThreeColumnLayout -> 1120.dp
            isWideLayout -> 920.dp
            else -> maxWidth
        }
        val verticalScreenPadding = when {
            isPhonePortrait && maxHeight < 700.dp -> 4.dp
            isTightPhoneLanding || isShortHeightLayout -> 6.dp
            !isWideLayout -> 8.dp
            else -> 12.dp
        }
        val heroSpacer = when {
            isPhonePortrait && maxHeight < 700.dp -> 4.dp
            isTightPhoneLanding -> 5.dp
            isShortHeightLayout -> 6.dp
            !isWideLayout -> 8.dp
            else -> 12.dp
        }
        val signalSpacer = if (isTightPhoneLanding || isShortHeightLayout) 0.dp else 2.dp
        val cardsTopSpacer = if (isTightPhoneLanding || isShortHeightLayout) 2.dp else if (isPhonePortrait) 4.dp else 6.dp
        val musicDetailText = stringResource(R.string.landing_music_detail)
        val videoDetailText = stringResource(R.string.landing_video_detail)
        val merchDetailText = stringResource(R.string.landing_merch_detail)
        val musicCardBackgroundUrl = screenHeaderSettings.musicHubImageUrl.ifBlank { null }
        val videoCardBackgroundUrl = screenHeaderSettings.videoHubImageUrl.ifBlank { null }
        val merchCardBackgroundUrl = screenHeaderSettings.shopImageUrl.ifBlank { null }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 36.dp)
                .offset(x = 22.dp)
                .width(if (isThreeColumnLayout) 280.dp else 180.dp)
                .height(if (isThreeColumnLayout) 280.dp else 180.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
                            Color.Transparent,
                        ),
                    ),
                    RoundedCornerShape(SkydownUiTokens.fullCapsuleRadius),
                )
                .blur(if (isThreeColumnLayout) 36.dp else 28.dp),
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 52.dp)
                .offset(x = (-28).dp)
                .width(if (isWideLayout) 220.dp else 168.dp)
                .height(if (isWideLayout) 220.dp else 168.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                            Color.Transparent,
                        ),
                    ),
                    RoundedCornerShape(SkydownUiTokens.fullCapsuleRadius),
                )
                .blur(if (isWideLayout) 34.dp else 26.dp),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = SkydownUiTokens.screenHorizontalPadding, vertical = verticalScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = contentMaxWidth)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingNone),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LaunchLandingMetaPill(
                        text = stringResource(R.string.brand_system_name),
                        accent = MaterialTheme.colorScheme.primary,
                        onClick = onOpenHome,
                    )
                    if (isWideLayout) {
                        LaunchLandingMetaPill(
                            text = stringResource(R.string.landing_living_system),
                            accent = MaterialTheme.colorScheme.secondary,
                            onClick = onOpenVideography,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(heroSpacer))

                BrandHeroCard(
                    eyebrow = screenHeaderSettings.homeEyebrow.ifBlank { stringResource(R.string.skydown_home_fallback_eyebrow) },
                    title = screenHeaderSettings.homeTitle.ifBlank { "SkyOS" },
                    subtitle = screenHeaderSettings.homeSubtitle.ifBlank { stringResource(R.string.landing_home_subtitle) },
                    detail = screenHeaderSettings.homeDetail.ifBlank {
                        stringResource(R.string.landing_home_detail)
                    },
                    backgroundImageUrl = screenHeaderSettings.homeImageUrl.ifBlank { null },
                    accent = MaterialTheme.colorScheme.primary,
                    secondaryAccent = MaterialTheme.colorScheme.secondary,
                    marks = emptyList(),
                    compactVisualDensity = !isWideLayout || isTightPhoneLanding,
                    edgeToEdge = true,
                    onSurfaceClick = onOpenHome,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(if (isTightPhoneLanding) SkydownUiTokens.stackSpacingNano else SkydownUiTokens.stackSpacingMicro)) {
                        Text(
                            text = stringResource(R.string.landing_home_recommended),
                            style = MaterialTheme.typography.labelLarge,
                            color = SpotifyGreen,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(R.string.landing_home_tap),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(if (isTightPhoneLanding) SkydownUiTokens.stackSpacingDense else SkydownUiTokens.stackSpacingPill)) {
                    LaunchLandingActionButton(
                        title = stringResource(R.string.landing_home_open),
                        icon = Icons.Default.Home,
                        primary = true,
                        minHeight = if (isTightPhoneLanding) 44.dp else 50.dp,
                        onClick = onOpenHome,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(if (isTightPhoneLanding) SkydownUiTokens.stackSpacingMicro else SkydownUiTokens.stackSpacingPill)) {
                        LaunchLandingActionButton(
                            title = stringResource(R.string.tabs_music),
                            icon = Icons.Default.GraphicEq,
                            primary = false,
                            minHeight = if (isTightPhoneLanding) 44.dp else 50.dp,
                            modifier = Modifier.weight(1f),
                            onClick = onOpenMusic,
                        )
                        LaunchLandingActionButton(
                            title = stringResource(R.string.tabs_merch),
                            icon = Icons.Default.ShoppingBag,
                            primary = false,
                            minHeight = if (isTightPhoneLanding) 44.dp else 50.dp,
                            modifier = Modifier.weight(1f),
                            onClick = onOpenShop,
                        )
                    }
                }

                if (!isTightPhoneLanding) {
                    Text(
                        text = stringResource(R.string.landing_home_note),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }

                Spacer(modifier = Modifier.height(signalSpacer))

                Spacer(modifier = Modifier.height(cardsTopSpacer))

                if (isThreeColumnLayout) {
                    Row(horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingCompact)) {
                        LaunchLandingChoiceCard(
                            eyebrow = stringResource(R.string.landing_eyebrow_music),
                            title = stringResource(R.string.tabs_music),
                            subtitle = stringResource(R.string.landing_music_subtitle),
                            detail = musicDetailText,
                            chips = emptyList(),
                            accentColor = MaterialTheme.colorScheme.primary,
                            icon = Icons.Default.GraphicEq,
                            onClick = onOpenMusic,
                            backgroundImageUrl = musicCardBackgroundUrl,
                            modifier = Modifier
                                .weight(1.12f)
                                .offset(y = (-2).dp),
                            emphasized = true,
                            pathTier = LaunchLandingPathTier.Primary,
                        )
                        LaunchLandingChoiceCard(
                            eyebrow = stringResource(R.string.landing_eyebrow_video),
                            title = stringResource(R.string.tabs_videos),
                            subtitle = stringResource(R.string.landing_video_subtitle),
                            detail = videoDetailText,
                            chips = emptyList(),
                            accentColor = MaterialTheme.colorScheme.tertiary,
                            icon = Icons.Default.PlayCircleFilled,
                            onClick = onOpenVideography,
                            backgroundImageUrl = videoCardBackgroundUrl,
                            modifier = Modifier.weight(0.94f),
                            pathTier = LaunchLandingPathTier.Secondary,
                        )
                        LaunchLandingChoiceCard(
                            eyebrow = stringResource(R.string.landing_eyebrow_store),
                            title = stringResource(R.string.tabs_merch),
                            subtitle = stringResource(R.string.landing_merch_subtitle),
                            detail = merchDetailText,
                            chips = emptyList(),
                            accentColor = MaterialTheme.colorScheme.secondary,
                            icon = Icons.Default.ShoppingBag,
                            onClick = onOpenShop,
                            backgroundImageUrl = merchCardBackgroundUrl,
                            modifier = Modifier.weight(0.94f),
                            pathTier = LaunchLandingPathTier.Secondary,
                        )
                    }
                } else if (isWideLayout) {
                    LaunchLandingChoiceCard(
                        eyebrow = stringResource(R.string.landing_eyebrow_music),
                        title = stringResource(R.string.tabs_music),
                        subtitle = stringResource(R.string.landing_music_subtitle),
                        detail = musicDetailText,
                        chips = emptyList(),
                        accentColor = MaterialTheme.colorScheme.primary,
                        icon = Icons.Default.GraphicEq,
                        onClick = onOpenMusic,
                        backgroundImageUrl = musicCardBackgroundUrl,
                        modifier = Modifier.offset(y = (-2).dp),
                        emphasized = true,
                        pathTier = LaunchLandingPathTier.Primary,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill)) {
                        LaunchLandingChoiceCard(
                            eyebrow = stringResource(R.string.landing_eyebrow_video),
                            title = stringResource(R.string.tabs_videos),
                            subtitle = stringResource(R.string.landing_video_subtitle),
                            detail = videoDetailText,
                            chips = emptyList(),
                            accentColor = MaterialTheme.colorScheme.tertiary,
                            icon = Icons.Default.PlayCircleFilled,
                            onClick = onOpenVideography,
                            backgroundImageUrl = videoCardBackgroundUrl,
                            modifier = Modifier.weight(1f),
                            pathTier = LaunchLandingPathTier.Secondary,
                        )
                        LaunchLandingChoiceCard(
                            eyebrow = stringResource(R.string.landing_eyebrow_store),
                            title = stringResource(R.string.tabs_merch),
                            subtitle = stringResource(R.string.landing_merch_subtitle),
                            detail = merchDetailText,
                            chips = emptyList(),
                            accentColor = MaterialTheme.colorScheme.secondary,
                            icon = Icons.Default.ShoppingBag,
                            onClick = onOpenShop,
                            backgroundImageUrl = merchCardBackgroundUrl,
                            modifier = Modifier.weight(1f),
                            pathTier = LaunchLandingPathTier.Secondary,
                        )
                    }
                } else {
                    LaunchLandingChoiceCard(
                        eyebrow = stringResource(R.string.landing_eyebrow_music),
                        title = stringResource(R.string.tabs_music),
                        subtitle = stringResource(R.string.landing_music_subtitle),
                        detail = musicDetailText,
                        chips = emptyList(),
                        accentColor = MaterialTheme.colorScheme.primary,
                        icon = Icons.Default.GraphicEq,
                        onClick = onOpenMusic,
                        backgroundImageUrl = musicCardBackgroundUrl,
                        modifier = Modifier.offset(y = (-2).dp),
                        emphasized = true,
                        pathTier = LaunchLandingPathTier.Primary,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill)) {
                        LaunchLandingChoiceCard(
                            eyebrow = stringResource(R.string.landing_eyebrow_video),
                            title = stringResource(R.string.tabs_videos),
                            subtitle = stringResource(R.string.landing_video_subtitle),
                            detail = videoDetailText,
                            chips = emptyList(),
                            accentColor = MaterialTheme.colorScheme.tertiary,
                            icon = Icons.Default.PlayCircleFilled,
                            onClick = onOpenVideography,
                            backgroundImageUrl = videoCardBackgroundUrl,
                            modifier = Modifier.weight(1f),
                            pathTier = LaunchLandingPathTier.Secondary,
                        )
                        LaunchLandingChoiceCard(
                            eyebrow = stringResource(R.string.landing_eyebrow_store),
                            title = stringResource(R.string.tabs_merch),
                            subtitle = stringResource(R.string.landing_merch_subtitle),
                            detail = merchDetailText,
                            chips = emptyList(),
                            accentColor = MaterialTheme.colorScheme.secondary,
                            icon = Icons.Default.ShoppingBag,
                            onClick = onOpenShop,
                            backgroundImageUrl = merchCardBackgroundUrl,
                            modifier = Modifier.weight(1f),
                            pathTier = LaunchLandingPathTier.Secondary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LaunchLandingChoiceCard(
    eyebrow: String,
    title: String,
    subtitle: String,
    detail: String,
    chips: List<String>,
    accentColor: androidx.compose.ui.graphics.Color,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compactVisualDensity: Boolean = true,
    emphasized: Boolean = false,
    pathTier: LaunchLandingPathTier = LaunchLandingPathTier.Primary,
    backgroundImageUrl: String? = null,
) {
    HubEntryCard(
        eyebrow = eyebrow,
        title = title,
        subtitle = subtitle,
        detail = detail,
        chips = chips,
        accentColor = accentColor,
        icon = icon,
        artwork = null,
        onClick = onClick,
        compactVisualDensity = compactVisualDensity,
        emphasized = emphasized && pathTier == LaunchLandingPathTier.Primary,
        pathTier = pathTier,
        backgroundImageUrl = backgroundImageUrl,
        modifier = modifier,
    )
}

@Composable
private fun LaunchLandingActionButton(
    title: String,
    icon: ImageVector,
    primary: Boolean,
    modifier: Modifier = Modifier,
    minHeight: Dp = 52.dp,
    onClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val bg = if (primary) {
        colorScheme.primary.copy(alpha = 0.56f)
    } else {
        colorScheme.surface.copy(alpha = 0.20f)
    }
    val border = if (primary) {
        colorScheme.primary.copy(alpha = 0.70f)
    } else {
        colorScheme.outline.copy(alpha = 0.32f)
    }
    val textColor = colorScheme.onSurface
    val interaction = remember { MutableInteractionSource() }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .clip(RoundedCornerShape(SkydownUiTokens.compactRadius))
            .skydownPressable(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        shape = RoundedCornerShape(SkydownUiTokens.compactRadius),
        color = bg,
        border = BorderStroke(1.dp, border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = title,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun MusicHubQuickActions(
    onOpenCatalog: () -> Unit,
    onOpenStudio: () -> Unit,
    activeSocialTitle: String,
    onOpenSocialLink: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro)) {
        Text(
            text = stringResource(R.string.music_hub_quick_access_title),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            fontWeight = FontWeight.SemiBold,
        )
        LaunchLandingActionButton(
            title = stringResource(R.string.music_hub_catalog_title),
            icon = Icons.Default.GraphicEq,
            primary = true,
            modifier = Modifier.testTag("music.hub.songs.open"),
            onClick = onOpenCatalog,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro)) {
            LaunchLandingActionButton(
                title = stringResource(R.string.music_hub_studio_title),
                icon = Icons.Default.AutoAwesome,
                primary = false,
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenStudio,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingDense)) {
            Text(
                text = stringResource(R.string.music_hub_artist_links_title),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                fontWeight = FontWeight.SemiBold,
            )
            Column(verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingDense)) {
                musicHubInstagramQuickLinks.forEach { link ->
                    MusicHubSocialLinkButton(
                        title = link.title,
                        subtitle = link.subtitle,
                        isActive = link.title == activeSocialTitle,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onOpenSocialLink(link.url) }
                    )
                }
            }
        }
    }
}

private enum class ZweizweiMusicDestination {
    Hub,
    Catalog,
    ArtistPage,
    NicmaProducer,
}

private data class MusicHubSocialLink(
    val title: String,
    val subtitle: String,
    val url: String,
)

private val musicHubInstagramQuickLinks = listOf(
    MusicHubSocialLink("22 Music", "@zweizwei_music", "https://www.instagram.com/zweizwei_music/"),
    MusicHubSocialLink("JANNO", "@janno_official_", "https://www.instagram.com/janno_official_/"),
    MusicHubSocialLink("Yang D. Nash", "@y.d.nash", "https://www.instagram.com/y.d.nash/"),
    MusicHubSocialLink("MAVE", "@mave040_official", "https://www.instagram.com/mave040_official/"),
    MusicHubSocialLink("ThaDude", "@thadude_offizielle", "https://www.instagram.com/thadude_offizielle/"),
    MusicHubSocialLink("TANGAJOE007", "@tangajoe007", "https://www.instagram.com/tangajoe007/"),
)

@Composable
private fun MusicHubSocialLinkButton(
    title: String,
    subtitle: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val accent = when (title) {
        "22 Music" -> colorScheme.primary
        "JANNO" -> colorScheme.secondary
        "Yang D. Nash" -> colorScheme.tertiary
        "MAVE" -> colorScheme.skydownAccentMystic()
        else -> colorScheme.skydownAccent()
    }
    val instagramGradientColors = listOf(
        Color(0xFFFDB347),
        Color(0xFFF56040),
        Color(0xFFC13584),
        Color(0xFF5851DB),
    )
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(SkydownUiTokens.tightRadius))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        colorScheme.skydownSecondaryBackground(),
                        accent.copy(alpha = 0.10f),
                        instagramGradientColors[1].copy(alpha = if (isActive) 0.18f else 0.10f),
                        instagramGradientColors[2].copy(alpha = if (isActive) 0.18f else 0.10f),
                    ),
                    start = Offset.Zero,
                    end = Offset.Infinite,
                ),
            )
            .border(
                width = 1.dp,
                color = accent.copy(alpha = 0.30f),
                shape = RoundedCornerShape(SkydownUiTokens.tightRadius),
            )
            .border(
                width = if (isActive) 1.6.dp else 1.dp,
                brush = Brush.linearGradient(
                    colors = instagramGradientColors.map { color ->
                        color.copy(alpha = if (isActive) 0.55f else 0.28f)
                    },
                    start = Offset.Zero,
                    end = Offset.Infinite,
                ),
                shape = RoundedCornerShape(SkydownUiTokens.tightRadius),
            )
            .skydownPressable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(SkydownUiTokens.fullCapsuleRadius))
                .background(accent.copy(alpha = if (isActive) 0.34f else 0.20f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.ArrowOutward,
                contentDescription = null,
                tint = if (isActive) Color.White else accent,
                modifier = Modifier.size(13.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingSingle),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = colorScheme.skydownText(),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.skydownSecondaryText(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (isActive) {
            Text(
                text = stringResource(R.string.music_hub_status_active),
                style = MaterialTheme.typography.labelSmall,
                color = accent.copy(alpha = 0.96f),
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(SkydownUiTokens.fullCapsuleRadius))
                    .background(accent.copy(alpha = 0.16f))
                    .padding(horizontal = 7.dp, vertical = 4.dp),
            )
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = colorScheme.skydownSecondaryText().copy(alpha = 0.72f),
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ZweizweiMusicLaneScreen(
    onOpenCart: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onGuestSignIn: (() -> Unit)? = null,
    onBackToLanding: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val compactLayout = rememberIsCompactAppLayout()
    val compactVisualDensity = rememberUsesCompactVisualDensity()
    val useCompactHubVisuals = compactLayout || compactVisualDensity
    var destination by rememberSaveable { mutableStateOf(ZweizweiMusicDestination.Hub) }
    var catalogInitialArtist by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedArtistPage by rememberSaveable { mutableStateOf<String?>(null) }
    var artistPageReturnDestination by rememberSaveable { mutableStateOf(ZweizweiMusicDestination.Hub) }
    var highlightedSocialArtist by rememberSaveable { mutableStateOf("JANNO") }
    val hubListState = rememberLazyListState()
    val hubHorizontalPadding = if (useCompactHubVisuals) 15.dp else 16.dp
    val hubTopPadding = if (useCompactHubVisuals) 12.dp else 18.dp
    val hubBottomPadding = if (useCompactHubVisuals) 18.dp else 24.dp
    val hubSectionSpacing = if (useCompactHubVisuals) 9.dp else 11.dp
    val useCompactHubHero = useCompactHubVisuals

    Box(
        modifier = Modifier
            .fillMaxSize()
            .skydownSelectionFeedback(trigger = destination),
    ) {
        when (destination) {
        ZweizweiMusicDestination.Hub -> Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                TopAppBar(
                    title = {
                        SkydownTopBarTitle(
                            title = stringResource(R.string.tabs_music),
                            subtitle = if (compactLayout) null else stringResource(R.string.music_hub_subtitle),
                        )
                    },
                    navigationIcon = if (onBackToLanding != null) {
                        {
                            IconButton(onClick = onBackToLanding) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.common_back),
                                )
                            }
                        }
                    } else {
                        {}
                    },
                    actions = {
                        AppTopBarSessionActions(
                            onOpenCart = onOpenCart,
                            onOpenProfile = onOpenProfile,
                            onOpenSettings = onOpenSettings,
                            onGuestSignIn = onGuestSignIn,
                            dense = useCompactHubVisuals,
                        )
                    },
                    colors = skydownTopBarColors(),
                )
            },
        ) { innerPadding ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .skydownAtmosphereBackground(
                        primaryColor = SpotifyGreen,
                        secondaryColor = MaterialTheme.colorScheme.secondary,
                        primaryAlpha = 0.095f,
                        secondaryAlpha = 0.060f,
                    ),
            ) {
                val isWideLayout = maxWidth >= 900.dp
                val contentMaxWidth = when {
                    isWideLayout -> maxWidth
                    maxWidth >= 640.dp -> 620.dp
                    else -> maxWidth
                }
                val isShortHubHeight = !isWideLayout && maxHeight < 760.dp
                val resolvedHubHorizontalPadding = if (isShortHubHeight) 12.dp else hubHorizontalPadding
                val resolvedHubTopPadding = if (isShortHubHeight) 8.dp else hubTopPadding
                val resolvedHubBottomPadding = if (isShortHubHeight) 14.dp else hubBottomPadding
                val resolvedHubSectionSpacing = if (isShortHubHeight) 7.dp else hubSectionSpacing
                val songDetailText = if (isShortHubHeight) {
                    stringResource(R.string.music_hub_song_detail_short)
                } else {
                    stringResource(R.string.music_hub_song_detail_long)
                }
                val beatDetailText = if (isShortHubHeight) {
                    stringResource(R.string.music_hub_beat_detail_short)
                } else {
                    stringResource(R.string.music_hub_beat_detail_long)
                }
                val studioDetailText = if (isShortHubHeight) {
                    stringResource(R.string.music_hub_studio_detail_short)
                } else {
                    stringResource(R.string.music_hub_studio_detail_long)
                }
                val useAnchoredHubLayout = !isWideLayout && maxHeight >= 900.dp && maxWidth < 560.dp
                val hubBottomScrollReserve = when {
                    useAnchoredHubLayout -> 180.dp
                    isShortHubHeight -> 104.dp
                    else -> 144.dp
                }
                val screenHeaderSettings by AppContainer.screenHeaderSettingsRepository.settings.collectAsStateWithLifecycle()

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = contentMaxWidth)
                            .testTag("music.hub.root"),
                        state = hubListState,
                        contentPadding = PaddingValues(
                            start = resolvedHubHorizontalPadding,
                            top = 0.dp,
                            end = resolvedHubHorizontalPadding,
                            bottom = innerPadding.calculateBottomPadding() + resolvedHubBottomPadding + hubBottomScrollReserve,
                        ),
                        verticalArrangement = Arrangement.spacedBy(resolvedHubSectionSpacing),
                        userScrollEnabled = true,
                    ) {
                        item {
                            BrandHeroCard(
                                eyebrow = screenHeaderSettings.musicHubEyebrow.ifBlank { stringResource(R.string.brand_system_name) },
                                title = screenHeaderSettings.musicHubTitle.ifBlank { stringResource(R.string.tabs_music) },
                                subtitle = screenHeaderSettings.musicHubSubtitle.ifBlank { stringResource(R.string.music_hub_subtitle) },
                                detail = screenHeaderSettings.musicHubDetail.ifBlank { stringResource(R.string.music_hub_detail) },
                                backgroundImageUrl = screenHeaderSettings.musicHubImageUrl.ifBlank { null },
                                accent = MaterialTheme.colorScheme.primary,
                                secondaryAccent = MaterialTheme.colorScheme.secondary,
                                marks = listOf(BrandArtwork.Zweizwei),
                                compactVisualDensity = useCompactHubHero,
                                edgeToEdge = true,
                                topContentPadding = innerPadding.calculateTopPadding() + resolvedHubTopPadding,
                                onSurfaceClick = {
                                    catalogInitialArtist = "JANNO"
                                    destination = ZweizweiMusicDestination.Catalog
                                },
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro)) {
                                    BrandPill(
                                        text = stringResource(R.string.music_hub_catalog_title),
                                        tint = MaterialTheme.colorScheme.primary,
                                        onClick = {
                                            catalogInitialArtist = "JANNO"
                                            destination = ZweizweiMusicDestination.Catalog
                                        },
                                    )
                                    BrandPill(
                                        text = stringResource(R.string.music_hub_studio_title),
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        onClick = { destination = ZweizweiMusicDestination.NicmaProducer },
                                    )
                                }
                            }
                        }

                        item {
                            MusicHubQuickActions(
                                onOpenCatalog = {
                                    catalogInitialArtist = "JANNO"
                                    destination = ZweizweiMusicDestination.Catalog
                                },
                                onOpenStudio = { destination = ZweizweiMusicDestination.NicmaProducer },
                                activeSocialTitle = highlightedSocialArtist,
                                onOpenSocialLink = {
                                    highlightedSocialArtist = musicHubInstagramQuickLinks
                                        .firstOrNull { link -> link.url == it }
                                        ?.title
                                        ?: highlightedSocialArtist
                                    openExternalLink(context, it)
                                },
                            )
                        }

                        item {
                            Spacer(
                                modifier = Modifier
                                    .height(if (isShortHubHeight) 24.dp else 36.dp)
                                    .testTag("music.hub.scroll.end"),
                            )
                        }
                    }
                }
            }
        }

        ZweizweiMusicDestination.Catalog -> MusicScreen(
            initialArtist = catalogInitialArtist,
            onArtistContextChange = { highlightedSocialArtist = it },
            onBack = {
                catalogInitialArtist = null
                destination = ZweizweiMusicDestination.Hub
            },
            onOpenStudio = { destination = ZweizweiMusicDestination.NicmaProducer },
            onOpenCart = onOpenCart,
            onOpenProfile = onOpenProfile,
            onOpenSettings = onOpenSettings,
            onGuestSignIn = onGuestSignIn,
            onOpenArtistPage = { artistName ->
                selectedArtistPage = artistName
                artistPageReturnDestination = ZweizweiMusicDestination.Catalog
                destination = ZweizweiMusicDestination.ArtistPage
            },
        )

        ZweizweiMusicDestination.ArtistPage -> ArtistPageScreen(
            artistName = selectedArtistPage ?: "Artist",
            brand = ArtistPageBrand.Zweizwei,
            onBack = { destination = artistPageReturnDestination },
        )

        ZweizweiMusicDestination.NicmaProducer -> NicmaProducerScreen(
            onBack = { destination = ZweizweiMusicDestination.Hub },
        )
        }
    }
}

@Composable
private fun LaunchLandingButton(
    title: String,
    subtitle: String,
    detail: String,
    chips: List<String>,
    accentColor: androidx.compose.ui.graphics.Color,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compactVisualDensity: Boolean = false,
    backgroundImageUrl: String? = null,
) {
    HubEntryCard(
        title = title,
        subtitle = subtitle,
        detail = detail,
        chips = chips,
        accentColor = accentColor,
        icon = icon,
        onClick = onClick,
        compactVisualDensity = compactVisualDensity,
        backgroundImageUrl = backgroundImageUrl,
        modifier = modifier,
    )
}

@Composable
private fun HubEntryCard(
    title: String,
    subtitle: String,
    accentColor: androidx.compose.ui.graphics.Color,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compactVisualDensity: Boolean = false,
    detail: String? = null,
    chips: List<String> = emptyList(),
    eyebrow: String? = null,
    artwork: BrandArtwork? = null,
    emphasized: Boolean = false,
    pathTier: LaunchLandingPathTier = LaunchLandingPathTier.Primary,
    backgroundImageUrl: String? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val colorScheme = MaterialTheme.colorScheme
    val isDarkPalette = colorScheme.skydownIsDarkPalette()
    val isSecondary = pathTier == LaunchLandingPathTier.Secondary
    val cardShape = RoundedCornerShape(
        if (isSecondary) {
            if (compactVisualDensity) SkydownUiTokens.heroPadding else SkydownUiTokens.cardCornerRadius
        } else if (compactVisualDensity) {
            SkydownUiTokens.sheetHeroRadius
        } else {
            SkydownUiTokens.spotlightRadius
        },
    )
    val panelShadowRadius = if (isSecondary) {
        2.dp
    } else {
        when {
            emphasized && compactVisualDensity -> SkydownUiTokens.denseRadius
            emphasized -> SkydownUiTokens.layoutProminentInset
            compactVisualDensity -> SkydownUiTokens.stackSpacingDockRow
            else -> SkydownUiTokens.denseRadius
        }
    }
    val panelShadowYOffset = if (isSecondary) {
        1.dp
    } else {
        when {
            emphasized && compactVisualDensity -> SkydownUiTokens.stackSpacingSnug
            emphasized -> SkydownUiTokens.stackSpacingToast
            compactVisualDensity -> SkydownUiTokens.stackSpacingChrome
            else -> SkydownUiTokens.stackSpacingSnug
        }
    }
    val iconTileSize = when {
        isSecondary -> if (compactVisualDensity) 48.dp else 52.dp
        emphasized && compactVisualDensity -> 60.dp
        emphasized -> 62.dp
        compactVisualDensity -> 56.dp
        else -> 60.dp
    }
    val minCardHeight = when {
        isSecondary -> if (compactVisualDensity) 128.dp else 138.dp
        emphasized && compactVisualDensity -> 210.dp
        emphasized -> 222.dp
        compactVisualDensity -> 194.dp
        else -> 204.dp
    }
    val titleTextStyle = if (isSecondary) {
        MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
    } else {
        SkydownCardTitleTextStyle
    }
    val hasBackgroundImage = !backgroundImageUrl.isNullOrBlank()
    var backgroundLuminance by remember(backgroundImageUrl) { mutableStateOf<Float?>(null) }
    LaunchedEffect(backgroundImageUrl) {
        backgroundLuminance = loadRemoteImageLuminance(backgroundImageUrl)
    }
    val readabilityFloor = if (hasBackgroundImage) {
        when (val luminance = backgroundLuminance) {
            null -> 0.34f
            else -> {
                val curve = luminance * luminance
                (0.24f + (curve * 0.40f)).coerceIn(0.24f, 0.64f)
            }
        }
    } else {
        0f
    }
    val chipsToShow = if (isSecondary) emptyList() else chips.take(3)
    val primaryContentColor = when {
        hasBackgroundImage -> Color.White.copy(alpha = if (isSecondary) 0.98f else 1.0f)
        isDarkPalette -> Color.White.copy(alpha = if (isSecondary) 0.82f else 0.94f)
        else -> colorScheme.skydownText().copy(alpha = if (isSecondary) 0.82f else 0.94f)
    }
    val secondaryContentColor = when {
        hasBackgroundImage -> Color.White.copy(alpha = if (isSecondary) 0.90f else 0.94f)
        isDarkPalette -> Color.White.copy(alpha = if (isSecondary) 0.56f else 0.78f)
        else -> colorScheme.skydownSecondaryText().copy(alpha = if (isSecondary) 0.78f else 0.92f)
    }
    val subtleContentColor = when {
        hasBackgroundImage -> Color.White.copy(alpha = if (isSecondary) 0.82f else 0.86f)
        isDarkPalette -> Color.White.copy(alpha = if (isSecondary) 0.48f else 0.68f)
        else -> colorScheme.skydownSecondaryText().copy(alpha = if (isSecondary) 0.70f else 0.82f)
    }
    val shouldShowEyebrow = !eyebrow.isNullOrBlank() &&
        !title.startsWith(eyebrow, ignoreCase = true)
    val normalizedSubtitle = subtitle.normalizedUiComparisonText()
    val normalizedDetail = detail?.normalizedUiComparisonText().orEmpty()
    val shouldShowDetail = !detail.isNullOrBlank() &&
        normalizedDetail.isNotEmpty() &&
        normalizedDetail != normalizedSubtitle

    Surface(
        shape = cardShape,
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = null,
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .skydownPressable(interactionSource)
            .skydownPanelSurface(
                accent = accentColor,
                cornerRadius = if (isSecondary) {
                    if (compactVisualDensity) 20.dp else 22.dp
                } else if (compactVisualDensity) {
                    26.dp
                } else {
                    28.dp
                },
                shadowRadius = panelShadowRadius,
                shadowYOffset = panelShadowYOffset,
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minCardHeight)
                .background(Color.Transparent),
        ) {
            if (!backgroundImageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = backgroundImageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .matchParentSize()
                        .alpha(if (isSecondary) 0.44f else 0.52f),
                )
            }
            if (hasBackgroundImage) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = if (isSecondary) 0.18f else 0.14f)),
                )
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        if (isSecondary) {
                            Brush.linearGradient(
                                colors = listOf(
                                    colorScheme.surface.copy(alpha = if (isDarkPalette) 0.06f else 0.12f),
                                    colorScheme.surfaceVariant.copy(alpha = if (isDarkPalette) if (hasBackgroundImage) 0.14f else 0.18f else if (hasBackgroundImage) 0.18f else 0.24f),
                                    accentColor.copy(alpha = if (isDarkPalette) 0.035f else 0.03f),
                                    colorScheme.skydownCinematicShadow().copy(alpha = if (isDarkPalette) if (hasBackgroundImage) 0.12f else 0.12f else if (hasBackgroundImage) 0.08f else 0.06f),
                                ),
                                start = Offset.Zero,
                                end = Offset.Infinite,
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(
                                    colorScheme.skydownLuminanceLift().copy(alpha = if (isDarkPalette) 0.06f else 0.12f),
                                    accentColor.copy(alpha = if (isDarkPalette) 0.07f else 0.04f),
                                    colorScheme.skydownCardBackground().copy(alpha = if (isDarkPalette) if (hasBackgroundImage) 0.14f else 0.16f else if (hasBackgroundImage) 0.22f else 0.30f),
                                    colorScheme.skydownCinematicShadow().copy(alpha = if (isDarkPalette) if (hasBackgroundImage) 0.18f else 0.21f else if (hasBackgroundImage) 0.08f else 0.04f),
                                ),
                                start = Offset.Zero,
                                end = Offset.Infinite,
                            )
                        },
                    ),
            )
            if (hasBackgroundImage) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = (readabilityFloor * 0.24f).coerceIn(0.08f, 0.22f)),
                                    Color.Black.copy(alpha = (readabilityFloor * 0.54f).coerceIn(0.16f, 0.36f)),
                                    Color.Black.copy(alpha = (readabilityFloor + 0.10f).coerceIn(0.28f, 0.56f)),
                                ),
                            ),
                        ),
                )
            }
            if (!isSecondary) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(if (compactVisualDensity) 116.dp else 132.dp)
                        .offset(x = 24.dp, y = (-24).dp)
                        .background(accentColor.copy(alpha = if (isDarkPalette) 0.16f else 0.10f), RoundedCornerShape(SkydownUiTokens.fullCapsuleRadius))
                        .blur(34.dp),
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(
                        start = if (isSecondary) SkydownUiTokens.cardPadding else SkydownUiTokens.cardCornerRadius,
                        top = if (isSecondary) SkydownUiTokens.compactRadius else SkydownUiTokens.heroPadding,
                    )
                    .width(if (isSecondary) 40.dp else 74.dp)
                    .height(if (isSecondary) 2.dp else 3.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                colorScheme.skydownLuminanceLift().copy(alpha = if (isDarkPalette) 0.26f else 0.34f),
                                accentColor.copy(alpha = if (isSecondary) 0.28f else 0.70f),
                                accentColor.copy(alpha = 0.06f),
                            ),
                        ),
                        RoundedCornerShape(SkydownUiTokens.fullCapsuleRadius),
                    ),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = if (isSecondary) {
                            if (compactVisualDensity) SkydownUiTokens.compactRadius else SkydownUiTokens.cardPadding
                        } else if (compactVisualDensity) {
                            SkydownUiTokens.stackSpacingSection
                        } else {
                            SkydownUiTokens.cardCornerRadius
                        },
                        vertical = if (isSecondary) {
                            if (compactVisualDensity) SkydownUiTokens.compactRadius else SkydownUiTokens.cardPadding
                        } else if (compactVisualDensity) {
                            SkydownUiTokens.layoutProminentInset
                        } else {
                            SkydownUiTokens.cardCornerRadius
                        },
                    ),
                verticalArrangement = Arrangement.spacedBy(
                    when {
                        isSecondary ->
                            if (compactVisualDensity) {
                                SkydownUiTokens.stackSpacingPill
                            } else {
                                SkydownUiTokens.stackSpacingCompact
                            }
                        compactVisualDensity -> SkydownUiTokens.stackSpacingRelaxed
                        else -> SkydownUiTokens.stackSpacingComfortable
                    },
                ),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingDockRow),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        modifier = Modifier
                            .size(iconTileSize)
                            .then(
                                if (isSecondary) {
                                    Modifier
                                        .clip(RoundedCornerShape(SkydownUiTokens.compactRadius))
                                        .background(colorScheme.surface.copy(alpha = if (isDarkPalette) 0.14f else 0.20f))
                                        .border(
                                            width = 0.5.dp,
                                            color = accentColor.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(SkydownUiTokens.compactRadius),
                                        )
                                } else {
                                    Modifier.skydownPanelSurface(
                                        accent = accentColor,
                                        cornerRadius = 18.dp,
                                        shadowRadius = 7.dp,
                                        shadowYOffset = 3.dp,
                                    )
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (artwork != null) {
                            Image(
                                painter = painterResource(id = artwork.drawableRes),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(iconTileSize)
                                    .padding(if (isSecondary) 7.dp else 9.dp),
                                contentScale = ContentScale.Fit,
                            )
                        } else {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = accentColor.copy(alpha = if (isSecondary) 0.55f else 1f),
                                modifier = Modifier.size(if (isSecondary) 22.dp else 26.dp),
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingDense),
                    ) {
                        if (shouldShowEyebrow) {
                            if (isSecondary) {
                                Text(
                                    text = eyebrow,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = accentColor.copy(alpha = 0.58f),
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            } else {
                                Text(
                                    text = eyebrow.uppercase(),
                                    style = SkydownHeroEyebrowTextStyle,
                                    color = accentColor.copy(alpha = 0.92f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Text(
                            text = title,
                            style = titleTextStyle,
                            color = primaryContentColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = subtitle,
                            style = SkydownEditorialCaptionTextStyle,
                            color = secondaryContentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    if (isSecondary) {
                        Icon(
                            imageVector = Icons.Default.ArrowOutward,
                            contentDescription = null,
                            tint = primaryContentColor.copy(alpha = 0.38f),
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(16.dp),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(if (compactVisualDensity) 38.dp else 40.dp)
                                .skydownCapsuleSurface(accent = accentColor),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowOutward,
                                contentDescription = null,
                                tint = primaryContentColor.copy(alpha = 0.84f),
                                modifier = Modifier
                                    .alpha(0.92f)
                                    .size(if (compactVisualDensity) 17.dp else 18.dp),
                            )
                        }
                    }
                }

                if (shouldShowDetail) {
                    Text(
                        text = detail,
                        style = SkydownEditorialCaptionTextStyle,
                        color = subtleContentColor.copy(alpha = if (isSecondary) 0.92f else 1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (chipsToShow.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingDense)) {
                        chipsToShow.forEach { chip ->
                            Text(
                                text = chip,
                                style = if (isSecondary) MaterialTheme.typography.labelSmall else SkydownBodyCaptionTextStyle,
                                color = accentColor.copy(alpha = if (isSecondary) 0.42f else 0.78f),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(SkydownUiTokens.fullCapsuleRadius))
                                    .background(colorScheme.onSurface.copy(alpha = if (isSecondary) 0.06f else 0.04f))
                                    .padding(
                                        horizontal = if (isSecondary) SkydownUiTokens.stackSpacingMicro else SkydownUiTokens.stackSpacingPill,
                                        vertical = if (isSecondary) SkydownUiTokens.stackSpacingNano else SkydownUiTokens.stackSpacingDense,
                                    ),
                            )
                        }
                    }
                }

                if (!isSecondary) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.skydown_launch_direct_in, title),
                            style = SkydownBodyCaptionTextStyle,
                            color = primaryContentColor,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingDense),
                            verticalAlignment = Alignment.CenterVertically,
                        )
                        {
                            Text(
                                text = stringResource(R.string.skydown_launch_start),
                                style = SkydownBodyCaptionTextStyle,
                                color = primaryContentColor.copy(alpha = 0.78f),
                                fontWeight = FontWeight.SemiBold,
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowOutward,
                                contentDescription = null,
                                tint = primaryContentColor.copy(alpha = 0.72f),
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

private suspend fun loadRemoteImageLuminance(url: String?): Float? {
    val normalizedUrl = url?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return withContext(Dispatchers.IO) {
        runCatching {
            URL(normalizedUrl).openStream().use { stream ->
                val bitmap = BitmapFactory.decodeStream(stream) ?: return@runCatching null
                bitmap.useAverageLuminance()
            }
        }.getOrNull()
    }
}

private fun Bitmap.useAverageLuminance(): Float {
    return try {
        averageLuminance()
    } finally {
        if (!isRecycled) recycle()
    }
}

private fun Bitmap.averageLuminance(): Float {
    if (width <= 0 || height <= 0) return 0.5f
    val stepX = (width / 24).coerceAtLeast(1)
    val stepY = (height / 24).coerceAtLeast(1)
    var samples = 0
    var luminanceSum = 0.0
    var y = 0
    while (y < height) {
        var x = 0
        while (x < width) {
            val pixel = getPixel(x, y)
            val red = android.graphics.Color.red(pixel) / 255.0
            val green = android.graphics.Color.green(pixel) / 255.0
            val blue = android.graphics.Color.blue(pixel) / 255.0
            luminanceSum += (0.2126 * red) + (0.7152 * green) + (0.0722 * blue)
            samples += 1
            x += stepX
        }
        y += stepY
    }
    return if (samples == 0) 0.5f else (luminanceSum / samples).toFloat().coerceIn(0f, 1f)
}

private fun String.normalizedUiComparisonText(): String {
    return lowercase()
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()
}

@Composable
private fun HubSignalCard(
    title: String,
    value: String,
    accentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDarkPalette = colorScheme.skydownIsDarkPalette()
    val interactionSource = remember { MutableInteractionSource() }
    val signalModifier = modifier
        .skydownPanelSurface(
            accent = accentColor,
            cornerRadius = 22.dp,
            shadowRadius = 10.dp,
            shadowYOffset = 5.dp,
        )
        .then(
            Modifier
                .skydownPressable(interactionSource)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ),
        )

    Box(
        modifier = signalModifier.background(
            Brush.linearGradient(
                colors = listOf(
                    colorScheme.skydownLuminanceLift().copy(alpha = if (isDarkPalette) 0.06f else 0.15f),
                    accentColor.copy(alpha = if (isDarkPalette) 0.08f else 0.055f),
                    colorScheme.skydownCinematicShadow().copy(alpha = if (isDarkPalette) 0.12f else 0.02f),
                ),
            ),
        ),
    ) {
        Column(
            modifier = Modifier.padding(SkydownUiTokens.cardPadding),
            verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill),
        ) {
            Text(
                text = title.uppercase(),
                style = SkydownHeroEyebrowTextStyle,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = value,
                style = SkydownEditorialCaptionTextStyle,
                color = colorScheme.skydownText().copy(alpha = 0.86f),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 12.dp, start = 16.dp)
                .width(52.dp)
                .height(3.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.90f),
                            accentColor.copy(alpha = 0.08f),
                        ),
                    ),
                    RoundedCornerShape(SkydownUiTokens.fullCapsuleRadius),
                ),
        )
    }
}

@Composable
private fun LaunchLandingMetaPill(
    text: String,
    accent: Color,
    onClick: () -> Unit = {},
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDarkPalette = colorScheme.skydownIsDarkPalette()
    val contentColor = if (isDarkPalette) {
        Color.White.copy(alpha = 0.90f)
    } else {
        colorScheme.skydownText().copy(alpha = 0.88f)
    }

    val interactionSource = remember { MutableInteractionSource() }
    Text(
        text = text,
        style = SkydownEditorialCaptionTextStyle,
        color = contentColor,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .skydownCapsuleSurface(accent = accent)
            .skydownPressable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .heightIn(min = 44.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    )
}

private tailrec fun Context.findComponentActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findComponentActivity()
    else -> null
}
