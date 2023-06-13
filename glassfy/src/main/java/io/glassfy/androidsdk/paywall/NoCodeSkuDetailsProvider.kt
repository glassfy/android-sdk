package io.glassfy.androidsdk.paywall

import android.os.Build
import androidx.annotation.RequiresApi
import io.glassfy.androidsdk.model.Sku
import org.json.JSONObject

@RequiresApi(Build.VERSION_CODES.N)
internal class NoCodeSkuDetailsProvider : BaseSkuDetailsProvider() {
    fun json(
        id: String,
        languageCode: String,
        uiStyle: String,
        skus: List<Sku>
    ): JSONObject {
        return JSONObject().apply {
            val locale = locale(languageCode)
            // TODO: check paywall code \$ vs $
            val skusDetails = buildSkusDetails(skus, locale, "$")

            val settings = JSONObject().apply {
                put("pwid", id)
                put("locale", languageCode)
                put("uiStyle", uiStyle)
            }

            put("skus", skusDetails)
            put("msg", durations())
            put("settings", settings)
        }
    }
}