package io.glassfy.androidsdk.internal.network

import io.glassfy.androidsdk.internal.network.model.request.InitializeRequest
import io.glassfy.androidsdk.internal.network.model.request.TokenRequest
import io.glassfy.androidsdk.internal.network.model.response.*
import retrofit2.Response
import retrofit2.http.*

internal interface IApiService {
    @GET("/v0/sku")
    suspend fun getSkuById(@Query("identifier") skuid: String): Response<SkuResponse>

    @GET("/v0/sku")
    suspend fun getSkuByProductId(@Query("productid") productid: String): Response<SkuResponse>

    @POST("/v0/init")
    suspend fun initialize(@Body body: InitializeRequest): Response<InitializeResponse>

    @GET("/v0/offerings")
    suspend fun getOfferings(): Response<OfferingsResponse>

    @POST("/v1/token")
    suspend fun postToken(@Body token: TokenRequest): Response<TransactionResponse>

    @GET("/v0/permissions")
    suspend fun getPermissions(): Response<PermissionsResponse>

    @PUT("/v0/lastseen")
    suspend fun putLastSeen()

    @POST("/v0/restoretokens")
    suspend fun postRestoreTokens(@Body tokens: List<TokenRequest>): Response<PermissionsResponse>
}