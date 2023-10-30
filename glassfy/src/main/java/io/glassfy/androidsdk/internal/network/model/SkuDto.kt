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

    @field:Json(name = "baseplan")
    val basePlanId: String?,

    @field:Json(name = "offerid")
    val offerId: String?,

    @field:Json(name = "type")
    val productType: ProductType?,

    @field:Json(name = "fallbacksku")
    val fallbackSku: SkuDetailsParamsDto?,
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
                if (identifier.isBlank() || productId.isBlank()) {
                    throw DTOException("Missing sku identifier/productId")
                }
                Sku(
                    identifier,
                    extravars ?: emptyMap(),
                    offeringId,
                    SkuDetailsParams(
                        productId,
                        basePlanId?.ifBlank { null },
                        offerId?.ifBlank { null },
                        productType ?: ProductType.UNKNOWN
                    ),
                    fallbackSku?.toSkuDetailsParams(productType ?: ProductType.UNKNOWN)
                )
            }
            else -> SkuBase(identifier, productId, store)
        }
    }
}