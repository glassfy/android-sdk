package io.glassfy.androidsdk.model

enum class ProductType {
    INAPP,
    SUBS,
    NON_RENEWABLE,
    LICENSE_CODE,
    GLASSFY_CODE,
    UNKNOWN;

    companion object {
        internal fun fromValue(value: Int): ProductType {
            return when (value) {
                1 -> SUBS
                2 -> INAPP
                3 -> NON_RENEWABLE
                4 -> LICENSE_CODE
                5 -> GLASSFY_CODE
                else -> UNKNOWN
            }
        }
    }
}

