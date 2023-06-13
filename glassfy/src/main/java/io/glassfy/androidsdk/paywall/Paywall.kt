package io.glassfy.androidsdk.paywall

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import io.glassfy.androidsdk.Glassfy
import io.glassfy.androidsdk.model.Sku
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resumeWithException

data class Paywall(
    val contentUrl: String,
    val pwid: String,
    val locale: String,
    val type: PaywallType,
    var skus: List<Sku>
) {
    var preloadedContent: String? = null

    private var contentAvailableHandler: () -> Unit = {}

    init {
        Glassfy.customScope.launch {
            preload()
            contentBecameAvailable()
        }
    }

    fun onContentAvailable(handler: () -> Unit) {
        contentAvailableHandler = handler
        if (preloadedContent != null) {
            contentBecameAvailable()
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun config(context: Context?): JSONObject {
        return SkuDetailsProvider().json(context, this)
    }

    private fun contentBecameAvailable() {
        MainScope().launch {
            contentAvailableHandler()
        }
    }

    private suspend fun preload() {
        preloadedContent = fetchContents()
    }

    private suspend fun fetchContents(): String? {
        return suspendCancellableCoroutine { cont ->
            try {
                val url = URL(contentUrl)
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "GET"
                val input = BufferedReader(InputStreamReader(connection.inputStream))

                cont.invokeOnCancellation {
                    connection.disconnect()
                }

                val result = input.use { it.readText() }
                cont.resumeWith(Result.success(result))
            } catch (exception: Exception) {
                cont.resumeWithException(exception)
            }
        }
    }
}