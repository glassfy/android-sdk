package io.glassfy.androidsdk.internal.billing.play.billing.mapper


import com.android.billingclient.api.PurchaseHistoryRecord
import io.glassfy.androidsdk.model.HistoryPurchase

internal fun convertHistoryPurchases(ps: List<PurchaseHistoryRecord>) =
    ps.map { convertHistoryPurchase(it) }

private fun convertHistoryPurchase(p: PurchaseHistoryRecord): HistoryPurchase =
    p.run {
        HistoryPurchase(
            developerPayload,
            purchaseTime,
            purchaseToken,
            quantity,
            signature,
            products,
            hashCode(),
            originalJson
        )
    }
