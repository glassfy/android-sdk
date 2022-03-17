package io.glassfy.androidsdk.internal.network.model.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.glassfy.androidsdk.internal.network.model.ErrorDto

@JsonClass(generateAdapter = true)
internal data class LastSeenResponse(
    @field:Json(name = "update")
    val update: Boolean?,
    @field:Json(name = "status")
    val status: Int,
    @field:Json(name = "error")
    val error: ErrorDto?
)
