package io.glassfy.paywall

import android.content.Context
import android.os.Parcelable
import io.glassfy.androidsdk.paywall.Paywall
import io.glassfy.androidsdk.paywall.PaywallType
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class ParcelablePaywall(
    val contentUrl: String,
    val pwid: String,
    val locale: String,
    val type: PaywallType,
    val skus: List<String>,
    val config: String,
    val preloadedContent: String?
): Parcelable {
    companion object {
        fun fromPaywall(context: Context, paywall: Paywall): ParcelablePaywall {
            return ParcelablePaywall(
                paywall.contentUrl,
                paywall.pwid,
                paywall.locale,
                paywall.type,
                paywall.skus.map { it.skuId },
                paywall.config(context).toString(),
                paywall.preloadedContent
            )
        }
    }
}