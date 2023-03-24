package io.glassfy.androidsdk.internal.network.model.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.glassfy.androidsdk.internal.network.model.ErrorDto
import io.glassfy.androidsdk.internal.network.model.PurchaseHistoryDto
import io.glassfy.androidsdk.internal.network.model.utils.DTOException
import io.glassfy.androidsdk.model.PurchasesHistory

@JsonClass(generateAdapter = true)
internal data class PurchaseHistoryResponse(
    @field:Json(name = "subscriberid")
    val subscriberId: String?,
    @field:Json(name = "customid")
    val customId: String?,
    @field:Json(name = "purchases")
    val purchases: List<PurchaseHistoryDto>?,
    @field:Json(name = "status")
    val status: Int,
    @field:Json(name = "error")
    val error: ErrorDto?,
) {
    @Throws(DTOException::class)
    internal fun toPurchasesHistory(): PurchasesHistory {
        return if (subscriberId.isNullOrEmpty()) {
            throw DTOException("Unexpected APIPurchaseHistory data format: missing subscriberId")
        } else {
            PurchasesHistory(
                purchases.orEmpty().map { it.toPurchaseHistory() },
                subscriberId,
                customId
            )
        }
    }
}
