package io.glassfy.androidsdk.internal.network.model.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.glassfy.androidsdk.internal.network.model.ErrorDto
import io.glassfy.androidsdk.internal.network.model.utils.DTOException
import io.glassfy.androidsdk.model.StoreInfo
import io.glassfy.androidsdk.model.StoresInfo

@JsonClass(generateAdapter = true)
internal data class StoresInfoResponse(
    @field:Json(name = "info")
    val info: List<StoreInfo>?,
    @field:Json(name = "status")
    val status: Int,
    @field:Json(name = "error")
    val error: ErrorDto?
) {
    @Throws(DTOException::class)
    internal fun toStoresInfo() = StoresInfo((info ?: emptyList()))
}
