package io.glassfy.androidsdk.internal.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.glassfy.androidsdk.internal.network.model.utils.DTOException
import io.glassfy.androidsdk.model.Offering
import io.glassfy.androidsdk.model.Sku

@JsonClass(generateAdapter = true)
internal data class OfferingDto(
    @field:Json(name = "appid")
    val appid: String?,
    @field:Json(name = "identifier")
    val identifier: String?,
    @field:Json(name = "skus")
    val skus: List<SkuDto>?
) {
    @Throws(DTOException::class)
    internal fun toOffering(): Offering {
        if (identifier == null) {
            throw DTOException("Missing offering identifier")
        }

        val skuList = skus?.mapNotNull {
            it.toSku(identifier) as? Sku
        }.orEmpty()
        return Offering(identifier, skuList)
    }
}
