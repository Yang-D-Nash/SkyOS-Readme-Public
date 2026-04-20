package com.skydown.android.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.media3.common.util.UnstableApi
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.skydown.android.R
import com.google.firebase.firestore.FirebaseFirestore
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skydown.android.data.AppContainer
import com.skydown.android.data.AppFeatureFlagsStore
import com.skydown.android.data.AppNetworkMonitor
import com.skydown.android.data.AppSessionStore
import com.skydown.android.data.ArtistPageBrand
import com.skydown.android.ui.component.BrandArtwork
import com.skydown.android.ui.component.BrandHeroCard
import com.skydown.android.ui.component.BrandPill
import com.skydown.android.ui.component.ConnectivityStatusBanner
import com.skydown.android.ui.component.AppTopBarSessionActions
import com.skydown.android.ui.component.LocalSessionUser
import com.skydown.android.ui.component.SkydownMotionTokens
import com.skydown.android.ui.component.SkydownTopBarTitle
import com.skydown.android.ui.component.rememberIsCompactAppLayout
import com.skydown.android.ui.component.rememberUsesCompactVisualDensity
import com.skydown.android.ui.component.skydownLuminousSweep
import com.skydown.android.ui.component.skydownPressable
import com.skydown.android.ui.component.skydownSelectionFeedback
import com.skydown.android.ui.component.skydownScreenBrush
import com.skydown.android.ui.component.skydownTopBarColors
import com.skydown.android.ui.screen.AiHubScreen
import com.skydown.android.ui.screen.ArtistPageScreen
import com.skydown.android.ui.screen.BeatHubScreen
import com.skydown.android.ui.screen.CartScreen
import com.skydown.android.ui.screen.HomeScreen
import com.skydown.android.ui.screen.IntroScreen
import com.skydown.android.ui.screen.LoginScreen
import com.skydown.android.ui.screen.MusicScreen
import com.skydown.android.ui.screen.NicmaProducerScreen
import com.skydown.android.ui.screen.OrderScreen
import com.skydown.android.ui.screen.ProfileScreen
import com.skydown.android.ui.screen.RegistrationScreen
import com.skydown.android.ui.screen.SettingsScreen
import com.skydown.android.ui.screen.ShopScreen
import com.skydown.android.ui.screen.VideoHubScreen
import com.skydown.android.ui.theme.BackgroundDark
import com.skydown.android.ui.theme.SpotifyGreen
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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
    val navController = rememberNavController()
    val isCompactLayout = rememberIsCompactAppLayout()
    val currentUser by AppContainer.currentUser.collectAsStateWithLifecycle()
    val aiAccessMode by AppFeatureFlagsStore.aiAccessMode.collectAsStateWithLifecycle()
    val isOnline by AppNetworkMonitor.isOnline.collectAsStateWithLifecycle()
    var showIntro by rememberSaveable(skipIntro) { mutableStateOf(!skipIntro) }
    var selectedEntryRoute by rememberSaveable(startRouteOverride) { mutableStateOf(startRouteOverride) }
    var showsWorkflowWorkspace by rememberSaveable { mutableStateOf(false) }
    var authSheet by rememberSaveable { mutableStateOf<AuthSheet?>(null) }
    var authSheetLocked by rememberSaveable { mutableStateOf(false) }
    var showOrders by rememberSaveable { mutableStateOf(false) }
    var observedAuthUid by rememberSaveable { mutableStateOf<String?>(null) }
    var initialSettingsWorkspaceKey by rememberSaveable { mutableStateOf<String?>(null) }
    val auth = remember { FirebaseAuth.getInstance() }
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
    val hasAiAccess = AppFeatureFlagsStore.allowsAiAccess(
        user = currentUser,
        accessMode = aiAccessMode,
    )

    LaunchedEffect(currentUser?.id) {
        if (currentUser != null && authSheet != null) {
            authSheetLocked = false
            authSheet = null
        }
    }

    LaunchedEffect(authSheet) {
        if (authSheet == null && authSheetLocked) {
            authSheetLocked = false
        }
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
        val destinations = buildList {
            add(BottomDestination("shop", stringResource(R.string.tabs_merch), MaterialTheme.colorScheme.tertiary) { _ ->
                Icon(Icons.Default.ShoppingBag, contentDescription = null)
            })
            add(BottomDestination("music", stringResource(R.string.tabs_music), SpotifyGreen) { _ ->
                Icon(Icons.Default.GraphicEq, contentDescription = null)
            })
            add(BottomDestination("home", stringResource(R.string.tabs_home), MaterialTheme.colorScheme.primary) { _ ->
                Icon(Icons.Default.Home, contentDescription = null)
            })
            add(BottomDestination("video", stringResource(R.string.tabs_videos), Color(0xFFFF6E74)) { _ ->
                Icon(Icons.Default.PlayCircleFilled, contentDescription = null)
            })
            add(BottomDestination("ai", stringResource(R.string.tabs_tools), MaterialTheme.colorScheme.secondary) { _ ->
                Icon(Icons.Default.AutoAwesome, contentDescription = null)
            })
        }

        if (showIntro) {
            IntroScreen(
                onFinished = { showIntro = false },
            )
        } else if (selectedEntryRoute == null) {
            LaunchLandingScreen(
                onOpenMusic = { selectedEntryRoute = "music" },
                onOpenVideography = { selectedEntryRoute = "video" },
                onOpenShop = { selectedEntryRoute = "shop" },
            )
        } else {
            val startRoute = selectedEntryRoute ?: "home"
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val showFloatingDock = currentDestination?.route != "ai" && !WindowInsets.isImeVisible
            val floatingDockContentPadding = if (showFloatingDock) {
                if (isCompactLayout) 106.dp else 116.dp
            } else {
                0.dp
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
            ) { _ ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(skydownScreenBrush()),
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
                                onOpenWorkflow = if (hasAiAccess) {
                                    {
                                        showsWorkflowWorkspace = true
                                        navController.navigate("ai") {
                                            launchSingleTop = true
                                        }
                                    }
                                } else {
                                    null
                                },
                            )
                        }
                        composable("shop") {
                            ShopScreen(
                                onOpenLogin = { authSheet = AuthSheet.Login },
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
                            )
                        }
                        composable("video") {
                            VideoHubScreen(
                                onOpenCart = openCart,
                                onOpenProfile = openProfile,
                                onOpenSettings = openSettings,
                            )
                        }
                        composable("cart") {
                            CartScreen(
                                onBack = { navController.popBackStack() },
                                onOpenLogin = { authSheet = AuthSheet.Login },
                                onOpenProfile = openProfile,
                                onOpenSettings = openSettings,
                            )
                        }
                        composable("ai") {
                            AiHubScreen(
                                showsWorkflowWorkspace = showsWorkflowWorkspace,
                                onToggleWorkflow = { showsWorkflowWorkspace = !showsWorkflowWorkspace },
                                onHideWorkflow = { showsWorkflowWorkspace = false },
                                onOpenLogin = { authSheet = AuthSheet.Login },
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
                                onOpenLogin = { authSheet = AuthSheet.Login },
                                onOpenRegistration = { authSheet = AuthSheet.Registration },
                                onOpenProfile = openProfile,
                                onOpenOrders = { showOrders = true },
                            )
                        }
                        composable("profile") {
                            ProfileScreen(
                                onBack = { navController.popBackStack() },
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
                                if (currentDestination?.route == "settings") {
                                    navController.popBackStack()
                                }

                                if (navController.currentDestination?.route != destination.route) {
                                    navController.navigate(destination.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
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
                    if (!authSheetLocked) {
                        authSheet = null
                    }
                },
                sheetState = authSheetState,
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                when (sheet) {
                    AuthSheet.Login -> LoginScreen(
                        onClose = { authSheet = null },
                        onOpenRegistration = { authSheet = AuthSheet.Registration },
                        onBusyStateChanged = { authSheetLocked = it },
                    )
                    AuthSheet.Registration -> RegistrationScreen(
                        onClose = { authSheet = null },
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
    val selectedAccentTarget = destinations.firstOrNull { isSelected(it) }?.accent
        ?: MaterialTheme.colorScheme.primary
    val selectedAccent by animateColorAsState(
        targetValue = selectedAccentTarget,
        animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing),
        label = "bottomDockAccent",
    )
    val dockBorder by animateColorAsState(
        targetValue = lerp(Color.White.copy(alpha = 0.14f), selectedAccent.copy(alpha = 0.34f), 0.42f),
        animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing),
        label = "bottomDockBorder",
    )
    val dockGlow by animateColorAsState(
        targetValue = selectedAccent.copy(alpha = 0.18f),
        animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing),
        label = "bottomDockGlow",
    )
    val dockShape = RoundedCornerShape(if (isCompactLayout) 28.dp else 32.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = if (isCompactLayout) 10.dp else 18.dp)
            .padding(top = 6.dp, bottom = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = if (isCompactLayout) 438.dp else 572.dp)
                .fillMaxWidth(if (isCompactLayout) 0.965f else 0.84f),
            shape = dockShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.46f),
            tonalElevation = 0.dp,
            shadowElevation = if (isCompactLayout) 18.dp else 22.dp,
            border = BorderStroke(
                width = 1.dp,
                color = dockBorder,
            ),
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.13f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.46f),
                                selectedAccent.copy(alpha = 0.12f),
                                dockGlow.copy(alpha = 0.44f),
                            ),
                        ),
                    )
                    .skydownLuminousSweep(
                        shape = dockShape,
                        accent = selectedAccent,
                        alpha = 0.16f,
                    ),
            ) {
                NavigationBar(
                    modifier = Modifier
                        .height(if (isCompactLayout) 82.dp else 88.dp)
                        .padding(
                            horizontal = if (isCompactLayout) 4.dp else 8.dp,
                            vertical = 6.dp,
                        ),
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp,
                ) {
                    destinations.forEach { destination ->
                        val selected = isSelected(destination)
                        val iconScale by animateFloatAsState(
                            targetValue = if (selected) 1f else 0.94f,
                            animationSpec = spring(dampingRatio = 0.84f, stiffness = 620f),
                            label = "bottomNavIconScale_${destination.route}",
                        )
                        val iconLift by animateFloatAsState(
                            targetValue = if (selected) -1f else 0f,
                            animationSpec = spring(dampingRatio = 0.86f, stiffness = 680f),
                            label = "bottomNavIconLift_${destination.route}",
                        )
                        val labelAlpha by animateFloatAsState(
                            targetValue = if (selected) 0.98f else 0.72f,
                            animationSpec = tween(
                                durationMillis = 220,
                                easing = FastOutSlowInEasing,
                            ),
                            label = "bottomNavLabelAlpha_${destination.route}",
                        )

                        NavigationBarItem(
                            selected = selected,
                            onClick = { onDestinationClick(destination) },
                            icon = {
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
                            },
                            label = {
                                Text(
                                    modifier = Modifier.alpha(labelAlpha),
                                    text = destination.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onSurface,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                indicatorColor = selectedAccent.copy(alpha = 0.18f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
                            ),
                        )
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
private val skydownOverlayRoutes = setOf("cart", "settings", "profile")

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
                    durationMillis = 280,
                    delayMillis = 60,
                    easing = LinearOutSlowInEasing,
                ),
            ) + slideIntoContainer(
                towards = direction,
                animationSpec = tween(
                    durationMillis = SkydownMotionTokens.primaryEnterDurationMillis,
                    easing = FastOutSlowInEasing,
                ),
                initialOffset = { fullSize -> (fullSize * 0.07f).toInt() },
            ) + scaleIn(
                initialScale = 0.989f,
                animationSpec = tween(
                    durationMillis = SkydownMotionTokens.primaryEnterDurationMillis,
                    easing = FastOutSlowInEasing,
                ),
            )
        }

        targetRoute in skydownOverlayRoutes -> {
            fadeIn(
                animationSpec = tween(
                    durationMillis = 240,
                    delayMillis = 40,
                    easing = LinearOutSlowInEasing,
                ),
            ) + slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Up,
                animationSpec = tween(
                    durationMillis = SkydownMotionTokens.overlayEnterDurationMillis,
                    easing = FastOutSlowInEasing,
                ),
                initialOffset = { fullSize -> (fullSize * 0.10f).toInt() },
            ) + scaleIn(
                initialScale = 0.986f,
                animationSpec = tween(
                    durationMillis = SkydownMotionTokens.overlayEnterDurationMillis,
                    easing = FastOutSlowInEasing,
                ),
            )
        }

        else -> {
            fadeIn(
                animationSpec = tween(
                    durationMillis = 220,
                    delayMillis = 50,
                    easing = LinearOutSlowInEasing,
                ),
            ) + scaleIn(
                initialScale = 0.994f,
                animationSpec = tween(
                    durationMillis = 320,
                    easing = FastOutSlowInEasing,
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
                    durationMillis = 180,
                    easing = FastOutSlowInEasing,
                ),
            ) + slideOutOfContainer(
                towards = direction,
                animationSpec = tween(
                    durationMillis = SkydownMotionTokens.primaryExitDurationMillis,
                    easing = FastOutSlowInEasing,
                ),
                targetOffset = { fullSize -> (fullSize * 0.04f).toInt() },
            ) + scaleOut(
                targetScale = 0.994f,
                animationSpec = tween(
                    durationMillis = 280,
                    easing = FastOutSlowInEasing,
                ),
            )
        }

        targetRoute in skydownOverlayRoutes -> {
            fadeOut(
                animationSpec = tween(
                    durationMillis = 160,
                    easing = FastOutSlowInEasing,
                ),
            ) + scaleOut(
                targetScale = 0.994f,
                animationSpec = tween(
                    durationMillis = 220,
                    easing = FastOutSlowInEasing,
                ),
            )
        }

        initialRoute in skydownOverlayRoutes -> {
            fadeOut(
                animationSpec = tween(
                    durationMillis = 180,
                    easing = FastOutSlowInEasing,
                ),
            ) + slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Down,
                animationSpec = tween(
                    durationMillis = SkydownMotionTokens.overlayExitDurationMillis,
                    easing = FastOutSlowInEasing,
                ),
                targetOffset = { fullSize -> (fullSize * 0.08f).toInt() },
            ) + scaleOut(
                targetScale = 0.990f,
                animationSpec = tween(
                    durationMillis = SkydownMotionTokens.overlayExitDurationMillis,
                    easing = FastOutSlowInEasing,
                ),
            )
        }

        else -> {
            fadeOut(
                animationSpec = tween(
                    durationMillis = 140,
                    easing = FastOutSlowInEasing,
                ),
            ) + scaleOut(
                targetScale = 0.994f,
                animationSpec = tween(
                    durationMillis = 220,
                    easing = FastOutSlowInEasing,
                ),
            )
        }
    }
}

