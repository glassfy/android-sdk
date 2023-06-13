package io.glassfy.paywall

import android.app.Activity
import android.app.UiModeManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration.UI_MODE_NIGHT_MASK
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.net.Uri
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.fragment.app.viewModels
import com.google.android.material.progressindicator.CircularProgressIndicator
import io.glassfy.androidsdk.Glassfy
import io.glassfy.androidsdk.GlassfyError
import io.glassfy.androidsdk.model.Sku
import io.glassfy.androidsdk.model.Transaction
import io.glassfy.androidsdk.paywall.Paywall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val paywallKey = "paywall"

internal class PaywallDialogFragment : PaywallFragment() {
    internal lateinit var viewModel: PaywallViewModel

    private var webView: PaywallWebView? = null
    private var progress: CircularProgressIndicator? = null
    private var skipNextOnClose = false

    private var closeHandler: ((Transaction?, GlassfyError?) -> Unit)? = null
    private var purchaseHandler: ((Sku) -> Unit)? = null
    private var restoreHandler: (() -> Unit)? = null
    private var linkHandler: ((Uri) -> Unit)? = null

    companion object {
        fun newInstance(context: Context, paywall: Paywall): PaywallDialogFragment {
            val fragment = PaywallDialogFragment()
            val bundle = Bundle()
            bundle.putParcelable(paywallKey, ParcelablePaywall.fromPaywall(context, paywall))
            fragment.arguments = bundle
            return fragment
        }
    }

    internal fun startLoading() {
        webView?.loadPaywall(this) {
            webViewDidLoad()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel: PaywallViewModel by viewModels()
        this.viewModel = viewModel
    }

    override fun onSaveInstanceState(outState: Bundle) {
        skipNextOnClose = true
        super.onSaveInstanceState(outState)
    }

    override fun onDismiss(dialog: DialogInterface) {
        if (!skipNextOnClose) {
            handleClose(null, null)
        }
        skipNextOnClose = false
        super.onDismiss(dialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val rootView = inflater.inflate(R.layout.fragment_paywall, container, false)
        val relativeLayout = rootView as RelativeLayout

        val bgColor = if (isDarkModeOn()) {
            R.color.background_dark
        } else {
            R.color.background_light
        }
        relativeLayout.setBackgroundColor(requireContext().getColor(bgColor))

        observeDismiss()
        attachWebView(relativeLayout)
        setupActivityIndicator(relativeLayout)
        startActivityIndicator()
        return rootView
    }

    private fun observeDismiss() {
        viewModel.shouldDismiss.observe(viewLifecycleOwner) { dismiss ->
            if (dismiss) {
                this.dismiss()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        retrievePaywallAndStartLoadingIfPossible(arguments)
    }

    private fun attachWebView(relativeLayout: RelativeLayout) {
        webView = PaywallWebView(requireContext())
        webView?.layoutParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT
        )
        webView?.visibility = INVISIBLE
        webView?.let { relativeLayout.addView(it) }
    }

    override fun getTheme(): Int {
        return R.style.PaywallUITheme
    }

    private fun isDarkModeOn(): Boolean {
        val context = requireContext()
        val configuration = context.resources.configuration
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        val currentNightMode = configuration.uiMode and UI_MODE_NIGHT_MASK
        return currentNightMode == UI_MODE_NIGHT_YES || uiModeManager.nightMode == UiModeManager.MODE_NIGHT_YES
    }

    private fun retrievePaywallAndStartLoadingIfPossible(bundle: Bundle?) {
        bundle?.classLoader = ParcelablePaywall::class.java.classLoader
        bundle?.getParcelable<ParcelablePaywall>(paywallKey)?.let {
            viewModel.paywall = it
            startLoading()
        }
    }

    private fun webViewDidLoad() {
        stopActivityIndicator()
        webView?.visibility = VISIBLE

        viewModel.paywall?.config?.let { paywallConfig ->
            GlassfyPaywall.customScope.launch {
                val script = viewModel.buildJSCode("setSkuDetails", paywallConfig)
                if (!script.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) {
                        webView?.evaluateJavascript(script) {}
                    }
                }
            }
        }
    }

    private fun startActivityIndicator() {
        progress?.visibility = VISIBLE
        webView?.isEnabled = false
    }

    private fun stopActivityIndicator() {
        progress?.visibility = GONE
        webView?.isEnabled = true
    }

    private fun setupActivityIndicator(relativeLayout: RelativeLayout) {
        val themeWrapper = ContextThemeWrapper(requireContext(), R.style.PaywallUITheme)
        val fgColor = if (isDarkModeOn()) {
            R.color.background_light
        } else {
            R.color.background_dark
        }

        progress = CircularProgressIndicator(themeWrapper)
        progress?.isIndeterminate = true
        progress?.trackColor = requireContext().getColor(fgColor)

        val layoutParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT)
        progress?.layoutParams = layoutParams
        progress?.let { relativeLayout.addView(it) }
    }

    override fun setCloseHandler(handler: (Transaction?, GlassfyError?) -> Unit) {
        closeHandler = handler
    }

    override fun setPurchaseHandler(handler: (Sku) -> Unit) {
        purchaseHandler = handler
    }

    override fun setRestoreHandler(handler: () -> Unit) {
        restoreHandler = handler
    }

    override fun setLinkHandler(handler: (Uri) -> Unit) {
        linkHandler = handler
    }

    private fun handleClose(transaction: Transaction?, error: GlassfyError?) {
        closeHandler?.invoke(transaction, error)
        closeHandler = null
        linkHandler = null
        restoreHandler = null
        purchaseHandler = null
    }

    internal fun handleLink(url: Uri) {
        if (linkHandler == null) {
            runCatching {
                val i = Intent(Intent.ACTION_VIEW).apply { data = url }
                startActivity(i)
            }
        } else {
            linkHandler?.invoke(url)
        }
    }

    internal fun handleRestore() {
        if (restoreHandler == null) {
            Glassfy.restore { _, _ ->
                viewModel.dismiss()
            }
        } else {
            restoreHandler?.invoke()
        }
    }

    internal fun handlePurchase(skuId: String) {
        MainScope().launch {
            startActivityIndicator()

            Glassfy.sku(skuId) { sku, error ->
                MainScope().launch {
                    handlePurchase(sku, error)
                }
            }
        }
    }

    private fun handlePurchase(sku: Sku?, error: GlassfyError?) {
        if (sku == null) {
            handleClose(null, error)
            viewModel.dismiss()
        } else {
            handlePurchase(sku)
            stopActivityIndicator()
        }
    }

    private fun handlePurchase(sku: Sku) {
        if (purchaseHandler == null) {
            Glassfy.purchase(activity as Activity, sku) { t, e ->
                handleClose(t, e)
                viewModel.dismiss()
            }
        } else {
            purchaseHandler?.invoke(sku)
        }
    }
}