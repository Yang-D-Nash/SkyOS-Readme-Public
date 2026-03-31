package com.skydown.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skydown.android.data.AppContainer
import com.skydown.android.data.AppFeatureFlagsStore
import com.skydown.android.ui.component.rememberIsCompactAppLayout
import com.skydown.android.ui.screen.AiHubScreen
import com.skydown.android.ui.screen.BeatHubScreen
import com.skydown.android.ui.screen.CartScreen
import com.skydown.android.ui.screen.HomeScreen
import com.skydown.android.ui.screen.IntroScreen
import com.skydown.android.ui.screen.LoginScreen
import com.skydown.android.ui.screen.MusicScreen
import com.skydown.android.ui.screen.NicmaProducerScreen
import com.skydown.android.ui.screen.OrderScreen
import com.skydown.android.ui.screen.RegistrationScreen
import com.skydown.android.ui.screen.SettingsScreen
import com.skydown.android.ui.screen.ShopScreen
import com.skydown.android.ui.screen.VideoHubScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkydownApp() {
    val navController = rememberNavController()
    val isCompactLayout = rememberIsCompactAppLayout()
    val currentUser by AppContainer.currentUser.collectAsStateWithLifecycle()
    val aiAccessMode by AppFeatureFlagsStore.aiAccessMode.collectAsStateWithLifecycle()
    var showIntro by remember { mutableStateOf(true) }
    var selectedEntryRoute by rememberSaveable { mutableStateOf<String?>(null) }
    var showsWorkflowWorkspace by rememberSaveable { mutableStateOf(false) }
    var authSheet by remember { mutableStateOf<AuthSheet?>(null) }
    var showOrders by remember { mutableStateOf(false) }
    val authSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val ordersSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val openSettings = remember(navController) {
        {
            navController.navigate("settings") {
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
    val destinations = buildList {
        add(BottomDestination("shop", "Merchandise", { Icon(Icons.Default.ShoppingBag, contentDescription = null) }))
        add(BottomDestination("music", "Zweizwei", { Icon(Icons.Default.MusicNote, contentDescription = null) }))
        add(BottomDestination("home", "Home", { Icon(Icons.Default.Home, contentDescription = null) }))
        add(BottomDestination("video", "Skydown", { Icon(Icons.Default.Movie, contentDescription = null) }))
        if (hasAiAccess) {
            add(BottomDestination("ai", "Tools", { Icon(Icons.Default.AutoAwesome, contentDescription = null) }))
        }
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

        LaunchedEffect(hasAiAccess, currentDestination?.route) {
            if (!hasAiAccess && currentDestination?.route == "ai") {
                showsWorkflowWorkspace = false
                navController.navigate("home") {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            } else if (currentDestination?.route != "ai") {
                showsWorkflowWorkspace = false
            }
        }

        Scaffold(
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(
                            horizontal = if (isCompactLayout) 10.dp else 16.dp,
                            vertical = if (isCompactLayout) 6.dp else 10.dp,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        tonalElevation = if (isCompactLayout) 8.dp else 10.dp,
                        shadowElevation = if (isCompactLayout) 10.dp else 14.dp,
                        shape = RoundedCornerShape(if (isCompactLayout) 24.dp else 30.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.10f),
                        ),
                    ) {
                        NavigationBar(
                            modifier = Modifier.padding(
                                horizontal = if (isCompactLayout) 4.dp else 6.dp,
                                vertical = if (isCompactLayout) 2.dp else 4.dp,
                            ),
                            containerColor = androidx.compose.ui.graphics.Color.Transparent,
                            tonalElevation = 0.dp,
                        ) {
                            destinations.forEach { destination ->
                                val isSelected = currentDestination?.hierarchy?.any { it.route == destination.route } == true

                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = {
                                        navController.navigate(destination.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = destination.icon,
                                    label = {
                                        Text(
                                            text = destination.label,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    },
                                    alwaysShowLabel = !isCompactLayout,
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                                    ),
                                )
                            }
                        }
                    }
                }
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = startRoute,
                modifier = Modifier.padding(innerPadding),
            ) {
                composable("home") {
                    HomeScreen(
                        onOpenCart = openCart,
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
                        onOpenSettings = openSettings,
                    )
                }
                composable("music") {
                    ZweizweiMusicLaneScreen(
                        onOpenCart = openCart,
                        onOpenSettings = openSettings,
                    )
                }
                composable("video") {
                    VideoHubScreen(
                        onOpenCart = openCart,
                        onOpenSettings = openSettings,
                    )
                }
                composable("cart") {
                    CartScreen(
                        onOpenLogin = { authSheet = AuthSheet.Login },
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
                        onOpenSettings = openSettings,
                    )
                }
                composable("settings") {
                    SettingsScreen(
                        onOpenLogin = { authSheet = AuthSheet.Login },
                        onOpenRegistration = { authSheet = AuthSheet.Registration },
                        onOpenOrders = { showOrders = true },
                    )
                }
            }
        }
    }

    authSheet?.let { sheet ->
        ModalBottomSheet(
            onDismissRequest = { authSheet = null },
            sheetState = authSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            when (sheet) {
                AuthSheet.Login -> LoginScreen(
                    onClose = { authSheet = null },
                    onOpenRegistration = { authSheet = AuthSheet.Registration },
                )
                AuthSheet.Registration -> RegistrationScreen(
                    onClose = { authSheet = null },
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

private data class BottomDestination(
    val route: String,
    val label: String,
    val icon: @Composable () -> Unit,
)

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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Skydown x 22",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Wohin soll's zuerst gehen?",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Danach landest du immer in der App und startest direkt im passenden Bereich.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                LaunchLandingButton(
                    title = "MUSIK",
                    subtitle = "Du startest direkt im Zweizwei-Musikbereich.",
                    accentColor = MaterialTheme.colorScheme.primary,
                    onClick = onOpenMusic,
                )
                LaunchLandingButton(
                    title = "VIDEOGRAPHY",
                    subtitle = "Du startest direkt bei Skydown Videography.",
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    onClick = onOpenVideography,
                )
                LaunchLandingButton(
                    title = "SHOP",
                    subtitle = "Du landest direkt im Merchandise-Bereich.",
                    accentColor = MaterialTheme.colorScheme.secondary,
                    onClick = onOpenShop,
                )
            }

            Spacer(modifier = Modifier.height(1.dp))
        }
    }
}

private enum class ZweizweiMusicDestination {
    Hub,
    Catalog,
    BeatHub,
    NicmaProducer,
}

@Composable
private fun ZweizweiMusicLaneScreen(
    onOpenCart: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onBackToLanding: (() -> Unit)? = null,
) {
    var destination by rememberSaveable { mutableStateOf(ZweizweiMusicDestination.Hub) }

    when (destination) {
        ZweizweiMusicDestination.Hub -> Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f),
                            MaterialTheme.colorScheme.background,
                        ),
                    ),
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                if (onBackToLanding != null) {
                    Button(
                        onClick = onBackToLanding,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        shape = RoundedCornerShape(999.dp),
                        elevation = null,
                    ) {
                        Text("Zurueck")
                    }
                } else {
                    Spacer(modifier = Modifier.height(1.dp))
                }

                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Zweizwei Music",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "Hier hat Zweizwei seinen eigenen Musikbereich. Catalog, Beat Hub und NICMA Producer bleiben klar getrennt von Skydown Videography.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
                    )
                    LaunchLandingButton(
                        title = "MUSIC CATALOG",
                        subtitle = "Artists, Releases, Preview-Playback und Spotify-Fokus unter Zweizwei.",
                        accentColor = MaterialTheme.colorScheme.primary,
                        onClick = { destination = ZweizweiMusicDestination.Catalog },
                    )
                    LaunchLandingButton(
                        title = "BEAT HUB",
                        subtitle = "Eigene Beat-Logik, Preview-Library und Upload-/Listener-Flow.",
                        accentColor = MaterialTheme.colorScheme.secondary,
                        onClick = { destination = ZweizweiMusicDestination.BeatHub },
                    )
                    LaunchLandingButton(
                        title = "NICMA PRODUCER",
                        subtitle = "Mixing, Mastering und Recording als eigener Music-Service.",
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        onClick = { destination = ZweizweiMusicDestination.NicmaProducer },
                    )
                }

                Spacer(modifier = Modifier.height(1.dp))
            }
        }

        ZweizweiMusicDestination.Catalog -> MusicScreen(
            onBack = { destination = ZweizweiMusicDestination.Hub },
            onOpenCart = onOpenCart,
            onOpenSettings = onOpenSettings,
        )

        ZweizweiMusicDestination.BeatHub -> BeatHubScreen(
            onBack = { destination = ZweizweiMusicDestination.Hub },
        )

        ZweizweiMusicDestination.NicmaProducer -> NicmaProducerScreen(
            onBack = { destination = ZweizweiMusicDestination.Hub },
        )
    }
}

@Composable
private fun LaunchLandingButton(
    title: String,
    subtitle: String,
    accentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = 10.dp,
        shadowElevation = 14.dp,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.24f)),
    ) {
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(22.dp),
            elevation = null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
            }
        }
    }
}
