package io.glassfy.androidsdk.internal.billing.play.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryRecord
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchaseHistoryParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.SkuDetailsParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.consumePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchaseHistory
import com.android.billingclient.api.queryPurchasesAsync
import com.android.billingclient.api.querySkuDetails
import io.glassfy.androidsdk.Glassfy
import io.glassfy.androidsdk.GlassfyErrorCode
import io.glassfy.androidsdk.internal.billing.play.IPlayBillingPurchaseDelegate
import io.glassfy.androidsdk.internal.billing.play.PlayBillingResource
import io.glassfy.androidsdk.internal.billing.play.legacy.prorationMode
import io.glassfy.androidsdk.internal.logger.Logger
import io.glassfy.androidsdk.model.SubscriptionUpdate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class PlayBillingClientWrapper(
    ctx: Context,
    private val delegate: IPlayBillingPurchaseDelegate,
    private val watcherMode: Boolean
) {
    companion object {
        private const val BACKOFF_RECONNECTION_MS = 2000L
        private const val MAX_RECONNECTION_RETRIES = 5
    }

    private val billingClient by lazy {
        BillingClient.newBuilder(ctx).enablePendingPurchases().setListener { r, p ->
            Glassfy.customScope.launch { handlePurchasesUpdate(r, p) }
        }.build()
    }

    private val purchasingProductTypeById = mutableMapOf<String, String>()

    private val billingConnectionMutex = Mutex()

    // purchase listener
    private val purchasingCallbacks =
        mutableMapOf<String, Continuation<PlayBillingResource<Purchase>>>()

    private suspend fun handlePurchasesUpdate(
        billingResult: BillingResult, purchases: List<Purchase>?
    ) {
        if (billingResult.isOk()) {
            purchases?.forEach { purchase ->
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    findProductsType(purchase.products)?.also { productType ->
                        // 1 - process purchase
                        if (!watcherMode) {
                            processPurchases(purchase, productType)
                        }
                        // 2 - call delegate
                        delegate.onPlayBillingPurchasePurchase(purchase, productType)
                    }
                }

                // 3 - call continuation
                purchase.products.intersect(purchasingCallbacks.keys).onEach {
                    purchasingCallbacks.remove(it)?.takeIf { c -> c.context.isActive }
                        ?.also { c -> c.resume(PlayBillingResource.Success(purchase)) }
                }
            }
        } else {
            Logger.logDebug("handlePurchasesUpdate result error - code:${billingResult.responseCode} ; msg:${billingResult.debugMessage} - ${Thread.currentThread().name}")

            val callbacks: MutableIterator<MutableMap.MutableEntry<String, Continuation<PlayBillingResource<Purchase>>>> =
                purchasingCallbacks.entries.iterator()
            while (callbacks.hasNext()) {
                val c = callbacks.next().value
                callbacks.remove()
                if (c.context.isActive) {
                    c.resume(PlayBillingResource.Error(billingResult))
                }
            }
        }
    }

    private suspend fun processPurchases(p: Purchase, type: String): PlayBillingResource<String?>? {
        Logger.logDebug("processPurchase - state:${p.purchaseState} ; ack:${p.isAcknowledged} - ${Thread.currentThread().name}")

        return when (type) {
            BillingClient.ProductType.INAPP -> consumeToken(p.purchaseToken)
            BillingClient.ProductType.SUBS -> {
                if (!p.isAcknowledged) {
                    acknowledgeToken(p.purchaseToken)
                } else {
                    null
                }
            }

            else -> {
                Logger.logError("UNKNOWN PRODUCT TYPE - ${Thread.currentThread().name}"); null
            }
        }
    }

    private suspend fun findProductsType(products: List<String>): String? =
        purchasingProductTypeById.firstNotNullOfOrNull { it.takeIf { products.contains(it.key) } }?.value
            ?: queryProductsDetails(
                products.toSet(), BillingClient.ProductType.SUBS
            ).data?.firstOrNull()?.productType ?: queryProductsDetails(
                products.toSet(), BillingClient.ProductType.INAPP
            ).data?.firstOrNull()?.productType


