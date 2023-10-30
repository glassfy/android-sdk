package io.glassfy.androidsdk.internal.billing.play.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import io.glassfy.androidsdk.GlassfyErrorCode
import io.glassfy.androidsdk.PurchaseDelegate
import io.glassfy.androidsdk.internal.billing.IBillingPurchaseDelegate
import io.glassfy.androidsdk.internal.billing.IBillingService
import io.glassfy.androidsdk.internal.billing.SkuDetailsQuery
import io.glassfy.androidsdk.internal.billing.play.IPlayBillingPurchaseDelegate
import io.glassfy.androidsdk.internal.billing.play.PlayBillingResource
import io.glassfy.androidsdk.internal.billing.play.billing.mapper.convertError
import io.glassfy.androidsdk.internal.billing.play.billing.mapper.convertHistoryPurchases
import io.glassfy.androidsdk.internal.billing.play.billing.mapper.convertLegacySkusDetails
import io.glassfy.androidsdk.internal.billing.play.billing.mapper.convertPurchase
import io.glassfy.androidsdk.internal.billing.play.billing.mapper.convertPurchases
import io.glassfy.androidsdk.internal.billing.play.billing.mapper.convertSkuDetails
import io.glassfy.androidsdk.internal.logger.Logger
import io.glassfy.androidsdk.internal.network.model.utils.Resource
import io.glassfy.androidsdk.model.HistoryPurchase
import io.glassfy.androidsdk.model.ProductType
import io.glassfy.androidsdk.model.Purchase
import io.glassfy.androidsdk.model.SkuDetails
import io.glassfy.androidsdk.model.SubscriptionUpdate

