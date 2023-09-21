package io.glassfy.androidsdk.internal.billing

import io.glassfy.androidsdk.model.Purchase

internal interface IBillingPurchaseDelegate {
    suspend fun onProductPurchase(p: Purchase, isSubscription: Boolean)
}