package io.glassfy.androidsdk.internal.network.model.utils

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import io.glassfy.androidsdk.model.ProductType

internal class ProductTypeAdapter {
    @ToJson
    private fun toJson(enum: ProductType): Int {
        return when(enum) {
            ProductType.INAPP -> 1
            ProductType.SUBS -> 2
            ProductType.NON_RENEWABLE -> 3
            ProductType.LICENSE_CODE -> 4
            ProductType.GLASSFY_CODE -> 5
            ProductType.UNKNOWN -> 0
        }
    }

    @FromJson
    fun fromJson(value: Int): ProductType {
        return ProductType.fromValue(value)
    }
}