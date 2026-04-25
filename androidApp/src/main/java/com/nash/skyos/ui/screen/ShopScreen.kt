package com.nash.skyos.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.nash.skyos.R
import com.nash.skyos.data.AppContainer
import com.nash.skyos.ui.component.AppTopBarSessionActions
import com.nash.skyos.ui.component.BrandActionButton
import com.nash.skyos.ui.component.BrandArtwork
import com.nash.skyos.ui.component.BrandHeroCard
import com.nash.skyos.ui.component.BrandHeroMetricCard
import com.nash.skyos.ui.component.BrandSectionBanner
import com.nash.skyos.ui.component.BrandStatusChip
import com.nash.skyos.ui.component.BrandPill
import com.nash.skyos.ui.component.MerchandiseCard
import com.nash.skyos.ui.component.SkydownCard
import com.nash.skyos.ui.component.SkydownTopBarTitle
import com.nash.skyos.ui.component.ToastHost
import com.nash.skyos.ui.component.ToastType
import com.nash.skyos.ui.component.rememberSkydownScreenSectionSpacing
import com.nash.skyos.ui.component.skydownContentPadding
import com.nash.skyos.ui.component.skydownPressable
import com.nash.skyos.ui.component.skydownScreenBrush
import com.nash.skyos.ui.component.skydownTopBarColors
import com.nash.skyos.ui.theme.skydownAccentMystic
import com.nash.skyos.ui.model.ShopUiState
import com.nash.skyos.ui.viewmodel.ShopViewModel
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
    onGuestSignIn: (() -> Unit)? = null,
    onOpenCart: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    viewModel: ShopViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val mainSectionSpacing = rememberSkydownScreenSectionSpacing() + 3.dp
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
    val featuredDropItem = remember(uiState.items) {
        uiState.items.firstOrNull { it.featured && it.available }
            ?: uiState.items.firstOrNull { it.available }
            ?: uiState.items.firstOrNull()
    }
    val editorialPickItems = remember(uiState.items) {
        uiState.items
            .sortedWith(
                compareByDescending<MerchandiseItem> { it.featured }
                    .thenBy { it.sortOrder }
                    .thenBy { it.name.lowercase() },
            )
            .take(5)
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
                TopAppBar(
                title = {
                    SkydownTopBarTitle(
                        "Shop",
                        stringResource(R.string.shop_topbar_subtitle),
                        accent = MaterialTheme.colorScheme.tertiary,
                    )
                },
                actions = {
                    AppTopBarSessionActions(
                        onOpenCart = onOpenCart,
                        onOpenProfile = onOpenProfile,
                        onOpenSettings = onOpenSettings,
                        onGuestSignIn = onGuestSignIn,
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
            )
        },
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    skydownScreenBrush(
                        primaryColor = MaterialTheme.colorScheme.primary,
                        secondaryColor = MaterialTheme.colorScheme.skydownAccentMystic(),
                        primaryAlpha = 0.028f,
                        secondaryAlpha = 0.018f,
                    ),
                ),
        ) {
            val isWideLayout = maxWidth >= 920.dp
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("shop.root"),
                contentPadding = skydownContentPadding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(mainSectionSpacing),
            ) {
                item {
                    ShopOverviewCard(
                        uiState = uiState,
                        laneCount = laneCount,
                    )
                }

                if (uiState.items.isNotEmpty()) {
                    item {
                        ShopMerchOpeningBlock(
                            showFeatured = editorialPickItems.isNotEmpty(),
                            featuredItem = featuredDropItem,
                            editorialPicks = editorialPickItems,
                            onOpenItem = viewModel::selectItem,
                            showConnectorLineAboveBrowse = editorialPickItems.isNotEmpty(),
                        )
                    }
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
                            title = stringResource(R.string.shop_paused_title),
                            body = stringResource(R.string.shop_paused_body),
                            icon = Icons.Default.Close,
                            accent = MaterialTheme.colorScheme.secondary,
                            tag = stringResource(R.string.shop_tag_pause),
                        )
                    }
                }

                if (uiState.items.isEmpty()) {
                    item {
                        val isSyncing = uiState.isCatalogLoading || uiState.isSyncingCatalog
                        ShopMessageCard(
                            title = stringResource(
                                if (isSyncing) {
                                    R.string.shop_empty_title_loading
                                } else {
                                    R.string.shop_empty_title_idle
                                },
                            ),
                            body = if (uiState.isAdmin) {
                                if (isSyncing) {
                                    stringResource(R.string.shop_empty_body_loading_admin)
                                } else {
                                    stringResource(R.string.shop_empty_body_idle_guest)
                                }
                            } else {
                                if (isSyncing) {
                                    stringResource(R.string.shop_empty_body_loading_guest)
                                } else {
                                    stringResource(R.string.shop_empty_body_idle_guest)
                                }
                            },
                            icon = if (isSyncing) Icons.Default.Sync else Icons.Default.ShoppingBag,
                            accent = MaterialTheme.colorScheme.primary,
                            tag = if (isSyncing) {
                                stringResource(R.string.shop_empty_tag_sync)
                            } else {
                                stringResource(R.string.shop_empty_tag_merch)
                            },
                        )
                    }
                } else {
                    if (isWideLayout) {
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
                                    verticalArrangement = Arrangement.spacedBy(18.dp),
                                ) {
                                    ShopCollabSelectionCard(
                                        lane = selectedCollabLane,
                                        totalItemCount = uiState.items.size,
                                    )

                                    if (filteredItems.isEmpty()) {
                                        ShopMessageCard(
                                            title = stringResource(R.string.shop_filter_empty_title),
                                            body = stringResource(
                                                R.string.shop_filter_empty_body,
                                                shopLaneTitle(selectedCollabLane),
                                            ),
                                            icon = Icons.Default.Person,
                                            accent = MaterialTheme.colorScheme.primary,
                                            tag = "FILTER",
                                        )
                                    } else {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(9.dp),
                                        ) {
                                            filteredItems.forEachIndexed { index, item ->
                                                MerchandiseCard(
                                                    item = item,
                                                    onTap = viewModel::selectItem,
                                                    modifier = Modifier.padding(
                                                        top = if (index == 2) 18.dp else 0.dp,
                                                    ),
                                                    shelfHighlight = index < 2,
                                                    shelfSettled = index > 2,
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

                                if (collabLanes.size > 1) {
                                    ShopCollabQuickGrid(
                                        lanes = collabLanes,
                                        selectedLaneId = selectedCollabLaneId,
                                        onSelect = { lane -> selectedCollabLaneId = lane.id },
                                    )
                                }
                            }
                        }
                    }

                    if (!isWideLayout) {
                        if (filteredItems.isEmpty()) {
                            item {
                                ShopMessageCard(
                                    title = stringResource(R.string.shop_filter_empty_title),
                                    body = stringResource(
                                        R.string.shop_filter_empty_body,
                                        shopLaneTitle(selectedCollabLane),
                                    ),
                                    icon = Icons.Default.Person,
                                    accent = MaterialTheme.colorScheme.primary,
                                    tag = "FILTER",
                                )
                            }
                        } else {
                            itemsIndexed(
                                filteredItems,
                                key = { _, it -> it.id.orEmpty() },
                            ) { index, item ->
                                MerchandiseCard(
                                    item = item,
                                    onTap = viewModel::selectItem,
                                    modifier = Modifier.padding(
                                        top = if (index == 2) 18.dp else 0.dp,
                                    ),
                                    shelfHighlight = index < 2,
                                    shelfSettled = index > 2,
                                )
                            }
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
        uiState.isCatalogLoading || uiState.isSyncingCatalog -> stringResource(R.string.shop_hero_pill_updating)
        uiState.items.size == 1 -> stringResource(R.string.shop_hero_pill_product_one)
        else -> stringResource(R.string.shop_hero_pill_product_other, uiState.items.size)
    }
    BrandHeroCard(
        eyebrow = screenHeaderSettings.shopEyebrow.ifBlank { "SKY OS" },
        title = screenHeaderSettings.shopTitle.ifBlank { "Merch" },
        subtitle = screenHeaderSettings.shopSubtitle.ifBlank { "Drops und Pieces." },
        detail = screenHeaderSettings.shopDetail.ifBlank {
            if (uiState.isCatalogLoading || uiState.isSyncingCatalog) {
                stringResource(R.string.shop_hero_detail_updating)
            } else if (uiState.isStoreOpen) {
                stringResource(R.string.shop_hero_detail_checkout_on)
            } else {
                stringResource(R.string.shop_hero_detail_checkout_off)
            }
        },
        backgroundImageUrl = screenHeaderSettings.shopImageUrl.ifBlank { null },
        accent = MaterialTheme.colorScheme.primary,
        secondaryAccent = MaterialTheme.colorScheme.tertiary,
        marks = listOf(BrandArtwork.Combined),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BrandPill(text = pieceLabel, tint = MaterialTheme.colorScheme.primary)
                BrandPill(
                    text = if (uiState.isStoreOpen) {
                        stringResource(R.string.shop_hero_pill_checkout_on)
                    } else {
                        stringResource(R.string.shop_hero_pill_checkout_off)
                    },
                    tint = if (uiState.isStoreOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                )
                if (laneCount > 0) {
                    BrandPill(
                        text = if (laneCount == 1) {
                            stringResource(R.string.shop_hero_pill_collection_one)
                        } else {
                            stringResource(R.string.shop_hero_pill_collection_other, laneCount)
                        },
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                }
                BrandPill(
                    text = if (uiState.isLoggedIn) {
                        stringResource(R.string.shop_hero_pill_account)
                    } else {
                        stringResource(R.string.shop_hero_pill_guest)
                    },
                    tint = MaterialTheme.colorScheme.tertiary,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ShopHeroStatusCard(
                    label = stringResource(R.string.shop_hero_metric_catalog),
                    value = if (uiState.isCatalogLoading || uiState.isSyncingCatalog) {
                        stringResource(R.string.shop_hero_metric_value_updating)
                    } else {
                        uiState.items.size.toString()
                    },
                    icon = Icons.Default.ShoppingBag,
                    accent = MaterialTheme.colorScheme.primary,
                    isActive = uiState.isCatalogLoading || uiState.isSyncingCatalog || uiState.items.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                )
                ShopHeroStatusCard(
                    label = stringResource(R.string.shop_hero_metric_store),
                    value = if (uiState.isStoreOpen) {
                        stringResource(R.string.shop_hero_metric_value_open)
                    } else {
                        stringResource(R.string.shop_hero_metric_value_closed)
                    },
                    icon = Icons.Default.CheckCircle,
                    accent = MaterialTheme.colorScheme.primary,
                    isActive = uiState.isStoreOpen,
                    modifier = Modifier.weight(1f),
                )
                ShopHeroStatusCard(
                    label = stringResource(R.string.shop_hero_metric_account),
                    value = if (uiState.isLoggedIn) {
                        stringResource(R.string.shop_hero_metric_signed_in)
                    } else {
                        stringResource(R.string.shop_hero_metric_guest)
                    },
                    icon = Icons.Default.Person,
                    accent = MaterialTheme.colorScheme.tertiary,
                    isActive = uiState.isLoggedIn,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ShopBrowseEntryHint(
    showConnectorLine: Boolean,
) {
    val edge = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (showConnectorLine) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to Color.Transparent,
                                0.5f to edge,
                                1f to Color.Transparent,
                            ),
                        ),
                    ),
            )
        }
        Text(
            text = stringResource(R.string.shop_browse_headline),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            letterSpacing = 0.2.sp,
        )
        Text(
            text = stringResource(R.string.shop_browse_subline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
        )
    }
}

@Composable
private fun ShopMerchOpeningBlock(
    showFeatured: Boolean,
    featuredItem: MerchandiseItem?,
    editorialPicks: List<MerchandiseItem>,
    onOpenItem: (MerchandiseItem) -> Unit,
    showConnectorLineAboveBrowse: Boolean,
) {
    Column(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        if (showFeatured) {
            ShopLandingCuratedModule(
                featuredItem = featuredItem,
                editorialPicks = editorialPicks,
                onOpenItem = onOpenItem,
            )
        }
        ShopBrowseEntryHint(showConnectorLine = showConnectorLineAboveBrowse)
    }
}

@Composable
private fun ShopLandingCuratedModule(
    featuredItem: MerchandiseItem?,
    editorialPicks: List<MerchandiseItem>,
    onOpenItem: (MerchandiseItem) -> Unit,
) {
    val momentShape = RoundedCornerShape(32.dp)
    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrandStatusChip(
                    text = stringResource(R.string.shop_featured_chip),
                    accent = MaterialTheme.colorScheme.tertiary,
                    isActive = true,
                )
                Text(
                    text = stringResource(R.string.shop_featured_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Text(
                text = stringResource(R.string.shop_featured_hint),
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 22.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
            )
        }

        featuredItem?.let { item ->
            val imageUrl = item.imageUrls.firstOrNull()?.takeIf { it.isNotBlank() }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(momentShape)
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                    )
                    .clickable { onOpenItem(item) },
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(288.dp),
                ) {
                    if (imageUrl != null) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f),
                                ),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0f to Color.Transparent,
                                        0.55f to Color.Transparent,
                                        1f to Color.Black.copy(alpha = 0.2f),
                                    ),
                                ),
                            ),
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.shop_featured_badge),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp,
                    )
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            lineHeight = 32.sp,
                        ),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${item.currency} ${"%.2f".format(item.price)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            editorialPicks.forEach { item ->
                Box(
                    modifier = Modifier
                        .width(132.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                        )
                        .clickable { onOpenItem(item) }
                        .padding(horizontal = 10.dp, vertical = 9.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                        )
                        Text(
                            text = "${item.currency} ${"%.2f".format(item.price)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        )
                    }
                }
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
            subtitle = "Store Status und Refresh.",
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
                text = if (uiState.isSyncingCatalog) "Store laedt..." else "Store aktualisieren",
                onClick = onSyncShopify,
                accent = MaterialTheme.colorScheme.tertiary,
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
    accent: Color? = null,
    tag: String? = null,
) {
    val bannerAccent = accent ?: MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f))
            .border(
                width = 1.dp,
                color = bannerAccent.copy(alpha = 0.20f),
                shape = shape,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = bannerAccent,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.weight(1f))
            tag?.takeIf { it.isNotBlank() }?.let { label ->
                BrandStatusChip(
                    text = label,
                    accent = bannerAccent,
                    isActive = true,
                )
            }
        }
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
        )
    }
}

