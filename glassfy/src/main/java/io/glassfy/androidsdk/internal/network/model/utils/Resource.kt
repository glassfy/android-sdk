package io.glassfy.androidsdk.internal.network.model.utils

import io.glassfy.androidsdk.GlassfyError

internal sealed class Resource<T>(
    val data: T? = null,
    val err: GlassfyError? = null
) {
    class Success<T>(data: T) : Resource<T>(data)
    class Error<T>(err: GlassfyError, data: T? = null) : Resource<T>(data, err)
}


