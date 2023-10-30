package io.glassfy.androidsdk.internal.billing.play.billing.mapper

class BillingPeriodConversions {
    companion object {
        fun days(period: String): Int? {
            if (period.length < 3) return null
            val num = period.substring(1, period.length - 1).toIntOrNull() ?: return null
            return when (period[period.length - 1]) {
                'D' -> num
                'W' -> num * 7
                'M' -> num * 30
                'Y' -> num * 365
                else -> null
            }
        }
    }
}