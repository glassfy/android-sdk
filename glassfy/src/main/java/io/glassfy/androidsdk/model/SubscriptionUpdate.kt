package io.glassfy.androidsdk.model

data class SubscriptionUpdate(
    val originalSku: String,
    val replacement: ReplacementMode = ReplacementMode.WITH_TIME_PRORATION
) {
    internal var purchaseToken: String = ""
}

enum class ReplacementMode(internal val mode: Int) {
    CHARGE_FULL_PRICE(5),
    CHARGE_PRORATED_PRICE(2),
    DEFERRED(6),
    UNKNOWN_REPLACEMENT_MODE(0),
    WITHOUT_PRORATION(3),
    WITH_TIME_PRORATION(1);

    companion object {
        fun fromReplacementModeValue(value: Int): ReplacementMode {
            return when (value) {
                0 -> UNKNOWN_REPLACEMENT_MODE
                1 -> WITH_TIME_PRORATION
                2 -> CHARGE_PRORATED_PRICE
                3 -> WITHOUT_PRORATION
                5 -> CHARGE_FULL_PRICE
                6 -> DEFERRED
                else -> throw IllegalArgumentException("Undefined ReplacementMode")
            }
        }
    }
}


