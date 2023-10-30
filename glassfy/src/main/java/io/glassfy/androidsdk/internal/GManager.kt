package io.glassfy.androidsdk.internal

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.android.billingclient.api.Purchase.PurchaseState.PURCHASED
import com.squareup.moshi.Moshi
import io.glassfy.androidsdk.Glassfy
import io.glassfy.androidsdk.GlassfyErrorCode
import io.glassfy.androidsdk.LogLevel
import io.glassfy.androidsdk.PurchaseDelegate
import io.glassfy.androidsdk.internal.billing.IBillingPurchaseDelegate
import io.glassfy.androidsdk.internal.billing.IBillingService
import io.glassfy.androidsdk.internal.billing.SkuDetailsQuery
import io.glassfy.androidsdk.internal.billing.play.PlayBillingServiceProvider
import io.glassfy.androidsdk.internal.cache.CacheManager
import io.glassfy.androidsdk.internal.cache.ICacheManager
import io.glassfy.androidsdk.internal.device.DeviceManager
import io.glassfy.androidsdk.internal.device.IDeviceManager
import io.glassfy.androidsdk.internal.logger.Logger
import io.glassfy.androidsdk.internal.network.IApiService
import io.glassfy.androidsdk.internal.network.model.AttributionItemTypeDto
import io.glassfy.androidsdk.internal.network.model.request.ConnectRequest
import io.glassfy.androidsdk.internal.network.model.request.InitializeRequest
import io.glassfy.androidsdk.internal.network.model.request.TokenRequest
import io.glassfy.androidsdk.internal.network.model.request.UserPropertiesRequest
import io.glassfy.androidsdk.internal.network.model.utils.*
import io.glassfy.androidsdk.internal.repository.IRepository
import io.glassfy.androidsdk.internal.repository.Repository
import io.glassfy.androidsdk.model.*
import io.glassfy.androidsdk.paywall.Paywall
import io.glassfy.androidsdk.paywall.PaywallTypeAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit


internal class GManager : LifecycleEventObserver, IBillingPurchaseDelegate {
    companion object {
        private const val INITIALIZED_TIMEOUT_MS = 10000L
    }

    private enum class SdkState {
        NotInitialized, Initializing, Failed, Initialized
    }

    private val state = MutableStateFlow(SdkState.NotInitialized)

    @Volatile
    private var packageName: String? = null

    @Volatile
    private var installTime: Long? = null

    @Volatile
    private var watcherMode = false

    @Volatile
    private var _delegate: PurchaseDelegate? = null

    @Volatile
    private lateinit var billingService: IBillingService

    @Volatile
    private lateinit var repository: IRepository

    @Volatile
    private lateinit var cacheManager: ICacheManager

