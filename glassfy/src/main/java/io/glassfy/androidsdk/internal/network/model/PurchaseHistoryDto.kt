package io.glassfy.androidsdk.internal.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.glassfy.androidsdk.internal.network.model.utils.DTOException
import io.glassfy.androidsdk.model.EventType
import io.glassfy.androidsdk.model.PurchaseHistory
import io.glassfy.androidsdk.model.Store
import java.util.*

@JsonClass(generateAdapter = true)
internal data class PurchaseHistoryDto(
    @field:Json(name = "productid")
    val productid: String?,
    @field:Json(name = "skuid")
    val skuid: String?,
    @field:Json(name = "type")
    val type: EventType?,
    @field:Json(name = "store")
    val store: Store?,
    @field:Json(name = "date_ms")
    val date_ms: Long?,
    @field:Json(name = "expire_date_ms")
    val expire_date_ms: Long?,
    @field:Json(name = "transaction_id")
    val transaction_id: String?,
    @field:Json(name = "subscriberid")
    val subscriberid: String?,
    @field:Json(name = "currency_code")
    val currency_code: String?,
    @field:Json(name = "country_code")
    val country_code: String?,
    @field:Json(name = "is_in_intro_offer_period")
    val is_in_intro_offer_period: Boolean?,
    @field:Json(name = "promotional_offer_id")
    val promotional_offer_id: String?,
    @field:Json(name = "offer_code_ref_name")
    val offer_code_ref_name: String?,
    @field:Json(name = "licensecode")
    val licensecode: String?,
    @field:Json(name = "web_order_line_item_id")
    val web_order_line_item_id: String?,
) {
    @Throws(DTOException::class)
    internal fun toPurchaseHistory(): PurchaseHistory {
        if (productid.isNullOrEmpty() || type == null || store == null) {
            throw DTOException("Unexpected PurchaseHistory data format: missing productId, type or store")
        }
        return PurchaseHistory(
            productid,
            skuid,
            type,
            store,
            date_ms?.let { Date(it) },
            expire_date_ms?.let { Date(it) },
            transaction_id,
            subscriberid,
            currency_code,
            country_code,
            is_in_intro_offer_period ?: false,
            promotional_offer_id,
            offer_code_ref_name,
            licensecode,
            web_order_line_item_id
        )
    }

}
