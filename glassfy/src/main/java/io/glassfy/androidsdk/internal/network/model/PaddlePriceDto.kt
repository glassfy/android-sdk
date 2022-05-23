package io.glassfy.androidsdk.internal.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PaddlePriceDto(
    @field:Json(name = "price")
    val price: Float?,
    @field:Json(name = "locale")
    val locale: String?
)
