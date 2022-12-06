package io.glassfy.androidsdk.internal.model

import android.content.Context
import android.os.Handler
import android.os.Parcelable
import android.webkit.WebView
import android.webkit.WebViewClient
import io.glassfy.androidsdk.Glassfy
import io.glassfy.androidsdk.Glassfy.runAndPostResult
import io.glassfy.androidsdk.PaywallUICallback
import io.glassfy.androidsdk.internal.network.model.utils.Resource
import io.glassfy.androidsdk.model.Paywall
import io.glassfy.androidsdk.model.Sku
import io.glassfy.androidsdk.model.ui.PaywallFragment
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlin.coroutines.resume

@Parcelize
internal data class PaywallImpl(
    val contentUrl: String,
    val pwid: String,
    val locale: String,
    val type: PaywallType,
    val version: String,
    internal var skus_: List<Sku>,
    var contentPath: String? = null,
): Parcelable, Paywall {

    val skus: List<Sku> get() = skus_

    @IgnoredOnParcel
    private var isLoaded = false

    @IgnoredOnParcel
    private var isLoading = false

    override fun loadPaywallViewController(ctx: Context, callback: PaywallUICallback) {
        Glassfy.customScope.runAndPostResult(callback) {
            startLoading(ctx)
        }
    }

    @IgnoredOnParcel
    private val _paywallFragment by lazy {
        PaywallFragmentImpl.newInstance(this)
    }
    override val paywallFragment: PaywallFragment
        get() = _paywallFragment

    internal suspend fun startLoading(ctx: Context): Resource<PaywallFragment> {
        if (contentPath != null || isLoaded || isLoading) {
            return Resource.Success(paywallFragment)
        }

        isLoading = true


        withTimeoutOrNull(15000) {

            suspendCancellableCoroutine<Boolean> { c ->
                var wv: WebView? = null
                val mHandler = Handler(ctx.mainLooper)
                mHandler.post {
                    wv = WebView(ctx)
                    wv?.loadUrl(contentUrl)
                    wv?.webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String) {
                            if (c.isActive && c.context.isActive) {
                                isLoaded = true
                                c.resume(true)
                            }
                        }
                    }
                }
                c.invokeOnCancellation {
                    mHandler.post {
                        wv?.stopLoading()
                    }
                }
            }
        }

        isLoading = false

        return Resource.Success(paywallFragment)
    }
}

