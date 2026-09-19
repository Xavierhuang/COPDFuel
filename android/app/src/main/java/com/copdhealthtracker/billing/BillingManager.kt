package com.copdhealthtracker.billing

import android.app.Activity
import android.content.Context
import android.util.Log
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages Google Play Billing for premium subscriptions (monthly and yearly).
 * Create one instance (e.g. in Application) and use isPremium to gate features.
 * Call launchSubscribe(activity, productId) for the chosen plan; call restorePurchases() for Restore.
 */
class BillingManager(private val context: Context) : PurchasesUpdatedListener {

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        // We only sell auto-renewing subscriptions; prepaid plans are not enabled
        // because we do not handle PENDING purchases.
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .enableAutoServiceReconnection()
        .build()

    private val prefs = context.getSharedPreferences(PREFS_BILLING, Context.MODE_PRIVATE)

    private val _isPremium = MutableStateFlow(isPremiumStored())
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _productDetailsMap = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    val productDetailsMap: StateFlow<Map<String, ProductDetails>> = _productDetailsMap.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    init {
        startConnection()
    }

    private fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    _isReady.value = true
                    queryProductDetails()
                    queryPurchasesAsync()
                } else {
                    Log.e(TAG, "Billing setup failed: ${result.debugMessage}")
                }
            }
            override fun onBillingServiceDisconnected() {
                _isReady.value = false
            }
        })
    }

    private fun queryProductDetails() {
        val productList = SUBSCRIPTION_PRODUCT_IDS.map { id ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(id)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()
        billingClient.queryProductDetailsAsync(params) { result, queryResult ->
            val productDetailsList = queryResult.productDetailsList
            if (result.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                _productDetailsMap.value = productDetailsList.associateBy { it.productId }
            } else {
                Log.e(TAG, "Query product details failed: ${result.debugMessage}")
            }
            // Billing 8+ reports products it could not fetch (not released, wrong ID, etc.)
            // instead of silently omitting them.
            for (unfetched in queryResult.unfetchedProductList) {
                Log.e(TAG, "Product unavailable: ${unfetched.productId} (${unfetched.statusCode})")
            }
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && !purchases.isNullOrEmpty()) {
            for (purchase in purchases) {
                if (purchase.isOurActiveSubscription()) {
                    if (!purchase.isAcknowledged) {
                        acknowledgePurchase(purchase)
                    }
                    setPremiumStored(true)
                    _isPremium.value = true
                }
            }
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        billingClient.acknowledgePurchase(params) { result ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.e(TAG, "Acknowledge failed: ${result.debugMessage}")
            }
        }
    }

    /**
     * Launches the billing flow for the given subscription product ID (monthly or yearly).
     */
    fun launchSubscribe(activity: Activity, productId: String, onError: (String) -> Unit = {}) {
        val details = _productDetailsMap.value[productId]
        if (details == null) {
            onError("Subscription not available. Try again later.")
            return
        }
        val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken
            ?: run {
                onError("Subscription offer not found.")
                return
            }
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .setOfferToken(offerToken)
                .build()
        )
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()
        val responseCode = billingClient.launchBillingFlow(activity, params).responseCode
        if (responseCode != BillingClient.BillingResponseCode.OK) {
            onError("Could not start purchase. Code: $responseCode")
        }
    }

    fun restorePurchases(onResult: (Boolean) -> Unit) {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasOurSubscription = purchases.any { it.isOurActiveSubscription() }
                if (hasOurSubscription) {
                    setPremiumStored(true)
                    _isPremium.value = true
                    onResult(true)
                } else {
                    onResult(false)
                }
            } else {
                onResult(false)
            }
        }
    }

    private fun queryPurchasesAsync() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasOurSubscription = purchases.any { it.isOurActiveSubscription() }
                if (hasOurSubscription) setPremiumStored(true)
                _isPremium.value = isPremiumStored()
            }
        }
    }

    /**
     * True only for one of our subscriptions that is actually paid for. A PENDING
     * purchase (e.g. cash payment awaiting completion) must not unlock premium.
     */
    private fun Purchase.isOurActiveSubscription(): Boolean =
        purchaseState == Purchase.PurchaseState.PURCHASED &&
            products.any { it in SUBSCRIPTION_PRODUCT_IDS }

    private fun isPremiumStored(): Boolean = prefs.getBoolean(KEY_PREMIUM, false)
    private fun setPremiumStored(value: Boolean) {
        prefs.edit().putBoolean(KEY_PREMIUM, value).apply()
    }

    companion object {
        const val PRODUCT_ID_MONTHLY = "copdfuel_premium_monthly"
        const val PRODUCT_ID_YEARLY = "copdfuel_premium_yearly"

        private val SUBSCRIPTION_PRODUCT_IDS = listOf(PRODUCT_ID_MONTHLY, PRODUCT_ID_YEARLY)

        private const val TAG = "BillingManager"
        private const val PREFS_BILLING = "billing_prefs"
        private const val KEY_PREMIUM = "is_premium"
    }
}
