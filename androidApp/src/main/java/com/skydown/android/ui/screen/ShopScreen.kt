package com.skydown.android.ui.screen

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.skydown.android.ui.component.AppTopBarSessionActions
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
    var showAddSheet by rememberSaveable { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<MerchandiseItem?>(null) }
    var itemPendingDelete by remember { mutableStateOf<MerchandiseItem?>(null) }

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
                title = { SkydownTopBarTitle("Skydown Merch", "Merch & Drops.") },
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
        floatingActionButton = {
            if (uiState.isAdmin) {
                FloatingActionButton(
                    onClick = { showAddSheet = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Artikel hinzufuegen",
                    )
                }
            }
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
                    ShopOverviewCard(uiState = uiState)
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

                items(uiState.items, key = { it.id.orEmpty() }) { item ->
                    MerchandiseCard(
                        item = item,
                        onTap = viewModel::selectItem,
                        onEdit = if (uiState.isAdmin) {
                            { editingItem = it }
                        } else {
                            null
                        },
                        onDelete = if (uiState.isAdmin) {
                            { itemPendingDelete = it }
                        } else {
                            null
                        },
                    )
                }
            }

            if (showAddSheet) {
                MerchandiseEditorSheet(
                    isSaving = uiState.isSaving,
                    initialItem = null,
                    onDismiss = { showAddSheet = false },
                    onSave = { name, description, price, available, imageDataList ->
                        viewModel.addItem(
                            name = name,
                            description = description,
                            priceInput = price,
                            available = available,
                            imageDataList = imageDataList,
                        ) {
                            showAddSheet = false
                        }
                    },
                )
            }

            editingItem?.let { item ->
                MerchandiseEditorSheet(
                    isSaving = uiState.isSaving,
                    initialItem = item,
                    onDismiss = { editingItem = null },
                    onSave = { name, description, price, available, imageDataList ->
                        viewModel.updateItem(
                            item = item,
                            name = name,
                            description = description,
                            priceInput = price,
                            available = available,
                            imageDataList = imageDataList,
                        ) {
                            editingItem = null
                        }
                    },
                )
            }

            uiState.selectedItem?.let { item ->
                MerchandiseDetailSheet(
                    item = item,
                    isAdmin = uiState.isAdmin,
                    isSaving = uiState.isSaving,
                    onDismiss = viewModel::dismissSelectedItem,
                    onEdit = {
                        viewModel.dismissSelectedItem()
                        editingItem = item
                    },
                    onDelete = { itemPendingDelete = item },
                )
            }

            itemPendingDelete?.let { item ->
                AlertDialog(
                    onDismissRequest = { itemPendingDelete = null },
                    title = { Text("Artikel loeschen?") },
                    text = {
                        Text("Der Artikel \"${item.name}\" wird dauerhaft entfernt.")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.deleteItem(item)
                                itemPendingDelete = null
                            },
                            shape = RoundedCornerShape(18.dp),
                        ) {
                            Text("Loeschen")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { itemPendingDelete = null }) {
                            Text("Abbrechen")
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
) {
    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "Skydown Merch",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Merch & Drops.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                )
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingBag,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        Row(
            modifier = Modifier.padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ShopBadge(
                text = "${uiState.items.size} Produkte",
                icon = Icons.Default.ShoppingBag,
                isActive = uiState.items.isNotEmpty(),
            )
            ShopBadge(
                text = if (uiState.isLoggedIn) "Konto aktiv" else "Gast",
                icon = if (uiState.isLoggedIn) Icons.Default.CheckCircle else Icons.Default.Sync,
                isActive = uiState.isLoggedIn,
            )
            if (uiState.isAdmin) {
                ShopBadge(
                    text = "Admin",
                    icon = Icons.Default.CheckCircle,
                    isActive = true,
                )
            }
        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MerchandiseEditorSheet(
    isSaving: Boolean,
    initialItem: MerchandiseItem?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Boolean, List<ByteArray>) -> Unit,
) {
    val context = LocalContext.current
    var name by rememberSaveable(initialItem?.id) { mutableStateOf(initialItem?.name.orEmpty()) }
    var description by rememberSaveable(initialItem?.id) { mutableStateOf(initialItem?.description.orEmpty()) }
    var price by rememberSaveable(initialItem?.id) { mutableStateOf(initialItem?.price?.toString().orEmpty()) }
    var available by rememberSaveable(initialItem?.id) { mutableStateOf(initialItem?.available ?: true) }
    var localError by rememberSaveable(initialItem?.id) { mutableStateOf<String?>(null) }
    val selectedUris = remember(initialItem?.id) { mutableStateListOf<Uri>() }
    val existingImageUrls = initialItem?.imageUrls.orEmpty()
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(10),
    ) { uris ->
        selectedUris.clear()
        selectedUris.addAll(uris)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (initialItem == null) "Artikel hinzufuegen" else "Artikel bearbeiten",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Beschreibung") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )
            OutlinedTextField(
                value = price,
                onValueChange = { price = it },
                label = { Text("Preis") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Verfuegbar")
                Switch(
                    checked = available,
                    onCheckedChange = { available = it },
                )
            }

            Button(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                shape = RoundedCornerShape(18.dp),
            ) {
                Text(if (initialItem == null) "Bilder auswaehlen" else "Bilder ersetzen")
            }

            if (selectedUris.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(selectedUris, key = { it.toString() }) { uri ->
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            modifier = Modifier
                                .size(110.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
                if (initialItem != null) {
                    Text(
                        text = "Die neu ausgewaehlten Bilder ersetzen beim Speichern die bestehende Galerie.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
            } else if (existingImageUrls.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(existingImageUrls, key = { it }) { imageUrl ->
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(110.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
            } else {
                Text(
                    text = "Waehle mindestens ein Bild aus dem nativen Android Photo Picker.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }

            localError?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
            ) {
                TextButton(
                    onClick = onDismiss,
                    enabled = !isSaving,
                ) {
                    Text("Abbrechen")
                }
                Button(
                    onClick = {
                        val imageDataList = selectedUris.mapNotNull { uri ->
                            context.readBytes(uri)
                        }
                        if (imageDataList.isEmpty() && existingImageUrls.isEmpty()) {
                            localError = "Bitte mindestens ein Bild auswaehlen."
                        } else {
                            localError = null
                            onSave(name, description, price, available, imageDataList)
                        }
                    },
                    enabled = !isSaving,
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text(if (isSaving) "Speichern ..." else "Speichern")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun MerchandiseDetailSheet(
    item: MerchandiseItem,
    isAdmin: Boolean,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val pagerState = rememberPagerState(pageCount = { item.imageUrls.size.coerceAtLeast(1) })

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
                    modifier = Modifier.fillMaxSize(),
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

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(20.dp),
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
            }

            if (isAdmin) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                ) {
                    TextButton(onClick = onEdit, enabled = !isSaving) {
                        Text("Bearbeiten")
                    }
                    TextButton(onClick = onDelete, enabled = !isSaving) {
                        Text("Loeschen")
                    }
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Text("Schliessen")
                    }
                }
            } else {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text("Schliessen")
                }
            }

            Box(modifier = Modifier.height(2.dp))
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

private fun Context.readBytes(uri: Uri): ByteArray? {
    return runCatching {
        contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.readBytes()
        }
    }.getOrNull()
}
