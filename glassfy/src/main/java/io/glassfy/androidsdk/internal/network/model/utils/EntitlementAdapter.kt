package io.glassfy.androidsdk.internal.network.model.utils

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import io.glassfy.androidsdk.model.Entitlement

internal class EntitlementAdapter {
    @ToJson
    private fun toJson(enum: Entitlement): Int {
        return enum.value
    }

    @FromJson
    fun fromJson(value: Int): Entitlement {
        return Entitlement.fromValue(value)
    }
}