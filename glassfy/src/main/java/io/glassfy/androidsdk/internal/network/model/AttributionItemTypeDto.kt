package io.glassfy.androidsdk.internal.network.model

import io.glassfy.androidsdk.model.AttributionItem

internal class AttributionItemTypeDto {
    companion object {
        internal fun field(type: AttributionItem.Type) = when(type) {
            AttributionItem.Type.AdjustID -> "adjustid"
            AttributionItem.Type.AppsFlyerID -> "appsflyerid"
            AttributionItem.Type.GAID -> "gaid"
            AttributionItem.Type.IP -> "ip"
        }
    }
}
