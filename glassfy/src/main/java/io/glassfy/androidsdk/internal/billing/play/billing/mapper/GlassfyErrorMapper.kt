package io.glassfy.androidsdk.internal.billing.play.billing.mapper

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import io.glassfy.androidsdk.GlassfyError
import io.glassfy.androidsdk.GlassfyErrorCode

fun convertError(b: BillingResult): GlassfyError = b.run {
    return when (responseCode) {
        GlassfyErrorCode.Purchasing.internalCode!! -> GlassfyErrorCode.Purchasing.toError(
            "Purchase already in progress..."
        )

        BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> GlassfyErrorCode.ProductAlreadyOwned.toError(
            "The purchase failed because the item is already owned. (ITEM_ALREADY_OWNED)"
        )

        BillingClient.BillingResponseCode.USER_CANCELED -> GlassfyErrorCode.UserCancelPurchase.toError(
            "Transaction was canceled by the user. (USER_CANCELED)"
        )

        BillingClient.BillingResponseCode.ITEM_NOT_OWNED -> GlassfyErrorCode.StoreError.toError(
            "Action on the item failed since it is not owned by the user. (ITEM_NOT_OWNED)"
        )

        BillingClient.BillingResponseCode.SERVICE_TIMEOUT -> GlassfyErrorCode.StoreError.toError(
            "The request has reached the maximum timeout before Google Play responds. (SERVICE_TIMEOUT)"
        )

        BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> GlassfyErrorCode.StoreError.toError(
            "Play Store service is not connected now. (SERVICE_DISCONNECTED)"
        )

        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> GlassfyErrorCode.StoreError.toError(
            "The service is currently unavailable. (SERVICE_UNAVAILABLE)"
        )

        BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> GlassfyErrorCode.StoreError.toError(
            "A user billing error occurred during processing. Examples where this error may occur:\n" +
                    "- The Play Store app on the user's device is out of date.\n" +
                    "- The user is in an unsupported country.\n" +
                    "- The user is an enterprise user and their enterprise admin has disabled users from making purchases.\n" +
                    "- Google Play is unable to charge the user’s payment method.\n" +
                    "(BILLING_UNAVAILABLE)"
        )

        BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> GlassfyErrorCode.StoreError.toError(
            "Requested product is not available for purchase. (ITEM_UNAVAILABLE)"
        )

        BillingClient.BillingResponseCode.DEVELOPER_ERROR -> GlassfyErrorCode.StoreError.toError(
            "Google Play does not recognize the configuration. " + "If you are just getting started, make sure you have configured the application correctly in the Google Play Console. " + "The SKU product ID must match and the APK you are using must be signed with release keys. (DEVELOPER_ERROR)"
        )

        BillingClient.BillingResponseCode.ERROR -> GlassfyErrorCode.StoreError.toError(
            "Internal Google Play error. Obsolete Play Store version? (ERROR)"
        )

        BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED -> GlassfyErrorCode.StoreError.toError(
            "The requested feature is not supported on the current device. (FEATURE_NOT_SUPPORTED)"
        )

        BillingClient.BillingResponseCode.NETWORK_ERROR -> GlassfyErrorCode.StoreError.toError("A network error occurred during the operation. (NETWORK_ERROR)")

        else -> GlassfyErrorCode.StoreError.toError("Unknown error")
    }
}