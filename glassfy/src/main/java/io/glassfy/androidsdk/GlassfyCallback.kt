package io.glassfy.androidsdk

import androidx.annotation.UiThread
import io.glassfy.androidsdk.model.Offerings
import io.glassfy.androidsdk.model.Permissions
import io.glassfy.androidsdk.model.Sku
import io.glassfy.androidsdk.model.Transaction

fun interface Callback<in T> {
    @UiThread
    fun onResult(result: T?, error: GlassfyError?)
}

fun interface PurchaseCallback : Callback<Transaction>

fun interface OfferingsCallback : Callback<Offerings>

fun interface SkuCallback : Callback<Sku>

fun interface PermissionsCallback : Callback<Permissions>

fun interface InitializeCallback : Callback<Boolean>