////// INTERNALS

    internal suspend fun isFeatureSupported(feature: String): PlayBillingResource<Boolean> =
        withClientReady {
            PlayBillingResource.Success(billingClient.isFeatureSupported(feature).isOk())
        }


    // PurchaseHistory

    internal suspend fun allPurchaseHistory(): PlayBillingResource<List<PurchaseHistoryRecord>> =
        queryPurchaseHistory(
            listOf(
                BillingClient.ProductType.INAPP, BillingClient.ProductType.SUBS
            )
        )

    internal suspend fun inappPurchaseHistory(): PlayBillingResource<List<PurchaseHistoryRecord>> =
        queryPurchaseHistory(listOf(BillingClient.ProductType.INAPP))

    internal suspend fun subsPurchaseHistory(): PlayBillingResource<List<PurchaseHistoryRecord>> =
        queryPurchaseHistory(listOf(BillingClient.ProductType.SUBS))

    internal suspend fun queryPurchaseHistory(types: List<String>): PlayBillingResource<List<PurchaseHistoryRecord>> =
        withClientReady {
            types.flatMap {
                val params = QueryPurchaseHistoryParams.newBuilder().setProductType(it).build()
                val result = billingClient.queryPurchaseHistory(params)
                if (!result.billingResult.isOk()) {
                    return@withClientReady PlayBillingResource.Error(result.billingResult)
                }
                result.purchaseHistoryRecordList.orEmpty()
            }.let { PlayBillingResource.Success(it) }
        }


// Purchases - Active subscriptions and non-consumed one-time purchases

    internal suspend fun allPurchase(): PlayBillingResource<List<Purchase>> =
        queryPurchase(listOf(BillingClient.ProductType.INAPP, BillingClient.ProductType.SUBS))

    internal suspend fun inappPurchase(): PlayBillingResource<List<Purchase>> =
        queryPurchase(listOf(BillingClient.ProductType.INAPP))

    internal suspend fun subsPurchase(): PlayBillingResource<List<Purchase>> =
        queryPurchase(listOf(BillingClient.ProductType.SUBS))

    internal suspend fun queryPurchase(types: List<String>): PlayBillingResource<List<Purchase>> =
        withClientReady {
            types.flatMap {
                val params = QueryPurchasesParams.newBuilder().setProductType(it).build()
                val result = billingClient.queryPurchasesAsync(params)
                if (!result.billingResult.isOk()) {
                    return@withClientReady PlayBillingResource.Error(result.billingResult)
                }
                result.purchasesList
            }.let { PlayBillingResource.Success(it) }
        }

    // Acknowledges subscription and consume in-app purchase
    internal suspend fun acknowledgeToken(purchaseToken: String): PlayBillingResource<String?> =
        withClientReady {
            Logger.logDebug("acknowledgeToken of SUBS product - ${Thread.currentThread().name}")
            val param =
                AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchaseToken).build()
            val res = billingClient.acknowledgePurchase(param)
            if (res.isOk()) {
                return@withClientReady PlayBillingResource.Success(purchaseToken)
            } else {
                return@withClientReady PlayBillingResource.Error(res)
            }
        }

    internal suspend fun consumeToken(purchaseToken: String): PlayBillingResource<String?> =
        withClientReady {
            Logger.logDebug("consumeToken of INAPP product - ${Thread.currentThread().name}")
            val param = ConsumeParams.newBuilder().setPurchaseToken(purchaseToken).build()
            val res = billingClient.consumePurchase(param)
            if (res.billingResult.isOk()) {
                return@withClientReady PlayBillingResource.Success(res.purchaseToken)
            } else {
                return@withClientReady PlayBillingResource.Error(res.billingResult)
            }
        }


    // Product details
    internal suspend fun queryProductsDetails(
        productIds: Set<String>, type: String
    ): PlayBillingResource<List<ProductDetails>> = withClientReady {
        productIds.ifEmpty {
            return@withClientReady PlayBillingResource.Success(emptyList())
        }.map {
            QueryProductDetailsParams.Product
                .newBuilder()
                .setProductId(it)
                .setProductType(type)
                .build()
        }.let {
            billingClient.queryProductDetails(
                QueryProductDetailsParams
                    .newBuilder()
                    .setProductList(it)
                    .build()
            )
        }.let {
            if (it.billingResult.isOk()) {
                PlayBillingResource.Success(it.productDetailsList.orEmpty())
            } else {
                PlayBillingResource.Error(it.billingResult)
            }
        }
    }


