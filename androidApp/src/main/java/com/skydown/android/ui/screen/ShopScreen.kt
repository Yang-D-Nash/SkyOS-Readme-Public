package com.skydown.android.ui.screen

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.skydown.android.ui.component.MerchandiseCard
import com.skydown.android.ui.component.SectionHeader
import com.skydown.android.ui.component.SkydownCard
import com.skydown.android.ui.component.ToastHost
import com.skydown.android.ui.component.ToastType
import com.skydown.android.ui.viewmodel.ShopViewModel
import com.skydown.shared.model.MerchandiseItem
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(
    onOpenLogin: () -> Unit = {},
    viewModel: ShopViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.toastMessage) {
        if (!uiState.toastMessage.isNullOrBlank()) {
            delay(3000)
            viewModel.clearToast()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                ShopHeader()
            }

            if (uiState.isAdmin) {
                item {
                    AdminSection(onAddClick = { showAddSheet = true })
                }
            }

            val errorMessage = uiState.errorMessage
            if (errorMessage != null && !uiState.isLoggedIn) {
                item {
                    LoginSection(
                        errorMessage = errorMessage,
                        onOpenLogin = onOpenLogin
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

        if (showAddSheet) {
            AddMerchandiseSheet(
                isSaving = uiState.isSaving,
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

        uiState.selectedItem?.let { item ->
            MerchandiseDetailSheet(
                item = item,
                isAdmin = uiState.isAdmin,
                isSaving = uiState.isSaving,
                onDismiss = viewModel::dismissSelectedItem,
                onUpdatePrice = { newPrice -> viewModel.updatePrice(item, newPrice) },
                onDelete = { viewModel.deleteItem(item) },
            )
        }

        ToastHost(
            message = uiState.toastMessage ?: uiState.errorMessage,
            type = if (uiState.isErrorToast || uiState.errorMessage != null) ToastType.Error else ToastType.Success,
            modifier = Modifier.padding(bottom = 12.dp),
        )
    }
}

@Composable
private fun ShopHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Skydown Merch",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Dieselbe Shop-Struktur wie auf iOS: grosse Produktkarten, Galerie und direkter Zugriff auf den Artikel.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun AdminSection(onAddClick: () -> Unit) {
    SkydownCard {
        SectionHeader("Admin")
        Text(
            text = "Artikel koennen hier direkt angelegt werden. Bilder kommen ueber den nativen Android Photo Picker.",
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
        )
        Button(
            onClick = onAddClick,
            modifier = Modifier.padding(top = 12.dp),
        ) {
            Text("Artikel hinzufuegen")
        }
    }
}

@Composable
private fun LoginSection(errorMessage: String, onOpenLogin: () -> Unit) {
    SkydownCard {
        SectionHeader("Anmeldung")
        Text(
            text = errorMessage,
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
        )
        Button(
            onClick = onOpenLogin,
            modifier = Modifier.padding(top = 12.dp),
        ) {
            Text("Anmelden")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMerchandiseSheet(
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Boolean, List<ByteArray>) -> Unit,
) {
    val context = LocalContext.current
    var name by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var available by rememberSaveable { mutableStateOf(true) }
    var localError by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedUris = remember { mutableStateListOf<Uri>() }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(10),
    ) { uris ->
        selectedUris.clear()
        selectedUris.addAll(uris)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Artikel hinzufuegen",
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
            ) {
                Text("Bilder auswaehlen")
            }

            if (selectedUris.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
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
                horizontalArrangement = Arrangement.End,
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
                        if (imageDataList.isEmpty()) {
                            localError = "Bitte mindestens ein Bild auswaehlen."
                        } else {
                            localError = null
                            onSave(name, description, price, available, imageDataList)
                        }
                    },
                    enabled = !isSaving,
                ) {
                    Text(if (isSaving) "Speichern ..." else "Speichern")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MerchandiseDetailSheet(
    item: MerchandiseItem,
    isAdmin: Boolean,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onUpdatePrice: (String) -> Unit,
    onDelete: () -> Unit,
) {
    var editedPrice by remember(item.id) { mutableStateOf(item.price.toString()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = item.description,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            )

            if (item.imageUrls.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(item.imageUrls, key = { it }) { imageUrl ->
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = item.name,
                            modifier = Modifier
                                .width(180.dp)
                                .height(180.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
            }

            if (isAdmin) {
                HorizontalDivider()
                OutlinedTextField(
                    value = editedPrice,
                    onValueChange = { editedPrice = it },
                    label = { Text("Preis") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = onDelete,
                        enabled = !isSaving,
                    ) {
                        Text("Loeschen")
                    }
                    Button(
                        onClick = { onUpdatePrice(editedPrice) },
                        enabled = !isSaving,
                    ) {
                        Text("Preis speichern")
                    }
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Schliessen")
                }
            }
        }
    }
}

private fun Context.readBytes(uri: Uri): ByteArray? {
    return runCatching {
        contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.readBytes()
        }
    }.getOrNull()
}
