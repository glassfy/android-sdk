package io.glassfy.androidsdk.internal.network.model.utils

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import io.glassfy.androidsdk.model.EventType

internal class EventTypeAdapter {
    @ToJson
    private fun toJson(enum: EventType): Int {
        return enum.value
    }

    @FromJson
    fun fromJson(value: Int): EventType {
        return EventType.fromValue(value)
    }
}