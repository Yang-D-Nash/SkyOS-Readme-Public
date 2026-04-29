package com.nash.skyos.ui.screen

import androidx.compose.animation.Crossfade
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
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.ExperimentalMaterial3Api
import com.nash.skyos.ui.component.BrandActionButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nash.skyos.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nash.skyos.ui.component.SkydownCard
import com.nash.skyos.ui.component.SkydownUiTokens
import com.nash.skyos.ui.component.skydownContentSizeRevealSpec
import com.nash.skyos.ui.component.skydownCrossfadeSpec
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
                    Column(verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingHairline)) {
                        Text(
                            text = stringResource(R.string.order_topbar_title),
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = stringResource(R.string.order_topbar_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                actions = {
                    BrandActionButton(
                        text = stringResource(R.string.order_action_close),
                        onClick = onClose,
                        accent = MaterialTheme.colorScheme.primary,
                        filled = false,
                        compact = true,
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

                if (uiState.orders.isEmpty()) {
                    item(key = "orders_empty_state") {
                        val emptyPhase = when {
                            uiState.isLoading -> OrdersEmptyPhase.Loading
                            uiState.errorMessage != null -> OrdersEmptyPhase.Error
                            else -> OrdersEmptyPhase.Empty
                        }
                        Crossfade(
                            targetState = emptyPhase,
                            modifier = Modifier.fillMaxWidth(),
                            animationSpec = skydownCrossfadeSpec(),
                            label = "orders_empty_crossfade",
                        ) { phase ->
                            when (phase) {
                                OrdersEmptyPhase.Loading -> OrdersInlineStatusStrip(
                                    icon = Icons.Default.Sync,
                                    title = stringResource(R.string.order_syncing_title),
                                ) {
                                    Text(
                                        text = stringResource(R.string.order_syncing_line1),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = stringResource(R.string.order_syncing_line2),
                                        modifier = Modifier.padding(top = 8.dp),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                                    )
                                }

                                OrdersEmptyPhase.Error -> OrdersInlineStatusStrip(
                                    icon = Icons.Default.Sync,
                                    title = stringResource(R.string.order_unavailable_title),
                                ) {
                                    Text(
                                        text = uiState.errorMessage
                                            ?: stringResource(R.string.order_error_generic),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                                    )
                                    Text(
                                        text = stringResource(R.string.order_error_retry),
                                        modifier = Modifier.padding(top = 8.dp),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    )
                                    BrandActionButton(
                                        text = stringResource(R.string.order_action_reload),
                                        onClick = viewModel::refreshOrders,
                                        accent = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 12.dp),
                                        filled = false,
                                    )
                                }

                                OrdersEmptyPhase.Empty -> OrdersInlineStatusStrip(
                                    icon = Icons.Default.ShoppingBag,
                                    title = stringResource(R.string.order_empty_title),
                                ) {
                                    Text(
                                        text = stringResource(R.string.order_empty_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = stringResource(R.string.order_empty_subtitle),
                                        modifier = Modifier.padding(top = 8.dp),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                                    )
                                }
                            }
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

private enum class OrdersEmptyPhase {
    Loading,
    Error,
    Empty,
}

private fun formatOrderDate(timestampEpochMillis: Long): String {
    return SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(timestampEpochMillis))
}

private fun formatCurrency(value: Double, decimals: Int = 2): String {
    return String.format(Locale.US, "%.${decimals}f", value)
}

@Composable
private fun OrdersOverviewCard(
    orderCount: Int,
    completedOrders: Int,
) {
    val countLabel = pluralStringResource(
        R.plurals.order_pill_order_count,
        orderCount,
        orderCount,
    )
    val openCount = (orderCount - completedOrders).coerceAtLeast(0)
    val shape = RoundedCornerShape(SkydownUiTokens.denseRadius)
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
        verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingSnug),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Default.Inventory2,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = stringResource(R.string.order_overview_title),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = stringResource(R.string.order_overview_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro)) {
            OrderStatusPill(text = countLabel)
            OrderStatusPill(
                text = stringResource(R.string.order_pill_done, completedOrders),
            )
            OrderStatusPill(
                text = stringResource(R.string.order_pill_open, openCount),
            )
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
                RoundedCornerShape(SkydownUiTokens.cardCornerRadius),
            )
            .animateContentSize(animationSpec = skydownContentSizeRevealSpec())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill),
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
    val paymentStatusRaw = order.paymentStatus?.takeIf { it.isNotBlank() }
    val paymentStatus = paymentStatusRaw?.let { localOrderStatusLabel(it) }
    val paymentReference = order.paymentReference?.takeIf { it.isNotBlank() }
    val shippingZone = order.shippingZone?.takeIf { it.isNotBlank() }
    val fulfillmentProvider = order.fulfillmentProvider?.takeIf { it.isNotBlank() }
    val fulfillmentStatus =
        order.fulfillmentStatus?.takeIf { it.isNotBlank() }?.let { localOrderStatusLabel(it) }
    val shopifyOrderId = order.shopifyOrderId?.takeIf { it.isNotBlank() }
    val shopifyOrderName = order.shopifyOrderName?.takeIf { it.isNotBlank() }
    val shopifySyncStatus =
        order.shopifySyncStatus?.takeIf { it.isNotBlank() }?.let { localOrderStatusLabel(it) }
    val stripeCheckoutStatus =
        order.stripeCheckoutStatus?.takeIf { it.isNotBlank() }?.let { localOrderStatusLabel(it) }
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
                verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
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
            horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
        ) {
            OrderStatusPill(
                text = if (order.isCompleted) {
                    stringResource(R.string.order_status_done)
                } else {
                    stringResource(R.string.order_status_open)
                },
                isAccent = order.isCompleted,
            )
            OrderStatusPill(
                text = stringResource(R.string.order_pieces, totalItems),
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
            verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingCompact),
        ) {
            OrderMetaRow(
                label = stringResource(R.string.order_label_contact),
                value = contactEmail,
            )

            whatsApp?.let { value ->
                OrderMetaRow(
                    label = stringResource(R.string.order_label_whatsapp),
                    value = value,
                )
            }

            shippingAddress?.let { value ->
                OrderMetaRow(
                    label = stringResource(R.string.order_label_address),
                    value = value,
                )
            }

            paymentMethod?.let { value ->
                OrderMetaRow(
                    label = stringResource(R.string.order_label_payment_method),
                    value = value,
                )
            }

            paymentProvider?.let { value ->
                OrderMetaRow(
                    label = stringResource(R.string.order_label_payment_provider),
                    value = value,
                )
            }

            paymentStatus?.let { value ->
                OrderMetaRow(
                    label = stringResource(R.string.order_label_payment_status),
                    value = value,
                )
            }

            paymentReference?.let { value ->
                OrderMetaRow(
                    label = stringResource(R.string.order_label_payment_reference),
                    value = value,
                )
            }

            shippingZone?.let { value ->
                OrderMetaRow(
                    label = stringResource(R.string.order_label_shipping_zone),
                    value = value,
                )
            }

            fulfillmentProvider?.let { value ->
                OrderMetaRow(
                    label = stringResource(R.string.order_label_fulfillment),
                    value = value,
                )
            }

            fulfillmentStatus?.let { value ->
                OrderMetaRow(
                    label = stringResource(R.string.order_label_fulfillment_status),
                    value = value,
                )
            }

            shopifyOrderName?.let { value ->
                OrderMetaRow(
                    label = stringResource(R.string.order_label_shopify_order),
                    value = value,
                )
            }

            shopifyOrderId?.let { value ->
                OrderMetaRow(
                    label = stringResource(R.string.order_label_shopify_id),
                    value = value,
                )
            }

            shopifySyncStatus?.let { value ->
                OrderMetaRow(
                    label = stringResource(R.string.order_label_shopify_sync),
                    value = value,
                )
            }

            stripeCheckoutStatus?.let { value ->
                OrderMetaRow(
                    label = stringResource(R.string.order_label_stripe_checkout),
                    value = value,
                )
            }

            stripeCheckoutSessionId?.let { value ->
                OrderMetaRow(
                    label = stringResource(R.string.order_label_checkout_ref),
                    value = value,
                )
            }

            stripePaymentIntentId?.let { value ->
                OrderMetaRow(
                    label = stringResource(R.string.order_label_stripe_pi),
                    value = value,
                )
            }

            if (order.userEmail != contactEmail) {
                OrderMetaRow(
                    label = stringResource(R.string.order_label_account_email),
                    value = order.userEmail,
                )
            }

            message?.let { value ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(SkydownUiTokens.cardCornerRadius),
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
                ) {
                    Text(
                        text = stringResource(R.string.order_message),
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
                            RoundedCornerShape(SkydownUiTokens.cardCornerRadius),
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
                ) {
                    Text(
                        text = stringResource(R.string.order_sum_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                    order.subtotalAmount?.let {
                        OrderMetaRow(
                            label = stringResource(R.string.order_subtotal),
                            value = stringResource(
                                R.string.order_price_eur,
                                formatCurrency(it),
                            ),
                        )
                    }
                    order.shippingAmount?.let {
                        OrderMetaRow(
                            label = stringResource(R.string.order_shipping),
                            value = stringResource(
                                R.string.order_price_eur,
                                formatCurrency(it),
                            ),
                        )
                    }
                    val taxAmount = order.taxAmount
                    val taxRate = order.taxRate
                    if (taxAmount != null && taxRate != null) {
                        OrderMetaRow(
                            label = stringResource(R.string.order_tax_included),
                            value = stringResource(
                                R.string.order_line_tax,
                                formatCurrency(taxAmount),
                                formatCurrency(taxRate, decimals = 1),
                            ),
                        )
                    }
                    order.totalAmount?.let {
                        OrderMetaRow(
                            label = stringResource(R.string.order_total),
                            value = stringResource(
                                R.string.order_price_eur,
                                formatCurrency(it),
                            ),
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier.padding(top = 14.dp),
            verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingCompact),
        ) {
            order.items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingNano),
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                        item.size?.takeIf { it.isNotBlank() }?.let { size ->
                            Text(
                                text = stringResource(R.string.order_size, size),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                            )
                        }
                        item.color?.takeIf { it.isNotBlank() }?.let { color ->
                            Text(
                                text = stringResource(R.string.order_color, color),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                            )
                        }
                    }
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingNano),
                    ) {
                        Text(
                            text = stringResource(R.string.order_item_qty, item.quantity),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                        item.unitPrice?.let { unitPrice ->
                            Text(
                                text = stringResource(
                                    R.string.order_price_eur,
                                    formatCurrency(unitPrice),
                                ),
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
                        verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill),
                    ) {
                        if (!order.paymentStatus.hasFinalPaymentStatus()) {
                            BrandActionButton(
                                text = if (isConfirmingPayment) {
                                    stringResource(R.string.order_action_confirming)
                                } else {
                                    stringResource(R.string.order_action_mark_paid)
                                },
                                onClick = onConfirmPayment,
                                accent = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.fillMaxWidth(),
                                icon = Icons.Default.CreditCard,
                                filled = false,
                                enabled = hasActionableId,
                                isLoading = isConfirmingPayment,
                            )
                        }

                        BrandActionButton(
                            text = if (order.isCompleted) {
                                stringResource(R.string.order_action_reopen)
                            } else {
                                stringResource(R.string.order_action_complete)
                            },
                            onClick = onToggleCompleted,
                            accent = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth(),
                            icon = if (order.isCompleted) Icons.Default.RadioButtonUnchecked else Icons.Default.CheckCircle,
                            enabled = hasActionableId,
                        )

                        BrandActionButton(
                            text = stringResource(R.string.order_action_remove),
                            onClick = onDelete,
                            accent = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth(),
                            filled = false,
                            enabled = hasActionableId,
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingRelaxed),
                    ) {
                        if (!order.paymentStatus.hasFinalPaymentStatus()) {
                            BrandActionButton(
                                text = if (isConfirmingPayment) {
                                    stringResource(R.string.order_action_confirming)
                                } else {
                                    stringResource(R.string.order_action_mark_paid)
                                },
                                onClick = onConfirmPayment,
                                accent = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.CreditCard,
                                filled = false,
                                enabled = hasActionableId,
                                isLoading = isConfirmingPayment,
                            )
                        }

                        BrandActionButton(
                            text = if (order.isCompleted) {
                                stringResource(R.string.order_action_reopen)
                            } else {
                                stringResource(R.string.order_action_complete)
                            },
                            onClick = onToggleCompleted,
                            accent = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                            icon = if (order.isCompleted) Icons.Default.RadioButtonUnchecked else Icons.Default.CheckCircle,
                            enabled = hasActionableId,
                        )
                        BrandActionButton(
                            text = stringResource(R.string.order_action_remove),
                            onClick = onDelete,
                            accent = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                            filled = false,
                            enabled = hasActionableId,
                        )
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
                RoundedCornerShape(SkydownUiTokens.cardCornerRadius),
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingRelaxed),
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
            .background(background, RoundedCornerShape(SkydownUiTokens.fullCapsuleRadius))
            .animateContentSize(animationSpec = skydownContentSizeRevealSpec())
            .padding(horizontal = 13.dp, vertical = 8.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = content,
        )
    }
}

@Composable
@ReadOnlyComposable
private fun localOrderStatusLabel(raw: String): String {
    return when (raw.trim().lowercase()) {
        "pending" -> stringResource(R.string.order_state_pending)
        "open" -> stringResource(R.string.order_state_open)
        "confirmed" -> stringResource(R.string.order_state_confirmed)
        "paid" -> stringResource(R.string.order_state_paid)
        "processing" -> stringResource(R.string.order_state_processing)
        "fulfilled" -> stringResource(R.string.order_state_fulfilled)
        "unfulfilled" -> stringResource(R.string.order_state_unfulfilled)
        "success", "succeeded" -> stringResource(R.string.order_state_succeeded)
        "failed" -> stringResource(R.string.order_state_failed)
        "expired" -> stringResource(R.string.order_state_expired)
        "canceled", "cancelled" -> stringResource(R.string.order_state_canceled)
        else -> raw.replace("_", " ")
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }
}

private fun String?.hasFinalPaymentStatus(): Boolean {
    val normalized = this?.trim()?.lowercase().orEmpty()
    if (normalized.isBlank()) return false
    return normalized in setOf("confirmed", "paid", "success", "succeeded")
}
