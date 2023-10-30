package io.glassfy.androidsdk.internal.billing

import io.glassfy.androidsdk.model.ProductType
import io.glassfy.androidsdk.model.Sku

internal data class SkuDetailsQuery(
    val productId: String,
    val basePlanId: String?,
    val offerId: String?,
    val productType: ProductType
) {
    companion object {
        fun fromSkus(skus: List<Sku>): List<SkuDetailsQuery> = skus.flatMap { fromSku(it) }.distinct()

        fun fromSku(sku: Sku): List<SkuDetailsQuery> = listOfNotNull(sku.skuParams.run {
            SkuDetailsQuery(
                productId = productId,
                basePlanId = basePlanId,
                offerId = offerId,
                productType = productType
            )
        }, sku.fallbackSkuParams?.run {
            SkuDetailsQuery(
                productId = productId,
                basePlanId = basePlanId,
                offerId = offerId,
                productType = productType
            )
        }).distinct()
    }
}