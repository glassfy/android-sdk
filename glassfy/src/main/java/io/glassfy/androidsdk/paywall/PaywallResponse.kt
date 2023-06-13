package io.glassfy.androidsdk.paywall

import android.os.Build
import androidx.annotation.RequiresApi
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.glassfy.androidsdk.internal.network.model.ErrorDto
import io.glassfy.androidsdk.internal.network.model.SkuDto
import io.glassfy.androidsdk.internal.network.model.utils.DTOException
import io.glassfy.androidsdk.model.Sku

@RequiresApi(Build.VERSION_CODES.N)
@JsonClass(generateAdapter = true)
internal data class PaywallResponse(
    @field:Json(name = "status")
    val status: Int?,

    @field:Json(name = "paywall")
    val paywall: PaywallData?,

    @field:Json(name = "skus")
    val skus: List<SkuDto>?,

    @field:Json(name = "error")
    val error: ErrorDto?,
) {
    @Throws(DTOException::class)
    internal fun toPaywall(): Paywall {
        if (paywall == null) {
            throw DTOException("Unexpected paywall data format: missing paywall data")
        }
        if (paywall.contentUrl.isNullOrEmpty()) {
            throw DTOException("Unexpected paywall data format: missing content or url")
        }
        if (paywall.pwid.isNullOrEmpty()) {
            throw DTOException("Unexpected paywall data format: missing pwid")
        }
        if (paywall.locale.isNullOrEmpty()) {
            throw DTOException("Unexpected paywall data format: missing locale")
        }

        val skuList = skus?.mapNotNull {
            val sku = it.toSku(paywall.pwid)
            if (sku is Sku) sku else null
        } ?: emptyList()

        return Paywall(
            paywall.contentUrl,
            paywall.pwid,
            paywall.locale,
            paywall.type ?: PaywallType.NoCode,
            skuList
        )
    }
}

@JsonClass(generateAdapter = true)
internal data class PaywallData(
    @field:Json(name = "status")
    val status: Int?,

    @field:Json(name = "url")
    val contentUrl: String?,

    @field:Json(name = "locale")
    val locale: String?,

    @field:Json(name = "pwid")
    val pwid: String?,

    @field:Json(name = "version")
    val version: Int?,

    @field:Json(name = "type")
    val type: PaywallType?,

    @field:Json(name = "skus")
    val skus: List<SkuDto>?,

    @field:Json(name = "error")
    val error: ErrorDto?,
)