package com.skydown.android.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.skydown.android.data.AppContainer
import com.skydown.android.ui.component.AppTopBarSessionActions
import com.skydown.android.ui.component.BrandActionButton
import com.skydown.android.ui.component.BrandArtwork
import com.skydown.android.ui.component.BrandHeroCard
import com.skydown.android.ui.component.BrandHeroMetricCard
import com.skydown.android.ui.component.BrandSectionBanner
import com.skydown.android.ui.component.BrandStatusChip
import com.skydown.android.ui.component.BrandPill
import com.skydown.android.ui.component.MerchandiseCard
import com.skydown.android.ui.component.SkydownCard
import com.skydown.android.ui.component.SkydownTopBarTitle
import com.skydown.android.ui.component.ToastHost
import com.skydown.android.ui.component.ToastType
import com.skydown.android.ui.component.skydownContentPadding
import com.skydown.android.ui.component.skydownScreenBrush
import com.skydown.android.ui.component.skydownTopBarColors
import com.skydown.android.ui.model.ShopUiState
import com.skydown.android.ui.theme.SpotifyGreen
import com.skydown.android.ui.theme.YouTubeDeepRed
import com.skydown.android.ui.viewmodel.ShopViewModel
import com.skydown.shared.model.MerchandiseItem
import com.skydown.shared.model.hasCuratedMerchCategory
import com.skydown.shared.model.merchCategoryKey
import com.skydown.shared.model.merchCategorySubtitle
import com.skydown.shared.model.merchCategoryTitle
import com.skydown.shared.usecase.MerchandiseVariantResolver
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(
    onOpenLogin: () -> Unit = {},
    onOpenCart: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    viewModel: ShopViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val collabLanes = remember(uiState.items) { buildShopCollabLanes(uiState.items) }
    var selectedCollabLaneId by rememberSaveable { mutableStateOf(ShopCollabLane.ALL_ID) }
    val filteredItems = remember(uiState.items, selectedCollabLaneId) {
        if (selectedCollabLaneId == ShopCollabLane.ALL_ID) {
            uiState.items
        } else {
            uiState.items.filter { item -> item.belongsToLane(selectedCollabLaneId) }
        }
    }
    val selectedCollabLane = remember(collabLanes, selectedCollabLaneId, uiState.items) {
        collabLanes.firstOrNull { lane -> lane.id == selectedCollabLaneId }
            ?: collabLanes.firstOrNull()
            ?: ShopCollabLane(
                id = ShopCollabLane.ALL_ID,
                title = "All Drops",
                subtitle = "Alle Collabs und Core Pieces.",
                itemCount = uiState.items.size,
                isCoreLane = false,
            )
    }
    val laneCount = remember(collabLanes, uiState.items) {
        val resolvedCount = collabLanes.count { it.id != ShopCollabLane.ALL_ID }
        if (resolvedCount == 0) {
            if (uiState.items.isEmpty()) 0 else 1
        } else {
            resolvedCount
        }
    }

    LaunchedEffect(uiState.toastMessage) {
        if (!uiState.toastMessage.isNullOrBlank()) {
            delay(3000)
            viewModel.clearToast()
        }
    }

    LaunchedEffect(collabLanes.joinToString(separator = "|") { it.id }) {
        if (collabLanes.none { it.id == selectedCollabLaneId }) {
            selectedCollabLaneId = ShopCollabLane.ALL_ID
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { SkydownTopBarTitle("Shop", "Produkte direkt in der App.") },
                actions = {
                    AppTopBarSessionActions(
                        onOpenCart = onOpenCart,
                        onOpenProfile = onOpenProfile,
                        onOpenSettings = onOpenSettings,
                    ) {
                        IconButton(
                            onClick = viewModel::refresh,
                            enabled = !uiState.isCatalogLoading && !uiState.isSyncingCatalog,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Shop aktualisieren",
                            )
                        }
                    }
                },
                colors = skydownTopBarColors(),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    skydownScreenBrush(),
                ),
        ) {
            val isWideLayout = maxWidth >= 920.dp
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = skydownContentPadding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    ShopOverviewCard(
                        uiState = uiState,
                        laneCount = laneCount,
                    )
                }

                if (uiState.isAdmin) {
                    item {
                        ShopAdminControlsCard(
                            uiState = uiState,
                            onToggleStore = viewModel::toggleStoreOpen,
                            onSyncShopify = viewModel::syncShopifyCatalog,
                        )
                    }
                }

                val errorMessage = uiState.errorMessage
                if (errorMessage != null && !uiState.isLoggedIn && uiState.items.isEmpty()) {
                    item {
                        LoginSection(
                            errorMessage = errorMessage,
                            onOpenLogin = onOpenLogin,
                        )
                    }
                }

                if (!uiState.isStoreOpen && !uiState.isAdmin) {
                    item {
                        ShopMessageCard(
                            title = "Merch Store pausiert",
                            body = "Produkte bleiben sichtbar, aber neue Kaeufe sind gerade geschlossen. Sobald du den Store wieder oeffnest, kann direkt wieder bestellt werden.",
                            icon = Icons.Default.Close,
                            accent = MaterialTheme.colorScheme.secondary,
                            tag = "PAUSE",
                        )
                    }
                }

                if (uiState.items.isEmpty()) {
                    item {
                        val isSyncing = uiState.isCatalogLoading || uiState.isSyncingCatalog
                        ShopMessageCard(
                            title = when {
                                isSyncing -> "Shop wird geladen"
                                else -> "Noch keine Shopify-Produkte"
                            },
                            body = if (uiState.isAdmin) {
                                if (isSyncing) {
                                    "Der Katalog wird gerade direkt aus Shopify neu aufgebaut."
                                } else {
                                    "Wenn Firestore leer ist, versucht die App den Shopify-Katalog jetzt automatisch neu zu laden."
                                }
                            } else {
                                if (isSyncing) {
                                    "Produkte, Verfuegbarkeit und Bilder werden gerade synchronisiert."
                                } else {
                                    "Sobald neuer Merch live ist, taucht er hier direkt als Card auf."
                                }
                            },
                            icon = if (isSyncing) Icons.Default.Sync else Icons.Default.ShoppingBag,
                            accent = if (isSyncing) MaterialTheme.colorScheme.primary else SpotifyGreen,
                            tag = if (isSyncing) "SYNC" else "MERCH",
                        )
                    }
                } else if (isWideLayout) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            ShopCollabSidebar(
                                lanes = collabLanes,
                                selectedLaneId = selectedCollabLaneId,
                                onSelect = { lane -> selectedCollabLaneId = lane.id },
                                modifier = Modifier.width(248.dp),
                            )

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                ShopCollabSelectionCard(
                                    lane = selectedCollabLane,
                                    totalItemCount = uiState.items.size,
                                )

                                if (filteredItems.isEmpty()) {
                                    ShopMessageCard(
                                        title = "Noch keine Pieces",
                                        body = "In ${selectedCollabLane.title} ist gerade noch kein sichtbarer Merch.",
                                        icon = Icons.Default.Person,
                                        accent = MaterialTheme.colorScheme.primary,
                                        tag = "FILTER",
                                    )
                                } else {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                    ) {
                                        filteredItems.forEach { item ->
                                            MerchandiseCard(
                                                item = item,
                                                onTap = viewModel::selectItem,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            ShopCollabCarousel(
                                lanes = collabLanes,
                                selectedLaneId = selectedCollabLaneId,
                                totalItemCount = uiState.items.size,
                                onSelect = { lane -> selectedCollabLaneId = lane.id },
                            )
                        }
                    }

                    if (filteredItems.isEmpty()) {
                        item {
                            ShopMessageCard(
                                title = "Noch keine Pieces",
                                body = "In ${selectedCollabLane.title} ist gerade noch kein sichtbarer Merch.",
                                icon = Icons.Default.Person,
                                accent = MaterialTheme.colorScheme.primary,
                                tag = "FILTER",
                            )
                        }
                    } else {
                        items(filteredItems, key = { it.id.orEmpty() }) { item ->
                            MerchandiseCard(
                                item = item,
                                onTap = viewModel::selectItem,
                            )
                        }
                    }
                }
            }

            uiState.selectedItem?.let { item ->
                MerchandiseDetailSheet(
                    item = item,
                    isStoreOpen = uiState.isStoreOpen,
                    canCheckout = uiState.isStoreOpen || uiState.isAdmin,
                    onDismiss = viewModel::dismissSelectedItem,
                    onAddToCart = { size, color, quantity ->
                        val result = viewModel.addSelectionToCart(
                            item = item,
                            size = size,
                            color = color,
                            quantity = quantity,
                        )
                        if (result.isSuccess) {
                            viewModel.dismissSelectedItem()
                        }
                    },
                )
            }

            ToastHost(
                message = uiState.toastMessage ?: uiState.errorMessage,
                type = if (uiState.isErrorToast || uiState.errorMessage != null) ToastType.Error else ToastType.Success,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }
    }
}

