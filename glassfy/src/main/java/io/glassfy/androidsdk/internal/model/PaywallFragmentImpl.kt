package io.glassfy.androidsdk.internal.model

import android.annotation.SuppressLint
import android.content.res.Configuration

import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.os.Build
import android.os.Build.VERSION.SDK_INT

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import io.glassfy.androidsdk.Glassfy
import io.glassfy.androidsdk.R
import io.glassfy.androidsdk.internal.logger.Logger

import io.glassfy.androidsdk.internal.utils.DurationFormatter
import io.glassfy.androidsdk.model.Sku
import io.glassfy.androidsdk.model.Store
import io.glassfy.androidsdk.model.ui.PaywallFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.net.MalformedURLException
import java.text.NumberFormat
import java.util.*


internal class PaywallFragmentImpl : PaywallFragment(), PaywallFragment.OnActionsListener {

    companion object {
        fun newInstance(p: PaywallImpl): PaywallFragment = PaywallFragmentImpl().apply {
            arguments = Bundle().apply {
                putParcelable("paywall", p)
            }
        }

        private class CustomWebViewClient(paywallFragment: PaywallFragmentImpl) : WebViewClient() {
            val weakFragment = WeakReference(paywallFragment)
            var contentLoaded = false

            override fun onPageFinished(view: WebView, url: String) {
                weakFragment.get()?.let { fragment ->
                    fragment.paywall?.let { paywall ->
                        if (view.progress != 100 || url != paywall.contentUrl) {
                            return
                        }

                        if (contentLoaded) {
                            return
                        }
                        contentLoaded = true
                        fragment.computeJSInitialization(view, paywall)
                    }
                }
            }
        }

        private class JSInterface(paywallFragment: PaywallFragmentImpl) {
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
                weakFragment.get()?.let {
                    val ctx = it.context
                    val listener = if (ctx is OnCloseListener) ctx else it
                    listener.onClose(it, null, null)
                }
            }

            private fun onLinkAction(url: Uri) {
                weakFragment.get()?.let {
                    val ctx = it.context
                    val listener = if (ctx is OnLinkListener) ctx else it
                    listener.onLink(it, url)
                }
            }

            private fun onRestoreAction() {
                weakFragment.get()?.let {
                    val ctx = it.context
                    val listener = if (ctx is OnRestoreListener) ctx else it
                    listener.onRestore(it)
                }
            }

            private fun onPurchaseAction(skuId: String) {
                weakFragment.get()?.let { fragment ->
                    val ctx = fragment.context
                    val listener = if (ctx is OnPurchaseListener) ctx else fragment
                    fragment.paywall
                        ?.skus
                        ?.find { it.skuId == skuId }
                        ?.let {
                            listener.onPurchase(fragment, it)
                        }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_paywall, container, false)
    }

    override fun getTheme(): Int {
        return R.style.PaywallUITheme
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        paywall?.let { paywall ->
            view.findViewById<WebView>(R.id.paywall_webview)?.apply {

                settings.apply {
                    javaScriptEnabled = true
                    allowFileAccess = true
                    allowContentAccess = true
                    builtInZoomControls = false
                    displayZoomControls = false
                    loadsImagesAutomatically = true
                }

                loadUrl(paywall.contentUrl)
                removeJavascriptInterface("AndroidHandler")
                addJavascriptInterface(JSInterface(this@PaywallFragmentImpl), "AndroidHandler")

                webViewClient = CustomWebViewClient(this@PaywallFragmentImpl)
            }
        }
    }

    private val paywall: PaywallImpl?
        get() = arguments?.getParcelable("paywall")

    private fun computeJSInitialization(view: WebView, paywall: PaywallImpl) {

        Glassfy.customScope.launch {
            val versionName = context?.let {
                it.packageManager.getPackageInfo(it.packageName, 0).versionName
            }
            val uiStyle = context?.let {
                when (it.resources.configuration.uiMode) {
                    Configuration.UI_MODE_NIGHT_YES -> "dark"
                    else -> "light"
                }
            }

            val data = if (paywall.type == PaywallType.NoCode) {
                noCodeSkuDetails(
                    paywall.pwid,
                    paywall.locale,
                    uiStyle ?: "light",
                    paywall.skus
                )
            } else {
                htmlSkuDetails(
                    paywall.pwid,
                    paywall.locale,
                    uiStyle ?: "light",
                    Glassfy.sdkVersion,
                    versionName ?: "unknown",
                    SDK_INT,
                    Build.VERSION.RELEASE,
                    Build.MODEL,
                    Store.PlayStore.value,
                    paywall.skus
                )
            }

            val script = buildJSCode("setSkuDetails", data)
            if (!script.isNullOrEmpty()) {
                withContext(Dispatchers.Main) {
                    view.evaluateJavascript(script) {}
                }
            }
        }
    }

    private fun buildJSCode(action: String, data: JSONObject?) = JSONObject().apply {
        put("action", action)
        putOpt("data", data)
    }.toString()
        .toByteArray()
        .let {
            try {
                Base64.encodeToString(it, Base64.NO_WRAP)
            } catch (_: Exception) {
                null
            }
        }
        ?.let { "callJs('$it');" }

    private fun noCodeSkuDetails(
        pwid: String,
        languageCode: String,
        uiStyle: String, //light or dark
        skus: List<Sku>
    ) = JSONObject().apply {
        val locale = try {
            val lan = Locale.forLanguageTag(languageCode)
            if (lan.language.isNullOrEmpty()) Locale.getDefault() else lan
        } catch (_: Exception) {
            Locale.getDefault()
        }

        val commonMsg = JSONObject().apply {
            put("\$DAY", DurationFormatter.unitName(DurationFormatter.Unit.DAY))
            put("\$WEEK", DurationFormatter.unitName(DurationFormatter.Unit.WEEK))
            put("\$MONTH", DurationFormatter.unitName(DurationFormatter.Unit.MONTH))
            put("\$YEAR", DurationFormatter.unitName(DurationFormatter.Unit.YEAR))
        }

        val skusDetails = JSONObject().apply {

            val percentFormatter = NumberFormat.getPercentInstance(locale)
            val priceFormatter = NumberFormat.getCurrencyInstance(locale)
            val skuDailyPrices = FloatArray(skus.count())
            skus.forEachIndexed { idx, s ->
                val skusDetail = JSONObject().apply {
                    val p = s.product

                    val localeCurrencyCode = priceFormatter.currency?.currencyCode
                    if (localeCurrencyCode.isNullOrEmpty() || localeCurrencyCode != p.priceCurrencyCode) {
                        priceFormatter.currency = try {
                            Currency.getInstance(p.priceCurrencyCode)
                        } catch (_: Exception) {
                            Currency.getInstance("USD")
                        }
                    }

                    val hasFreeOffers = if (p.freeTrialPeriod.isNotEmpty()) 1 else -1 // phase 1
                    val hasIntroOffers =
                        if (p.introductoryPriceAmountPeriod.isNotEmpty()) 1 else -1    // phase 2

                    val msg = JSONObject().apply {
                        put("\$TITLE", p.title)
                        put("\$DESCRIPTION", p.description)
                        put("\$ORIGINAL_PRICE", p.originalPrice)

                        val formatter = DurationFormatter.parseISO8601Period(p.subscriptionPeriod)
                        when (formatter?.unit) {
                            DurationFormatter.Unit.YEAR -> put(
                                "\$ORIGINAL_PERIOD",
                                commonMsg.optString("YEAR")
                            )
                            DurationFormatter.Unit.MONTH -> put(
                                "\$ORIGINAL_PERIOD",
                                commonMsg.optString("MONTH")
                            )
                            DurationFormatter.Unit.WEEK -> put(
                                "\$ORIGINAL_PERIOD",
                                commonMsg.optString("WEEK")
                            )
                            DurationFormatter.Unit.DAY -> put(
                                "\$ORIGINAL_PERIOD",
                                commonMsg.optString("DAY")
                            )
                            else -> put("\$ORIGINAL_PERIOD", commonMsg.optString("DAY"))
                        }
                        put("\$ORIGINAL_DURATION", formatter?.format(locale))

                        val totalDays = formatter?.totalDays ?: 1

                        val originalPrice = p.originalPriceAmountMicro / 1000000.0f
                        val originalPriceDaily = originalPrice / totalDays
                        val originalPriceWeekly = originalPriceDaily * 7.0f
                        val originalPriceYearly = originalPriceDaily * 365.0f
                        val originalPriceMonthly = originalPriceYearly / 12.0f

                        skuDailyPrices[idx] = originalPriceDaily

                        put("\$ORIGINAL_DAILY", priceFormatter.format(originalPriceDaily))
                        put("\$ORIGINAL_WEEKLY", priceFormatter.format(originalPriceWeekly))
                        put("\$ORIGINAL_MONTHLY", priceFormatter.format(originalPriceMonthly))
                        put("\$ORIGINAL_YEARLY", priceFormatter.format(originalPriceYearly))

                        if (hasFreeOffers > 0) {
                            val freeFormatter =
                                DurationFormatter.parseISO8601Period(p.freeTrialPeriod)
                            when (freeFormatter?.unit) {
                                DurationFormatter.Unit.YEAR -> put(
                                    "\$INTRO_PERIOD",
                                    commonMsg.optString("YEAR")
                                )
                                DurationFormatter.Unit.MONTH -> put(
                                    "\$INTRO_PERIOD",
                                    commonMsg.optString("MONTH")
                                )
                                DurationFormatter.Unit.WEEK -> put(
                                    "\$INTRO_PERIOD",
                                    commonMsg.optString("WEEK")
                                )
                                DurationFormatter.Unit.DAY -> put(
                                    "\$INTRO_PERIOD",
                                    commonMsg.optString("DAY")
                                )
                                else -> put("INTRO_PERIOD", commonMsg.optString("DAY"))
                            }
                            put("\$INTRO_DURATION", freeFormatter?.format(locale))

                            put("\$INTRO_PRICE", "\$GL_FREE")
                            put("\$INTRO_DAILY", "\$GL_FREE")
                            put("\$INTRO_WEEKLY", "\$GL_FREE")
                            put("\$INTRO_MONTHLY", "\$GL_FREE")
                            put("\$INTRO_YEARLY", "\$GL_FREE")

                            put("\$INTRO_DISCOUNT", percentFormatter.format(0))
                        } else if (hasIntroOffers > 0) {
                            val introFormatter =
                                DurationFormatter.parseISO8601Period(p.introductoryPriceAmountPeriod)
                            when (introFormatter?.unit) {
                                DurationFormatter.Unit.YEAR -> put(
                                    "\$INTRO_PERIOD",
                                    commonMsg.optString("YEAR")
                                )
                                DurationFormatter.Unit.MONTH -> put(
                                    "\$INTRO_PERIOD",
                                    commonMsg.optString("MONTH")
                                )
                                DurationFormatter.Unit.WEEK -> put(
                                    "\$INTRO_PERIOD",
                                    commonMsg.optString("WEEK")
                                )
                                DurationFormatter.Unit.DAY -> put(
                                    "\$INTRO_PERIOD",
                                    commonMsg.optString("DAY")
                                )
                                else -> put("\$INTRO_PERIOD", commonMsg.optString("DAY"))
                            }
                            put("\$INTRO_DURATION", introFormatter?.format(locale))

                            val introPrice = p.introductoryPriceAmountMicro / 1000000.0f
                            val introTotalDays = introFormatter?.totalDays ?: 1
                            val introPriceDaily = introPrice / introTotalDays
                            val introPriceWeekly = introPriceDaily * 7.0f
                            val introPriceYearly = introPriceDaily * 365.0f
                            val intropriceMonthly = introPriceYearly / 12.0f

                            put("\$INTRO_PRICE", priceFormatter.format(introPrice))
                            put("\$INTRO_DAILY", priceFormatter.format(introPriceDaily))
                            put("\$INTRO_WEEKLY", priceFormatter.format(introPriceWeekly))
                            put("\$INTRO_MONTHLY", priceFormatter.format(intropriceMonthly))
                            put("\$INTRO_YEARLY", priceFormatter.format(introPriceYearly))

                            val introDiscount = introPriceDaily / originalPriceDaily
                            put("INTRO_DISCOUNT", percentFormatter.format(introDiscount))
                        }


                        val price = p.priceAmountMicro / 1000000.0f
                        val priceDaily = price / totalDays
                        val priceWeekly = priceDaily * 7.0f
                        val priceYearly = priceDaily * 365.0f
                        val priceMonthly = priceYearly / 12.0f

                        put("\$PERIOD", optString("ORIGINAL_PERIOD"))
                        put("\$PRICE", p.price)
                        put("\$DURATION", optString("ORIGINAL_DURATION"))
                        put("\$DAILY", priceFormatter.format(priceDaily))
                        put("\$WEEKLY", priceFormatter.format(priceWeekly))
                        put("\$MONTHLY", priceFormatter.format(priceMonthly))
                        put("\$YEARLY", priceFormatter.format(priceYearly))
                    }

                    put("msg", msg)
                    put("product", p)
                    put("identifier", s.skuId)
                    put("offeringid", s.offeringId)
                    put("introductoryeligibility", 0)
                    put("promotionaleligibility", -1)
                    put("extravars", s.extravars)
                }
                put(s.skuId, skusDetail)
            }

            skus.forEachIndexed { i, sku ->
                skus.forEachIndexed { j, _ ->
                    var discount = 0.0
                    if (skuDailyPrices[i] > 0.0 && skuDailyPrices[j] > 0.0) {
                        discount = 1.0 - skuDailyPrices[i] / skuDailyPrices[j]
                    }

                    optJSONObject(sku.skuId)
                        ?.optJSONObject("msg")
                        ?.put("\$ORIGINAL_DISCOUNT_" + j + 1, percentFormatter.format(discount))
                }
            }
        }

        val settings = JSONObject().apply {
            put("pwid", pwid)
            put("locale", languageCode)
            put("uiStyle", uiStyle)
        }

        put("skus", skusDetails)
        put("msg", commonMsg)
        put("settings", settings)
    }

    private fun htmlSkuDetails(
        pwid: String,
        languageCode: String,
        uiStyle: String, //light or dark
        sdkVersion: String,
        appVersion: String,
        subplatform: Int,
        systemVersion: String,
        sysInfo: String,
        store: Int = Store.PlayStore.value,
        skus: List<Sku>
    ) = JSONObject().apply {
        val locale = try {
            val lan = Locale.forLanguageTag(languageCode)
            if (lan.language.isNullOrEmpty()) Locale.getDefault() else lan
        } catch (_: Exception) {
            Locale.getDefault()
        }
        val details = JSONObject().apply {
            val commonMsg = JSONObject().apply {
                put("DAY", DurationFormatter.unitName(DurationFormatter.Unit.DAY))
                put("WEEK", DurationFormatter.unitName(DurationFormatter.Unit.WEEK))
                put("MONTH", DurationFormatter.unitName(DurationFormatter.Unit.MONTH))
                put("YEAR", DurationFormatter.unitName(DurationFormatter.Unit.YEAR))
            }

            val skusDetails = JSONObject().apply {

                val percentFormatter = NumberFormat.getPercentInstance(locale)
                val priceFormatter = NumberFormat.getCurrencyInstance(locale)
                val skuDailyPrices = FloatArray(skus.count())
                skus.forEachIndexed { idx, s ->
                    val skusDetail = JSONObject().apply {
                        val p = s.product

                        val localeCurrencyCode = priceFormatter.currency?.currencyCode
                        if (localeCurrencyCode.isNullOrEmpty() || localeCurrencyCode != p.priceCurrencyCode) {
                            priceFormatter.currency = try {
                                Currency.getInstance(p.priceCurrencyCode)
                            } catch (_: Exception) {
                                Currency.getInstance("USD")
                            }
                        }

                        val hasFreeOffers = if (p.freeTrialPeriod.isNotEmpty()) 1 else -1 // phase 1
                        val hasIntroOffers =
                            if (p.introductoryPriceAmountPeriod.isNotEmpty()) 1 else -1    // phase 2

                        val msg = JSONObject().apply {
                            put("TITLE", p.title)
                            put("DESCRIPTION", p.description)
                            put("ORIGINAL_PRICE", p.originalPrice)

                            val formatter =
                                DurationFormatter.parseISO8601Period(p.subscriptionPeriod)
                            when (formatter?.unit) {
                                DurationFormatter.Unit.YEAR -> put(
                                    "ORIGINAL_PERIOD",
                                    commonMsg.optString("YEAR")
                                )
                                DurationFormatter.Unit.MONTH -> put(
                                    "ORIGINAL_PERIOD",
                                    commonMsg.optString("MONTH")
                                )
                                DurationFormatter.Unit.WEEK -> put(
                                    "ORIGINAL_PERIOD",
                                    commonMsg.optString("WEEK")
                                )
                                DurationFormatter.Unit.DAY -> put(
                                    "ORIGINAL_PERIOD",
                                    commonMsg.optString("DAY")
                                )
                                else -> put("ORIGINAL_PERIOD", commonMsg.optString("DAY"))
                            }
                            put("ORIGINAL_DURATION", formatter?.format(locale))

                            val totalDays = formatter?.totalDays ?: 1

                            val originalPrice = p.originalPriceAmountMicro / 1000000.0f
                            val originalPriceDaily = originalPrice / totalDays
                            val originalPriceWeekly = originalPriceDaily * 7.0f
                            val originalPriceYearly = originalPriceDaily * 365.0f
                            val originalPriceMonthly = originalPriceYearly / 12.0f

                            skuDailyPrices[idx] = originalPriceDaily

                            put("ORIGINAL_DAILY", priceFormatter.format(originalPriceDaily))
                            put("ORIGINAL_WEEKLY", priceFormatter.format(originalPriceWeekly))
                            put("ORIGINAL_MONTHLY", priceFormatter.format(originalPriceMonthly))
                            put("ORIGINAL_YEARLY", priceFormatter.format(originalPriceYearly))

                            if (hasFreeOffers > 0) {
                                val freeFormatter =
                                    DurationFormatter.parseISO8601Period(p.freeTrialPeriod)
                                when (freeFormatter?.unit) {
                                    DurationFormatter.Unit.YEAR -> put(
                                        "INTRO_PERIOD",
                                        commonMsg.optString("YEAR")
                                    )
                                    DurationFormatter.Unit.MONTH -> put(
                                        "INTRO_PERIOD",
                                        commonMsg.optString("MONTH")
                                    )
                                    DurationFormatter.Unit.WEEK -> put(
                                        "INTRO_PERIOD",
                                        commonMsg.optString("WEEK")
                                    )
                                    DurationFormatter.Unit.DAY -> put(
                                        "INTRO_PERIOD",
                                        commonMsg.optString("DAY")
                                    )
                                    else -> put("INTRO_PERIOD", commonMsg.optString("DAY"))
                                }
                                put("INTRO_DURATION", freeFormatter?.format(locale))

                                put("INTRO_PRICE", "\$GL_FREE")
                                put("INTRO_DAILY", "\$GL_FREE")
                                put("INTRO_WEEKLY", "\$GL_FREE")
                                put("INTRO_MONTHLY", "\$GL_FREE")
                                put("INTRO_YEARLY", "\$GL_FREE")

                                put("INTRO_DISCOUNT", percentFormatter.format(0))
                            } else if (hasIntroOffers > 0) {
                                val introFormatter =
                                    DurationFormatter.parseISO8601Period(p.introductoryPriceAmountPeriod)
                                when (introFormatter?.unit) {
                                    DurationFormatter.Unit.YEAR -> put(
                                        "INTRO_PERIOD",
                                        commonMsg.optString("YEAR")
                                    )
                                    DurationFormatter.Unit.MONTH -> put(
                                        "INTRO_PERIOD",
                                        commonMsg.optString("MONTH")
                                    )
                                    DurationFormatter.Unit.WEEK -> put(
                                        "INTRO_PERIOD",
                                        commonMsg.optString("WEEK")
                                    )
                                    DurationFormatter.Unit.DAY -> put(
                                        "INTRO_PERIOD",
                                        commonMsg.optString("DAY")
                                    )
                                    else -> put("INTRO_PERIOD", commonMsg.optString("DAY"))
                                }
                                put("INTRO_DURATION", introFormatter?.format(locale))

                                val introPrice = p.introductoryPriceAmountMicro / 1000000.0f
                                val introTotalDays = introFormatter?.totalDays ?: 1
                                val introPriceDaily = introPrice / introTotalDays
                                val introPriceWeekly = introPriceDaily * 7.0f
                                val introPriceYearly = introPriceDaily * 365.0f
                                val intropriceMonthly = introPriceYearly / 12.0f

                                put("INTRO_PRICE", priceFormatter.format(introPrice))
                                put("INTRO_DAILY", priceFormatter.format(introPriceDaily))
                                put("INTRO_WEEKLY", priceFormatter.format(introPriceWeekly))
                                put("INTRO_MONTHLY", priceFormatter.format(intropriceMonthly))
                                put("INTRO_YEARLY", priceFormatter.format(introPriceYearly))

                                val introDiscount = introPriceDaily / originalPriceDaily
                                put("INTRO_DISCOUNT", percentFormatter.format(introDiscount))
                            }


                            val price = p.priceAmountMicro / 1000000.0f
                            val priceDaily = price / totalDays
                            val priceWeekly = priceDaily * 7.0f
                            val priceYearly = priceDaily * 365.0f
                            val priceMonthly = priceYearly / 12.0f

                            put("PERIOD", optString("ORIGINAL_PERIOD"))
                            put("PRICE", p.price)
                            put("DURATION", optString("ORIGINAL_DURATION"))
                            put("DAILY", priceFormatter.format(priceDaily))
                            put("WEEKLY", priceFormatter.format(priceWeekly))
                            put("MONTHLY", priceFormatter.format(priceMonthly))
                            put("YEARLY", priceFormatter.format(priceYearly))
                        }

                        put("msg", msg)
                        put("product", p)
                        put("identifier", s.skuId)
                        put("offeringid", s.offeringId)
                        put("introductoryeligibility", 0)
                        put("promotionaleligibility", -1)
                        put("extravars", s.extravars)
                    }
                    put(s.skuId, skusDetail)
                }

                skus.forEachIndexed { i, sku ->
                    skus.forEachIndexed { j, _ ->
                        var discount = 0.0
                        if (skuDailyPrices[i] > 0.0 && skuDailyPrices[j] > 0.0) {
                            discount = 1.0 - skuDailyPrices[i] / skuDailyPrices[j]
                        }

                        optJSONObject(sku.skuId)
                            ?.optJSONObject("msg")
                            ?.put("ORIGINAL_DISCOUNT_" + j + 1, percentFormatter.format(discount))
                    }
                }
            }

            val settings = JSONObject().apply {
                put("pwid", pwid)
                put("locale", languageCode)
                put("uiStyle", uiStyle)
                put("sdkVersion", sdkVersion)
                put("appVersion", appVersion)
                put("subplatform", subplatform)
                put("store", store)
                put("systemVersion", systemVersion)
                put("sysInfo", sysInfo)
            }

            put("skus", skusDetails)
            put("msg", commonMsg)
            put("settings", settings)
        }
        put("gy", details)
    }
}
