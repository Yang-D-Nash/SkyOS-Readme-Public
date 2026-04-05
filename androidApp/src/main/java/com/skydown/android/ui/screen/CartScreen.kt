package com.skydown.android.ui.screen

import android.content.Context
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skydown.android.ui.component.AppTopBarSessionActions
import com.skydown.android.ui.component.SectionHeader
import com.skydown.android.ui.component.SkydownCard
import com.skydown.android.ui.component.SkydownTopBarTitle
import com.skydown.android.ui.component.ToastHost
import com.skydown.android.ui.component.ToastType
import com.skydown.android.ui.component.skydownTopBarColors
import com.skydown.android.data.CheckoutRedirectStore
import com.skydown.android.data.ShippingService
import com.skydown.android.ui.viewmodel.CartViewModel
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    onBack: (() -> Unit)? = null,
    onOpenLogin: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    viewModel: CartViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val checkoutRedirectEvent by CheckoutRedirectStore.latestEvent.collectAsStateWithLifecycle()
    val appContext = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val pricing = cartPricingSummary(uiState)

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

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    SkydownTopBarTitle(
                        title = "Warenkorb",
                        subtitle = "Produkte und Checkout.",
                    )
                },
                navigationIcon = if (onBack != null) {
                    {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Zurueck",
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
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = innerPadding.calculateTopPadding() + com.skydown.android.ui.component.SkydownUiTokens.screenTopPadding,
                    end = 16.dp,
                    bottom = innerPadding.calculateBottomPadding() + 28.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    CartOverviewCard(
                        itemCount = uiState.items.size,
                        totalPrice = pricing.total,
                        isLoggedIn = uiState.isLoggedIn,
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
                    if (uiState.isStoreOpen || uiState.isAdmin) {
                        if (uiState.paymentMethods.checkoutMethodLabels.isNotEmpty()) {
                            item {
                                PaymentMethodSelectionCard(
                                    methods = uiState.paymentMethods.checkoutMethodLabels,
                                    selectedMethod = uiState.selectedPaymentMethod,
                                    onSelect = viewModel::selectPaymentMethod,
                                )
                            }

                            if (uiState.selectedPaymentMethod.isNotBlank()) {
                                item {
                                    PaymentMethodDetailCard(
                                        selectedMethod = uiState.selectedPaymentMethod,
                                        settings = uiState.paymentMethods,
                                    )
                                }
                            }
                        }
                    } else {
                        item {
                            SkydownCard(contentPadding = PaddingValues(18.dp)) {
                                SectionHeader("Checkout pausiert")
                                Text(
                                    text = "Der Merchandise-Store ist gerade pausiert. Deine Auswahl bleibt sichtbar, aber neue Bestellungen werden erst wieder freigeschaltet, sobald der Store geoeffnet ist.",
                                    modifier = Modifier.padding(top = 8.dp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                                )
                            }
                        }
                    }

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

                    items(uiState.items, key = { "${it.item.id}-${it.size}-${it.color.orEmpty()}" }) { cartItem ->
                        CartItemCard(
                            name = cartItem.item.name,
                            size = cartItem.size,
                            color = cartItem.color,
                            quantity = cartItem.quantity,
                            price = (cartItem.unitPrice ?: cartItem.item.price) * cartItem.quantity,
                            onRemove = { viewModel.removeItem(cartItem.item.id.orEmpty(), cartItem.size, cartItem.color) },
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
                            SectionHeader("Lieferadresse")
                            Text(
                                text = "Die Versandadresse wird fuer Rueckmeldung, Versand und Bestellabwicklung benoetigt.",
                                modifier = Modifier.padding(top = 8.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            )
                            OutlinedTextField(
                                value = uiState.shippingStreet,
                                onValueChange = viewModel::updateShippingStreet,
                                label = { Text("Strasse und Hausnummer*") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 14.dp),
                                singleLine = true,
                                shape = RoundedCornerShape(18.dp),
                            )
                            OutlinedTextField(
                                value = uiState.shippingAddressExtra,
                                onValueChange = viewModel::updateShippingAddressExtra,
                                label = { Text("Adresszusatz (optional)") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                singleLine = true,
                                shape = RoundedCornerShape(18.dp),
                            )
                            OutlinedTextField(
                                value = uiState.shippingPostalCode,
                                onValueChange = viewModel::updateShippingPostalCode,
                                label = { Text("PLZ*") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                singleLine = true,
                                shape = RoundedCornerShape(18.dp),
                            )
                            OutlinedTextField(
                                value = uiState.shippingCity,
                                onValueChange = viewModel::updateShippingCity,
                                label = { Text("Ort*") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                singleLine = true,
                                shape = RoundedCornerShape(18.dp),
                            )
                            OutlinedTextField(
                                value = uiState.shippingCountry,
                                onValueChange = viewModel::updateShippingCountry,
                                label = { Text("Land") },
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
                        PricingSummaryCard(
                            summary = pricing,
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
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "Checkout",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = if (uiState.selectedPaymentMethod in listOf("Stripe", "Klarna")) {
                                            "Stripe oeffnet danach den sicheren Live-Checkout."
                                        } else {
                                            "Wir melden uns danach per E-Mail oder WhatsApp."
                                        },
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    )
                                }
                                CartInfoPill(text = "${uiState.items.size} Artikel")
                            }

                            Button(
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
                                enabled = viewModel.isFormValid() && !uiState.isSubmitting,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 14.dp),
                                shape = RoundedCornerShape(18.dp),
                            ) {
                                Text(
                                    if (uiState.isSubmitting) {
                                        "Sende Bestellung..."
                                    } else if (uiState.selectedPaymentMethod in listOf("Stripe", "Klarna")) {
                                        "Zum sicheren Checkout"
                                    } else {
                                        "Bestellung abschicken"
                                    },
                                )
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
                    .padding(
                        top = innerPadding.calculateTopPadding() + com.skydown.android.ui.component.SkydownUiTokens.screenTopPadding,
                    ),
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
private fun PaymentMethodAvailabilityCard(
    methods: List<String>,
    bankTransferEnabled: Boolean,
    isCheckoutAvailable: Boolean,
) {
    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        SectionHeader("Zahlungsarten")
        if (!isCheckoutAvailable) {
            Text(
                text = "Der Merchandise-Store ist aktuell pausiert. Zahlarten und Checkout werden erst wieder aktiv, sobald der Store geoeffnet ist.",
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
        } else if (methods.isEmpty()) {
            Text(
                text = "Aktuell ist noch keine Zahlart fuer Kunden sichtbar. Der Merch-Checkout bleibt bis dahin auf Anfrage und Rueckkontakt ausgelegt.",
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
        } else {
            Text(
                text = "Diese Zahlarten sind aktuell aktiv:",
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                methods.forEach { method ->
                    CartInfoPill(text = method)
                }
            }
            Text(
                text = if (bankTransferEnabled) {
                    "Bankdaten und genaue Anweisung folgen nach der Bestellbestaetigung direkt durch das Team."
                } else {
                    "Stripe und Klarna laufen als sicherer Live-Checkout. PayPal und Bankueberweisung bleiben manuell owner-geprueft."
                },
                modifier = Modifier.padding(top = 12.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
        }
    }
}

@Composable
private fun PricingSummaryCard(
    summary: CartPricingSummaryUi,
    shippingNote: String,
    companyName: String,
) {
    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        SectionHeader("Bestellsumme")
        PaymentInfoLine("Zwischensumme", "EUR ${formatCurrency(summary.subtotal)}")
        PaymentInfoLine("Versandzone", summary.zoneLabel)
        PaymentInfoLine("Versand", "EUR ${formatCurrency(summary.shipping)}")
        PaymentInfoLine(
            "inkl. MwSt. (${formatCurrency(summary.taxRate, decimals = 1)}%)",
            "EUR ${formatCurrency(summary.includedTax)}",
        )
        PaymentInfoLine("Gesamt", "EUR ${formatCurrency(summary.total)}")
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
            text = "Rechnung und Rueckmeldung laufen ueber ${companyName.ifBlank { "Skydown Entertainment" }}.",
            modifier = Modifier.padding(top = 10.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
        )
    }
}

@Composable
private fun PaymentMethodSelectionCard(
    methods: List<String>,
    selectedMethod: String,
    onSelect: (String) -> Unit,
) {
    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        SectionHeader("Zahlart waehlen")
        Text(
            text = "Waehle die Zahlart fuer diese Bestellung.",
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
        )

        Column(
            modifier = Modifier.padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            methods.forEach { method ->
                OutlinedButton(
                    onClick = { onSelect(method) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(method)
                        Text(if (selectedMethod == method) "Ausgewaehlt" else "Waehlen")
                    }
                }
            }
        }

        if (selectedMethod == "Klarna") {
            Text(
                text = "Klarna oeffnet nach dem Absenden einen sicheren Live-Checkout ueber Stripe.",
                modifier = Modifier.padding(top = 12.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
        }
    }
}

@Composable
private fun PaymentMethodDetailCard(
    selectedMethod: String,
    settings: com.skydown.android.data.PaymentMethodsSettings,
) {
    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        SectionHeader("Zahlungsinfo")

        when (selectedMethod) {
            "PayPal" -> {
                Text(
                    text = "PayPal wird hier als sicherer manueller Handoff genutzt. Fuer einen direkten Flow hinterlege am besten einen PayPal.Me-Link.",
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
                if (settings.paypal.accountHint.isBlank()) {
                    Text(
                        text = "Im Admin-Bereich ist noch kein PayPal.Me-Link oder keine Business-Mail hinterlegt.",
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
                    text = "Die Bankueberweisung laeuft direkt und ohne Gateway-Kosten. Die hinterlegten Daten gelten fuer diese Bestellung.",
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (settings.bankTransfer.accountHolder.isNotBlank()) {
                        PaymentInfoLine("Kontoinhaber", settings.bankTransfer.accountHolder)
                    }
                    if (settings.bankTransfer.bankName.isNotBlank()) {
                        PaymentInfoLine("Bank", settings.bankTransfer.bankName)
                    }
                    if (settings.bankTransfer.iban.isNotBlank()) {
                        PaymentInfoLine("IBAN", settings.bankTransfer.iban)
                    }
                    if (settings.bankTransfer.bic.isNotBlank()) {
                        PaymentInfoLine("BIC", settings.bankTransfer.bic)
                    }
                    if (settings.bankTransfer.paymentInstructions.isNotBlank()) {
                        PaymentInfoLine("Hinweis", settings.bankTransfer.paymentInstructions)
                    }
                }
            }

            "Stripe", "Klarna" -> {
                Text(
                    text = if (selectedMethod == "Klarna") {
                        "Klarna startet nach dem Absenden einen sicheren Live-Checkout ueber Stripe und bestaetigt die Zahlung automatisch im Backend."
                    } else {
                        "Stripe startet nach dem Absenden einen sicheren Live-Checkout fuer Kartenzahlungen."
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
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                    color?.takeIf { it.isNotBlank() }?.let {
                        CartInfoPill(text = it)
                    }
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

private fun formatCurrency(value: Double, decimals: Int): String {
    return String.format(Locale.US, "%.${decimals}f", value)
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
    state: com.skydown.android.ui.model.CartUiState,
): CartPricingSummaryUi {
    val subtotal = state.items.sumOf { (it.unitPrice ?: it.item.price) * it.quantity }
    if (state.items.isEmpty()) {
        return CartPricingSummaryUi(
            subtotal = 0.0,
            shipping = 0.0,
            taxRate = state.commerceSettings.invoice.taxRate,
            includedTax = 0.0,
            total = 0.0,
            zoneLabel = "Noch offen",
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
        zoneLabel = shippingQuote?.zone?.name ?: "Land pruefen",
        shippingError = quoteResult.exceptionOrNull()?.message,
    )
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
            val price = (cartItem.unitPrice ?: cartItem.item.price) * cartItem.quantity
            val colorPart = cartItem.color?.takeIf { it.isNotBlank() }?.let { " | Farbe: $it" }.orEmpty()
            "- ${cartItem.item.name} | Groesse: ${cartItem.size}$colorPart | Menge: ${cartItem.quantity} | Preis: EUR ${formatCurrency(price)}"
        }
    }
    val pricing = cartPricingSummary(state)
    val shippingAddress = listOf(
        state.shippingStreet.trim(),
        state.shippingAddressExtra.trim(),
        listOf(state.shippingPostalCode.trim(), state.shippingCity.trim())
            .filter { it.isNotBlank() }
            .joinToString(" "),
        state.shippingCountry.trim().ifBlank { "Deutschland" },
    ).filter { it.isNotBlank() }.joinToString("\n")
    val body = """
        Hallo Skydown-Team,

        es wurde eine neue Bestellung in der Skydown App vorbereitet.

        Name: ${state.name.ifBlank { "Nicht angegeben" }}
        E-Mail: ${state.email.ifBlank { "Nicht angegeben" }}
        WhatsApp: ${state.whatsApp.ifBlank { "Nicht angegeben" }}
        Adresse:
        ${shippingAddress.ifBlank { "Nicht angegeben" }}

        Warenkorb:
        $itemSummary

        Zwischensumme: EUR ${formatCurrency(pricing.subtotal)}
        Versand: EUR ${formatCurrency(pricing.shipping)}
        Enthaltene MwSt. (${formatCurrency(pricing.taxRate, decimals = 1)}%): EUR ${formatCurrency(pricing.includedTax)}
        Gesamt: EUR ${formatCurrency(pricing.total)}

        Zahlart:
        ${state.selectedPaymentMethod.ifBlank { "Noch offen / per Rueckkontakt" }}

        Nachricht:
        ${state.message.ifBlank { "Keine zusaetzliche Nachricht." }}
    """.trimIndent()
    openEmailDraft(
        context = context,
        recipients = listOf("skydownent@gmail.com"),
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
        browserMissingMessage = "Checkout-Link konnte nicht geoeffnet werden.",
    )
}
