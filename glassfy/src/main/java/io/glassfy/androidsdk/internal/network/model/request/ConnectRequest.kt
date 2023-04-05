package io.glassfy.androidsdk.internal.network.model.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.glassfy.androidsdk.model.Store

@JsonClass(generateAdapter = true)
internal data class ConnectRequest(
    @field:Json(name = "customid")
    val customId: String? = null,
    @field:Json(name = "store")
    val store: Int? = null,
    @field:Json(name = "licensekey")
    val licenseKey: String? = null,
    @field:Json(name = "force")
    val force: Boolean? = null
) {
    companion object {
        internal fun customSubscriber(customId: String?) =
            ConnectRequest(customId)

        internal fun paddleLicense(licenseKey: String, force: Boolean) =
            ConnectRequest(store = Store.Paddle.value, licenseKey = licenseKey, force = force)

        internal fun universalCode(universalCode: String, force: Boolean) =
            ConnectRequest(store = Store.Glassfy.value, licenseKey = universalCode, force = force)
    }
}
