package com.nash.skyos.ui.screen

import android.content.Context
import android.content.res.Configuration
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nash.skyos.R
import com.nash.skyos.ui.component.AppTopBarSessionActions
import com.nash.skyos.ui.component.SectionHeader
import com.nash.skyos.ui.component.BrandActionButton
import com.nash.skyos.ui.component.SkydownCard
import com.nash.skyos.ui.component.SkydownTopBarTitle
import com.nash.skyos.ui.component.SkydownUiTokens
import com.nash.skyos.ui.component.skydownContentSizeRevealSpec
import com.nash.skyos.ui.component.ToastHost
import com.nash.skyos.ui.component.ToastType
import com.nash.skyos.ui.component.rememberSkydownScreenSectionSpacing
import com.nash.skyos.ui.component.skydownContentPadding
import com.nash.skyos.ui.component.skydownTopBarColors
import com.nash.skyos.data.CheckoutRedirectStore
import com.nash.skyos.data.ShippingService
import com.nash.skyos.ui.viewmodel.CartViewModel
import com.skydown.shared.model.PlatformContactEmails
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    onBack: (() -> Unit)? = null,
    onOpenLogin: () -> Unit = {},
    onGuestSignIn: (() -> Unit)? = null,
    onOpenProfile: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    viewModel: CartViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val checkoutRedirectEvent by CheckoutRedirectStore.latestEvent.collectAsStateWithLifecycle()
    val appContext = LocalContext.current
    val configuration = LocalConfiguration.current
    val coroutineScope = rememberCoroutineScope()
    val defaultCartMessage = rememberSaveable { com.nash.skyos.ui.model.CartUiState().message }
    var showOptionalContactFields by rememberSaveable { mutableStateOf(false) }
    var showOptionalAddressFields by rememberSaveable { mutableStateOf(false) }
    var showOptionalMessageField by rememberSaveable { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val pricing = cartPricingSummary(uiState)
    val hasOrderItems = uiState.items.isNotEmpty()
    val isZeroCostOrder = hasOrderItems && pricing.total <= 0.01
    val checkoutReadinessTitle = when {
        uiState.isSubmitting -> stringResource(R.string.cart_readiness_preparing)
        viewModel.isFormValid() && (uiState.isStoreOpen || uiState.isAdmin) -> stringResource(R.string.cart_readiness_ready)
        !uiState.isLoggedIn -> stringResource(R.string.cart_readiness_sign_in_missing)
        !(uiState.isStoreOpen || uiState.isAdmin) -> stringResource(R.string.cart_readiness_paused)
        uiState.paymentMethods.checkoutMethodLabels.isNotEmpty() && uiState.selectedPaymentMethod.isBlank() -> stringResource(R.string.cart_readiness_payment_open)
        !viewModel.isFormValid() -> stringResource(R.string.cart_readiness_fields_open)
        else -> stringResource(R.string.cart_readiness_check)
    }
    val checkoutReadinessDetail = when {
        uiState.isSubmitting -> stringResource(R.string.cart_readiness_detail_preparing)
        viewModel.isFormValid() && (uiState.isStoreOpen || uiState.isAdmin) -> stringResource(R.string.cart_readiness_detail_ready)
        !uiState.isLoggedIn -> stringResource(R.string.cart_readiness_detail_sign_in)
        !(uiState.isStoreOpen || uiState.isAdmin) -> stringResource(R.string.cart_readiness_detail_paused)
        uiState.paymentMethods.checkoutMethodLabels.isNotEmpty() && uiState.selectedPaymentMethod.isBlank() -> stringResource(R.string.cart_readiness_detail_payment)
        !viewModel.isFormValid() -> stringResource(R.string.cart_readiness_detail_fields)
        else -> stringResource(R.string.cart_readiness_detail_check)
    }
    val checkoutPaymentTitle = uiState.selectedPaymentMethod.ifBlank {
        if (uiState.paymentMethods.checkoutMethodLabels.isEmpty()) stringResource(R.string.cart_payment_callback) else stringResource(R.string.cart_payment_choose)
    }
    val checkoutPaymentDetail = paymentRouteDetail(
        uiState.selectedPaymentMethod.ifBlank { stringResource(R.string.cart_payment_callback) },
    )
    val shippingZoneLabel = when (pricing.zoneLabel) {
        "pending" -> stringResource(R.string.cart_shipping_zone_pending)
        "check-country" -> stringResource(R.string.cart_shipping_zone_check_country)
        else -> pricing.zoneLabel
    }
    val checkoutTotalTitle = if (hasOrderItems) {
        stringResource(R.string.order_price_eur, formatCurrency(pricing.total))
    } else {
        stringResource(R.string.cart_total_empty)
    }
    val checkoutTotalDetail = when {
        !hasOrderItems -> stringResource(R.string.cart_total_detail_empty)
        isZeroCostOrder -> stringResource(R.string.cart_total_detail_zero_payment, shippingZoneLabel)
        else -> stringResource(R.string.cart_total_detail_shipping_included, shippingZoneLabel)
    }
    val sectionSpacing = rememberSkydownScreenSectionSpacing()
    val prefersCompactPulseLayout = configuration.screenWidthDp < 420 ||
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        if (uiState.errorMessage != null || uiState.successMessage != null) {
            delay(3_000)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(checkoutRedirectEvent) {
        val event = checkoutRedirectEvent ?: return@LaunchedEffect
        viewModel.handleCheckoutRedirect(event.status)
        CheckoutRedirectStore.clear()
    }

    LaunchedEffect(uiState.whatsApp) {
        if (uiState.whatsApp.isNotBlank()) {
            showOptionalContactFields = true
        }
    }

    LaunchedEffect(uiState.shippingAddressExtra) {
        if (uiState.shippingAddressExtra.isNotBlank()) {
            showOptionalAddressFields = true
        }
    }

    LaunchedEffect(uiState.message) {
        if (uiState.message.isNotBlank() &&
            uiState.message != defaultCartMessage
        ) {
            showOptionalMessageField = true
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    SkydownTopBarTitle(
                        title = stringResource(R.string.cart_title),
                        subtitle = stringResource(R.string.cart_subtitle),
                    )
                },
                navigationIcon = if (onBack != null) {
                    {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.common_back),
                            )
                        }
                    }
                } else {
                    {}
                },
                actions = {
                    AppTopBarSessionActions(
                        onOpenProfile = onOpenProfile,
                        onOpenSettings = onOpenSettings,
                        onGuestSignIn = onGuestSignIn,
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
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f),
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
                    CartOverviewCard(
                        itemCount = uiState.items.size,
                        totalPrice = pricing.total,
                        isLoggedIn = uiState.isLoggedIn,
                    )
                }

                item {
                    CheckoutPulseCard(
                        readinessTitle = checkoutReadinessTitle,
                        readinessDetail = checkoutReadinessDetail,
                        paymentTitle = checkoutPaymentTitle,
                        paymentDetail = checkoutPaymentDetail,
                        totalTitle = checkoutTotalTitle,
                        totalDetail = checkoutTotalDetail,
                        compactLayout = prefersCompactPulseLayout,
                    )
                }

                item {
                    PaymentMethodAvailabilityCard(
                        methods = uiState.paymentMethods.checkoutMethodLabels,
                        bankTransferEnabled = uiState.paymentMethods.bankTransfer.enabled &&
                            uiState.paymentMethods.bankTransfer.isConfigured,
                        isCheckoutAvailable = uiState.isStoreOpen || uiState.isAdmin,
                    )
                }

                if (!uiState.isLoggedIn) {
                    item {
                        SkydownCard {
                            SectionHeader(stringResource(R.string.auth_cart_login_title))
                            Text(
                                text = stringResource(R.string.auth_cart_login_subtitle),
                                modifier = Modifier.padding(top = 8.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                            )
                            BrandActionButton(
                                text = stringResource(R.string.auth_cart_login_cta),
                                onClick = onOpenLogin,
                                accent = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 14.dp),
                                icon = Icons.AutoMirrored.Filled.Login,
                            )
                        }
                    }
                } else {
                    if (uiState.isStoreOpen || uiState.isAdmin) {
                        if (uiState.paymentMethods.checkoutMethodLabels.isNotEmpty()) {
                            item {
                                PaymentMethodSelectionCard(
                                    methods = uiState.paymentMethods.checkoutMethodLabels,
                                    selectedMethod = uiState.selectedPaymentMethod,
                                    isZeroCostOrder = isZeroCostOrder,
                                    onSelect = viewModel::selectPaymentMethod,
                                )
                            }

                            if (uiState.selectedPaymentMethod.isNotBlank()) {
                                item {
                                    PaymentMethodDetailCard(
                                        selectedMethod = uiState.selectedPaymentMethod,
                                        settings = uiState.paymentMethods,
                                        isZeroCostOrder = isZeroCostOrder,
                                    )
                                }
                            }
                        }
                    } else {
                        item {
                            CartInlineStatusStrip(
                                icon = Icons.Default.PauseCircle,
                                title = stringResource(R.string.cart_checkout_paused_title),
                            ) {
                                Text(
                                    text = stringResource(R.string.cart_checkout_paused_body),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                                )
                                Text(
                                    text = stringResource(R.string.cart_checkout_paused_hint),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                                )
                            }
                        }
                    }

                    item {
                        SkydownCard {
                            SectionHeader(stringResource(R.string.cart_selection_title))
                            Text(
                                text = if (uiState.items.isEmpty()) {
                                    stringResource(R.string.cart_selection_empty)
                                } else {
                                    stringResource(R.string.cart_selection_review)
                                },
                                modifier = Modifier.padding(top = 8.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            )
                        }
                    }

                    items(uiState.items, key = { "${it.item.id}-${it.size}-${it.color.orEmpty()}" }) { cartItem ->
                        CartItemCard(
                            name = cartItem.item.name,
                            size = cartItem.size,
                            color = cartItem.color,
                            quantity = cartItem.quantity,
                            price = (cartItem.unitPrice ?: cartItem.item.price) * cartItem.quantity,
                            onQuantityChange = { delta ->
                                viewModel.updateItemQuantity(
                                    itemId = cartItem.item.id.orEmpty(),
                                    size = cartItem.size,
                                    color = cartItem.color,
                                    delta = delta,
                                )
                            },
                            onRemove = { viewModel.removeItem(cartItem.item.id.orEmpty(), cartItem.size, cartItem.color) },
                        )
                    }

                    if (uiState.items.isEmpty()) {
                        item {
                            SkydownCard {
                                SectionHeader(stringResource(R.string.cart_selection_title))
                                Text(
                                    text = stringResource(R.string.cart_selection_empty),
                                    modifier = Modifier.padding(top = 8.dp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                                )
                                BrandActionButton(
                                    text = stringResource(R.string.cart_action_continue_shopping),
                                    onClick = { onBack?.invoke() },
                                    accent = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 14.dp),
                                    enabled = onBack != null,
                                )
                            }
                        }
                    } else {
                        item {
                            SkydownCard {
                                SectionHeader(stringResource(R.string.cart_contact_title))
                                Text(
                                    text = stringResource(R.string.cart_contact_subtitle),
                                    modifier = Modifier.padding(top = 8.dp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                                )
                                CartSingleLineField(
                                    value = uiState.name,
                                    onValueChange = viewModel::updateName,
                                    labelRes = R.string.cart_field_name_required,
                                    topPadding = 14.dp,
                                )
                                CartSingleLineField(
                                    value = uiState.email,
                                    onValueChange = viewModel::updateEmail,
                                    labelRes = R.string.cart_field_email_required,
                                )
                                if (showOptionalContactFields || uiState.whatsApp.isNotBlank()) {
                                    CartSingleLineField(
                                        value = uiState.whatsApp,
                                        onValueChange = viewModel::updateWhatsApp,
                                        labelRes = R.string.cart_field_whatsapp_optional,
                                    )
                                } else {
                                    CartOptionalRevealButton(
                                        onClick = { showOptionalContactFields = true },
                                        labelRes = R.string.cart_contact_add_whatsapp,
                                    )
                                }
                            }
                        }

                        item {
                            SkydownCard {
                                SectionHeader(stringResource(R.string.cart_shipping_title))
                                Text(
                                    text = stringResource(R.string.cart_shipping_subtitle),
                                    modifier = Modifier.padding(top = 8.dp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                                )
                                CartSingleLineField(
                                    value = uiState.shippingStreet,
                                    onValueChange = viewModel::updateShippingStreet,
                                    labelRes = R.string.cart_field_street_required,
                                    topPadding = 14.dp,
                                )
                                if (showOptionalAddressFields || uiState.shippingAddressExtra.isNotBlank()) {
                                    CartSingleLineField(
                                        value = uiState.shippingAddressExtra,
                                        onValueChange = viewModel::updateShippingAddressExtra,
                                        labelRes = R.string.cart_field_address_extra_optional,
                                    )
                                } else {
                                    CartOptionalRevealButton(
                                        onClick = { showOptionalAddressFields = true },
                                        labelRes = R.string.cart_shipping_add_address_extra,
                                    )
                                }
                                CartSingleLineField(
                                    value = uiState.shippingPostalCode,
                                    onValueChange = viewModel::updateShippingPostalCode,
                                    labelRes = R.string.cart_field_postal_required,
                                )
                                CartSingleLineField(
                                    value = uiState.shippingCity,
                                    onValueChange = viewModel::updateShippingCity,
                                    labelRes = R.string.cart_field_city_required,
                                )
                                CartSingleLineField(
                                    value = uiState.shippingCountry,
                                    onValueChange = viewModel::updateShippingCountry,
                                    labelRes = R.string.cart_field_country,
                                )
                            }
                        }

                        item {
                            SkydownCard {
                                SectionHeader(stringResource(R.string.cart_message_title))
                                Text(
                                    text = stringResource(R.string.cart_message_subtitle),
                                    modifier = Modifier.padding(top = 8.dp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                                )
                                if (showOptionalMessageField ||
                                    (uiState.message.isNotBlank() &&
                                        uiState.message != defaultCartMessage)
                                ) {
                                    CartMessageField(
                                        value = uiState.message,
                                        onValueChange = viewModel::updateMessage,
                                        labelRes = R.string.cart_field_message,
                                        topPadding = 14.dp,
                                    )
                                } else {
                                    CartOptionalRevealButton(
                                        onClick = { showOptionalMessageField = true },
                                        labelRes = R.string.cart_message_add_optional,
                                        topPadding = 14.dp,
                                    )
                                }
                            }
                        }

                        item {
                            PricingSummaryCard(
                                summary = pricing,
                                shippingZoneLabel = shippingZoneLabel,
                                shippingNote = uiState.commerceSettings.shipping.shippingNotes,
                                companyName = uiState.commerceSettings.invoice.companyName,
                            )
                        }

                        item {
                            SkydownCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingNano)) {
                                        Text(
                                            text = stringResource(R.string.cart_checkout_title),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Text(
                                            text = if (isZeroCostOrder && uiState.selectedPaymentMethod in listOf("Stripe", "Klarna")) {
                                                stringResource(R.string.cart_checkout_zero_eur_hint)
                                            } else if (uiState.selectedPaymentMethod in listOf("Stripe", "Klarna")) {
                                                stringResource(R.string.cart_checkout_stripe_live_hint)
                                            } else {
                                                stringResource(R.string.cart_checkout_followup_hint)
                                            },
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        )
                                    }
                                    CartInfoPill(text = stringResource(R.string.cart_items_count, uiState.items.size))
                                }

                                BrandActionButton(
                                    text = if (uiState.isSubmitting) {
                                        stringResource(R.string.cart_action_preparing)
                                    } else if (isZeroCostOrder && uiState.selectedPaymentMethod in listOf("Stripe", "Klarna")) {
                                        stringResource(R.string.cart_action_confirm_order)
                                    } else if (uiState.selectedPaymentMethod in listOf("Stripe", "Klarna")) {
                                        stringResource(R.string.cart_action_continue_securely)
                                    } else {
                                        stringResource(R.string.cart_action_review_order)
                                    },
                                    onClick = {
                                        val orderSnapshot = uiState
                                        coroutineScope.launch {
                                            if (uiState.selectedPaymentMethod in listOf("Stripe", "Klarna")) {
                                                val result = viewModel.startHostedCheckout()
                                                result.getOrNull()?.let { session ->
                                                    openExternalUrl(appContext, session.checkoutUrl)
                                                }
                                            } else {
                                                val result = viewModel.submitOrder()
                                                if (result.isSuccess) {
                                                    openOrderEmail(appContext, orderSnapshot)
                                                }
                                            }
                                        }
                                    },
                                    accent = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 14.dp),
                                    enabled = viewModel.isFormValid(),
                                    isLoading = uiState.isSubmitting,
                                )
                            }
                        }
                        item {
                            CheckoutSafetyZone()
                        }
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

@Composable
private fun CartSingleLineField(
    value: String,
    onValueChange: (String) -> Unit,
    labelRes: Int,
    topPadding: Dp = 12.dp,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(labelRes)) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding),
        singleLine = true,
        shape = RoundedCornerShape(SkydownUiTokens.cardCornerRadius),
    )
}

