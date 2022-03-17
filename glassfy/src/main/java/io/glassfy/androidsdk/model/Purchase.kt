package io.glassfy.androidsdk.model

data class Purchase(
    val accountIdentifier: AccountIdentifiers?,
    val developerPayload: String,
    val orderId: String,
    val packageName: String,
    val purchaseState: Int,
    val purchaseTime: Long,
    val purchaseToken: String,
    val quantity: Int,
    val signature: String,
    val skus: ArrayList<String>,
    val hashCode: Int,
    val isAcknowledged: Boolean,
    val isAutoRenewing: Boolean,
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
