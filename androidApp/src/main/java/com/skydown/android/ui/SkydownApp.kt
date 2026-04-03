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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.media3.common.util.UnstableApi
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.draw.alpha
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
import com.skydown.android.data.ArtistPageBrand
import com.skydown.android.ui.component.BrandArtwork
import com.skydown.android.ui.component.BrandHeroCard
import com.skydown.android.ui.component.BrandPill
import com.skydown.android.ui.component.AppTopBarSessionActions
import com.skydown.android.ui.component.SkydownTopBarTitle
import com.skydown.android.ui.component.rememberIsCompactAppLayout
import com.skydown.android.ui.component.skydownPressable
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
import com.skydown.android.ui.theme.SurfaceDark
import com.skydown.android.ui.theme.TextDark
import com.skydown.android.ui.theme.TextMutedDark

@OptIn(ExperimentalMaterial3Api::class)
@UnstableApi
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
    val destinations = buildList {
        add(BottomDestination("shop", "Merch") { _ ->
            Icon(Icons.Default.ShoppingBag, contentDescription = null)
        })
        add(BottomDestination("music", "Music") { _ ->
            Icon(Icons.Default.GraphicEq, contentDescription = null)
        })
        add(BottomDestination("home", "Home") { _ ->
            Icon(Icons.Default.Home, contentDescription = null)
        })
        add(BottomDestination("video", "Videos") { _ ->
            Icon(Icons.Default.PlayCircleFilled, contentDescription = null)
        })
        add(BottomDestination("ai", "Tools") { _ ->
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
                                    icon = { destination.icon(isSelected) },
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
                    )
                }
                composable("settings") {
                    SettingsScreen(
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
    val icon: @Composable (Boolean) -> Unit,
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
                        androidx.compose.ui.graphics.Color.Black,
                        androidx.compose.ui.graphics.Color(0xFF030810),
                        androidx.compose.ui.graphics.Color(0xFF071222),
                        androidx.compose.ui.graphics.Color(0xFF123055).copy(alpha = 0.56f),
                        androidx.compose.ui.graphics.Color(0xFF06101D),
                        androidx.compose.ui.graphics.Color.Black,
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
                    androidx.compose.ui.graphics.Color(0xFF4B83CF).copy(alpha = 0.18f),
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
                    androidx.compose.ui.graphics.Color(0xFF9CBDE8).copy(alpha = 0.10f),
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
                modifier = Modifier
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                SurfaceDark.copy(alpha = 0.96f),
                                androidx.compose.ui.graphics.Color(0xFF18344F).copy(alpha = 0.24f),
                            ),
                        ),
                        RoundedCornerShape(24.dp),
                    )
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Sky²²",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextMutedDark,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Sky²²",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = TextDark,
                )
                Text(
                    text = "Waehle deinen Start.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextDark.copy(alpha = 0.72f),
                )
            }

            Surface(
                shape = RoundedCornerShape(26.dp),
                color = SurfaceDark.copy(alpha = 0.94f),
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
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
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
                            text = "Musik, Video, Merch.",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextDark,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            text = "Alles unten direkt griffbereit.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextDark.copy(alpha = 0.70f),
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LaunchLandingChoiceCard(
                    eyebrow = "Music",
                    title = "Music",
                    subtitle = "Releases, Artists, Beats.",
                    accentColor = MaterialTheme.colorScheme.primary,
                    icon = Icons.Default.GraphicEq,
                    artwork = BrandArtwork.Zweizwei,
                    onClick = onOpenMusic,
                )
                LaunchLandingChoiceCard(
                    eyebrow = "Video",
                    title = "Videos",
                    subtitle = "Reels, Clips, Collabs.",
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    icon = Icons.Default.PlayCircleFilled,
                    artwork = BrandArtwork.Skydown,
                    onClick = onOpenVideography,
                )
                LaunchLandingChoiceCard(
                    eyebrow = "Store",
                    title = "Merch",
                    subtitle = "Drops, Styles, Checkout.",
                    accentColor = MaterialTheme.colorScheme.secondary,
                    icon = Icons.Default.ShoppingBag,
                    artwork = BrandArtwork.Combined,
                    onClick = onOpenShop,
                )
            }

            Text(
                text = "Unten wechselst du jederzeit.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMutedDark,
            )

            Spacer(modifier = Modifier.height(1.dp))
        }
    }
}

