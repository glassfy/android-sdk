package io.glassfy.androidsdk.internal.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.glassfy.androidsdk.model.Store

@JsonClass(generateAdapter = true)
internal data class StoreInfoDto(
    @field:Json(name = "store")
    val store: Store
)
