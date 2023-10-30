package io.glassfy.androidsdk.internal.billing.play.billing

import com.android.billingclient.api.ProductDetails.PricingPhase
import com.android.billingclient.api.ProductDetails.SubscriptionOfferDetails

internal fun findBasePlan(offers: List<SubscriptionOfferDetails>): SubscriptionOfferDetails? =
    offers.firstOrNull { it.pricingPhases.pricingPhaseList.size == 1 }

internal fun findOffer(offerId: String, offers: List<SubscriptionOfferDetails>): SubscriptionOfferDetails? =
    offers.firstOrNull { o -> o.offerId == offerId }
