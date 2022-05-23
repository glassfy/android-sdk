package io.glassfy.androidsdk.internal.network.model.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.glassfy.androidsdk.internal.network.model.ErrorDto
import io.glassfy.androidsdk.internal.network.model.UserPropertiesDto
import io.glassfy.androidsdk.model.UserProperties

@JsonClass(generateAdapter = true)
internal data class UserPropertiesResponse(
    @field:Json(name = "property")
    val property: UserPropertiesDto?,
    @field:Json(name = "status")
    val status: Int,
    @field:Json(name = "error")
    val error: ErrorDto?
) {
    internal fun toUserProperties() =
        property?.toUserProperties() ?: UserProperties(null, null, null)
}
