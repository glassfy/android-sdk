package io.glassfy.androidsdk.internal.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.glassfy.androidsdk.internal.network.model.utils.DTOException
import io.glassfy.androidsdk.model.SkuBase
import io.glassfy.androidsdk.model.Store

@JsonClass(generateAdapter = true)
data class SkuBaseDto(
    @field:Json(name = "identifier")
    val identifier: String?,
    @field:Json(name = "productid")
    val productId: String?,
    @field:Json(name = "store")
    val store: Store?
) {
    @Throws(DTOException::class)
    internal fun toSkuBase(): SkuBase {
        return if (identifier == null || productId == null || store == null) {
            throw DTOException("Unexpected SkuBase data format: missing identifier/productId")
        } else {
            SkuBase(identifier, productId, store)
        }
    }
}
