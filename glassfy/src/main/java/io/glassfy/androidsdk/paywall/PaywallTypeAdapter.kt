package io.glassfy.androidsdk.paywall

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson

internal class PaywallTypeAdapter {
    @ToJson
    fun toJson(enum: PaywallType): String {
        return enum.value
    }

    @FromJson
    fun fromJson(value: String): PaywallType {
        return PaywallType.fromValue(value)
    }
}