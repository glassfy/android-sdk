package io.glassfy.androidsdk.model

data class AccountableSku(
    override val skuId: String,
    override val productId: String,
    val basePlanId: String?,
    val offerId: String?,
    val isInIntroOfferPeriod: Boolean,
    val isInTrialPeriod: Boolean,
    override val store: Store
) : ISkuBase