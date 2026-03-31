package com.skydown.android.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skydown.android.ui.component.SectionHeader
import com.skydown.android.ui.component.SkydownCard
import com.skydown.android.ui.component.SkydownTopBarTitle
import com.skydown.android.ui.component.skydownContentPadding
import com.skydown.android.ui.component.skydownScreenBrush
import com.skydown.android.ui.component.skydownTopBarColors
import com.skydown.android.ui.model.SettingsLegalDocumentType
import com.skydown.android.ui.model.SettingsUiState
import com.skydown.android.ui.model.resolve
import com.skydown.android.ui.theme.AppearanceMode
import com.skydown.android.ui.viewmodel.SettingsViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onClose: (() -> Unit)? = null,
    onOpenLogin: () -> Unit = {},
    onOpenRegistration: () -> Unit = {},
    onOpenOrders: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var stripeAccountHintDraft by rememberSaveable { mutableStateOf("") }
    var paypalAccountHintDraft by rememberSaveable { mutableStateOf("") }
    var klarnaAccountHintDraft by rememberSaveable { mutableStateOf("") }
    var bankAccountHolderDraft by rememberSaveable { mutableStateOf("") }
    var bankIbanDraft by rememberSaveable { mutableStateOf("") }
    var bankBicDraft by rememberSaveable { mutableStateOf("") }
    var bankNameDraft by rememberSaveable { mutableStateOf("") }
    var bankInstructionsDraft by rememberSaveable { mutableStateOf("") }
    var domesticShippingDraft by rememberSaveable { mutableStateOf("") }
    var internationalShippingDraft by rememberSaveable { mutableStateOf("") }
    var freeShippingThresholdDraft by rememberSaveable { mutableStateOf("") }
    var shippingNotesDraft by rememberSaveable { mutableStateOf("") }
    var invoiceCompanyNameDraft by rememberSaveable { mutableStateOf("") }
    var invoiceCompanyAddressDraft by rememberSaveable { mutableStateOf("") }
    var invoiceTaxNumberDraft by rememberSaveable { mutableStateOf("") }
    var invoiceVatIdDraft by rememberSaveable { mutableStateOf("") }
    var invoiceTaxRateDraft by rememberSaveable { mutableStateOf("") }
    var invoicePrefixDraft by rememberSaveable { mutableStateOf("") }
    var invoiceSupportEmailDraft by rememberSaveable { mutableStateOf("") }
    val activeLegalDocument = rememberSaveable {
        mutableStateOf<SettingsLegalDocumentType?>(null)
    }
    val showDeleteAccountDialog = rememberSaveable {
        mutableStateOf(false)
    }

    LaunchedEffect(uiState.paymentMethods) {
        stripeAccountHintDraft = uiState.paymentMethods.stripe.accountHint
        paypalAccountHintDraft = uiState.paymentMethods.paypal.accountHint
        klarnaAccountHintDraft = uiState.paymentMethods.klarna.accountHint
        bankAccountHolderDraft = uiState.paymentMethods.bankTransfer.accountHolder
        bankIbanDraft = uiState.paymentMethods.bankTransfer.iban
        bankBicDraft = uiState.paymentMethods.bankTransfer.bic
        bankNameDraft = uiState.paymentMethods.bankTransfer.bankName
        bankInstructionsDraft = uiState.paymentMethods.bankTransfer.paymentInstructions
    }

    LaunchedEffect(uiState.commerceSettings) {
        domesticShippingDraft = formatDecimalDraft(uiState.commerceSettings.shipping.domesticCost)
        internationalShippingDraft = formatDecimalDraft(uiState.commerceSettings.shipping.internationalCost)
        freeShippingThresholdDraft = formatDecimalDraft(uiState.commerceSettings.shipping.freeShippingThreshold)
        shippingNotesDraft = uiState.commerceSettings.shipping.shippingNotes
        invoiceCompanyNameDraft = uiState.commerceSettings.invoice.companyName
        invoiceCompanyAddressDraft = uiState.commerceSettings.invoice.companyAddress
        invoiceTaxNumberDraft = uiState.commerceSettings.invoice.taxNumber
        invoiceVatIdDraft = uiState.commerceSettings.invoice.vatId
        invoiceTaxRateDraft = formatDecimalDraft(uiState.commerceSettings.invoice.taxRate, decimals = 1)
        invoicePrefixDraft = uiState.commerceSettings.invoice.invoicePrefix
        invoiceSupportEmailDraft = uiState.commerceSettings.invoice.supportEmail
    }

    LaunchedEffect(uiState.paymentFeedbackMessage) {
        val message = uiState.paymentFeedbackMessage ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        viewModel.clearPaymentFeedback()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    SkydownTopBarTitle(
                        title = "Einstellungen",
                        subtitle = "Konto, Rechtliches, Anzeige und Support sauber sortiert.",
                    )
                },
                navigationIcon = {
                    onClose?.let { close ->
                        IconButton(onClick = close) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Schliessen",
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
                    skydownScreenBrush(
                        secondaryColor = MaterialTheme.colorScheme.tertiary,
                        primaryAlpha = 0.06f,
                        secondaryAlpha = 0.05f,
                    ),
                ),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = skydownContentPadding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    SettingsOverviewCard(uiState = uiState)
                }

                item {
                    SkydownCard(contentPadding = PaddingValues(18.dp)) {
                        SectionHeader("Konto")
                        if (uiState.isLoggedIn) {
                            Text(
                                text = "Angemeldet als ${uiState.username}",
                                modifier = Modifier.padding(top = 8.dp),
                                fontWeight = FontWeight.SemiBold,
                            )
                            if (uiState.email.isNotBlank()) {
                                Text(
                                    text = uiState.email,
                                    modifier = Modifier.padding(top = 4.dp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                                )
                            }
                            Text(
                                text = "Du kannst dich hier abmelden, mit einem anderen Konto neu anmelden oder dein Konto loeschen.",
                                modifier = Modifier.padding(top = 8.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Button(
                                    onClick = { viewModel.signOut() },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !uiState.isSigningOut && !uiState.isDeletingAccount,
                                    shape = RoundedCornerShape(18.dp),
                                ) {
                                    Text(if (uiState.isSigningOut) "Abmelden..." else "Abmelden")
                                }
                                OutlinedButton(
                                    onClick = { viewModel.signOut(onOpenLogin) },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !uiState.isSigningOut && !uiState.isDeletingAccount,
                                    shape = RoundedCornerShape(18.dp),
                                ) {
                                    Text("Mit anderem Konto anmelden")
                                }
                                OutlinedButton(
                                    onClick = { showDeleteAccountDialog.value = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !uiState.isSigningOut && !uiState.isDeletingAccount,
                                    shape = RoundedCornerShape(18.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error,
                                    ),
                                ) {
                                    Text(
                                        if (uiState.isDeletingAccount) {
                                            "Konto wird geloescht..."
                                        } else {
                                            "Konto loeschen"
                                        },
                                    )
                                }
                            }
                            uiState.accountErrorMessage?.let { message ->
                                Text(
                                    text = message,
                                    modifier = Modifier.padding(top = 10.dp),
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        } else {
                            Text(
                                text = "Melde dich an oder registriere dich, um Bestellungen und persoenliche Bereiche freizuschalten.",
                                modifier = Modifier.padding(top = 8.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Button(
                                    onClick = onOpenLogin,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                ) {
                                    Text("Anmelden")
                                }
                                OutlinedButton(
                                    onClick = onOpenRegistration,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                ) {
                                    Text("Registrieren")
                                }
                            }
                        }
                    }
                }

                item {
                    SkydownCard(contentPadding = PaddingValues(18.dp)) {
                        SectionHeader("Admin")
                        Text(
                            text = if (uiState.isAdmin) "Bestellungen verfuegbar" else "Keine Admin-Berechtigung",
                            modifier = Modifier.padding(top = 8.dp),
                            color = if (uiState.isAdmin) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            },
                        )
                        Text(
                            text = "Admin-Bereiche bleiben auf Android sichtbar, aber nur mit passender Berechtigung aktiv.",
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        )
                        OutlinedButton(
                            onClick = onOpenOrders,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            enabled = uiState.isAdmin,
                            shape = RoundedCornerShape(18.dp),
                        ) {
                            Text("Bestellungen oeffnen")
                        }

                        if (uiState.isAdmin) {
                            Text(
                                text = "Visual Reference Pack",
                                modifier = Modifier.padding(top = 16.dp),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            SettingsToggleRow(
                                title = "Referenzbibliothek aktiv",
                                body = "Drive-Link, Benennungs-Praefix und bis zu 5 Referenzhinweise fuer Visual-Prompts auf diesem Admin-Geraet.",
                                checked = uiState.aiVisualReferenceLibrary.isEnabled,
                                onCheckedChange = viewModel::updateAiVisualReferenceEnabled,
                                modifier = Modifier.padding(top = 10.dp),
                            )
                            OutlinedTextField(
                                value = uiState.aiVisualReferenceLibrary.storageLink,
                                onValueChange = viewModel::updateAiVisualStorageLink,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                label = { Text("Drive- oder Asset-Link") },
                                placeholder = { Text("https://drive.google.com/...") },
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = uiState.aiVisualReferenceLibrary.namingPrefix,
                                onValueChange = viewModel::updateAiVisualNamingPrefix,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                label = { Text("Benennungs-Praefix") },
                                placeholder = { Text("z. B. skydown_drop_") },
                                singleLine = true,
                            )
                            uiState.aiVisualReferenceLibrary.referenceHints.forEachIndexed { index, referenceHint ->
                                OutlinedTextField(
                                    value = referenceHint,
                                    onValueChange = { value ->
                                        viewModel.updateAiVisualReferenceHint(index, value)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp),
                                    label = { Text("Referenz ${index + 1}") },
                                    placeholder = {
                                        Text("z. B. Charakter, Outfit, Shot, Element oder Mood")
                                    },
                                    minLines = 2,
                                    maxLines = 3,
                                )
                            }

                            Text(
                                text = "Workflow Google Verbindung",
                                modifier = Modifier.padding(top = 18.dp),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            SettingsToggleRow(
                                title = "Google fuer Automationen separat halten",
                                body = "Das normale Google-Login der App bleibt getrennt von Google fuer spaetere n8n-, Drive-, Sheets- oder Calendar-Automationen.",
                                checked = uiState.workflowAutomationSettings.keepsGoogleSeparate,
                                onCheckedChange = viewModel::updateWorkflowKeepsGoogleSeparate,
                                modifier = Modifier.padding(top = 10.dp),
                            )
                            SettingsToggleRow(
                                title = "Automation-Google vorbereitet",
                                body = "Markiert, dass ein separates Google-Konto fuer Workflows spaeter angebunden werden soll.",
                                checked = uiState.workflowAutomationSettings.isPrepared,
                                onCheckedChange = viewModel::updateWorkflowPrepared,
                                modifier = Modifier.padding(top = 10.dp),
                            )
                            OutlinedTextField(
                                value = uiState.workflowAutomationSettings.googleAccountHint,
                                onValueChange = viewModel::updateWorkflowGoogleAccountHint,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                label = { Text("Automation Google Konto") },
                                placeholder = { Text("z. B. automation@deinedomain.de") },
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = uiState.workflowAutomationSettings.googleScopeHint,
                                onValueChange = viewModel::updateWorkflowGoogleScopeHint,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                label = { Text("Google Scope / Einsatz") },
                                placeholder = { Text("z. B. Drive, Sheets, Calendar") },
                                singleLine = true,
                            )
                        }
                    }
                }

                if (uiState.isAdmin) {
                    item {
                        SkydownCard(contentPadding = PaddingValues(18.dp)) {
                            SectionHeader("Zahlungen")
                            Text(
                                text = "PayPal und Bankueberweisung kannst du sofort als manuellen Checkout-Handoff nutzen. Stripe und Klarna bleiben vorerst vorbereitete Live-Provider fuer spaeter.",
                                modifier = Modifier.padding(top = 8.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            )

                            PaymentProviderAdminCard(
                                title = "Stripe",
                                connected = uiState.paymentMethods.stripe.connected,
                                enabledInCheckout = uiState.paymentMethods.stripe.connected &&
                                    uiState.paymentMethods.stripe.enabled,
                                accountHint = stripeAccountHintDraft,
                                accountHintLabel = "Stripe Konto / Workspace",
                                accountHintPlaceholder = "z. B. Skydown Merch Workspace",
                                onAccountHintChange = { stripeAccountHintDraft = it },
                                onSaveConnection = { viewModel.connectStripe(stripeAccountHintDraft) },
                                onDisconnect = if (uiState.paymentMethods.stripe.connected) {
                                    { viewModel.disconnectStripe() }
                                } else {
                                    null
                                },
                                onToggleEnabled = viewModel::setStripeEnabled,
                                modifier = Modifier.padding(top = 16.dp),
                            )

                            PaymentProviderAdminCard(
                                title = "PayPal",
                                connected = uiState.paymentMethods.paypal.connected,
                                enabledInCheckout = uiState.paymentMethods.paypal.connected &&
                                    uiState.paymentMethods.paypal.enabled,
                                accountHint = paypalAccountHintDraft,
                                accountHintLabel = "PayPal.Me Link oder Business-Mail",
                                accountHintPlaceholder = "z. B. https://paypal.me/deinname",
                                onAccountHintChange = { paypalAccountHintDraft = it },
                                onSaveConnection = { viewModel.connectPayPal(paypalAccountHintDraft) },
                                onDisconnect = if (uiState.paymentMethods.paypal.connected) {
                                    { viewModel.disconnectPayPal() }
                                } else {
                                    null
                                },
                                onToggleEnabled = viewModel::setPayPalEnabled,
                                modifier = Modifier.padding(top = 14.dp),
                            )

                            PaymentProviderAdminCard(
                                title = "Klarna",
                                connected = uiState.paymentMethods.klarna.connected,
                                enabledInCheckout = uiState.paymentMethods.klarna.connected &&
                                    uiState.paymentMethods.klarna.enabled,
                                accountHint = klarnaAccountHintDraft,
                                accountHintLabel = "Klarna Merchant / Store ID",
                                accountHintPlaceholder = "z. B. Klarna Merchant EU",
                                onAccountHintChange = { klarnaAccountHintDraft = it },
                                onSaveConnection = { viewModel.connectKlarna(klarnaAccountHintDraft) },
                                onDisconnect = if (uiState.paymentMethods.klarna.connected) {
                                    { viewModel.disconnectKlarna() }
                                } else {
                                    null
                                },
                                onToggleEnabled = viewModel::setKlarnaEnabled,
                                modifier = Modifier.padding(top = 14.dp),
                            )

                            BankTransferAdminCard(
                                configured = uiState.paymentMethods.bankTransfer.isConfigured,
                                enabledInCheckout = uiState.paymentMethods.bankTransfer.enabled &&
                                    uiState.paymentMethods.bankTransfer.isConfigured,
                                accountHolder = bankAccountHolderDraft,
                                iban = bankIbanDraft,
                                bic = bankBicDraft,
                                bankName = bankNameDraft,
                                paymentInstructions = bankInstructionsDraft,
                                onAccountHolderChange = { bankAccountHolderDraft = it },
                                onIbanChange = { bankIbanDraft = it },
                                onBicChange = { bankBicDraft = it },
                                onBankNameChange = { bankNameDraft = it },
                                onPaymentInstructionsChange = { bankInstructionsDraft = it },
                                onSave = {
                                    viewModel.saveBankTransfer(
                                        accountHolder = bankAccountHolderDraft,
                                        iban = bankIbanDraft,
                                        bic = bankBicDraft,
                                        bankName = bankNameDraft,
                                        paymentInstructions = bankInstructionsDraft,
                                    )
                                },
                                onToggleEnabled = viewModel::setBankTransferEnabled,
                                modifier = Modifier.padding(top = 14.dp),
                            )
                        }
                    }

                    item {
                        SkydownCard(contentPadding = PaddingValues(18.dp)) {
                            SectionHeader("Versand & Rechnung")
                            Text(
                                text = "Der Checkout nutzt diese Werte direkt fuer Versand, MwSt.-Ausweisung und vorbereitete Bestellsummen. Der Store-Schalter aus Merchandise bleibt dabei die harte Freigabe fuer Kunden.",
                                modifier = Modifier.padding(top = 8.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            )

                            Text(
                                text = "Versand",
                                modifier = Modifier.padding(top = 16.dp),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )

                            OutlinedTextField(
                                value = domesticShippingDraft,
                                onValueChange = { domesticShippingDraft = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                label = { Text("Versand Deutschland (EUR)") },
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = internationalShippingDraft,
                                onValueChange = { internationalShippingDraft = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                label = { Text("Versand International (EUR)") },
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = freeShippingThresholdDraft,
                                onValueChange = { freeShippingThresholdDraft = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                label = { Text("Versand frei ab (EUR)") },
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = shippingNotesDraft,
                                onValueChange = { shippingNotesDraft = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                label = { Text("Versandhinweis") },
                                minLines = 2,
                                maxLines = 3,
                            )

                            Text(
                                text = "Rechnung",
                                modifier = Modifier.padding(top = 18.dp),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )

                            OutlinedTextField(
                                value = invoiceCompanyNameDraft,
                                onValueChange = { invoiceCompanyNameDraft = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                label = { Text("Firmenname") },
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = invoiceCompanyAddressDraft,
                                onValueChange = { invoiceCompanyAddressDraft = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                label = { Text("Firmenadresse") },
                                minLines = 2,
                                maxLines = 3,
                            )
                            OutlinedTextField(
                                value = invoiceTaxNumberDraft,
                                onValueChange = { invoiceTaxNumberDraft = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                label = { Text("Steuernummer") },
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = invoiceVatIdDraft,
                                onValueChange = { invoiceVatIdDraft = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                label = { Text("USt-IdNr.") },
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = invoiceTaxRateDraft,
                                onValueChange = { invoiceTaxRateDraft = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                label = { Text("MwSt. Satz (%)") },
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = invoicePrefixDraft,
                                onValueChange = { invoicePrefixDraft = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                label = { Text("Rechnungs-Praefix") },
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = invoiceSupportEmailDraft,
                                onValueChange = { invoiceSupportEmailDraft = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                label = { Text("Support / Rechnungs-Mail") },
                                singleLine = true,
                            )

                            Button(
                                onClick = {
                                    val domesticCost = domesticShippingDraft.parseDecimalInput()
                                        ?: uiState.commerceSettings.shipping.domesticCost
                                    val internationalCost = internationalShippingDraft.parseDecimalInput()
                                        ?: uiState.commerceSettings.shipping.internationalCost
                                    val freeShippingThreshold = freeShippingThresholdDraft.parseDecimalInput()
                                        ?: uiState.commerceSettings.shipping.freeShippingThreshold
                                    val taxRate = invoiceTaxRateDraft.parseDecimalInput()
                                        ?: uiState.commerceSettings.invoice.taxRate

                                    viewModel.saveCommerceSettings(
                                        uiState.commerceSettings.copy(
                                            shipping = uiState.commerceSettings.shipping.copy(
                                                domesticCost = domesticCost.coerceAtLeast(0.0),
                                                internationalCost = internationalCost.coerceAtLeast(0.0),
                                                freeShippingThreshold = freeShippingThreshold.coerceAtLeast(0.0),
                                                shippingNotes = shippingNotesDraft.trim(),
                                            ),
                                            invoice = uiState.commerceSettings.invoice.copy(
                                                companyName = invoiceCompanyNameDraft.trim(),
                                                companyAddress = invoiceCompanyAddressDraft.trim(),
                                                taxNumber = invoiceTaxNumberDraft.trim(),
                                                vatId = invoiceVatIdDraft.trim(),
                                                taxRate = taxRate.coerceAtLeast(0.0),
                                                invoicePrefix = invoicePrefixDraft.trim(),
                                                supportEmail = invoiceSupportEmailDraft.trim(),
                                            ),
                                        ),
                                        successMessage = "Versand- und Rechnungsdaten gespeichert.",
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 14.dp),
                                shape = RoundedCornerShape(18.dp),
                            ) {
                                Text("Versand & Rechnung speichern")
                            }
                        }
                    }
                }

                item {
                    SkydownCard(contentPadding = PaddingValues(18.dp)) {
                        SectionHeader("Allgemein")
                        Text(
                            text = "Sprache: ${uiState.language}",
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        )
                        SettingsToggleRow(
                            title = "Benachrichtigungen",
                            body = "Push-Hinweise fuer Updates und wichtige App-Aktionen.",
                            checked = uiState.notificationsEnabled,
                            onCheckedChange = viewModel::updateNotifications,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }

                item {
                    SkydownCard(contentPadding = PaddingValues(18.dp)) {
                        SectionHeader("Anzeige")
                        Text(
                            text = "Aktuell: ${uiState.colorScheme.label}",
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        )
                        AppearanceMode.entries.forEach { scheme ->
                            AppearanceChoiceRow(
                                title = scheme.label,
                                selected = uiState.colorScheme == scheme,
                                onClick = { viewModel.updateColorScheme(scheme) },
                                modifier = Modifier.padding(top = 10.dp),
                            )
                        }
                    }
                }

                item {
                    SkydownCard(contentPadding = PaddingValues(18.dp)) {
                        SectionHeader("App-Info")
                        Text(
                            text = "Version ${uiState.appVersion}",
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        OutlinedButton(
                            onClick = {
                                activeLegalDocument.value = SettingsLegalDocumentType.PrivacyPolicy
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            shape = RoundedCornerShape(18.dp),
                        ) {
                            Text("Datenschutzbestimmungen")
                        }
                        OutlinedButton(
                            onClick = {
                                activeLegalDocument.value = SettingsLegalDocumentType.TermsOfService
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            shape = RoundedCornerShape(18.dp),
                        ) {
                            Text("Nutzungsbedingungen")
                        }
                        Text(
                            text = "Support",
                            modifier = Modifier.padding(top = 12.dp),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = "skydownent@gmail.com",
                            modifier = Modifier.padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        )
                        Button(
                            onClick = {
                                openSupportEmail(
                                    context = context,
                                    userEmail = uiState.email,
                                    userName = uiState.username,
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            shape = RoundedCornerShape(18.dp),
                        ) {
                            Text("Support-Anfrage senden")
                        }
                        Text(
                            text = "Support und rechtliche Hinweise sind hier direkt aus der App erreichbar.",
                            modifier = Modifier.padding(top = 10.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        )
                    }
                }
            }
        }
    }

    activeLegalDocument.value?.let { documentType ->
        SettingsLegalDocumentSheet(
            documentType = documentType,
            onDismiss = { activeLegalDocument.value = null },
        )
    }

    if (showDeleteAccountDialog.value) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog.value = false },
            title = {
                Text("Konto loeschen")
            },
            text = {
                Text("Moechtest du dein Konto unwiderruflich loeschen?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteAccountDialog.value = false
                        viewModel.deleteAccount()
                    },
                    enabled = !uiState.isDeletingAccount,
                ) {
                    Text("Konto loeschen", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteAccountDialog.value = false },
                ) {
                    Text("Abbrechen")
                }
            },
        )
    }
}

@Composable
private fun PaymentProviderAdminCard(
    title: String,
    connected: Boolean,
    enabledInCheckout: Boolean,
    accountHint: String,
    accountHintLabel: String,
    accountHintPlaceholder: String,
    onAccountHintChange: (String) -> Unit,
    onSaveConnection: () -> Unit,
    onDisconnect: (() -> Unit)?,
    onToggleEnabled: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    SkydownCard(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
    ) {
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
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = when (title) {
                        "PayPal" -> if (connected) "Hinterlegt" else "Noch nicht hinterlegt"
                        else -> if (connected) "Verbunden" else "Nicht verbunden"
                    },
                    color = if (connected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    },
                )
            }
            SettingsBadge(
                text = if (enabledInCheckout) "Im Checkout sichtbar" else "Ausgeblendet",
                icon = Icons.Default.CheckCircle,
                isActive = enabledInCheckout,
            )
        }

        OutlinedTextField(
            value = accountHint,
            onValueChange = onAccountHintChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            label = { Text(accountHintLabel) },
            placeholder = { Text(accountHintPlaceholder) },
            singleLine = true,
        )

        SettingsToggleRow(
            title = "Fuer Kunden im Checkout anzeigen",
            body = "Erst nach der Verbindung sichtbar schalten.",
            checked = enabledInCheckout,
            onCheckedChange = onToggleEnabled,
            modifier = Modifier.padding(top = 12.dp),
            enabled = connected,
        )

        Row(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = onSaveConnection,
                shape = RoundedCornerShape(18.dp),
            ) {
                Text(
                    when (title) {
                        "PayPal" -> if (connected) "PayPal aktualisieren" else "PayPal hinterlegen"
                        else -> if (connected) "Verbindung aktualisieren" else "Verbinden"
                    },
                )
            }
            onDisconnect?.let { disconnect ->
                OutlinedButton(
                    onClick = disconnect,
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text(if (title == "PayPal") "Entfernen" else "Trennen")
                }
            }
        }
    }
}

@Composable
private fun BankTransferAdminCard(
    configured: Boolean,
    enabledInCheckout: Boolean,
    accountHolder: String,
    iban: String,
    bic: String,
    bankName: String,
    paymentInstructions: String,
    onAccountHolderChange: (String) -> Unit,
    onIbanChange: (String) -> Unit,
    onBicChange: (String) -> Unit,
    onBankNameChange: (String) -> Unit,
    onPaymentInstructionsChange: (String) -> Unit,
    onSave: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    SkydownCard(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
    ) {
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
                    text = "Bankueberweisung",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (configured) "Bankdaten hinterlegt" else "Noch nicht hinterlegt",
                    color = if (configured) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    },
                )
            }
            SettingsBadge(
                text = if (enabledInCheckout) "Im Checkout sichtbar" else "Ausgeblendet",
                icon = Icons.Default.CheckCircle,
                isActive = enabledInCheckout,
            )
        }

        OutlinedTextField(
            value = accountHolder,
            onValueChange = onAccountHolderChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            label = { Text("Kontoinhaber") },
            singleLine = true,
        )
        OutlinedTextField(
            value = iban,
            onValueChange = onIbanChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            label = { Text("IBAN") },
            singleLine = true,
        )
        OutlinedTextField(
            value = bic,
            onValueChange = onBicChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            label = { Text("BIC") },
            singleLine = true,
        )
        OutlinedTextField(
            value = bankName,
            onValueChange = onBankNameChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            label = { Text("Bankname") },
            singleLine = true,
        )
        OutlinedTextField(
            value = paymentInstructions,
            onValueChange = onPaymentInstructionsChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            label = { Text("Zahlungsanweisung") },
            minLines = 2,
            maxLines = 3,
        )

        SettingsToggleRow(
            title = "Fuer Kunden im Checkout anzeigen",
            body = "Erst aktivieren, wenn die Bankdaten vollstaendig sind.",
            checked = enabledInCheckout,
            onCheckedChange = onToggleEnabled,
            modifier = Modifier.padding(top = 12.dp),
            enabled = configured,
        )

        Button(
            onClick = onSave,
            modifier = Modifier.padding(top = 12.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text(if (configured) "Bankdaten aktualisieren" else "Bankdaten hinterlegen")
        }
    }
}

@Composable
private fun SettingsOverviewCard(
    uiState: SettingsUiState,
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
                    text = if (uiState.isLoggedIn) uiState.username else "Skydown Einstellungen",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Android zieht hier mit iOS gleich: Konto, Rechtliches, Anzeige und Support bleiben in einem klaren Flow gebuendelt.",
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
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        Row(
            modifier = Modifier.padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SettingsBadge(
                text = if (uiState.isLoggedIn) "Konto aktiv" else "Gast",
                icon = Icons.Default.Person,
                isActive = uiState.isLoggedIn,
            )
            SettingsBadge(
                text = if (uiState.notificationsEnabled) "Hinweise an" else "Hinweise aus",
                icon = Icons.Default.Notifications,
                isActive = uiState.notificationsEnabled,
            )
            SettingsBadge(
                text = uiState.colorScheme.label,
                icon = Icons.Default.Palette,
                isActive = true,
            )
        }

        if (uiState.isAdmin) {
            SettingsBadge(
                text = "Admin aktiv",
                icon = Icons.Default.CheckCircle,
                isActive = true,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsLegalDocumentSheet(
    documentType: SettingsLegalDocumentType,
    onDismiss: () -> Unit,
) {
    val document = documentType.resolve()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        LazyColumn(
            contentPadding = PaddingValues(
                start = 20.dp,
                top = com.skydown.android.ui.component.SkydownUiTokens.screenTopPadding,
                end = 20.dp,
                bottom = 36.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = document.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Zuletzt aktualisiert: ${document.updatedAt}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                    )
                    Text(
                        text = document.introduction,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
                    )
                }
            }

            items(document.sections) { section ->
                SkydownCard(contentPadding = PaddingValues(18.dp)) {
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = section.body,
                        modifier = Modifier.padding(top = 10.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
                    )
                }
            }

            item {
                SkydownCard(contentPadding = PaddingValues(18.dp)) {
                    Text(
                        text = "Kontakt",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "skydownent@gmail.com",
                        modifier = Modifier.padding(top = 10.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    body: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}

@Composable
private fun AppearanceChoiceRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
            )
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
        RadioButton(
            selected = selected,
            onClick = onClick,
        )
    }
}

@Composable
private fun SettingsBadge(
    text: String,
    icon: ImageVector,
    isActive: Boolean,
    modifier: Modifier = Modifier,
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
        modifier = modifier
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

private fun openSupportEmail(
    context: Context,
    userEmail: String,
    userName: String,
) {
    val subject = if (userEmail.isNotBlank()) {
        "Support-Anfrage - $userEmail"
    } else {
        "Support-Anfrage"
    }
    val body = """
        Hallo Skydown-Team,

        ich habe folgende Anfrage:

        Eingeloggter Account: ${userName.ifBlank { "Nicht verfuegbar" }}
        Account-E-Mail: ${userEmail.ifBlank { "Nicht verfuegbar" }}

        Nachricht:
    """.trimIndent()
    openEmailDraft(
        context = context,
        recipients = listOf("skydownent@gmail.com"),
        subject = subject,
        body = body,
    )
}

private fun String.parseDecimalInput(): Double? {
    return trim().replace(",", ".").toDoubleOrNull()
}

private fun formatDecimalDraft(value: Double, decimals: Int = 2): String {
    return String.format(Locale.US, "%.${decimals}f", value)
}