internal class PlayBillingService(
    override val delegate: IBillingPurchaseDelegate, ctx: Context, watcherMode: Boolean
) : IBillingService, IPlayBillingPurchaseDelegate {

    override val version: Int
        get() = 6

    private var _delegate: PurchaseDelegate? = null

    private val billingClientWrapper = PlayBillingClientWrapper(ctx, this, watcherMode)

    suspend fun isAvailable(): Resource<Boolean> {
        billingClientWrapper.isFeatureSupported(BillingClient.FeatureType.PRODUCT_DETAILS).let {
            return when (it) {
                is PlayBillingResource.Success -> Resource.Success(it.data!!)
                is PlayBillingResource.Error -> Resource.Error(convertError(it.err!!))
            }
        }
    }

    override suspend fun inAppPurchaseHistory(): Resource<List<HistoryPurchase>> {
        billingClientWrapper.inappPurchaseHistory().let {
            return when (it) {
                is PlayBillingResource.Success -> Resource.Success(convertHistoryPurchases(it.data!!))
                is PlayBillingResource.Error -> Resource.Error(convertError(it.err!!))
            }
        }
    }

    override suspend fun subsPurchaseHistory(): Resource<List<HistoryPurchase>> {
        billingClientWrapper.subsPurchaseHistory().let {
            return when (it) {
                is PlayBillingResource.Success -> Resource.Success(convertHistoryPurchases(it.data!!))
                is PlayBillingResource.Error -> Resource.Error(convertError(it.err!!))
            }
        }
    }

    override suspend fun allPurchaseHistory(): Resource<List<HistoryPurchase>> {
        billingClientWrapper.allPurchaseHistory().let {
            return when (it) {
                is PlayBillingResource.Success -> Resource.Success(convertHistoryPurchases(it.data!!))
                is PlayBillingResource.Error -> Resource.Error(convertError(it.err!!))
            }
        }
    }

    override suspend fun inAppPurchases(): Resource<List<Purchase>> {
        billingClientWrapper.inappPurchase().let {
            return when (it) {
                is PlayBillingResource.Success -> Resource.Success(convertPurchases(it.data!!))
                is PlayBillingResource.Error -> Resource.Error(convertError(it.err!!))
            }
        }
    }

    override suspend fun subsPurchases(): Resource<List<Purchase>> {
        billingClientWrapper.subsPurchase().let {
            return when (it) {
                is PlayBillingResource.Success -> Resource.Success(convertPurchases(it.data!!))
                is PlayBillingResource.Error -> Resource.Error(convertError(it.err!!))
            }
        }
    }

    override suspend fun allPurchases(): Resource<List<Purchase>> {
        billingClientWrapper.allPurchase().let {
            return when (it) {
                is PlayBillingResource.Success -> Resource.Success(convertPurchases(it.data!!))
                is PlayBillingResource.Error -> Resource.Error(convertError(it.err!!))
            }
        }
    }

    override suspend fun skuDetails(query: SkuDetailsQuery): Resource<SkuDetails> =
        skusDetails(listOf(query)).let { res ->
            when (res) {
                is Resource.Success -> res.data?.firstOrNull()?.let {
                    Resource.Success(it)
                } ?: Resource.Error(GlassfyErrorCode.NotFoundOnStore.toError())

                is Resource.Error -> Resource.Error(res.err!!)
            }
        }

    override suspend fun skusDetails(queries: List<SkuDetailsQuery>): Resource<List<SkuDetails>> {
        val (legacyQueries, productQueries) = queries.partition {
            it.basePlanId == null && it.productType != ProductType.INAPP
        }

        val legacyRes = legacySkuDetails(legacyQueries)
        if (legacyRes is Resource.Error) {
            return legacyRes
        }

        val productRes = productSkuDetails(productQueries)
        if (productRes is Resource.Error) {
            return productRes
        }
        return Resource.Success(legacyRes.data.orEmpty() + productRes.data.orEmpty())
    }

    override suspend fun purchase(
        activity: Activity, product: SkuDetails, update: SubscriptionUpdate?, accountId: String?
    ): Resource<Purchase> {
        if (product.originalJson.isNotEmpty()) {
            return legacyPurchaseSkuDetails(activity, product, update, accountId)
        }

        val billingProduct = when (product.type) {
            ProductType.SUBS -> BillingClient.ProductType.SUBS
            ProductType.INAPP -> BillingClient.ProductType.INAPP
            else -> null
        }?.let {
            billingClientWrapper.queryProductsDetails(setOf(product.sku), it)
        }?.let {
            it.data?.firstOrNull()
        } ?: return Resource.Error(GlassfyErrorCode.NotFoundOnStore.toError())

        billingClientWrapper.purchaseProductDetails(
            activity, billingProduct, product.offerToken, update, accountId
        ).let {
            return when (it) {
                is PlayBillingResource.Success -> Resource.Success(convertPurchase(it.data!!))
                is PlayBillingResource.Error -> Resource.Error(convertError(it.err!!))
            }
        }

    }


    override suspend fun consume(purchaseToken: String): Resource<String?> =
        billingClientWrapper.consumeToken(purchaseToken).let {
            return when (it) {
                is PlayBillingResource.Success -> Resource.Success(purchaseToken)
                is PlayBillingResource.Error -> Resource.Error(convertError(it.err!!))
            }
        }

    override suspend fun acknowledge(purchaseToken: String): Resource<String?> =
        billingClientWrapper.acknowledgeToken(purchaseToken).let {
            return when (it) {
                is PlayBillingResource.Success -> Resource.Success(purchaseToken)
                is PlayBillingResource.Error -> Resource.Error(convertError(it.err!!))
            }
        }


    ///    IPlayBillingPurchaseDelegate

    override suspend fun onPlayBillingPurchasePurchase(
        p: com.android.billingclient.api.Purchase, productType: String
    ) {
        Logger.logDebug("onPlayBillingPurchasePurchase ${p.products} - ${Thread.currentThread().name}")

        delegate.onProductPurchase(
            convertPurchase(p), productType == BillingClient.ProductType.SUBS
        )
    }


    //// Utils

    private suspend fun productSkuDetails(queries: List<SkuDetailsQuery>): Resource<List<SkuDetails>> {
        val resInapp =
            queries.filter { it.productType == ProductType.INAPP || it.productType == ProductType.UNKNOWN }
                .map { it.productId }.toSet().let {
                    billingClientWrapper.queryProductsDetails(
                        it, BillingClient.ProductType.INAPP
                    )
                }
        if (resInapp is PlayBillingResource.Error) {
            return Resource.Error(convertError(resInapp.err!!))
        }

        val resSubs =
            queries.filter { it.productType == ProductType.SUBS || it.productType == ProductType.UNKNOWN }
                .map { it.productId }.toSet().let {
                    billingClientWrapper.queryProductsDetails(
                        it, BillingClient.ProductType.SUBS
                    )
                }
        if (resSubs is PlayBillingResource.Error) {
            return Resource.Error(convertError(resSubs.err!!))
        }

        return convertSkuDetails(resInapp.data.orEmpty() + resSubs.data.orEmpty(), queries).let {
            Resource.Success(it)
        }
    }

    private suspend fun legacySkuDetails(queries: List<SkuDetailsQuery>): Resource<List<SkuDetails>> {
        val skuList = queries.map { it.productId }.toSet()
        return billingClientWrapper.querySkuDetails(skuList).let { billingRes ->
            when (billingRes) {
                is PlayBillingResource.Success -> {
                    val skuDetails = convertLegacySkusDetails(billingRes.data!!)
                    val invalidProductIdentifiers = skuList.minus(skuDetails.map { it.sku }.toSet())
                    if (invalidProductIdentifiers.isNotEmpty()) {
                        val message = "PlayStore did not return details for the following products:"
                        val docs =
                            "Check the guide at \uD83D\uDD17 https://docs.glassfy.io/26293898"
                        Logger.logDebug("$message\n\t${invalidProductIdentifiers.joinToString("\n\t")}\n$docs")
                    }
                    return Resource.Success(skuDetails)
                }

                is PlayBillingResource.Error -> Resource.Error(convertError(billingRes.err!!))
            }
        }
    }

    private suspend fun legacyPurchaseSkuDetails(
        activity: Activity, product: SkuDetails, update: SubscriptionUpdate?, accountId: String?
    ): Resource<Purchase> {
        val s = product.run { com.android.billingclient.api.SkuDetails(originalJson) }
        billingClientWrapper.purchaseSkuDetails(activity, s, update, accountId).let {
            return when (it) {
                is PlayBillingResource.Success -> Resource.Success(convertPurchase(it.data!!))
                is PlayBillingResource.Error -> Resource.Error(convertError(it.err!!))
            }
        }
    }
}