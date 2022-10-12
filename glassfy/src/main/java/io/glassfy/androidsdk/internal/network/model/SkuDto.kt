package io.glassfy.androidsdk.internal.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.glassfy.androidsdk.internal.network.model.utils.DTOException
import io.glassfy.androidsdk.model.*

@JsonClass(generateAdapter = true)
internal data class SkuDto(
    @field:Json(name = "identifier")
    val identifier: String?,
    @field:Json(name = "productid")
    val productId: String?,
    @field:Json(name = "store")
    val store: Store?,
    @field:Json(name = "extravars")
    val extravars: Map<String, String>?,
    @field:Json(name = "name")
    val name: String?,
    @field:Json(name = "recurringprice")
    val initialprice: PaddlePriceDto?,
    @field:Json(name = "initialprice")
    val recurringprice: PaddlePriceDto?,
) {
    @Throws(DTOException::class)
    internal fun toSku(offeringId: String? = null): ISkuBase {
        if (identifier == null || productId == null || store == null) {
            throw DTOException("Unexpected Sku data format: missing identifier/productId")
        }

        return when (store) {
            Store.Paddle -> {
                if (name == null) {
                    throw DTOException("Unexpected PaddleSku data format: missing name")
                }
                SkuPaddle(
                    identifier,
                    productId,
                    extravars ?: emptyMap(),
                    name,
                    initialprice?.price,
                    initialprice?.locale,
                    recurringprice?.price,
                    recurringprice?.locale
                )
            }
            Store.PlayStore -> {
                if (identifier.isEmpty() || productId.isEmpty()) {
                    throw DTOException("Missing sku identifier/productId")
                }

                Sku(
                    identifier,
                    productId,
                    extravars ?: emptyMap(),
                    offeringId
                )
            }
            else -> SkuBase(identifier, productId, store)
        }
    }
}
