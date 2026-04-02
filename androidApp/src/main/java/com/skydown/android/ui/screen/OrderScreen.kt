package com.skydown.android.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skydown.android.ui.component.SkydownCard
import com.skydown.android.ui.component.ToastHost
import com.skydown.android.ui.component.ToastType
import com.skydown.android.ui.component.skydownTopBarColors
import com.skydown.android.ui.viewmodel.OrderViewModel
import com.skydown.shared.model.Order
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderScreen(
    onClose: () -> Unit,
    viewModel: OrderViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val completedOrders = uiState.orders.count { it.isCompleted }

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
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Bestellungen",
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Status, Mengen und Rueckstand klar im Blick.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onClose) {
                        Text("Schliessen")
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
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.05f),
                            MaterialTheme.colorScheme.background,
                        ),
                    ),
                ),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = innerPadding.calculateTopPadding() + com.skydown.android.ui.component.SkydownUiTokens.screenTopPadding,
                    end = 16.dp,
                    bottom = innerPadding.calculateBottomPadding() + 28.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    OrdersOverviewCard(
                        orderCount = uiState.orders.size,
                        completedOrders = completedOrders,
                    )
                }

                if (uiState.isLoading && uiState.orders.isEmpty()) {
                    item {
                        SkydownCard {
                            Text(
                                text = "Bestellungen werden geladen...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "Die aktuelle Uebersicht wird gerade aktualisiert.",
                                modifier = Modifier.padding(top = 8.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            )
                        }
                    }
                } else if (uiState.orders.isEmpty()) {
                    item {
                        SkydownCard {
                            Text(
                                text = "Keine Bestellungen vorhanden",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "Sobald neue Orders eingehen, erscheinen sie hier direkt als Karten.",
                                modifier = Modifier.padding(top = 8.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            )
                        }
                    }
                } else {
                    items(uiState.orders, key = { it.id.orEmpty() }) { order ->
                        OrderCard(
                            order = order,
                            isConfirmingPayment = uiState.confirmingPaymentOrderIds.contains(order.id.orEmpty()),
                            onConfirmPayment = { viewModel.confirmPayment(order.id.orEmpty()) },
                            onToggleCompleted = { viewModel.toggleCompleted(order.id.orEmpty()) },
                            onDelete = { viewModel.deleteOrder(order.id.orEmpty()) },
                        )
                    }
                }
            }

            ToastHost(
                message = uiState.errorMessage ?: uiState.successMessage,
                type = if (uiState.errorMessage != null) ToastType.Error else ToastType.Success,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(
                        top = innerPadding.calculateTopPadding() + com.skydown.android.ui.component.SkydownUiTokens.screenTopPadding,
                    ),
            )
        }
    }
}

private fun formatOrderDate(timestampEpochMillis: Long): String {
    return SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY).format(Date(timestampEpochMillis))
}

private fun formatCurrency(value: Double, decimals: Int = 2): String {
    return String.format(Locale.US, "%.${decimals}f", value)
}

@Composable
private fun OrdersOverviewCard(
    orderCount: Int,
    completedOrders: Int,
) {
    SkydownCard(contentPadding = PaddingValues(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Owner Queue",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Kontakt, Status und Rueckstand liegen direkt auf den Karten, damit du Orders als Owner ohne Umwege pruefen kannst.",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OrderStatusPill(text = "$orderCount Orders")
                OrderStatusPill(text = "$completedOrders erledigt")
                OrderStatusPill(text = "${(orderCount - completedOrders).coerceAtLeast(0)} offen")
            }
        }
    }
}

