package io.glassfy.androidsdk.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Sku(
    override val skuId: String,
    override val productId: String,
    val extravars: Map<String, String>,
    internal val offeringId: String?,
    var product: SkuDetails = SkuDetails.empty
) : Parcelable, ISkuBase {
    override val store: Store
        get() = Store.PlayStore
}
