package io.glassfy.androidsdk.model

data class SubscriptionUpdate(
    val originalSku: String,
    val proration: ProrationMode = ProrationMode.IMMEDIATE_WITH_TIME_PRORATION
) {
    internal var purchaseToken: String = ""
}

enum class ProrationMode(internal val mode: Int) {
    DEFERRED(4),
    IMMEDIATE_AND_CHARGE_FULL_PRICE(5),
    IMMEDIATE_AND_CHARGE_PRORATED_PRICE(2), // available only for subscription upgrade
    IMMEDIATE_WITHOUT_PRORATION(3),
    IMMEDIATE_WITH_TIME_PRORATION(1),
    UNKNOWN_SUBSCRIPTION_UPGRADE_DOWNGRADE_POLICY(0);

    companion object {
        fun fromProrationModeValue(value: Int): ProrationMode {
            return when (value) {
                0 -> UNKNOWN_SUBSCRIPTION_UPGRADE_DOWNGRADE_POLICY
                1 -> IMMEDIATE_WITH_TIME_PRORATION
                2 -> IMMEDIATE_AND_CHARGE_PRORATED_PRICE
                3 -> IMMEDIATE_WITHOUT_PRORATION
                4 -> DEFERRED
                5 -> IMMEDIATE_AND_CHARGE_FULL_PRICE
                else -> throw IllegalArgumentException("Undefined ProrationMode")
            }
        }
    }
}
