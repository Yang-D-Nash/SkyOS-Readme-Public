package com.skydown.android.data

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.ProductDetails
import com.skydown.shared.model.User
import android.util.Log
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MembershipOpenReason {
    Manual,
    CriticalUsage,
    FeatureLocked,
    WorkflowLocked,
    AgentLimit,
    Settings,
}

data class MembershipProduct(
    val planLabel: String,
    val monthly: ProductDetails?,
    val yearly: ProductDetails?,
)

data class AiMembershipUiState(
    val isOpen: Boolean = false,
    val reason: MembershipOpenReason = MembershipOpenReason.Manual,
    val products: List<MembershipProduct> = emptyList(),
    val isLoading: Boolean = false,
    val isPurchasing: Boolean = false,
    val selectedAnnual: Boolean = false,
    val currentPlan: String = "Free",
    val surface: String = "ai_chat",
    val successMessage: String = "",
    val errorMessage: String = "",
    val annualDiscountCopy: String = "",
    val highlightedPlan: String = "creator",
)

class AiMembershipCoordinator(
    context: Context,
    private val syncClient: AiSubscriptionSyncClient = AiSubscriptionSyncClient(),
    private val runtimeConfigRepository: AiMembershipRuntimeConfigRepository = AiMembershipRuntimeConfigRepository(),
) {
    private val logTag = "AiMembershipCoordinator"
    private val scope = CoroutineScope(Dispatchers.Main.immediate)
    private val billing = AiNativeBillingManager(context)
    private val analytics = MembershipAnalyticsTracker(context)
    private var runtimeConfig = AiMembershipRuntimeConfig()
    private var runtimeConfigListener: ListenerRegistration? = null
    private val _uiState = MutableStateFlow(AiMembershipUiState())
    val uiState: StateFlow<AiMembershipUiState> = _uiState.asStateFlow()

    init {
        runtimeConfigListener = runtimeConfigRepository.observe { result ->
            result.onSuccess { config ->
                runtimeConfig = config
                _uiState.update {
                    it.copy(
                        selectedAnnual = if (it.products.isEmpty()) config.defaultAnnualToggle else it.selectedAnnual,
                        annualDiscountCopy = config.annualDiscountCopy,
                        highlightedPlan = config.highlightedPlan,
                    )
                }
            }
        }
    }

    fun analyticsSource(reason: MembershipOpenReason): String = when (reason) {
        MembershipOpenReason.Manual -> "manual"
        MembershipOpenReason.CriticalUsage -> "critical_usage"
        MembershipOpenReason.FeatureLocked -> "feature_locked"
        MembershipOpenReason.WorkflowLocked -> "workflow_locked"
        MembershipOpenReason.AgentLimit -> "agent_limit"
        MembershipOpenReason.Settings -> "settings"
    }

    fun updateCurrentPlan(user: User?) {
        val plan = when (user?.quotaPlan?.lowercase()) {
            "studio" -> "Creator"
            "creator" -> "Pro"
            else -> "Free"
        }
        _uiState.update { it.copy(currentPlan = plan) }
    }

    fun openMembership(reason: MembershipOpenReason, surface: String = "ai_chat") {
        track("membership_open_reason", mapOf("reason" to analyticsSource(reason)))
        if (reason == MembershipOpenReason.CriticalUsage) {
            track("upgrade_after_limit_warning")
            analytics.track("upgrade_after_warning", reason = analyticsSource(reason), surface = surface, currentPlan = _uiState.value.currentPlan)
        }
        analytics.track("membership_open", reason = analyticsSource(reason), surface = surface, currentPlan = _uiState.value.currentPlan)
        analytics.track("membership_reason", reason = analyticsSource(reason), surface = surface, currentPlan = _uiState.value.currentPlan)
        _uiState.update {
            it.copy(
                isOpen = true,
                reason = reason,
                surface = surface,
                successMessage = "",
                errorMessage = "",
            )
        }
        loadProducts()
    }

    fun trackUpgradeAfterDeny(surface: String) {
        analytics.track(
            event = "upgrade_after_deny",
            reason = analyticsSource(MembershipOpenReason.FeatureLocked),
            surface = surface,
            currentPlan = _uiState.value.currentPlan,
        )
    }

    fun closeMembership() {
        track("membership_close")
        _uiState.update { it.copy(isOpen = false, errorMessage = "") }
    }

    fun setAnnualOption(enabled: Boolean) {
        analytics.track(
            event = "annual_toggle_changed",
            annual = enabled,
            surface = _uiState.value.surface,
            currentPlan = _uiState.value.currentPlan,
        )
        _uiState.update { it.copy(selectedAnnual = enabled) }
    }

    private fun loadProducts() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = "") }
            runCatching {
                billing.queryMembershipProducts(runtimeConfig)
            }.onSuccess { list ->
                _uiState.update { it.copy(products = list, isLoading = false) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Membership Produkte konnten nicht geladen werden: ${error.localizedMessage}",
                    )
                }
            }
        }
    }

    fun purchaseSelectedPlan(activity: Activity, planLabel: String, onRefreshEntitlement: suspend () -> Unit) {
        val selected = _uiState.value.products.firstOrNull { it.planLabel == planLabel }
        if (selected == null) {
            _uiState.update { it.copy(errorMessage = "Plan ist gerade nicht verfuegbar.") }
            return
        }
        val product = if (_uiState.value.selectedAnnual) selected.yearly ?: selected.monthly else selected.monthly ?: selected.yearly
        if (product == null) {
            _uiState.update { it.copy(errorMessage = "Keine gueltige Preisoption fuer diesen Plan gefunden.") }
            return
        }
        scope.launch {
            track("plan_selected", mapOf("plan" to planLabel, "annual" to _uiState.value.selectedAnnual.toString()))
            track("purchase_started", mapOf("plan" to planLabel))
            analytics.track(
                event = "plan_selected",
                plan = planLabel.lowercase(),
                annual = _uiState.value.selectedAnnual,
                reason = analyticsSource(_uiState.value.reason),
                surface = _uiState.value.surface,
                currentPlan = _uiState.value.currentPlan,
            )
            analytics.track(
                event = "purchase_started",
                plan = planLabel.lowercase(),
                annual = _uiState.value.selectedAnnual,
                reason = analyticsSource(_uiState.value.reason),
                surface = _uiState.value.surface,
                currentPlan = _uiState.value.currentPlan,
            )
            _uiState.update { it.copy(isPurchasing = true, errorMessage = "", successMessage = "") }
            runCatching {
                val purchase = billing.launchSubscriptionPurchase(activity, product)
                var syncResult: AndroidSubscriptionSyncResult? = null
                if (purchase != null) {
                    syncResult = syncClient.requestAndroidSubscriptionSync(
                        productId = purchase.productId,
                        purchaseToken = purchase.purchaseToken,
                        packageName = activity.packageName,
                        orderId = purchase.orderId,
                    )
                    onRefreshEntitlement()
                }
                purchase to syncResult
            }.onSuccess { (purchase, syncResult) ->
                track(
                    if (purchase == null) "purchase_cancelled" else "purchase_success",
                    mapOf("plan" to planLabel),
                )
                analytics.track(
                    event = if (purchase == null) "purchase_cancelled" else "purchase_success",
                    plan = planLabel.lowercase(),
                    annual = _uiState.value.selectedAnnual,
                    reason = analyticsSource(_uiState.value.reason),
                    surface = _uiState.value.surface,
                    currentPlan = _uiState.value.currentPlan,
                )
                _uiState.update {
                    it.copy(
                        isPurchasing = false,
                        successMessage = purchaseSuccessMessage(purchase, syncResult),
                    )
                }
            }.onFailure { error ->
                track("purchase_error", mapOf("plan" to planLabel, "message" to (error.localizedMessage ?: "unknown")))
                analytics.track(
                    event = "purchase_cancelled",
                    plan = planLabel.lowercase(),
                    annual = _uiState.value.selectedAnnual,
                    reason = analyticsSource(_uiState.value.reason),
                    surface = _uiState.value.surface,
                    currentPlan = _uiState.value.currentPlan,
                )
                _uiState.update {
                    it.copy(
                        isPurchasing = false,
                        errorMessage = "Kauf konnte nicht abgeschlossen werden: ${error.localizedMessage}",
                    )
                }
            }
        }
    }

    fun restore(onRefreshEntitlement: suspend () -> Unit) {
        scope.launch {
            track("restore_started")
            _uiState.update { it.copy(isPurchasing = true, errorMessage = "", successMessage = "") }
            runCatching {
                val ownedSubscriptions = billing.queryOwnedSubscriptions()
                val results = ownedSubscriptions.map { owned ->
                    syncClient.requestAndroidSubscriptionSync(
                        productId = owned.productId,
                        purchaseToken = owned.purchaseToken,
                        packageName = billing.packageName,
                        orderId = owned.orderId,
                    )
                }
                if (ownedSubscriptions.isNotEmpty()) {
                    onRefreshEntitlement()
                }
                results
            }.onSuccess { results ->
                track("restore_success")
                analytics.track(
                    event = "restore_success",
                    reason = analyticsSource(_uiState.value.reason),
                    surface = _uiState.value.surface,
                    currentPlan = _uiState.value.currentPlan,
                )
                _uiState.update {
                    it.copy(
                        isPurchasing = false,
                        successMessage = restoreSuccessMessage(results),
                    )
                }
            }.onFailure { error ->
                track("restore_error", mapOf("message" to (error.localizedMessage ?: "unknown")))
                _uiState.update {
                    it.copy(
                        isPurchasing = false,
                        errorMessage = "Wiederherstellung fehlgeschlagen: ${error.localizedMessage}",
                    )
                }
            }
        }
    }

    private fun purchaseSuccessMessage(
        purchase: NativeSubscriptionPurchase?,
        syncResult: AndroidSubscriptionSyncResult?,
    ): String = when {
        purchase == null -> "Kauf wurde abgebrochen."
        syncResult?.status.equals("pending", ignoreCase = true) ->
            "Der Kauf wartet noch auf die Freigabe im Play Store."
        syncResult?.status.equals("active", ignoreCase = true) ->
            "Upgrade aktiv. Dein Plan wurde aktualisiert."
        syncResult?.status.equals("canceled", ignoreCase = true) ->
            "Abo erkannt. Der Status wurde aktualisiert."
        syncResult?.status.equals("inactive", ignoreCase = true) ->
            "Abo erkannt, aber ohne aktive Laufzeit."
        else -> "Abo wurde uebermittelt. Status wird aktualisiert."
    }

    private fun restoreSuccessMessage(results: List<AndroidSubscriptionSyncResult>): String = when {
        results.isEmpty() -> "Keine Play-Store-Kaeufe gefunden."
        results.any { it.status.equals("active", ignoreCase = true) } ->
            "Play-Store-Kaeufe wurden synchronisiert."
        results.any { it.status.equals("pending", ignoreCase = true) } ->
            "Ein Kauf wartet noch auf die Freigabe im Play Store."
        else -> "Play-Store-Status wurde aktualisiert."
    }

    private fun track(name: String, payload: Map<String, String> = emptyMap()) {
        Log.i(logTag, "event=$name payload=$payload")
    }
}