    internal suspend fun initialize(opt: Glassfy.InitializeOptions): Resource<Boolean> {
        val currState =
            state.getAndUpdate { if (it == SdkState.NotInitialized) SdkState.Initializing else it }
        if (currState == SdkState.Initializing) {
            Logger.logDebug("Sdk in initializing state")
            return Resource.Success(false)
        }
        if (currState == SdkState.Initialized) {
            Logger.logDebug("Sdk already initialized")
            return Resource.Success(false)
        }

        // destructuring opt
        val ctx = opt.context
        val apiKey = opt.apiKey
        val watcherMode = opt.watcherMode
        val crossPlatformSdkFramework = opt.crossPlatformSdkFramework
        val crossPlatformSdkVersion = opt.crossPlatformSdkVersion

        // init
        this.watcherMode = watcherMode
        val appContext = ctx.applicationContext
        packageName = appContext.packageName
        installTime = packageName?.runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ctx.packageManager.getPackageInfo(
                    this, PackageManager.PackageInfoFlags.of(0)
                ).firstInstallTime
            } else {
                @Suppress("DEPRECATION") ctx.packageManager.getPackageInfo(this, 0).firstInstallTime
            }
        }?.getOrNull()
        cacheManager = CacheManager(appContext.applicationContext)
        val deviceManager: IDeviceManager = DeviceManager(appContext.applicationContext)
        val apiService: IApiService = makeApiService(cacheManager, deviceManager, apiKey)
        repository = Repository(apiService)
        billingService = PlayBillingServiceProvider.billingService(this, appContext, watcherMode)

        val res = _initialize(crossPlatformSdkFramework, crossPlatformSdkVersion)
        if (res.err != null) {
            return res
        }

        withContext(Dispatchers.Main) {
            ProcessLifecycleOwner.get().lifecycle.addObserver(Glassfy.manager)
        }
        return Resource.Success(true)
    }

    internal fun setPurchaseDelegate(delegate: PurchaseDelegate) {
        _delegate = delegate
    }

    internal fun setLogLevel(level: LogLevel) {
        Logger.loglevel = level
    }

    internal suspend fun purchase(
        activity: Activity, sku: Sku, upgradeSku: SubscriptionUpdate?
    ): Resource<Transaction> = withSdkInitializedOrError {
        _purchase(
            activity, sku, upgradeSku, cacheManager.subscriberId
        )
    }

    internal suspend fun permissions(): Resource<Permissions> = withSdkInitializedOrError {
        repository.permissions().apply {
            data?.installationId_ = cacheManager.installationId
        }
    }

    internal suspend fun restore(): Resource<Permissions> = withSdkInitializedOrError { _restore() }

    internal suspend fun sku(identifier: String): Resource<Sku> =
        withSdkInitializedOrError { _playStoreSku(identifier) }

    internal suspend fun skubase(identifier: String, store: Store): Resource<out ISkuBase> =
        withSdkInitializedOrError { _skubase(identifier, store) }

    internal suspend fun offerings(): Resource<Offerings> =
        withSdkInitializedOrError { _offerings() }

    internal suspend fun connectCustomSubscriber(customId: String?): Resource<Unit> =
        withSdkInitializedOrError { _connectCustomSubscriber(customId) }

    internal suspend fun connectPaddleLicenseKey(
        licenseKey: String, force: Boolean
    ): Resource<Unit> = withSdkInitializedOrError { _connectPaddleLicense(licenseKey, force) }

    internal suspend fun connectGlassfyUniversalCode(
        universalCode: String, force: Boolean
    ): Resource<Unit> =
        withSdkInitializedOrError { _connectGlassfyUniversalCode(universalCode, force) }

    internal suspend fun storeInfo(): Resource<StoresInfo> =
        withSdkInitializedOrError { repository.storeInfo() }

    internal suspend fun setDeviceToken(token: String?): Resource<Unit> =
        withSdkInitializedOrError { repository.setUserProperty(UserPropertiesRequest.Token(token)) }

    internal suspend fun setEmailUserProperty(email: String?): Resource<Unit> =
        withSdkInitializedOrError { repository.setUserProperty(UserPropertiesRequest.Email(email)) }

    internal suspend fun setExtraUserProperty(extra: Map<String, String>?): Resource<Unit> =
        withSdkInitializedOrError { repository.setUserProperty(UserPropertiesRequest.Extra(extra)) }

    internal suspend fun getUserProperties(): Resource<UserProperties> =
        withSdkInitializedOrError { repository.getUserProperty() }

    @RequiresApi(Build.VERSION_CODES.N)
    internal suspend fun paywall(remoteConfigurationId: String): Resource<Paywall> =
        withSdkInitializedOrError { _paywall(remoteConfigurationId) }

    internal suspend fun setAttribution(
        type: AttributionItem.Type, value: String?
    ): Resource<Unit> = setAttributions(listOf(AttributionItem(type, value)))

    internal suspend fun setAttributions(attributions: List<AttributionItem>): Resource<Unit> =
        withSdkInitializedOrError {
            attributions.associate {
                AttributionItemTypeDto.field(it.type) to it.value
            }.let {
                repository.setAttributions(it)
            }
        }

    internal suspend fun purchaseHistory(): Resource<PurchasesHistory> =
        withSdkInitializedOrError { repository.getPurchaseHistory() }

    /// Impl

    private suspend fun _initialize(
        crossPlatformSdkFramework: String? = null, crossPlatformSdkVersion: String? = null
    ): Resource<Boolean> {
        state.emit(SdkState.Initializing)

        val inappHRes = billingService.inAppPurchaseHistory()
        if (inappHRes.err != null) {
            state.emit(SdkState.Failed)
            return Resource.Error(inappHRes.err)
        }
        val subsHRes = billingService.subsPurchaseHistory()
        if (subsHRes.err != null) {
            state.emit(SdkState.Failed)
            return Resource.Error(subsHRes.err)
        }

        val inappRes = billingService.inAppPurchases()
        if (inappRes.err != null) {
            state.emit(SdkState.Failed)
            return Resource.Error(inappRes.err)
        }
        val subsRes = billingService.subsPurchases()
        if (subsRes.err != null) {
            state.emit(SdkState.Failed)
            return Resource.Error(subsRes.err)
        }

        val initReq = InitializeRequest.from(
            packageName ?: "",
            subsHRes.data.orEmpty(),
            inappHRes.data.orEmpty(),
            subsRes.data.orEmpty(),
            inappRes.data.orEmpty(),
            installTime,
            crossPlatformSdkFramework,
            crossPlatformSdkVersion
        )
        val serverInfo = repository.initialize(initReq)
        if (serverInfo.err != null) {
            state.emit(SdkState.Failed)
            return Resource.Error(serverInfo.err)
        }

        val subscriberId = serverInfo.data?.subscriberId
        if (subscriberId.isNullOrEmpty()) {
            state.emit(SdkState.Failed)
            return Resource.Error(GlassfyErrorCode.SDKNotInitialized.toError("SubscriberId cannot be found"))
        }
        cacheManager.subscriberId = subscriberId

        // consume/ack purchases
        if (!watcherMode) {
            inappRes.data?.forEach {
                billingService.consume(it.purchaseToken)
                onProductPurchase(it, false)
            }
            subsRes.data?.forEach {
                if (!it.isAcknowledged) {
                    billingService.acknowledge(it.purchaseToken)
                    onProductPurchase(it, true)
                }
            }
        }

        state.emit(SdkState.Initialized)

        return Resource.Success(true)
    }

    private suspend fun _offerings(): Resource<Offerings> {
        val offRes = repository.offerings(billingService.version)
        if (offRes.err != null) return offRes
        if (offRes.data == null) return Resource.Error(GlassfyErrorCode.NotFoundOnGlassfy.toError())

        val queries =
            offRes.data.all.flatMap { off -> SkuDetailsQuery.fromSkus(off.skus) }.distinct()

        val detailRes = billingService.skusDetails(queries).data ?: emptyList()
        for (o in offRes.data.all) {
            o.skus_ = o.skus.mapNotNull { s ->
                matchSkuWithStoreDetails(s, detailRes)?.let {
                    s.apply {
                        product = it
                    }
                }
            }
        }
        return Resource.Success(offRes.data)
    }

    private fun matchSkuWithStoreDetails(s: Sku, storeDetails: List<SkuDetails>) =
        matchSkuWithStoreDetailsAndFallback(s, storeDetails)

    private fun matchSkuWithStoreDetailsAndFallback(
        s: Sku, storeDetails: List<SkuDetails>
    ): SkuDetails? {
        return storeDetails.find {
            it.sku == s.skuParams.productId && it.basePlanId == s.skuParams.basePlanId.orEmpty() && it.offerId == s.skuParams.offerId.orEmpty()
        }?.also {
            Logger.logDebug(
                "Sku Found ${s.skuId}: " + "\t${s.skuParams.productId} - ${s.skuParams.basePlanId} - ${s.skuParams.offerId}"
            )
        } ?: storeDetails.find {
            it.sku == s.fallbackSkuParams?.productId && it.basePlanId == s.fallbackSkuParams.basePlanId.orEmpty() && it.offerId == s.fallbackSkuParams.offerId.orEmpty()
        }.also {
            if (it == null) {
                Logger.logDebug(
                    "Sku NOT Found ${s.skuId}: " + "\t${s.skuParams.productId} - ${s.skuParams.basePlanId} - ${s.skuParams.offerId}"
                )
            } else {
                Logger.logDebug(
                    "Sku Fallback ${s.skuId}: " + "\n\t${s.skuParams.productId} - ${s.skuParams.basePlanId} - ${s.skuParams.offerId}" + "\n\t" +
                            "${it.sku} - ${it.basePlanId.ifEmpty { null }} - ${it.offerId.ifEmpty { null }}"
                )
            }
        }
    }

    private suspend fun _purchase(
        activity: Activity, sku: Sku, upgradeSku: SubscriptionUpdate?, accountId: String?
    ): Resource<Transaction> {
        if (upgradeSku != null) {
            val purchases = billingService.allPurchases()
            if (purchases.err != null) return Resource.Error(purchases.err)

            val res = repository.skuByIdentifier(upgradeSku.originalSku)
            if (res.err != null) return Resource.Error(res.err)

            listOfNotNull(
                res.data?.skuParams?.productId, res.data?.fallbackSkuParams?.productId
            ).firstNotNullOfOrNull { purchasedProduct ->
                purchases.data?.firstOrNull { purchase ->
                    purchase.skus.contains(purchasedProduct)
                }
            }?.also { purchase ->
                upgradeSku.purchaseToken = purchase.purchaseToken
            }

            if (upgradeSku.purchaseToken.isEmpty()) {
                return Resource.Error(
                    GlassfyErrorCode.MissingPurchase.toError(
                        "purchaseToken not found for ${upgradeSku.originalSku}"
                    )
                )
            }
        }

        val result = billingService.purchase(
            activity, sku.product, upgradeSku, accountId
        )
        if (result.err != null) return Resource.Error(result.err)
        if (result.data == null) return Resource.Error(GlassfyErrorCode.MissingPurchase.toError())
        if (result.data.purchaseState != PURCHASED) return Resource.Error(GlassfyErrorCode.PendingPurchase.toError())

        return result.data.let { p ->
            val tokenReq = TokenRequest.from(p, sku.isSubscription(), sku.offeringId, sku.product)

            repository.token(tokenReq).apply {
                data?.permissions?.installationId_ = cacheManager.installationId
            }
        }
    }

    private suspend fun _restore(): Resource<Permissions> {
        val inappHistoryRes = billingService.inAppPurchaseHistory()
        if (inappHistoryRes.err != null) return Resource.Error(inappHistoryRes.err)

        val subsHistoryRes = billingService.subsPurchaseHistory()
        if (subsHistoryRes.err != null) return Resource.Error(subsHistoryRes.err)

        return repository.restoreTokens(
            subsHistoryRes.data.orEmpty(), inappHistoryRes.data.orEmpty()
        )
    }

    private suspend fun _skubase(identifier: String, store: Store): Resource<out ISkuBase> {
        if (store == Store.PlayStore) {
            return _playStoreSku(identifier)
        }

        val skuRes = repository.skuByIdentifierAndStore(identifier, store)
        if (skuRes.err != null) return skuRes
        if (skuRes.data == null) return Resource.Error(GlassfyErrorCode.NotFoundOnGlassfy.toError())
        return Resource.Success(skuRes.data)
    }

    private suspend fun _playStoreSku(identifier: String): Resource<Sku> {
        val skuRes = repository.skuByIdentifier(identifier)
        if (skuRes.err != null) return skuRes
        if (skuRes.data == null) return Resource.Error(GlassfyErrorCode.NotFoundOnGlassfy.toError())

        val detailRes = billingService.skusDetails(SkuDetailsQuery.fromSku(skuRes.data))
        if (detailRes.err != null) return Resource.Error(detailRes.err)
        if (detailRes.data == null) return Resource.Error(GlassfyErrorCode.NotFoundOnGlassfy.toError())

        return matchSkuWithStoreDetails(skuRes.data, detailRes.data)?.let {
            skuRes.data.product = it
            Resource.Success(skuRes.data)
        } ?: Resource.Error(GlassfyErrorCode.NotFoundOnGlassfy.toError())
    }

    private suspend fun _connectCustomSubscriber(customId: String?): Resource<Unit> {
        val res = repository.connectCustomSubscriber(ConnectRequest.customSubscriber(customId))
        return if (res.err != null) Resource.Error(res.err) else Resource.Success(Unit)
    }

    private suspend fun _connectPaddleLicense(licenseKey: String, force: Boolean): Resource<Unit> {
        val res = repository.connectPaddleLicense(ConnectRequest.paddleLicense(licenseKey, force))
        return if (res.err != null) Resource.Error(res.err) else Resource.Success(Unit)
    }

    private suspend fun _connectGlassfyUniversalCode(
        universalCode: String, force: Boolean
    ): Resource<Unit> {
        val res = repository.connectGlassfyUniversalCode(
            ConnectRequest.universalCode(
                universalCode, force
            )
        )
        return if (res.err != null) Resource.Error(res.err) else Resource.Success(Unit)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private suspend fun _paywall(remoteConfigurationId: String): Resource<Paywall> {
        val pRes = repository.paywall(remoteConfigurationId)
        if (pRes.err != null) return Resource.Error(pRes.err)
        if (pRes.data == null || pRes.data.skus.isEmpty()) return Resource.Error(GlassfyErrorCode.NotFoundOnGlassfy.toError())

        val dRes = billingService.skusDetails(SkuDetailsQuery.fromSkus(pRes.data.skus))
        if (dRes.err != null) return Resource.Error(dRes.err)
        if (dRes.data == null) return Resource.Error(GlassfyErrorCode.NotFoundOnStore.toError())

        pRes.data.skus = pRes.data.skus.mapNotNull { s ->
            matchSkuWithStoreDetails(s, dRes.data)?.let {
                s.apply {
                    product = it
                }
            }
        }
        return Resource.Success(pRes.data)
    }

/// LifecycleEventObserver

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_START -> Glassfy.customScope.launch {
                onStartProcessState()
            }

            else -> {
//                Logger.logDebug("${event.name} - ${Thread.currentThread().name}")
            }
        }
    }

    private suspend fun onStartProcessState() = withSdkInitialized()?.also { repository.lastSeen() }