@Composable
private fun LoginSection(
    errorMessage: String,
    onOpenLogin: () -> Unit,
) {
    SkydownCard {
        BrandSectionBanner(
            title = stringResource(R.string.auth_merch_login_banner_title),
            subtitle = stringResource(R.string.auth_merch_login_banner_subtitle),
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
            text = stringResource(R.string.auth_merch_login_cta),
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

@Composable
private fun shopLaneTitle(lane: ShopCollabLane): String = when (lane.id) {
    ShopCollabLane.ALL_ID -> stringResource(R.string.shop_lane_all_drops_title)
    else -> lane.title
}

@Composable
private fun shopLaneBannerSubtitle(lane: ShopCollabLane): String = when (lane.id) {
    ShopCollabLane.ALL_ID -> stringResource(R.string.shop_lane_all_drops_subtitle)
    else -> lane.subtitle
}

@Composable
private fun shopLaneKindLabel(lane: ShopCollabLane): String = when {
    lane.id == ShopCollabLane.ALL_ID -> stringResource(R.string.shop_lane_kind_all)
    lane.isCoreLane -> stringResource(R.string.shop_lane_kind_core)
    else -> stringResource(R.string.shop_lane_kind_collection)
}

@Composable
private fun shopPiecesLabel(count: Int): String = if (count == 1) {
    stringResource(R.string.shop_pieces_in_lane_one)
} else {
    stringResource(R.string.shop_pieces_in_lane_other, count)
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
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            BrandSectionBanner(
                title = stringResource(R.string.shop_map_title),
                subtitle = stringResource(R.string.shop_map_subtitle),
                accent = MaterialTheme.colorScheme.primary,
                icon = Icons.Default.Person,
                tag = stringResource(R.string.shop_map_tag),
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
private fun ShopCollabQuickGrid(
    lanes: List<ShopCollabLane>,
    selectedLaneId: String,
    onSelect: (ShopCollabLane) -> Unit,
) {
    SkydownCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BrandSectionBanner(
                title = stringResource(R.string.shop_quick_title),
                subtitle = stringResource(R.string.shop_quick_subtitle),
                accent = MaterialTheme.colorScheme.primary,
                icon = Icons.Default.ShoppingBag,
                tag = stringResource(R.string.shop_quick_tag),
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                lanes.forEach { lane ->
                    ShopCollabSidebarButton(
                        lane = lane,
                        isSelected = lane.id == selectedLaneId,
                        compact = true,
                        onTap = { onSelect(lane) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
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
            modifier = Modifier
                .fillMaxWidth()
                .height(218.dp),
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
    val laneLabel = shopPiecesLabel(lane.itemCount)
    val laneKindLabel = shopLaneKindLabel(lane)
    val coverageLabel = if (totalItemCount <= 0) {
        "0%"
    } else {
        "${((lane.itemCount.toFloat() / totalItemCount.toFloat()) * 100f).toInt()}%"
    }
    val focusDetail = if (lane.id == ShopCollabLane.ALL_ID) {
        stringResource(R.string.shop_lane_focus_all)
    } else {
        stringResource(R.string.shop_lane_focus_named, shopLaneTitle(lane))
    }

    SkydownCard(
        modifier = Modifier.height(176.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BrandSectionBanner(
                title = shopLaneTitle(lane),
                subtitle = if (lane.id == ShopCollabLane.ALL_ID) {
                    stringResource(R.string.shop_lane_all_drops_subtitle)
                } else {
                    shopLaneBannerSubtitle(lane)
                },
                accent = MaterialTheme.colorScheme.primary,
                icon = if (lane.id == ShopCollabLane.ALL_ID) Icons.Default.ShoppingBag else Icons.Default.Person,
                tag = laneLabel.uppercase(),
            )

            Text(
                text = focusDetail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ShopLaneMetricCard(
                    title = stringResource(R.string.shop_lane_metric_group_type),
                    value = laneKindLabel,
                    detail = shopLaneBannerSubtitle(lane),
                    accent = MaterialTheme.colorScheme.secondary,
                )
                ShopLaneMetricCard(
                    title = stringResource(R.string.shop_lane_metric_group_coverage),
                    value = coverageLabel,
                    detail = if (lane.itemCount == totalItemCount) {
                        stringResource(R.string.shop_lane_coverage_detail_full)
                    } else {
                        stringResource(R.string.shop_lane_coverage_detail_partial)
                    },
                    accent = MaterialTheme.colorScheme.tertiary,
                )
                ShopLaneMetricCard(
                    title = stringResource(R.string.shop_lane_metric_group_count),
                    value = laneLabel,
                    detail = if (lane.itemCount == totalItemCount) {
                        stringResource(R.string.shop_lane_count_detail_all)
                    } else {
                        stringResource(R.string.shop_lane_count_detail_filter)
                    },
                    accent = MaterialTheme.colorScheme.primary,
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
    val interactionSource = remember { MutableInteractionSource() }
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
            .skydownPressable(interactionSource, pressedScale = 0.984f)
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onTap,
            )
            .height(if (compact) 106.dp else 118.dp)
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
                    text = shopLaneTitle(lane),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    maxLines = if (compact) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = shopLaneBannerSubtitle(lane),
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = if (isSelected) 0.84f else 0.74f),
                    maxLines = if (compact) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ShopLaneMetaChip(
                text = shopPiecesLabel(lane.itemCount),
                isSelected = isSelected,
            )
            ShopLaneMetaChip(
                text = shopLaneKindLabel(lane),
                isSelected = isSelected,
            )
        }
    }
}

@Composable
private fun ShopLaneMetricCard(
    title: String,
    value: String,
    detail: String,
    accent: Color,
) {
    Column(
        modifier = Modifier
            .width(136.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.76f))
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = detail,
            style = MaterialTheme.typography.bodySmall,
            color = accent.copy(alpha = 0.86f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ShopLaneMetaChip(
    text: String,
    isSelected: Boolean,
) {
    Text(
        text = text,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f)
                },
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
    )
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
    val selectionSummary = remember(selectedSize, selectedColor, quantity) {
        buildString {
            append(quantity)
            append("x")
            if (selectedSize.isNotBlank()) {
                append(" • ")
                append(selectedSize)
            }
            if (selectedColor.isNotBlank()) {
                append(" • ")
                append(selectedColor)
            }
        }
    }
    val optionSummary = remember(sizeOptions, colorOptions) {
        buildString {
            append(sizeOptions.size)
            append(" Groessen")
            if (colorOptions.isNotEmpty()) {
                append(" • ")
                append(colorOptions.size)
                append(" Farben")
            }
        }
    }
    val readinessTitle = when {
        item.available && canCheckout && selectedSize.isNotBlank() -> "Bereit"
        !isStoreOpen -> "Store pausiert"
        !item.available -> "Nicht live"
        else -> "Auswahl pruefen"
    }
    val readinessDetail = when {
        item.available && canCheckout && selectedSize.isNotBlank() -> "Direkt in den Warenkorb"
        !isStoreOpen -> "Produkte bleiben sichtbar, neue Kaeufe sind pausiert"
        !item.available -> "Dieses Produkt ist aktuell offline"
        else -> "Bitte Variante vervollstaendigen"
    }
    val addToCartLabel = remember(item.price) {
        "Jetzt sichern • EUR ${String.format(java.util.Locale.US, "%.2f", item.price)}"
    }
    val availabilityTrustLine = when {
        item.available && isStoreOpen -> "Drop aktuell verfuegbar."
        !isStoreOpen -> "Drop sichtbar, Checkout pausiert."
        else -> "Drop aktuell nicht verfuegbar."
    }
    val shippingTrustLine = "Versandstatus und finale Kosten siehst du vor dem Absenden im Checkout."
    val supportTrustLine = "Support ist jederzeit in den Einstellungen erreichbar."

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
                .testTag("shop.merch.detail.root")
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
                        .testTag("shop.merch.detail.close")
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
                        .testTag("shop.merch.fullscreen.open")
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
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Drop Story",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f),
                    )
                }

                BoxWithConstraints {
                    val wideCards = maxWidth >= 520.dp
                    if (wideCards) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            MerchDetailSignalCard(
                                title = readinessTitle,
                                detail = readinessDetail,
                                accent = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f),
                            )
                            MerchDetailSignalCard(
                                title = selectionSummary,
                                detail = optionSummary,
                                accent = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.weight(1f),
                            )
                            MerchDetailSignalCard(
                                title = if (isStoreOpen) "Checkout in der App" else "Store-Ablauf",
                                detail = if (isStoreOpen) "Schliessen, weiterstoebern und spaeter direkt bestellen" else "Der Produktablauf bleibt sichtbar und klar lesbar",
                                accent = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            MerchDetailSignalCard(
                                title = readinessTitle,
                                detail = readinessDetail,
                                accent = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            MerchDetailSignalCard(
                                title = selectionSummary,
                                detail = optionSummary,
                                accent = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            MerchDetailSignalCard(
                                title = if (isStoreOpen) "Checkout in der App" else "Store-Ablauf",
                                detail = if (isStoreOpen) "Schliessen, weiterstoebern und spaeter direkt bestellen" else "Der Produktablauf bleibt sichtbar und klar lesbar",
                                accent = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

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

                MerchDetailTrustModule(
                    availability = availabilityTrustLine,
                    shipping = shippingTrustLine,
                    support = supportTrustLine,
                )

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
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Schliessen")
                }
                BrandActionButton(
                    text = addToCartLabel,
                    onClick = { onAddToCart(selectedSize, selectedColor.takeIf { it.isNotBlank() }, quantity) },
                    accent = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.ShoppingBag,
                    enabled = item.available && canCheckout && selectedSize.isNotBlank(),
                )
            }
            Text(
                text = if (item.available && canCheckout && selectedSize.isNotBlank()) {
                    "Sicher kaufen · $selectionSummary"
                } else {
                    "Vor dem Kauf: Auswahl und Status pruefen"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            )

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
private fun MerchDetailSignalCard(
    title: String,
    detail: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.14f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
                    ),
                ),
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = detail,
            style = MaterialTheme.typography.bodySmall,
            color = accent.copy(alpha = 0.90f),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MerchDetailTrustModule(
    availability: String,
    shipping: String,
    support: String,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f),
                shape = shape,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ShopBadge(
                text = "Trust",
                icon = Icons.Default.CheckCircle,
                isActive = true,
            )
            Text(
                text = "Kaufklarheit",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
        TrustRow(availability)
        TrustRow(shipping)
        TrustRow(support)
    }
}

@Composable
private fun TrustRow(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier
                .size(14.dp)
                .padding(top = 1.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
        )
    }
}

@Composable
private fun FilterPill(
    text: String,
    selected: Boolean,
    onTap: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
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
            .skydownPressable(interactionSource, pressedScale = 0.988f)
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onTap,
            )
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
                .testTag("shop.merch.fullscreen.root")
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
                    .statusBarsPadding()
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

                BrandActionButton(
                    text = "Schliessen",
                    onClick = onDismiss,
                    accent = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("shop.merch.fullscreen.close"),
                    icon = Icons.Default.Close,
                    filled = false,
                    compact = true,
                )
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
