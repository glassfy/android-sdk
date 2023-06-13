package io.glassfy.androidsdk.paywall

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.annotation.RequiresApi
import io.glassfy.androidsdk.Glassfy
import io.glassfy.androidsdk.model.Store
import org.json.JSONObject
import com.squareup.moshi.*

@RequiresApi(Build.VERSION_CODES.N)
internal class SkuDetailsProvider {
    private val noCode = NoCodeSkuDetailsProvider()
    private val html = HtmlSkuDetailsProvider()

    fun json(context: Context?, paywall: Paywall): JSONObject {
        val versionName = versionName(context)
        val uiStyle = uiStyle(context)

        return if (paywall.type == PaywallType.NoCode) {
            noCode.json(
                paywall.pwid,
                paywall.locale,
                uiStyle,
                paywall.skus
            )
        } else {
            html.json(
                paywall.pwid,
                paywall.locale,
                uiStyle,
                Glassfy.sdkVersion,
                versionName,
                Build.VERSION.SDK_INT,
                Build.VERSION.RELEASE,
                Build.MODEL,
                Store.PlayStore.value,
                paywall.skus
            )
        }
    }

    private fun versionName(context: Context?): String {
        return context?.packageName?.runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(this, PackageManager.PackageInfoFlags.of(0)).versionName
            } else {
                @Suppress("DEPRECATION") context.packageManager.getPackageInfo(this, 0).versionName
            }
        }?.getOrNull() ?: "unknown"
    }

    private fun uiStyle(context: Context?): String {
        return context?.let {
            when (it.resources.configuration.uiMode) {
                Configuration.UI_MODE_NIGHT_YES -> "dark"
                else -> "light"
            }
        } ?: "light"
    }
}

