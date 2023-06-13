package io.glassfy.paywall

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import io.glassfy.androidsdk.internal.logger.Logger
import io.glassfy.androidsdk.paywall.Paywall
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.net.MalformedURLException

@SuppressLint("SetJavaScriptEnabled")
internal class PaywallWebView(
    context: Context
) : WebView(context) {
    private var paywall: ParcelablePaywall? = null
    private var isLoading = false

    init {
        settings.apply {
            javaScriptEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            builtInZoomControls = false
            displayZoomControls = false
            loadsImagesAutomatically = true
        }
    }

    fun loadPaywall(fragment: PaywallDialogFragment, completion: () -> Unit) {
        if (isLoading) { return }

        fragment.viewModel.paywall?.let { paywall ->
            isLoading = true
            this.paywall = paywall
            removeJavascriptInterface("AndroidHandler")
            addJavascriptInterface(JSInterface(fragment), "AndroidHandler")
            webViewClient = PaywallWebViewClient(paywall.contentUrl, completion)

            val content = paywall.preloadedContent
            if (content != null) {
                loadDataWithBaseURL(paywall.contentUrl, content, "text/html", "UTF-8", "")
            } else {
                loadUrl(paywall.contentUrl)
            }
        }
    }
}

private class PaywallWebViewClient(
    private val targetUrl: String,
    private val completion: () -> Unit
) : WebViewClient() {
    var contentLoaded = false

    override fun onPageFinished(view: WebView, url: String) {
        if (contentLoaded || view.progress != 100 || url != targetUrl) {
            return
        }
        contentLoaded = true
        completion()
    }
}

private class JSInterface(paywallFragment: PaywallDialogFragment) {
    val weakFragment = WeakReference(paywallFragment)

    @JavascriptInterface
    fun postMessage(json: String) {
        val msg = try {
            JSONObject(json)
        } catch (_: Exception) {
            JSONObject()
        }
        Logger.logDebug("PAYWALL - postMessage " + msg.optString("action"))
        when (msg.optString("action")) {
            "restore" ->
                onRestoreAction()

            "link" -> {
                val urlStr = msg.optJSONObject("data")?.optString("url")
                if (urlStr.isNullOrEmpty()) {
                    Logger.logError("PAYWALL Link action: url is missing")
                    return
                }
                try {
                    val url = Uri.parse(urlStr)
                    onLinkAction(url)
                } catch (e: MalformedURLException) {
                    Logger.logError("PAYWALL Link action: url malformed")
                }
            }

            "purchase" -> {
                val skuId = msg.optJSONObject("data")?.optString("sku")
                if (skuId.isNullOrEmpty()) {
                    Logger.logError("PAYWALL Purchase action: SKU is missing")
                    return
                }
                onPurchaseAction(skuId)
            }

            "close" -> onCloseAction()
            "" -> Logger.logError("PAYWALL Missing action from paywall's js")
            else -> Logger.logError("PAYWALL Paywall message not handled")
        }
    }

    private fun onCloseAction() {
        weakFragment.get()?.dismiss()
    }

    private fun onLinkAction(url: Uri) {
        weakFragment.get()?.handleLink(url)
    }

    private fun onRestoreAction() {
        weakFragment.get()?.handleRestore()
    }

    private fun onPurchaseAction(skuId: String) {
        weakFragment.get()?.handlePurchase(skuId)
    }
}