private enum class AuthSheet {
    Login,
    Registration,
}

@Composable
private fun LaunchLandingScreen(
    onOpenMusic: () -> Unit,
    onOpenVideography: () -> Unit,
    onOpenShop: () -> Unit,
) {
    val screenHeaderSettings by AppContainer.screenHeaderSettingsRepository.settings.collectAsStateWithLifecycle()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            androidx.compose.ui.graphics.Color.Black,
                            androidx.compose.ui.graphics.Color(0xFF030810),
                            androidx.compose.ui.graphics.Color(0xFF071222),
                            androidx.compose.ui.graphics.Color(0xFF163761).copy(alpha = 0.62f),
                            androidx.compose.ui.graphics.Color(0xFF123055).copy(alpha = 0.44f),
                            androidx.compose.ui.graphics.Color(0xFF06101D),
                            androidx.compose.ui.graphics.Color.Black,
                        ),
                    ),
                ),
    ) {
        val isWideLayout = maxWidth >= 900.dp
        val isThreeColumnLayout = maxWidth >= 1180.dp
        val contentMaxWidth = when {
            isThreeColumnLayout -> 1120.dp
            isWideLayout -> 920.dp
            else -> maxWidth
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp)
                .offset(x = 28.dp)
                .width(if (isThreeColumnLayout) 320.dp else 220.dp)
                .height(if (isThreeColumnLayout) 320.dp else 220.dp)
                .background(
                    androidx.compose.ui.graphics.Color(0xFF4B83CF).copy(alpha = 0.18f),
                    RoundedCornerShape(999.dp),
                ),
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 56.dp)
                .offset(x = (-36).dp)
                .width(if (isWideLayout) 260.dp else 200.dp)
                .height(if (isWideLayout) 260.dp else 200.dp)
                .background(
                    androidx.compose.ui.graphics.Color(0xFF9CBDE8).copy(alpha = 0.10f),
                    RoundedCornerShape(999.dp),
                ),
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = contentMaxWidth)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BrandPill(text = "Sky OS", tint = MaterialTheme.colorScheme.primary)
                            BrandPill(text = "One Flow", tint = MaterialTheme.colorScheme.tertiary)
                            BrandPill(text = "Direct", tint = SpotifyGreen)
                        }

                        BrandHeroCard(
                            eyebrow = screenHeaderSettings.homeEyebrow.ifBlank { "SkyOs Home" },
                            title = screenHeaderSettings.homeTitle.ifBlank { "SkyOs" },
                            subtitle = screenHeaderSettings.homeSubtitle.ifBlank { "Alles fuehlt sich wie eine einzige App an." },
                            detail = screenHeaderSettings.homeDetail.ifBlank { "Music, Video, Merch und Tools in einem Flow." },
                            backgroundImageUrl = screenHeaderSettings.homeImageUrl.ifBlank { null },
                            accent = MaterialTheme.colorScheme.primary,
                            secondaryAccent = MaterialTheme.colorScheme.secondary,
                            marks = listOf(BrandArtwork.Combined),
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    BrandPill(text = "Music", tint = MaterialTheme.colorScheme.primary)
                                    BrandPill(text = "Video", tint = MaterialTheme.colorScheme.tertiary)
                                    BrandPill(text = "Merch", tint = MaterialTheme.colorScheme.secondary)
                                    BrandPill(text = "AI", tint = MaterialTheme.colorScheme.primary)
                                }
                                Text(
                                    text = "Direkt rein, unten wechseln, frei zurueck.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
                                )
                            }
                        }

                        if (isThreeColumnLayout) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                HubSignalCard(
                                    title = "1 Tap Start",
                                    value = "Direkt in Music, Video oder Shop.",
                                    accentColor = SpotifyGreen,
                                    modifier = Modifier.weight(1f),
                                )
                                HubSignalCard(
                                    title = "Immer Zurueck",
                                    value = "Navigation bleibt klar und frei.",
                                    accentColor = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.weight(1f),
                                )
                                HubSignalCard(
                                    title = "Ein System",
                                    value = "Discovery, Playback und Checkout greifen zusammen.",
                                    accentColor = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                HubSignalCard(
                                    title = "1 Tap Start",
                                    value = "Direkt in Music, Video oder Shop.",
                                    accentColor = SpotifyGreen,
                                )
                                HubSignalCard(
                                    title = "Immer Zurueck",
                                    value = "Navigation bleibt klar und frei.",
                                    accentColor = MaterialTheme.colorScheme.tertiary,
                                )
                            }
                        }
                    }
                }
            }

            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = contentMaxWidth)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (isThreeColumnLayout) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                LaunchLandingChoiceCard(
                                    eyebrow = "Music",
                                    title = "Music",
                                    subtitle = "Songs, Artists und Beats.",
                                    detail = "Catalog, Beat Hub und Studio in einer direkten Lane ohne Umwege.",
                                    chips = listOf("Catalog", "Beats", "Studio"),
                                    accentColor = MaterialTheme.colorScheme.primary,
                                    icon = Icons.Default.GraphicEq,
                                    artwork = BrandArtwork.Zweizwei,
                                    onClick = onOpenMusic,
                                    modifier = Modifier.weight(1f),
                                )
                                LaunchLandingChoiceCard(
                                    eyebrow = "Video",
                                    title = "Videos",
                                    subtitle = "Reels, Clips und Collabs.",
                                    detail = "Playback, Creator-Flows und finale Clips greifen wie ein Produkt zusammen.",
                                    chips = listOf("Playback", "Reels", "Collabs"),
                                    accentColor = MaterialTheme.colorScheme.tertiary,
                                    icon = Icons.Default.PlayCircleFilled,
                                    artwork = BrandArtwork.Skydown,
                                    onClick = onOpenVideography,
                                    modifier = Modifier.weight(1f),
                                )
                                LaunchLandingChoiceCard(
                                    eyebrow = "Store",
                                    title = "Merch",
                                    subtitle = "Drops und Checkout.",
                                    detail = "Direkt zu Fits, neuen Pieces und sauber im Checkout bleiben.",
                                    chips = listOf("Drops", "Fits", "Checkout"),
                                    accentColor = MaterialTheme.colorScheme.secondary,
                                    icon = Icons.Default.ShoppingBag,
                                    artwork = BrandArtwork.Combined,
                                    onClick = onOpenShop,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        } else if (isWideLayout) {
                            LaunchLandingChoiceCard(
                                eyebrow = "Music",
                                title = "Music",
                                subtitle = "Songs, Artists und Beats.",
                                detail = "Catalog, Beat Hub und Studio in einer direkten Lane ohne Umwege.",
                                chips = listOf("Catalog", "Beats", "Studio"),
                                accentColor = MaterialTheme.colorScheme.primary,
                                icon = Icons.Default.GraphicEq,
                                artwork = BrandArtwork.Zweizwei,
                                onClick = onOpenMusic,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                LaunchLandingChoiceCard(
                                    eyebrow = "Video",
                                    title = "Videos",
                                    subtitle = "Reels, Clips und Collabs.",
                                    detail = "Playback, Creator-Flows und finale Clips greifen wie ein Produkt zusammen.",
                                    chips = listOf("Playback", "Reels", "Collabs"),
                                    accentColor = MaterialTheme.colorScheme.tertiary,
                                    icon = Icons.Default.PlayCircleFilled,
                                    artwork = BrandArtwork.Skydown,
                                    onClick = onOpenVideography,
                                    modifier = Modifier.weight(1f),
                                )
                                LaunchLandingChoiceCard(
                                    eyebrow = "Store",
                                    title = "Merch",
                                    subtitle = "Drops und Checkout.",
                                    detail = "Direkt zu Fits, neuen Pieces und sauber im Checkout bleiben.",
                                    chips = listOf("Drops", "Fits", "Checkout"),
                                    accentColor = MaterialTheme.colorScheme.secondary,
                                    icon = Icons.Default.ShoppingBag,
                                    artwork = BrandArtwork.Combined,
                                    onClick = onOpenShop,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        } else {
                            LaunchLandingChoiceCard(
                                eyebrow = "Music",
                                title = "Music",
                                subtitle = "Songs, Artists und Beats.",
                                detail = "Catalog, Beat Hub und Studio in einer direkten Lane ohne Umwege.",
                                chips = listOf("Catalog", "Beats", "Studio"),
                                accentColor = MaterialTheme.colorScheme.primary,
                                icon = Icons.Default.GraphicEq,
                                artwork = BrandArtwork.Zweizwei,
                                onClick = onOpenMusic,
                            )
                            LaunchLandingChoiceCard(
                                eyebrow = "Video",
                                title = "Videos",
                                subtitle = "Reels, Clips und Collabs.",
                                detail = "Playback, Creator-Flows und finale Clips greifen wie ein Produkt zusammen.",
                                chips = listOf("Playback", "Reels", "Collabs"),
                                accentColor = MaterialTheme.colorScheme.tertiary,
                                icon = Icons.Default.PlayCircleFilled,
                                artwork = BrandArtwork.Skydown,
                                onClick = onOpenVideography,
                            )
                            LaunchLandingChoiceCard(
                                eyebrow = "Store",
                                title = "Merch",
                                subtitle = "Drops und Checkout.",
                                detail = "Direkt zu Fits, neuen Pieces und sauber im Checkout bleiben.",
                                chips = listOf("Drops", "Fits", "Checkout"),
                                accentColor = MaterialTheme.colorScheme.secondary,
                                icon = Icons.Default.ShoppingBag,
                                artwork = BrandArtwork.Combined,
                                onClick = onOpenShop,
                            )
                        }
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
    artwork: BrandArtwork,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HubEntryCard(
        eyebrow = eyebrow,
        title = title,
        subtitle = subtitle,
        detail = detail,
        chips = chips,
        accentColor = accentColor,
        icon = icon,
        artwork = artwork,
        onClick = onClick,
        modifier = modifier,
    )
}

private enum class ZweizweiMusicDestination {
    Hub,
    Catalog,
    ArtistPage,
    BeatHub,
    NicmaProducer,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ZweizweiMusicLaneScreen(
    onOpenCart: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onBackToLanding: (() -> Unit)? = null,
) {
    val compactLayout = rememberIsCompactAppLayout()
    val compactVisualDensity = rememberUsesCompactVisualDensity()
    val useCompactHubVisuals = compactLayout || compactVisualDensity
    var destination by rememberSaveable { mutableStateOf(ZweizweiMusicDestination.Hub) }
    var catalogInitialArtist by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedArtistPage by rememberSaveable { mutableStateOf<String?>(null) }
    var artistPageReturnDestination by rememberSaveable { mutableStateOf(ZweizweiMusicDestination.Hub) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val hubHorizontalPadding = if (useCompactHubVisuals) 16.dp else 18.dp
    val hubTopPadding = if (useCompactHubVisuals) 14.dp else 22.dp
    val hubBottomPadding = if (useCompactHubVisuals) 24.dp else 30.dp
    val hubSectionSpacing = if (useCompactHubVisuals) 10.dp else 14.dp
    val useCompactHubHero = useCompactHubVisuals

    Box(
        modifier = Modifier
            .fillMaxSize()
            .skydownSelectionFeedback(trigger = destination),
    ) {
        when (destination) {
        ZweizweiMusicDestination.Hub -> Scaffold(
            modifier = Modifier,
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                TopAppBar(
                    title = {
                        SkydownTopBarTitle(
                            title = "Music",
                            subtitle = if (compactLayout) null else "Releases, Artists, Beats.",
                        )
                    },
                    navigationIcon = if (onBackToLanding != null) {
                        {
                            IconButton(onClick = onBackToLanding) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Zurueck",
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
                            dense = useCompactHubVisuals,
                        )
                    },
                    colors = skydownTopBarColors(),
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { innerPadding ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(
                        skydownScreenBrush(
                            primaryColor = SpotifyGreen,
                            secondaryColor = MaterialTheme.colorScheme.secondary,
                            primaryAlpha = 0.095f,
                            secondaryAlpha = 0.060f,
                        ),
                    ),
            ) {
                val isWideLayout = maxWidth >= 900.dp
                val contentMaxWidth = when {
                    isWideLayout -> maxWidth
                    maxWidth >= 640.dp -> 620.dp
                    else -> maxWidth
                }
                val useAnchoredHubLayout = !isWideLayout && maxHeight >= 900.dp && maxWidth < 560.dp
                val screenHeaderSettings by AppContainer.screenHeaderSettingsRepository.settings.collectAsStateWithLifecycle()

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    if (useAnchoredHubLayout) {
                        Column(
                            modifier = Modifier
                                .widthIn(max = contentMaxWidth)
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .padding(
                                    start = hubHorizontalPadding,
                                    top = hubTopPadding,
                                    end = hubHorizontalPadding,
                                    bottom = hubBottomPadding,
                                ),
                            verticalArrangement = Arrangement.spacedBy(hubSectionSpacing),
                        ) {
                            BrandHeroCard(
                                eyebrow = screenHeaderSettings.musicHubEyebrow.ifBlank { "Music" },
                                title = screenHeaderSettings.musicHubTitle.ifBlank { "Music" },
                                subtitle = screenHeaderSettings.musicHubSubtitle.ifBlank { "Releases, Artists, Beats." },
                                detail = screenHeaderSettings.musicHubDetail.ifBlank { "Direkt zu Songs, Beats und Studio." },
                                backgroundImageUrl = screenHeaderSettings.musicHubImageUrl.ifBlank { null },
                                accent = MaterialTheme.colorScheme.primary,
                                secondaryAccent = MaterialTheme.colorScheme.secondary,
                                marks = listOf(BrandArtwork.Zweizwei),
                                compactVisualDensity = useCompactHubHero,
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    BrandPill(text = "Catalog", tint = MaterialTheme.colorScheme.primary)
                                    BrandPill(text = "Beats", tint = MaterialTheme.colorScheme.secondary)
                                    BrandPill(text = "Studio", tint = MaterialTheme.colorScheme.tertiary)
                                }
                            }

                            HubSignalCard(
                                title = "Direkt rein",
                                value = "Songs, Beat Hub und Studio bleiben unten sofort griffbereit.",
                                accentColor = SpotifyGreen,
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            LaunchLandingButton(
                                title = "Songs",
                                subtitle = "Mit JANNO starten und im Katalog direkt alle Artists finden.",
                                detail = "Preview, Spotify und Artist-Pages liegen direkt auf derselben Stage.",
                                chips = listOf("Tracks", "Spotify", "Pages"),
                                accentColor = SpotifyGreen,
                                icon = Icons.Default.GraphicEq,
                                compactVisualDensity = useCompactHubHero,
                                onClick = {
                                    catalogInitialArtist = "JANNO"
                                    destination = ZweizweiMusicDestination.Catalog
                                },
                                modifier = Modifier.testTag("music.hub.songs.open"),
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                HubSignalCard(
                                    title = "Beat Hub",
                                    value = "Direkt in den Vibe.",
                                    accentColor = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.weight(1f),
                                    onClick = { destination = ZweizweiMusicDestination.BeatHub },
                                )
                                HubSignalCard(
                                    title = "Studio",
                                    value = "Record, Mix, Master.",
                                    accentColor = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.weight(1f),
                                    onClick = { destination = ZweizweiMusicDestination.NicmaProducer },
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .widthIn(max = contentMaxWidth)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(
                                    start = hubHorizontalPadding,
                                    top = hubTopPadding,
                                    end = hubHorizontalPadding,
                                    bottom = if (compactLayout) 24.dp else 30.dp,
                                ),
                            verticalArrangement = Arrangement.spacedBy(hubSectionSpacing),
                        ) {
                            BrandHeroCard(
                                eyebrow = screenHeaderSettings.musicHubEyebrow.ifBlank { "Music" },
                                title = screenHeaderSettings.musicHubTitle.ifBlank { "Music" },
                                subtitle = screenHeaderSettings.musicHubSubtitle.ifBlank { "Releases, Artists, Beats." },
                                detail = screenHeaderSettings.musicHubDetail.ifBlank { "Direkt zu Songs, Beats und Studio." },
                                backgroundImageUrl = screenHeaderSettings.musicHubImageUrl.ifBlank { null },
                                accent = MaterialTheme.colorScheme.primary,
                                secondaryAccent = MaterialTheme.colorScheme.secondary,
                                marks = listOf(BrandArtwork.Zweizwei),
                                compactVisualDensity = useCompactHubHero,
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    BrandPill(text = "Catalog", tint = MaterialTheme.colorScheme.primary)
                                    BrandPill(text = "Beats", tint = MaterialTheme.colorScheme.secondary)
                                    BrandPill(text = "Studio", tint = MaterialTheme.colorScheme.tertiary)
                                }
                            }

                            if (isWideLayout) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    HubSignalCard(
                                        title = "Catalog",
                                        value = "Artists, Tracks, Pages",
                                        accentColor = SpotifyGreen,
                                        modifier = Modifier.weight(1f),
                                    )
                                    HubSignalCard(
                                        title = "Beat Hub",
                                        value = "Direkt in den Vibe",
                                        accentColor = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.weight(1f),
                                    )
                                    HubSignalCard(
                                        title = "Studio",
                                        value = "Record, Mix, Master",
                                        accentColor = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }

                            if (isWideLayout) {
                                LaunchLandingButton(
                                    title = "Songs",
                                    subtitle = "Mit JANNO starten und im Katalog direkt alle Artists finden.",
                                    detail = "Preview, Spotify und Artist-Pages liegen direkt auf derselben Stage.",
                                    chips = listOf("Tracks", "Spotify", "Pages"),
                                    accentColor = MaterialTheme.colorScheme.primary,
                                    icon = Icons.Default.GraphicEq,
                                    compactVisualDensity = useCompactHubHero,
                                    onClick = {
                                        catalogInitialArtist = "JANNO"
                                        destination = ZweizweiMusicDestination.Catalog
                                    },
                                    modifier = Modifier.testTag("music.hub.songs.open"),
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    LaunchLandingButton(
                                        title = "Beat Hub",
                                        subtitle = "Beats direkt.",
                                        detail = "Schnell in neue Sounds springen und den richtigen Vibe greifen.",
                                        chips = listOf("Playback", "Selection", "Flow"),
                                        accentColor = MaterialTheme.colorScheme.secondary,
                                        icon = Icons.Default.GraphicEq,
                                        compactVisualDensity = useCompactHubHero,
                                        onClick = { destination = ZweizweiMusicDestination.BeatHub },
                                        modifier = Modifier.weight(1f),
                                    )
                                    LaunchLandingButton(
                                        title = "Studio",
                                        subtitle = "Recording, Mix, Master.",
                                        detail = "Die Services bleiben ohne Umweg direkt aus dem Music-Hub erreichbar.",
                                        chips = listOf("Record", "Mix", "Master"),
                                        accentColor = MaterialTheme.colorScheme.tertiary,
                                        icon = Icons.Default.AutoAwesome,
                                        compactVisualDensity = useCompactHubHero,
                                        onClick = { destination = ZweizweiMusicDestination.NicmaProducer },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            } else {
                                LaunchLandingButton(
                                    title = "Songs",
                                    subtitle = "Mit JANNO starten und im Katalog direkt alle Artists finden.",
                                    detail = "Preview, Spotify und Artist-Pages liegen direkt auf derselben Stage.",
                                    chips = listOf("Tracks", "Spotify", "Pages"),
                                    accentColor = SpotifyGreen,
                                    icon = Icons.Default.GraphicEq,
                                    compactVisualDensity = useCompactHubHero,
                                    modifier = Modifier.testTag("music.hub.songs.open"),
                                    onClick = {
                                        catalogInitialArtist = "JANNO"
                                        destination = ZweizweiMusicDestination.Catalog
                                    },
                                )
                                LaunchLandingButton(
                                    title = "Beat Hub",
                                    subtitle = "Beats direkt.",
                                    detail = "Schnell in neue Sounds springen und den richtigen Vibe greifen.",
                                    chips = listOf("Playback", "Selection", "Flow"),
                                    accentColor = MaterialTheme.colorScheme.secondary,
                                    icon = Icons.Default.PlayCircleFilled,
                                    compactVisualDensity = useCompactHubHero,
                                    onClick = { destination = ZweizweiMusicDestination.BeatHub },
                                )
                                LaunchLandingButton(
                                    title = "Studio",
                                    subtitle = "Recording, Mix, Master.",
                                    detail = "Die Services bleiben ohne Umweg direkt aus dem Music-Hub erreichbar.",
                                    chips = listOf("Record", "Mix", "Master"),
                                    accentColor = MaterialTheme.colorScheme.tertiary,
                                    icon = Icons.Default.AutoAwesome,
                                    compactVisualDensity = useCompactHubHero,
                                    onClick = { destination = ZweizweiMusicDestination.NicmaProducer },
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }
        }

        ZweizweiMusicDestination.Catalog -> MusicScreen(
            initialArtist = catalogInitialArtist,
            onBack = {
                catalogInitialArtist = null
                destination = ZweizweiMusicDestination.Hub
            },
            onOpenBeatHub = { destination = ZweizweiMusicDestination.BeatHub },
            onOpenStudio = { destination = ZweizweiMusicDestination.NicmaProducer },
            onOpenCart = onOpenCart,
            onOpenProfile = onOpenProfile,
            onOpenSettings = onOpenSettings,
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

        ZweizweiMusicDestination.BeatHub -> BeatHubScreen(
            onBack = { destination = ZweizweiMusicDestination.Hub },
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
    compactVisualDensity: Boolean = false,
    modifier: Modifier = Modifier,
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
    compactVisualDensity: Boolean = false,
    modifier: Modifier = Modifier,
    detail: String? = null,
    chips: List<String> = emptyList(),
    eyebrow: String? = null,
    artwork: BrandArtwork? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val cardShape = RoundedCornerShape(if (compactVisualDensity) 22.dp else 28.dp)
    val haloSize = if (compactVisualDensity) 82.dp else 120.dp
    val accentBarHeight = if (compactVisualDensity) 54.dp else 72.dp
    val contentHorizontalPadding = if (compactVisualDensity) 14.dp else 18.dp
    val contentVerticalPadding = if (compactVisualDensity) 14.dp else 18.dp
    val contentSpacing = if (compactVisualDensity) 10.dp else 16.dp
    val headerSpacing = if (compactVisualDensity) 8.dp else 12.dp
    val titleStyle = if (compactVisualDensity) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall
    val subtitleStyle = if (compactVisualDensity) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium
    val arrowContainerSize = if (compactVisualDensity) 28.dp else 36.dp

    Surface(
        shape = cardShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = 10.dp,
        shadowElevation = 14.dp,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.24f)),
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier.skydownPressable(interactionSource),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(haloSize)
                    .offset(
                        x = if (compactVisualDensity) 12.dp else 18.dp,
                        y = if (compactVisualDensity) (-12).dp else (-18).dp,
                    )
                    .background(
                        accentColor.copy(alpha = 0.10f),
                        RoundedCornerShape(999.dp),
                    ),
            )

            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 10.dp)
                    .width(4.dp)
                    .height(accentBarHeight)
                    .background(accentColor, RoundedCornerShape(999.dp)),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = contentHorizontalPadding, vertical = contentVerticalPadding),
                verticalArrangement = Arrangement.spacedBy(contentSpacing),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(headerSpacing),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (compactVisualDensity) 44.dp else 52.dp)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        accentColor.copy(alpha = 0.22f),
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                                    ),
                                ),
                                RoundedCornerShape(if (compactVisualDensity) 14.dp else 18.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (artwork != null) {
                            Image(
                                painter = painterResource(id = artwork.drawableRes),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(if (compactVisualDensity) 26.dp else 34.dp)
                                    .padding(4.dp),
                                contentScale = ContentScale.Fit,
                            )
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(if (compactVisualDensity) 12.dp else 15.dp),
                            )
                        } else {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = accentColor,
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(if (compactVisualDensity) 4.dp else 6.dp),
                    ) {
                        if (!eyebrow.isNullOrBlank()) {
                            Text(
                                text = eyebrow.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Text(
                            text = title,
                            style = titleStyle,
                            fontWeight = FontWeight.Black,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = subtitle,
                            style = subtitleStyle,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(arrowContainerSize)
                            .background(
                                accentColor.copy(alpha = 0.12f),
                                RoundedCornerShape(999.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier
                                .alpha(0.92f)
                                .size(if (compactVisualDensity) 14.dp else 18.dp),
                        )
                    }
                }

                if (!detail.isNullOrBlank()) {
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                        maxLines = if (compactVisualDensity) 2 else 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (chips.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(if (compactVisualDensity) 6.dp else 8.dp)) {
                        chips.take(if (compactVisualDensity) 2 else 3).forEach { chip ->
                            BrandPill(text = chip, tint = accentColor)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HubSignalCard(
    title: String,
    value: String,
    accentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    if (onClick != null) {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.18f)),
            tonalElevation = 6.dp,
            onClick = onClick,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.84f),
                )
            }
        }
    } else {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.18f)),
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.84f),
                )
            }
        }
    }
}
