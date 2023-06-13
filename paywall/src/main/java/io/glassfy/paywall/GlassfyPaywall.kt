package io.glassfy.paywall

import android.content.Context
import androidx.annotation.UiThread
import io.glassfy.androidsdk.Glassfy
import io.glassfy.androidsdk.GlassfyError
import io.glassfy.androidsdk.GlassfyErrorCode
import io.glassfy.androidsdk.paywall.Paywall
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class GlassfyPaywall {
    companion object {
        /**
         * Returns a fragment representing the paywall.
         *
         * @param paywall Paywall object obtained via `Glassfy.paywall`
         */
        @JvmStatic
        fun fragment(context: Context, paywall: Paywall): PaywallFragment {
            return PaywallDialogFragment.newInstance(context, paywall)
        }

        /**
         * Returns a fragment representing the paywall.
         *
         * @param remoteConfigurationId Remote configuration identifier
         * @param awaitLoading Default false, if true, the callback is executed after the paywall content has been fetched.
         */
        @JvmStatic
        fun fragment(
            context: Context,
            remoteConfigurationId: String,
            awaitLoading: Boolean = false,
            callback: PaywallUICallback
        ) {
            Glassfy.paywall(remoteConfigurationId) { paywall, error ->
                if (error == null && paywall != null) {
                    if (awaitLoading) {
                        paywall.onContentAvailable {
                            val fragment = fragment(context, paywall)
                            callback.onResult(fragment, null)
                        }
                    } else {
                        val fragment = fragment(context, paywall)
                        callback.onResult(fragment, null)
                    }
                } else {
                    callback.onResult(null, error ?: GlassfyErrorCode.CouldNotBuildPaywall.toError())
                }
            }
        }

        internal val customScope by lazy {
            CoroutineScope(
                SupervisorJob() + Dispatchers.IO + CoroutineName(
                    "glassfy-paywall"
                )
            )
        }
    }
}

fun interface PaywallUICallback {
    @UiThread
    fun onResult(result: PaywallFragment?, error: GlassfyError?)
}