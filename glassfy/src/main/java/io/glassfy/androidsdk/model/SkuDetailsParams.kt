package io.glassfy.androidsdk.model

data class SkuDetailsParams(
    val productId: String,
    val basePlanId: String?,
    val offerId: String?,
    var productType: ProductType,
)
