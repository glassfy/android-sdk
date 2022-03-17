package io.glassfy.androidsdk.internal.network.model.response


import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.glassfy.androidsdk.internal.network.model.ErrorDto
import io.glassfy.androidsdk.internal.network.model.PermissionDto
import io.glassfy.androidsdk.model.Permissions
import io.glassfy.androidsdk.model.Transaction

@JsonClass(generateAdapter = true)
internal data class TransactionResponse(
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
    internal fun toPermissions() = Transaction(
        Permissions(
            originalApplicationVersion ?: "",
            originalPurchaseDate ?: "",
            subscriberId ?: "",
            (permissions ?: emptyList()).map { it.toPermission() })
    )
}

