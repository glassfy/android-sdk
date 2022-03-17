package io.glassfy.androidsdk.internal.cache

internal interface ICacheManager {
    val installationId: String
    var subscriberId: String?
}