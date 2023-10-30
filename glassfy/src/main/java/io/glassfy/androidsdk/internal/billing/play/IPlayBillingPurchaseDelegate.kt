package io.glassfy.androidsdk.internal.billing.play

import com.android.billingclient.api.Purchase

internal interface IPlayBillingPurchaseDelegate {
    suspend fun onPlayBillingPurchasePurchase(p: Purchase, productType: String)
}