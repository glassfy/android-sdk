package io.glassfy.androidsdk.internal.billing

import android.app.Activity
import io.glassfy.androidsdk.internal.network.model.utils.Resource
import io.glassfy.androidsdk.model.HistoryPurchase
import io.glassfy.androidsdk.model.Purchase
import io.glassfy.androidsdk.model.SkuDetails
import io.glassfy.androidsdk.model.SubscriptionUpdate

internal interface IBillingService {
    val version: Int
    val delegate: IBillingPurchaseDelegate

    suspend fun inAppPurchaseHistory(): Resource<List<HistoryPurchase>>
    suspend fun subsPurchaseHistory(): Resource<List<HistoryPurchase>>
    suspend fun allPurchaseHistory(): Resource<List<HistoryPurchase>>

    suspend fun inAppPurchases(): Resource<List<Purchase>>
    suspend fun subsPurchases(): Resource<List<Purchase>>
    suspend fun allPurchases(): Resource<List<Purchase>>
    
    suspend fun skuDetails(query: SkuDetailsQuery): Resource<SkuDetails>
    suspend fun skusDetails(queries: List<SkuDetailsQuery>): Resource<List<SkuDetails>>

    suspend fun purchase(
        activity: Activity,
        product: SkuDetails,
        update: SubscriptionUpdate? = null,
        accountId: String? = null
    ): Resource<Purchase>

    suspend fun consume(purchaseToken: String): Resource<String?>
    suspend fun acknowledge(purchaseToken: String): Resource<String?>
}
