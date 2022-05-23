package io.glassfy.androidsdk.internal.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.glassfy.androidsdk.model.UserProperties

@JsonClass(generateAdapter = true)
internal data class UserPropertiesDto(
    @field:Json(name = "info")
    val extra: Map<String, String>?,
    @field:Json(name = "token")
    val token: String?,
    @field:Json(name = "email")
    val email: String?
) {
    internal fun toUserProperties() =
        UserProperties(
            email?.ifEmpty { null },
            token?.ifEmpty { null },
            extra
        )
}
