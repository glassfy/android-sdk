package io.glassfy.androidsdk

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import io.glassfy.androidsdk.BuildConfig.SDK_VERSION
import io.glassfy.androidsdk.internal.GManager
import io.glassfy.androidsdk.internal.network.model.utils.Resource
import io.glassfy.androidsdk.model.AttributionItem
import io.glassfy.androidsdk.model.Sku
import io.glassfy.androidsdk.model.Store
import io.glassfy.androidsdk.model.SubscriptionUpdate
import kotlinx.coroutines.*

object Glassfy {
    const val sdkVersion = SDK_VERSION

    /**
     * Initialize the SDK
     *
     * @param ctx Android's context [android.content.Context]
     * @param apiKey API Key
     * @param watcherMode Take advantage of our charts and stats without changing your existing code
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
        val opt = InitializeOptions(ctx, apiKey).watcherMode(watcherMode)
        initialize(opt, callback)
    }

    /**
     * SDK initialization options
     *
     * @param context Android's context [android.content.Context]
     * @param apiKey API Key
     */
    class InitializeOptions(val context: Context, val apiKey: String) {
        /**
         * WatcherMode status. By default it's disabled
         * */
        var watcherMode: Boolean = false
            private set

        /**
         * Cross-platform SDK framework
         * */
        var crossPlatformSdkFramework: String? = null
            private set

        /**
         * Cross-platform SDK version
         * */
        var crossPlatformSdkVersion: String? = null
            private set

        /**
         * Set WatcherMode
         * @param enable Enable/Disable WatcherMode
         * */
        fun watcherMode(enable: Boolean) = apply { this.watcherMode = enable }

        /**
         * Cross-platform SDK
         * @param framework Cross-platform SDK framework
         * */
        fun crossPlatformSdkFramework(framework: String?) =
            apply { this.crossPlatformSdkFramework = framework }

        /**
         * Cross-platform SDK
         * @param version Cross-platform SDK version
         * */
        fun crossPlatformSdkVersion(version: String?) =
            apply { this.crossPlatformSdkVersion = version }
    }

    /**
     * Initialize the SDK
     *
     * @param options Initialization options [io.glassfy.androidsdk.Glassfy.InitializeOptions]
     * @param callback Completion callback with results
     */
    @JvmStatic
    fun initialize(
        options: InitializeOptions,
        callback: InitializeCallback?
    ) {
        if (callback != null) {
            customScope.runAndPostResult(callback) { manager.initialize(options) }
        } else {
            customScope.runNoResult { manager.initialize(options) }
        }
    }

    /**
     * Returns a Paywall object which can be used to build a fragment later on.
     *
     * @param remoteConfigurationId Remote configuration identifier
     * @param callback Completion callback with results
     */
    @RequiresApi(Build.VERSION_CODES.N)
    @JvmStatic
    fun paywall(remoteConfigurationId: String, callback: PaywallCallback) {
        customScope.runAndPostResult(callback) { manager.paywall(remoteConfigurationId) }
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
     * Fetch Base Sku info from other stores
     *
     * @param identifier Sku's identifier
     * @param store Store
     * @param callback Completion callback with results
     */
    @JvmStatic
    fun sku(identifier: String, store: Store, callback: SkuBaseCallback) {
        customScope.runAndPostResult(callback) { manager.skubase(identifier, store) }
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

    /**
     * Connect custom subscriber
     *
     * @param customId Custom subscriber id
     * @param callback Completion callback
     *
     */
    @JvmStatic
    fun connectCustomSubscriber(customId: String?, callback: ErrorCallback) {
        customScope.runAndPostErrResult(callback) { manager.connectCustomSubscriber(customId) }
    }

    /**
     * Connect paddle license key
     *
     * @param licenseKey Paddle license key
     * @param force Disconnect license from other subscriber(s) and connect with current subscriber
     * @param callback Completion callback
     *
     * @note  Check [GlassfyErrorCode] LicenseAlreadyConnected, LicenseNotFound to handle those cases
     */
    @JvmStatic
    @JvmOverloads
    fun connectPaddleLicenseKey(
        licenseKey: String,
        force: Boolean = false,
        callback: ErrorCallback
    ) {
        customScope.runAndPostErrResult(callback) {
            manager.connectPaddleLicenseKey(
                licenseKey,
                force
            )
        }
    }

    /**
     * Connect Glassfy Universal Code
     *
     * @param universalCode Glassfy Universal Code
     * @param force Disconnect the code from other subscriber(s) and connect with current subscriber
     * @param callback Completion callback
     *
     * @note  Check [GlassfyErrorCode] UniversalCodeAlreadyConnected, UniversalCodeNotFound to handle those cases
     */
    @JvmStatic
    @JvmOverloads
    fun connectGlassfyUniversalCode(
        universalCode: String,
        force: Boolean = false,
        callback: ErrorCallback
    ) {
        customScope.runAndPostErrResult(callback) {
            manager.connectGlassfyUniversalCode(
                universalCode,
                force
            )
        }
    }

    @JvmStatic
    fun storeInfo(callback: StoreCallback) {
        customScope.runAndPostResult(callback) {
            manager.storeInfo()
        }
    }

    @JvmStatic
    fun setDeviceToken(token: String?, callback: ErrorCallback) {
        customScope.runAndPostErrResult(callback) {
            manager.setDeviceToken(token)
        }
    }

    @JvmStatic
    fun setEmailUserProperty(email: String?, callback: ErrorCallback) {
        customScope.runAndPostErrResult(callback) {
            manager.setEmailUserProperty(email)
        }
    }

    @JvmStatic
    fun setExtraUserProperty(extra: Map<String, String>?, callback: ErrorCallback) {
        customScope.runAndPostErrResult(callback) {
            manager.setExtraUserProperty(extra)
        }
    }

    @JvmStatic
    fun getUserProperties(callback: UserPropertiesCallback) {
        customScope.runAndPostResult(callback) {
            manager.getUserProperties()
        }
    }

    /**
     * Set attribution values
     *
     * @param type Sku's identifier
     * @param value Store
     * @param callback Completion callback
     */
    @JvmStatic
    @JvmOverloads
    fun setAttribution(
        type: AttributionItem.Type,
        value: String?,
        callback: ErrorCallback? = null
    ) {
        if (callback != null) {
            customScope.runAndPostErrResult(callback) { manager.setAttribution(type, value) }
        } else {
            customScope.runNoResult { manager.setAttribution(type, value) }
        }
    }

    /**
     * Set attribution values
     *
     * @param attributions List of attributions
     * @param callback Completion callback
     */
    @JvmStatic
    @JvmOverloads
    fun setAttributions(attributions: List<AttributionItem>, callback: ErrorCallback? = null) {
        if (callback != null) {
            customScope.runAndPostErrResult(callback) { manager.setAttributions(attributions) }
        } else {
            customScope.runNoResult { manager.setAttributions(attributions) }
        }
    }

    /**
     * Purchase history
     *
     * @param callback Completion block
     */
    @JvmStatic
    fun purchaseHistory(callback: PurchaseHistoryCallback) {
        customScope.runAndPostResult(callback) {
            manager.purchaseHistory()
        }
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

    private fun CoroutineScope.runAndPostErrResult(
        callback: ErrorCallback,
        block: suspend CoroutineScope.() -> Resource<Unit>
    ) {
        launch {
            val r = block()
            withContext(Dispatchers.Main) {
                when (r) {
                    is Resource.Success -> {
                        callback.onResult(null)
                    }
                    is Resource.Error -> {
                        callback.onResult(r.err!!)
                    }
                }
            }
        }
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