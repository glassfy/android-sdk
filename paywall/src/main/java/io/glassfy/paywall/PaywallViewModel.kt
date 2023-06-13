package io.glassfy.paywall

import android.util.Base64
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.json.JSONObject

internal class PaywallViewModel : ViewModel() {
    var paywall: ParcelablePaywall? = null
    val shouldDismiss = MutableLiveData<Boolean>()

    fun dismiss() {
        shouldDismiss.postValue(true)
    }

    fun buildJSCode(action: String, configString: String): String? {
        val config = JSONObject(configString)
        val json = JSONObject().apply {
            put("action", action)
            putOpt("data", config)
        }
        return json
            .toString()
            .toByteArray()
            .let {
                try {
                    Base64.encodeToString(it, Base64.NO_WRAP)
                } catch (_: Exception) {
                    null
                }
            }
            ?.let { "callJs('$it');" }
    }
}
