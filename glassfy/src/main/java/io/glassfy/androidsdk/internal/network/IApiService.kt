package io.glassfy.androidsdk.internal.network

import io.glassfy.androidsdk.internal.network.model.request.*
import io.glassfy.androidsdk.internal.network.model.response.*
import io.glassfy.androidsdk.paywall.PaywallResponse
import retrofit2.Response
import retrofit2.http.*

internal interface IApiService {
    @GET("/v0/sku")
    suspend fun getSku(@Query("identifier") skuid: String): Response<SkuResponse>

    @GET("/v1/sku")
    suspend fun getSku(
        @Query("identifier") skuid: String,
        @Query("store") store: Int,
        @Query("pricelocale") locale: String
    ): Response<SkuResponse>

    @POST("/v0/init")
    suspend fun initialize(@Body body: InitializeRequest): Response<InitializeResponse>

    @GET("/v0/offerings")
    suspend fun getOfferingsBilling4(): Response<OfferingsResponse>

    @GET("/v1/offerings")
    suspend fun getOfferingsBilling5(): Response<OfferingsResponse>

    @POST("/v1/token")
    suspend fun postToken(@Body token: TokenRequest): Response<TransactionResponse>

    @GET("/v1/permissions")
    suspend fun getPermissions(): Response<PermissionsResponse>

    @PUT("/v0/lastseen")
    suspend fun putLastSeen()

    @POST("/v1/restoretokens")
    suspend fun postRestoreTokens(@Body tokens: List<TokenRequest>): Response<PermissionsResponse>

    @POST("/v0/connect")
    suspend fun connectCustomSubscriber(@Body body: ConnectRequest): Response<ErrorResponse>

    @POST("/v0/connect")
    suspend fun connectPaddleLicense(@Body body: ConnectRequest): Response<ErrorResponse>

    @POST("/v0/connect")
    suspend fun connectUniversalCode(@Body body: ConnectRequest): Response<ErrorResponse>

    @GET("/v0/storeinfo")
    suspend fun getStoreInfo(): Response<StoresInfoResponse>

    @POST("/v1/property")
    suspend fun postUserProperty(@Body body: UserPropertiesRequest): Response<ErrorResponse>

    @GET("/v0/property")
    suspend fun getUserProperty(): Response<UserPropertiesResponse>

    @POST("/v0/attribution")
    suspend fun postAttributions(@Body body: Map<String, String?>): Response<ErrorResponse>

    @GET("/v0/purchases")
    suspend fun getPurchaseHistory(): Response<PurchaseHistoryResponse>

    @GET("/v1/paywall")
    suspend fun getPaywall(@Query("identifier") paywallId: String,
                           @Query("locale") locale: String): Response<PaywallResponse>
}