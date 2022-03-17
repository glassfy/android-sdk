package io.glassfy.androidsdk.internal.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.glassfy.androidsdk.internal.network.model.utils.DTOException
import io.glassfy.androidsdk.model.Offering

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
    internal fun toOffering(): Offering =
        identifier?.let { Offering(identifier, (skus ?: emptyList()).map { it.toSku() }) }
            ?: throw DTOException("Missing offering identifier")
}
