package io.glassfy.androidsdk.model

data class SkuPaddle(
    override val skuId: String,
    override val productId: String,
    val extravars: Map<String, String>,

    val name: String,

    val initialPrice: Number?,
    val initialPriceCode: String?,         // three-letter ISO currency code

    val recurringPrice: Number?,
    val recurringPriceCode: String?,      // three-letter ISO currency code
) : ISkuBase {
    override val store: Store
        get() = Store.Paddle
}
