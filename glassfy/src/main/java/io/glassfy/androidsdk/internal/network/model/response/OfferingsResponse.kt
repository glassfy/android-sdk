package io.glassfy.androidsdk.internal.network.model.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.glassfy.androidsdk.internal.network.model.ErrorDto
import io.glassfy.androidsdk.internal.network.model.OfferingDto
import io.glassfy.androidsdk.internal.network.model.utils.DTOException
import io.glassfy.androidsdk.model.Offerings

@JsonClass(generateAdapter = true)
internal data class OfferingsResponse(
    @field:Json(name = "offerings")
    val offerings: List<OfferingDto>?,
    @field:Json(name = "status")
    val status: Int,
    @field:Json(name = "error")
    val error: ErrorDto?
) {
    @Throws(DTOException::class)
    internal fun toOfferings():Offerings = Offerings((offerings ?: emptyList()).map { o -> o.toOffering() })
}
