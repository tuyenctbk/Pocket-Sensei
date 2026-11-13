package com.example.donation

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.example.R
import com.example.analytics.EngagementManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.min

private const val TAG = "DonationBillingManager"

class DonationBillingManager(
    context: Context,
    private val engagementManager: EngagementManager? = null
) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var reconnectDelayMs = 1000L
    private var isDestroyed = false

    private val cachedProductDetails = mutableMapOf<String, ProductDetails>()

    private val _uiState = MutableStateFlow(
        DonationUiState(
            isActiveSupporter = engagementManager?.isActiveSupporter?.value ?: false
        )
    )
    val uiState: StateFlow<DonationUiState> = _uiState.asStateFlow()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    handlePurchase(purchase)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _uiState.update {
                    it.copy(
                        isPurchasing = false,
                        statusMessage = appContext.getString(R.string.billing_purchase_canceled),
                        isErrorStatus = false
                    )
                }
                engagementManager?.trackEvent("donation_play_canceled", "donation")
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                queryExistingPurchases()
            }
            else -> {
                val detail = billingResult.debugMessage.ifBlank {
                    appContext.getString(R.string.billing_service_unavailable)
                }
                _uiState.update {
                    it.copy(
                        isPurchasing = false,
                        statusMessage = appContext.getString(R.string.billing_error_fallback, detail),
                        isErrorStatus = true
                    )
                }
                engagementManager?.trackEvent(
                    "donation_play_error",
                    "donation",
                    mapOf("code" to billingResult.responseCode, "msg" to billingResult.debugMessage)
                )
            }
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    init {
        connectBillingClient()
    }

    fun connectBillingClient() {
        if (isDestroyed || billingClient.isReady) return
        try {
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (isDestroyed) return
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        reconnectDelayMs = 1000L
                        _uiState.update { it.copy(isBillingConnected = true) }
                        queryDonationProducts()
                        queryExistingPurchases()
                    } else {
                        _uiState.update { it.copy(isBillingConnected = false) }
                        Log.w(TAG, "Billing setup failed (${billingResult.responseCode}): ${billingResult.debugMessage}")
                    }
                }

                override fun onBillingServiceDisconnected() {
                    if (isDestroyed) return
                    _uiState.update { it.copy(isBillingConnected = false) }
                    val delay = reconnectDelayMs
                    reconnectDelayMs = min(reconnectDelayMs * 2L, 16_000L)
                    mainHandler.postDelayed({ connectBillingClient() }, delay)
                }
            })
        } catch (e: Exception) {
            Log.w(TAG, "Unable to initialize BillingClient connection", e)
        }
    }

    fun refreshPurchases() {
        if (billingClient.isReady) {
            queryExistingPurchases()
        } else {
            connectBillingClient()
        }
    }

    private fun queryDonationProducts(onLoaded: (() -> Unit)? = null) {
        val productList = DonationCatalog.playTiers.map { tier ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(tier.productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, queryProductDetailsResult ->
            if (isDestroyed) return@queryProductDetailsAsync
            val productDetailsList = queryProductDetailsResult.productDetailsList
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                val priceMap = mutableMapOf<String, String>()
                productDetailsList.forEach { details ->
                    cachedProductDetails[details.productId] = details
                    details.oneTimePurchaseOfferDetails?.formattedPrice?.let { price ->
                        priceMap[details.productId] = price
                    }
                }
                _uiState.update { it.copy(productPrices = priceMap) }
            }
            onLoaded?.invoke()
        }
    }

    private fun queryExistingPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchasesList ->
            if (isDestroyed) return@queryPurchasesAsync
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                purchasesList.forEach { purchase ->
                    handlePurchase(purchase)
                }
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        val isDonationProduct = purchase.products.any { prod ->
            DonationCatalog.playTiers.any { it.productId == prod }
        }
        if (!isDonationProduct) return

        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                if (!validatePlayReceipt(purchase.purchaseToken, purchase.products, purchase.purchaseState)) {
                    _uiState.update {
                        it.copy(
                            isPurchasing = false,
                            statusMessage = appContext.getString(R.string.billing_receipt_failed),
                            isErrorStatus = true
                        )
                    }
                    return
                }
                if (!purchase.isAcknowledged) {
                    val ackParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    billingClient.acknowledgePurchase(ackParams) { ackResult ->
                        if (ackResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            markSupporterUnlocked(purchase.products.firstOrNull().orEmpty())
                        }
                    }
                } else {
                    markSupporterUnlocked(purchase.products.firstOrNull().orEmpty())
                }
            }
            Purchase.PurchaseState.PENDING -> {
                _uiState.update {
                    it.copy(
                        isPurchasing = false,
                        statusMessage = appContext.getString(R.string.billing_pending),
                        isErrorStatus = false
                    )
                }
                engagementManager?.trackEvent("donation_play_pending", "donation")
            }
            else -> {
                _uiState.update { it.copy(isPurchasing = false) }
            }
        }
    }

    private fun markSupporterUnlocked(productId: String) {
        engagementManager?.markActiveSupporter(source = "google_play", detail = productId)
        _uiState.update {
            it.copy(
                isActiveSupporter = true,
                isPurchasing = false,
                statusMessage = appContext.getString(R.string.billing_verified),
                isErrorStatus = false
            )
        }
    }

    /**
     * Launches the Google Play Billing flow using a verified Activity session.
     * Unwraps ContextWrappers via Context.findActivity() so Compose Dialogs/Sheets
     * never fail with "Billing Activity requires an Activity session".
     */
    fun launchDonationPurchase(context: Context, productId: String) {
        val activity: Activity? = context.findActivity()
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            _uiState.update {
                it.copy(
                    isPurchasing = false,
                    statusMessage = appContext.getString(R.string.billing_missing_activity),
                    isErrorStatus = true
                )
            }
            engagementManager?.trackEvent("donation_play_missing_activity", "error")
            return
        }

        _uiState.update { it.copy(isPurchasing = true, statusMessage = null, isErrorStatus = false) }
        engagementManager?.trackEvent("donation_play_clicked", "donation", mapOf("product_id" to productId))

        if (!billingClient.isReady) {
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (isDestroyed) return
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        _uiState.update { it.copy(isBillingConnected = true) }
                        queryAndLaunch(activity, productId)
                    } else {
                        val reason = billingResult.debugMessage.ifBlank {
                            appContext.getString(R.string.billing_code_prefix, billingResult.responseCode)
                        }
                        _uiState.update {
                            it.copy(
                                isPurchasing = false,
                                statusMessage = appContext.getString(R.string.billing_store_unavailable, reason),
                                isErrorStatus = true
                            )
                        }
                    }
                }

                override fun onBillingServiceDisconnected() {
                    if (isDestroyed) return
                    _uiState.update {
                        it.copy(
                            isPurchasing = false,
                            statusMessage = appContext.getString(R.string.billing_disconnected),
                            isErrorStatus = true
                        )
                    }
                }
            })
            return
        }

        queryAndLaunch(activity, productId)
    }

    private fun queryAndLaunch(activity: Activity, productId: String) {
        queryDonationProducts {
            val details = cachedProductDetails[productId]
            if (details == null) {
                mainHandler.post {
                    _uiState.update {
                        it.copy(
                            isPurchasing = false,
                            statusMessage = appContext.getString(R.string.billing_item_unpublished, productId),
                            isErrorStatus = true
                        )
                    }
                }
                return@queryDonationProducts
            }

            mainHandler.post {
                if (activity.isFinishing || activity.isDestroyed) {
                    _uiState.update { it.copy(isPurchasing = false) }
                    return@post
                }
                val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(details)
                    .build()
                val flowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(listOf(productDetailsParams))
                    .build()

                val launchResult = billingClient.launchBillingFlow(activity, flowParams)
                if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    val reason = launchResult.debugMessage.ifBlank {
                        appContext.getString(R.string.billing_code_prefix, launchResult.responseCode)
                    }
                    _uiState.update {
                        it.copy(
                            isPurchasing = false,
                            statusMessage = appContext.getString(R.string.billing_checkout_error, reason),
                            isErrorStatus = true
                        )
                    }
                }
            }
        }
    }

    fun openPayPalDonation(context: Context, amountUsd: String?) {
        val url = DonationConfig.buildPayPalUrl(amountUsd)
        engagementManager?.trackEvent(
            "donation_paypal_opened",
            "donation",
            mapOf("amount" to (amountUsd ?: "any"))
        )
        openExternalUrlWithClipboardFallback(
            context = context,
            url = url,
            label = appContext.getString(R.string.paypal_label),
            fallbackToast = appContext.getString(R.string.paypal_no_browser_toast)
        )
    }

    fun copyPayPalLink(context: Context, amountUsd: String?) {
        val url = DonationConfig.buildPayPalUrl(amountUsd)
        copyToClipboard(context, appContext.getString(R.string.paypal_label), url)
        Toast.makeText(context, appContext.getString(R.string.paypal_copied_toast, url), Toast.LENGTH_SHORT).show()
        engagementManager?.trackEvent("donation_paypal_copied", "donation")
    }

    fun sharePayPalLink(context: Context, amountUsd: String?) {
        val url = DonationConfig.buildPayPalUrl(amountUsd)
        engagementManager?.trackEvent("donation_paypal_shared", "donation")
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, appContext.getString(R.string.paypal_share_subject))
            putExtra(Intent.EXTRA_TEXT, appContext.getString(R.string.paypal_share_text, url))
        }
        val chooser = Intent.createChooser(intent, appContext.getString(R.string.paypal_share_chooser))
        try {
            if (context.findActivity() == null) {
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (_: Exception) {
            copyPayPalLink(context, amountUsd)
        }
    }

    fun openKofiDonation(context: Context) {
        val url = DonationConfig.KOFI_PAGE_URL
        engagementManager?.trackEvent("donation_kofi_opened", "donation")
        openExternalUrlWithClipboardFallback(
            context = context,
            url = url,
            label = appContext.getString(R.string.kofi_label),
            fallbackToast = appContext.getString(R.string.kofi_no_browser_toast)
        )
    }

    fun copyKofiLink(context: Context) {
        val url = DonationConfig.KOFI_PAGE_URL
        copyToClipboard(context, appContext.getString(R.string.kofi_label), url)
        Toast.makeText(context, appContext.getString(R.string.kofi_copied_toast, url), Toast.LENGTH_SHORT).show()
        engagementManager?.trackEvent("donation_kofi_copied", "donation")
    }

    fun openPrivacyPolicy(context: Context) {
        engagementManager?.trackEvent("privacy_policy_opened", "settings")
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(DonationConfig.PRIVACY_POLICY_ASSET_URL))
            if (context.findActivity() == null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            _uiState.update {
                it.copy(
                    statusMessage = appContext.getString(R.string.privacy_loaded_in_app),
                    isErrorStatus = false
                )
            }
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null, isErrorStatus = false) }
    }

    private fun openExternalUrlWithClipboardFallback(
        context: Context,
        url: String,
        label: String,
        fallbackToast: String
    ) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        try {
            if (context.findActivity() == null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            copyToClipboard(context, label, url)
            Toast.makeText(context, fallbackToast, Toast.LENGTH_LONG).show()
            _uiState.update {
                it.copy(statusMessage = fallbackToast, isErrorStatus = false)
            }
        }
    }

    private fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    fun destroy() {
        isDestroyed = true
        mainHandler.removeCallbacksAndMessages(null)
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }

    companion object {
        fun validatePlayReceipt(
            purchaseToken: String,
            productIds: List<String>,
            purchaseState: Int
        ): Boolean {
            if (purchaseState != Purchase.PurchaseState.PURCHASED) return false
            if (purchaseToken.isBlank()) return false
            return productIds.any { prod -> DonationCatalog.playTiers.any { it.productId == prod } }
        }
    }
}
