package io.glassfy.androidsdk.model

enum class Entitlement(val value: Int) {
    NeverBuy(-9),
    OtherRefund(-8),
    IssueRefund(-7),
    Upgraded(-6),
    ExpiredVoluntarily(-5),
    ProductNotAvailable(-4),
    FailToAcceptIncrease(-3),
    ExpiredFromBilling(-2),
    InRetry(-1),
    MissingInfo(0),
    ExpiredInGrace(1),
    OffPlatform(2),
    NonRenewing(3),
    AutoRenewOff(4),
    AutoRenewOn(5);

    companion object {
        internal fun fromValue(value: Int): Entitlement {
            return when (value) {
                -9 -> NeverBuy
                -8 -> OtherRefund
                -7 -> IssueRefund
                -6 -> Upgraded
                -5 -> ExpiredVoluntarily
                -4 -> ProductNotAvailable
                -3 -> FailToAcceptIncrease
                -2 -> ExpiredFromBilling
                -1 -> InRetry
                0 -> MissingInfo
                1 -> ExpiredInGrace
                2 -> OffPlatform
                3 -> NonRenewing
                4 -> AutoRenewOff
                5 -> AutoRenewOn
                else -> throw IllegalArgumentException("Undefined Entitlement")
            }
        }
    }
}