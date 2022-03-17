package io.glassfy.androidsdk.internal.billing.google

import com.android.billingclient.api.Purchase

internal interface PlayBillingPurchaseDelegate {
    fun onPlayBillingPurchasePurchase(p: Purchase)
}