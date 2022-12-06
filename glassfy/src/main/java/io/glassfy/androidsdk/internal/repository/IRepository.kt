package io.glassfy.androidsdk.internal.repository

import io.glassfy.androidsdk.internal.model.PaywallImpl
import io.glassfy.androidsdk.internal.network.model.request.ConnectRequest
import io.glassfy.androidsdk.internal.network.model.request.InitializeRequest
import io.glassfy.androidsdk.internal.network.model.request.TokenRequest
import io.glassfy.androidsdk.internal.network.model.request.UserPropertiesRequest
import io.glassfy.androidsdk.internal.network.model.response.ServerInfo
import io.glassfy.androidsdk.internal.network.model.utils.Resource
import io.glassfy.androidsdk.model.*

internal interface IRepository {
    suspend fun skuByIdentifier(id: String): Resource<Sku>
    suspend fun skuByIdentifierAndStore(id: String, store: Store): Resource<ISkuBase>
    suspend fun skuByProductId(id: String): Resource<Sku>
    suspend fun offerings(): Resource<Offerings>
    suspend fun token(token: TokenRequest): Resource<Transaction>
    suspend fun permissions(): Resource<Permissions>
    suspend fun lastSeen(): Resource<Unit>
    suspend fun restoreTokens(
        historySubs: List<HistoryPurchase>,
        historyInapp: List<HistoryPurchase>
    ): Resource<Permissions>

    suspend fun initialize(init: InitializeRequest): Resource<ServerInfo>
    suspend fun connectCustomSubscriber(connect: ConnectRequest): Resource<Unit>
    suspend fun connectPaddleLicense(connect: ConnectRequest): Resource<Unit>
    suspend fun storeInfo(): Resource<StoresInfo>
    suspend fun setUserProperty(req: UserPropertiesRequest): Resource<Unit>
    suspend fun getUserProperty(): Resource<UserProperties>
    suspend fun paywall(id: String): Resource<PaywallImpl>
}