package com.skydown.android.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.skydown.android.ui.component.AppTopBarSessionActions
import com.skydown.android.ui.component.BrandArtwork
import com.skydown.android.ui.component.BrandHeroCard
import com.skydown.android.ui.component.MerchandiseCard
import com.skydown.android.ui.component.SectionHeader
import com.skydown.android.ui.component.SkydownCard
import com.skydown.android.ui.component.SkydownTopBarTitle
import com.skydown.android.ui.component.ToastHost
import com.skydown.android.ui.component.ToastType
import com.skydown.android.ui.component.skydownContentPadding
import com.skydown.android.ui.component.skydownScreenBrush
import com.skydown.android.ui.component.skydownTopBarColors
import com.skydown.android.ui.model.ShopUiState
import com.skydown.android.ui.viewmodel.ShopViewModel
import com.skydown.shared.model.MerchandiseItem
import com.skydown.shared.usecase.MerchandiseVariantResolver
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(
    onOpenLogin: () -> Unit = {},
    onOpenCart: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    viewModel: ShopViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(uiState.toastMessage) {
        if (!uiState.toastMessage.isNullOrBlank()) {
            delay(3000)
            viewModel.clearToast()
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
                        onOpenSettings = onOpenSettings,
                    ) {
                        IconButton(onClick = viewModel::refresh) {
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    skydownScreenBrush(),
                ),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = skydownContentPadding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    ShopOverviewCard(
                        uiState = uiState,
                        onToggleStore = if (uiState.isAdmin) {
                            viewModel::toggleStoreOpen
                        } else {
                            null
                        },
                        onSyncShopify = if (uiState.isAdmin) {
                            viewModel::syncShopifyCatalog
                        } else {
                            null
                        },
                    )
                }

                val errorMessage = uiState.errorMessage
                if (errorMessage != null && !uiState.isLoggedIn) {
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
                        )
                    }
                }

                if (uiState.items.isEmpty()) {
                    item {
                        ShopMessageCard(
                            title = if (uiState.isSyncingCatalog) "Shopify wird geladen" else "Noch keine Shopify-Produkte",
                            body = if (uiState.isAdmin) {
                                if (uiState.isSyncingCatalog) {
                                    "Der Katalog wird gerade direkt aus Shopify neu aufgebaut."
                                } else {
                                    "Wenn Firestore leer ist, versucht die App den Shopify-Katalog jetzt automatisch neu zu laden."
                                }
                            } else {
                                "Sobald neuer Merch live ist, taucht er hier direkt als Card auf."
                            },
                        )
                    }
                }

                items(uiState.items, key = { it.id.orEmpty() }) { item ->
                    MerchandiseCard(
                        item = item,
                        onTap = viewModel::selectItem,
                    )
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
    onToggleStore: (() -> Unit)? = null,
    onSyncShopify: (() -> Unit)? = null,
) {
    BrandHeroCard(
        eyebrow = "Store",
        title = "Shop",
        subtitle = "Produkte direkt in der App.",
        detail = if (uiState.isStoreOpen) {
            "Offen fuer Bestellungen."
        } else {
            "Ansicht aktiv, Checkout pausiert."
        },
        accent = MaterialTheme.colorScheme.tertiary,
        secondaryAccent = MaterialTheme.colorScheme.secondary,
        marks = listOf(BrandArtwork.Combined),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ShopBadge(
                text = "${uiState.items.size} Produkte",
                icon = Icons.Default.ShoppingBag,
                isActive = uiState.items.isNotEmpty(),
            )
            ShopBadge(
                text = if (uiState.isStoreOpen) "Store offen" else "Store pausiert",
                icon = if (uiState.isStoreOpen) Icons.Default.CheckCircle else Icons.Default.Sync,
                isActive = uiState.isStoreOpen,
            )
            ShopBadge(
                text = if (uiState.isLoggedIn) "Konto aktiv" else "Gast",
                icon = if (uiState.isLoggedIn) Icons.Default.CheckCircle else Icons.Default.Sync,
                isActive = uiState.isLoggedIn,
            )
        }

        if (onToggleStore != null || onSyncShopify != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                onToggleStore?.let { toggleStore ->
                    Button(
                        onClick = toggleStore,
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isUpdatingStoreState,
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Text(
                            if (uiState.isUpdatingStoreState) {
                                "Store wird aktualisiert..."
                            } else if (uiState.isStoreOpen) {
                                "Store schliessen"
                            } else {
                                "Store oeffnen"
                            },
                        )
                    }
                }

                onSyncShopify?.let { syncShopify ->
                    Button(
                        onClick = syncShopify,
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isSyncingCatalog,
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Text(if (uiState.isSyncingCatalog) "Katalog laedt..." else "Shopify syncen")
                    }
                }
            }
        }

        if (uiState.isAdmin) {
            Text(
                text = "Shopify liefert Produkte. Hier steuerst du nur Sichtbarkeit und Reihenfolge.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun ShopMessageCard(
    title: String,
    body: String,
) {
    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
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
        SectionHeader("Anmeldung")
        Text(
            text = errorMessage,
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
        )
        Button(
            onClick = onOpenLogin,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text("Anmelden")
        }
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
    var quantity by rememberSaveable(item.id) { mutableStateOf(1) }

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
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ShopBadge(
                        text = if (item.available) "Drop live" else "Nicht verfuegbar",
                        icon = if (item.available) Icons.Default.CheckCircle else Icons.Default.Sync,
                        isActive = item.available,
                    )
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
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
                Button(
                    onClick = { onAddToCart(selectedSize, selectedColor.takeIf { it.isNotBlank() }, quantity) },
                    modifier = Modifier.weight(1f),
                    enabled = item.available && canCheckout && selectedSize.isNotBlank(),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text("In den Warenkorb")
                }
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
    val backgroundColor = if (isActive) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)
    }
    val contentColor = if (isActive) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
