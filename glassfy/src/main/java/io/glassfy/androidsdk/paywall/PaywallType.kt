package io.glassfy.androidsdk.paywall

enum class PaywallType(val value: String) {
    HTML("html"),
    NoCode("nocode"),
    Unknown("unknown");

    companion object {
        internal fun fromValue(value: String): PaywallType {
            return when (value) {
                "html" -> HTML
                "nocode" -> NoCode
                else -> {
                    Unknown
                }
            }
        }
    }
}