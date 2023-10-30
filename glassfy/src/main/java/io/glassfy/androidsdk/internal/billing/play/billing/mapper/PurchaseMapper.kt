package io.glassfy.androidsdk.internal.billing.play.billing.mapper

import io.glassfy.androidsdk.model.Purchase

internal fun convertPurchases(ps: List<com.android.billingclient.api.Purchase>) =
    ps.map { convertPurchase(it) }

internal fun convertPurchase(p: com.android.billingclient.api.Purchase) = p.run {
    Purchase(
        convertAccountIdentifier(accountIdentifiers),
        developerPayload,
        orderId.orEmpty(), // orderId is null with purchaseState != PURCHASED
        packageName,
        purchaseState,
        purchaseTime,
        purchaseToken,
        quantity,
        signature,
        products,
        hashCode(),
        isAcknowledged,
        isAutoRenewing,
        originalJson
    )
}
