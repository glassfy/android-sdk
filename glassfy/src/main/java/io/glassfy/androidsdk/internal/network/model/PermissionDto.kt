package io.glassfy.androidsdk.internal.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.glassfy.androidsdk.internal.network.model.utils.DTOException
import io.glassfy.androidsdk.model.Entitlement
import io.glassfy.androidsdk.model.Permission

@JsonClass(generateAdapter = true)
internal data class PermissionDto(
    @field:Json(name = "identifier")
    val identifier: String?,
    @field:Json(name = "entitlement")
    val entitlement: Entitlement?,
    @field:Json(name = "expires_date")
    val expiresDate: Long?,
    @field:Json(name = "skuarray")
    val skuarray: List<SkuBaseDto>?
) {
    @Throws(DTOException::class)
    internal fun toPermission(): Permission {
        if (identifier.isNullOrEmpty() || expiresDate == null) {
            throw DTOException("Missing permission identifier or expiresDate")
        }

        return Permission(
            identifier,
            entitlement ?: Entitlement.NeverBuy,
            expiresDate,
            skuarray?.map { it.toSkuBase() } ?: emptyList()
        )
    }

}