@Composable
private fun LaunchLandingChoiceCard(
    eyebrow: String,
    title: String,
    subtitle: String,
    accentColor: androidx.compose.ui.graphics.Color,
    icon: ImageVector,
    artwork: BrandArtwork,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = SurfaceDark.copy(alpha = 0.95f),
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
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                SurfaceDark.copy(alpha = 0.98f),
                                accentColor.copy(alpha = 0.18f),
                            ),
                        ),
                        RoundedCornerShape(18.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = artwork.drawableRes),
                        contentDescription = null,
                        modifier = Modifier
                            .size(38.dp)
                            .padding(4.dp),
                        contentScale = ContentScale.Fit,
                    )
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = TextDark.copy(alpha = 0.88f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(16.dp),
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
                        color = TextDark.copy(alpha = 0.68f),
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextDark,
                        fontWeight = FontWeight.Black,
                    )
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDark.copy(alpha = 0.70f),
                )
            }
        }
    }
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
    var destination by rememberSaveable { mutableStateOf(ZweizweiMusicDestination.Hub) }
    var catalogInitialArtist by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedArtistPage by rememberSaveable { mutableStateOf<String?>(null) }
    var artistPageReturnDestination by rememberSaveable { mutableStateOf(ZweizweiMusicDestination.Hub) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    when (destination) {
        ZweizweiMusicDestination.Hub -> Scaffold(
            modifier = Modifier,
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        SkydownTopBarTitle(
                            title = "Music",
                            subtitle = "Releases, Artists, Beats.",
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
                        )
                    },
                    colors = skydownTopBarColors(),
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .navigationBarsPadding()
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
                        .padding(horizontal = 18.dp, vertical = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    val screenHeaderSettings by AppContainer.screenHeaderSettingsRepository.settings.collectAsStateWithLifecycle()
                    BrandHeroCard(
                        eyebrow = screenHeaderSettings.musicHubEyebrow.ifBlank { "Music" },
                        title = screenHeaderSettings.musicHubTitle.ifBlank { "Music" },
                        subtitle = screenHeaderSettings.musicHubSubtitle.ifBlank { "Releases, Artists, Beats." },
                        detail = screenHeaderSettings.musicHubDetail.ifBlank { "Direkt zu Songs, Beats und Studio." },
                        backgroundImageUrl = screenHeaderSettings.musicHubImageUrl.ifBlank { null },
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
                        title = "Songs",
                        subtitle = "Mit JANNO starten und im Katalog direkt alle Artists finden.",
                        accentColor = MaterialTheme.colorScheme.primary,
                        icon = Icons.Default.GraphicEq,
                        onClick = {
                            catalogInitialArtist = "JANNO"
                            destination = ZweizweiMusicDestination.Catalog
                        },
                    )
                    LaunchLandingButton(
                        title = "Beat Hub",
                        subtitle = "Beats direkt.",
                        accentColor = MaterialTheme.colorScheme.secondary,
                        icon = Icons.Default.GraphicEq,
                        onClick = { destination = ZweizweiMusicDestination.BeatHub },
                    )
                    LaunchLandingButton(
                        title = "Studio",
                        subtitle = "Recording, Mix, Master.",
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        icon = Icons.Default.AutoAwesome,
                        onClick = { destination = ZweizweiMusicDestination.NicmaProducer },
                    )
                    Spacer(modifier = Modifier.height(1.dp))
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

@Composable
private fun LaunchLandingButton(
    title: String,
    subtitle: String,
    accentColor: androidx.compose.ui.graphics.Color,
    icon: ImageVector,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.22f),
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                                ),
                            ),
                            RoundedCornerShape(14.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            accentColor.copy(alpha = 0.92f),
                            RoundedCornerShape(999.dp),
                        ),
                )
            }
        }
    }
}
