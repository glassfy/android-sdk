package io.glassfy.androidsdk.internal.network.model.utils

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.ToJson
import io.glassfy.androidsdk.internal.network.model.StoreInfoDto
import io.glassfy.androidsdk.model.Store
import io.glassfy.androidsdk.model.StoreInfo
import io.glassfy.androidsdk.model.StoreInfoPaddle
import io.glassfy.androidsdk.model.StoreInfoUnknown

internal class StoreInfoAdapter {
    @ToJson
    private fun toJson(store: StoreInfo): Map<String, Any> {
        return store.rawData
    }

    @FromJson
    fun fromJson(jsonReader: JsonReader, storeInfoAdapter: JsonAdapter<StoreInfoDto>): StoreInfo {
        val store = storeInfoAdapter.fromJson(jsonReader)?.store ?: Store.Unknown

        var rawValue: Map<String, Any> = emptyMap()
        val readJsonValue = jsonReader.readJsonValue()
        if (readJsonValue is Map<*, *>) {
            @Suppress("UNCHECKED_CAST")
            rawValue = readJsonValue as Map<String, Any>
        }

        return when (store) {
            Store.Paddle -> StoreInfoPaddle(rawValue)
            else -> StoreInfoUnknown(store, rawValue)
        }
    }
}