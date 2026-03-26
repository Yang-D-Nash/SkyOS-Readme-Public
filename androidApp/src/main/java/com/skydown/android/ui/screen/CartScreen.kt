package com.skydown.android.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skydown.android.ui.component.SectionHeader
import com.skydown.android.ui.component.SkydownCard
import com.skydown.android.ui.component.ToastHost
import com.skydown.android.ui.component.ToastType
import com.skydown.android.ui.viewmodel.CartViewModel
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    onOpenLogin: () -> Unit = {},
    viewModel: CartViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val appContext = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val totalPrice = uiState.items.sumOf { it.item.price * it.quantity }

    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        if (uiState.errorMessage != null || uiState.successMessage != null) {
            delay(3_000)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Warenkorb",
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Artikel, Kontakt und Checkout in einem klaren Flow.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.94f),
                    scrolledContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.98f),
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f),
                            MaterialTheme.colorScheme.background,
                        ),
                    ),
                ),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    end = 16.dp,
                    bottom = innerPadding.calculateBottomPadding() + 28.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    CartOverviewCard(
                        itemCount = uiState.items.size,
                        totalPrice = totalPrice,
                        isLoggedIn = uiState.isLoggedIn,
                    )
                }

                if (!uiState.isLoggedIn) {
                    item {
                        SkydownCard {
                            SectionHeader("Konto fehlt")
                            Text(
                                text = "Melde dich an, damit du deinen Warenkorb sichern und eine Bestellung abschicken kannst.",
                                modifier = Modifier.padding(top = 8.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                            )
                            Button(
                                onClick = onOpenLogin,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 14.dp),
                                shape = RoundedCornerShape(18.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Login,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = "Anmelden",
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                        }
                    }
                } else {
                    item {
                        SkydownCard {
                            SectionHeader("Deine Auswahl")
                            Text(
                                text = if (uiState.items.isEmpty()) {
                                    "Noch keine Artikel im Warenkorb. Sobald du etwas hinzufuegst, erscheint hier direkt deine Auswahl."
                                } else {
                                    "Pruefe Mengen, Groessen und Preise, bevor du die Bestellung abschickst."
                                },
                                modifier = Modifier.padding(top = 8.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            )
                        }
                    }

                    items(uiState.items, key = { "${it.item.id}-${it.size}" }) { cartItem ->
                        CartItemCard(
                            name = cartItem.item.name,
                            size = cartItem.size,
                            quantity = cartItem.quantity,
                            price = cartItem.item.price * cartItem.quantity,
                            onRemove = { viewModel.removeItem(cartItem.item.id.orEmpty(), cartItem.size) },
                        )
                    }

                    item {
                        SkydownCard {
                            SectionHeader("Kontaktdaten")
                            Text(
                                text = "Diese Angaben helfen uns beim Rueckkontakt zu deiner Bestellung.",
                                modifier = Modifier.padding(top = 8.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            )
                            OutlinedTextField(
                                value = uiState.name,
                                onValueChange = viewModel::updateName,
                                label = { Text("Name*") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 14.dp),
                                singleLine = true,
                                shape = RoundedCornerShape(18.dp),
                            )
                            OutlinedTextField(
                                value = uiState.email,
                                onValueChange = viewModel::updateEmail,
                                label = { Text("E-Mail*") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                singleLine = true,
                                shape = RoundedCornerShape(18.dp),
                            )
                            OutlinedTextField(
                                value = uiState.whatsApp,
                                onValueChange = viewModel::updateWhatsApp,
                                label = { Text("WhatsApp (optional)") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                singleLine = true,
                                shape = RoundedCornerShape(18.dp),
                            )
                        }
                    }

                    item {
                        SkydownCard {
                            SectionHeader("Nachricht")
                            Text(
                                text = "Optional fuer Hinweise zu Lieferung, Verfuegbarkeit oder Sonderwuenschen.",
                                modifier = Modifier.padding(top = 8.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            )
                            OutlinedTextField(
                                value = uiState.message,
                                onValueChange = viewModel::updateMessage,
                                label = { Text("Nachricht") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 14.dp),
                                minLines = 4,
                                shape = RoundedCornerShape(18.dp),
                            )
                        }
                    }

                    item {
                        SkydownCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "Checkout",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = "Wir melden uns danach per E-Mail oder WhatsApp.",
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    )
                                }
                                CartInfoPill(text = "${uiState.items.size} Artikel")
                            }

                            Button(
                                onClick = {
                                    val orderSnapshot = uiState
                                    coroutineScope.launch {
                                        val result = viewModel.submitOrder()
                                        if (result.isSuccess) {
                                            openOrderEmail(appContext, orderSnapshot)
                                        }
                                    }
                                },
                                enabled = viewModel.isFormValid() && !uiState.isSubmitting,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 14.dp),
                                shape = RoundedCornerShape(18.dp),
                            ) {
                                Text(if (uiState.isSubmitting) "Sende Bestellung..." else "Bestellung abschicken")
                            }

                            if (uiState.items.isEmpty()) {
                                OutlinedButton(
                                    onClick = {},
                                    enabled = false,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp),
                                    shape = RoundedCornerShape(18.dp),
                                ) {
                                    Text("Fuege zuerst Artikel hinzu")
                                }
                            }
                        }
                    }
                }
            }

            ToastHost(
                message = uiState.errorMessage ?: uiState.successMessage,
                type = if (uiState.errorMessage != null) ToastType.Error else ToastType.Success,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = innerPadding.calculateTopPadding() + 8.dp),
            )
        }
    }
}

