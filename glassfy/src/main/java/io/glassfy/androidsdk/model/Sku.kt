package io.glassfy.androidsdk.model


data class Sku(
    val skuId: String,
    val productId: String,
    val extravars: Map<String, String>
) {
    lateinit var product: SkuDetails
}