@Composable
private fun CartMessageField(
    value: String,
    onValueChange: (String) -> Unit,
    labelRes: Int,
    topPadding: Dp = 12.dp,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(labelRes)) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding),
        minLines = 4,
        shape = RoundedCornerShape(SkydownUiTokens.cardCornerRadius),
    )
}

@Composable
private fun CartOptionalRevealButton(
    onClick: () -> Unit,
    labelRes: Int,
    topPadding: Dp = 12.dp,
) {
    BrandActionButton(
        text = stringResource(labelRes),
        onClick = onClick,
        accent = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(top = topPadding)
            .fillMaxWidth(),
        filled = false,
    )
}

@Composable
private fun CartOverviewCard(
    itemCount: Int,
    totalPrice: Double,
    isLoggedIn: Boolean,
) {
    SkydownCard(contentPadding = PaddingValues(20.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingComfortable),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill),
            ) {
                Text(
                    text = stringResource(R.string.cart_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.cart_subtitle),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro)) {
                    CartInfoPill(text = stringResource(R.string.cart_items_count, itemCount))
                    CartInfoPill(text = if (isLoggedIn) stringResource(R.string.cart_account_active) else stringResource(R.string.cart_guest))
                    if (itemCount > 0) {
                        CartInfoPill(
                            text = stringResource(R.string.order_price_eur, formatCurrency(totalPrice)),
                        )
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
private fun PaymentMethodAvailabilityCard(
    methods: List<String>,
    bankTransferEnabled: Boolean,
    isCheckoutAvailable: Boolean,
) {
    SkydownCard {
        SectionHeader(stringResource(R.string.cart_payment_methods_title))
        if (!isCheckoutAvailable) {
            Text(
                text = stringResource(R.string.cart_payment_methods_paused),
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
        } else if (methods.isEmpty()) {
            Text(
                text = stringResource(R.string.cart_payment_methods_none_visible),
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
        } else {
            val checkoutTrustCopy = when {
                bankTransferEnabled -> {
                    stringResource(R.string.cart_methods_trust_bank_transfer)
                }
                methods.contains("Stripe") && methods.contains("Klarna") -> {
                    stringResource(R.string.cart_methods_trust_stripe_klarna)
                }
                methods.contains("Stripe") -> {
                    stringResource(R.string.cart_methods_trust_stripe)
                }
                methods.contains("Klarna") -> {
                    stringResource(R.string.cart_methods_trust_klarna)
                }
                methods.contains("PayPal") -> {
                    stringResource(R.string.cart_methods_trust_paypal)
                }
                else -> {
                    stringResource(R.string.cart_payment_methods_active_now)
                }
            }
            Text(
                text = stringResource(R.string.cart_payment_methods_active_now),
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
            ) {
                methods.forEach { method ->
                    CartInfoPill(text = method)
                }
            }
            Text(
                text = checkoutTrustCopy,
                modifier = Modifier.padding(top = 12.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
        }
    }
}

@Composable
private fun PricingSummaryCard(
    summary: CartPricingSummaryUi,
    shippingZoneLabel: String,
    shippingNote: String,
    companyName: String,
) {
    SkydownCard {
        SectionHeader(stringResource(R.string.cart_summary_title))
        PaymentInfoLine(
            stringResource(R.string.cart_summary_subtotal),
            stringResource(R.string.order_price_eur, formatCurrency(summary.subtotal)),
        )
        PaymentInfoLine(stringResource(R.string.cart_summary_shipping_zone), shippingZoneLabel)
        PaymentInfoLine(
            stringResource(R.string.cart_summary_shipping),
            stringResource(R.string.order_price_eur, formatCurrency(summary.shipping)),
        )
        PaymentInfoLine(
            stringResource(R.string.cart_summary_tax_included, formatCurrency(summary.taxRate, decimals = 1)),
            stringResource(R.string.order_price_eur, formatCurrency(summary.includedTax)),
        )
        PaymentInfoLine(
            stringResource(R.string.cart_summary_total),
            stringResource(R.string.order_price_eur, formatCurrency(summary.total)),
        )
        summary.shippingError?.let { error ->
            Text(
                text = error,
                modifier = Modifier.padding(top = 12.dp),
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (shippingNote.isNotBlank()) {
            Text(
                text = shippingNote,
                modifier = Modifier.padding(top = 12.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
        }
        Text(
            text = stringResource(
                R.string.cart_invoice_contact_line,
                companyName.ifBlank { stringResource(R.string.cart_invoice_contact_fallback_company) },
            ),
            modifier = Modifier.padding(top = 10.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
        )
    }
}

@Composable
private fun PaymentMethodSelectionCard(
    methods: List<String>,
    selectedMethod: String,
    isZeroCostOrder: Boolean,
    onSelect: (String) -> Unit,
) {
    SkydownCard {
        SectionHeader(stringResource(R.string.cart_select_payment_title))
        Text(
            text = stringResource(R.string.cart_select_payment_subtitle),
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
        )

        Column(
            modifier = Modifier.padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill),
        ) {
            methods.forEach { method ->
                val isSelected = selectedMethod == method
                val content: @Composable () -> Unit = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingDense),
                        ) {
                            Text(method, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = paymentRouteDetail(method),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            )
                            Text(
                                text = if (isSelected) stringResource(R.string.cart_selected) else stringResource(R.string.cart_available),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
                            )
                        }
                    }
                }
                Surface(
                    onClick = { onSelect(method) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(SkydownUiTokens.cardCornerRadius),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    border = if (isSelected) {
                        null
                    } else {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f))
                    },
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    ) {
                        content()
                    }
                }
            }
        }

        if (isZeroCostOrder && selectedMethod in listOf("Stripe", "Klarna")) {
            Text(
                text = stringResource(R.string.cart_zero_eur_no_payment),
                modifier = Modifier.padding(top = 12.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
        } else if (selectedMethod == "Klarna") {
            Text(
                text = stringResource(R.string.cart_klarna_checkout_hint),
                modifier = Modifier.padding(top = 12.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
        } else if (selectedMethod == "Stripe") {
            Text(
                text = stringResource(R.string.cart_stripe_checkout_hint),
                modifier = Modifier.padding(top = 12.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
        }
    }
}

@Composable
private fun CheckoutSafetyZone() {
    SkydownCard {
        Column(verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro)) {
            Text(
                text = stringResource(R.string.cart_safety_title),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
            Text(
                text = stringResource(R.string.cart_safety_line_one),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
            Text(
                text = stringResource(R.string.cart_safety_line_two),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
            Text(
                text = stringResource(R.string.cart_safety_line_three),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
        }
    }
}

@Composable
private fun CheckoutPulseCard(
    readinessTitle: String,
    readinessDetail: String,
    paymentTitle: String,
    paymentDetail: String,
    totalTitle: String,
    totalDetail: String,
    compactLayout: Boolean,
) {
    SkydownCard {
        SectionHeader(stringResource(R.string.cart_checkout_pulse_title))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingCompact),
        ) {
            if (compactLayout) {
                CheckoutSignalCard(
                    title = stringResource(R.string.cart_pulse_status),
                    value = readinessTitle,
                    detail = readinessDetail,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingCompact),
                ) {
                    CheckoutSignalCard(
                        title = stringResource(R.string.cart_pulse_payment),
                        value = paymentTitle,
                        detail = paymentDetail,
                        modifier = Modifier.weight(1f),
                    )
                    CheckoutSignalCard(
                        title = stringResource(R.string.cart_pulse_total),
                        value = totalTitle,
                        detail = totalDetail,
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingCompact),
                ) {
                    CheckoutSignalCard(
                        title = stringResource(R.string.cart_pulse_status),
                        value = readinessTitle,
                        detail = readinessDetail,
                        modifier = Modifier.weight(1f),
                    )
                    CheckoutSignalCard(
                        title = stringResource(R.string.cart_pulse_payment),
                        value = paymentTitle,
                        detail = paymentDetail,
                        modifier = Modifier.weight(1f),
                    )
                    CheckoutSignalCard(
                        title = stringResource(R.string.cart_pulse_total),
                        value = totalTitle,
                        detail = totalDetail,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CartInlineStatusStrip(
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
            Icon(
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
private fun CheckoutSignalCard(
    title: String,
    value: String,
    detail: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(SkydownUiTokens.cardCornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.76f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            maxLines = 2,
        )
    }
}

@Composable
private fun PaymentMethodDetailCard(
    selectedMethod: String,
    settings: com.nash.skyos.data.PaymentMethodsSettings,
    isZeroCostOrder: Boolean,
) {
    SkydownCard {
        SectionHeader(stringResource(R.string.cart_payment_info_title))

        when (selectedMethod) {
            "PayPal" -> {
                Text(
                    text = stringResource(R.string.cart_payment_info_paypal_body),
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
                if (settings.paypal.accountHint.isBlank()) {
                    Text(
                        text = stringResource(R.string.cart_payment_info_paypal_missing),
                        modifier = Modifier.padding(top = 12.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    )
                } else {
                    CartInfoPill(
                        text = settings.paypal.accountHint,
                    )
                }
            }

            "Bankueberweisung" -> {
                Text(
                    text = stringResource(R.string.cart_payment_info_bank_body),
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
                ) {
                    if (settings.bankTransfer.accountHolder.isNotBlank()) {
                        PaymentInfoLine(stringResource(R.string.cart_bank_account_holder), settings.bankTransfer.accountHolder)
                    }
                    if (settings.bankTransfer.bankName.isNotBlank()) {
                        PaymentInfoLine(stringResource(R.string.cart_bank_name), settings.bankTransfer.bankName)
                    }
                    if (settings.bankTransfer.iban.isNotBlank()) {
                        PaymentInfoLine(stringResource(R.string.settings_payments_iban), settings.bankTransfer.iban)
                    }
                    if (settings.bankTransfer.bic.isNotBlank()) {
                        PaymentInfoLine(stringResource(R.string.settings_payments_bic), settings.bankTransfer.bic)
                    }
                    if (settings.bankTransfer.paymentInstructions.isNotBlank()) {
                        PaymentInfoLine(stringResource(R.string.cart_bank_note), settings.bankTransfer.paymentInstructions)
                    }
                }
            }

            "Stripe", "Klarna" -> {
                Text(
                    text = if (isZeroCostOrder) {
                        stringResource(R.string.cart_payment_info_stripe_zero_eur)
                    } else if (selectedMethod == "Klarna") {
                        stringResource(R.string.cart_payment_info_klarna_body)
                    } else {
                        stringResource(R.string.cart_payment_info_stripe_body)
                    },
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
            }

            else -> Unit
        }
    }
}

@Composable
private fun PaymentInfoLine(
    title: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingNano)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun CartItemCard(
    name: String,
    size: String,
    color: String?,
    quantity: Int,
    price: Double,
    onQuantityChange: (Int) -> Unit,
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
                verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro)) {
                    CartInfoPill(text = stringResource(R.string.cart_size_value, size))
                    color?.takeIf { it.isNotBlank() }?.let {
                        CartInfoPill(text = it)
                    }
                    CartInfoPill(text = stringResource(R.string.cart_quantity_value, quantity))
                }
            }
            Text(
                text = stringResource(R.string.order_price_eur, formatCurrency(price)),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrandActionButton(
                text = stringResource(R.string.cart_quantity_decrease),
                onClick = { onQuantityChange(-1) },
                accent = MaterialTheme.colorScheme.tertiary,
                filled = false,
                compact = true,
                enabled = quantity > 1,
            )
            BrandActionButton(
                text = stringResource(R.string.cart_quantity_increase),
                onClick = { onQuantityChange(1) },
                accent = MaterialTheme.colorScheme.tertiary,
                filled = false,
                compact = true,
                enabled = quantity < 10,
            )
            BrandActionButton(
                text = stringResource(R.string.common_remove),
                onClick = onRemove,
                accent = MaterialTheme.colorScheme.error,
                icon = Icons.Default.DeleteOutline,
                filled = false,
                compact = true,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CartInfoPill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(SkydownUiTokens.fullCapsuleRadius))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .animateContentSize(animationSpec = skydownContentSizeRevealSpec())
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

private fun formatCurrency(value: Double, decimals: Int): String {
    return String.format(Locale.US, "%.${decimals}f", value)
}

@Composable
private fun paymentRouteDetail(method: String): String {
    return when (method) {
        "Stripe" -> stringResource(R.string.cart_route_stripe)
        "Klarna" -> stringResource(R.string.cart_route_klarna)
        "PayPal" -> stringResource(R.string.cart_route_paypal)
        "Bankueberweisung" -> stringResource(R.string.cart_route_bank_transfer)
        stringResource(R.string.cart_payment_callback) -> stringResource(R.string.cart_route_callback)
        else -> stringResource(R.string.cart_route_default)
    }
}

private data class CartPricingSummaryUi(
    val subtotal: Double,
    val shipping: Double,
    val taxRate: Double,
    val includedTax: Double,
    val total: Double,
    val zoneLabel: String,
    val shippingError: String? = null,
)

private fun cartPricingSummary(
    state: com.nash.skyos.ui.model.CartUiState,
): CartPricingSummaryUi {
    val subtotal = state.items.sumOf { (it.unitPrice ?: it.item.price) * it.quantity }
    if (state.items.isEmpty()) {
        return CartPricingSummaryUi(
            subtotal = 0.0,
            shipping = 0.0,
            taxRate = state.commerceSettings.invoice.taxRate,
            includedTax = 0.0,
            total = 0.0,
            zoneLabel = "pending",
        )
    }
    val countryCodeResult = ShippingService.resolveCountryCode(state.shippingCountry)
    val quoteResult = countryCodeResult.mapCatching { countryCode ->
        ShippingService.calculateShippingPrice(
            settings = state.commerceSettings.shipping,
            countryCode = countryCode,
            items = state.items,
            subtotal = subtotal,
        ).getOrThrow()
    }
    val shippingQuote = quoteResult.getOrNull()
    val shipping = shippingQuote?.price ?: 0.0
    val total = subtotal + shipping
    val taxRate = state.commerceSettings.invoice.taxRate
    val includedTax = if (taxRate > 0) total * (taxRate / (100.0 + taxRate)) else 0.0

    return CartPricingSummaryUi(
        subtotal = subtotal,
        shipping = shipping,
        taxRate = taxRate,
        includedTax = includedTax,
        total = total,
        zoneLabel = shippingQuote?.zone?.name ?: "check-country",
        shippingError = quoteResult.exceptionOrNull()?.message,
    )
}

private fun openOrderEmail(
    context: Context,
    state: com.nash.skyos.ui.model.CartUiState,
) {
    val notProvided = context.getString(R.string.cart_email_not_provided)
    val paymentPending = context.getString(R.string.cart_email_payment_pending)
    val noAdditionalMessage = context.getString(R.string.cart_email_no_additional_message)
    val subject = if (state.email.isNotBlank()) {
        context.getString(R.string.cart_email_subject_with_email, state.email)
    } else {
        context.getString(R.string.cart_email_subject)
    }
    val itemSummary = if (state.items.isEmpty()) {
        context.getString(R.string.cart_email_no_items)
    } else {
        state.items.joinToString(separator = "\n") { cartItem ->
            val price = (cartItem.unitPrice ?: cartItem.item.price) * cartItem.quantity
            val colorPart = cartItem.color?.takeIf { it.isNotBlank() }?.let { color ->
                context.getString(R.string.cart_email_item_color_part, color)
            }.orEmpty()
            context.getString(
                R.string.cart_email_item_line,
                cartItem.item.name,
                cartItem.size,
                colorPart,
                cartItem.quantity,
                formatCurrency(price),
            )
        }
    }
    val pricing = cartPricingSummary(state)
    val shippingAddress = listOf(
        state.shippingStreet.trim(),
        state.shippingAddressExtra.trim(),
        listOf(state.shippingPostalCode.trim(), state.shippingCity.trim())
            .filter { it.isNotBlank() }
            .joinToString(" "),
        state.shippingCountry.trim().ifBlank { context.getString(R.string.cart_email_country_default) },
    ).filter { it.isNotBlank() }.joinToString("\n")
    val body = context.getString(
        R.string.cart_email_body_template,
        state.name.ifBlank { notProvided },
        state.email.ifBlank { notProvided },
        state.whatsApp.ifBlank { notProvided },
        shippingAddress.ifBlank { notProvided },
        itemSummary,
        formatCurrency(pricing.subtotal),
        formatCurrency(pricing.shipping),
        formatCurrency(pricing.taxRate, decimals = 1),
        formatCurrency(pricing.includedTax),
        formatCurrency(pricing.total),
        state.selectedPaymentMethod.ifBlank { paymentPending },
        state.message.ifBlank { noAdditionalMessage },
    )
    openEmailDraft(
        context = context,
        recipients = listOf(PlatformContactEmails.DEFAULT_SUPPORT_EMAIL),
        subject = subject,
        body = body,
    )
}

private fun openExternalUrl(
    context: Context,
    url: String,
) {
    openExternalLink(
        context = context,
        url = url,
        browserMissingMessage = context.getString(R.string.cart_checkout_link_open_failed),
    )
}
