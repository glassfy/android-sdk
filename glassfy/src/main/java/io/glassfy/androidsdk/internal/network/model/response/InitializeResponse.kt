package io.glassfy.androidsdk.internal.network.model.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.glassfy.androidsdk.internal.network.model.ErrorDto
import io.glassfy.androidsdk.internal.network.model.utils.DTOException

@JsonClass(generateAdapter = true)
internal data class InitializeResponse(
    @field:Json(name = "subscriberid")
    val subscriberId: String?,
    @field:Json(name = "status")
    val status: Int,
    @field:Json(name = "error")
    val error: ErrorDto?
) {
    @Throws(DTOException::class)
    internal fun toServerInfo(): ServerInfo = subscriberId?.let { ServerInfo(it) }
        ?: throw DTOException("Missing subscriberId ServerInfo")
}

internal data class ServerInfo(val subscriberId: String)
