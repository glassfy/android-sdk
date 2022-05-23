package io.glassfy.androidsdk.model

sealed interface ISkuBase {
    val skuId: String
    val productId: String
    val store: Store
}