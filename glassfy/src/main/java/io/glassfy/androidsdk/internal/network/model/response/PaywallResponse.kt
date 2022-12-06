package io.glassfy.androidsdk.internal.network.model.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.glassfy.androidsdk.internal.model.PaywallImpl
import io.glassfy.androidsdk.internal.network.model.ErrorDto
import io.glassfy.androidsdk.internal.network.model.PaywallDto
import io.glassfy.androidsdk.internal.network.model.SkuDto
import io.glassfy.androidsdk.internal.network.model.utils.DTOException
import io.glassfy.androidsdk.model.Sku

@JsonClass(generateAdapter = true)
internal data class PaywallResponse(
    @field:Json(name = "paywall")
    val paywall: PaywallDto?,
    @field:Json(name = "skus")
    val skus: List<SkuDto>?,
    @field:Json(name = "status")
    val status: Int,
    @field:Json(name = "error")
    val error: ErrorDto?
) {
    @Throws(DTOException::class)
    internal fun toPaywall(): PaywallImpl =
        paywall?.let { dto ->
            if (dto.contentUrl.isNullOrEmpty()) {
                throw DTOException("Unexpected paywall data format: missing content or url")
            }
            if (dto.pwid.isNullOrEmpty()) {
                throw DTOException("Unexpected paywall data format: missing pwid")
            }
            if (dto.locale.isNullOrEmpty()) {
                throw DTOException("Unexpected paywall data format: missing locale")
            }
            if (dto.type == null) {
                throw DTOException("Unexpected paywall data format: missing type")
            }

            val skuList = skus?.mapNotNull {
                val sku = it.toSku(dto.pwid)
                if (sku is Sku) sku else null
            } ?: emptyList()
            return PaywallImpl(dto.contentUrl,
                dto.pwid, dto.locale, dto.type, dto.version ?: "", skuList)
        } ?: throw DTOException("Unexpected data format")
}