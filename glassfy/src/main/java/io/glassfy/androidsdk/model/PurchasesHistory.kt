package io.glassfy.androidsdk.model

data class PurchasesHistory(
    val all: List<PurchaseHistory>,
    
    val subscriberId: String,
    val customId: String?
)