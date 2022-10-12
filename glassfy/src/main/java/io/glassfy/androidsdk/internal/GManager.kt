package io.glassfy.androidsdk.internal

import android.app.Activity
import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase.PurchaseState.PURCHASED
import com.squareup.moshi.Moshi
import io.glassfy.androidsdk.Glassfy
import io.glassfy.androidsdk.GlassfyErrorCode
import io.glassfy.androidsdk.LogLevel
import io.glassfy.androidsdk.PurchaseDelegate
import io.glassfy.androidsdk.internal.billing.IBillingService
import io.glassfy.androidsdk.internal.billing.google.PlayBillingService
import io.glassfy.androidsdk.internal.cache.CacheManager
import io.glassfy.androidsdk.internal.cache.ICacheManager
import io.glassfy.androidsdk.internal.device.DeviceManager
import io.glassfy.androidsdk.internal.device.IDeviceManager
import io.glassfy.androidsdk.internal.logger.Logger
import io.glassfy.androidsdk.internal.network.IApiService
import io.glassfy.androidsdk.internal.network.model.request.ConnectRequest
import io.glassfy.androidsdk.internal.network.model.request.InitializeRequest
import io.glassfy.androidsdk.internal.network.model.request.TokenRequest
import io.glassfy.androidsdk.internal.network.model.request.UserPropertiesRequest
import io.glassfy.androidsdk.internal.network.model.utils.*
import io.glassfy.androidsdk.internal.repository.IRepository
import io.glassfy.androidsdk.internal.repository.Repository
import io.glassfy.androidsdk.model.*
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


internal class GManager : LifecycleEventObserver {
    companion object {
        private const val INITIALIZED_TIMEOUT_MS = 10000L
    }

    private enum class SdkState {
        NotInitialized,
        Initializing,
        Failed,
        Initialized
    }

    private val state = MutableStateFlow(SdkState.NotInitialized)

    @Volatile
    private var packageName: String? = null

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


    internal suspend fun initialize(
        ctx: Context,
        apiKey: String,
        watcherMode: Boolean
    ): Resource<Boolean> {
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

        // init
        val appContext = ctx.applicationContext
        packageName = appContext.packageName
        cacheManager = CacheManager(appContext.applicationContext)
        val deviceManager: IDeviceManager = DeviceManager(appContext.applicationContext)
        val apiService: IApiService = makeApiService(cacheManager, deviceManager, apiKey)
        repository = Repository(apiService)
        billingService = PlayBillingService(appContext, watcherMode)
        billingService.setDelegate(_delegate)

        val res = _initialize()
        if (res.err != null) {
            return res
        }

        withContext(Dispatchers.Main) {
            ProcessLifecycleOwner.get().lifecycle.addObserver(Glassfy.manager)
        }

        return Resource.Success(true)
    }

    internal suspend fun setPurchaseDelegate(delegate: PurchaseDelegate) {
        _delegate = delegate

        withSdkInitialized()?.also {
            billingService.setDelegate(_delegate)
        }
    }

    internal fun setLogLevel(level: LogLevel) {
        Logger.loglevel = level
    }

    internal suspend fun purchase(
        activity: Activity,
        sku: Sku,
        upgradeSku: SubscriptionUpdate?
    ): Resource<Transaction> =
        withSdkInitializedOrError {
            _purchase(
                activity,
                sku,
                upgradeSku,
                cacheManager.subscriberId
            )
        }

    internal suspend fun permissions(): Resource<Permissions> =
        withSdkInitializedOrError {
            repository.permissions().apply {
                data?.installationId_ = cacheManager.installationId
            }
        }

    internal suspend fun restore(): Resource<Permissions> =
        withSdkInitializedOrError { _restore() }

    internal suspend fun sku(identifier: String): Resource<Sku> =
        withSdkInitializedOrError { _playstoresku(identifier) }

    internal suspend fun skubase(identifier: String, store: Store): Resource<out ISkuBase> =
        withSdkInitializedOrError { _skubase(identifier, store) }

    internal suspend fun skuWithProductId(identifier: String): Resource<Sku> =
        withSdkInitializedOrError { _skuWithProductId(identifier) }