@Composable
private fun OrderCard(
    order: Order,
    isConfirmingPayment: Boolean,
    onConfirmPayment: () -> Unit,
    onToggleCompleted: () -> Unit,
    onDelete: () -> Unit,
) {
    val title = order.customerName?.takeIf { it.isNotBlank() } ?: order.userEmail
    val contactEmail = order.customerEmail?.takeIf { it.isNotBlank() } ?: order.userEmail
    val whatsApp = order.whatsApp?.takeIf { it.isNotBlank() }
    val shippingAddress = order.shippingAddress?.takeIf { it.isNotBlank() }
    val paymentMethod = order.paymentMethod?.takeIf { it.isNotBlank() }
    val paymentProvider = order.paymentProvider?.takeIf { it.isNotBlank() }
    val paymentStatus = order.paymentStatus?.takeIf { it.isNotBlank() }
    val paymentReference = order.paymentReference?.takeIf { it.isNotBlank() }
    val shippingZone = order.shippingZone?.takeIf { it.isNotBlank() }
    val fulfillmentProvider = order.fulfillmentProvider?.takeIf { it.isNotBlank() }
    val fulfillmentStatus = order.fulfillmentStatus?.takeIf { it.isNotBlank() }
    val shopifyOrderId = order.shopifyOrderId?.takeIf { it.isNotBlank() }
    val shopifyOrderName = order.shopifyOrderName?.takeIf { it.isNotBlank() }
    val shopifySyncStatus = order.shopifySyncStatus?.takeIf { it.isNotBlank() }
    val stripeCheckoutStatus = order.stripeCheckoutStatus?.takeIf { it.isNotBlank() }
    val stripeCheckoutSessionId = order.stripeCheckoutSessionId?.takeIf { it.isNotBlank() }
    val stripePaymentIntentId = order.stripePaymentIntentId?.takeIf { it.isNotBlank() }
    val message = order.message?.takeIf { it.isNotBlank() }
    val totalItems = order.items.sumOf { it.quantity }

    SkydownCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = contactEmail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                )
            }
            Text(
                text = formatOrderDate(order.timestampEpochMillis),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }

        Row(
            modifier = Modifier.padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OrderStatusPill(
                text = if (order.isCompleted) "Erledigt" else "Offen",
                isAccent = order.isCompleted,
            )
            OrderStatusPill(
                text = "$totalItems Teile",
                isAccent = false,
            )
            paymentStatus?.let { status ->
                OrderStatusPill(
                    text = status,
                    isAccent = false,
                )
            }
        }

        Column(
            modifier = Modifier.padding(top = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OrderMetaRow(
                label = "Kontakt",
                value = contactEmail,
            )

            whatsApp?.let { value ->
                OrderMetaRow(
                    label = "WhatsApp",
                    value = value,
                )
            }

            shippingAddress?.let { value ->
                OrderMetaRow(
                    label = "Adresse",
                    value = value,
                )
            }

            paymentMethod?.let { value ->
                OrderMetaRow(
                    label = "Zahlart",
                    value = value,
                )
            }

            paymentProvider?.let { value ->
                OrderMetaRow(
                    label = "Provider",
                    value = value,
                )
            }

            paymentStatus?.let { value ->
                OrderMetaRow(
                    label = "Zahlstatus",
                    value = value,
                )
            }

            paymentReference?.let { value ->
                OrderMetaRow(
                    label = "Zahlreferenz",
                    value = value,
                )
            }

            shippingZone?.let { value ->
                OrderMetaRow(
                    label = "Versandzone",
                    value = value,
                )
            }

            fulfillmentProvider?.let { value ->
                OrderMetaRow(
                    label = "Fulfillment",
                    value = value,
                )
            }

            fulfillmentStatus?.let { value ->
                OrderMetaRow(
                    label = "Fulfillment-Status",
                    value = value,
                )
            }

            shopifyOrderName?.let { value ->
                OrderMetaRow(
                    label = "Shopify Order",
                    value = value,
                )
            }

            shopifyOrderId?.let { value ->
                OrderMetaRow(
                    label = "Shopify ID",
                    value = value,
                )
            }

            shopifySyncStatus?.let { value ->
                OrderMetaRow(
                    label = "Shopify Sync",
                    value = value,
                )
            }

            stripeCheckoutStatus?.let { value ->
                OrderMetaRow(
                    label = "Stripe Checkout",
                    value = value,
                )
            }

            stripeCheckoutSessionId?.let { value ->
                OrderMetaRow(
                    label = "Stripe Session",
                    value = value,
                )
            }

            stripePaymentIntentId?.let { value ->
                OrderMetaRow(
                    label = "Stripe Payment",
                    value = value,
                )
            }

            if (order.userEmail != contactEmail) {
                OrderMetaRow(
                    label = "Login-Mail",
                    value = order.userEmail,
                )
            }

            message?.let { value ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(18.dp),
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "Nachricht",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            if (
                order.subtotalAmount != null ||
                order.shippingAmount != null ||
                order.taxAmount != null ||
                order.totalAmount != null
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(18.dp),
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Bestellsumme",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                    order.subtotalAmount?.let { OrderMetaRow(label = "Zwischensumme", value = "EUR ${formatCurrency(it)}") }
                    order.shippingAmount?.let { OrderMetaRow(label = "Versand", value = "EUR ${formatCurrency(it)}") }
                    val taxAmount = order.taxAmount
                    val taxRate = order.taxRate
                    if (taxAmount != null && taxRate != null) {
                        OrderMetaRow(
                            label = "inkl. MwSt.",
                            value = "EUR ${formatCurrency(taxAmount)} bei ${formatCurrency(taxRate, decimals = 1)}%",
                        )
                    }
                    order.totalAmount?.let { OrderMetaRow(label = "Gesamt", value = "EUR ${formatCurrency(it)}") }
                }
            }
        }

        Column(
            modifier = Modifier.padding(top = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            order.items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                        item.size?.takeIf { it.isNotBlank() }?.let { size ->
                            Text(
                                text = "Groesse $size",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                            )
                        }
                        item.color?.takeIf { it.isNotBlank() }?.let { color ->
                            Text(
                                text = "Farbe $color",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                            )
                        }
                    }
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "x${item.quantity}",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                        item.unitPrice?.let { unitPrice ->
                            Text(
                                text = "EUR ${formatCurrency(unitPrice)}",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (paymentStatus != "confirmed") {
                FilledTonalButton(
                    onClick = onConfirmPayment,
                    modifier = Modifier.weight(1f),
                    enabled = !isConfirmingPayment,
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text(
                        text = if (isConfirmingPayment) "Bestaetige..." else "Zahlung bestaetigen",
                    )
                }
            }

            FilledTonalButton(
                onClick = onToggleCompleted,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp),
            ) {
                androidx.compose.material3.Icon(
                    imageVector = if (order.isCompleted) Icons.Default.RadioButtonUnchecked else Icons.Default.CheckCircle,
                    contentDescription = null,
                )
                Text(
                    text = if (order.isCompleted) "Wieder oeffnen" else "Als erledigt markieren",
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            TextButton(
                onClick = onDelete,
                modifier = Modifier.weight(1f),
            ) {
                Text("Loeschen")
            }
        }
    }
}

@Composable
private fun OrderMetaRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                RoundedCornerShape(18.dp),
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun OrderStatusPill(
    text: String,
    isAccent: Boolean = true,
) {
    val background = if (isAccent) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f)
    }
    val content = if (isAccent) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
    }

    Box(
        modifier = Modifier
            .background(background, RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = content,
        )
    }
}
