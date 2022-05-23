package io.glassfy.androidsdk.model

data class SkuBase(
    override val skuId: String,
    override val productId: String,
    override val store: Store
) : ISkuBase
