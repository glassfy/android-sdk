package io.glassfy.androidsdk.internal.network.model.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.glassfy.androidsdk.model.HistoryPurchase

@JsonClass(generateAdapter = true)
internal data class InitializeRequest(
    @field:Json(name = "packagename")
    val applicationId: String?,
    @field:Json(name = "tokens")
    val tokens: List<TokenRequest>?,
) {
    companion object {
        internal fun from(
            applicationId: String,
            subs: List<HistoryPurchase>,
            inapp: List<HistoryPurchase>
        ) =
            InitializeRequest(applicationId,
                subs.map { TokenRequest.from(it, true) } + inapp.map {
                    TokenRequest.from(
                        it,
                        false
                    )
                })
    }
}