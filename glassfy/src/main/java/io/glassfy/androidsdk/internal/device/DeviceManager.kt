package io.glassfy.androidsdk.internal.device

import android.content.Context
import android.os.Build
import io.glassfy.androidsdk.Glassfy

internal class DeviceManager(ctx: Context) : IDeviceManager {
    override val glii: String by lazy {
        val sdkVersion = Glassfy.sdkVersion
        val appVersion = ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName
        val model = Build.MODEL
        val version = Build.VERSION.RELEASE

        "2:1:$version:$model:$sdkVersion:$appVersion"
    }
}