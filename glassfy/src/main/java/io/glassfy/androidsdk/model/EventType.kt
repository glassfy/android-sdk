package io.glassfy.androidsdk.model

enum class EventType(val value: Int) {
    InitialBuy(5001),
    Restarted(5002),
    Renewed(5003),
    Expired(5004),
    DidChangeRenewalStatus(5005),
    IsInBillingRetryPeriod(5006),
    ProductChange(5007),
    InAppPurchase(5008),
    Refund(5009),
    Paused(5010),
    Resumed(5011),
    ConnectLicense(5012),
    DisconnectLicense(5013),
    Unknown(-1);

    companion object {
        internal fun fromValue(value: Int): EventType {
            return when (value) {
                5001 -> InitialBuy
                5002 -> Restarted
                5003 -> Renewed
                5004 -> Expired
                5005 -> DidChangeRenewalStatus
                5006 -> IsInBillingRetryPeriod
                5007 -> ProductChange
                5008 -> InAppPurchase
                5009 -> Refund
                5010 -> Paused
                5011 -> Resumed
                5012 -> ConnectLicense
                5013 -> DisconnectLicense

                else -> {
                    Unknown
                }
            }
        }
    }
}