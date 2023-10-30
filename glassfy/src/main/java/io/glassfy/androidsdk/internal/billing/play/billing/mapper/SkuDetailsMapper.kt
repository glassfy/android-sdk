package io.glassfy.androidsdk.internal.billing.play.billing.mapper

import com.android.billingclient.api.BillingClient
import io.glassfy.androidsdk.internal.billing.SkuDetailsQuery
import io.glassfy.androidsdk.model.ProductType
import io.glassfy.androidsdk.model.SkuDetails


internal fun convertSkuDetails(
    products: List<com.android.billingclient.api.ProductDetails>, queries: List<SkuDetailsQuery>
): List<SkuDetails> {
    return queries.mapNotNull { convertSkuDetails(products, it) }
}

private fun convertSkuDetails(
    products: List<com.android.billingclient.api.ProductDetails>, query: SkuDetailsQuery
): SkuDetails? {
    return products.find {
        it.productId == query.productId
    }?.let {
        if (it.productType == BillingClient.ProductType.INAPP) {
            return convertInAppPurchaseSkuDetails(it)
        } else {
            return convertSubscriptionSkuDetails(it, query)
        }
    }
}

private fun convertSubscriptionSkuDetails(
    product: com.android.billingclient.api.ProductDetails, query: SkuDetailsQuery
): SkuDetails? = query.basePlanId?.let { basePlanId ->
    // offers
    product.subscriptionOfferDetails?.filter {
        it.basePlanId == basePlanId
    }
}?.let { offers ->
    // base plan
    val basePlan = findBasePlan(offers) ?: return null

    // offer
    val offer = query.offerId?.let { findOffer(it, offers) }
    if (offer == null && query.offerId != null) {
        return null
    }

    // SkuDetails
    convertSubscriptionSkuDetails(product, basePlan, offer)
}

private fun convertSubscriptionSkuDetails(
    product: com.android.billingclient.api.ProductDetails,
    basePlan: com.android.billingclient.api.ProductDetails.SubscriptionOfferDetails,
    offer: com.android.billingclient.api.ProductDetails.SubscriptionOfferDetails?
): SkuDetails? {
    val basePricing = basePlan.pricingPhases.pricingPhaseList.firstOrNull() ?: return null
    // filter base plan phase from offer's pricingPhase
    val offerPhases = offer?.pricingPhases?.pricingPhaseList?.filter {
        !(basePricing.billingCycleCount == it.billingCycleCount && basePricing.billingPeriod == it.billingPeriod && basePricing.formattedPrice == it.formattedPrice && basePricing.priceAmountMicros == it.priceAmountMicros && basePricing.priceCurrencyCode == it.priceCurrencyCode && basePricing.recurrenceMode == it.recurrenceMode)
    }

    val freeTrial = offerPhases?.firstOrNull { it.priceAmountMicros == 0L }
    val introPrice = offerPhases?.firstOrNull { it.priceAmountMicros != 0L }

    return SkuDetails(
        description = product.description,
        freeTrialPeriod = freeTrial?.billingPeriod.orEmpty(),
        iconUrl = "",
        sku = product.productId,
        subscriptionPeriod = basePricing.billingPeriod,
        title = product.title,
        type = ProductType.SUBS,
        basePlanId = basePlan.basePlanId,
        offerId = offer?.offerId.orEmpty(),
        offerToken = offer?.offerToken ?: basePlan.offerToken,
        hashCode = product.hashCode(),
        introductoryPrice = introPrice?.formattedPrice.orEmpty(),
        introductoryPriceAmountMicro = introPrice?.priceAmountMicros ?: 0,
        introductoryPriceAmountCycles = introPrice?.billingCycleCount ?: 0,
        introductoryPriceAmountPeriod = introPrice?.billingPeriod.orEmpty(),
        originalPrice = basePricing.formattedPrice,
        originalPriceAmountMicro = basePricing.priceAmountMicros,
        price = basePricing.formattedPrice,
        priceAmountMicro = basePricing.priceAmountMicros,
        priceCurrencyCode = basePricing.priceCurrencyCode
    )
}

private fun findBasePlan(offers: List<com.android.billingclient.api.ProductDetails.SubscriptionOfferDetails>): com.android.billingclient.api.ProductDetails.SubscriptionOfferDetails? =
    offers.firstOrNull { it.pricingPhases.pricingPhaseList.size == 1 }

private fun findOffer(
    offerId: String,
    offers: List<com.android.billingclient.api.ProductDetails.SubscriptionOfferDetails>
): com.android.billingclient.api.ProductDetails.SubscriptionOfferDetails? =
    offers.firstOrNull { o -> o.offerId == offerId }

internal fun convertInAppPurchaseSkuDetails(product: com.android.billingclient.api.ProductDetails): SkuDetails? {
    val offer = product.oneTimePurchaseOfferDetails ?: return null

    return SkuDetails(
        description = product.description,
        freeTrialPeriod = "",
        iconUrl = "",
        sku = product.productId,
        subscriptionPeriod = "",
        title = product.title,
        type = ProductType.INAPP,
        basePlanId = "",
        offerId = "",
        offerToken = "",
        hashCode = product.hashCode(),
        introductoryPrice = "",
        introductoryPriceAmountMicro = 0,
        introductoryPriceAmountCycles = 0,
        introductoryPriceAmountPeriod = "",
        originalPrice = offer.formattedPrice,
        originalPriceAmountMicro = offer.priceAmountMicros,
        price = offer.formattedPrice,
        priceAmountMicro = offer.priceAmountMicros,
        priceCurrencyCode = offer.priceCurrencyCode
    )
}


// LEGACY
internal fun convertLegacySkusDetails(ps: List<com.android.billingclient.api.SkuDetails>): List<SkuDetails> =
    ps.map { convertLegacySkusDetails(it) }

private fun convertLegacySkusDetails(s: com.android.billingclient.api.SkuDetails): SkuDetails =
    s.run {
        SkuDetails(
            description = description,
            freeTrialPeriod = freeTrialPeriod,
            iconUrl = iconUrl,
            sku = sku,
            subscriptionPeriod = subscriptionPeriod,
            title = title,
            type = convertLegacySkuType(type),
            basePlanId = "",
            offerId = "",
            offerToken = "",
            hashCode = hashCode(),
            introductoryPrice = introductoryPrice,
            introductoryPriceAmountMicro = introductoryPriceAmountMicros,
            introductoryPriceAmountCycles = introductoryPriceCycles,
            introductoryPriceAmountPeriod = introductoryPricePeriod,
            originalPrice = originalPrice,
            originalPriceAmountMicro = originalPriceAmountMicros,
            price = price,
            priceAmountMicro = priceAmountMicros,
            priceCurrencyCode = priceCurrencyCode,
            originalJson = originalJson
        )
    }

private fun convertLegacySkuType(type: String) = when (type) {
    BillingClient.SkuType.INAPP -> ProductType.INAPP
    BillingClient.SkuType.SUBS -> ProductType.SUBS
    else -> ProductType.UNKNOWN
}