// Purchase

    internal suspend fun purchaseProductDetails(
        activity: Activity,
        product: ProductDetails,
        offerToken: String,
        update: SubscriptionUpdate? = null,
        accountId: String? = null
    ): PlayBillingResource<Purchase> {
        val paramsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder().run {
            setProductDetails(product)
            if (product.productType == BillingClient.ProductType.SUBS) {
                setOfferToken(offerToken)
            }
            build()
        }.let {
            BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(it))
        }

        update?.run {
            if (purchaseToken.isEmpty()) {
                return PlayBillingResource.Error(
                    BillingResult.newBuilder()
                        .setResponseCode(BillingClient.BillingResponseCode.ITEM_NOT_OWNED).build()
                )
            }

            if (product.productType == BillingClient.ProductType.SUBS) {
                paramsBuilder.setSubscriptionUpdateParams(
                    BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                        .setOldPurchaseToken(purchaseToken)
                        .setSubscriptionReplacementMode(replacement.mode)
                        .build()
                )
            }
        }

        accountId?.let {
            paramsBuilder.setObfuscatedAccountId(it)
        }

        return purchaseProductDetails(activity, product, paramsBuilder.build())
    }

    private suspend fun purchaseProductDetails(
        activity: Activity, product: ProductDetails, params: BillingFlowParams
    ): PlayBillingResource<Purchase> {
        Logger.logDebug("purchaseProduct - 0 - ${Thread.currentThread().name}")
        if (purchasingProductTypeById.contains(product.productId)) {
            val err = BillingResult.newBuilder()
                .setResponseCode(GlassfyErrorCode.Purchasing.internalCode!!)
                .setDebugMessage("Already Purchasing...").build()
            return PlayBillingResource.Error(err)
        }
        purchasingProductTypeById[product.productId] = product.productType
        Logger.logDebug("purchaseProduct - 1 - ${Thread.currentThread().name}")

        withContext(Dispatchers.Main) {
            Logger.logDebug("purchaseProduct - 2 - ${Thread.currentThread().name}")
            billingClient.launchBillingFlow(activity, params)
        }.takeIf { !it.isOk() }?.let {
            Logger.logDebug("purchaseProduct - 3 fail - ${Thread.currentThread().name}")
            purchasingProductTypeById.remove(product.productId)
            return@purchaseProductDetails PlayBillingResource.Error(it)
        }

        suspendCoroutine<PlayBillingResource<Purchase>> {
            Logger.logDebug("purchaseProduct - 3 - ${Thread.currentThread().name}")
            purchasingCallbacks[product.productId] = it
        }.also {
            Logger.logDebug("purchaseProduct - 4 - ${Thread.currentThread().name}")
            purchasingProductTypeById.remove(product.productId)

            return@purchaseProductDetails it
        }
    }


