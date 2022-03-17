package io.glassfy.androidsdk.internal.repository

import io.glassfy.androidsdk.internal.network.model.request.InitializeRequest
import io.glassfy.androidsdk.internal.network.model.request.TokenRequest
import io.glassfy.androidsdk.internal.network.model.response.ServerInfo
import io.glassfy.androidsdk.internal.network.model.utils.Resource
import io.glassfy.androidsdk.model.*

internal interface IRepository {
    suspend fun skuByIdentifier(id: String): Resource<Sku>
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
}