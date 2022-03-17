package io.glassfy.androidsdk.internal.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class ErrorDto(
    @field:Json(name = "code")
    val code: Int,
    @field:Json(name = "description")
    val description: String
)
