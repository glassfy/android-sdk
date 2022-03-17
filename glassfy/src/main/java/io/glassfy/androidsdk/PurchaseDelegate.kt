package io.glassfy.androidsdk

import io.glassfy.androidsdk.model.Purchase

interface PurchaseDelegate {
    fun onProductPurchase(p: Purchase)
}
