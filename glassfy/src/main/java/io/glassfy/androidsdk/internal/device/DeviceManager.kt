package io.glassfy.androidsdk.internal.device

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import io.glassfy.androidsdk.Glassfy

internal class DeviceManager(ctx: Context) : IDeviceManager {
    override val glii: String by lazy {
        val sdkVersion = Glassfy.sdkVersion
        val appVersion = ctx.packageName?.runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ctx.packageManager.getPackageInfo(this, PackageManager.PackageInfoFlags.of(0)).versionName
            } else {
                @Suppress("DEPRECATION") ctx.packageManager.getPackageInfo(this, 0).versionName
            }
        }?.getOrNull() ?: "unknown"
        val model = Build.MODEL
        val version = Build.VERSION.RELEASE

        "2:1:$version:$model:$sdkVersion:$appVersion"
    }
}