/// Utils

    private fun makeApiService(
        cacheManager: ICacheManager, deviceManager: IDeviceManager, apiKey: String
    ): IApiService {
        val httpClient = OkHttpClient.Builder().readTimeout(20L, TimeUnit.SECONDS)
            .writeTimeout(20L, TimeUnit.SECONDS).connectTimeout(20L, TimeUnit.SECONDS)
            .addInterceptor { c ->
                val original = c.request()
                val url =
                    original.url.newBuilder().addEncodedQueryParameter("glii", deviceManager.glii)
                        .addEncodedQueryParameter("installationid", cacheManager.installationId)
                        .addEncodedQueryParameter("subscriberid", cacheManager.subscriberId).build()
                val r = c.request().newBuilder().header("Authorization", "Bearer $apiKey").url(url)
                    .build()
                c.proceed(r)
            }
//            .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
            .build()

        val moshi =
            Moshi.Builder().add(EntitlementAdapter()).add(ProductTypeAdapter()).add(StoreAdapter())
                .add(StoreInfoAdapter()).add(EventTypeAdapter()).add(UserPropertiesAdapter())
                .add(PaywallTypeAdapter())
                // .addLast(KotlinJsonAdapterFactory()) if not using Codegen, use Reflection (2.5 MiB .jar file)
                .build()

        return Retrofit.Builder().client(httpClient).baseUrl("https://api.glassfy.io")
            .addConverterFactory(MoshiConverterFactory.create(moshi)).build()
            .create(IApiService::class.java)
    }

    private suspend inline fun <T> withSdkInitializedOrError(
        block: () -> Resource<T>
    ): Resource<T> {
        return withSdkInitialized()?.let { block() }
            ?: Resource.Error(GlassfyErrorCode.SDKNotInitialized.toError())
    }

    private suspend fun withSdkInitialized(): Boolean? {
        return withTimeoutOrNull(INITIALIZED_TIMEOUT_MS) {
            // try initialization again if previously failed
            if (state.value == SdkState.Failed) {
                _initialize()
            }
            state.first { it == SdkState.Initialized }
            true
        }
    }


    //// IBillingPurchaseDelegate
    override suspend fun onProductPurchase(p: Purchase, isSubscription: Boolean) {
        withContext(Dispatchers.Main) {
            _delegate?.onProductPurchase(p)
        }

        if (watcherMode) {
            repository.token(TokenRequest.from(p, isSubscription))
        }
    }
}