@Composable
private fun ShopOverviewCard(
    uiState: ShopUiState,
    laneCount: Int,
) {
    val screenHeaderSettings by AppContainer.screenHeaderSettingsRepository.settings.collectAsStateWithLifecycle()
    val pieceLabel = when {
        uiState.isCatalogLoading || uiState.isSyncingCatalog -> "Sync live"
        uiState.items.size == 1 -> "1 Piece"
        else -> "${uiState.items.size} Pieces"
    }
    BrandHeroCard(
        eyebrow = screenHeaderSettings.shopEyebrow.ifBlank { "SKY²²" },
        title = screenHeaderSettings.shopTitle.ifBlank { "Merch" },
        subtitle = screenHeaderSettings.shopSubtitle.ifBlank { "Drops, Pieces und Checkout direkt im Sky²² Store." },
        detail = screenHeaderSettings.shopDetail.ifBlank {
            if (uiState.isCatalogLoading) {
                "Produkte und Verfuegbarkeit werden synchronisiert."
            } else if (uiState.isStoreOpen) {
                "Bestellungen direkt aus dem Merch Hub."
            } else {
                "Checkout ist gerade pausiert."
            }
        },
        backgroundImageUrl = screenHeaderSettings.shopImageUrl.ifBlank { null },
        accent = SpotifyGreen,
        secondaryAccent = YouTubeDeepRed,
        marks = listOf(BrandArtwork.Combined),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BrandPill(text = pieceLabel, tint = SpotifyGreen)
                BrandPill(
                    text = if (uiState.isStoreOpen) "Checkout live" else "Checkout pausiert",
                    tint = if (uiState.isStoreOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                )
                if (laneCount > 0) {
                    BrandPill(
                        text = if (laneCount == 1) "1 Lane" else "$laneCount Lanes",
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                }
                BrandPill(
                    text = if (uiState.isLoggedIn) "Account" else "Gast",
                    tint = MaterialTheme.colorScheme.tertiary,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ShopHeroStatusCard(
                    label = "Pieces",
                    value = if (uiState.isCatalogLoading || uiState.isSyncingCatalog) "Sync" else uiState.items.size.toString(),
                    icon = Icons.Default.ShoppingBag,
                    accent = SpotifyGreen,
                    isActive = uiState.isCatalogLoading || uiState.isSyncingCatalog || uiState.items.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                )
                ShopHeroStatusCard(
                    label = "Store",
                    value = if (uiState.isStoreOpen) "Live" else "Pause",
                    icon = Icons.Default.CheckCircle,
                    accent = MaterialTheme.colorScheme.primary,
                    isActive = uiState.isStoreOpen,
                    modifier = Modifier.weight(1f),
                )
                ShopHeroStatusCard(
                    label = "Access",
                    value = if (uiState.isLoggedIn) "Account" else "Gast",
                    icon = Icons.Default.Person,
                    accent = YouTubeDeepRed,
                    isActive = uiState.isLoggedIn,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ShopHeroStatusCard(
    label: String,
    value: String,
    icon: ImageVector,
    accent: Color,
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    BrandHeroMetricCard(
        label = label,
        value = value,
        accent = accent,
        modifier = modifier,
        icon = icon,
        isActive = isActive,
    )
}

@Composable
private fun ShopAdminControlsCard(
    uiState: ShopUiState,
    onToggleStore: () -> Unit,
    onSyncShopify: () -> Unit,
) {
    SkydownCard(contentPadding = PaddingValues(14.dp)) {
        BrandSectionBanner(
            title = "Store Control",
            subtitle = "Shopify liefert Produkte. Hier steuerst du Sichtbarkeit und Sync getrennt vom Header.",
            accent = MaterialTheme.colorScheme.primary,
            icon = Icons.Default.Sync,
            tag = "ADMIN",
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BrandActionButton(
                text = if (uiState.isUpdatingStoreState) {
                    "Store wird aktualisiert..."
                } else if (uiState.isStoreOpen) {
                    "Store schliessen"
                } else {
                    "Store oeffnen"
                },
                onClick = onToggleStore,
                accent = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
                icon = Icons.Default.CheckCircle,
                enabled = !uiState.isUpdatingStoreState,
            )

            BrandActionButton(
                text = if (uiState.isSyncingCatalog) "Katalog laedt..." else "Shopify syncen",
                onClick = onSyncShopify,
                accent = SpotifyGreen,
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Sync,
                enabled = !uiState.isSyncingCatalog,
                isLoading = uiState.isSyncingCatalog,
            )
        }
    }
}

@Composable
private fun ShopMessageCard(
    title: String,
    body: String,
    icon: ImageVector = Icons.Default.ShoppingBag,
    accent: Color = SpotifyGreen,
    tag: String? = null,
) {
    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        BrandSectionBanner(
            title = title,
            accent = accent,
            icon = icon,
            tag = tag,
        )
        Text(
            text = body,
            modifier = Modifier.padding(top = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
        )
    }
}

@Composable
private fun LoginSection(
    errorMessage: String,
    onOpenLogin: () -> Unit,
) {
    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        BrandSectionBanner(
            title = "Login",
            subtitle = "Melde dich an, um Checkout und Account-Funktionen zu nutzen.",
            accent = MaterialTheme.colorScheme.primary,
            icon = Icons.Default.Person,
            tag = "ACCESS",
        )
        Text(
            text = errorMessage,
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
        )
        BrandActionButton(
            text = "Anmelden",
            onClick = onOpenLogin,
            accent = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            icon = Icons.Default.Person,
        )
    }
}

private data class ShopCollabLane(
    val id: String,
    val title: String,
    val subtitle: String,
    val itemCount: Int,
    val isCoreLane: Boolean,
) {
    companion object {
        const val ALL_ID = "all-drops"
    }
}

private fun buildShopCollabLanes(items: List<MerchandiseItem>): List<ShopCollabLane> {
    if (items.isEmpty()) {
        return listOf(
            ShopCollabLane(
                id = ShopCollabLane.ALL_ID,
                title = "All Drops",
                subtitle = "Alle Collabs und Core Pieces.",
                itemCount = 0,
                isCoreLane = false,
            ),
        )
    }

    val laneRows = mutableMapOf<String, Triple<String, String, Boolean>>()
    val laneItemIds = mutableMapOf<String, MutableSet<String>>()

    items.forEach { item ->
        val memberships = item.laneMemberships().distinctBy { it.first }
        memberships.forEach { (laneId, laneType) ->
            laneRows.putIfAbsent(
                laneId,
                Triple(
                    item.titleForLane(laneId, laneType),
                    item.subtitleForLane(laneId, laneType),
                    item.isCoreLane(laneType),
                ),
            )
            laneItemIds.getOrPut(laneId) { mutableSetOf() }
                .add(item.id ?: item.shopifyProductId ?: item.name)
        }
    }

    val lanes = laneRows.map { (laneId, laneMeta) ->
        ShopCollabLane(
            id = laneId,
            title = laneMeta.first,
            subtitle = laneMeta.second,
            itemCount = laneItemIds[laneId]?.size ?: 0,
            isCoreLane = laneMeta.third,
        )
    }.sortedWith(
        compareBy<ShopCollabLane> { it.isCoreLane }
            .thenByDescending { it.itemCount }
            .thenBy { it.title.lowercase() },
    )

    return listOf(
        ShopCollabLane(
            id = ShopCollabLane.ALL_ID,
            title = "All Drops",
            subtitle = "Alle Collabs und Core Pieces.",
            itemCount = items.size,
            isCoreLane = false,
        ),
    ) + lanes
}

private fun MerchandiseItem.laneMemberships(): List<Pair<String, String>> {
    val normalizedHandles = shopifyCollectionHandles
        .mapNotNull { value ->
            value.trim().lowercase().takeIf { it.isNotEmpty() }
        }
        .distinct()
    if (!shopifyProductId.isNullOrBlank() && normalizedHandles.isNotEmpty()) {
        return normalizedHandles.map { handle -> "collection:$handle" to "collection" }
    }
    return listOf(merchCategoryKey to "category")
}

private fun MerchandiseItem.belongsToLane(laneId: String): Boolean {
    return laneMemberships().any { it.first == laneId }
}

private fun MerchandiseItem.titleForLane(laneId: String, laneType: String): String {
    if (laneType == "collection") {
        return laneId.removePrefix("collection:").prettifiedCollectionHandle()
    }
    return merchCategoryTitle
}

private fun MerchandiseItem.subtitleForLane(laneId: String, laneType: String): String {
    if (laneType == "collection") {
        return "Shopify Collection"
    }
    return merchCategorySubtitle
}

private fun MerchandiseItem.isCoreLane(laneType: String): Boolean {
    return laneType != "collection" && !hasCuratedMerchCategory
}

private fun String.prettifiedCollectionHandle(): String {
    return split("-")
        .filter { it.isNotBlank() }
        .joinToString(" ") { part ->
            part.replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase() else char.toString()
            }
        }
}

@Composable
private fun ShopCollabSidebar(
    lanes: List<ShopCollabLane>,
    selectedLaneId: String,
    onSelect: (ShopCollabLane) -> Unit,
    modifier: Modifier = Modifier,
) {
    SkydownCard(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            BrandSectionBanner(
                title = "Collection Sidebar",
                subtitle = "Collections",
                accent = MaterialTheme.colorScheme.primary,
                icon = Icons.Default.Person,
                tag = "LANES",
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                lanes.forEach { lane ->
                    ShopCollabSidebarButton(
                        lane = lane,
                        isSelected = lane.id == selectedLaneId,
                        onTap = { onSelect(lane) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ShopCollabRail(
    lanes: List<ShopCollabLane>,
    selectedLaneId: String,
    onSelect: (ShopCollabLane) -> Unit,
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        lanes.forEach { lane ->
            ShopCollabSidebarButton(
                lane = lane,
                isSelected = lane.id == selectedLaneId,
                compact = true,
                onTap = { onSelect(lane) },
                modifier = Modifier.width(214.dp),
            )
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun ShopCollabCarousel(
    lanes: List<ShopCollabLane>,
    selectedLaneId: String,
    totalItemCount: Int,
    onSelect: (ShopCollabLane) -> Unit,
) {
    val safeLanes = lanes.ifEmpty {
        listOf(
            ShopCollabLane(
                id = ShopCollabLane.ALL_ID,
                title = "All Drops",
                subtitle = "Alle Collabs und Core Pieces.",
                itemCount = totalItemCount,
                isCoreLane = false,
            ),
        )
    }
    val initialPage = safeLanes.indexOfFirst { it.id == selectedLaneId }.takeIf { it >= 0 } ?: 0
    val pagerState = rememberPagerState(initialPage = initialPage) { safeLanes.size }

    LaunchedEffect(selectedLaneId, safeLanes) {
        val targetPage = safeLanes.indexOfFirst { it.id == selectedLaneId }.takeIf { it >= 0 } ?: 0
        if (targetPage != pagerState.currentPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    LaunchedEffect(pagerState.settledPage, safeLanes) {
        safeLanes.getOrNull(pagerState.settledPage)?.let { lane ->
            if (lane.id != selectedLaneId) {
                onSelect(lane)
            }
        }
    }

    Column {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            pageSpacing = 12.dp,
            contentPadding = PaddingValues(horizontal = 6.dp),
        ) { pageIndex ->
            val lane = safeLanes[pageIndex]
            ShopCollabSelectionCard(
                lane = lane,
                totalItemCount = totalItemCount,
            )
        }

        if (safeLanes.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                safeLanes.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (pagerState.currentPage == index) 9.dp else 7.dp)
                            .clip(CircleShape)
                            .background(
                                if (pagerState.currentPage == index) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
                                },
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun ShopCollabSelectionCard(
    lane: ShopCollabLane,
    totalItemCount: Int,
) {
    SkydownCard(contentPadding = PaddingValues(16.dp)) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BrandSectionBanner(
                title = lane.title,
                subtitle = lane.subtitle,
                accent = MaterialTheme.colorScheme.primary,
                icon = if (lane.id == ShopCollabLane.ALL_ID) Icons.Default.ShoppingBag else Icons.Default.Person,
                tag = if (lane.itemCount == 1) "1 PIECE" else "${lane.itemCount} PIECES",
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BrandStatusChip(
                    text = when {
                        lane.id == ShopCollabLane.ALL_ID -> "Alle"
                        lane.isCoreLane -> "Core"
                        else -> "Collection"
                    },
                    accent = MaterialTheme.colorScheme.secondary,
                )
                BrandStatusChip(
                    text = if (lane.itemCount == totalItemCount) "Gesamt" else "Auswahl",
                    accent = SpotifyGreen,
                )
            }
        }
    }
}

@Composable
private fun ShopCollabSidebarButton(
    lane: ShopCollabLane,
    isSelected: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.52f)
    }
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .clickable(onClick = onTap)
            .padding(horizontal = if (compact) 14.dp else 16.dp, vertical = if (compact) 14.dp else 15.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.92f)
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        },
                    ),
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = lane.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                )
                Text(
                    text = lane.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = if (isSelected) 0.84f else 0.74f),
                )
            }
        }

        BrandStatusChip(
            text = if (lane.itemCount == 1) "1 Piece" else "${lane.itemCount} Pieces",
            accent = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.primary
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun MerchandiseDetailSheet(
    item: MerchandiseItem,
    isStoreOpen: Boolean,
    canCheckout: Boolean,
    onDismiss: () -> Unit,
    onAddToCart: (String, String?, Int) -> Unit,
) {
    val scrollState = rememberScrollState()
    val pagerState = rememberPagerState(pageCount = { item.imageUrls.size.coerceAtLeast(1) })
    var fullscreenGalleryInitialPage by rememberSaveable { mutableStateOf<Int?>(null) }
    val sizeOptions = remember(item.id, item.variants) {
        MerchandiseVariantResolver.availableSizes(item).ifEmpty {
            listOf("XS", "S", "M", "L", "XL")
        }
    }
    var selectedSize by rememberSaveable(item.id) { mutableStateOf(sizeOptions.firstOrNull().orEmpty()) }
    val colorOptions = remember(item.id, item.variants, selectedSize) {
        MerchandiseVariantResolver.availableColors(item, selectedSize)
    }
    var selectedColor by rememberSaveable(item.id) { mutableStateOf(colorOptions.firstOrNull().orEmpty()) }
    var quantity by rememberSaveable(item.id) { mutableIntStateOf(1) }

    LaunchedEffect(item.id, colorOptions) {
        if (selectedColor.isNotBlank() && colorOptions.any { it.equals(selectedColor, ignoreCase = true) }) {
            return@LaunchedEffect
        }
        selectedColor = colorOptions.firstOrNull().orEmpty()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.52f))
                .padding(12.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.99f))
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(460.dp)
                    .clip(RoundedCornerShape(30.dp)),
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { fullscreenGalleryInitialPage = pagerState.currentPage },
                ) { page ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = item.imageUrls.getOrNull(page),
                            contentDescription = item.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.04f),
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.12f),
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                        ),
                                    ),
                                ),
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ShopBadge(
                        text = if (item.available) "Drop live" else "Nicht verfuegbar",
                        icon = if (item.available) Icons.Default.CheckCircle else Icons.Default.Sync,
                        isActive = item.available,
                    )
                    if (item.hasCuratedMerchCategory) {
                        ShopBadge(
                            text = item.merchCategoryTitle,
                            icon = Icons.Default.Person,
                            isActive = false,
                        )
                    }
                    ShopBadge(
                        text = if (isStoreOpen) "Store offen" else "Store pausiert",
                        icon = if (isStoreOpen) Icons.Default.CheckCircle else Icons.Default.Sync,
                        isActive = isStoreOpen,
                    )
                    if (item.imageUrls.size > 1) {
                        ShopBadge(
                            text = "${item.imageUrls.size} Bilder",
                            icon = Icons.Default.ShoppingBag,
                            isActive = false,
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Schliessen",
                    )
                }

                TextButton(
                    onClick = { fullscreenGalleryInitialPage = pagerState.currentPage },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 20.dp, bottom = 18.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.76f)),
                ) {
                    Text("Vollbild")
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 64.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = "EUR ${String.format(java.util.Locale.US, "%.2f", item.price)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.94f),
                    )
                }

                if (item.imageUrls.size > 1) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        repeat(item.imageUrls.size) { index ->
                            Box(
                                modifier = Modifier
                                    .size(if (pagerState.currentPage == index) 10.dp else 8.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(
                                        if (pagerState.currentPage == index) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.35f)
                                        },
                                    ),
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f),
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (item.hasCuratedMerchCategory) {
                        ShopBadge(
                            text = item.merchCategoryTitle,
                            icon = Icons.Default.Person,
                            isActive = false,
                        )
                    }
                    item.customBadge.takeIf { it.isNotBlank() }?.let { badge ->
                        ShopBadge(
                            text = badge,
                            icon = Icons.Default.Sync,
                            isActive = true,
                        )
                    }
                    ShopBadge(
                        text = if (item.available) "Verfuegbar" else "Pause",
                        icon = if (item.available) Icons.Default.CheckCircle else Icons.Default.Sync,
                        isActive = item.available,
                    )
                    ShopBadge(
                        text = "Produktansicht",
                        icon = Icons.Default.ShoppingBag,
                        isActive = false,
                    )
                }

                if (!canCheckout) {
                    Text(
                        text = "Der Merch Store ist aktuell geschlossen. Produkte bleiben sichtbar, neue Kaeufe sind aber pausiert.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Variante waehlen",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        sizeOptions.forEach { size ->
                            FilterPill(
                                text = size,
                                selected = selectedSize.equals(size, ignoreCase = true),
                                onTap = { selectedSize = size },
                            )
                        }
                    }

                    if (colorOptions.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            colorOptions.forEach { color ->
                                FilterPill(
                                    text = color,
                                    selected = selectedColor.equals(color, ignoreCase = true),
                                    onTap = { selectedColor = color },
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Menge",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        )
                        TextButton(onClick = { if (quantity > 1) quantity -= 1 }) {
                            Text("-")
                        }
                        Text(
                            text = quantity.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        TextButton(onClick = { if (quantity < 10) quantity += 1 }) {
                            Text("+")
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Schliessen")
                }
                BrandActionButton(
                    text = "In den Warenkorb",
                    onClick = { onAddToCart(selectedSize, selectedColor.takeIf { it.isNotBlank() }, quantity) },
                    accent = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.ShoppingBag,
                    enabled = item.available && canCheckout && selectedSize.isNotBlank(),
                )
            }

            Box(modifier = Modifier.height(2.dp))
        }
    }

    fullscreenGalleryInitialPage?.let { page ->
        MerchandiseImageViewerDialog(
            itemName = item.name,
            imageUrls = item.imageUrls,
            initialPage = page,
            onDismiss = { fullscreenGalleryInitialPage = null },
        )
    }
}

@Composable
private fun FilterPill(
    text: String,
    selected: Boolean,
    onTap: () -> Unit,
) {
    val background = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }
    val content = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .clickable(onClick = onTap)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = content,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MerchandiseImageViewerDialog(
    itemName: String,
    imageUrls: List<String>,
    initialPage: Int,
    onDismiss: () -> Unit,
) {
    val safeInitialPage = initialPage.coerceIn(0, imageUrls.lastIndex.coerceAtLeast(0))
    val pagerState = rememberPagerState(
        initialPage = safeInitialPage,
        pageCount = { imageUrls.size.coerceAtLeast(1) },
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.94f)),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = imageUrls.getOrNull(page),
                        contentDescription = itemName,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 48.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = itemName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Text(
                        text = "${pagerState.currentPage + 1} von ${imageUrls.size.coerceAtLeast(1)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.72f),
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.16f)),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Schliessen",
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun ShopBadge(
    text: String,
    icon: ImageVector,
    isActive: Boolean,
) {
    BrandStatusChip(
        text = text,
        accent = MaterialTheme.colorScheme.primary,
        icon = icon,
        isActive = isActive,
    )
}
