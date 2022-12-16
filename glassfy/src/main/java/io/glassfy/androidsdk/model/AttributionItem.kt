package io.glassfy.androidsdk.model

data class AttributionItem(
    val type: Type,
    val value: String?) {

    enum class Type {
        /**
         * Unique Adjust ID of the device. Available after the installation has been successfully tracked.
         */
        AdjustID,

        /**
         * App identifier generated by AppsFlyer at installation. A new ID is generated if an app is deleted, then reinstalled.
         */
        AppsFlyerID,

        /**
         * Google Advertising ID - unique, user-resettable ID for advertising, provided by Google Play services.
         */
        GAID,

        /**
         * privacy friendly App Set Id (see below link) Android 12+ only
         */
        ASID,

        /**
         * Android ID - Android 8+ (superseed by ASID in Android 12+)
         */
        AID,

        /**
         * User IP address
         */
        IP,
    }
}
