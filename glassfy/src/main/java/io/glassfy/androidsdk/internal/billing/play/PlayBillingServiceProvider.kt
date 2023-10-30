package io.glassfy.androidsdk.internal.billing.play

import android.content.Context
import io.glassfy.androidsdk.internal.billing.IBillingPurchaseDelegate
import io.glassfy.androidsdk.internal.billing.IBillingService
import io.glassfy.androidsdk.internal.billing.play.billing.PlayBillingService
import io.glassfy.androidsdk.internal.billing.play.legacy.PlayBilling4Service
import io.glassfy.androidsdk.internal.network.model.utils.Resource

internal class PlayBillingServiceProvider {
    companion object {
        suspend fun billingService(
            delegate: IBillingPurchaseDelegate, context: Context, watcherMode: Boolean
        ): IBillingService {
            val defaultService = PlayBillingService(delegate, context, watcherMode)
            return when (val res = defaultService.isAvailable()) {
                is Resource.Success -> {
                    if (res.data!!) {
                        defaultService
                    } else {
                        PlayBilling4Service(delegate, context, watcherMode)
                    }
                }

                is Resource.Error -> defaultService
            }
        }
    }
}