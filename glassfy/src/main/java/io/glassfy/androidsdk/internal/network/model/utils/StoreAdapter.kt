package io.glassfy.androidsdk.internal.network.model.utils

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import io.glassfy.androidsdk.model.Store

internal class StoreAdapter {
    @ToJson
    private fun toJson(enum: Store): Int {
        return enum.value
    }

    @FromJson
    fun fromJson(value: Int): Store {
        return Store.fromValue(value)
    }
}