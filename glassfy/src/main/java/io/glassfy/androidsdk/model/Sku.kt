package io.glassfy.androidsdk.model

data class Sku(
    override val skuId: String,
    override val productId: String,
    val extravars: Map<String, String>,
    internal val offeringId: String?
) : ISkuBase {
    override val store: Store
        get() = Store.PlayStore

    lateinit var product: SkuDetails
}
