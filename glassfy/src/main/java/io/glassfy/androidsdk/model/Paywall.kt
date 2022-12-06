package io.glassfy.androidsdk.model

import android.content.Context
import io.glassfy.androidsdk.PaywallUICallback
import io.glassfy.androidsdk.model.ui.PaywallFragment

interface Paywall {
    fun loadPaywallViewController(ctx: Context, callback: PaywallUICallback)

    val paywallFragment: PaywallFragment
}