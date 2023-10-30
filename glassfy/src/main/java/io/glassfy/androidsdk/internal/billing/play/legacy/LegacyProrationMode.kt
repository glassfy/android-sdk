package io.glassfy.androidsdk.internal.billing.play.legacy

import com.android.billingclient.api.BillingFlowParams
import io.glassfy.androidsdk.model.ReplacementMode

internal fun ReplacementMode.prorationMode() = when(this) {
    ReplacementMode.CHARGE_FULL_PRICE -> BillingFlowParams.ProrationMode.IMMEDIATE_AND_CHARGE_FULL_PRICE
    ReplacementMode.CHARGE_PRORATED_PRICE -> BillingFlowParams.ProrationMode.IMMEDIATE_AND_CHARGE_PRORATED_PRICE
    ReplacementMode.DEFERRED -> BillingFlowParams.ProrationMode.DEFERRED
    ReplacementMode.UNKNOWN_REPLACEMENT_MODE -> BillingFlowParams.ProrationMode.UNKNOWN_SUBSCRIPTION_UPGRADE_DOWNGRADE_POLICY
    ReplacementMode.WITHOUT_PRORATION -> BillingFlowParams.ProrationMode.IMMEDIATE_WITHOUT_PRORATION
    ReplacementMode.WITH_TIME_PRORATION -> BillingFlowParams.ProrationMode.IMMEDIATE_WITH_TIME_PRORATION
}