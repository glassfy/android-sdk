package io.glassfy.androidsdk.internal.billing.play.billing.mapper

import io.glassfy.androidsdk.model.AccountIdentifiers

internal fun convertAccountIdentifier(a: com.android.billingclient.api.AccountIdentifiers?) =
    a?.run {
        AccountIdentifiers(
            obfuscatedAccountId,
            obfuscatedProfileId
        )
    }