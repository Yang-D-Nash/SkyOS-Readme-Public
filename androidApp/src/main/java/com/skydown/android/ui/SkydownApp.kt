package com.skydown.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Widgets
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.skydown.android.R
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skydown.android.data.AppContainer
import com.skydown.android.data.AppFeatureFlagsStore
import com.skydown.android.ui.component.BrandArtwork
import com.skydown.android.ui.component.BrandHeroCard
import com.skydown.android.ui.component.BrandPill
import com.skydown.android.ui.component.rememberIsCompactAppLayout
import com.skydown.android.ui.component.skydownPressable
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
        add(BottomDestination("shop", "Shop", { Icon(Icons.Default.ShoppingBag, contentDescription = null) }))
        add(BottomDestination("music", "Music", { Icon(Icons.Default.GraphicEq, contentDescription = null) }))
        add(BottomDestination("home", "Home", { Icon(Icons.Default.Widgets, contentDescription = null) }))
        add(BottomDestination("video", "Video", { Icon(Icons.Default.Slideshow, contentDescription = null) }))
        add(BottomDestination("ai", "Tools", { Icon(Icons.Default.AutoAwesome, contentDescription = null) }))
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
            if (!hasAiAccess) {
                showsWorkflowWorkspace = false
            }
            if (currentDestination?.route != "ai") {
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
                        onClose = { navController.popBackStack() },
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
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp)
                .offset(x = 28.dp)
                .width(220.dp)
                .height(220.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                    RoundedCornerShape(999.dp),
                ),
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 56.dp)
                .offset(x = (-36).dp)
                .width(200.dp)
                .height(200.dp)
                .background(
                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                    RoundedCornerShape(999.dp),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterVertically),
        ) {
            Spacer(modifier = Modifier.height(1.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Sky²²",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f),
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Sky²²",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Waehle den Bereich, mit dem du gerade starten willst. Unten kannst du spaeter jederzeit wechseln.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.74f),
                )
            }

            Surface(
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                tonalElevation = 10.dp,
                shadowElevation = 12.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.20f)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(86.dp)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.94f),
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                                    ),
                                ),
                                RoundedCornerShape(24.dp),
                            ),
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.skydown_x22_brand_logo),
                            contentDescription = "Sky²² Logo",
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(68.dp),
                            contentScale = ContentScale.Fit,
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "Ein Ort fuer Musik, Videos und Merchandise.",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Starte dort, wo du gerade weitermachen willst. Alles andere bleibt unten fuer dich direkt erreichbar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LaunchLandingChoiceCard(
                    step = "01",
                    eyebrow = "Listen & Artists",
                    title = "Music",
                    subtitle = "Wenn du hoeren, Artists entdecken oder direkt zu Beats willst.",
                    accentColor = MaterialTheme.colorScheme.primary,
                    icon = Icons.Default.GraphicEq,
                    onClick = onOpenMusic,
                )
                LaunchLandingChoiceCard(
                    step = "02",
                    eyebrow = "Clips & Reels",
                    title = "Video",
                    subtitle = "Wenn du Reels schauen, Produktionen sehen oder Kontakt aufnehmen willst.",
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    icon = Icons.Default.Slideshow,
                    onClick = onOpenVideography,
                )
                LaunchLandingChoiceCard(
                    step = "03",
                    eyebrow = "Store",
                    title = "Shop",
                    subtitle = "Wenn du Produkte entdecken, in Ruhe ansehen oder direkt bestellen willst.",
                    accentColor = MaterialTheme.colorScheme.secondary,
                    icon = Icons.Default.Inventory2,
                    onClick = onOpenShop,
                )
            }

            Text(
                text = "Danach wechselst du unten jederzeit dorthin, wo du weitermachen willst.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f),
            )

            Spacer(modifier = Modifier.height(1.dp))
        }
    }
}

@Composable
private fun LaunchLandingChoiceCard(
    step: String,
    eyebrow: String,
    title: String,
    subtitle: String,
    accentColor: androidx.compose.ui.graphics.Color,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = 8.dp,
        shadowElevation = 10.dp,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.18f)),
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier.skydownPressable(interactionSource),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = step,
                    style = MaterialTheme.typography.labelMedium,
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                )
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.94f),
                                    accentColor.copy(alpha = 0.56f),
                                ),
                            ),
                            RoundedCornerShape(14.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        text = eyebrow.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f),
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Black,
                    )
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
            }
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
                    .padding(horizontal = 18.dp, vertical = 22.dp),
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
                    BrandHeroCard(
                        eyebrow = "Music",
                        title = "Music",
                        subtitle = "Hier findest du Releases, Artists und alles rund ums Produzieren.",
                        detail = "Hoer rein, entdecke Artists und geh von hier direkt weiter zu Beats oder Studio.",
                        accent = MaterialTheme.colorScheme.primary,
                        secondaryAccent = MaterialTheme.colorScheme.secondary,
                        marks = listOf(BrandArtwork.Zweizwei),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BrandPill(text = "Catalog", tint = MaterialTheme.colorScheme.primary)
                            BrandPill(text = "Beats", tint = MaterialTheme.colorScheme.secondary)
                            BrandPill(text = "Studio", tint = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                    LaunchLandingButton(
                        title = "Songs & Artists",
                        subtitle = "Zum Entdecken, Hoeren und direkten Weitergehen.",
                        accentColor = MaterialTheme.colorScheme.primary,
                        onClick = { destination = ZweizweiMusicDestination.Catalog },
                    )
                    LaunchLandingButton(
                        title = "Beat Library",
                        subtitle = "Wenn du schnell ein Gefuehl fuer Beats bekommen willst.",
                        accentColor = MaterialTheme.colorScheme.secondary,
                        onClick = { destination = ZweizweiMusicDestination.BeatHub },
                    )
                    LaunchLandingButton(
                        title = "Studio Services",
                        subtitle = "Wenn du Recording, Mixing oder Mastering anfragen willst.",
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        onClick = { destination = ZweizweiMusicDestination.NicmaProducer },
                    )
                }

                Spacer(modifier = Modifier.height(1.dp))
            }
        }

        ZweizweiMusicDestination.Catalog -> MusicScreen(
            onBack = { destination = ZweizweiMusicDestination.Hub },
            onOpenBeatHub = { destination = ZweizweiMusicDestination.BeatHub },
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
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = 8.dp,
        shadowElevation = 10.dp,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.24f)),
    ) {
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
            elevation = null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
            }
        }
    }
}
