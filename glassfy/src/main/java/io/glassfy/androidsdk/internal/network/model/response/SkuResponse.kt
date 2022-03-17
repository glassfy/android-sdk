package io.glassfy.androidsdk.internal.network.model.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.glassfy.androidsdk.internal.network.model.ErrorDto
import io.glassfy.androidsdk.internal.network.model.SkuDto

@JsonClass(generateAdapter = true)
internal data class SkuResponse(
    @field:Json(name = "sku")
    val sku: SkuDto?,
    @field:Json(name = "status")
    val status: Int,
    @field:Json(name = "error")
    val error: ErrorDto?
)
