package io.glassfy.androidsdk.internal.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.glassfy.androidsdk.internal.network.model.utils.DTOException
import io.glassfy.androidsdk.model.ProductType
import io.glassfy.androidsdk.model.SkuDetailsParams

@JsonClass(generateAdapter = true)
data class SkuDetailsParamsDto(
    @field:Json(name = "productid")
    val productId: String?,
    @field:Json(name = "baseplan")
    val basePlanId: String?,
    @field:Json(name = "offerid")
    val offerId: String?,
) {
    @Throws(DTOException::class)
    internal fun toSkuDetailsParams(type: ProductType): SkuDetailsParams? {
        if (productId == null || basePlanId == null) {
            return null
        }

        return SkuDetailsParams(
            productId = productId,
            basePlanId = basePlanId,
            offerId = offerId,
            type
        )
    }
}
