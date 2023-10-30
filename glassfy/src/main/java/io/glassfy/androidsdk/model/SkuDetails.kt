package io.glassfy.androidsdk.model

data class SkuDetails(
    val description: String,
    val freeTrialPeriod: String,
    val iconUrl: String,
    val sku: String,
    val subscriptionPeriod: String,
    val title: String,
    val type: ProductType,
    var basePlanId: String,
    var offerId: String,
    val offerToken: String,
    val hashCode: Int,

    /**
     * This is a special discounted price, not accounting for any free-trial period.
     */
    val introductoryPrice: String,
    val introductoryPriceAmountMicro: Long,
    val introductoryPriceAmountCycles: Int,
    val introductoryPriceAmountPeriod: String,

    /**
     * Represents the full, undiscounted price of the product or subscription.
     */
    val originalPrice: String,
    val originalPriceAmountMicro: Long,

    /**
     * Represents the current price of the product or subscription, which could be the full price or a discounted price.
     * This price will be applied to subscription renewal after both the free-trial and introductory pricing period have ended.
     */
    val price: String,
    val priceAmountMicro: Long,
    val priceCurrencyCode: String,

    /**
     * Required for compatibility with LegacyBillingService
     */
    internal var originalJson: String = ""
)
