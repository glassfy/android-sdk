package io.glassfy.androidsdk.internal.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.glassfy.androidsdk.internal.model.PaywallType

@JsonClass(generateAdapter = true)
data class PaywallDto(
    @field:Json(name = "version")
    val version: String?,
    @field:Json(name = "type")
    val type: PaywallType?,
    @field:Json(name = "url")
    val contentUrl: String?,
    @field:Json(name = "locale")
    val locale: String?,
    @field:Json(name = "pwid")
    val pwid: String?
)
