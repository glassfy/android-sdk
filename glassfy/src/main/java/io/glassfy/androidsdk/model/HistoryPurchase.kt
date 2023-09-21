package io.glassfy.androidsdk.model

data class HistoryPurchase(
    val developerPayload: String,
    val purchaseTime: Long,
    val purchaseToken: String,
    val quantity: Int,
    val signature: String,
    val skus: List<String>,
    val hashCode: Int,
    val originalJson: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HistoryPurchase

        if (hashCode != other.hashCode) return false

        return true
    }

    override fun hashCode(): Int {
        return hashCode
    }
}