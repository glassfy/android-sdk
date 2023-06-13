package io.glassfy.androidsdk.internal.logger

import android.util.Log
import io.glassfy.androidsdk.Glassfy
import io.glassfy.androidsdk.LogLevel


object Logger {
    private val TAG by lazy { Glassfy::class.java.simpleName }

    var loglevel: LogLevel = LogLevel.DEBUG

    fun logDebug(msg: String) = log(LogLevel.DEBUG, msg)
    fun logError(msg: String) = log(LogLevel.ERROR, msg)


    private fun log(level: LogLevel, msg: String) {
        if (loglevel == LogLevel.NONE) return
        if (loglevel == LogLevel.ERROR && level == LogLevel.DEBUG) return

        if (level == LogLevel.ERROR) Log.e(TAG, msg) else Log.d(TAG, msg)
    }
}