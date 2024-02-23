package io.glassfy.androidsdk.model

import java.net.MalformedURLException
import java.net.URL

abstract class StoreInfo(open val store: Store, open val rawData: Map<String, Any>)

data class StoreInfoUnknown(
    override val store: Store,
    override val rawData: Map<String, Any>
) : StoreInfo(store, rawData)

data class StoreInfoPaddle(
    override val rawData: Map<String, Any>
) : StoreInfo(Store.Paddle, rawData) {
    val userId: String? get() = rawData["userid"] as? String
    val planId: String? get() = rawData["planid"] as? String
    val subscriptionId: String? get() = rawData["subscriptionid"] as? String

    val updateURL: URL?
        get() = (rawData["updateurl"] as? String)?.let {
            return try {
                URL(it)
            } catch (_: MalformedURLException) {
                null
            }
        }

    val cancelURL: URL?
        get() = (rawData["cancelurl"] as? String)?.let {
            return try {
                URL(it)
            } catch (_: MalformedURLException) {
                null
            }
        }
}

data class StoreInfoStripe(
    override val rawData: Map<String, Any>
) : StoreInfo(Store.Stripe, rawData) {
    val customerId: String? get() = rawData["customerid"] as? String
    val subscriptionId: String? get() = rawData["subscriptionid"] as? String
    val producId: String? get() = rawData["productid"] as? String
}
