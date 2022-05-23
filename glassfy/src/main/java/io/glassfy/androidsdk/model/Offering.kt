package io.glassfy.androidsdk.model

data class Offering(val offeringId: String, internal var skus_: List<Sku>) {
    val skus: List<Sku> get() = skus_
}
