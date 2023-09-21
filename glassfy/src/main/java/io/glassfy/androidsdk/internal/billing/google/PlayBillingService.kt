package io.glassfy.androidsdk.internal.billing.google

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import io.glassfy.androidsdk.GlassfyError
import io.glassfy.androidsdk.GlassfyErrorCode
import io.glassfy.androidsdk.internal.billing.IBillingPurchaseDelegate
import io.glassfy.androidsdk.internal.billing.IBillingService
import io.glassfy.androidsdk.internal.logger.Logger
import io.glassfy.androidsdk.internal.network.model.utils.Resource
import io.glassfy.androidsdk.model.AccountIdentifiers
import io.glassfy.androidsdk.model.HistoryPurchase
import io.glassfy.androidsdk.model.Purchase
import io.glassfy.androidsdk.model.SkuDetails
import io.glassfy.androidsdk.model.SubscriptionUpdate

internal class PlayBillingService(
    override val delegate: IBillingPurchaseDelegate, ctx: Context, watcherMode: Boolean
) : IBillingService, IPlayBillingPurchaseDelegate {

    companion object {
        internal const val PURCHASING = -199
    }

    private val billingClientWrapper = PlayBillingClientWrapper(ctx, this, watcherMode)

    override suspend fun inAppPurchaseHistory(): Resource<List<HistoryPurchase>> =
        billingClientWrapper.queryPurchaseHistory(arrayOf(BillingClient.SkuType.INAPP)).let {
            return when (it) {
                is PlayBillingResource.Success -> Resource.Success(convertHistoryPurchases(it.data!!))
                else -> Resource.Error(convertError(it.err!!))
            }
        }

    override suspend fun subsPurchaseHistory(): Resource<List<HistoryPurchase>> =
        billingClientWrapper.queryPurchaseHistory(arrayOf(BillingClient.SkuType.SUBS)).let {
            return when (it) {
                is PlayBillingResource.Success -> Resource.Success(convertHistoryPurchases(it.data!!))
                else -> Resource.Error(convertError(it.err!!))
            }
        }

    override suspend fun allPurchaseHistory(): Resource<List<HistoryPurchase>> =
        billingClientWrapper.queryPurchaseHistory().let {
            return when (it) {
                is PlayBillingResource.Success -> Resource.Success(convertHistoryPurchases(it.data!!))
                else -> Resource.Error(convertError(it.err!!))
            }
        }

    override suspend fun allPurchases(): Resource<List<Purchase>> =
        billingClientWrapper.queryPurchase().let {
            return when (it) {
                is PlayBillingResource.Success -> Resource.Success(convertPurchases(it.data!!))
                else -> Resource.Error(convertError(it.err!!))
            }
        }

    override suspend fun subsPurchases(): Resource<List<Purchase>> =
        billingClientWrapper.queryPurchase(arrayOf(BillingClient.SkuType.SUBS)).let {
            return when (it) {
                is PlayBillingResource.Success -> Resource.Success(convertPurchases(it.data!!))
                else -> Resource.Error(convertError(it.err!!))
            }
        }

    override suspend fun inAppPurchases(): Resource<List<Purchase>> =
        billingClientWrapper.queryPurchase(arrayOf(BillingClient.SkuType.INAPP)).let {
            return when (it) {
                is PlayBillingResource.Success -> Resource.Success(convertPurchases(it.data!!))
                else -> Resource.Error(convertError(it.err!!))
            }
        }

    override suspend fun skuDetails(skuList: Set<String>): Resource<List<SkuDetails>> =
        billingClientWrapper.querySkuDetails(skuList).let { billingRes ->
            return when (billingRes) {
                is PlayBillingResource.Success -> {
                    val skuDetails = convertSkusDetails(billingRes.data!!)
                    val invalidProductIdentifiers =
                        skuList.minus(skuDetails.map { it.sku }.toSet()).joinToString("\n\t")
                    if (invalidProductIdentifiers.isNotEmpty()) {
                        Logger.logDebug("PlayStore does not return details for the following products:\n\t${invalidProductIdentifiers}\nCheck the guide at 🔗 https://docs.glassfy.io/26293898")
                    }

                    return Resource.Success(skuDetails)
                }

                else -> Resource.Error(convertError(billingRes.err!!))
            }
        }

    override suspend fun purchase(
        activity: Activity, sku: SkuDetails, update: SubscriptionUpdate?, accountId: String?
    ): Resource<Purchase> =
        billingClientWrapper.purchaseSku(activity, convertToSkuDetails(sku), update, accountId)
            .let {
                return when (it) {
                    is PlayBillingResource.Success -> Resource.Success(convertPurchase(it.data!!))
                    else -> Resource.Error(convertError(it.err!!))
                }
            }

    override suspend fun consume(purchaseToken: String): Resource<String?> =
        billingClientWrapper.consumeToken(purchaseToken).let {
            return when (it) {
                is PlayBillingResource.Success -> Resource.Success(purchaseToken)
                else -> Resource.Error(convertError(it.err!!))
            }
        }

    override suspend fun acknowledge(purchaseToken: String): Resource<String?> =
        billingClientWrapper.acknowledgeToken(purchaseToken).let {
            return when (it) {
                is PlayBillingResource.Success -> Resource.Success(purchaseToken)
                else -> Resource.Error(convertError(it.err!!))
            }
        }


    // Mapper

    private fun convertHistoryPurchases(ps: List<com.android.billingclient.api.PurchaseHistoryRecord>) =
        ps.map { convertHistoryPurchase(it) }

    private fun convertHistoryPurchase(p: com.android.billingclient.api.PurchaseHistoryRecord) =
        p.run {
            HistoryPurchase(
                developerPayload,
                purchaseTime,
                purchaseToken,
                quantity,
                signature,
                products,
                hashCode(),
                originalJson
            )
        }

    private fun convertPurchases(ps: List<com.android.billingclient.api.Purchase>) =
        ps.map { convertPurchase(it) }

    private fun convertPurchase(p: com.android.billingclient.api.Purchase) = p.run {
        Purchase(
            convertAccountIdentifier(accountIdentifiers),
            developerPayload,
            orderId,
            packageName,
            purchaseState,
            purchaseTime,
            purchaseToken,
            quantity,
            signature,
            products,
            hashCode(),
            isAcknowledged,
            isAutoRenewing,
            originalJson
        )
    }

    private fun convertAccountIdentifier(a: com.android.billingclient.api.AccountIdentifiers?) =
        a?.run {
            AccountIdentifiers(
                obfuscatedAccountId, obfuscatedProfileId
            )
        }

    private fun convertToSkuDetails(s: SkuDetails) = s.run {
        com.android.billingclient.api.SkuDetails(originalJson)
    }

    private fun convertSkusDetails(ps: List<com.android.billingclient.api.SkuDetails>) =
        ps.map { convertSkuDetails(it) }

    private fun convertSkuDetails(s: com.android.billingclient.api.SkuDetails) = s.run {
        SkuDetails(
            description,
            freeTrialPeriod,
            iconUrl,
            introductoryPrice,
            introductoryPriceAmountMicros,
            introductoryPriceCycles,
            introductoryPricePeriod,
            originalPrice,
            originalPriceAmountMicros,
            price,
            priceAmountMicros,
            priceCurrencyCode,
            sku,
            subscriptionPeriod,
            title,
            type,
            hashCode(),
            originalJson
        )
    }

    private fun convertError(b: BillingResult): GlassfyError = b.run {
        return when (responseCode) {
            PURCHASING -> GlassfyErrorCode.Purchasing.toError()
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> GlassfyErrorCode.ProductAlreadyOwned.toError(
                "Failure to purchase since item is already owned (ITEM_ALREADY_OWNED)"
            )

            BillingClient.BillingResponseCode.USER_CANCELED -> GlassfyErrorCode.UserCancelPurchase.toError(
                "User pressed back or canceled a dialog (USER_CANCELED)"
            )

            BillingClient.BillingResponseCode.ITEM_NOT_OWNED -> GlassfyErrorCode.StoreError.toError(
                "Failure to consume since item is not owned (ITEM_NOT_OWNED)"
            )

            BillingClient.BillingResponseCode.SERVICE_TIMEOUT -> GlassfyErrorCode.StoreError.toError(
                "The request has reached the maximum timeout before Google Play responds (SERVICE_TIMEOUT)"
            )

            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> GlassfyErrorCode.StoreError.toError(
                "Play Store service is not connected now (SERVICE_DISCONNECTED)"
            )

            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> GlassfyErrorCode.StoreError.toError(
                "Network connection is down (SERVICE_UNAVAILABLE)"
            )

            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> GlassfyErrorCode.StoreError.toError(
                "Billing API version is not supported for the type requested (BILLING_UNAVAILABLE)"
            )

            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> GlassfyErrorCode.StoreError.toError(
                "Requested product is not available for purchase (ITEM_UNAVAILABLE)"
            )

            BillingClient.BillingResponseCode.DEVELOPER_ERROR -> GlassfyErrorCode.StoreError.toError(
                "Google Play does not recognize the configuration. " + "If you are just getting started, make sure you have configured the application correctly in the Google Play Console. " + "The SKU product ID must match and the APK you are using must be signed with release keys. (DEVELOPER_ERROR)"
            )

            BillingClient.BillingResponseCode.ERROR -> GlassfyErrorCode.StoreError.toError("ERROR")
            BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED -> GlassfyErrorCode.StoreError.toError(
                "The requested feature is not supported (FEATURE_NOT_SUPPORTED)"
            )

            else -> GlassfyErrorCode.StoreError.toError("Unknown error")
        }
    }


    ///    IPlayBillingPurchaseDelegate
    override suspend fun onPlayBillingPurchasePurchase(
        p: com.android.billingclient.api.Purchase, productType: String
    ) {
        Logger.logDebug("onPlayBillingPurchasePurchase ${p.products} - ${Thread.currentThread().name}")

        delegate.onProductPurchase(convertPurchase(p), productType == BillingClient.SkuType.SUBS)
    }
}