package io.glassfy.androidsdk.model.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.fragment.app.DialogFragment
import io.glassfy.androidsdk.Glassfy
import io.glassfy.androidsdk.GlassfyError
import io.glassfy.androidsdk.model.Sku
import io.glassfy.androidsdk.model.Transaction

abstract class PaywallFragment: DialogFragment() {
    interface OnCloseListener {
        fun onClose(f: PaywallFragment, transaction: Transaction?, error: GlassfyError?) = f.dismiss()
    }

    interface OnLinkListener {
        fun onLink(f: PaywallFragment, url: Uri) {
            runCatching {
                val i = Intent(Intent.ACTION_VIEW)
                    .apply { data = url }
                f.startActivity(i)
            }
        }
    }

    interface OnRestoreListener {
        fun onRestore(f: PaywallFragment) = Glassfy.restore { _, err ->
            if (f is OnCloseListener) f.onClose(f, null, err)
        }
    }

    interface OnPurchaseListener {
        fun onPurchase(f: PaywallFragment, sku: Sku) = Glassfy.purchase(f.activity as Activity, sku) { t, err ->
            if (f is OnCloseListener) f.onClose(f, t, err)
        }
    }

    interface OnActionsListener: OnCloseListener, OnLinkListener, OnRestoreListener, OnPurchaseListener
}

