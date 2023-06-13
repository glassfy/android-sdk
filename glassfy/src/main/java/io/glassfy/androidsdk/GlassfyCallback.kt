package io.glassfy.androidsdk

import androidx.annotation.UiThread
import io.glassfy.androidsdk.model.*
import io.glassfy.androidsdk.paywall.Paywall

fun interface ErrorCallback {
    @UiThread
    fun onResult(error: GlassfyError?)
}

fun interface Callback<in T> {
    @UiThread
    fun onResult(result: T?, error: GlassfyError?)
}

fun interface PurchaseCallback : Callback<Transaction>

fun interface OfferingsCallback : Callback<Offerings>

fun interface SkuCallback : Callback<Sku>

fun interface SkuBaseCallback : Callback<ISkuBase>

fun interface PermissionsCallback : Callback<Permissions>

fun interface InitializeCallback : Callback<Boolean>

fun interface StoreCallback : Callback<StoresInfo>

fun interface UserPropertiesCallback : Callback<UserProperties>

fun interface PurchaseHistoryCallback : Callback<PurchasesHistory>

fun interface PaywallCallback : Callback<Paywall>