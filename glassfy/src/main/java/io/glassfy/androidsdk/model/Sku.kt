package io.glassfy.androidsdk.model

data class Sku(
    override val skuId: String,
    val extravars: Map<String, String>,
    internal val offeringId: String?,
    internal val skuParams: SkuDetailsParams,
    internal val fallbackSkuParams: SkuDetailsParams?,
) : ISkuBase {

    override val store: Store
        get() = Store.PlayStore

    override val productId: String
        get() = product.sku

    val basePlanId: String?
        get() = product.basePlanId.ifEmpty { null }

    val offerId: String?
        get() = product.offerId.ifEmpty { null }

    val productType: ProductType
        get() = product.type

    lateinit var product: SkuDetails

    fun isSubscription(): Boolean {
        return product.type == ProductType.SUBS
    }
}
