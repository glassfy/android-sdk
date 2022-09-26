package io.glassfy.androidsdk.internal.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.glassfy.androidsdk.internal.network.model.utils.DTOException
import io.glassfy.androidsdk.model.AccountableSku
import io.glassfy.androidsdk.model.Store

@JsonClass(generateAdapter = true)
data class AccountableSkuDto(
    @field:Json(name = "identifier")
    val identifier: String?,
    @field:Json(name = "productid")
    val productId: String?,
    @field:Json(name = "isinintrooffer")
    val isInIntroOfferPeriod: Boolean?,
    @field:Json(name = "istrial")
    val isInTrialPeriod: Boolean?,
    @field:Json(name = "store")
    val store: Store?
) {
    @Throws(DTOException::class)
    internal fun toAccountableSku(): AccountableSku {
        return if (identifier == null || productId == null || store == null) {
            throw DTOException("Unexpected AccountableSku data format: missing identifier/productId")
        } else {
            AccountableSku(identifier,
                productId,
                isInIntroOfferPeriod ?: false,
                isInTrialPeriod ?: false,
                store)
        }
    }
}
