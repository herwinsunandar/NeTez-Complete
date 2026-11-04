package com.herwin.netez.entitlement

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.herwin.netez.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BillingManager private constructor(private val context: Context) : PurchasesUpdatedListener {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val subManager = SubscriptionManager.getInstance(context)

    val productIdMonthly: String = try { context.getString(R.string.product_id_pro_monthly) } catch (e: Exception) { "netez_pro_monthly" }
    val productIdYearly: String = try { context.getString(R.string.product_id_pro_yearly) } catch (e: Exception) { "netez_pro_yearly" }

    private val _monthlyProduct = MutableStateFlow<ProductDetails?>(null)
    val monthlyProduct: StateFlow<ProductDetails?> = _monthlyProduct.asStateFlow()

    private val _yearlyProduct = MutableStateFlow<ProductDetails?>(null)
    val yearlyProduct: StateFlow<ProductDetails?> = _yearlyProduct.asStateFlow()

    private val _billingConnected = MutableStateFlow(false)
    val billingConnected: StateFlow<Boolean> = _billingConnected.asStateFlow()

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    init {
        startConnection()
    }

    fun startConnection() {
        if (billingClient.isReady) {
            _billingConnected.value = true
            querySubscriptionProducts()
            queryActivePurchases()
            return
        }

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Google Play Billing setup successful")
                    _billingConnected.value = true
                    querySubscriptionProducts()
                    queryActivePurchases()
                } else {
                    Log.e(TAG, "Google Play Billing setup failed: ${billingResult.debugMessage}")
                    _billingConnected.value = false
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Google Play Billing service disconnected. Reconnecting...")
                _billingConnected.value = false
            }
        })
    }

    fun querySubscriptionProducts() {
        if (!billingClient.isReady) return

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productIdMonthly)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productIdYearly)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsResult != null) {
                val list = productDetailsResult.productDetailsList
                if (list != null) {
                    for (product in list) {
                        Log.d(TAG, "Queried Product: ${product.productId} -> ${product.title}")
                        if (product.productId == productIdMonthly || product.productId == "netez_pro_monthly") {
                            _monthlyProduct.value = product
                        }
                        if (product.productId == productIdYearly || product.productId == "netez_pro_yearly") {
                            _yearlyProduct.value = product
                        }
                    }
                }
            } else {
                Log.e(TAG, "Failed to query products: ${billingResult.debugMessage}")
            }
        }
    }

    fun launchPurchase(activity: Activity, productDetails: ProductDetails): Boolean {
        if (!billingClient.isReady) {
            startConnection()
            return false
        }

        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull { offer ->
            offer.offerToken.isNotEmpty()
        }?.offerToken ?: productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: return false

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val result = billingClient.launchBillingFlow(activity, billingFlowParams)
        return result.responseCode == BillingClient.BillingResponseCode.OK
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d(TAG, "User canceled purchase flow")
        } else {
            Log.e(TAG, "Purchase failed: ${billingResult.debugMessage}")
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            // Activate PRO Tier
            subManager.setProTier(true)

            // Acknowledge Purchase if not acknowledged
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { result ->
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.d(TAG, "Purchase acknowledged successfully")
                    } else {
                        Log.e(TAG, "Purchase acknowledgment failed: ${result.debugMessage}")
                    }
                }
            }
        }
    }

    fun queryActivePurchases() {
        if (!billingClient.isReady) return

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                val hasActivePro = purchases.any { purchase ->
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                            (purchase.products.contains(productIdMonthly) || purchase.products.contains(productIdYearly))
                }
                subManager.setProTier(hasActivePro)
            }
        }
    }

    fun restorePurchases(): Boolean {
        queryActivePurchases()
        return subManager.isPro()
    }

    companion object {
        private const val TAG = "BillingManager"

        @Volatile
        private var instance: BillingManager? = null

        fun getInstance(context: Context): BillingManager {
            return instance ?: synchronized(this) {
                instance ?: BillingManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
