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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
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
import com.skydown.shared.model.User
import com.skydown.shared.model.UserQuotaPlan
import com.skydown.shared.model.canManageMusic
import com.skydown.shared.model.canManageVideos
import com.skydown.shared.model.canModerateUserProfiles
import com.skydown.shared.model.UserRole
import com.skydown.shared.model.isPlatformOwner
import com.skydown.shared.model.resolvedAiAgentRequestsPerDay
import com.skydown.shared.model.resolvedAiHistoryRetentionDays
import com.skydown.shared.model.resolvedAiTextRequestsPerDay
import com.skydown.shared.model.resolvedAiVisualRequestsPerDay
import com.skydown.shared.model.resolvedQuotaPlan
import com.skydown.shared.model.resolvedRole
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onClose: (() -> Unit)? = null,
    onOpenLogin: () -> Unit = {},
    onOpenRegistration: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
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
    var euShippingDraft by rememberSaveable { mutableStateOf("") }
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
    var shopifyStoreDomainDraft by rememberSaveable { mutableStateOf("") }
    var shopifyStorefrontAccessTokenDraft by rememberSaveable { mutableStateOf("") }
    var shopifyCollectionHandleDraft by rememberSaveable { mutableStateOf("") }
    var automationEnabledDraft by rememberSaveable { mutableStateOf(false) }
    var automationSendsUserContextDraft by rememberSaveable { mutableStateOf(true) }
    var automationWorkflowNameDraft by rememberSaveable { mutableStateOf("") }
    var automationBaseUrlDraft by rememberSaveable { mutableStateOf("") }
    var automationWebhookPathDraft by rememberSaveable { mutableStateOf("") }
    var automationAuthHeaderNameDraft by rememberSaveable { mutableStateOf("") }
    var automationAuthHeaderValueDraft by rememberSaveable { mutableStateOf("") }
    var profileUsernameDraft by rememberSaveable { mutableStateOf("") }
    var profileWhatsAppDraft by rememberSaveable { mutableStateOf("") }
    var profileTaglineDraft by rememberSaveable { mutableStateOf("") }
    var profileBioDraft by rememberSaveable { mutableStateOf("") }
    var profileInstagramHandleDraft by rememberSaveable { mutableStateOf("") }
    val activeLegalDocument = rememberSaveable {
        mutableStateOf<SettingsLegalDocumentType?>(null)
    }
    val showDeleteAccountDialog = rememberSaveable {
        mutableStateOf(false)
    }
    var activeAdminWorkspaceKey by rememberSaveable { mutableStateOf(AdminWorkspaceSection.Overview.name) }
    val activeAdminWorkspace = AdminWorkspaceSection.valueOf(activeAdminWorkspaceKey)
    var showAdminWorkspaceSheet by rememberSaveable { mutableStateOf(false) }
    val connectedPaymentMethodCount = listOf(
        uiState.paymentMethods.stripe.connected,
        uiState.paymentMethods.paypal.connected,
        uiState.paymentMethods.klarna.connected,
        uiState.paymentMethods.bankTransfer.isConfigured,
    ).count { it }
    val visiblePaymentMethodCount = listOf(
        uiState.paymentMethods.stripe.connected && uiState.paymentMethods.stripe.enabled,
        uiState.paymentMethods.paypal.connected && uiState.paymentMethods.paypal.enabled,
        uiState.paymentMethods.klarna.connected && uiState.paymentMethods.klarna.enabled,
        uiState.paymentMethods.bankTransfer.isConfigured && uiState.paymentMethods.bankTransfer.enabled,
    ).count { it }

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
        euShippingDraft = formatDecimalDraft(uiState.commerceSettings.shipping.euCost)
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

    LaunchedEffect(uiState.shopifyAdminSettings) {
        shopifyStoreDomainDraft = uiState.shopifyAdminSettings.storeDomain
        shopifyStorefrontAccessTokenDraft = uiState.shopifyAdminSettings.storefrontAccessToken
        shopifyCollectionHandleDraft = uiState.shopifyAdminSettings.collectionHandle
    }

    LaunchedEffect(uiState.workflowAutomationSettings) {
        automationEnabledDraft = uiState.workflowAutomationSettings.isEnabled
        automationSendsUserContextDraft = uiState.workflowAutomationSettings.sendsUserContext
        automationWorkflowNameDraft = uiState.workflowAutomationSettings.workflowName
        automationBaseUrlDraft = uiState.workflowAutomationSettings.baseUrl
        automationWebhookPathDraft = uiState.workflowAutomationSettings.webhookPath
        automationAuthHeaderNameDraft = uiState.workflowAutomationSettings.authHeaderName
        automationAuthHeaderValueDraft = uiState.workflowAutomationSettings.authHeaderValue
    }

    LaunchedEffect(
        uiState.currentUserId,
        uiState.username,
        uiState.whatsApp,
        uiState.profileTagline,
        uiState.profileBio,
        uiState.instagramHandle,
    ) {
        profileUsernameDraft = uiState.username
        profileWhatsAppDraft = uiState.whatsApp
        profileTaglineDraft = uiState.profileTagline
        profileBioDraft = uiState.profileBio
        profileInstagramHandleDraft = uiState.instagramHandle
    }

    LaunchedEffect(uiState.paymentFeedbackMessage) {
        val message = uiState.paymentFeedbackMessage ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        viewModel.clearPaymentFeedback()
    }

    val adminWorkspaceContent: @Composable (AdminWorkspaceSection) -> Unit = { section ->
        AdminWorkspaceSummaryCard(section = section)

        when (section) {
            AdminWorkspaceSection.Overview -> {
                Text(
                    text = "Heute im Blick",
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                LazyRow(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        SettingsBadge(
                            text = "$connectedPaymentMethodCount Zahlarten verbunden",
                            icon = Icons.Default.CheckCircle,
                            isActive = connectedPaymentMethodCount > 0,
                        )
                    }
                    item {
                        SettingsBadge(
                            text = "$visiblePaymentMethodCount im Checkout sichtbar",
                            icon = Icons.Default.CheckCircle,
                            isActive = visiblePaymentMethodCount > 0,
                        )
                    }
                    item {
                        SettingsBadge(
                            text = if (uiState.shopifyAdminSettings.hasCollectionFilter) {
                                "Shopify: ${uiState.shopifyAdminSettings.activeCollectionLabel}"
                            } else {
                                "Shopify: Gesamter Store"
                            },
                            icon = Icons.Default.ShoppingBag,
                            isActive = true,
                        )
                    }
                    item {
                        SettingsBadge(
                            text = if (uiState.aiVisualReferenceLibrary.isEnabled) "Visuals aktiv" else "Visuals aus",
                            icon = Icons.Default.Palette,
                            isActive = uiState.aiVisualReferenceLibrary.isEnabled,
                        )
                    }
                    item {
                        SettingsBadge(
                            text = if (uiState.workflowAutomationSettings.isPrepared) "n8n bereit" else "n8n offen",
                            icon = Icons.Default.Settings,
                            isActive = uiState.workflowAutomationSettings.isPrepared,
                        )
                    }
                    item {
                        SettingsBadge(
                            text = "${uiState.managedUsers.size} Konten",
                            icon = Icons.Default.Person,
                            isActive = uiState.managedUsers.isNotEmpty(),
                        )
                    }
                }
                Text(
                    text = "Jeder Bereich oeffnet sich jetzt separat. So bleibt die Settings-Seite kurz und du bist schneller direkt in der passenden Aufgabe.",
                    modifier = Modifier.padding(top = 12.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
            }

            AdminWorkspaceSection.Users -> {
                Text(
                    text = "Hier steuerst du, welche Konten normaler User, Subadmin, Admin oder Owner sind. Gleichzeitig legst du fest, ob KI fuer ein Konto aktiv ist und wie hoch die Tageslimits fuer Bot, Visuals und Agent liegen.",
                    modifier = Modifier.padding(top = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )

                LazyRow(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        SettingsBadge(
                            text = "4 Rollen",
                            icon = Icons.Default.Person,
                            isActive = true,
                        )
                    }
                    item {
                        SettingsBadge(
                            text = "${uiState.managedUsers.size} Konten",
                            icon = Icons.Default.Person,
                            isActive = uiState.managedUsers.isNotEmpty(),
                        )
                    }
                }

                AdminUserRoleGuideCard(
                    modifier = Modifier.padding(top = 14.dp),
                )

                uiState.managedUsersErrorMessage?.let { message ->
                    Text(
                        text = message,
                        modifier = Modifier.padding(top = 12.dp),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                if (uiState.managedUsers.isEmpty()) {
                    Text(
                        text = "Sobald weitere Konten in der App registriert sind, erscheinen sie hier direkt zur Rollen- und KI-Verwaltung.",
                        modifier = Modifier.padding(top = 12.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    )
                } else {
                    Column(
                        modifier = Modifier.padding(top = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        uiState.managedUsers.forEach { managedUser ->
                            AdminManagedUserCard(
                                user = managedUser,
                                currentUserId = uiState.currentUserId,
                                onSave = viewModel::saveManagedUser,
                            )
                        }
                    }
                }
            }

            AdminWorkspaceSection.Shopify -> {
                Text(
                    text = "Fuer den Merch-Katalog pflegt der Owner hier die Store-Domain, den Storefront Access Token und optional einen Collection-Handle. Danach laedt der Shop direkt aus Shopify.",
                    modifier = Modifier.padding(top = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )

                Text(
                    text = "Shopify Quelle",
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )

                OutlinedTextField(
                    value = shopifyStoreDomainDraft,
                    onValueChange = { shopifyStoreDomainDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    label = { Text("Store-Domain") },
                    placeholder = { Text("k5t1sc-ps.myshopify.com") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = shopifyStorefrontAccessTokenDraft,
                    onValueChange = { shopifyStorefrontAccessTokenDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("Storefront Access Token") },
                    placeholder = { Text("shpat_... oder storefront token") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = shopifyCollectionHandleDraft,
                    onValueChange = { shopifyCollectionHandleDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("Collection-Handle") },
                    placeholder = { Text("z. B. spring-drop-2026") },
                    singleLine = true,
                )

                Text(
                    text = "Den Collection-Handle kannst du leer lassen, dann nimmt die App den ganzen veroeffentlichten Store.",
                    modifier = Modifier.padding(top = 10.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )

                Text(
                    text = if (uiState.shopifyAdminSettings.hasCollectionFilter) {
                        "Aktuell aktiv: ${uiState.shopifyAdminSettings.activeCollectionLabel}"
                    } else {
                        "Aktuell aktiv: gesamter Shopify-Store"
                    },
                    modifier = Modifier.padding(top = 12.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )

                Button(
                    onClick = {
                        viewModel.saveShopifyAdminSettings(
                            uiState.shopifyAdminSettings.copy(
                                storeDomain = shopifyStoreDomainDraft.trim(),
                                storefrontAccessToken = shopifyStorefrontAccessTokenDraft.trim(),
                                collectionHandle = shopifyCollectionHandleDraft.trim(),
                            ),
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text("Shopify speichern")
                }
            }

            AdminWorkspaceSection.Payments -> {
                Text(
                    text = "PayPal und Bankueberweisung laufen als manueller Owner-Handoff. Stripe ist als sicherer Live-Checkout aktiv, und Klarna laeuft live ueber Stripe, sobald es im Stripe-Dashboard freigeschaltet und serverseitig konfiguriert ist.",
                    modifier = Modifier.padding(top = 16.dp),
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

            AdminWorkspaceSection.Commerce -> {
                Text(
                    text = "Der Checkout nutzt diese Werte direkt fuer Versand, MwSt.-Ausweisung und vorbereitete Bestellsummen. Der Store-Schalter aus Merchandise bleibt dabei die harte Freigabe fuer Kunden.",
                    modifier = Modifier.padding(top = 16.dp),
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
                    value = euShippingDraft,
                    onValueChange = { euShippingDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("Versand EU (EUR)") },
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
                        val euCost = euShippingDraft.parseDecimalInput()
                            ?: uiState.commerceSettings.shipping.euCost
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
                                    euCost = euCost.coerceAtLeast(0.0),
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

            AdminWorkspaceSection.Visuals -> {
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
            }

            AdminWorkspaceSection.Automation -> {
                Text(
                    text = "n8n Verbindung",
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                SettingsToggleRow(
                    title = "n8n aktiv",
                    body = "Die App bleibt normal ueber Firebase eingeloggt. Der Owner hinterlegt hier die zentrale n8n-Verbindung.",
                    checked = automationEnabledDraft,
                    onCheckedChange = { automationEnabledDraft = it },
                    modifier = Modifier.padding(top = 10.dp),
                )
                SettingsToggleRow(
                    title = "App-User-Kontext mitsenden",
                    body = "UID, E-Mail und Username werden serverseitig geprueft und an n8n uebergeben.",
                    checked = automationSendsUserContextDraft,
                    onCheckedChange = { automationSendsUserContextDraft = it },
                    modifier = Modifier.padding(top = 10.dp),
                )
                OutlinedTextField(
                    value = automationWorkflowNameDraft,
                    onValueChange = { automationWorkflowNameDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    label = { Text("Workflow Name") },
                    placeholder = { Text("z. B. AI Script Pipeline") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = automationBaseUrlDraft,
                    onValueChange = { automationBaseUrlDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("n8n Base URL") },
                    placeholder = { Text("https://n8n.deinedomain.de") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = automationWebhookPathDraft,
                    onValueChange = { automationWebhookPathDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("Webhook Path") },
                    placeholder = { Text("webhook/skydown-app") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = automationAuthHeaderNameDraft,
                    onValueChange = { automationAuthHeaderNameDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("Auth Header Name") },
                    placeholder = { Text("z. B. X-Skydown-Automation-Key") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = automationAuthHeaderValueDraft,
                    onValueChange = { automationAuthHeaderValueDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("Auth Header Value") },
                    placeholder = { Text("optional") },
                    singleLine = true,
                )

                val resolvedWebhookUrl = resolveAutomationDraftWebhookUrl(
                    baseUrl = automationBaseUrlDraft,
                    webhookPath = automationWebhookPathDraft,
                )

                Text(
                    text = resolvedWebhookUrl?.let { "Webhook: $it" } ?: "Webhook noch nicht vollstaendig",
                    modifier = Modifier.padding(top = 12.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    val updatedAutomationSettings = uiState.workflowAutomationSettings.copy(
                        isEnabled = automationEnabledDraft,
                        sendsUserContext = automationSendsUserContextDraft,
                        workflowName = automationWorkflowNameDraft.trim(),
                        baseUrl = automationBaseUrlDraft.trim(),
                        webhookPath = automationWebhookPathDraft.trim(),
                        authHeaderName = automationAuthHeaderNameDraft.trim(),
                        authHeaderValue = automationAuthHeaderValueDraft.trim(),
                    )

                    Button(
                        onClick = {
                            viewModel.saveWorkflowAutomationSettings(updatedAutomationSettings)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Text("n8n speichern")
                    }

                    OutlinedButton(
                        onClick = { viewModel.testWorkflowAutomationSettings(updatedAutomationSettings) },
                        modifier = Modifier.weight(1f),
                        enabled = automationEnabledDraft && resolvedWebhookUrl != null,
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Text("Test senden")
                    }
                }
            }
        }
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
                            Button(
                                onClick = onOpenProfile,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                shape = RoundedCornerShape(18.dp),
                            ) {
                                Text("Profil bearbeiten")
                            }
                            Text(
                                text = "Kontoaktionen",
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
                                    Text("Anderes Konto")
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
                        SectionHeader("Owner")
                        Text(
                            text = if (uiState.isOwner) {
                                "Diese Systembereiche gehoeren jetzt allein zum Owner-Konto. Shopify, Zahlarten, Versand, Nutzerrollen und n8n laufen damit bewusst ueber eine zentrale Hand."
                            } else {
                                "Die Systembereiche sind nur fuer das feste Owner-Konto aktiv."
                            },
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        )
                        OutlinedButton(
                            onClick = onOpenOrders,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            enabled = uiState.isOwner,
                            shape = RoundedCornerShape(18.dp),
                        ) {
                            Text("Bestellungen oeffnen")
                        }

                        if (uiState.isOwner) {
                            Column(
                                modifier = Modifier.padding(top = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                AdminWorkspaceSection.entries.forEach { section ->
                                    AdminWorkspaceListRow(
                                        section = section,
                                        detailText = adminWorkspaceStatusText(
                                            section = section,
                                            uiState = uiState,
                                            connectedPaymentMethodCount = connectedPaymentMethodCount,
                                            visiblePaymentMethodCount = visiblePaymentMethodCount,
                                        ),
                                        onClick = {
                                            activeAdminWorkspaceKey = section.name
                                            showAdminWorkspaceSheet = true
                                        },
                                    )
                                }
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
                                activeLegalDocument.value = SettingsLegalDocumentType.TermsAndConditions
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            shape = RoundedCornerShape(18.dp),
                        ) {
                            Text("AGB")
                        }
                        OutlinedButton(
                            onClick = {
                                activeLegalDocument.value = SettingsLegalDocumentType.PrivacyPolicy
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
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
                            text = "Rechtstexte und Support-Infos sind hier direkt aus der App erreichbar.",
                            modifier = Modifier.padding(top = 10.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        )
                    }
                }
            }
        }
    }

    if (showAdminWorkspaceSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAdminWorkspaceSheet = false },
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = activeAdminWorkspace.label,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                adminWorkspaceContent(activeAdminWorkspace)
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
private fun ProfileEditorCard(
    username: String,
    whatsApp: String,
    profileTagline: String,
    profileBio: String,
    instagramHandle: String,
    isSaving: Boolean,
    onUsernameChange: (String) -> Unit,
    onWhatsAppChange: (String) -> Unit,
    onProfileTaglineChange: (String) -> Unit,
    onProfileBioChange: (String) -> Unit,
    onInstagramHandleChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Profil",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Username, Kurzinfo und Links.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
            }
        }

        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Benutzername") },
            singleLine = true,
        )
        OutlinedTextField(
            value = profileTagline,
            onValueChange = onProfileTaglineChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Kurzinfo") },
            singleLine = true,
        )
        OutlinedTextField(
            value = profileBio,
            onValueChange = onProfileBioChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Bio") },
            minLines = 3,
            maxLines = 5,
        )
        OutlinedTextField(
            value = instagramHandle,
            onValueChange = onInstagramHandleChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Instagram") },
            singleLine = true,
        )
        OutlinedTextField(
            value = whatsApp,
            onValueChange = onWhatsAppChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("WhatsApp") },
            singleLine = true,
        )

        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving,
            shape = RoundedCornerShape(18.dp),
        ) {
            Text(if (isSaving) "Profil wird gespeichert..." else "Profil speichern")
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

        if (uiState.isOwner) {
            SettingsBadge(
                text = "Owner aktiv",
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

private enum class AdminWorkspaceSection(
    val label: String,
    val subtitle: String,
    val icon: ImageVector,
) {
    Overview(
        label = "Uebersicht",
        subtitle = "Schneller Status fuer Owner-Bereiche und Systemverbindungen.",
        icon = Icons.Default.Settings,
    ),
    Payments(
        label = "Zahlungen",
        subtitle = "Provider verbinden und fuer den Checkout sichtbar schalten.",
        icon = Icons.Default.CreditCard,
    ),
    Users(
        label = "User",
        subtitle = "Rollen, KI-Zugriff, Tageslimits und History pro Konto steuern.",
        icon = Icons.Default.Person,
    ),
    Shopify(
        label = "Shopify",
        subtitle = "Owner-Quelle fuer Store-Domain, Token und Merch-Sync pflegen.",
        icon = Icons.Default.ShoppingBag,
    ),
    Commerce(
        label = "Versand",
        subtitle = "Versandkosten, MwSt. und Rechnungsdaten gesammelt pflegen.",
        icon = Icons.Default.LocalShipping,
    ),
    Visuals(
        label = "Visuals",
        subtitle = "Drive-Link, Referenzhinweise und Namensschema pflegen.",
        icon = Icons.Default.Palette,
    ),
    Automation(
        label = "Automation",
        subtitle = "Owner-seitig n8n anbinden, User-Kontext steuern und den Webhook testen.",
        icon = Icons.Default.Bolt,
    ),
}

private fun adminWorkspaceStatusText(
    section: AdminWorkspaceSection,
    uiState: SettingsUiState,
    connectedPaymentMethodCount: Int,
    visiblePaymentMethodCount: Int,
): String {
    return when (section) {
        AdminWorkspaceSection.Overview -> "$connectedPaymentMethodCount Bereiche aktiv"
        AdminWorkspaceSection.Payments -> "$visiblePaymentMethodCount live im Checkout"
        AdminWorkspaceSection.Users -> "${uiState.managedUsers.size} Konten"
        AdminWorkspaceSection.Shopify -> uiState.shopifyAdminSettings.activeCollectionLabel
        AdminWorkspaceSection.Commerce -> uiState.commerceSettings.invoice.supportEmail.ifBlank { "Versand & Rechnung" }
        AdminWorkspaceSection.Visuals -> if (uiState.aiVisualReferenceLibrary.isEnabled) "Visuals aktiv" else "Visuals aus"
        AdminWorkspaceSection.Automation -> if (uiState.workflowAutomationSettings.isPrepared) "n8n bereit" else "Noch offen"
    }
}

@Composable
private fun AdminWorkspaceListRow(
    section: AdminWorkspaceSection,
    detailText: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = section.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = section.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = section.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = detailText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                Text(
                    text = "Oeffnen",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
private fun AdminWorkspaceChip(
    section: AdminWorkspaceSection,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surface
            },
            contentColor = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        ),
    ) {
        Icon(
            imageVector = section.icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = section.label,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun AdminWorkspaceRailButton(
    section: AdminWorkspaceSection,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surface
            },
            contentColor = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = section.icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = section.label,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun AdminWorkspaceSummaryCard(
    section: AdminWorkspaceSection,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = section.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = section.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = section.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
        }
    }
}

@Composable
private fun AdminUserRoleGuideCard(
    modifier: Modifier = Modifier,
) {
    SkydownCard(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
    ) {
        Text(
            text = "Rollen im System",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Column(
            modifier = Modifier.padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            UserRole.entries.forEach { role ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = role.displayTitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = role.roleSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminManagedUserCard(
    user: User,
    currentUserId: String?,
    onSave: (User) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isCurrentUser = user.id == currentUserId
    var selectedRole by rememberSaveable(user.id, user.role) {
        mutableStateOf(user.resolvedRole.rawValue)
    }
    var selectedQuotaPlan by rememberSaveable(user.id, user.quotaPlan) {
        mutableStateOf(user.resolvedQuotaPlan.rawValue)
    }
    var aiAccessEnabled by rememberSaveable(user.id, user.aiAccessEnabled) {
        mutableStateOf(user.aiAccessEnabled)
    }
    var textLimitDraft by rememberSaveable(user.id, user.aiTextRequestsPerDay) {
        mutableStateOf(user.resolvedAiTextRequestsPerDay.toString())
    }
    var visualLimitDraft by rememberSaveable(user.id, user.aiVisualRequestsPerDay) {
        mutableStateOf(user.resolvedAiVisualRequestsPerDay.toString())
    }
    var agentLimitDraft by rememberSaveable(user.id, user.aiAgentRequestsPerDay) {
        mutableStateOf(user.resolvedAiAgentRequestsPerDay.toString())
    }
    var historyRetentionDays by rememberSaveable(user.id, user.aiHistoryRetentionDays) {
        mutableStateOf(user.resolvedAiHistoryRetentionDays)
    }
    var canManageMusicCatalog by rememberSaveable(user.id, user.canManageMusicCatalog) {
        mutableStateOf(user.canManageMusic)
    }
    var canManageVideoCatalog by rememberSaveable(user.id, user.canManageVideoCatalog) {
        mutableStateOf(user.canManageVideos)
    }
    var canModerateProfiles by rememberSaveable(user.id, user.canModerateProfiles) {
        mutableStateOf(user.canModerateUserProfiles)
    }
    val resolvedRole = UserRole.resolve(selectedRole, user.isAdmin, user.email)
    val resolvedQuotaPlan = UserQuotaPlan.resolve(selectedQuotaPlan, resolvedRole)

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
                    text = user.username,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SettingsBadge(
                    text = resolvedRole.displayTitle,
                    icon = Icons.Default.Person,
                    isActive = true,
                )
                if (isCurrentUser) {
                    SettingsBadge(
                        text = "Du",
                        icon = Icons.Default.CheckCircle,
                        isActive = true,
                    )
                }
            }
        }

        Text(
            text = "Rolle",
            modifier = Modifier.padding(top = 14.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )

        LazyRow(
            modifier = Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(UserRole.entries.toList()) { role ->
                val selected = resolvedRole == role
                if (selected) {
                    Button(
                        onClick = { selectedRole = role.rawValue },
                        shape = RoundedCornerShape(999.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Text(role.displayTitle)
                    }
                } else {
                    OutlinedButton(
                        onClick = { selectedRole = role.rawValue },
                        enabled = !isCurrentUser && !user.isPlatformOwner,
                        shape = RoundedCornerShape(999.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Text(role.displayTitle)
                    }
                }
            }
        }

        if (user.isPlatformOwner) {
            Text(
                text = "Das Owner-Konto ist fest an nash.lioncorna@gmail.com gebunden und bleibt immer Owner.",
                modifier = Modifier.padding(top = 10.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
        } else if (isCurrentUser) {
            Text(
                text = "Dein eigenes Konto bleibt vor versehentlichen Rollenwechseln geschuetzt. Limits kannst du hier trotzdem anpassen.",
                modifier = Modifier.padding(top = 10.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
        }

        when (resolvedRole) {
            UserRole.Owner -> {
                Text(
                    text = "Owner-Kontrolle: Shopify, Zahlungen, Rollen, n8n und Recovery laufen nur ueber dieses Konto.",
                    modifier = Modifier.padding(top = 10.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
            }

            UserRole.Admin -> {
                Text(
                    text = "Zugewiesene Funktionen",
                    modifier = Modifier.padding(top = 14.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )

                SettingsToggleRow(
                    title = "Music verwalten",
                    body = "Beats, Releases und Upload-Freigaben pflegen.",
                    checked = canManageMusicCatalog,
                    onCheckedChange = { canManageMusicCatalog = it },
                    modifier = Modifier.padding(top = 10.dp),
                )
                SettingsToggleRow(
                    title = "Video verwalten",
                    body = "Video Hub, Uploads und Home-Highlights steuern.",
                    checked = canManageVideoCatalog,
                    onCheckedChange = { canManageVideoCatalog = it },
                    modifier = Modifier.padding(top = 10.dp),
                )
                SettingsToggleRow(
                    title = "Profile moderieren",
                    body = "Profile und Galerie-Inhalte fuer Support und Moderation einsehen.",
                    checked = canModerateProfiles,
                    onCheckedChange = { canModerateProfiles = it },
                    modifier = Modifier.padding(top = 10.dp),
                )
            }

            UserRole.Subadmin, UserRole.User -> {
                Text(
                    text = if (resolvedRole == UserRole.Subadmin) "Kontingentmodell" else "Kontingent",
                    modifier = Modifier.padding(top = 14.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )

                if (resolvedRole == UserRole.User) {
                    Text(
                        text = UserQuotaPlan.Free.planSummary,
                        modifier = Modifier.padding(top = 10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    )
                } else {
                    LazyRow(
                        modifier = Modifier.padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(listOf(UserQuotaPlan.Creator, UserQuotaPlan.Studio)) { plan ->
                            val isSelected = resolvedQuotaPlan == plan
                            if (isSelected) {
                                Button(
                                    onClick = { selectedQuotaPlan = plan.rawValue },
                                    shape = RoundedCornerShape(999.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                ) {
                                    Text(plan.displayTitle)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { selectedQuotaPlan = plan.rawValue },
                                    shape = RoundedCornerShape(999.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                ) {
                                    Text(plan.displayTitle)
                                }
                            }
                        }
                    }

                    Text(
                        text = resolvedQuotaPlan.planSummary,
                        modifier = Modifier.padding(top = 10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    )
                }
            }
        }

        SettingsToggleRow(
            title = "KI fuer dieses Konto aktiv",
            body = "Wenn aus, sind Bot, Visuals und Agent fuer dieses Konto gesperrt.",
            checked = aiAccessEnabled,
            onCheckedChange = { aiAccessEnabled = it },
            modifier = Modifier.padding(top = 14.dp),
        )

        Text(
            text = "Tageslimits",
            modifier = Modifier.padding(top = 14.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )

        OutlinedTextField(
            value = textLimitDraft,
            onValueChange = { textLimitDraft = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            label = { Text("Bot pro Tag") },
            singleLine = true,
        )
        OutlinedTextField(
            value = visualLimitDraft,
            onValueChange = { visualLimitDraft = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            label = { Text("Visuals pro Tag") },
            singleLine = true,
        )
        OutlinedTextField(
            value = agentLimitDraft,
            onValueChange = { agentLimitDraft = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            label = { Text("Agent pro Tag") },
            singleLine = true,
        )

        Text(
            text = "History-Aufbewahrung",
            modifier = Modifier.padding(top = 14.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )

        LazyRow(
            modifier = Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(listOf(1, 3, 7, 30)) { option ->
                val isSelected = historyRetentionDays == option
                if (isSelected) {
                    Button(
                        onClick = { historyRetentionDays = option },
                        shape = RoundedCornerShape(999.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Text(historyOptionLabel(option))
                    }
                } else {
                    OutlinedButton(
                        onClick = { historyRetentionDays = option },
                        shape = RoundedCornerShape(999.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Text(historyOptionLabel(option))
                    }
                }
            }
        }

        Button(
            onClick = {
                val finalQuotaPlan = when (resolvedRole) {
                    UserRole.Owner -> UserQuotaPlan.OwnerUnlimited
                    UserRole.Admin -> UserQuotaPlan.InternalTeam
                    UserRole.Subadmin -> if (resolvedQuotaPlan == UserQuotaPlan.Studio) {
                        UserQuotaPlan.Studio
                    } else {
                        UserQuotaPlan.Creator
                    }
                    UserRole.User -> UserQuotaPlan.Free
                }
                onSave(
                    user.copy(
                        isAdmin = resolvedRole.hasStaffAccess,
                        role = resolvedRole.rawValue,
                        quotaPlan = finalQuotaPlan.rawValue,
                        aiAccessEnabled = aiAccessEnabled,
                        aiTextRequestsPerDay = textLimitDraft.parsePositiveIntOrDefault(
                            finalQuotaPlan.aiTextRequestsPerDay,
                        ),
                        aiVisualRequestsPerDay = visualLimitDraft.parsePositiveIntOrDefault(
                            finalQuotaPlan.aiVisualRequestsPerDay,
                        ),
                        aiAgentRequestsPerDay = agentLimitDraft.parsePositiveIntOrDefault(
                            finalQuotaPlan.aiAgentRequestsPerDay,
                        ),
                        aiHistoryRetentionDays = historyRetentionDays,
                        canManageMusicCatalog = when (resolvedRole) {
                            UserRole.Owner -> true
                            UserRole.Admin -> canManageMusicCatalog
                            else -> false
                        },
                        canManageVideoCatalog = when (resolvedRole) {
                            UserRole.Owner -> true
                            UserRole.Admin -> canManageVideoCatalog
                            else -> false
                        },
                        canModerateProfiles = when (resolvedRole) {
                            UserRole.Owner -> true
                            UserRole.Admin -> canModerateProfiles
                            else -> false
                        },
                    ),
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text("Konto speichern")
        }
    }

    LaunchedEffect(resolvedRole) {
        when (resolvedRole) {
            UserRole.Owner -> {
                selectedQuotaPlan = UserQuotaPlan.OwnerUnlimited.rawValue
                canManageMusicCatalog = true
                canManageVideoCatalog = true
                canModerateProfiles = true
                textLimitDraft = UserQuotaPlan.OwnerUnlimited.aiTextRequestsPerDay.toString()
                visualLimitDraft = UserQuotaPlan.OwnerUnlimited.aiVisualRequestsPerDay.toString()
                agentLimitDraft = UserQuotaPlan.OwnerUnlimited.aiAgentRequestsPerDay.toString()
                historyRetentionDays = UserQuotaPlan.OwnerUnlimited.aiHistoryRetentionDays
            }
            UserRole.Admin -> {
                selectedQuotaPlan = UserQuotaPlan.InternalTeam.rawValue
                textLimitDraft = UserQuotaPlan.InternalTeam.aiTextRequestsPerDay.toString()
                visualLimitDraft = UserQuotaPlan.InternalTeam.aiVisualRequestsPerDay.toString()
                agentLimitDraft = UserQuotaPlan.InternalTeam.aiAgentRequestsPerDay.toString()
                historyRetentionDays = UserQuotaPlan.InternalTeam.aiHistoryRetentionDays
            }
            UserRole.Subadmin -> {
                if (resolvedQuotaPlan !in listOf(UserQuotaPlan.Creator, UserQuotaPlan.Studio)) {
                    selectedQuotaPlan = UserQuotaPlan.Creator.rawValue
                }
                canManageMusicCatalog = false
                canManageVideoCatalog = false
                canModerateProfiles = false
            }
            UserRole.User -> {
                selectedQuotaPlan = UserQuotaPlan.Free.rawValue
                canManageMusicCatalog = false
                canManageVideoCatalog = false
                canModerateProfiles = false
                textLimitDraft = UserQuotaPlan.Free.aiTextRequestsPerDay.toString()
                visualLimitDraft = UserQuotaPlan.Free.aiVisualRequestsPerDay.toString()
                agentLimitDraft = UserQuotaPlan.Free.aiAgentRequestsPerDay.toString()
                historyRetentionDays = UserQuotaPlan.Free.aiHistoryRetentionDays
            }
        }
    }

    LaunchedEffect(selectedQuotaPlan, resolvedRole) {
        if (resolvedRole == UserRole.Subadmin) {
            val plan = UserQuotaPlan.resolve(selectedQuotaPlan, resolvedRole)
            textLimitDraft = plan.aiTextRequestsPerDay.toString()
            visualLimitDraft = plan.aiVisualRequestsPerDay.toString()
            agentLimitDraft = plan.aiAgentRequestsPerDay.toString()
            historyRetentionDays = plan.aiHistoryRetentionDays
        }
    }
}

private val UserRole.displayTitle: String
    get() = when (this) {
        UserRole.Owner -> "Owner"
        UserRole.Admin -> "Admin"
        UserRole.Subadmin -> "Subadmin"
        UserRole.User -> "User"
    }

private val UserRole.roleSummary: String
    get() = when (this) {
        UserRole.Owner -> "Festes Hauptkonto der App. Fuer diese App ist nash.lioncorna@gmail.com immer der Owner. Root-Zugriff auf Shopify, Zahlungen, Rollen, n8n und Recovery."
        UserRole.Admin -> "Teaminterne Leute. Der Owner weist ihnen gezielt Funktionen wie Music, Video oder Profil-Moderation zu. Kein Zugriff auf Owner-Systembereiche."
        UserRole.Subadmin -> "Externe Premium-Konten mit buchbarem Kontingentmodell. Kein Admin-Workspace, keine Owner-Rechte."
        UserRole.User -> "Normales Nutzerkonto mit Free-Kontingent. Nicht eingeloggte Leute sind zusaetzlich Gast-Nutzer ohne gespeichertes Konto."
    }

private val UserQuotaPlan.displayTitle: String
    get() = when (this) {
        UserQuotaPlan.OwnerUnlimited -> "Owner Unlimited"
        UserQuotaPlan.InternalTeam -> "Internal Team"
        UserQuotaPlan.Free -> "Free"
        UserQuotaPlan.Creator -> "Creator"
        UserQuotaPlan.Studio -> "Studio"
    }

private val UserQuotaPlan.planSummary: String
    get() = when (this) {
        UserQuotaPlan.OwnerUnlimited -> "Praktisch unbegrenztes Owner-Kontingent fuer Systemsteuerung, Tests und Recovery."
        UserQuotaPlan.InternalTeam -> "Internes Team-Kontingent fuer feste Mitarbeiter."
        UserQuotaPlan.Free -> "Basiszugang mit kleinem Free-Kontingent."
        UserQuotaPlan.Creator -> "Erweitertes Creator-Kontingent fuer regelmaessige Nutzung."
        UserQuotaPlan.Studio -> "Grosses Studio-Kontingent fuer intensivere Nutzung und laengere History."
    }

private fun String.parsePositiveIntOrDefault(fallback: Int): Int {
    val value = trim().toIntOrNull() ?: return fallback
    return if (value > 0) value else fallback
}

private fun historyOptionLabel(days: Int): String {
    return if (days == 1) "1 Tag" else "$days Tage"
}

private fun resolveAutomationDraftWebhookUrl(
    baseUrl: String,
    webhookPath: String,
): String? {
    val trimmedBaseUrl = baseUrl.trim()
    if (trimmedBaseUrl.isBlank()) {
        return null
    }

    val normalizedBaseUrl = if (trimmedBaseUrl.startsWith("https://") || trimmedBaseUrl.startsWith("http://")) {
        trimmedBaseUrl
    } else {
        "https://$trimmedBaseUrl"
    }.trimEnd('/')

    val trimmedPath = webhookPath.trim().trim('/')
    return if (trimmedPath.isBlank()) normalizedBaseUrl else "$normalizedBaseUrl/$trimmedPath"
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
