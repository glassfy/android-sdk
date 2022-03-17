package io.glassfy.androidsdk

import android.app.Activity
import android.content.Context
import io.glassfy.androidsdk.BuildConfig.SDK_VERSION
import io.glassfy.androidsdk.internal.GManager
import io.glassfy.androidsdk.internal.network.model.utils.Resource
import io.glassfy.androidsdk.model.Sku
import io.glassfy.androidsdk.model.SubscriptionUpdate
import kotlinx.coroutines.*

object Glassfy {
    const val sdkVersion = SDK_VERSION

    /**
     * Initialize the SDK
     *
     * @param ctx the [android.content.Context]
     * @param apiKey API Key
     * @param watcherMode Take advantage of our charts and stats without change your existing code
     * @param callback Completion callback with results
     */
    @JvmStatic
    @JvmOverloads
    fun initialize(
        ctx: Context,
        apiKey: String,
        watcherMode: Boolean = false,
        callback: InitializeCallback?
    ) {
        if (callback != null) {
            customScope.runAndPostResult(callback) { manager.initialize(ctx, apiKey, watcherMode) }
        } else {
            customScope.runNoResult { manager.initialize(ctx, apiKey, watcherMode) }
        }
    }

    /**
     * Set purchase delegate
     *
     * @param delegate Interface implementing [io.glassfy.androidsdk.PurchaseDelegate]
     */
    @JvmStatic
    fun setPurchaseDelegate(delegate: PurchaseDelegate) {
        customScope.runNoResult { manager.setPurchaseDelegate(delegate) }
    }

    /**
     * Set log level of the SDK
     *
     * @param level Log level
     */
    @JvmStatic
    fun setLogLevel(level: LogLevel) {
        customScope.runNoResult { manager.setLogLevel(level) }
    }

    /**
     * Fetch offerings
     *
     * @param callback Completion callback with results
     */
    @JvmStatic
    fun offerings(callback: OfferingsCallback) {
        customScope.runAndPostResult(callback) { manager.offerings() }
    }


    /**
     * Fetch Sku
     *
     * @param identifier Sku's identifier
     * @param callback Completion callback with results
     */
    @JvmStatic
    fun sku(identifier: String, callback: SkuCallback) {
        customScope.runAndPostResult(callback) { manager.sku(identifier) }
    }

    /**
     * Fetch Sku
     *
     * @param identifier Store product identifier
     * @param callback Completion callback with results
     */
    @JvmStatic
    fun skuWithProductId(identifier: String, callback: SkuCallback) {
        customScope.runAndPostResult(callback) { manager.skuWithProductId(identifier) }
    }

    /**
     * Chek permissions status of the user
     *
     * @param callback Completion callback with results
     */
    @JvmStatic
    fun permissions(callback: PermissionsCallback) {
        customScope.runAndPostResult(callback) { manager.permissions() }
    }

    /**
     * Restore all user's purchases
     *
     * @param callback Completion callback with results
     */
    @JvmStatic
    fun restore(callback: PermissionsCallback) {
        customScope.runAndPostResult(callback) { manager.restore() }
    }

    /**
     * Make a purchase
     *
     * @param activity An activity reference from which the billing flow will be launched.
     * @param sku The sku that represent the item to buy. To get a reference check [offerings]
     * @param updateSku Details about the sku that you want to upgrade/downgrade
     * @param callback Completion callback with results
     *
     */
    @JvmStatic
    @JvmOverloads
    fun purchase(
        activity: Activity,
        sku: Sku,
        updateSku: SubscriptionUpdate? = null,
        callback: PurchaseCallback
    ) {
        customScope.runAndPostResult(callback) { manager.purchase(activity, sku, updateSku) }
    }


//  UTILS

    internal val manager: GManager by lazy { GManager() }
    internal val customScope by lazy {
        CoroutineScope(
            SupervisorJob() + Dispatchers.IO + CoroutineName(
                "glassfy"
            )
        )
    }

    private fun CoroutineScope.runNoResult(block: suspend CoroutineScope.() -> Unit) {
        launch(block = block)
    }

    private fun <T> CoroutineScope.runAndPostResult(
        callback: Callback<T>,
        block: suspend CoroutineScope.() -> Resource<T>
    ) {
        launch {
            val r = block()
            withContext(Dispatchers.Main) {
                when (r) {
                    is Resource.Success -> {
                        callback.onResult(r.data!!, null)
                    }
                    is Resource.Error -> {
                        callback.onResult(null, r.err!!)
                    }
                }
            }
        }
    }
}
