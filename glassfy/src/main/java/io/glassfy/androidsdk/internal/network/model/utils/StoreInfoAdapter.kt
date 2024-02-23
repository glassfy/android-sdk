package io.glassfy.androidsdk.internal.network.model.utils

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.ToJson
import io.glassfy.androidsdk.model.Store
import io.glassfy.androidsdk.model.StoreInfo
import io.glassfy.androidsdk.model.StoreInfoPaddle
import io.glassfy.androidsdk.model.StoreInfoStripe
import io.glassfy.androidsdk.model.StoreInfoUnknown

internal class StoreInfoAdapter {
    @ToJson
    private fun toJson(store: StoreInfo): Map<String, Any> {
        return store.rawData
    }

    @FromJson
    fun fromJson(jsonReader: JsonReader): StoreInfo {
        var rawValue: Map<String, Any> = emptyMap()
        val readJsonValue = jsonReader.readJsonValue()
        if (readJsonValue is Map<*, *>) {
            @Suppress("UNCHECKED_CAST")
            rawValue = readJsonValue as Map<String, Any>
        }

        val storeValue = when (val rawValueStore = rawValue["store"]) {
            is String -> rawValueStore.toIntOrNull() ?: -1
            is Int -> rawValueStore
            else -> -1
        }
        return when (val store = Store.fromValue(storeValue)) {
            Store.Paddle -> StoreInfoPaddle(rawValue)
            Store.Stripe -> StoreInfoStripe(rawValue)
            else -> StoreInfoUnknown(store, rawValue)
        }
    }
}