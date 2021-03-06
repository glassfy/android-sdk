package io.glassfy.androidsdk.internal.network.model.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.glassfy.androidsdk.internal.network.model.ErrorDto
import io.glassfy.androidsdk.internal.network.model.PermissionDto
import io.glassfy.androidsdk.internal.network.model.utils.DTOException
import io.glassfy.androidsdk.model.Permissions

@JsonClass(generateAdapter = true)
internal data class PermissionsResponse (
    @field:Json(name = "permissions")
    val permissions: List<PermissionDto>?,
    @field:Json(name = "original_application_version")
    val originalApplicationVersion: String?,
    @field:Json(name = "original_purchase_date")
    val originalPurchaseDate: String?,
    @field:Json(name = "subscriberid")
    val subscriberId: String?,
    @field:Json(name = "status")
    val status: Int,
    @field:Json(name = "error")
    val error: ErrorDto?
) {
    @Throws(DTOException::class)
    internal fun toPermissions() = Permissions(
        originalApplicationVersion ?: "",
        originalPurchaseDate ?: "",
        subscriberId ?: "",
        (permissions ?: emptyList()).map { it.toPermission() },
        ""
    )
}
