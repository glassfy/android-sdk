package io.glassfy.paywall

import android.net.Uri
import androidx.fragment.app.DialogFragment
import io.glassfy.androidsdk.GlassfyError
import io.glassfy.androidsdk.model.Sku
import io.glassfy.androidsdk.model.Transaction

abstract class PaywallFragment : DialogFragment() {
    abstract fun setCloseHandler(handler: (Transaction?, GlassfyError?) -> Unit)
    abstract fun setPurchaseHandler(handler: (Sku) -> Unit)
    abstract fun setRestoreHandler(handler: () -> Unit)
    abstract fun setLinkHandler(handler: (Uri) -> Unit)
}
