package io.glassfy.androidsdk.internal.billing.google

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.SkuType.INAPP
import com.android.billingclient.api.BillingClient.SkuType.SUBS
import com.android.billingclient.api.Purchase.PurchaseState.PURCHASED
import io.glassfy.androidsdk.Glassfy
import io.glassfy.androidsdk.internal.billing.google.PlayBillingService.Companion.PURCHASING
import io.glassfy.androidsdk.internal.logger.Logger
import io.glassfy.androidsdk.model.SubscriptionUpdate
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class PlayBillingClientWrapper(
    ctx: Context,
    private val delegate: PlayBillingPurchaseDelegate,
    private val watcherMode: Boolean
) {
    companion object {
        private const val BACKOFF_RECONNECTION_MS = 1500L
        private const val MAX_RECONNECTION_RETRIES = 3
    }

    private val billingClient by lazy {
        BillingClient
            .newBuilder(ctx)
            .enablePendingPurchases()
            .setListener { r, p ->
                Glassfy.customScope.launch { handlePurchasesUpdate(r, p) }
            }
            .build()
    }

    private val purchasingSku = mutableMapOf<String, String>()


    private val billingConnectionMutex = Mutex()

    // purchase listener
    private val purchasingCallbacks =
        mutableMapOf<String, Continuation<PlayBillingResource<Purchase>>>()

    private suspend fun handlePurchasesUpdate(
        billingResult: BillingResult,
        purchases: List<Purchase>?
    ) {
        if (billingResult.isOk()) {
            purchases?.forEach { purchase ->
                if (purchase.purchaseState == PURCHASED) {
                    // 1 - process purchase
                    if (!watcherMode) {
                        processPurchases(purchase)
                    }

                    // 2 - call delegate
                    withContext(Dispatchers.Main) {
                        delegate.onPlayBillingPurchasePurchase(purchase)
                    }
                }

                // 3 - call continuation
                purchase.skus
                    .intersect(purchasingCallbacks.keys)
                    .onEach {
                        purchasingCallbacks.remove(it)
                            ?.takeIf { c -> c.context.isActive }
                            ?.also { c -> c.resume(PlayBillingResource.Success(purchase)) }
                    }
            }
        }
        else {
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


    internal suspend fun queryPurchaseHistory(
        types: Array<String> = arrayOf(
            INAPP,
            SUBS
        )
    ): PlayBillingResource<List<PurchaseHistoryRecord>> = withClientReady {
        val purchases = mutableListOf<PurchaseHistoryRecord>()
        types.forEach {
            val result = billingClient.queryPurchaseHistory(it)
            if (result.billingResult.isOk()) {
                purchases += result.purchaseHistoryRecordList.orEmpty()
            } else {
                return@withClientReady PlayBillingResource.Error(result.billingResult)
            }
        }
        return@withClientReady PlayBillingResource.Success(purchases)
    }

    internal suspend fun queryPurchase(
        types: Array<String> = arrayOf(
            INAPP,
            SUBS
        )
    ): PlayBillingResource<List<Purchase>> = withClientReady {
        val purchases = mutableListOf<Purchase>()
        types.forEach {
            val result = billingClient.queryPurchasesAsync(it)
            if (result.billingResult.isOk()) {
                purchases += result.purchasesList
            } else {
                return@withClientReady PlayBillingResource.Error(result.billingResult)
            }
        }
        return@withClientReady PlayBillingResource.Success(purchases)
    }

    internal suspend fun querySkuDetails(skuList: Set<String>): PlayBillingResource<List<SkuDetails>> =
        withClientReady {
            val inApp = SkuDetailsParams.newBuilder()
                .setSkusList(skuList.toList())
                .setType(INAPP)
                .build().let {
                    billingClient.querySkuDetails(it)
                }

            if (!inApp.billingResult.isOk()) {
                return@withClientReady PlayBillingResource.Error(inApp.billingResult)
            }

        val subs = SkuDetailsParams.newBuilder()
            .setSkusList(skuList.toList())
            .setType(SUBS)
            .build().let {
                billingClient.querySkuDetails(it)
            }

        if (!subs.billingResult.isOk()) {
            return@withClientReady PlayBillingResource.Error(inApp.billingResult)
        }

            return@withClientReady PlayBillingResource.Success(inApp.skuDetailsList.orEmpty() + subs.skuDetailsList.orEmpty())
    }

    internal suspend fun purchaseSku(
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
                        .setResponseCode(BillingClient.BillingResponseCode.ITEM_NOT_OWNED)
                        .build()
                )
            }

            paramsBuilder
                .setSkuDetails(sku)
                .setSubscriptionUpdateParams(
                    BillingFlowParams.SubscriptionUpdateParams
                        .newBuilder()
                        .setOldSkuPurchaseToken(purchaseToken)
                        .setReplaceSkusProrationMode(proration.mode)
                        .build()
                )
        }
        accountId?.let {
            paramsBuilder.setObfuscatedAccountId(it)
        }

        return purchase(activity, sku, paramsBuilder.build())
    }

    // Acknowledges subscription product.
    internal suspend fun acknowledgeToken(purchaseToken: String): PlayBillingResource<String?> =
        withClientReady {
            Logger.logDebug("acknowledgeToken of SUBS product - ${Thread.currentThread().name}")
            val param = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchaseToken)
                .build()
            val res = billingClient.acknowledgePurchase(param)
            if (res.isOk()) {
                return@withClientReady PlayBillingResource.Success(purchaseToken)
            } else {
                return@withClientReady PlayBillingResource.Error(res)
            }
    }

    //    Consumes a given in-app product.
    internal suspend fun consumeToken(purchaseToken: String): PlayBillingResource<String?> =
        withClientReady {
            Logger.logDebug("consumeToken of INAPP product - ${Thread.currentThread().name}")
            val param = ConsumeParams.newBuilder()
                .setPurchaseToken(purchaseToken)
                .build()
            val res = billingClient.consumePurchase(param)
            if (res.billingResult.isOk()) {
                return@withClientReady PlayBillingResource.Success(res.purchaseToken)
            } else {
                return@withClientReady PlayBillingResource.Error(res.billingResult)
            }
        }


    //// UTILS

    private suspend fun purchase(
        activity: Activity,
        sku: SkuDetails,
        params: BillingFlowParams
    ): PlayBillingResource<Purchase> {
        Logger.logDebug("purchaseSku - -1 - ${Thread.currentThread().name}")
        if (purchasingSku.contains(sku.sku)) {
            val err = BillingResult.newBuilder()
                .setResponseCode(PURCHASING)
                .setDebugMessage("Already Purchasing...")
                .build()
            return PlayBillingResource.Error(err)
        }
        purchasingSku[sku.sku] = sku.type
        Logger.logDebug("purchaseSku - 0 - ${Thread.currentThread().name}")

        withContext(Dispatchers.Main) {
            Logger.logDebug("purchaseSku - 1 - ${Thread.currentThread().name}")
            billingClient.launchBillingFlow(activity, params)
        }.takeIf  { !it.isOk() }
            ?.let {
                Logger.logDebug("purchaseSku - 2 fail - ${Thread.currentThread().name}")
                purchasingSku.remove(sku.sku)
                return@purchase PlayBillingResource.Error(it)
            }

        suspendCoroutine<PlayBillingResource<Purchase>> {
            Logger.logDebug("purchaseSku - 2 - ${Thread.currentThread().name}")
            purchasingCallbacks[sku.sku] = it
        }.also {
            Logger.logDebug("purchaseSku - 3 - ${Thread.currentThread().name}")
            purchasingSku.remove(sku.sku)

            return@purchase it
        }
    }

    private suspend fun processPurchases(p: Purchase): PlayBillingResource<String?>? {
        Logger.logDebug("processPurchase - state:${p.purchaseState} ; ack:${p.isAcknowledged} - ${Thread.currentThread().name}")

        var type =
            purchasingSku.firstNotNullOfOrNull { it.takeIf { p.skus.contains(it.key) } }?.value
        if (type == null) {
            val result = querySkuDetails(p.skus.toSet())
            if (result.err != null) return PlayBillingResource.Error(result.err)
            type = result.data?.firstOrNull()?.type
        }

        return when (type) {
            INAPP -> consumeToken(p.purchaseToken)
            SUBS -> if (!p.isAcknowledged) acknowledgeToken(p.purchaseToken) else null
            else -> {
                Logger.logError("UNKNOWN SKU - ${p.skus} - ${Thread.currentThread().name}"); null
            }
        }
    }

    private suspend fun <T> withClientReady(
        block: suspend () -> PlayBillingResource<T>
    ): PlayBillingResource<T> = billingClient.isReady
        .let {
            if (it) return block()

            var retryCount = 0
            var connectionResult: BillingResult
            do {
                delay(BACKOFF_RECONNECTION_MS * retryCount)
                connectionResult = startConnection()
                retryCount += 1
            } while (!connectionResult.isOk() && retryCount < MAX_RECONNECTION_RETRIES)

            return if (connectionResult.isOk()) block() else PlayBillingResource.Error(
                connectionResult
            )
        }

    private suspend fun startConnection(): BillingResult {
        billingConnectionMutex.withLock {
            return suspendCancellableCoroutine { c ->
                billingClient.startConnection(object : BillingClientStateListener {
                    override fun onBillingSetupFinished(billingResult: BillingResult) {
                        Logger.logDebug("onBillingSetupFinished code:${billingResult.responseCode} ; msg:${billingResult.debugMessage} - ${Thread.currentThread().name}")

                        if (!billingResult.isOk()) {
                            billingClient.endConnection()
                        }

                        if (c.isActive && c.context.isActive) {
                            c.resume(billingResult)
                        }
                    }

                    override fun onBillingServiceDisconnected() {
                        Logger.logDebug("onBillingServiceDisconnected - ${Thread.currentThread().name}")
                        billingClient.endConnection()
                    }
                })
            }
        }
    }

    private fun BillingResult.isOk(): Boolean {
        return this.responseCode == BillingClient.BillingResponseCode.OK
    }
}