    internal suspend fun offerings(): Resource<Offerings> =
        withSdkInitializedOrError { _offerings() }

    internal suspend fun connectCustomSubscriber(customId: String?): Resource<Unit> =
        withSdkInitializedOrError { _connectCustomSubscriber(customId) }

    internal suspend fun connectPaddleLicenseKey(
        licenseKey: String,
        force: Boolean
    ): Resource<Unit> =
        withSdkInitializedOrError { _connectPaddleLicense(licenseKey, force) }

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

    /// Impl

    private suspend fun _initialize(): Resource<Boolean> {
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

        val initReq = InitializeRequest.from(
            packageName ?: "",
            subsHRes.data.orEmpty(),
            inappHRes.data.orEmpty()
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
            billingService.inAppPurchases().data?.forEach {
                billingService.consume(it.purchaseToken)
                withContext(Dispatchers.Main) {
                    _delegate?.onProductPurchase(it)
                }
            }
            billingService.subsPurchases().data?.forEach {
                if (!it.isAcknowledged) {
                    billingService.acknowledge(it.purchaseToken)
                    withContext(Dispatchers.Main) {
                        _delegate?.onProductPurchase(it)
                    }
                }
            }
        }
        state.emit(SdkState.Initialized)

        return Resource.Success(true)
    }

    private suspend fun _offerings(): Resource<Offerings> {
        val offRes = repository.offerings()
        if (offRes.err != null) return offRes
        if (offRes.data == null) return Resource.Error(GlassfyErrorCode.NotFoundOnGlassfy.toError())

        val detailRes = offRes.data.all
            .flatMap { it.skus.map { s -> s.productId } }
            .toSet()
            .let { billingService.skuDetails(it) }
        if (detailRes.err != null) return Resource.Error(detailRes.err)
        if (detailRes.data == null) return Resource.Error(GlassfyErrorCode.NotFoundOnStore.toError())

        for (o in offRes.data.all) {
            o.skus_ = o.skus.filter { sku -> detailRes.data.map { it.sku }.contains(sku.productId) }
                .map { s ->
                    detailRes.data
                        .find { detail -> detail.sku == s.productId }
                        ?.let { detail -> s.product = detail }
                    return@map s
                }
        }
        return offRes
    }

    private suspend fun _purchase(
        activity: Activity,
        sku: Sku,
        upgradeSku: SubscriptionUpdate?,
        accountId: String?
    ): Resource<Transaction> {
        if (upgradeSku != null) {
            val res = repository.skuByIdentifier(upgradeSku.originalSku)
            if (res.err != null) return Resource.Error(res.err)

            val purchases = billingService.allPurchases()
            if (purchases.err != null) return Resource.Error(purchases.err)
            purchases.data
                ?.firstOrNull { it.skus.contains(res.data?.productId ?: "") }
                ?.let { upgradeSku.purchaseToken = it.purchaseToken }

            if (upgradeSku.purchaseToken.isEmpty()) {
                return Resource.Error(GlassfyErrorCode.MissingPurchase.toError("purchaseToken not found for ${upgradeSku.originalSku}"))
            }
        }

        val result = billingService.purchase(activity, sku.product, upgradeSku, accountId)
        if (result.err != null) return Resource.Error(result.err)
        if (result.data == null) return Resource.Error(GlassfyErrorCode.MissingPurchase.toError())
        if (result.data.purchaseState != PURCHASED) return Resource.Error(GlassfyErrorCode.PendingPurchase.toError())

        return result.data.let { p ->
            val tokenReq = TokenRequest.from(p, sku.product.type == BillingClient.SkuType.SUBS)
            tokenReq.offeringId = sku.offeringId
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
            subsHistoryRes.data.orEmpty(),
            inappHistoryRes.data.orEmpty()
        )
    }

    private suspend fun _skubase(identifier: String, store: Store): Resource<out ISkuBase> {
        if (store == Store.PlayStore) {
            return _playstoresku(identifier)
        }

        val skuRes = repository.skuByIdentifierAndStore(identifier, store)
        if (skuRes.err != null) return skuRes
        if (skuRes.data == null) return Resource.Error(GlassfyErrorCode.NotFoundOnGlassfy.toError())
        return Resource.Success(skuRes.data)
    }

