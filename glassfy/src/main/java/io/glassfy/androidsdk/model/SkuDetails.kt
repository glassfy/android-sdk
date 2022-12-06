package io.glassfy.androidsdk.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SkuDetails(
    val description: String,
    val freeTrialPeriod: String,
    val iconUrl: String,
    val introductoryPrice: String,
    val introductoryPriceAmountMicro: Long,
    val introductoryPriceAmountCycles: Int,
    val introductoryPriceAmountPeriod: String,
    val originalPrice: String,
    val originalPriceAmountMicro: Long,
    val price: String,
    val priceAmountMicro: Long,
    val priceCurrencyCode: String,
    val sku: String,
    val subscriptionPeriod: String,
    val title: String,
    val type: String,
    val hashCode: Int,
    val originalJson: String
): Parcelable {
    companion object {
         val empty = SkuDetails("","","","",0,0,"","",0,"",0,"","","","","",0,"")
    }
}
