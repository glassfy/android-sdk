package io.glassfy.androidsdk.internal.cache

import android.content.Context
import java.util.*

internal class CacheManager(ctx: Context) : ICacheManager {

    private val pref = ctx.getSharedPreferences("GLASSFY_PREF", 0)

    override val installationId: String by lazy {
        var iid = pref.getString(kInstallationId, null)
        if (iid.isNullOrBlank()) {
            iid = UUID.randomUUID().toString()
            pref.edit().apply {
                putString(kInstallationId, iid)
            }
        }

        return@lazy iid
    }

    override var subscriberId: String? = null

    private companion object {
        private const val kInstallationId = "kInstallationId"
    }
}