    private suspend fun _playstoresku(identifier: String): Resource<Sku> {
        val skuRes = repository.skuByIdentifier(identifier)
        if (skuRes.err != null) return skuRes
        if (skuRes.data == null) return Resource.Error(GlassfyErrorCode.NotFoundOnGlassfy.toError())

        return skuRes.data.let {
            val detailRes = billingService.skuDetails(setOf(it.productId))
            if (detailRes.err != null) return Resource.Error(detailRes.err)
            if (detailRes.data == null || detailRes.data.isEmpty()) return Resource.Error(
                GlassfyErrorCode.NotFoundOnStore.toError()
            )

            Resource.Success(it.apply {
                product = detailRes.data.first()
            })
        }
    }

    private suspend fun _skuWithProductId(identifier: String): Resource<Sku> {
        val skuRes = repository.skuByProductId(identifier)
        if (skuRes.err != null) return skuRes
        if (skuRes.data == null) return Resource.Error(GlassfyErrorCode.NotFoundOnGlassfy.toError())

        return skuRes.data.let {
            val detailRes = billingService.skuDetails(setOf(it.productId))
            if (detailRes.err != null) return Resource.Error(detailRes.err)
            if (detailRes.data == null || detailRes.data.isEmpty()) return Resource.Error(
                GlassfyErrorCode.NotFoundOnStore.toError()
            )

            Resource.Success(it.apply {
                product = detailRes.data.first()
            })
        }
    }

    private suspend fun _connectCustomSubscriber(customId: String?): Resource<Unit> {
        val res = repository.connectCustomSubscriber(ConnectRequest.customSubscriber(customId))
        return if (res.err != null) Resource.Error(res.err) else Resource.Success(Unit)
    }

    private suspend fun _connectPaddleLicense(licenseKey: String, force: Boolean): Resource<Unit> {
        val res = repository.connectPaddleLicense(ConnectRequest.paddleLicense(licenseKey, force))
        return if (res.err != null) Resource.Error(res.err) else Resource.Success(Unit)
    }


    /// LifecycleEventObserver

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_START ->
                Glassfy.customScope.launch {
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
        cacheManager: ICacheManager,
        deviceManager: IDeviceManager,
        apiKey: String
    ): IApiService {
        val httpClient = OkHttpClient.Builder()
            .addInterceptor { c ->
                val original = c.request()
                val url = original.url.newBuilder()
                    .addEncodedQueryParameter("glii", deviceManager.glii)
                    .addEncodedQueryParameter("installationid", cacheManager.installationId)
                    .addEncodedQueryParameter("subscriberid", cacheManager.subscriberId)
                    .build()
                val r = c.request().newBuilder()
                    .header("Authorization", "Bearer $apiKey")
                    .url(url)
                    .build()
                c.proceed(r)
            }
//            .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
            .build()

        val moshi = Moshi.Builder()
            .add(EntitlementAdapter())
            .add(StoreAdapter())
            .add(StoreInfoAdapter())
            .add(UserPropertiesAdapter())
            // .addLast(KotlinJsonAdapterFactory()) if not using Codegen, use Reflection (2.5 MiB .jar file)
            .build()

        return Retrofit.Builder()
            .client(httpClient)
            .baseUrl("https://api.glassfy.io")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build().create(IApiService::class.java)
    }

    private suspend inline fun <T> withSdkInitializedOrError(
        block: () -> Resource<T>
    ): Resource<T> =
        withSdkInitialized()?.let { block() }
            ?: Resource.Error(GlassfyErrorCode.SDKNotInitialized.toError())

    private suspend fun withSdkInitialized(): Boolean? = withTimeoutOrNull(INITIALIZED_TIMEOUT_MS) {
        // try initialization again if previously failed
        if (state.value == SdkState.Failed) {
            _initialize()
        }
        state.first { it == SdkState.Initialized }
        true
    }
}