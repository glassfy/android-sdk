package io.glassfy.androidsdk.model

import java.util.Date

data class PurchaseHistory(
    val productId: String,
    val skuId: String?,

    val type: EventType,
    val store: Store,

    val purchaseDate: Date?,
    val expireDate: Date?,

    val transactionId: String?,
    val subscriberId: String?,
    val currencyCode: String?,
    val countryCode: String?,

    val isInIntroOfferPeriod: Boolean,
    val promotionalOfferId: String?,
    val offerCodeRefName: String?,
    val licenseCode: String?,
    val webOrderLineItemId: String?,
)