package io.glassfy.androidsdk.internal.network.model.utils

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import io.glassfy.androidsdk.internal.model.PaywallType

internal class PaywallTypeAdapter {
    @ToJson
    private fun toJson(enum: PaywallType): String {
        return enum.value
    }

    @FromJson
    fun fromJson(value: String): PaywallType {
        return PaywallType.fromValue(value)
    }
}