@Composable
private fun CartOverviewCard(
    itemCount: Int,
    totalPrice: Double,
    isLoggedIn: Boolean,
) {
    SkydownCard(contentPadding = PaddingValues(20.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Bereit zum Checkout",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Der Warenkorb folgt jetzt derselben klaren Material-3-Struktur wie Music, Shop und Settings.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CartInfoPill(text = "$itemCount Artikel")
                    CartInfoPill(text = if (isLoggedIn) "Konto aktiv" else "Gast")
                    if (itemCount > 0) {
                        CartInfoPill(text = "EUR ${formatCurrency(totalPrice)}")
                    }
                }
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .padding(14.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingBag,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun CartItemCard(
    name: String,
    size: String,
    quantity: Int,
    price: Double,
    onRemove: () -> Unit,
) {
    SkydownCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CartInfoPill(text = "Groesse $size")
                    CartInfoPill(text = "x$quantity")
                }
            }
            Text(
                text = "EUR ${formatCurrency(price)}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        FilledTonalButton(
            onClick = onRemove,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = "Entfernen",
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun CartInfoPill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

private fun formatCurrency(value: Double): String {
    return String.format(Locale.US, "%.2f", value)
}

private fun openOrderEmail(
    context: Context,
    state: com.skydown.android.ui.model.CartUiState,
) {
    val subject = if (state.email.isNotBlank()) {
        "Neue Bestellung - ${state.email}"
    } else {
        "Neue Bestellung"
    }
    val itemSummary = if (state.items.isEmpty()) {
        "- Keine Artikel"
    } else {
        state.items.joinToString(separator = "\n") { cartItem ->
            val price = cartItem.item.price * cartItem.quantity
            "- ${cartItem.item.name} | Groesse: ${cartItem.size} | Menge: ${cartItem.quantity} | Preis: EUR ${formatCurrency(price)}"
        }
    }
    val total = state.items.sumOf { cartItem -> cartItem.item.price * cartItem.quantity }
    val body = """
        Hallo Skydown-Team,

        es wurde eine neue Bestellung in der Skydown App vorbereitet.

        Name: ${state.name.ifBlank { "Nicht angegeben" }}
        E-Mail: ${state.email.ifBlank { "Nicht angegeben" }}
        WhatsApp: ${state.whatsApp.ifBlank { "Nicht angegeben" }}

        Warenkorb:
        $itemSummary

        Gesamt: EUR ${formatCurrency(total)}

        Nachricht:
        ${state.message.ifBlank { "Keine zusaetzliche Nachricht." }}
    """.trimIndent()
    val uri = Uri.parse("mailto:skydownent@gmail.com")
        .buildUpon()
        .appendQueryParameter("subject", subject)
        .appendQueryParameter("body", body)
        .build()
    val intent = Intent(Intent.ACTION_SENDTO, uri)
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    }
}
