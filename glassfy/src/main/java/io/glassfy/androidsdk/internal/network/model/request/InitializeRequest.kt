package io.glassfy.androidsdk.internal.network.model.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.glassfy.androidsdk.model.HistoryPurchase
import io.glassfy.androidsdk.model.Purchase

@JsonClass(generateAdapter = true)
internal data class InitializeRequest(
    @field:Json(name = "packagename")
    val applicationId: String?,
    @field:Json(name = "tokens")
    val tokens: List<TokenRequest>?,
    @field:Json(name = "install_time")
    val installTime: Long?,
    @field:Json(name = "cross_platform_sdk_framework")
    val crossPlatformSdkFramework: String?,
    @field:Json(name = "cross_platform_sdk_version")
    val crossPlatformSdkVersion: String?,
) {
    companion object {
        internal fun from(
            applicationId: String,
            hSubs: List<HistoryPurchase>,
            hInapp: List<HistoryPurchase>,
            subs: List<Purchase>,
            inapp: List<Purchase>,
            installTime: Long?,
            crossPlatformSdkFramework: String?,
            crossPlatformSdkVersion: String?,
        ) =
            InitializeRequest(
                applicationId,
                hSubs.map { TokenRequest.from(it, true) } +
                        hInapp.map { TokenRequest.from(it, false) } +
                        subs.map { TokenRequest.from(it, true) } +
                        inapp.map { TokenRequest.from(it, false) } ,
                installTime,
                crossPlatformSdkFramework,
                crossPlatformSdkVersion,
            )
    }
}