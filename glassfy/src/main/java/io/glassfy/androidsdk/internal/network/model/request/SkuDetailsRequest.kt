package io.glassfy.androidsdk.internal.network.model.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.glassfy.androidsdk.model.SkuDetails

@JsonClass(generateAdapter = true)
internal data class SkuDetailsRequest(
    @field:Json(name = "productid") val sku: String?,
    @field:Json(name = "baseplan") var basePlanId: String?,
    @field:Json(name = "offerid") var offerId: String?,

    @field:Json(name = "subscription_period") val subscriptionPeriod: String?,
    @field:Json(name = "freetrial_period") val freeTrialPeriod: String?,

    @field:Json(name = "price_currency") val priceCurrencyCode: String?,
    @field:Json(name = "price_micro") val priceAmountMicro: Long?,
    @field:Json(name = "originalprice_micro") val originalPriceAmountMicro: Long?,

    @field:Json(name = "intro_micro") val introductoryPriceAmountMicro: Long?,
    @field:Json(name = "intro_cycles") val introductoryPriceAmountCycles: Int?,
    @field:Json(name = "intro_period") val introductoryPriceAmountPeriod: String?,
) {
    companion object {
        internal fun from(s: SkuDetails) =
            SkuDetailsRequest(
                s.sku.ifEmpty { null },
                s.basePlanId.ifEmpty { null },
                s.offerId.ifEmpty { null },
                s.subscriptionPeriod.ifEmpty { null },
                s.freeTrialPeriod.ifEmpty { null },
                s.priceCurrencyCode.ifEmpty { null },
                s.priceAmountMicro.takeIf { it > 0 },
                s.originalPriceAmountMicro.takeIf { it != s.priceAmountMicro },
                s.introductoryPriceAmountMicro.takeIf { it > 0 },
                s.introductoryPriceAmountCycles.takeIf { it > 0 },
                s.introductoryPriceAmountPeriod.ifEmpty { null }
            )
    }
}