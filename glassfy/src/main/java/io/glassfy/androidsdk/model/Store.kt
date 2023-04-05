package io.glassfy.androidsdk.model

enum class Store(val value: Int) {
    AppStore(1),
    PlayStore(2),
    Paddle(3),
    Stripe(4),
    Glassfy(5),
    Unknown(-1);

    companion object {
        internal fun fromValue(value: Int): Store {
            return when (value) {
                1 -> AppStore
                2 -> PlayStore
                3 -> Paddle
                4 -> Stripe
                5 -> Glassfy

                else -> {
                    Unknown
                }
            }
        }
    }
}