package io.glassfy.androidsdk.internal.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.glassfy.androidsdk.internal.network.model.utils.DTOException
import io.glassfy.androidsdk.model.Sku

@JsonClass(generateAdapter = true)
internal data class SkuDto(
    @field:Json(name = "extravars")
    val extravars: Map<String, String>?,
    @field:Json(name = "identifier")
    val identifier: String?,
    @field:Json(name = "productid")
    val productId: String?,
    @field:Json(name = "promotionalid")
    val promotionalId: String?,
    @field:Json(name = "store")
    val store: String?
) {
    @Throws(DTOException::class)
    internal fun toSku(): Sku {
        if (identifier.isNullOrEmpty() || productId.isNullOrEmpty()) {
            throw DTOException("Missing sku identifier/productId")
        }
        return Sku(identifier, productId, extravars ?: mapOf())
    }
}
