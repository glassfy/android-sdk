package io.glassfy.androidsdk.internal.billing.google

import com.android.billingclient.api.BillingResult

internal sealed class PlayBillingResource<T>(
    val data: T? = null,
    val err: BillingResult? = null
) {
    class Success<T>(data: T) : PlayBillingResource<T>(data)
    class Error<T>(err: BillingResult, data: T? = null) : PlayBillingResource<T>(data, err)
}