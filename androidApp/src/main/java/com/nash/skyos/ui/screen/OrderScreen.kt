package com.nash.skyos.ui.screen

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nash.skyos.ui.component.SkydownCard
import com.nash.skyos.ui.component.ToastHost
import com.nash.skyos.ui.component.ToastType
import com.nash.skyos.ui.component.rememberSkydownScreenSectionSpacing
import com.nash.skyos.ui.component.skydownContentPadding
import com.nash.skyos.ui.component.skydownTopBarColors
import com.nash.skyos.ui.viewmodel.OrderViewModel
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
    val sectionSpacing = rememberSkydownScreenSectionSpacing()

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
                contentPadding = skydownContentPadding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(sectionSpacing),
            ) {
                item {
                    OrdersOverviewCard(
                        orderCount = uiState.orders.size,
                        completedOrders = completedOrders,
                    )
                }

                if (uiState.isLoading && uiState.orders.isEmpty()) {
                    item {
                        OrdersInlineStatusStrip(
                            icon = Icons.Default.Sync,
                            title = "Synchronisierung laeuft",
                        ) {
                            Text(
                                text = "Bestellungen werden aktualisiert...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "Die Uebersicht wird sicher aktualisiert.",
                                modifier = Modifier.padding(top = 8.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            )
                        }
                    }
                } else if (uiState.errorMessage != null && uiState.orders.isEmpty()) {
                    item {
                        OrdersInlineStatusStrip(
                            icon = Icons.Default.Sync,
                            title = "Bestellungen gerade nicht verfuegbar",
                        ) {
                            Text(
                                text = uiState.errorMessage ?: "Bestellungen konnten nicht geladen werden.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                            )
                            Text(
                                text = "Bitte kurz erneut laden.",
                                modifier = Modifier.padding(top = 8.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            )
                            OutlinedButton(
                                onClick = viewModel::refreshOrders,
                                modifier = Modifier.padding(top = 12.dp),
                                shape = RoundedCornerShape(16.dp),
                            ) {
                                Text("Erneut laden")
                            }
                        }
                    }
                } else if (uiState.orders.isEmpty()) {
                    item {
                        OrdersInlineStatusStrip(
                            icon = Icons.Default.ShoppingBag,
                            title = "Noch keine Bestellung",
                        ) {
                            Text(
                                text = "Noch keine Bestellung",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "Neue Bestellungen erscheinen hier automatisch mit aktuellem Status.",
                                modifier = Modifier.padding(top = 8.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            )
                        }
                    }
                } else {
                    itemsIndexed(
                        items = uiState.orders,
                        key = { index, order -> order.id ?: "order-fallback-$index" },
                    ) { _, order ->
                        val hasOrderId = !order.id.isNullOrBlank()
                        OrderCard(
                            order = order,
                            isConfirmingPayment = uiState.confirmingPaymentOrderIds.contains(order.id.orEmpty()),
                            hasActionableId = hasOrderId,
                            canManageOrders = uiState.canManageOrders,
                            onConfirmPayment = {
                                order.id?.let(viewModel::confirmPayment)
                            },
                            onToggleCompleted = {
                                order.id?.let(viewModel::toggleCompleted)
                            },
                            onDelete = {
                                order.id?.let(viewModel::deleteOrder)
                            },
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
                        top = innerPadding.calculateTopPadding() + com.nash.skyos.ui.component.SkydownUiTokens.screenTopPadding,
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
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                shape = shape,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Default.Inventory2,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = "Bestelluebersicht",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = "Status und Rueckstand liegen kompakt bereit, damit du Auftraege schneller pruefst.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OrderStatusPill(text = if (orderCount == 1) "1 Bestellung" else "$orderCount Bestellungen")
            OrderStatusPill(text = "$completedOrders erledigt")
            OrderStatusPill(text = "${(orderCount - completedOrders).coerceAtLeast(0)} offen")
        }
    }
}

@Composable
private fun OrdersInlineStatusStrip(
    icon: ImageVector,
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                RoundedCornerShape(20.dp),
            )
            .animateContentSize()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.material3.Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
        content()
    }
}

@Composable
private fun OrderCard(
    order: Order,
    isConfirmingPayment: Boolean,
    hasActionableId: Boolean,
    canManageOrders: Boolean,
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
    val paymentStatus = order.paymentStatus?.takeIf { it.isNotBlank() }?.asUserFacingOrderStatus()
    val paymentReference = order.paymentReference?.takeIf { it.isNotBlank() }
    val shippingZone = order.shippingZone?.takeIf { it.isNotBlank() }
    val fulfillmentProvider = order.fulfillmentProvider?.takeIf { it.isNotBlank() }
    val fulfillmentStatus = order.fulfillmentStatus?.takeIf { it.isNotBlank() }?.asUserFacingOrderStatus()
    val shopifyOrderId = order.shopifyOrderId?.takeIf { it.isNotBlank() }
    val shopifyOrderName = order.shopifyOrderName?.takeIf { it.isNotBlank() }
    val shopifySyncStatus = order.shopifySyncStatus?.takeIf { it.isNotBlank() }?.asUserFacingOrderStatus()
    val stripeCheckoutStatus = order.stripeCheckoutStatus?.takeIf { it.isNotBlank() }?.asUserFacingOrderStatus()
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
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
                    label = "Zahlanbieter",
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
                    label = "Versanddienst",
                    value = value,
                )
            }

            fulfillmentStatus?.let { value ->
                OrderMetaRow(
                    label = "Versandstatus",
                    value = value,
                )
            }

            shopifyOrderName?.let { value ->
                OrderMetaRow(
                    label = "Shop-Bestellnummer",
                    value = value,
                )
            }

            shopifyOrderId?.let { value ->
                OrderMetaRow(
                    label = "Shop-Referenz",
                    value = value,
                )
            }

            shopifySyncStatus?.let { value ->
                OrderMetaRow(
                    label = "Shop-Synchronisierung",
                    value = value,
                )
            }

            stripeCheckoutStatus?.let { value ->
                OrderMetaRow(
                    label = "Checkout-Status",
                    value = value,
                )
            }

            stripeCheckoutSessionId?.let { value ->
                OrderMetaRow(
                    label = "Checkout-Referenz",
                    value = value,
                )
            }

            stripePaymentIntentId?.let { value ->
                OrderMetaRow(
                    label = "Zahlungsreferenz",
                    value = value,
                )
            }

            if (order.userEmail != contactEmail) {
                OrderMetaRow(
                    label = "Kontomail",
                    value = order.userEmail,
                )
            }

            message?.let { value ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(20.dp),
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
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
                            RoundedCornerShape(20.dp),
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
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

        if (canManageOrders) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),
            ) {
                val compactActions = maxWidth < 460.dp

                if (compactActions) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (!order.paymentStatus.hasFinalPaymentStatus()) {
                            FilledTonalButton(
                                onClick = onConfirmPayment,
                                modifier = Modifier.fillMaxWidth(),
                                enabled = hasActionableId && !isConfirmingPayment,
                                shape = RoundedCornerShape(20.dp),
                            ) {
                                Text(
                                    text = if (isConfirmingPayment) "Wird bestaetigt..." else "Zahlung als eingegangen markieren",
                                )
                            }
                        }

                        FilledTonalButton(
                            onClick = onToggleCompleted,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = hasActionableId,
                            shape = RoundedCornerShape(20.dp),
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = if (order.isCompleted) Icons.Default.RadioButtonUnchecked else Icons.Default.CheckCircle,
                                contentDescription = null,
                            )
                            Text(
                                text = if (order.isCompleted) "Bestellung wieder oeffnen" else "Bestellung als erledigt markieren",
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }

                        TextButton(
                            onClick = onDelete,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = hasActionableId,
                        ) {
                            Text("Entfernen")
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        if (!order.paymentStatus.hasFinalPaymentStatus()) {
                            FilledTonalButton(
                                onClick = onConfirmPayment,
                                modifier = Modifier.weight(1f),
                                enabled = hasActionableId && !isConfirmingPayment,
                                shape = RoundedCornerShape(20.dp),
                            ) {
                                Text(
                                    text = if (isConfirmingPayment) "Wird bestaetigt..." else "Zahlung als eingegangen markieren",
                                )
                            }
                        }

                        FilledTonalButton(
                            onClick = onToggleCompleted,
                            modifier = Modifier.weight(1f),
                            enabled = hasActionableId,
                            shape = RoundedCornerShape(20.dp),
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = if (order.isCompleted) Icons.Default.RadioButtonUnchecked else Icons.Default.CheckCircle,
                                contentDescription = null,
                            )
                            Text(
                                text = if (order.isCompleted) "Bestellung wieder oeffnen" else "Bestellung als erledigt markieren",
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                        TextButton(
                            onClick = onDelete,
                            modifier = Modifier.weight(1f),
                            enabled = hasActionableId,
                        ) {
                            Text("Entfernen")
                        }
                    }
                }
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
                RoundedCornerShape(20.dp),
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
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
            .animateContentSize()
            .padding(horizontal = 13.dp, vertical = 8.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = content,
        )
    }
}

private fun String.asUserFacingOrderStatus(): String {
    return when (trim().lowercase()) {
        "pending" -> "In Klaerung"
        "open" -> "Offen"
        "confirmed" -> "Bestaetigt"
        "paid" -> "Bezahlt"
        "processing" -> "In Bearbeitung"
        "fulfilled" -> "Versendet"
        "unfulfilled" -> "Nicht versendet"
        "success", "succeeded" -> "Abgeschlossen"
        "failed" -> "Nicht erfolgreich"
        "expired" -> "Abgelaufen"
        "canceled", "cancelled" -> "Storniert"
        else -> replace("_", " ").replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }
}

private fun String?.hasFinalPaymentStatus(): Boolean {
    val normalized = this?.trim()?.lowercase().orEmpty()
    if (normalized.isBlank()) return false
    return normalized in setOf("confirmed", "paid", "success", "succeeded")
}