////// LEGACY - SkuDetails

    internal suspend fun querySkuDetails(skuList: Set<String>): PlayBillingResource<List<SkuDetails>> =
        withClientReady {
            if (skuList.isEmpty()) {
                return@withClientReady PlayBillingResource.Success(emptyList())
            }

            val inApp = SkuDetailsParams.newBuilder().setSkusList(skuList.toList())
                .setType(BillingClient.SkuType.INAPP).build().let {
                    billingClient.querySkuDetails(it)
                }

            if (!inApp.billingResult.isOk()) {
                return@withClientReady PlayBillingResource.Error(inApp.billingResult)
            }

            val subs = SkuDetailsParams.newBuilder().setSkusList(skuList.toList())
                .setType(BillingClient.SkuType.SUBS).build().let {
                    billingClient.querySkuDetails(it)
                }

            if (!subs.billingResult.isOk()) {
                return@withClientReady PlayBillingResource.Error(inApp.billingResult)
            }

            return@withClientReady PlayBillingResource.Success(inApp.skuDetailsList.orEmpty() + subs.skuDetailsList.orEmpty())
        }

    internal suspend fun purchaseSkuDetails(
        activity: Activity,
        sku: SkuDetails,
        update: SubscriptionUpdate? = null,
        accountId: String? = null
    ): PlayBillingResource<Purchase> {
        val paramsBuilder = BillingFlowParams.newBuilder().setSkuDetails(sku)
        update?.run {
            if (purchaseToken.isEmpty()) {
                return PlayBillingResource.Error(
                    BillingResult.newBuilder()
                        .setResponseCode(BillingClient.BillingResponseCode.ITEM_NOT_OWNED).build()
                )
            }

            paramsBuilder.setSkuDetails(sku).setSubscriptionUpdateParams(
                BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                    .setOldSkuPurchaseToken(purchaseToken)
                    .setReplaceSkusProrationMode(replacement.prorationMode()).build()
            )
        }
        accountId?.let {
            paramsBuilder.setObfuscatedAccountId(it)
        }

        return purchaseSkuDetails(activity, sku, paramsBuilder.build())
    }

    private suspend fun purchaseSkuDetails(
        activity: Activity, sku: SkuDetails, params: BillingFlowParams
    ): PlayBillingResource<Purchase> {
        Logger.logDebug("purchaseSku - 0 - ${Thread.currentThread().name}")
        if (purchasingProductTypeById.contains(sku.sku)) {
            val err = BillingResult.newBuilder()
                .setResponseCode(GlassfyErrorCode.Purchasing.internalCode!!)
                .setDebugMessage("Already Purchasing...").build()
            return PlayBillingResource.Error(err)
        }
        purchasingProductTypeById[sku.sku] = sku.type
        Logger.logDebug("purchaseSku - 1 - ${Thread.currentThread().name}")

        withContext(Dispatchers.Main) {
            Logger.logDebug("purchaseSku - 2 - ${Thread.currentThread().name}")
            billingClient.launchBillingFlow(activity, params)
        }.takeIf { !it.isOk() }?.let {
            Logger.logDebug("purchaseSku - 3 fail - ${Thread.currentThread().name}")
            purchasingProductTypeById.remove(sku.sku)
            return@purchaseSkuDetails PlayBillingResource.Error(it)
        }

        suspendCoroutine<PlayBillingResource<Purchase>> {
            Logger.logDebug("purchaseSku - 3 - ${Thread.currentThread().name}")
            purchasingCallbacks[sku.sku] = it
        }.also {
            Logger.logDebug("purchaseSku - 4 - ${Thread.currentThread().name}")
            purchasingProductTypeById.remove(sku.sku)

            return@purchaseSkuDetails it
        }
    }


////// UTILS

    private suspend fun <T> withClientReady(
        block: suspend () -> PlayBillingResource<T>
    ): PlayBillingResource<T> = billingClient.isReady.let {
        if (it) return block()

        var retryCount = 0
        var connectionResult: BillingResult
        do {
            delay(BACKOFF_RECONNECTION_MS * retryCount)
            connectionResult = billingConnectionMutex.withLock {
                startConnectionSync(billingClient)
            }
            retryCount += 1
        } while (!connectionResult.isOk() && retryCount < MAX_RECONNECTION_RETRIES)

        return if (connectionResult.isOk()) block() else PlayBillingResource.Error(
            connectionResult
        )
    }

    private suspend fun startConnectionSync(bc: BillingClient): BillingResult =
        suspendCancellableCoroutine { c ->
            bc.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    Logger.logDebug("onBillingSetupFinished code:${billingResult.responseCode} ; msg:${billingResult.debugMessage} - ${Thread.currentThread().name}")
                    if (c.isActive && c.context.isActive) {
                        c.resume(billingResult)
                    }
                }

                override fun onBillingServiceDisconnected() {
                    Logger.logDebug("onBillingServiceDisconnected - ${Thread.currentThread().name}")
                    if (c.isActive && c.context.isActive) {
                        val disconnectedResult = BillingResult.newBuilder()
                            .setResponseCode(BillingClient.BillingResponseCode.SERVICE_DISCONNECTED)
                            .build()
                        c.resume(disconnectedResult)
                    }
                }
            })
        }

    private fun BillingResult.isOk(): Boolean {
        return this.responseCode == BillingClient.BillingResponseCode.OK
    }
}