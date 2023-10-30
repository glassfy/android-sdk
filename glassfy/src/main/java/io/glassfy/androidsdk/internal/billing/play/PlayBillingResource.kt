package io.glassfy.androidsdk.internal.billing.play

import com.android.billingclient.api.BillingResult

internal sealed class PlayBillingResource<T>(
    val data: T? = null,
    val err: BillingResult? = null
) {
    internal class Success<T>(data: T) : PlayBillingResource<T>(data)
    internal class Error<T>(err: BillingResult, data: T? = null) : PlayBillingResource<T>(data, err)
}