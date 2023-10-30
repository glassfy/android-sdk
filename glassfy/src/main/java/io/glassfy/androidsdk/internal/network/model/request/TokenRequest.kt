package io.glassfy.androidsdk.internal.network.model.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.glassfy.androidsdk.model.HistoryPurchase
import io.glassfy.androidsdk.model.Purchase
import io.glassfy.androidsdk.model.SkuDetails

@JsonClass(generateAdapter = true)
internal data class TokenRequest(
    @field:Json(name = "purchasesubscription") val isSubscription: Boolean?,
    @field:Json(name = "productid") val productid: List<String>?,
    @field:Json(name = "orderid") val orderid: String?,
    @field:Json(name = "purchasetime") val purchasetime: Long?,
    @field:Json(name = "token") val token: String?,
    @field:Json(name = "quantity") val quantity: Int?,
    @field:Json(name = "offeringidentifier") val offeringId: String?,
    @field:Json(name = "details") val details: SkuDetailsRequest?
) {
    companion object {
        internal fun from(p: HistoryPurchase, isSubscription: Boolean) = TokenRequest(
            isSubscription, p.skus, null, p.purchaseTime, p.purchaseToken, p.quantity, null, null
        )

        internal fun from(p: Purchase, isSubscription: Boolean) = from(
            p, isSubscription, null, null
        )

        internal fun from(
            p: Purchase, isSubscription: Boolean, offeringId: String?, details: SkuDetails?
        ) = TokenRequest(isSubscription,
            p.skus,
            null,
            p.purchaseTime,
            p.purchaseToken,
            p.quantity,
            offeringId,
            details?.let { SkuDetailsRequest.from(it) })
    }
}
